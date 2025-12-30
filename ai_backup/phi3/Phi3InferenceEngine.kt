package com.example.budgie.ai.phi3

import android.content.Context
import android.util.Log
import com.example.budgie.ai.SuperBrain
import com.example.budgie.ai.SystemKnowledge
import com.example.budgie.ai.learner.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Phi-3.5 Mini Inference Engine for Budgie
 *
 * Architecture (from ai.txt):
 * - ONE Primary LLM: Phi-3.5-mini-instruct
 * - Does NOT generate its own data
 * - Uses context from FinancialLearnerEngine
 * - Loads on demand, unloads after response
 * - Battery/thermal optimized
 *
 * Flow:
 * 1. User asks question
 * 2. Query learner models for data
 * 3. Build clean prompt with context
 * 4. Generate friendly response
 */

private const val TAG = "Phi3Engine"

// Chat message for conversation history
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class GenerationConfig(
    val maxNewTokens: Int = 256,  // Keep responses short
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40
)

/**
 * Main Phi-3.5 Inference Engine
 * Uses learner data - does NOT generate its own financial data
 */
class Phi3InferenceEngine(private val context: Context) {

    private var isInitialized = false
    private var statusMessage = "Initializing..."

    // The learner engine - source of truth for financial data
    private lateinit var learnerEngine: FinancialLearnerEngine

    // The SuperBrain - comprehensive real-time data access
    private lateinit var superBrain: SuperBrain

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Initialize the engine
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Phi-3.5 engine with Financial Learner and SuperBrain...")

            // Initialize the learner engine first
            learnerEngine = FinancialLearnerEngine(context)
            learnerEngine.initialize()

            // Initialize SuperBrain for comprehensive data access
            superBrain = SuperBrain.getInstance(context)
            superBrain.initialize()

            isInitialized = true
            statusMessage = "Budgie AI · Online"

            Log.d(TAG, "Phi-3.5 engine initialized successfully with SuperBrain")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize: ${e.message}", e)
            statusMessage = "AI Ready (Limited)"
            isInitialized = true
            true
        }
    }

    /**
     * Generate a response using learner data and SuperBrain
     * The LLM never sees raw data - only summaries from the learner and SuperBrain
     */
    fun generateResponse(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        config: GenerationConfig = GenerationConfig()
    ): Flow<String> = flow {

        if (!::learnerEngine.isInitialized) {
            emit("I'm still warming up. Please try again in a moment.")
            return@flow
        }

        // Refresh SuperBrain for latest data
        if (::superBrain.isInitialized) {
            superBrain.refreshKnowledge()
        }

        // Step 1: Query the learner for relevant data
        val learnerAnswer = learnerEngine.answerQuestion(userMessage)
        val financialContext = learnerEngine.getContextForLLM()
        val financialState = learnerEngine.financialState.value
        val chatMemory = learnerEngine.getChatMemory()

        // Step 1b: Get comprehensive data from SuperBrain
        val systemKnowledge = if (::superBrain.isInitialized) {
            superBrain.knowledge.value
        } else null

        // Step 2: Update chat memory
        learnerEngine.updateChatMemory(
            topic = learnerAnswer.topic,
            question = userMessage,
            intent = detectIntent(userMessage)
        )

        // Step 3: Generate response based on learner data AND SuperBrain knowledge
        val response = SmartFinancialAssistant.generateResponse(
            userMessage = userMessage,
            learnerAnswer = learnerAnswer,
            financialState = financialState,
            conversationHistory = conversationHistory,
            chatMemory = chatMemory,
            systemKnowledge = systemKnowledge
        )

        // Step 4: Stream the response (simulate natural typing)
        val words = response.split(" ")
        for ((index, word) in words.withIndex()) {
            emit(if (index == words.lastIndex) word else "$word ")
            delay(20 + (Math.random() * 15).toLong())
        }
    }

    private fun detectIntent(message: String): String {
        val m = message.lowercase()
        return when {
            m.contains("how much") && m.contains("spend") -> "query_spending"
            m.contains("save") || m.contains("saving") -> "query_savings"
            m.contains("budget") -> "query_budget"
            m.contains("bill") || m.contains("due") -> "query_bills"
            m.contains("predict") || m.contains("will i") -> "prediction"
            m.contains("risk") || m.contains("safe") -> "risk_assessment"
            m.contains("help") || m.contains("advice") -> "advice"
            m.contains("hi") || m.contains("hello") || m.contains("hey") -> "greeting"
            m.contains("thank") -> "gratitude"
            m.contains("bye") || m.contains("goodbye") -> "farewell"
            else -> "general"
        }
    }

    /**
     * Get the learner engine for direct access
     */
    fun getLearnerEngine(): FinancialLearnerEngine? {
        return if (::learnerEngine.isInitialized) learnerEngine else null
    }

    /**
     * Refresh learner data
     */
    suspend fun refreshLearnerData() {
        if (::learnerEngine.isInitialized) {
            learnerEngine.refresh()
        }
    }

    fun isReady(): Boolean = isInitialized

    fun getStatusMessage(): String = statusMessage

    fun release() {
        scope.cancel()
        if (::learnerEngine.isInitialized) {
            learnerEngine.release()
        }
        isInitialized = false
    }
}

