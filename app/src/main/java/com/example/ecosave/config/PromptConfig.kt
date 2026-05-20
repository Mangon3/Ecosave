package com.example.ecosave.config

/**
 * Centralized prompt configuration for the Ecosave AI buddy.
 * Keep the system prompt SHORT - TinyLlama echoes long prompts.
 */
object PromptConfig {

    /** Maximum number of tokens the model can generate per response */
    const val MAX_TOKENS = 512

    /** Stop sequences that terminate generation */
    val STOP_SEQUENCES = listOf("</s>", "<|user|>", "<|system|>")

    /** Model filename in the app's external files directory */
    const val MODEL_FILENAME = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"

    /** Context window size for the model (must fit system prompt + data + response) */
    const val CONTEXT_LENGTH = 1024

    /** Number of recent transactions to include in context */
    const val RECENT_TRANSACTIONS_COUNT = 10

    /**
     * System prompt - kept SHORT to prevent TinyLlama from echoing it back.
     * Added strict "First Person" instruction to stop third-person narrator behavior.
     */
    val SYSTEM_PROMPT = """
        You are Ecosave AI, a professional financial assistant. You must always speak in the first person ("I").
        Respond in a concise, direct, and formal tone.
        Based on the user's budget and ASX trends, provide general advice on whether they should save or invest.
        Avoid meta-language, pleasantries, and conversational filler. Do not describe yourself in the third person.
    """.trimIndent()

    /**
     * Builds the full system prompt with dynamic user data injected.
     */
    fun buildFullSystemPrompt(budgetContext: String, marketContext: String): String {
        return buildString {
            append(SYSTEM_PROMPT)
            append("\n\nBudget: ")
            append(budgetContext)
            append("\nMarket: ")
            append(marketContext)
        }
    }

    /**
     * Wraps the system prompt and user message in the ChatML template.
     */
    fun formatPrompt(systemPrompt: String, userMessage: String): String {
        return "<|system|>\n$systemPrompt</s>\n<|user|>\n$userMessage</s>\n<|assistant|>\n"
    }

    /** * Changed to a natural user question. This prompts the model to answer naturally 
     * rather than getting confused by a complex behavioral instruction. 
     */
    const val GREETING_PROMPT = "Hello! Who are you and what features can you help me with?"

    /**
     * Builds the auto-greeting prompt using the system context.
     */
    fun formatGreetingPrompt(budgetContext: String, marketContext: String): String {
        val systemPrompt = buildFullSystemPrompt(budgetContext, marketContext)
        return formatPrompt(systemPrompt, GREETING_PROMPT)
    }
}