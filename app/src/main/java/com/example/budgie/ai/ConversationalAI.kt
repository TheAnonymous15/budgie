package com.example.budgie.ai

import android.content.Context
import com.example.budgie.ai.ml.OnDeviceAIEngine
import com.example.budgie.ai.ml.Priority
import com.example.budgie.ai.ml.SpendingTrend
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.preferences.UserPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * Conversational AI Financial Advisor
 * Powered by on-device ML models (runs 100% offline)
 */
class ConversationalAI(context: Context) {

    private val database = BudgieDatabase.getDatabase(context)
    private val onDeviceAI = OnDeviceAIEngine.getInstance(context)
    private val preferencesManager = UserPreferencesManager.getInstance(context)

    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())

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
     * Process user message and generate AI response
     */
    suspend fun processMessage(userMessage: String): ChatMessage = withContext(Dispatchers.Default) {
        val intent = detectIntent(userMessage.lowercase())
        val response = generateResponse(intent, userMessage)
        val suggestions = generateSuggestions(intent)

        ChatMessage(
            id = UUID.randomUUID().toString(),
            content = response,
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            suggestions = suggestions
        )
    }

    private fun detectIntent(message: String): Intent {
        return when {
            message.contains(Regex("hello|hi|hey|good morning|good afternoon|good evening")) -> Intent.GREETING
            message.contains(Regex("thank|thanks|appreciate")) -> Intent.THANKS
            message.contains(Regex("how much.*spent|spending|expenses|total.*expenses")) -> Intent.SPENDING_QUERY
            message.contains(Regex("savings|saved|saving rate")) -> Intent.SAVINGS_QUERY
            message.contains(Regex("budget|budgets")) -> Intent.BUDGET_QUERY
            message.contains(Regex("bills?|due|payment")) -> Intent.BILL_QUERY
            message.contains(Regex("advice|suggest|recommend|tips|help.*save|improve")) -> Intent.ADVICE_REQUEST
            message.contains(Regex("predict|forecast|next month|future")) -> Intent.PREDICTION_QUERY
            message.contains(Regex("health|score|status|how.*doing")) -> Intent.HEALTH_SCORE
            message.contains(Regex("compare|vs|last month|trend")) -> Intent.COMPARISON
            else -> Intent.GENERAL
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun generateResponse(intent: Intent, originalMessage: String): String {
        val userProfile = preferencesManager.userProfile.first()
        val userName = userProfile?.name ?: "there"

        return when (intent) {
            Intent.GREETING -> generateGreeting(userName)
            Intent.THANKS -> "You're welcome, $userName! I'm here anytime you need financial guidance. 💪"
            Intent.SPENDING_QUERY -> generateSpendingResponse()
            Intent.SAVINGS_QUERY -> generateSavingsResponse()
            Intent.BUDGET_QUERY -> generateBudgetResponse()
            Intent.BILL_QUERY -> generateBillResponse()
            Intent.ADVICE_REQUEST -> generateAdviceResponse()
            Intent.PREDICTION_QUERY -> generatePredictionResponse()
            Intent.HEALTH_SCORE -> generateHealthScoreResponse()
            Intent.COMPARISON -> generateComparisonResponse()
            Intent.GENERAL -> generateGeneralResponse(userName)
        }
    }

    private fun generateGreeting(userName: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        return "$greeting, $userName! 👋 I'm your AI financial advisor. How can I help you today?"
    }

    private suspend fun generateSpendingResponse(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val expenses = database.expenseDao().getExpensesByDateRange(monthStart, monthEnd).first()
        val total = expenses.sumOf { it.amount }
        val count = expenses.size

        if (count == 0) {
            return "📝 No spending recorded this month. Don't forget to log your expenses!"
        }

        val topCategories = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .entries.sortedByDescending { it.value }
            .take(3)

        val breakdown = topCategories.joinToString("\n") {
            "• ${it.key.displayName}: $${String.format(Locale.US, "%.2f", it.value)}"
        }

        return """
💰 **Spending Summary for ${monthFormat.format(Date())}:**

Total: **$${String.format(Locale.US, "%.2f", total)}** ($count transactions)

📊 **Top Categories:**
$breakdown

💡 Would you like tips on reducing spending?
        """.trimIndent()
    }

    private suspend fun generateSavingsResponse(): String {
        val analysis = onDeviceAI.generateComprehensiveAnalysis()
        val savingsRate = analysis.savingsRate
        val monthlySavings = analysis.monthlyIncome - analysis.monthlySpending

        val emoji = when {
            savingsRate >= 0.30 -> "🌟"
            savingsRate >= 0.20 -> "✅"
            savingsRate >= 0.10 -> "📊"
            else -> "⚠️"
        }

        return """
$emoji **Your Savings Analysis (AI-Powered):**

📈 Savings Rate: **${(savingsRate * 100).toInt()}%**
💵 Monthly Savings: **$${String.format(Locale.US, "%.2f", monthlySavings)}**
🎯 Health Score: **${analysis.healthScore.overall}/100**

💡 **AI Recommendations:**
• ${analysis.budgetRecommendation.insights.firstOrNull()?.message ?: "Keep up the good work!"}
• Potential extra savings: $${String.format(Locale.US, "%.0f", analysis.budgetRecommendation.potentialMonthlySavings)}/month

Would you like detailed budget optimization?
        """.trimIndent()
    }

    private suspend fun generateBudgetResponse(): String {
        val budgets = database.budgetDao().getAllBudgets().first()

        if (budgets.isEmpty()) {
            return """
📋 You haven't set up any budgets yet!

💡 **I recommend starting with:**
• Food: 15-20% of income
• Transport: 10-15% of income
• Entertainment: 5-10% of income

Would you like help setting up your budgets?
            """.trimIndent()
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val expenses = database.expenseDao().getExpensesByDateRange(monthStart, monthEnd).first()

        val budgetStatus = budgets.map { budget ->
            val spent = expenses.filter { it.category == budget.category }.sumOf { it.amount }
            val percentage = if (budget.limit > 0) (spent / budget.limit * 100).toInt() else 0
            val statusEmoji = when {
                percentage >= 100 -> "🔴"
                percentage >= 80 -> "🟡"
                else -> "🟢"
            }
            "$statusEmoji ${budget.category.displayName}: $${String.format(Locale.US, "%.0f", spent)}/$${String.format(Locale.US, "%.0f", budget.limit)} ($percentage%)"
        }.joinToString("\n")

        return """
📊 **Budget Status for ${monthFormat.format(Date())}:**

$budgetStatus

💡 Categories over 80% are highlighted.
        """.trimIndent()
    }

    private suspend fun generateBillResponse(): String {
        val bills = database.billDao().getAllBills().first()
        val today = System.currentTimeMillis()

        val upcomingBills = bills.filter { !it.isPaid }.sortedBy { it.dueDate }.take(5)

        if (upcomingBills.isEmpty()) {
            return "✅ Great news! You have no pending bills."
        }

        val billList = upcomingBills.map { bill ->
            val daysUntil = ((bill.dueDate - today) / (1000 * 60 * 60 * 24)).toInt()
            val urgency = when {
                daysUntil < 0 -> "🔴 OVERDUE"
                daysUntil == 0 -> "🟠 Due Today"
                daysUntil <= 3 -> "🟡 Due Soon"
                else -> "🟢 Upcoming"
            }
            "$urgency ${bill.title}: $${String.format(Locale.US, "%.2f", bill.amount)} - ${dateFormat.format(Date(bill.dueDate))}"
        }.joinToString("\n")

        val totalDue = upcomingBills.sumOf { it.amount }

        return """
📅 **Upcoming Bills:**

$billList

💰 **Total Due:** $${String.format(Locale.US, "%.2f", totalDue)}
        """.trimIndent()
    }

    private suspend fun generateAdviceResponse(): String {
        val analysis = onDeviceAI.generateComprehensiveAnalysis()
        val topInsights = analysis.insights.filter { it.actionable }.take(3)

        val advice = topInsights.mapIndexed { index, insight ->
            "**${index + 1}. ${insight.title}** (${insight.priority} priority)\n${insight.description}"
        }.joinToString("\n\n")

        val budgetAdvice = analysis.budgetRecommendation.insights.take(2)
            .joinToString("\n") { "• ${it.title}: ${it.message}" }

        return """
🎯 **AI-Powered Financial Advice:**

$advice

📊 **Budget Optimization:**
$budgetAdvice

🤖 *Powered by on-device ML - 100% private*

Would you like more details on any of these?
        """.trimIndent()
    }

    private suspend fun generatePredictionResponse(): String {
        val analysis = onDeviceAI.generateComprehensiveAnalysis()
        val prediction = analysis.spendingPrediction

        val trend = when (prediction.trend) {
            SpendingTrend.INCREASING -> "📈 increasing"
            SpendingTrend.DECREASING -> "📉 decreasing"
            SpendingTrend.STABLE -> "➡️ stable"
        }

        val predictedSavings = analysis.monthlyIncome - prediction.predictedAmount

        return """
🔮 **AI Financial Predictions:**

📊 **Next Month Forecast (ML-Powered):**
• Predicted Expenses: **$${String.format(Locale.US, "%.0f", prediction.predictedAmount)}** (${(prediction.confidence * 100).toInt()}% confidence)
• Current Income: **$${String.format(Locale.US, "%.0f", analysis.monthlyIncome)}**
• Estimated Savings: **$${String.format(Locale.US, "%.0f", predictedSavings)}**

📈 **Trend:** Your spending is $trend

🤖 *TensorFlow Lite prediction - runs offline*

💡 Based on your last 6 months of data patterns.
        """.trimIndent()
    }

    private suspend fun generateHealthScoreResponse(): String {
        val analysis = onDeviceAI.generateComprehensiveAnalysis()
        val score = analysis.healthScore

        val scoreEmoji = when (score.status) {
            com.example.budgie.ai.ml.OverallStatus.EXCELLENT -> "🌟"
            com.example.budgie.ai.ml.OverallStatus.GOOD -> "✅"
            com.example.budgie.ai.ml.OverallStatus.FAIR -> "📊"
            com.example.budgie.ai.ml.OverallStatus.NEEDS_WORK -> "⚠️"
        }

        val factorBreakdown = score.components.map { component ->
            val statusEmoji = when (component.status) {
                com.example.budgie.ai.ml.ComponentStatus.EXCELLENT -> "🟢"
                com.example.budgie.ai.ml.ComponentStatus.GOOD -> "🟡"
                com.example.budgie.ai.ml.ComponentStatus.FAIR -> "🟠"
                com.example.budgie.ai.ml.ComponentStatus.POOR -> "🔴"
            }
            "$statusEmoji ${component.name}: ${component.score.toInt()}/${component.maxScore.toInt()}"
        }.joinToString("\n")

        return """
$scoreEmoji **Your Financial Health Score: ${score.overall}/100**
Status: ${score.status.name.replace("_", " ")}

📊 **Score Breakdown (AI Analysis):**
$factorBreakdown

💰 **Key Metrics:**
• Monthly Income: $${String.format(Locale.US, "%.2f", analysis.monthlyIncome)}
• Monthly Expenses: $${String.format(Locale.US, "%.2f", analysis.monthlySpending)}
• Savings Rate: ${(analysis.savingsRate * 100).toInt()}%

🤖 *Powered by on-device ML models*

Would you like advice on improving your score?
        """.trimIndent()
    }

    private suspend fun generateComparisonResponse(): String {
        val calendar = Calendar.getInstance()

        // This month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val thisMonthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val thisMonthEnd = calendar.timeInMillis

        // Last month
        calendar.add(Calendar.MONTH, -2)
        val lastMonthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val lastMonthEnd = calendar.timeInMillis

        val thisMonthExpenses = database.expenseDao().getExpensesByDateRange(thisMonthStart, thisMonthEnd).first()
        val lastMonthExpenses = database.expenseDao().getExpensesByDateRange(lastMonthStart, lastMonthEnd).first()

        val thisTotal = thisMonthExpenses.sumOf { it.amount }
        val lastTotal = lastMonthExpenses.sumOf { it.amount }
        val difference = thisTotal - lastTotal
        val percentChange = if (lastTotal > 0) ((difference / lastTotal) * 100) else 0.0

        val changeEmoji = if (difference > 0) "📈" else "📉"
        val changeWord = if (difference > 0) "more" else "less"

        return """
📊 **Month-over-Month Comparison:**

📅 **This Month:** $${String.format(Locale.US, "%.2f", thisTotal)}
📅 **Last Month:** $${String.format(Locale.US, "%.2f", lastTotal)}

$changeEmoji **Change:** $${String.format(Locale.US, "%.2f", abs(difference))} $changeWord (${abs(percentChange).toInt()}%)

${if (difference > 0) "⚠️ Spending increased. Review your categories." else "✅ Great job spending less!"}
        """.trimIndent()
    }

    private suspend fun generateGeneralResponse(userName: String): String {
        val analysis = onDeviceAI.generateComprehensiveAnalysis()

        return """
I'm here to help, $userName! 🤖

**Quick Snapshot (AI-Powered):**
• 💰 Health Score: ${analysis.healthScore.overall}/100
• 📊 This month: $${String.format(Locale.US, "%.2f", analysis.monthlySpending)}
• 💵 Savings Rate: ${(analysis.savingsRate * 100).toInt()}%

**I can help with:**
• Spending analysis
• Savings advice
• Budget tracking
• Bill reminders
• ML-powered predictions

🤖 *100% offline - your data never leaves your device*

What would you like to know?
        """.trimIndent()
    }

    private fun generateSuggestions(intent: Intent): List<String> {
        return when (intent) {
            Intent.SPENDING_QUERY -> listOf("Show this week", "Top category?", "How to spend less?")
            Intent.SAVINGS_QUERY -> listOf("How to save more?", "My health score", "Show predictions")
            Intent.BUDGET_QUERY -> listOf("Show all budgets", "Over budget?", "Optimize budgets")
            Intent.BILL_QUERY -> listOf("Due this week?", "Total bills?", "Show spending")
            Intent.HEALTH_SCORE -> listOf("How to improve?", "Show advice", "Compare months")
            else -> listOf("How much did I spend?", "My savings rate?", "Show advice")
        }
    }

    enum class Intent {
        GREETING, THANKS, SPENDING_QUERY, SAVINGS_QUERY, BUDGET_QUERY,
        BILL_QUERY, ADVICE_REQUEST, PREDICTION_QUERY, HEALTH_SCORE,
        COMPARISON, GENERAL
    }
}