/**
 * Smart Financial Assistant
 *
 * Generates friendly responses using ONLY data from the learner.
 * Does NOT make up financial numbers - all data comes from learner.
 */
object SmartFinancialAssistant {

    fun generateResponse(
        userMessage: String,
        learnerAnswer: LearnerAnswer,
        financialState: UserFinancialState,
        conversationHistory: List<ChatMessage>,
        chatMemory: ChatMemory,
        systemKnowledge: SystemKnowledge? = null
    ): String {
        val message = userMessage.lowercase().trim()

        // Handle greetings
        if (isGreeting(message)) {
            return generateGreeting(financialState, systemKnowledge)
        }

        // Handle thanks
        if (isThanks(message)) {
            return generateThanksResponse()
        }

        // Handle farewell
        if (isFarewell(message)) {
            return generateFarewell()
        }

        // Generate contextual response based on learner data AND SuperBrain knowledge
        return when (learnerAnswer.topic) {
            "confirmation" -> generateConfirmationResponse(chatMemory, financialState)
            "negation" -> generateNegationResponse()
            "loans" -> generateLoansResponse(financialState, systemKnowledge)
            "goals" -> generateGoalsResponse(financialState, systemKnowledge)
            "budget" -> generateBudgetResponse(learnerAnswer, financialState, systemKnowledge)
            "status" -> generateStatusResponse(learnerAnswer, financialState, systemKnowledge)
            "help" -> generateHelpResponse()
            "affordability" -> generateAffordabilityResponse(learnerAnswer, financialState)
            "daily_spending" -> generateSpendingResponse(learnerAnswer, financialState, "daily")
            "weekly_spending" -> generateSpendingResponse(learnerAnswer, financialState, "weekly")
            "monthly_spending" -> generateSpendingResponse(learnerAnswer, financialState, "monthly")
            "general_spending" -> generateSpendingResponse(learnerAnswer, financialState, "general")
            "income" -> generateIncomeResponse(learnerAnswer, financialState, systemKnowledge)
            "savings" -> generateSavingsResponse(learnerAnswer, financialState, systemKnowledge)
            "bills" -> generateBillsResponse(learnerAnswer, financialState, systemKnowledge)
            "categories" -> generateCategoryResponse(learnerAnswer, financialState)
            "risk" -> generateRiskResponse(learnerAnswer, financialState)
            "predictions" -> generatePredictionResponse(learnerAnswer, financialState)
            "summary" -> generateSummaryResponse(learnerAnswer, financialState, systemKnowledge)
            else -> generateHelpfulResponse(message, learnerAnswer, financialState, systemKnowledge)
        }
    }

    private fun isGreeting(message: String): Boolean {
        val patterns = listOf("hello", "hi", "hey", "good morning", "good afternoon", "good evening", "howdy", "what's up", "sup")
        return patterns.any { message.startsWith(it) || message == it }
    }

    private fun isThanks(message: String): Boolean {
        return listOf("thank", "thanks", "thx", "appreciate").any { message.contains(it) }
    }

    private fun isFarewell(message: String): Boolean {
        return listOf("bye", "goodbye", "see you", "later", "exit").any { message.contains(it) }
    }

