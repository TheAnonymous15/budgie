package com.example.budgie.ai

import android.content.Context
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.Bill
import com.example.budgie.data.model.Budget
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.InsightPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.regression.SimpleRegression
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * AI Engine for Financial Analysis using ML/Statistical methods
 */
class FinancialAIEngine(context: Context) {

    private val database = BudgieDatabase.getDatabase(context)
    private val expenseDao = database.expenseDao()
    private val incomeDao = database.incomeDao()
    private val billDao = database.billDao()
    private val budgetDao = database.budgetDao()

    companion object {
        const val SAVINGS_RATE_EXCELLENT = 0.30
        const val SAVINGS_RATE_GOOD = 0.20
        const val SAVINGS_RATE_FAIR = 0.10
        const val ANOMALY_THRESHOLD = 2.0

        @Volatile
        private var INSTANCE: FinancialAIEngine? = null

        fun getInstance(context: Context): FinancialAIEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinancialAIEngine(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Generate comprehensive AI-powered financial analysis
     */
    suspend fun generateFinancialAnalysis(): FinancialAnalysis = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance()

        // Get all data
        val allExpenses = expenseDao.getAllExpenses().first()
        val allIncomes = incomeDao.getAllIncomes().first()
        val allBills = billDao.getAllBills().first()
        val allBudgets = budgetDao.getAllBudgets().first()

        // Get current month data
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val monthlyExpenses = allExpenses.filter { it.date in monthStart until monthEnd }
        val monthlyIncome = allIncomes.filter { it.date in monthStart until monthEnd }

        val totalMonthlyExpenses = monthlyExpenses.sumOf { it.amount }
        val totalMonthlyIncome = monthlyIncome.sumOf { it.amount }

        // Generate analysis components
        val healthScore = calculateHealthScore(totalMonthlyIncome, totalMonthlyExpenses, allBills, allBudgets, allExpenses)
        val spendingPatterns = analyzeSpendingPatterns(allExpenses)
        val anomalies = detectAnomalies(monthlyExpenses)
        val predictions = generatePredictions(allExpenses, allIncomes)
        val insights = generateInsights(totalMonthlyIncome, totalMonthlyExpenses, spendingPatterns, anomalies, predictions, allBills)
        val recommendations = generateRecommendations(healthScore, spendingPatterns, predictions)

        FinancialAnalysis(
            healthScore = healthScore,
            spendingPatterns = spendingPatterns,
            anomalies = anomalies,
            predictions = predictions,
            insights = insights,
            recommendations = recommendations,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun calculateHealthScore(
        monthlyIncome: Double,
        monthlyExpenses: Double,
        bills: List<Bill>,
        budgets: List<Budget>,
        expenses: List<Expense>
    ): FinancialHealthScore {
        var score = 0.0
        val factors = mutableListOf<ScoreFactor>()

        // Savings Rate (30 points)
        val savingsRate = if (monthlyIncome > 0) (monthlyIncome - monthlyExpenses) / monthlyIncome else 0.0
        val savingsScore = when {
            savingsRate >= SAVINGS_RATE_EXCELLENT -> 30.0
            savingsRate >= SAVINGS_RATE_GOOD -> 25.0
            savingsRate >= SAVINGS_RATE_FAIR -> 15.0
            savingsRate >= 0 -> 10.0
            else -> 0.0
        }
        score += savingsScore
        factors.add(ScoreFactor(
            name = "Savings Rate",
            score = savingsScore,
            maxScore = 30.0,
            description = "Saving ${String.format(Locale.US, "%.0f", savingsRate * 100)}% of income",
            status = when {
                savingsRate >= SAVINGS_RATE_GOOD -> FactorStatus.EXCELLENT
                savingsRate >= SAVINGS_RATE_FAIR -> FactorStatus.GOOD
                savingsRate >= 0 -> FactorStatus.FAIR
                else -> FactorStatus.POOR
            }
        ))

        // Budget Adherence (25 points)
        val budgetScore = if (budgets.isEmpty()) 15.0 else {
            var adherence = 0.0
            budgets.forEach { budget ->
                val spent = expenses.filter { it.category == budget.category }.sumOf { it.amount }
                val util = if (budget.limit > 0) spent / budget.limit else 0.0
                adherence += when {
                    util <= 0.8 -> 25.0 / budgets.size
                    util <= 0.95 -> 20.0 / budgets.size
                    util <= 1.0 -> 15.0 / budgets.size
                    else -> 5.0 / budgets.size
                }
            }
            adherence
        }
        score += budgetScore
        factors.add(ScoreFactor(
            name = "Budget Adherence",
            score = budgetScore,
            maxScore = 25.0,
            description = "How well you stick to budgets",
            status = when {
                budgetScore >= 20 -> FactorStatus.EXCELLENT
                budgetScore >= 15 -> FactorStatus.GOOD
                budgetScore >= 10 -> FactorStatus.FAIR
                else -> FactorStatus.POOR
            }
        ))

        // Bill Management (20 points)
        val billScore = if (bills.isEmpty()) 15.0 else {
            val paidOnTime = bills.count { it.isPaid }
            (paidOnTime.toDouble() / bills.size) * 20
        }
        score += billScore
        factors.add(ScoreFactor(
            name = "Bill Management",
            score = billScore,
            maxScore = 20.0,
            description = "Timely bill payments",
            status = when {
                billScore >= 18 -> FactorStatus.EXCELLENT
                billScore >= 14 -> FactorStatus.GOOD
                billScore >= 10 -> FactorStatus.FAIR
                else -> FactorStatus.POOR
            }
        ))

        // Spending Diversity (15 points)
        val categoryCount = expenses.map { it.category }.distinct().size
        val diversityScore = (categoryCount.toDouble() / ExpenseCategory.entries.size) * 15
        score += diversityScore
        factors.add(ScoreFactor(
            name = "Spending Balance",
            score = diversityScore,
            maxScore = 15.0,
            description = "Balance across categories",
            status = when {
                diversityScore >= 12 -> FactorStatus.EXCELLENT
                diversityScore >= 9 -> FactorStatus.GOOD
                diversityScore >= 6 -> FactorStatus.FAIR
                else -> FactorStatus.POOR
            }
        ))

        // Stability (10 points)
        val stabilityScore = if (expenses.size < 10) 5.0 else {
            val stats = DescriptiveStatistics()
            expenses.forEach { stats.addValue(it.amount) }
            val cv = if (stats.mean > 0) stats.standardDeviation / stats.mean else 0.0
            when {
                cv < 0.3 -> 10.0
                cv < 0.5 -> 7.0
                cv < 0.8 -> 5.0
                else -> 3.0
            }
        }
        score += stabilityScore
        factors.add(ScoreFactor(
            name = "Financial Stability",
            score = stabilityScore,
            maxScore = 10.0,
            description = "Consistency in spending",
            status = when {
                stabilityScore >= 8 -> FactorStatus.EXCELLENT
                stabilityScore >= 6 -> FactorStatus.GOOD
                stabilityScore >= 4 -> FactorStatus.FAIR
                else -> FactorStatus.POOR
            }
        ))

        val status = when {
            score >= 80 -> HealthStatus.EXCELLENT
            score >= 60 -> HealthStatus.GOOD
            score >= 40 -> HealthStatus.FAIR
            else -> HealthStatus.NEEDS_ATTENTION
        }

        return FinancialHealthScore(
            overallScore = score.toInt(),
            status = status,
            factors = factors,
            savingsRate = savingsRate,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses
        )
    }

    private fun analyzeSpendingPatterns(expenses: List<Expense>): SpendingPatterns {
        if (expenses.isEmpty()) {
            return SpendingPatterns(
                categoryBreakdown = emptyMap(),
                dailyAverage = 0.0,
                weeklyAverage = 0.0,
                monthlyAverage = 0.0,
                peakSpendingDays = emptyList(),
                peakSpendingCategories = emptyList(),
                trendDirection = TrendDirection.STABLE,
                volatility = 0.0
            )
        }

        // Category breakdown
        val categoryTotals = expenses.groupBy { it.category }.mapValues { (_, exps) -> exps.sumOf { it.amount } }
        val totalSpending = categoryTotals.values.sum()
        val categoryBreakdown = categoryTotals.mapValues { (_, amount) ->
            if (totalSpending > 0) (amount / totalSpending * 100) else 0.0
        }

        // Averages
        val calendar = Calendar.getInstance()
        val dayGroups = expenses.groupBy { exp ->
            calendar.timeInMillis = exp.date
            calendar.get(Calendar.DAY_OF_YEAR)
        }
        val dailyAverage = if (dayGroups.isNotEmpty()) dayGroups.values.map { it.sumOf { e -> e.amount } }.average() else 0.0

        val weekGroups = expenses.groupBy { exp ->
            calendar.timeInMillis = exp.date
            calendar.get(Calendar.WEEK_OF_YEAR)
        }
        val weeklyAverage = if (weekGroups.isNotEmpty()) weekGroups.values.map { it.sumOf { e -> e.amount } }.average() else 0.0

        val monthGroups = expenses.groupBy { exp ->
            calendar.timeInMillis = exp.date
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
        }
        val monthlyTotals = monthGroups.mapValues { (_, exps) -> exps.sumOf { it.amount } }
        val monthlyAverage = if (monthlyTotals.isNotEmpty()) monthlyTotals.values.average() else 0.0

        // Peak days
        val dayOfWeekTotals = expenses.groupBy { exp ->
            calendar.timeInMillis = exp.date
            calendar.get(Calendar.DAY_OF_WEEK)
        }.mapValues { (_, exps) -> exps.sumOf { it.amount } }

        val peakDays = dayOfWeekTotals.entries.sortedByDescending { it.value }.take(3).map { getDayName(it.key) }
        val peakCategories = categoryTotals.entries.sortedByDescending { it.value }.take(3).map { it.key }

        // Trend using regression
        val regression = SimpleRegression()
        val stats = DescriptiveStatistics()
        val sortedMonths = monthlyTotals.entries.sortedBy { it.key }
        sortedMonths.forEachIndexed { index, entry ->
            regression.addData(index.toDouble(), entry.value)
            stats.addValue(entry.value)
        }

        val slope = if (sortedMonths.size >= 2) regression.slope else 0.0
        val trendDirection = when {
            slope > monthlyAverage * 0.05 -> TrendDirection.INCREASING
            slope < -monthlyAverage * 0.05 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }

        val volatility = if (stats.n > 1 && stats.mean > 0) stats.standardDeviation / stats.mean else 0.0

        return SpendingPatterns(
            categoryBreakdown = categoryBreakdown,
            dailyAverage = dailyAverage,
            weeklyAverage = weeklyAverage,
            monthlyAverage = monthlyAverage,
            peakSpendingDays = peakDays,
            peakSpendingCategories = peakCategories,
            trendDirection = trendDirection,
            volatility = volatility
        )
    }

    private fun detectAnomalies(expenses: List<Expense>): List<SpendingAnomaly> {
        if (expenses.size < 5) return emptyList()

        val anomalies = mutableListOf<SpendingAnomaly>()
        val categoryGroups = expenses.groupBy { it.category }

        categoryGroups.forEach { (_, categoryExpenses) ->
            if (categoryExpenses.size >= 3) {
                val stats = DescriptiveStatistics()
                categoryExpenses.forEach { stats.addValue(it.amount) }
                val mean = stats.mean
                val stdDev = stats.standardDeviation

                categoryExpenses.forEach { expense ->
                    val zScore = if (stdDev > 0) (expense.amount - mean) / stdDev else 0.0
                    if (abs(zScore) > ANOMALY_THRESHOLD) {
                        val severity = when {
                            abs(zScore) > 3.5 -> AnomalySeverity.HIGH
                            abs(zScore) > 2.5 -> AnomalySeverity.MEDIUM
                            else -> AnomalySeverity.LOW
                        }
                        val type = if (zScore > 0) AnomalyType.UNUSUALLY_HIGH else AnomalyType.UNUSUALLY_LOW
                        anomalies.add(SpendingAnomaly(
                            expense = expense,
                            zScore = zScore,
                            expectedAmount = mean,
                            severity = severity,
                            type = type,
                            message = "${expense.category.displayName}: $${String.format(Locale.US, "%.2f", expense.amount)} is unusual"
                        ))
                    }
                }
            }
        }
        return anomalies.sortedByDescending { abs(it.zScore) }
    }

    private fun generatePredictions(expenses: List<Expense>, incomes: List<Income>): FinancialPredictions {
        val calendar = Calendar.getInstance()
        val monthlyExpenseTotals = mutableListOf<Double>()
        val monthlyIncomeTotals = mutableListOf<Double>()

        for (i in 5 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, -i)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis

            val monthExpenses = expenses.filter { it.date in monthStart until monthEnd }.sumOf { it.amount }
            val monthIncome = incomes.filter { it.date in monthStart until monthEnd }.sumOf { it.amount }

            monthlyExpenseTotals.add(monthExpenses)
            monthlyIncomeTotals.add(monthIncome)
        }

        // Predict using regression
        val expenseRegression = SimpleRegression()
        monthlyExpenseTotals.forEachIndexed { index, total -> expenseRegression.addData(index.toDouble(), total) }
        val predictedExpenses = max(0.0, expenseRegression.predict(6.0))
        val expenseConfidence = calculateConfidence(expenseRegression, monthlyExpenseTotals)

        val incomeRegression = SimpleRegression()
        monthlyIncomeTotals.forEachIndexed { index, total -> incomeRegression.addData(index.toDouble(), total) }
        val predictedIncome = max(0.0, incomeRegression.predict(6.0))
        val incomeConfidence = calculateConfidence(incomeRegression, monthlyIncomeTotals)

        // Category predictions
        val categoryPredictions = mutableMapOf<ExpenseCategory, Double>()
        ExpenseCategory.entries.forEach { category ->
            val categoryExpenses = expenses.filter { it.category == category }
            if (categoryExpenses.isNotEmpty()) {
                categoryPredictions[category] = categoryExpenses.takeLast(10).map { it.amount }.average()
            }
        }

        return FinancialPredictions(
            predictedMonthlyExpenses = predictedExpenses,
            expenseConfidence = expenseConfidence,
            predictedMonthlyIncome = predictedIncome,
            incomeConfidence = incomeConfidence,
            predictedSavings = predictedIncome - predictedExpenses,
            categoryPredictions = categoryPredictions,
            historicalExpenses = monthlyExpenseTotals,
            historicalIncome = monthlyIncomeTotals
        )
    }

    private fun generateInsights(
        monthlyIncome: Double,
        monthlyExpenses: Double,
        patterns: SpendingPatterns,
        anomalies: List<SpendingAnomaly>,
        predictions: FinancialPredictions,
        bills: List<Bill>
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""

        // Savings insight
        val savingsRate = if (monthlyIncome > 0) (monthlyIncome - monthlyExpenses) / monthlyIncome else 0.0
        insights.add(AIInsight(
            id = "savings_rate",
            title = "Savings Analysis",
            message = when {
                savingsRate >= 0.30 -> "🌟 Outstanding! You're saving ${(savingsRate * 100).toInt()}% of your income!"
                savingsRate >= 0.20 -> "✅ Great job! Your ${(savingsRate * 100).toInt()}% savings rate is above recommended 20%."
                savingsRate >= 0.10 -> "📊 Saving ${(savingsRate * 100).toInt()}%. Try pushing to 20% for better security."
                savingsRate >= 0 -> "⚠️ Savings rate is ${(savingsRate * 100).toInt()}%. Consider the 50/30/20 rule."
                else -> "🚨 Spending exceeds income. Budget review needed."
            },
            category = InsightCategory.SAVINGS,
            priority = if (savingsRate < 0.10) InsightPriority.HIGH else InsightPriority.MEDIUM,
            actionable = savingsRate < 0.20,
            action = if (savingsRate < 0.20) "Review budget and find areas to cut" else null
        ))

        // Spending trend
        insights.add(AIInsight(
            id = "spending_trend",
            title = "Spending Trend",
            message = when (patterns.trendDirection) {
                TrendDirection.INCREASING -> "📈 Spending increasing. Predicted for $currentMonth: $${String.format(Locale.US, "%.0f", predictions.predictedMonthlyExpenses)}"
                TrendDirection.DECREASING -> "📉 Great! Your spending is trending down."
                TrendDirection.STABLE -> "➡️ Spending stable at ~$${String.format(Locale.US, "%.0f", patterns.monthlyAverage)}/month."
            },
            category = InsightCategory.SPENDING,
            priority = if (patterns.trendDirection == TrendDirection.INCREASING) InsightPriority.HIGH else InsightPriority.LOW,
            actionable = patterns.trendDirection == TrendDirection.INCREASING,
            action = if (patterns.trendDirection == TrendDirection.INCREASING) "Set spending limits" else null
        ))

        // Top category
        if (patterns.peakSpendingCategories.isNotEmpty()) {
            val topCategory = patterns.peakSpendingCategories.first()
            val percentage = patterns.categoryBreakdown[topCategory] ?: 0.0
            insights.add(AIInsight(
                id = "top_category",
                title = "Top Spending Category",
                message = "💡 ${topCategory.displayName} is ${percentage.toInt()}% of spending. ${getCategoryTip(topCategory)}",
                category = InsightCategory.CATEGORY_ANALYSIS,
                priority = if (percentage > 40) InsightPriority.MEDIUM else InsightPriority.LOW,
                actionable = percentage > 40,
                action = if (percentage > 40) "Consider setting a budget for ${topCategory.displayName}" else null
            ))
        }

        // Anomalies
        val highAnomalies = anomalies.filter { it.severity == AnomalySeverity.HIGH }
        if (highAnomalies.isNotEmpty()) {
            insights.add(AIInsight(
                id = "anomalies",
                title = "Unusual Spending Detected",
                message = "🔍 Found ${highAnomalies.size} unusual transaction(s). Review for accuracy.",
                category = InsightCategory.ANOMALY,
                priority = InsightPriority.HIGH,
                actionable = true,
                action = "Review flagged transactions"
            ))
        }

        // Upcoming bills
        val upcomingBills = bills.filter { bill ->
            val daysUntilDue = ((bill.dueDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
            daysUntilDue in 0..7 && !bill.isPaid
        }
        if (upcomingBills.isNotEmpty()) {
            val totalDue = upcomingBills.sumOf { it.amount }
            insights.add(AIInsight(
                id = "upcoming_bills",
                title = "Bills Due Soon",
                message = "📅 ${upcomingBills.size} bill(s) due in 7 days totaling $${String.format(Locale.US, "%.2f", totalDue)}",
                category = InsightCategory.BILLS,
                priority = InsightPriority.HIGH,
                actionable = true,
                action = "Pay upcoming bills"
            ))
        }

        return insights.sortedByDescending { insight: AIInsight -> insight.priority.ordinal }
    }

    private fun generateRecommendations(
        healthScore: FinancialHealthScore,
        patterns: SpendingPatterns,
        predictions: FinancialPredictions
    ): List<AIRecommendation> {
        val recommendations = mutableListOf<AIRecommendation>()

        // Health improvement
        if (healthScore.overallScore < 60) {
            val poorFactors = healthScore.factors.filter { it.status == FactorStatus.POOR || it.status == FactorStatus.FAIR }
            recommendations.add(AIRecommendation(
                id = "improve_health",
                title = "Improve Financial Health",
                description = "Score: ${healthScore.overallScore}/100. Focus on weak areas.",
                steps = poorFactors.map { "Improve ${it.name}: ${it.description}" },
                impact = RecommendationImpact.HIGH,
                difficulty = RecommendationDifficulty.MEDIUM,
                timeframe = "1-3 months"
            ))
        }

        // Savings
        if (healthScore.savingsRate < 0.20) {
            val target = healthScore.monthlyIncome * 0.20
            val current = healthScore.monthlyIncome - healthScore.monthlyExpenses
            recommendations.add(AIRecommendation(
                id = "increase_savings",
                title = "Boost Savings to 20%",
                description = "Target: $${String.format(Locale.US, "%.0f", target)}/month",
                steps = listOf(
                    "Current: $${String.format(Locale.US, "%.0f", current)}/month",
                    "Cut $${String.format(Locale.US, "%.0f", target - current)} from discretionary",
                    "Automate savings transfers"
                ),
                impact = RecommendationImpact.HIGH,
                difficulty = RecommendationDifficulty.MEDIUM,
                timeframe = "1 month"
            ))
        }

        // Budget alerts
        if (predictions.predictedMonthlyExpenses > patterns.monthlyAverage * 1.1) {
            recommendations.add(AIRecommendation(
                id = "budget_alert",
                title = "Predicted Overspending",
                description = "Next month may exceed average",
                steps = listOf(
                    "Predicted: $${String.format(Locale.US, "%.0f", predictions.predictedMonthlyExpenses)}",
                    "Average: $${String.format(Locale.US, "%.0f", patterns.monthlyAverage)}",
                    "Set spending alerts at 75%"
                ),
                impact = RecommendationImpact.MEDIUM,
                difficulty = RecommendationDifficulty.EASY,
                timeframe = "This week"
            ))
        }

        // Emergency fund
        if (healthScore.savingsRate < 0.10) {
            recommendations.add(AIRecommendation(
                id = "emergency_fund",
                title = "Build Emergency Fund",
                description = "Target: 3-6 months of expenses",
                steps = listOf(
                    "Target: $${String.format(Locale.US, "%.0f", patterns.monthlyAverage * 3)}+",
                    "Start with $100/month auto-transfer",
                    "Use high-yield savings account"
                ),
                impact = RecommendationImpact.HIGH,
                difficulty = RecommendationDifficulty.HARD,
                timeframe = "6-12 months"
            ))
        }

        return recommendations.sortedByDescending { it.impact.ordinal }
    }

    private fun calculateConfidence(regression: SimpleRegression, data: List<Double>): Double {
        if (data.size < 3) return 0.5
        val rSquared = regression.rSquare
        return if (rSquared.isNaN()) 0.5 else min(0.95, max(0.3, rSquared))
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }

    private fun getCategoryTip(category: ExpenseCategory): String {
        return when (category) {
            ExpenseCategory.FOOD -> "Try meal prepping to save."
            ExpenseCategory.TRANSPORT -> "Consider carpooling."
            ExpenseCategory.ENTERTAINMENT -> "Look for free alternatives."
            ExpenseCategory.SHOPPING -> "Use the 24-hour rule."
            ExpenseCategory.UTILITIES -> "Check energy-saving options."
            ExpenseCategory.HEALTH -> "Use preventive care."
            ExpenseCategory.EDUCATION -> "Look for scholarships."
            ExpenseCategory.RENT -> "Review lease annually."
            ExpenseCategory.INSURANCE -> "Compare providers annually."
            ExpenseCategory.SAVINGS -> "Great! Keep saving."
            ExpenseCategory.INVESTMENT -> "Diversify portfolio."
            ExpenseCategory.OTHER -> "Categorize for better tracking."
        }
    }
}

