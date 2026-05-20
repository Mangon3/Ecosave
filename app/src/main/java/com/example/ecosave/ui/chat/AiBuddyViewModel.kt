package com.example.ecosave.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ecosave.BuildConfig
import com.example.ecosave.config.PromptConfig
import com.example.ecosave.data.local.AppDatabase
import com.example.ecosave.data.network.FinnhubApi
import com.example.ecosave.data.network.RetrofitClient
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.coroutines.resume

class AiBuddyViewModel(application: Application) : AndroidViewModel(application) {

    private val _response = MutableLiveData<String>()
    val response: LiveData<String> = _response

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _history = MutableLiveData<List<ChatMessage>>()
    val history: LiveData<List<ChatMessage>> = _history

    private val _chatReset = MutableLiveData<Boolean>()
    val chatReset: LiveData<Boolean> = _chatReset

    private val llmEvents = MutableSharedFlow<LlamaHelper.LLMEvent>(extraBufferCapacity = 64)
    private val llamaHelper: LlamaHelper
    private val budgetDao = AppDatabase.getDatabase(application).budgetDao()
    private val chatDao = AppDatabase.getDatabase(application).chatDao()

    // Cache market data so we don't refetch on every message
    private var cachedMarketContext: String = "Market data not yet loaded."

    init {
        llamaHelper = LlamaHelper(
            contentResolver = application.contentResolver,
            scope = viewModelScope,
            sharedFlow = llmEvents
        )

        // Observe events from the native llama.cpp engine
        viewModelScope.launch {
            llmEvents.collect { event ->
                android.util.Log.d("AiBuddyViewModel", ">>> Collected event: $event")
                when (event) {
                    is LlamaHelper.LLMEvent.Loaded -> {
                        _isLoading.postValue(false)
                        // Fetch history from DB. If empty, send LLM-generated greeting.
                        withContext(Dispatchers.IO) {
                            val existingMessages = chatDao.getAllMessages()
                            if (existingMessages.isEmpty()) {
                                sendGreeting()
                            } else {
                                _history.postValue(existingMessages)
                            }
                        }
                    }
                    is LlamaHelper.LLMEvent.Started -> {
                        _isLoading.postValue(true)
                    }
                    is LlamaHelper.LLMEvent.Ongoing -> {
                        val currentReply = llamaHelper.generatedText.trim()
                        if (currentReply.isNotEmpty()) {
                            _response.postValue(currentReply)
                        }
                    }
                    is LlamaHelper.LLMEvent.Done -> {
                        val reply = event.fullText.trim()
                        android.util.Log.d("AiBuddyViewModel", ">>> LLM Done. reply: $reply")
                        if (reply.isNotEmpty()) {
                            _response.postValue(reply)
                            // Save AI response to DB
                            withContext(Dispatchers.IO) {
                                chatDao.insert(ChatMessage(reply, false))
                            }
                        }
                        _isLoading.postValue(false)
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        _response.postValue("Error: ${event.message}")
                        _isLoading.postValue(false)
                    }
                }
            }
        }

        // Initialize the model
        val modelFile = java.io.File(application.getExternalFilesDir(null), PromptConfig.MODEL_FILENAME)
        if (!modelFile.exists()) {
            _response.postValue("Error: Model file not found at " + modelFile.absolutePath)
            _isLoading.postValue(false)
        } else {
            val modelUri = androidx.core.content.FileProvider.getUriForFile(
                application,
                "com.example.ecosave.fileprovider",
                modelFile
            )
            val modelPath = modelUri.toString()
            _isLoading.postValue(true)
            llamaHelper.load(path = modelPath, contextLength = PromptConfig.CONTEXT_LENGTH) { contextId ->
                // Model loaded successfully
            }
        }

        // Pre-fetch market data on init
        viewModelScope.launch {
            cachedMarketContext = fetchMarketData()
        }
    }

    private fun sendGreeting() {
        viewModelScope.launch {
            val budgetContext = withContext(Dispatchers.IO) { buildBudgetContext() }
            if (cachedMarketContext == "Market data not yet loaded.") {
                cachedMarketContext = fetchMarketData()
            }
            val greetingPrompt = PromptConfig.formatGreetingPrompt(budgetContext, cachedMarketContext)
            android.util.Log.d("AiBuddyViewModel", ">>> GREETING PROMPT:\n$greetingPrompt")
            llamaHelper.predict(prompt = greetingPrompt, partialCompletion = true)
        }
    }