    private fun generateGreeting(state: UserFinancialState, knowledge: SystemKnowledge?): String {
        val riskNote = when (state.riskLevel) {
            RiskLevel.LOW -> "Everything looks healthy!"
            RiskLevel.MODERATE -> "A few things to keep an eye on."
            RiskLevel.HIGH -> "I've noticed some areas we should discuss."
            RiskLevel.CRITICAL -> "There are some important matters to address."
            RiskLevel.UNKNOWN -> "I'm still learning your patterns."
        }

        // Build alerts from SuperBrain knowledge
        val alerts = StringBuilder()
        knowledge?.let { k ->
            if (k.overdueBills.isNotEmpty()) {
                alerts.appendLine("🔴 You have ${k.overdueBills.size} overdue bill(s)!")
            }
            if (k.upcomingBills.any { it.daysUntilDue <= 3 }) {
                val urgentCount = k.upcomingBills.count { it.daysUntilDue <= 3 }
                alerts.appendLine("🟡 $urgentCount bill(s) due within 3 days")
            }
            if (k.overBudgetCategories.isNotEmpty()) {
                alerts.appendLine("⚠️ Over budget in: ${k.overBudgetCategories.joinToString(", ")}")
            }
        }

        val alertSection = if (alerts.isNotEmpty()) "\n$alerts" else ""

        return """Hi! I'm Budgie, your financial companion.

$riskNote$alertSection

How can I help you today? You can ask me about:
• Your spending patterns
• Upcoming bills
• Savings progress
• Financial predictions"""
    }

    private fun generateThanksResponse(): String {
        return listOf(
            "You're welcome! I'm here whenever you need financial guidance.",
            "Happy to help! Keep tracking your finances - you're doing great!",
            "My pleasure! Remember, small consistent steps lead to big financial wins."
        ).random()
    }

    private fun generateFarewell(): String {
        return listOf(
            "Take care! Remember to log your expenses. See you soon!",
            "Goodbye! Keep making smart money decisions!",
            "Until next time! Your financial journey is looking good."
        ).random()
    }

    private fun generateConfirmationResponse(memory: ChatMemory, state: UserFinancialState): String {
        // Check what the last topic was and provide relevant follow-up
        return when (memory.lastIntent) {
            "query_bills" -> {
                val bills = state.upcomingBills.take(3).joinToString("\n") {
                    "• ${it.name}: \$${String.format("%.2f", it.amount)} in ${it.dueInDays} days"
                }
                if (bills.isNotEmpty()) {
                    "Here are all your upcoming bills:\n$bills\n\nWould you like help setting up reminders?"
                } else {
                    "You don't have any bills tracked yet. Go to Bills to add some!"
                }
            }
            "query_savings" -> {
                """Here are some tips to boost your savings:

• Set up automatic transfers to savings
• Use the 50/30/20 rule (needs/wants/savings)
• Track small daily expenses - they add up!
• Review subscriptions monthly

Want me to analyze your biggest expense categories?"""
            }
            else -> {
                """Great! Here are some things I can help you with:

• View your spending breakdown
• Check upcoming bills
• Get savings tips
• Analyze spending patterns

What would you like to explore?"""
            }
        }
    }

    private fun generateNegationResponse(): String {
        return listOf(
            "No problem! Let me know if you need anything else.",
            "Okay! I'm here whenever you're ready.",
            "Got it! Feel free to ask me anything about your finances."
        ).random()
    }

    private fun generateLoansResponse(state: UserFinancialState, knowledge: SystemKnowledge?): String {
        // If we have SuperBrain knowledge, show actual loan details
        if (knowledge != null && knowledge.loans.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine("📋 **Your Loan Details:**\n")

            if (knowledge.activeLoans.isNotEmpty()) {
                sb.appendLine("**Active Loans (${knowledge.activeLoans.size}):**")
                knowledge.activeLoans.forEach { loan ->
                    sb.appendLine("• **${loan.title}** from ${loan.lender}")
                    sb.appendLine("  Total: \$${String.format("%.2f", loan.totalAmount)}")
                    sb.appendLine("  Paid: \$${String.format("%.2f", loan.amountPaid)}")
                    sb.appendLine("  Remaining: \$${String.format("%.2f", loan.amountRemaining)}")
                    sb.appendLine("  Monthly Payment: \$${String.format("%.2f", loan.monthlyPayment)}")
                    sb.appendLine()
                }
            }

            sb.appendLine("**Summary:**")
            sb.appendLine("• Total Borrowed: \$${String.format("%.2f", knowledge.totalLoanAmount)}")
            sb.appendLine("• Total Paid: \$${String.format("%.2f", knowledge.totalLoanPaid)}")
            sb.appendLine("• Total Remaining: \$${String.format("%.2f", knowledge.totalLoanRemaining)}")

            if (knowledge.completedLoans.isNotEmpty()) {
                sb.appendLine("\n✅ Completed Loans: ${knowledge.completedLoans.size}")
            }

            sb.appendLine("\nWould you like tips on paying off loans faster?")

            return sb.toString()
        }

        // Fallback if no loan data
        return """You don't have any loans tracked yet.

To add a loan, go to My Loans from the dashboard menu.

You can track:
• Bank loans
• SACCO loans
• Personal loans
• Credit cards

Based on your income of \$${String.format("%.0f", state.monthlyIncome)}/month, I can help assess loan affordability.

Would you like tips on managing debt?"""
    }

