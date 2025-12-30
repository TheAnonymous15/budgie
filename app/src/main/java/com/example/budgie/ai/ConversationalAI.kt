package com.example.budgie.ai

import android.content.Context

/**
 * ConversationalAI - Simple chat interface
 * Provides fallback responses until full AI is integrated
 */
class ConversationalAI(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ConversationalAI? = null

        fun getInstance(context: Context): ConversationalAI {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConversationalAI(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Initialize the AI (stub for now)
     */
    suspend fun initialize() {
        // Will be implemented when full AI is ready
    }

    /**
     * Process a user message and return a response
     */
    suspend fun processMessage(userMessage: String): ChatResponse {
        // Simple fallback response system
        val response = generateFallbackResponse(userMessage)
        return ChatResponse(
            content = response,
            suggestions = generateSuggestions(userMessage)
        )
    }

    private fun generateFallbackResponse(query: String): String {
        val queryLower = query.lowercase()

        return when {
            queryLower.contains("hello") || queryLower.contains("hi") || queryLower.contains("hey") ->
                "Hello! I'm Budgie, your financial companion. How can I help you today?"

            queryLower.contains("how are you") ->
                "I'm doing great, thank you for asking! I'm here to help with your finances."

            queryLower.contains("spend") || queryLower.contains("expense") ->
                "I can help you track your spending. Use the expenses screen to see your recent transactions and spending patterns."

            queryLower.contains("save") || queryLower.contains("saving") ->
                "Great question about savings! The key is to track your expenses and set a monthly savings goal. Start with the 50/30/20 rule: 50% needs, 30% wants, 20% savings."

            queryLower.contains("budget") ->
                "Budgeting is essential for financial health! Go to the Budget screen to set spending limits for different categories."

            queryLower.contains("bill") || queryLower.contains("due") ->
                "Keep track of your bills in the Bills section. You can set reminders to never miss a payment."

            queryLower.contains("income") || queryLower.contains("earn") ->
                "Track all your income sources in the Income section to get a complete picture of your finances."

            queryLower.contains("loan") || queryLower.contains("debt") ->
                "Managing loans is important. Use the Loans section to track your debts and plan your repayments."

            queryLower.contains("goal") ->
                "Setting financial goals is the first step to success! Visit the Goals section to create and track your objectives."

            queryLower.contains("thank") ->
                "You're welcome! Feel free to ask if you have more questions about your finances."

            queryLower.contains("help") ->
                "I can help you with:\n• Tracking expenses and income\n• Setting budgets\n• Managing bills and loans\n• Setting financial goals\n\nJust ask me anything!"

            else ->
                "I understand you're asking about \"$query\". For the best experience, try using the specific sections in the app for expenses, income, bills, budgets, and goals. Is there anything specific I can help you with?"
        }
    }

    private fun generateSuggestions(lastQuery: String): List<String> {
        val query = lastQuery.lowercase()
        return when {
            query.contains("spend") || query.contains("expense") ->
                listOf("My top categories", "Tips to reduce", "Monthly summary")
            query.contains("save") || query.contains("saving") ->
                listOf("How to save more?", "50/30/20 rule", "Set savings goal")
            query.contains("budget") ->
                listOf("Budget status", "Adjust budget", "Budget tips")
            query.contains("bill") || query.contains("due") ->
                listOf("Upcoming bills", "Payment reminders", "Bill strategies")
            query.contains("income") || query.contains("earn") ->
                listOf("Income breakdown", "Savings rate", "Track sources")
            query.contains("loan") || query.contains("debt") ->
                listOf("Pay off faster", "Loan vs savings", "Interest tips")
            query.contains("goal") ->
                listOf("Create goal", "Track progress", "Goal strategies")
            query.contains("hello") || query.contains("hi") || query.contains("hey") ->
                listOf("How am I doing?", "Help me save", "Track spending")
            else ->
                listOf("How am I doing?", "Help me save", "Track spending")
        }
    }
}

/**
 * Response from the AI
 */
data class ChatResponse(
    val content: String,
    val suggestions: List<String> = emptyList()
)

/**
 * Chat message data class (for compatibility)
 */
data class ChatMessage(
    val content: String,
    val suggestions: List<String> = emptyList()
)