    fun askQuestion(prompt: String) {
        viewModelScope.launch {
            // Save user prompt to DB
            withContext(Dispatchers.IO) {
                chatDao.insert(ChatMessage(prompt, true))
            }

            // Fetch budget data on a background thread
            val budgetContext = withContext(Dispatchers.IO) {
                buildBudgetContext()
            }

            // Refresh market data if stale
            if (cachedMarketContext == "Market data not yet loaded.") {
                cachedMarketContext = fetchMarketData()
            }

            val systemPrompt = PromptConfig.buildFullSystemPrompt(budgetContext, cachedMarketContext)
            
            // Build recent chat history as context
            val historyContext = withContext(Dispatchers.IO) {
                val pastMsgs = chatDao.getAllMessages()
                // Take last 3 messages to keep prompt short for TinyLlama
                val recent = if (pastMsgs.size > 3) pastMsgs.subList(pastMsgs.size - 3, pastMsgs.size) else pastMsgs
                buildString {
                    for (msg in recent) {
                        val role = if (msg.isUser) "user" else "assistant"
                        append("<|$role|>\n${msg.text}</s>\n")
                    }
                }
            }

            val formattedPrompt = "<|system|>\n$systemPrompt</s>\n$historyContext<|assistant|>\n"
            android.util.Log.d("AiBuddyViewModel", ">>> FULL PROMPT:\n$formattedPrompt")
            llamaHelper.predict(prompt = formattedPrompt, partialCompletion = true)
        }
    }

    fun resetChat() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                chatDao.deleteAllMessages()
            }
            _chatReset.postValue(true)
            sendGreeting()
        }
    }

    private fun buildBudgetContext(): String {
        return try {
            val totalIncome = budgetDao.totalIncome
            val totalExpenses = budgetDao.totalExpenses
            val balance = totalIncome - totalExpenses
            val recentEntries = budgetDao.recentEntries

            buildString {
                append("Total Income: $${String.format("%.2f", totalIncome)}\n")
                append("Total Expenses: $${String.format("%.2f", totalExpenses)}\n")
                append("Current Balance: $${String.format("%.2f", balance)}\n")

                if (recentEntries.isNotEmpty()) {
                    append("Recent transactions:\n")
                    for (entry in recentEntries) {
                        val type = if (entry.isExpense) "Expense" else "Income"
                        append("  $type: ${entry.description} - $${String.format("%.2f", entry.amount)} (${entry.category})\n")
                    }
                } else {
                    append("No transactions recorded yet.\n")
                }
            }
        } catch (e: Exception) {
            "Budget data unavailable.\n"
        }
    }

    private suspend fun fetchMarketData(): String = kotlinx.coroutines.coroutineScope {
        return@coroutineScope try {
            val api = RetrofitClient.getClient().create(FinnhubApi::class.java)
            val apiKey = BuildConfig.FINNHUB_API_KEY
            if (apiKey.isNullOrEmpty()) return@coroutineScope "API key not configured."

            val stocks = arrayOf(
                arrayOf("BHP", "BHP Group"),
                arrayOf("RIO", "Rio Tinto"),
                arrayOf("WDS", "Woodside Energy")
            )

            val results = awaitAll(
                *stocks.map { stock: Array<String> ->
                    async {
                        suspendCancellableCoroutine<String> { cont ->
                            api.getQuote(stock[0], apiKey).enqueue(object : Callback<JsonObject> {
                                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                                    if (response.isSuccessful && response.body() != null) {
                                        val data = response.body()!!
                                        val price = if (data.has("c")) data.get("c").asDouble else 0.0
                                        val change = if (data.has("d")) data.get("d").asDouble else 0.0
                                        val pct = if (data.has("dp")) data.get("dp").asDouble else 0.0
                                        if (price > 0) {
                                            val sign = if (change >= 0) "+" else ""
                                            cont.resume(String.format(Locale.US,
                                                "%s (%s): $%.2f %s%.2f (%.2f%%)",
                                                stock[0], stock[1], price, sign, change, pct))
                                        } else {
                                            cont.resume("")
                                        }
                                    } else {
                                        cont.resume("")
                                    }
                                }

                                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                                    cont.resume("")
                                }
                            })
                        }
                    }
                }.toTypedArray()
            )

            val marketText = results.filter { it.isNotEmpty() }.joinToString("\n")
            if (marketText.isNotEmpty()) marketText else "Market data currently unavailable."
        } catch (e: Exception) {
            "Market data fetch failed."
        }
    }

    fun stopGeneration() {
        llamaHelper.stopPrediction()
        _isLoading.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        llamaHelper.release()
    }
}
