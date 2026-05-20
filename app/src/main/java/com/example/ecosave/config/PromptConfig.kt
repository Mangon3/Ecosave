package com.example.ecosave.config

/**
 * Centralized prompt configuration for the Ecosave AI buddy.
 * Kept minimal to prevent TinyLlama from echoing instructions.
 */
object PromptConfig {

    /** Maximum number of tokens the model can generate per response */
    const val MAX_TOKENS = 150

    /** Stop sequences that terminate generation */
    val STOP_SEQUENCES = listOf("</s>", "<|user|>", "<|system|>")

    /** Model filename in the app's external files directory */
    const val MODEL_FILENAME = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"

    /** Context window size for the model */
    const val CONTEXT_LENGTH = 1024

    /** Number of recent transactions to include in context */
    const val RECENT_TRANSACTIONS_COUNT = 10

    /** System prompt - one short sentence. */
    val SYSTEM_PROMPT = "You are a helpful financial assistant. Answer briefly in under 50 words."

    /** Simple greeting prompt sent once on startup. */
    const val GREETING_PROMPT = "Hello! Introduce yourself briefly and tell me what you can help with."

    /**
     * Builds the full system prompt with dynamic user data injected.
     */
    fun buildFullSystemPrompt(budgetContext: String, marketContext: String): String {
        return "$SYSTEM_PROMPT\nUser budget: $budgetContext\nMarket: $marketContext"
    }

    /**
     * Wraps the system prompt and user message in the ChatML template.
     */
    fun formatPrompt(systemPrompt: String, userMessage: String): String {
        return "<|system|>\n$systemPrompt</s>\n<|user|>\n$userMessage</s>\n<|assistant|>\n"
    }

    /**
     * Builds the auto-greeting prompt using the system context.
     */
    fun formatGreetingPrompt(budgetContext: String, marketContext: String): String {
        val systemPrompt = buildFullSystemPrompt(budgetContext, marketContext)
        return formatPrompt(systemPrompt, GREETING_PROMPT)
    }
}