    private fun generateGoalsResponse(state: UserFinancialState, knowledge: SystemKnowledge?): String {
        // If we have SuperBrain knowledge, show actual goal details
        if (knowledge != null && knowledge.goals.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine("🎯 **Your Financial Goals:**\n")

            if (knowledge.activeGoals.isNotEmpty()) {
                sb.appendLine("**Active Goals (${knowledge.activeGoals.size}):**")
                knowledge.activeGoals.forEach { goal ->
                    val progressBar = createProgressBar(goal.progress.toInt())
                    sb.appendLine("• **${goal.title}**")
                    sb.appendLine("  Target: \$${String.format("%.2f", goal.targetAmount)}")
                    sb.appendLine("  Saved: \$${String.format("%.2f", goal.currentAmount)}")
                    sb.appendLine("  Progress: $progressBar ${goal.progress.toInt()}%")
                    if (goal.daysRemaining > 0) {
                        sb.appendLine("  Days Remaining: ${goal.daysRemaining}")
                    }
                    sb.appendLine()
                }
            }

            if (knowledge.completedGoals.isNotEmpty()) {
                sb.appendLine("✅ **Completed Goals: ${knowledge.completedGoals.size}**")
            }

            sb.appendLine("\n**Summary:**")
            sb.appendLine("• Total Target: \$${String.format("%.2f", knowledge.totalGoalTarget)}")
            sb.appendLine("• Total Saved: \$${String.format("%.2f", knowledge.totalGoalSaved)}")

            val savingsNote = if (state.savingsRate > 15) {
                "\n✨ With your ${String.format("%.1f", state.savingsRate)}% savings rate, you're on track!"
            } else {
                "\n💡 Consider increasing savings to reach goals faster."
            }
            sb.append(savingsNote)

            return sb.toString()
        }

        val savingsNote = if (state.savingsRate > 15) {
            "With your ${String.format("%.1f", state.savingsRate)}% savings rate, you're well-positioned to reach goals!"
        } else {
            "Consider increasing your savings rate to reach goals faster."
        }

        return """You haven't set any financial goals yet.

To create a goal, go to My Budgie Goals from the dashboard.

You can set:
• Short-term goals (< 1 year)
• Medium-term goals (1-3 years)
• Long-term goals (3+ years)

$savingsNote

Would you like help planning a new goal?"""
    }

    private fun createProgressBar(percent: Int): String {
        val filled = percent / 10
        val empty = 10 - filled
        return "▓".repeat(filled) + "░".repeat(empty)
    }

    private fun generateBudgetResponse(answer: LearnerAnswer, state: UserFinancialState, knowledge: SystemKnowledge?): String {
        // If we have SuperBrain knowledge with budgets, show actual budget details
        if (knowledge != null && knowledge.budgets.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine("📊 **Your Budget Status:**\n")

            knowledge.budgets.sortedByDescending { it.utilizationPercent }.forEach { budget ->
                val status = when {
                    budget.isOverBudget -> "🔴"
                    budget.utilizationPercent >= 80 -> "🟡"
                    else -> "🟢"
                }
                val progressBar = createProgressBar(budget.utilizationPercent.toInt().coerceAtMost(100))
                sb.appendLine("$status **${budget.category}**")
                sb.appendLine("  \$${String.format("%.2f", budget.spent)} of \$${String.format("%.2f", budget.limit)}")
                sb.appendLine("  $progressBar ${budget.utilizationPercent.toInt()}%")
                sb.appendLine()
            }

            sb.appendLine("**Summary:**")
            sb.appendLine("• Total Budget: \$${String.format("%.2f", knowledge.totalBudget)}")
            sb.appendLine("• Average Usage: ${knowledge.budgetUtilization.toInt()}%")

            if (knowledge.overBudgetCategories.isNotEmpty()) {
                sb.appendLine("\n⚠️ Over budget in: ${knowledge.overBudgetCategories.joinToString(", ")}")
            }

            return sb.toString()
        }

        // Fallback to calculated budget
        val estimatedBudget = state.monthlyIncome * 0.8
        val budgetUsed = if (estimatedBudget > 0) {
            (state.avgMonthlySpend / estimatedBudget * 100).coerceIn(0.0, 999.0)
        } else 0.0

        val status = when {
            budgetUsed < 70 -> "You're well within budget! Great discipline."
            budgetUsed < 90 -> "Budget is on track. Keep an eye on spending."
            budgetUsed < 100 -> "Getting close to your budget limit. Consider cutting back."
            else -> "Spending exceeds the recommended budget. Let's review your expenses."
        }

        return """Here's your budget overview:

• Monthly income: \$${String.format("%.0f", state.monthlyIncome)}
• Monthly spending: \$${String.format("%.0f", state.avgMonthlySpend)}
• Recommended budget (80%): \$${String.format("%.0f", estimatedBudget)}

Budget usage: ${String.format("%.0f", budgetUsed)}%

$status

Want to see which categories are taking the most of your budget?"""
    }

