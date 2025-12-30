package com.example.budgie.ai

import android.util.Log

/**
 * Lightweight Router - First layer of AI processing
 * Detects simple queries that don't need LLM reasoning
 * Reduces LLM usage by ~40-60%
 */
class LightweightRouter {

    enum class RouteType {
        FAST_PATH,      // Simple greeting/thanks - no LLM needed
        SIMPLE_QA,      // Basic questions - can use templates
        FINANCIAL_ACTION, // Needs data engine
        NEEDS_REASONING  // Complex query - needs LLM
    }

    data class RouteDecision(
        val type: RouteType,
        val fastResponse: String? = null, // Pre-made response for fast path
        val confidence: Float = 1.0f
    )

    private val greetings = setOf(
        "hello", "hi", "hey", "hola", "bonjour", "ciao", "namaste", "salaam",
        "good morning", "good afternoon", "good evening", "howdy", "greetings",
        "jambo", "habari", "mambo", "salut", "ola", "konnichiwa", "안녕", "你好"
    )

    private val thanks = setOf(
        "thanks", "thank you", "thx", "merci", "gracias", "danke", "asante",
        "شكرا", "谢谢", "ありがとう", "감사합니다", "obrigado", "grazie", "спасибо"
    )

    private val farewells = setOf(
        "bye", "goodbye", "see you", "farewell", "adios", "au revoir", "ciao",
        "kwaheri", "sayonara", "до свидания", "再见", "안녕히 가세요", "tchau"
    )

    // Financial action keywords
    private val financialKeywords = setOf(
        "save", "saving", "savings", "spend", "spending", "budget", "afford",
        "buy", "purchase", "income", "expense", "bill", "loan", "goal",
        "money", "cash", "cost", "price", "pay", "payment", "invest", "investment"
    )

    // Question indicators
    private val questionWords = setOf(
        "can", "should", "will", "would", "how", "what", "when", "where", "why",
        "is", "are", "am", "do", "does", "did"
    )

    /**
     * Route incoming user input to appropriate processing path
     */
    fun route(input: String): RouteDecision {
        val normalized = input.lowercase().trim()

        Log.d("LightweightRouter", "Routing: '$input'")

        // Fast path: Greetings
        if (containsAny(normalized, greetings)) {
            return RouteDecision(
                type = RouteType.FAST_PATH,
                fastResponse = "Hello! I'm Budgie, your financial assistant. How can I help you today?",
                confidence = 1.0f
            )
        }

        // Fast path: Thanks
        if (containsAny(normalized, thanks)) {
            return RouteDecision(
                type = RouteType.FAST_PATH,
                fastResponse = "You're welcome! Is there anything else I can help you with?",
                confidence = 1.0f
            )
        }

        // Fast path: Farewells
        if (containsAny(normalized, farewells)) {
            return RouteDecision(
                type = RouteType.FAST_PATH,
                fastResponse = "Goodbye! Have a great day ahead!",
                confidence = 1.0f
            )
        }

        // Check if it's a financial query
        val hasFinancialKeyword = containsAny(normalized, financialKeywords)
        val isQuestion = containsAny(normalized, questionWords) || normalized.contains("?")

        // Financial action or query
        if (hasFinancialKeyword) {
            return if (isQuestion && (normalized.contains("can i") || normalized.contains("should i") ||
                                     normalized.contains("how much") || normalized.contains("will i"))) {
                // Complex financial prediction/advice - needs reasoning
                RouteDecision(
                    type = RouteType.NEEDS_REASONING,
                    confidence = 0.9f
                )
            } else {
                // Simple financial query - data engine can handle
                RouteDecision(
                    type = RouteType.FINANCIAL_ACTION,
                    confidence = 0.85f
                )
            }
        }

        // Simple Q&A - can use templates
        if (isQuestion && normalized.length < 50) {
            return RouteDecision(
                type = RouteType.SIMPLE_QA,
                confidence = 0.7f
            )
        }

        // Default: needs reasoning
        return RouteDecision(
            type = RouteType.NEEDS_REASONING,
            confidence = 0.6f
        )
    }

    private fun containsAny(text: String, keywords: Set<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
}

