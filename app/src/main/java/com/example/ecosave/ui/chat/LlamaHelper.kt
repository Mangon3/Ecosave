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
        // Load the model context with specified parameters
        currentContext?.let { id -> llama.releaseContext(id) }
        
        try {
            val modelUri = Uri.parse(path)
            
            contentResolver.openInputStream(modelUri)?.use { input ->
                val firstByte = input.read()
                val size = contentResolver.openFileDescriptor(modelUri, "r")?.use { it.statSize } ?: -1
            }

            val modelPfd = contentResolver.openFileDescriptor(modelUri, "r")
                ?: throw IllegalArgumentException("Cannot open model URI: $modelUri")
            val modelFd = modelPfd.detachFd()

            val config = mutableMapOf<String, Any>(
                "model" to path,
                "model_fd" to modelFd,
                "use_mmap" to true,
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
                val mmPfd = contentResolver.openFileDescriptor(mmUri, "r")
                if (mmPfd != null) {
                    val mmFd = mmPfd.detachFd()
                    config["mmproj_fd"] = mmFd
                }
            }

            loadJob = scope.launch {
                val result = try {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        llama.startEngine(config) { token ->
                            generatedText += token
                            tokenCount++
                            sharedFlow.tryEmit(LLMEvent.Ongoing(token, tokenCount))
                        }
                    }
                } catch (e: Exception) {
                    null
                }

                if (result == null) {
                    sharedFlow.tryEmit(LLMEvent.Error("Model initialization failed"))
                    return@launch
                }

                val id = result["contextId"] ?: throw Exception("contextId not found in result map")
                currentContext = (id as Number).toInt()

                sharedFlow.tryEmit(LLMEvent.Loaded(path))
                loaded(currentContext!!.toLong())
            }
        } catch (e: Exception) {
            sharedFlow.tryEmit(LLMEvent.Error("Failed to open files: ${e.message}"))
        }
    }

    fun predict(prompt: String, imagePath: String? = null, partialCompletion: Boolean = true) {
        // Predict next tokens using the current prompt
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
                contentResolver.openFileDescriptor(imgUri, "r")?.use { pfd ->
                    val imgFd = pfd.detachFd()
                    params["image_fds"] = listOf(imgFd)
                }
            } catch (e: Exception) {
            }
        }

        completionJob = scope.launch {
            sharedFlow.tryEmit(LLMEvent.Started(prompt))
            try {
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    llama.launchCompletion(
                        id = context,
                        params = params
                    )
                }
                if (result != null) {
                    val resultText = result["text"] as? String ?: ""
                    if (resultText.isNotEmpty()) {
                        allText = resultText
                        generatedText = resultText
                    }
                    val metadata = result["completion_metadata"] as? Map<*, *>
                    val generatedTokens = metadata?.get("completed_tokens") as? Number
                    tokenCount = generatedTokens?.toInt() ?: tokenCount
                }
            } catch (e: Exception) {
            }
            val duration = System.currentTimeMillis() - startTime
            sharedFlow.tryEmit(LLMEvent.Done(generatedText, tokenCount, duration))
        }
    }

    fun stopPrediction() {
        // Stop prediction & generation tasks
        val id = currentContext ?: return
        scope.launch {
            llama.stopCompletion(id)
        }
        completionJob?.cancel()
    }

    fun release() {
        // Release the loaded model context
        currentContext?.let { id ->
            llama.releaseContext(id)
        }
        currentContext = null
    }

    fun abort() {
        // Abort both loading & generation tasks
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