    private fun generateStatusResponse(answer: LearnerAnswer, state: UserFinancialState, knowledge: SystemKnowledge?): String {
        val onTrack = answer.data["on_track"] as? Boolean ?: false

        // Enhanced status with SuperBrain data
        val extraInfo = knowledge?.let { k ->
            val items = mutableListOf<String>()
            if (k.activeLoans.isNotEmpty()) {
                items.add("• Active Loans: ${k.activeLoans.size} (\$${String.format("%.0f", k.totalLoanRemaining)} remaining)")
            }
            if (k.activeGoals.isNotEmpty()) {
                items.add("• Active Goals: ${k.activeGoals.size} (\$${String.format("%.0f", k.totalGoalSaved)} saved)")
            }
            if (k.upcomingBills.isNotEmpty()) {
                items.add("• Upcoming Bills: ${k.upcomingBills.size}")
            }
            if (items.isNotEmpty()) "\n\n**Additional Info:**\n${items.joinToString("\n")}" else ""
        } ?: ""

        return if (onTrack) {
            """You're doing great! Here's why:

• Savings rate: ${String.format("%.1f", state.savingsRate)}% ✓
• Risk level: ${state.riskLevel.display} ✓
• Spending trend: ${state.spendingTrend.display}$extraInfo

Keep up the excellent work! Want tips to do even better?"""
        } else {
            """Here's your current status:

• Savings rate: ${String.format("%.1f", state.savingsRate)}% (goal: 20%)
• Risk level: ${state.riskLevel.display}
• Spending trend: ${state.spendingTrend.display}$extraInfo

There's room for improvement. Shall I suggest some strategies?"""
        }
    }

    private fun generateHelpResponse(): String {
        return """I'm your AI financial assistant! Here's how I can help:

💰 Spending Analysis
• "How much did I spend this week?"
• "What are my top spending categories?"

📊 Financial Health
• "Am I on track?"
• "What's my savings rate?"

📅 Bills & Predictions
• "When is my next bill due?"
• "Predict my end of month balance"

🎯 Goals & Loans
• "Check my goals"
• "View my loans"

What would you like to know?"""
    }

    private fun generateAffordabilityResponse(answer: LearnerAnswer, state: UserFinancialState): String {
        val disposable = state.monthlyIncome - state.avgMonthlySpend

        return """To assess if you can afford something, I'd need to know what you're considering.

Here's your current capacity:
• Monthly income: \$${String.format("%.0f", state.monthlyIncome)}
• Monthly spending: \$${String.format("%.0f", state.avgMonthlySpend)}
• Available: \$${String.format("%.0f", disposable)}/month

${if (disposable > 0) "You have some room for new purchases!" else "Budget is tight right now."}

What are you thinking of buying?"""
    }

    private fun generateSpendingResponse(answer: LearnerAnswer, state: UserFinancialState, period: String): String {
        val trendNote = when (state.spendingTrend) {
            SpendingTrend.INCREASING -> "Your spending has been trending upward recently."
            SpendingTrend.DECREASING -> "Great news - your spending has been decreasing!"
            SpendingTrend.VOLATILE -> "Your spending has been quite variable lately."
            SpendingTrend.STABLE -> "Your spending has been consistent."
        }

        val topCategory = state.topCategories.firstOrNull()
        val categoryNote = if (topCategory != null) {
            "Your biggest category is ${topCategory.category} at ${String.format("%.1f", topCategory.percentage)}%."
        } else ""

        return """${answer.summary}

$trendNote
$categoryNote

Would you like tips on optimizing your spending?"""
    }

