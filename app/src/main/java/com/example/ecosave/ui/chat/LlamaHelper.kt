package com.example.ecosave.ui.chat

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LlamaAndroid

class LlamaHelper(
    val contentResolver: ContentResolver,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val sharedFlow: MutableSharedFlow<LLMEvent>
) {

    private val llama by lazy { LlamaAndroid(contentResolver) }
    private var loadJob: Job? = null
    private var completionJob: Job? = null
    private var currentContext: Int? = null
    private var tokenCount = 0
    private var allText = ""
    var generatedText = ""

    fun load(
        path: String,
        contextLength: Int,
        mmprojPath: String? = null,
        loaded: (Long) -> Unit
    ) {
        currentContext?.let { id -> llama.releaseContext(id) }
        
        try {
            val modelUri = Uri.parse(path)
            Log.d("LlamaHelper", ">>> Custom LlamaHelper - Opening model FD for URI: $modelUri")
            
            // Explicitly check readability
            contentResolver.openInputStream(modelUri)?.use { input ->
                val firstByte = input.read()
                val size = contentResolver.openFileDescriptor(modelUri, "r")?.use { it.statSize } ?: -1
                Log.d("LlamaHelper", ">>> Model is readable, first byte: $firstByte, size: $size")
            } ?: Log.e("LlamaHelper", ">>> Model is NOT readable via openInputStream")

            val modelPfd = contentResolver.openFileDescriptor(modelUri, "r")
                ?: throw IllegalArgumentException("Cannot open model URI: $modelUri")
            val modelFd = modelPfd.detachFd()
            Log.d("LlamaHelper", ">>> Model FD: $modelFd")

            val config = mutableMapOf<String, Any>(
                "model" to path,
                "model_fd" to modelFd,
                "use_mmap" to true, // Enable memory mapping
                "use_mlock" to false,
                "n_ctx" to contextLength,
                "embedding" to false,
                "n_batch" to 512,
                "n_threads" to 0,
                "n_gpu_layers" to 0,
                "vocab_only" to false,
                "lora" to "",
                "lora_scaled" to 1.0,
                "rope_freq_base" to 0.0,
                "rope_freq_scale" to 0.0
            )

            mmprojPath?.let {
                val mmUri = Uri.parse(it)
                Log.d("LlamaHelper", ">>> Opening mmproj FD for URI: $mmUri")
                val mmPfd = contentResolver.openFileDescriptor(mmUri, "r")
                if (mmPfd != null) {
                    val mmFd = mmPfd.detachFd()
                    config["mmproj_fd"] = mmFd
                    Log.d("LlamaHelper", ">>> Mmproj FD: $mmFd")
                }
            }

            loadJob = scope.launch {
                Log.d("LlamaHelper", ">>> will start llama context with config: $config")
                val result = try {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        llama.startEngine(config) { token ->
                            // This callback fires during BOTH prompt processing and generation.
                            // We only accumulate into generatedText during active prediction.
                            generatedText += token
                            tokenCount++
                            val ongoingResult = sharedFlow.tryEmit(LLMEvent.Ongoing(token, tokenCount))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LlamaHelper", "Engine start failed", e)
                    null
                }

                if (result == null) {
                    sharedFlow.tryEmit(LLMEvent.Error("Model initialization failed"))
                    return@launch
                }

                val id = result["contextId"] ?: throw Exception("contextId not found in result map")
                currentContext = (id as Number).toInt()

                Log.d("LlamaHelper", ">>> Context loaded successfully with ID: $currentContext")
                sharedFlow.tryEmit(LLMEvent.Loaded(path))
                loaded(currentContext!!.toLong())
            }
        } catch (e: Exception) {
            Log.e("LlamaHelper", "Failed to prepare model loading", e)
            sharedFlow.tryEmit(LLMEvent.Error("Failed to open files: ${e.message}"))
        }
    }

    fun predict(prompt: String, imagePath: String? = null, partialCompletion: Boolean = true) {
        val context = currentContext ?: throw Exception("Model was not loaded yet")
        val startTime = System.currentTimeMillis()
        tokenCount = 0
        allText = ""
        generatedText = ""
        
        val params = mutableMapOf<String, Any>(
            "prompt" to prompt,
            "emit_partial_completion" to partialCompletion,
            "n_predict" to com.example.ecosave.config.PromptConfig.MAX_TOKENS,
            "stop" to com.example.ecosave.config.PromptConfig.STOP_SEQUENCES,
        )
        
        imagePath?.let {
            try {
                val imgUri = Uri.parse(it)
                Log.d("LlamaHelper", ">>> Opening image FD for URI: $imgUri")
                contentResolver.openFileDescriptor(imgUri, "r")?.use { pfd ->
                    val imgFd = pfd.detachFd()
                    params["image_fds"] = listOf(imgFd)
                    Log.d("LlamaHelper", ">>> Image FD added to params: $imgFd")
                }
            } catch (e: Exception) {
                Log.e("LlamaHelper", "Failed to open image FD", e)
            }
        }

        completionJob = scope.launch {
            val startedResult = sharedFlow.tryEmit(LLMEvent.Started(prompt))
            Log.d("LlamaHelper", ">>> tryEmit Started result: $startedResult")
            try {
                Log.d("LlamaHelper", ">>> Calling launchCompletion...")
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    llama.launchCompletion(
                        id = context,
                        params = params
                    )
                }
                Log.d("LlamaHelper", ">>> launchCompletion finished. Result keys: ${result?.keys}")
                if (result != null) {
                    val resultText = result["text"] as? String ?: ""
                    Log.d("LlamaHelper", ">>> Generated text from result map: $resultText")
                    if (resultText.isNotEmpty()) {
                        allText = resultText
                        generatedText = resultText
                    }
                    // If token count is not set/streamed, set it to metadata or length
                    val metadata = result["completion_metadata"] as? Map<*, *>
                    val generatedTokens = metadata?.get("completed_tokens") as? Number
                    tokenCount = generatedTokens?.toInt() ?: tokenCount
                }
            } catch (e: Exception) {
                Log.e("LlamaHelper", ">>> launchCompletion threw exception", e)
            }
            val duration = System.currentTimeMillis() - startTime
            val doneResult = sharedFlow.tryEmit(LLMEvent.Done(generatedText, tokenCount, duration))
            Log.d("LlamaHelper", ">>> tryEmit Done result: $doneResult, tokens: $tokenCount, textLength: ${generatedText.length}")
        }
    }

    fun stopPrediction() {
        val id = currentContext ?: return
        scope.launch {
            llama.stopCompletion(id)
        }
        completionJob?.cancel()
    }

    fun release() {
        currentContext?.let { id ->
            llama.releaseContext(id)
        }
        currentContext = null
    }

    fun abort() {
        loadJob?.cancel()
        stopPrediction()
    }

    sealed class LLMEvent {
        data class Loaded(val path: String) : LLMEvent()
        data class Started(val prompt: String) : LLMEvent()
        data class Ongoing(val word: String, val tokenCount: Int) : LLMEvent()
        data class Done(val fullText: String, val tokenCount: Int, val duration: Long) : LLMEvent()
        data class Error(val message: String) : LLMEvent()
    }
}
