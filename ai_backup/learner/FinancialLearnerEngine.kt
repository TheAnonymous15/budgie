package com.example.budgie.ai.learner

import android.content.Context
import android.util.Log
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.math.*

/**
 * Financial Learner Engine
 *
 * This is the "brain" that learns from user's financial data.
 * It does NOT generate text - it produces structured insights that
 * the chatbot can reference.
 *
 * Architecture based on ai.txt specifications:
 * - Spending clusters (MiniBatch K-Means equivalent)
 * - Bill prediction (LSTM-lite equivalent)
 * - Anomaly detection (Isolation Forest equivalent)
 * - Seasonality detection (Holt-Winters equivalent)
 * - Risk scoring (Logistic regression equivalent)
 *
 * All implemented as lightweight Kotlin algorithms optimized for mobile.
 */

private const val TAG = "FinancialLearner"

/**
 * User's current financial state - structured data for the LLM
 */
data class UserFinancialState(
    val monthlyIncome: Double = 0.0,
    val avgMonthlySpend: Double = 0.0,
    val avgDailySpend: Double = 0.0,
    val totalSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val topCategories: List<CategorySpend> = emptyList(),
    val nextBill: NextBillInfo? = null,
    val upcomingBills: List<NextBillInfo> = emptyList(),
    val riskLevel: RiskLevel = RiskLevel.UNKNOWN,
    val spendingTrend: SpendingTrend = SpendingTrend.STABLE,
    val anomalies: List<SpendingAnomaly> = emptyList(),
    val predictions: FinancialPredictions = FinancialPredictions(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class CategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val trend: SpendingTrend = SpendingTrend.STABLE
)

data class NextBillInfo(
    val name: String,
    val amount: Double,
    val dueInDays: Int,
    val isRecurring: Boolean = true,
    val confidence: Float = 0.8f
)

enum class RiskLevel(val display: String) {
    LOW("Low Risk"),
    MODERATE("Moderate Risk"),
    HIGH("High Risk"),
    CRITICAL("Critical"),
    UNKNOWN("Unknown")
}

enum class SpendingTrend(val display: String) {
    DECREASING("Decreasing"),
    STABLE("Stable"),
    INCREASING("Increasing"),
    VOLATILE("Volatile")
}

data class SpendingAnomaly(
    val description: String,
    val category: String,
    val amount: Double,
    val expectedAmount: Double,
    val severity: AnomalySeverity,
    val date: Long
)

enum class AnomalySeverity { LOW, MEDIUM, HIGH }

data class FinancialPredictions(
    val nextMonthSpend: Double = 0.0,
    val nextWeekSpend: Double = 0.0,
    val endOfMonthBalance: Double = 0.0,
    val savingsIn30Days: Double = 0.0,
    val willExceedBudget: Boolean = false,
    val budgetExceedProbability: Float = 0.0f,
    val confidence: Float = 0.0f
)

/**
 * User preferences for chat responses
 */
data class UserPreferences(
    val tone: ResponseTone = ResponseTone.FRIENDLY,
    val detailLevel: DetailLevel = DetailLevel.MEDIUM,
    val language: String = "en",
    val currency: String = "USD",
    val currencySymbol: String = "$"
)

enum class ResponseTone { FRIENDLY, PROFESSIONAL, CASUAL }
enum class DetailLevel { SHORT, MEDIUM, DETAILED }

/**
 * Short-term chat memory (last 3-5 turns)
 */
data class ChatMemory(
    val recentTopics: MutableList<String> = mutableListOf(),
    val recentQuestions: MutableList<String> = mutableListOf(),
    var lastIntent: String? = null,
    val contextFlags: MutableMap<String, Any> = mutableMapOf()
) {
    fun addTopic(topic: String) {
        recentTopics.add(0, topic)
        if (recentTopics.size > 5) recentTopics.removeLast()
    }

    fun addQuestion(question: String) {
        recentQuestions.add(0, question)
        if (recentQuestions.size > 5) recentQuestions.removeLast()
    }

    fun clear() {
        recentTopics.clear()
        recentQuestions.clear()
        lastIntent = null
        contextFlags.clear()
    }
}

/**
 * Main Financial Learner Engine
 */
class FinancialLearnerEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var database: BudgieDatabase? = null

    // Cached state - updated periodically
    private var _financialState = MutableStateFlow(UserFinancialState())
    val financialState: StateFlow<UserFinancialState> = _financialState.asStateFlow()

    private var _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val chatMemory = ChatMemory()

    // Historical data for learning
    private var expenseHistory: List<ExpenseData> = emptyList()
    private var incomeHistory: List<IncomeData> = emptyList()
    private var billHistory: List<BillData> = emptyList()

    // Learning model states
    private var categoryAverages: Map<String, Double> = emptyMap()
    private var categoryStdDevs: Map<String, Double> = emptyMap()
    private var weekdayPatterns: Map<Int, Double> = emptyMap()
    private var monthlyPatterns: Map<Int, Double> = emptyMap()

    data class ExpenseData(
        val amount: Double,
        val category: String,
        val date: Long,
        val dayOfWeek: Int,
        val dayOfMonth: Int,
        val month: Int
    )

    data class IncomeData(
        val amount: Double,
        val source: String,
        val date: Long,
        val isRecurring: Boolean
    )

    data class BillData(
        val name: String,
        val amount: Double,
        val dueDate: Long,  // Timestamp
        val isPaid: Boolean,
        val isRecurring: Boolean
    )

    /**
     * Initialize the learner - call once on app start
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Financial Learner Engine...")
            database = BudgieDatabase.getDatabase(context)

            // Load historical data
            loadHistoricalData()

            // Run initial learning
            learnPatterns()

            // Update financial state
            updateFinancialState()

            Log.d(TAG, "Financial Learner initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize learner: ${e.message}", e)
        }
    }

    /**
     * Load all historical data from database
     */
    private suspend fun loadHistoricalData() {
        val db = database ?: return
        val calendar = Calendar.getInstance()

        // Load expenses from last 90 days
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)

        try {
            db.expenseDao().getAllExpensesOnce().let { expenses ->
                expenseHistory = expenses.map { expense ->
                    calendar.timeInMillis = expense.date
                    ExpenseData(
                        amount = expense.amount,
                        category = expense.category.displayName,
                        date = expense.date,
                        dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                        month = calendar.get(Calendar.MONTH)
                    )
                }
            }

            db.incomeDao().getAllIncomesOnce().let { incomes ->
                incomeHistory = incomes.map { income ->
                    IncomeData(
                        amount = income.amount,
                        source = income.source.displayName,
                        date = income.date,
                        isRecurring = income.isRecurring
                    )
                }
            }

            db.billDao().getAllBillsOnce().let { bills ->
                billHistory = bills.map { bill ->
                    BillData(
                        name = bill.title,
                        amount = bill.amount,
                        dueDate = bill.dueDate,
                        isPaid = bill.isPaid,
                        isRecurring = bill.isRecurring
                    )
                }
            }

            Log.d(TAG, "Loaded ${expenseHistory.size} expenses, ${incomeHistory.size} incomes, ${billHistory.size} bills")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading historical data: ${e.message}")
        }
    }

    /**
     * Learn patterns from historical data
     * This is the "training" phase - runs once per day or on data change
     */
    private fun learnPatterns() {
        if (expenseHistory.isEmpty()) return

        // 1. Learn category spending patterns
        learnCategoryPatterns()

        // 2. Learn weekday patterns
        learnWeekdayPatterns()

        // 3. Learn monthly patterns
        learnMonthlyPatterns()

        Log.d(TAG, "Patterns learned: ${categoryAverages.size} categories, ${weekdayPatterns.size} weekday patterns")
    }

    /**
     * Learn average spending per category and standard deviations
     * (MiniBatch K-Means equivalent - simplified for mobile)
     */
    private fun learnCategoryPatterns() {
        val categoryExpenses = expenseHistory.groupBy { it.category }

        categoryAverages = categoryExpenses.mapValues { (_, expenses) ->
            expenses.map { it.amount }.average()
        }

        categoryStdDevs = categoryExpenses.mapValues { (_, expenses) ->
            val amounts = expenses.map { it.amount }
            val mean = amounts.average()
            sqrt(amounts.map { (it - mean).pow(2) }.average())
        }
    }

    /**
     * Learn spending patterns by day of week
     */
    private fun learnWeekdayPatterns() {
        weekdayPatterns = expenseHistory
            .groupBy { it.dayOfWeek }
            .mapValues { (_, expenses) -> expenses.map { it.amount }.average() }
    }

    /**
     * Learn spending patterns by day of month
     */
    private fun learnMonthlyPatterns() {
        monthlyPatterns = expenseHistory
            .groupBy { it.dayOfMonth }
            .mapValues { (_, expenses) -> expenses.map { it.amount }.average() }
    }

    /**
     * Update the financial state with latest calculations
     * This produces the structured data that the LLM will reference
     */
    suspend fun updateFinancialState() = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Calculate monthly income
        val monthlyIncome = calculateMonthlyIncome()

        // Calculate spending metrics
        val (avgMonthlySpend, avgDailySpend) = calculateSpendingMetrics()

        // Calculate top categories
        val topCategories = calculateTopCategories()

        // Calculate risk level
        val riskLevel = calculateRiskLevel(monthlyIncome, avgMonthlySpend)

        // Detect spending trend
        val spendingTrend = detectSpendingTrend()

        // Detect anomalies
        val anomalies = detectAnomalies()

        // Get upcoming bills
        val upcomingBills = predictUpcomingBills()

        // Generate predictions
        val predictions = generatePredictions(monthlyIncome, avgMonthlySpend)

        // Calculate savings
        val savingsRate = if (monthlyIncome > 0) {
            ((monthlyIncome - avgMonthlySpend) / monthlyIncome * 100).coerceIn(0.0, 100.0)
        } else 0.0

        _financialState.value = UserFinancialState(
            monthlyIncome = monthlyIncome,
            avgMonthlySpend = avgMonthlySpend,
            avgDailySpend = avgDailySpend,
            totalSavings = monthlyIncome - avgMonthlySpend,
            savingsRate = savingsRate,
            topCategories = topCategories,
            nextBill = upcomingBills.firstOrNull(),
            upcomingBills = upcomingBills,
            riskLevel = riskLevel,
            spendingTrend = spendingTrend,
            anomalies = anomalies,
            predictions = predictions,
            lastUpdated = System.currentTimeMillis()
        )

        Log.d(TAG, "Financial state updated: income=$monthlyIncome, spend=$avgMonthlySpend, risk=$riskLevel")
    }

    private fun calculateMonthlyIncome(): Double {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return incomeHistory
            .filter { it.date >= thirtyDaysAgo }
            .sumOf { it.amount }
    }

    private fun calculateSpendingMetrics(): Pair<Double, Double> {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentExpenses = expenseHistory.filter { it.date >= thirtyDaysAgo }

        val monthlySpend = recentExpenses.sumOf { it.amount }
        val dailySpend = if (recentExpenses.isNotEmpty()) monthlySpend / 30 else 0.0

        return Pair(monthlySpend, dailySpend)
    }

    private fun calculateTopCategories(): List<CategorySpend> {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentExpenses = expenseHistory.filter { it.date >= thirtyDaysAgo }
        val totalSpend = recentExpenses.sumOf { it.amount }

        if (totalSpend == 0.0) return emptyList()

        return recentExpenses
            .groupBy { it.category }
            .map { (category, expenses) ->
                val amount = expenses.sumOf { it.amount }
                CategorySpend(
                    category = category,
                    amount = amount,
                    percentage = (amount / totalSpend * 100),
                    trend = detectCategoryTrend(category)
                )
            }
            .sortedByDescending { it.amount }
            .take(5)
    }

    private fun detectCategoryTrend(category: String): SpendingTrend {
        val categoryExpenses = expenseHistory.filter { it.category == category }
        if (categoryExpenses.size < 4) return SpendingTrend.STABLE

        val recentAvg = categoryExpenses.takeLast(categoryExpenses.size / 2).map { it.amount }.average()
        val olderAvg = categoryExpenses.take(categoryExpenses.size / 2).map { it.amount }.average()

        val changePercent = if (olderAvg > 0) ((recentAvg - olderAvg) / olderAvg * 100) else 0.0

        return when {
            changePercent > 20 -> SpendingTrend.INCREASING
            changePercent < -20 -> SpendingTrend.DECREASING
            else -> SpendingTrend.STABLE
        }
    }

    /**
     * Calculate financial risk level
     * (Logistic regression equivalent)
     */
    private fun calculateRiskLevel(income: Double, spending: Double): RiskLevel {
        if (income == 0.0) return RiskLevel.UNKNOWN

        val spendingRatio = spending / income
        val savingsRatio = 1 - spendingRatio

        // Risk factors
        var riskScore = 0.0

        // Spending ratio factor
        riskScore += when {
            spendingRatio > 1.0 -> 40.0  // Spending more than earning
            spendingRatio > 0.9 -> 25.0  // Very tight
            spendingRatio > 0.8 -> 15.0  // Moderate
            else -> 5.0
        }

        // Savings factor
        riskScore += when {
            savingsRatio < 0 -> 30.0
            savingsRatio < 0.1 -> 20.0
            savingsRatio < 0.2 -> 10.0
            else -> 0.0
        }

        // Volatility factor (if spending varies a lot)
        val stdDev = categoryStdDevs.values.average()
        val avgSpend = categoryAverages.values.average()
        val cv = if (avgSpend > 0) stdDev / avgSpend else 0.0
        riskScore += when {
            cv > 0.5 -> 15.0
            cv > 0.3 -> 8.0
            else -> 0.0
        }

        // Unpaid bills factor
        val unpaidBills = billHistory.count { !it.isPaid }
        riskScore += unpaidBills * 5.0

        return when {
            riskScore >= 60 -> RiskLevel.CRITICAL
            riskScore >= 40 -> RiskLevel.HIGH
            riskScore >= 20 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }
    }

    /**
     * Detect overall spending trend
     * (Holt-Winters equivalent - simplified)
     */
    private fun detectSpendingTrend(): SpendingTrend {
        if (expenseHistory.size < 14) return SpendingTrend.STABLE

        val twoWeeksAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
        val oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)

        val week1Spend = expenseHistory
            .filter { it.date in twoWeeksAgo until oneWeekAgo }
            .sumOf { it.amount }

        val week2Spend = expenseHistory
            .filter { it.date >= oneWeekAgo }
            .sumOf { it.amount }

        if (week1Spend == 0.0) return SpendingTrend.STABLE

        val changePercent = ((week2Spend - week1Spend) / week1Spend * 100)

        // Check for volatility
        val amounts = expenseHistory.takeLast(14).map { it.amount }
        val mean = amounts.average()
        val variance = amounts.map { (it - mean).pow(2) }.average()
        val cv = if (mean > 0) sqrt(variance) / mean else 0.0

        return when {
            cv > 0.7 -> SpendingTrend.VOLATILE
            changePercent > 25 -> SpendingTrend.INCREASING
            changePercent < -25 -> SpendingTrend.DECREASING
            else -> SpendingTrend.STABLE
        }
    }

    /**
     * Detect spending anomalies
     * (Isolation Forest equivalent - using Z-score)
     */
    private fun detectAnomalies(): List<SpendingAnomaly> {
        val anomalies = mutableListOf<SpendingAnomaly>()
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)

        expenseHistory.filter { it.date >= sevenDaysAgo }.forEach { expense ->
            val categoryMean = categoryAverages[expense.category] ?: return@forEach
            val categoryStd = categoryStdDevs[expense.category] ?: return@forEach

            if (categoryStd == 0.0) return@forEach

            // Z-score calculation
            val zScore = abs((expense.amount - categoryMean) / categoryStd)

            if (zScore > 2.0) {  // More than 2 standard deviations
                val severity = when {
                    zScore > 3.0 -> AnomalySeverity.HIGH
                    zScore > 2.5 -> AnomalySeverity.MEDIUM
                    else -> AnomalySeverity.LOW
                }

                anomalies.add(SpendingAnomaly(
                    description = "Unusual ${expense.category} spending",
                    category = expense.category,
                    amount = expense.amount,
                    expectedAmount = categoryMean,
                    severity = severity,
                    date = expense.date
                ))
            }
        }

        return anomalies.sortedByDescending { it.severity }
    }

    /**
     * Predict upcoming bills
     * (LSTM-lite equivalent - pattern matching)
     */
    private fun predictUpcomingBills(): List<NextBillInfo> {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        return billHistory
            .filter { !it.isPaid }
            .map { bill ->
                val daysUntilDue = ((bill.dueDate - now) / oneDay).toInt()

                NextBillInfo(
                    name = bill.name,
                    amount = bill.amount,
                    dueInDays = daysUntilDue.coerceAtLeast(0),
                    isRecurring = bill.isRecurring,
                    confidence = 0.9f
                )
            }
            .filter { it.dueInDays <= 30 }  // Only show bills due in next 30 days
            .sortedBy { it.dueInDays }
    }

    /**
     * Generate financial predictions
     */
    private fun generatePredictions(income: Double, avgSpend: Double): FinancialPredictions {
        val calendar = Calendar.getInstance()
        val daysLeftInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Predict spending for rest of month
        val dailyRate = if (dayOfMonth > 0) avgSpend / 30 else 0.0
        val predictedRemainingSpend = dailyRate * daysLeftInMonth

        // Already spent this month
        val currentMonthStart = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val spentThisMonth = expenseHistory
            .filter { it.date >= currentMonthStart }
            .sumOf { it.amount }

        val predictedMonthlyTotal = spentThisMonth + predictedRemainingSpend
        val predictedEndBalance = income - predictedMonthlyTotal

        // Budget exceed probability
        val budgetLimit = income * 0.8  // Assume 80% budget
        val exceedProbability = when {
            predictedMonthlyTotal > budgetLimit * 1.2 -> 0.9f
            predictedMonthlyTotal > budgetLimit -> 0.7f
            predictedMonthlyTotal > budgetLimit * 0.9 -> 0.4f
            else -> 0.1f
        }

        return FinancialPredictions(
            nextMonthSpend = avgSpend,
            nextWeekSpend = dailyRate * 7,
            endOfMonthBalance = predictedEndBalance,
            savingsIn30Days = (income - avgSpend).coerceAtLeast(0.0),
            willExceedBudget = predictedMonthlyTotal > budgetLimit,
            budgetExceedProbability = exceedProbability,
            confidence = if (expenseHistory.size > 30) 0.8f else 0.5f
        )
    }

    /**
     * Get formatted summary for LLM context
     * This is what the chatbot sees - NOT raw data
     */
    fun getContextForLLM(): String {
        val state = _financialState.value
        val prefs = _preferences.value

        return buildString {
            appendLine("=== USER FINANCIAL CONTEXT ===")
            appendLine("Monthly Income: ${prefs.currencySymbol}${formatNumber(state.monthlyIncome)}")
            appendLine("Monthly Spending: ${prefs.currencySymbol}${formatNumber(state.avgMonthlySpend)}")
            appendLine("Daily Average: ${prefs.currencySymbol}${formatNumber(state.avgDailySpend)}")
            appendLine("Savings Rate: ${formatNumber(state.savingsRate)}%")
            appendLine("Risk Level: ${state.riskLevel.display}")
            appendLine("Spending Trend: ${state.spendingTrend.display}")
            appendLine()

            if (state.topCategories.isNotEmpty()) {
                appendLine("Top Spending Categories:")
                state.topCategories.take(3).forEach { cat ->
                    appendLine("- ${cat.category}: ${prefs.currencySymbol}${formatNumber(cat.amount)} (${formatNumber(cat.percentage)}%)")
                }
                appendLine()
            }

            state.nextBill?.let { bill ->
                appendLine("Next Bill: ${bill.name} - ${prefs.currencySymbol}${formatNumber(bill.amount)} due in ${bill.dueInDays} days")
                appendLine()
            }

            if (state.anomalies.isNotEmpty()) {
                appendLine("Recent Anomalies: ${state.anomalies.size} unusual transactions detected")
                appendLine()
            }

            appendLine("Predictions:")
            appendLine("- End of month balance: ${prefs.currencySymbol}${formatNumber(state.predictions.endOfMonthBalance)}")
            appendLine("- Budget exceed risk: ${(state.predictions.budgetExceedProbability * 100).toInt()}%")
            appendLine("=== END CONTEXT ===")
        }
    }

    /**
     * Answer specific questions using learned data
     */
    fun answerQuestion(question: String): LearnerAnswer {
        val q = question.lowercase()
        val state = _financialState.value
        val prefs = _preferences.value
        val symbol = prefs.currencySymbol

        return when {
            // Confirmation responses (yes, no, okay, sure)
            isConfirmation(q) -> {
                // Return based on last context
                LearnerAnswer(
                    topic = "confirmation",
                    data = mapOf("confirmed" to true),
                    summary = "confirmation_response"
                )
            }

            // Negation responses
            isNegation(q) -> {
                LearnerAnswer(
                    topic = "negation",
                    data = mapOf("confirmed" to false),
                    summary = "negation_response"
                )
            }

            // Loan questions
            q.contains("loan") || q.contains("debt") || q.contains("owe") || q.contains("borrow") -> {
                LearnerAnswer(
                    topic = "loans",
                    data = emptyMap(),
                    summary = "loan_query"
                )
            }

            // Goal questions
            q.contains("goal") || q.contains("target") || q.contains("aim") -> {
                LearnerAnswer(
                    topic = "goals",
                    data = emptyMap(),
                    summary = "goal_query"
                )
            }

            // Budget questions
            q.contains("budget") -> {
                val estimatedBudget = state.monthlyIncome * 0.8 // 80% of income for spending
                LearnerAnswer(
                    topic = "budget",
                    data = mapOf(
                        "estimated_budget" to estimatedBudget,
                        "spent" to state.avgMonthlySpend
                    ),
                    summary = "Based on your income, recommended spending budget is $symbol${formatNumber(estimatedBudget)}. You're spending $symbol${formatNumber(state.avgMonthlySpend)}/month."
                )
            }

            // Track/on track questions
            q.contains("track") || q.contains("on track") || q.contains("doing") || q.contains("status") -> {
                val onTrack = state.savingsRate >= 15 && state.riskLevel in listOf(RiskLevel.LOW, RiskLevel.MODERATE)
                LearnerAnswer(
                    topic = "status",
                    data = mapOf(
                        "on_track" to onTrack,
                        "savings_rate" to state.savingsRate,
                        "risk" to state.riskLevel.name
                    ),
                    summary = if (onTrack) "You're on track! Savings rate: ${formatNumber(state.savingsRate)}%." else "There's room for improvement. Current savings rate: ${formatNumber(state.savingsRate)}%."
                )
            }

            // Spending questions
            q.contains("how much") && (q.contains("spend") || q.contains("spent")) -> {
                when {
                    q.contains("today") -> LearnerAnswer(
                        topic = "daily_spending",
                        data = mapOf("amount" to state.avgDailySpend),
                        summary = "Today's estimated spending is around $symbol${formatNumber(state.avgDailySpend)} based on your patterns."
                    )
                    q.contains("week") -> LearnerAnswer(
                        topic = "weekly_spending",
                        data = mapOf("amount" to state.avgDailySpend * 7),
                        summary = "Your weekly spending averages $symbol${formatNumber(state.avgDailySpend * 7)}."
                    )
                    q.contains("month") || q.contains("this month") -> LearnerAnswer(
                        topic = "monthly_spending",
                        data = mapOf("amount" to state.avgMonthlySpend),
                        summary = "Your monthly spending is $symbol${formatNumber(state.avgMonthlySpend)}."
                    )
                    else -> LearnerAnswer(
                        topic = "general_spending",
                        data = mapOf(
                            "monthly" to state.avgMonthlySpend,
                            "daily" to state.avgDailySpend
                        ),
                        summary = "You spend about $symbol${formatNumber(state.avgMonthlySpend)}/month or $symbol${formatNumber(state.avgDailySpend)}/day on average."
                    )
                }
            }

            // Income questions
            q.contains("income") || q.contains("earn") || q.contains("make") || q.contains("salary") -> {
                LearnerAnswer(
                    topic = "income",
                    data = mapOf("monthly_income" to state.monthlyIncome),
                    summary = "Your monthly income is $symbol${formatNumber(state.monthlyIncome)}."
                )
            }

            // Savings questions
            q.contains("save") || q.contains("saving") -> {
                LearnerAnswer(
                    topic = "savings",
                    data = mapOf(
                        "savings" to state.totalSavings,
                        "rate" to state.savingsRate
                    ),
                    summary = "You're saving $symbol${formatNumber(state.totalSavings)}/month (${formatNumber(state.savingsRate)}% savings rate)."
                )
            }

            // Bill questions
            q.contains("bill") || q.contains("due") || q.contains("payment") -> {
                val nextBill = state.nextBill
                if (nextBill != null) {
                    LearnerAnswer(
                        topic = "bills",
                        data = mapOf(
                            "next_bill" to nextBill.name,
                            "amount" to nextBill.amount,
                            "days" to nextBill.dueInDays
                        ),
                        summary = "Your next bill is ${nextBill.name} for $symbol${formatNumber(nextBill.amount)}, due in ${nextBill.dueInDays} days."
                    )
                } else {
                    LearnerAnswer(
                        topic = "bills",
                        data = emptyMap(),
                        summary = "You have no upcoming bills tracked."
                    )
                }
            }

            // Category questions
            q.contains("category") || q.contains("most") || q.contains("biggest") || q.contains("where") && q.contains("money") -> {
                val top = state.topCategories.firstOrNull()
                if (top != null) {
                    LearnerAnswer(
                        topic = "categories",
                        data = mapOf(
                            "top_category" to top.category,
                            "amount" to top.amount,
                            "percentage" to top.percentage
                        ),
                        summary = "Your biggest spending category is ${top.category} at $symbol${formatNumber(top.amount)} (${formatNumber(top.percentage)}% of total)."
                    )
                } else {
                    LearnerAnswer(
                        topic = "categories",
                        data = emptyMap(),
                        summary = "Not enough data to determine top categories yet."
                    )
                }
            }

            // Risk questions
            q.contains("risk") || q.contains("safe") || q.contains("okay") || q.contains("fine") || q.contains("healthy") -> {
                LearnerAnswer(
                    topic = "risk",
                    data = mapOf(
                        "risk_level" to state.riskLevel.name,
                        "trend" to state.spendingTrend.name
                    ),
                    summary = "Your financial risk is ${state.riskLevel.display} with ${state.spendingTrend.display.lowercase()} spending."
                )
            }

            // Prediction questions
            q.contains("predict") || q.contains("forecast") || q.contains("will i") || q.contains("next month") || q.contains("end of month") -> {
                LearnerAnswer(
                    topic = "predictions",
                    data = mapOf(
                        "end_balance" to state.predictions.endOfMonthBalance,
                        "exceed_risk" to state.predictions.budgetExceedProbability
                    ),
                    summary = "Predicted end-of-month balance: $symbol${formatNumber(state.predictions.endOfMonthBalance)}. Budget exceed risk: ${(state.predictions.budgetExceedProbability * 100).toInt()}%."
                )
            }

            // Afford questions
            q.contains("afford") || q.contains("can i buy") -> {
                LearnerAnswer(
                    topic = "affordability",
                    data = mapOf(
                        "disposable" to state.totalSavings,
                        "risk" to state.riskLevel.name
                    ),
                    summary = "Based on your savings of $symbol${formatNumber(state.totalSavings)} and ${state.riskLevel.display.lowercase()} risk level, I can help assess affordability."
                )
            }

            // Expense/spending general
            q.contains("expense") || q.contains("spending") || q.contains("spend") -> {
                LearnerAnswer(
                    topic = "general_spending",
                    data = mapOf(
                        "monthly" to state.avgMonthlySpend,
                        "daily" to state.avgDailySpend
                    ),
                    summary = "Your spending: $symbol${formatNumber(state.avgMonthlySpend)}/month, $symbol${formatNumber(state.avgDailySpend)}/day."
                )
            }

            // Help/what can you do
            q.contains("help") || q.contains("what can") || q.contains("features") -> {
                LearnerAnswer(
                    topic = "help",
                    data = emptyMap(),
                    summary = "help_response"
                )
            }

            // Default - return general summary
            else -> {
                LearnerAnswer(
                    topic = "summary",
                    data = mapOf(
                        "income" to state.monthlyIncome,
                        "spending" to state.avgMonthlySpend,
                        "savings" to state.totalSavings,
                        "risk" to state.riskLevel.name
                    ),
                    summary = "Income: $symbol${formatNumber(state.monthlyIncome)}, Spending: $symbol${formatNumber(state.avgMonthlySpend)}, Savings: $symbol${formatNumber(state.totalSavings)}, Risk: ${state.riskLevel.display}."
                )
            }
        }
    }

    private fun isConfirmation(text: String): Boolean {
        val confirmPatterns = listOf("yes", "yeah", "yep", "sure", "ok", "okay", "alright", "definitely", "absolutely", "please", "go ahead")
        return confirmPatterns.any { text.trim() == it || text.startsWith("$it ") || text.startsWith("$it,") }
    }

    private fun isNegation(text: String): Boolean {
        val negPatterns = listOf("no", "nope", "nah", "not now", "maybe later", "skip", "never mind")
        return negPatterns.any { text.trim() == it || text.startsWith("$it ") }
    }

    /**
     * Update chat memory
     */
    fun updateChatMemory(topic: String, question: String, intent: String? = null) {
        chatMemory.addTopic(topic)
        chatMemory.addQuestion(question)
        if (intent != null) {
            chatMemory.lastIntent = intent
        }
    }

    fun getChatMemory(): ChatMemory = chatMemory

    /**
     * Refresh data - call after new transactions
     */
    suspend fun refresh() {
        loadHistoricalData()
        learnPatterns()
        updateFinancialState()
    }

    /**
     * Release resources
     */
    fun release() {
        scope.cancel()
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value)
        }
    }
}

/**
 * Answer from the learner - structured data for LLM
 */
data class LearnerAnswer(
    val topic: String,
    val data: Map<String, Any>,
    val summary: String
)