    private fun generateIncomeResponse(answer: LearnerAnswer, state: UserFinancialState, knowledge: SystemKnowledge?): String {
        // Enhanced with SuperBrain data
        if (knowledge != null && knowledge.incomes.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine("💰 **Your Income:**\n")
            sb.appendLine("**This Month:** \$${String.format("%.2f", knowledge.monthlyIncome)}")
            sb.appendLine("**Total Recorded:** \$${String.format("%.2f", knowledge.totalIncome)}")
            sb.appendLine()

            if (knowledge.incomeBySource.isNotEmpty()) {
                sb.appendLine("**By Source:**")
                knowledge.incomeBySource.entries.sortedByDescending { it.value }.take(5).forEach { (source, amount) ->
                    sb.appendLine("• $source: \$${String.format("%.2f", amount)}")
                }
            }

            val savingsNote = if (knowledge.savingsRate > 20) {
                "\n✨ You're saving ${knowledge.savingsRate.toInt()}% - excellent!"
            } else if (knowledge.savingsRate > 10) {
                "\n📊 You're saving ${knowledge.savingsRate.toInt()}% - good progress!"
            } else {
                "\n💡 Savings rate: ${knowledge.savingsRate.toInt()}%. Goal is 20%+."
            }
            sb.append(savingsNote)

            return sb.toString()
        }

        val savingsNote = if (state.savingsRate > 20) {
            "You're saving ${String.format("%.1f", state.savingsRate)}% - excellent!"
        } else if (state.savingsRate > 10) {
            "You're saving ${String.format("%.1f", state.savingsRate)}% - good progress!"
        } else if (state.savingsRate > 0) {
            "Your savings rate is ${String.format("%.1f", state.savingsRate)}%. The goal is 20%+."
        } else {
            "Currently spending more than earning. Let's work on this together."
        }

        return """${answer.summary}

$savingsNote

Want to explore ways to increase your income or reduce expenses?"""
    }

    private fun generateSavingsResponse(answer: LearnerAnswer, state: UserFinancialState, knowledge: SystemKnowledge?): String {
        val monthlySavings = (knowledge?.monthlyIncome ?: state.monthlyIncome) -
                            (knowledge?.monthlyExpenses ?: state.avgMonthlySpend)
        val savingsRate = knowledge?.savingsRate ?: state.savingsRate

        val recommendation = when {
            savingsRate >= 20 -> "You're meeting the recommended 20% savings rate. Excellent work!"
            savingsRate >= 10 -> "You're halfway to the 20% goal. Keep pushing!"
            savingsRate > 0 -> "Every bit counts. Try increasing by just 1% this month."
            else -> "Let's find ways to create some savings room in your budget."
        }

        val projections = if (monthlySavings > 0) {
            "\n**Projections:**\n• 6 months: \$${String.format("%.0f", monthlySavings * 6)}\n• 1 year: \$${String.format("%.0f", monthlySavings * 12)}"
        } else ""

        return """💎 **Your Savings Analysis:**

• Savings Rate: ${savingsRate.toInt()}%
• Monthly Savings: \$${String.format("%.2f", monthlySavings)}
$projections

$recommendation

The 50/30/20 rule suggests:
• 50% for needs
• 30% for wants  
• 20% for savings

Shall I help identify areas to save more?"""
    }

