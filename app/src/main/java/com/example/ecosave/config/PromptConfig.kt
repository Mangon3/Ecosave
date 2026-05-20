package com.example.ecosave.config

object PromptConfig {
    const val MAX_TOKENS = 150

    val STOP_SEQUENCES = listOf("</s>", "<|user|>", "<|system|>")

    const val MODEL_FILENAME = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"

    const val CONTEXT_LENGTH = 1024

    // Number of recent transactions to load
    const val RECENT_TRANSACTIONS_COUNT = 10

    val SYSTEM_PROMPT = "You are a helpful financial assistant. Answer briefly in under 50 words."

    const val GREETING_PROMPT = "Hello! Introduce yourself briefly and tell me what you can help with."

    // Builds the system prompt with context
    fun buildFullSystemPrompt(budgetContext: String, marketContext: String): String {
        return "$SYSTEM_PROMPT\nUser budget: $budgetContext\nMarket: $marketContext"
    }

    // Format message into chatml format
    fun formatPrompt(systemPrompt: String, userMessage: String): String {
        return "<|system|>\n$systemPrompt</s>\n<|user|>\n$userMessage</s>\n<|assistant|>\n"
    }

    // Format greeting prompt w' context
    fun formatGreetingPrompt(budgetContext: String, marketContext: String): String {
        val systemPrompt = buildFullSystemPrompt(budgetContext, marketContext)
        return formatPrompt(systemPrompt, GREETING_PROMPT)
    }
}