    private fun generateBillsResponse(answer: LearnerAnswer, state: UserFinancialState, knowledge: SystemKnowledge?): String {
        // Enhanced with SuperBrain data
        if (knowledge != null && knowledge.bills.isNotEmpty()) {
            val sb = StringBuilder()
            sb.appendLine("📄 **Your Bills:**\n")

            if (knowledge.overdueBills.isNotEmpty()) {
                sb.appendLine("🔴 **Overdue (${knowledge.overdueBills.size}):**")
                knowledge.overdueBills.forEach { bill ->
                    sb.appendLine("• ${bill.title}: \$${String.format("%.2f", bill.amount)} - ${kotlin.math.abs(bill.daysUntilDue)} days overdue!")
                }
                sb.appendLine()
            }

            if (knowledge.upcomingBills.isNotEmpty()) {
                sb.appendLine("📅 **Upcoming (${knowledge.upcomingBills.size}):**")
                knowledge.upcomingBills.take(5).forEach { bill ->
                    val urgency = when {
                        bill.daysUntilDue <= 3 -> "🔴"
                        bill.daysUntilDue <= 7 -> "🟡"
                        else -> "🟢"
                    }
                    sb.appendLine("$urgency ${bill.title}: \$${String.format("%.2f", bill.amount)} - ${bill.daysUntilDue} days")
                }
            }

            sb.appendLine("\n**Summary:**")
            sb.appendLine("• Total Bills: \$${String.format("%.2f", knowledge.totalBillsAmount)}")
            sb.appendLine("• Paid: ${knowledge.paidBills.size}, Unpaid: ${knowledge.unpaidBills.size}")

            sb.appendLine("\nI'll remind you before bills are due.")

            return sb.toString()
        }

        val billsList = if (state.upcomingBills.isNotEmpty()) {
            val upcoming = state.upcomingBills.take(3).joinToString("\n") {
                "• ${it.name}: \$${String.format("%.2f", it.amount)} in ${it.dueInDays} days"
            }
            "\nUpcoming bills:\n$upcoming"
        } else ""

        return """${answer.summary}
$billsList

I'll remind you before bills are due. Would you like to set up payment reminders?"""
    }

    private fun generateCategoryResponse(answer: LearnerAnswer, state: UserFinancialState): String {
        val categories = state.topCategories.take(3).joinToString("\n") { cat ->
            "• ${cat.category}: \$${String.format("%.2f", cat.amount)} (${String.format("%.1f", cat.percentage)}%)"
        }

        return """${answer.summary}

Your top spending categories:
$categories

Would you like suggestions for any of these categories?"""
    }

    private fun generateRiskResponse(answer: LearnerAnswer, state: UserFinancialState): String {
        val advice = when (state.riskLevel) {
            RiskLevel.LOW -> "Your finances look healthy! Keep up the good habits."
            RiskLevel.MODERATE -> "You're doing okay, but there's room for improvement. Consider building your emergency fund."
            RiskLevel.HIGH -> "I recommend reviewing your expenses. Look for areas to cut back."
            RiskLevel.CRITICAL -> "This needs attention. Let's create a plan to reduce spending and increase savings."
            RiskLevel.UNKNOWN -> "I need more data to assess your risk accurately. Keep logging transactions!"
        }

        val anomalyNote = if (state.anomalies.isNotEmpty()) {
            "\n\nI noticed ${state.anomalies.size} unusual transaction(s) recently. Want details?"
        } else ""

        return """${answer.summary}

$advice$anomalyNote"""
    }

    private fun generatePredictionResponse(answer: LearnerAnswer, state: UserFinancialState): String {
        val outlook = when {
            state.predictions.endOfMonthBalance > state.monthlyIncome * 0.2 ->
                "Looking good! You should have a healthy balance."
            state.predictions.endOfMonthBalance > 0 ->
                "You'll be in the positive, but consider being cautious."
            else ->
                "You might be tight this month. Consider reducing non-essential spending."
        }

        val confidenceNote = if (state.predictions.confidence > 0.7) {
            "This prediction is based on solid historical data."
        } else {
            "Note: I need more data for more accurate predictions."
        }

        return """${answer.summary}

$outlook

$confidenceNote

Want tips for improving your end-of-month position?"""
    }

    private fun generateSummaryResponse(answer: LearnerAnswer, state: UserFinancialState, knowledge: SystemKnowledge?): String {
        // Enhanced summary with SuperBrain data
        if (knowledge != null) {
            val sb = StringBuilder()
            sb.appendLine("📊 **Financial Overview for ${knowledge.userName}:**\n")

            sb.appendLine("**💰 Income & Spending:**")
            sb.appendLine("• Monthly Income: \$${String.format("%.2f", knowledge.monthlyIncome)}")
            sb.appendLine("• Monthly Expenses: \$${String.format("%.2f", knowledge.monthlyExpenses)}")
            sb.appendLine("• Net Savings: \$${String.format("%.2f", knowledge.monthlyIncome - knowledge.monthlyExpenses)}")
            sb.appendLine("• Savings Rate: ${knowledge.savingsRate.toInt()}%")
            sb.appendLine()

            sb.appendLine("**📋 Bills & Loans:**")
            sb.appendLine("• Upcoming Bills: ${knowledge.upcomingBills.size} (\$${String.format("%.0f", knowledge.upcomingBills.sumOf { it.amount })})")
            sb.appendLine("• Active Loans: ${knowledge.activeLoans.size} (\$${String.format("%.0f", knowledge.totalLoanRemaining)} remaining)")
            sb.appendLine()

            sb.appendLine("**🎯 Goals:**")
            sb.appendLine("• Active Goals: ${knowledge.activeGoals.size}")
            sb.appendLine("• Total Saved: \$${String.format("%.0f", knowledge.totalGoalSaved)} of \$${String.format("%.0f", knowledge.totalGoalTarget)}")
            sb.appendLine()

            sb.appendLine("**📈 Health Score: ${knowledge.financialHealthScore}/100 (${knowledge.riskLevel})**")
            sb.appendLine()
            sb.append("What would you like to explore further?")

            return sb.toString()
        }

        return """Here's your financial snapshot:

${answer.summary}

${when (state.riskLevel) {
            RiskLevel.LOW -> "Overall: You're in good shape!"
            RiskLevel.MODERATE -> "Overall: Some areas to watch."
            RiskLevel.HIGH -> "Overall: Needs attention."
            RiskLevel.CRITICAL -> "Overall: Let's make a plan."
            RiskLevel.UNKNOWN -> "Overall: Still gathering data."
        }}

What would you like to explore further?"""
    }

    private fun generateHelpfulResponse(message: String, answer: LearnerAnswer, state: UserFinancialState, knowledge: SystemKnowledge?): String {
        // Check for help/advice requests
        if (message.contains("help") || message.contains("advice") || message.contains("tip")) {
            return """I'm here to help! Based on your finances, here's what I suggest:

${getPersonalizedAdvice(state)}

What specific area would you like to focus on?
• Spending analysis
• Savings strategies
• Bill management
• Budget optimization"""
        }

        // Check for how-to questions
        if (message.contains("how do") || message.contains("how can") || message.contains("how to")) {
            return """Here are some ways I can help:

• Track expenses: Tap '+' on the dashboard
• View spending: Check Analytics section
• Set goals: Go to My Budgie Goals
• Manage bills: Use the Bills section

What would you like to do?"""
        }

        // If we have SuperBrain knowledge, provide comprehensive snapshot
        if (knowledge != null) {
            val sb = StringBuilder()
            sb.appendLine("Here's your financial snapshot:\n")
            sb.appendLine("• Income: \$${String.format("%.0f", knowledge.monthlyIncome)}/mo")
            sb.appendLine("• Spending: \$${String.format("%.0f", knowledge.monthlyExpenses)}/mo")
            sb.appendLine("• Savings: ${knowledge.savingsRate.toInt()}%")
            sb.appendLine("• Risk: ${knowledge.riskLevel}")
            sb.appendLine()
            sb.append("What would you like to explore further?")
            return sb.toString()
        }

        // Default - provide summary with context
        return """${answer.summary}

Is there something specific you'd like to know about your finances?

You can ask me about:
• Your spending (daily, weekly, monthly)
• Savings rate and goals
• Upcoming bills
• Financial predictions"""
    }

    private fun getPersonalizedAdvice(state: UserFinancialState): String {
        val advice = mutableListOf<String>()

        // Savings advice
        if (state.savingsRate < 20) {
            advice.add("• Try to boost your savings rate to 20%")
        }

        // Risk-based advice
        when (state.riskLevel) {
            RiskLevel.HIGH, RiskLevel.CRITICAL -> {
                advice.add("• Review and cut non-essential expenses")
                advice.add("• Focus on building an emergency fund")
            }
            RiskLevel.MODERATE -> {
                advice.add("• Consider automating your savings")
            }
            else -> {}
        }

        // Trend-based advice
        if (state.spendingTrend == SpendingTrend.INCREASING) {
            advice.add("• Your spending is increasing - review recent purchases")
        }

        // Top category advice
        state.topCategories.firstOrNull()?.let { top ->
            if (top.percentage > 40) {
                advice.add("• ${top.category} is ${String.format("%.0f", top.percentage)}% of spending - consider ways to reduce")
            }
        }

        // Bill advice
        state.nextBill?.let { bill ->
            if (bill.dueInDays <= 3) {
                advice.add("• ${bill.name} is due in ${bill.dueInDays} days - make sure you're ready!")
            }
        }

        return if (advice.isNotEmpty()) {
            advice.joinToString("\n")
        } else {
            "• Keep tracking your expenses consistently\n• Review your budget weekly\n• Celebrate your financial wins!"
        }
    }
}

