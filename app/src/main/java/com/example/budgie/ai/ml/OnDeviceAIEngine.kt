package com.example.budgie.ai.ml

import android.content.Context
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Unified On-Device AI Engine
 * Combines all ML models for comprehensive financial analysis
 * Runs 100% offline
 */
class OnDeviceAIEngine(private val context: Context) {

    private val database = BudgieDatabase.getDatabase(context)
    private val spendingPredictor = TFLiteSpendingPredictor.getInstance(context)
    private val budgetOptimizer = BudgetOptimizer.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: OnDeviceAIEngine? = null

        fun getInstance(context: Context): OnDeviceAIEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnDeviceAIEngine(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Initialize all ML models
     */
    suspend fun initialize() {
        spendingPredictor.initialize()
    }

    /**
     * Generate comprehensive AI analysis
     */
    suspend fun generateComprehensiveAnalysis(): ComprehensiveAnalysis = withContext(Dispatchers.Default) {
        val expenses = database.expenseDao().getAllExpenses().first()
        val incomes = database.incomeDao().getAllIncomes().first()
        val budgets = database.budgetDao().getAllBudgets().first()
        val bills = database.billDao().getAllBills().first()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val monthlyExpenses = expenses.filter { it.date in monthStart until monthEnd }
        val monthlyIncome = incomes.filter { it.date in monthStart until monthEnd }.sumOf { it.amount }
        val monthlySpending = monthlyExpenses.sumOf { it.amount }

        // Run all AI models
        val spendingPrediction = spendingPredictor.predictNextMonthSpending(expenses)
        val anomalies = spendingPredictor.detectAnomalies(monthlyExpenses)
        val budgetRecommendation = budgetOptimizer.generateOptimizedBudget(
            monthlyIncome = monthlyIncome,
            expenses = expenses,
            currentBudgets = budgets
        )

        // Calculate financial health
        val healthScore = calculateHealthScore(
            income = monthlyIncome,
            spending = monthlySpending,
            savingsRate = budgetRecommendation.savingsRate,
            budgetAdherence = calculateBudgetAdherence(budgets, monthlyExpenses),
            billsOnTime = bills.filter { it.isPaid }.size.toDouble() / max(1, bills.size)
        )

        // Generate smart insights
        val insights = generateSmartInsights(
            expenses = monthlyExpenses,
            income = monthlyIncome,
            prediction = spendingPrediction,
            anomalies = anomalies,
            healthScore = healthScore
        )

        ComprehensiveAnalysis(
            healthScore = healthScore,
            spendingPrediction = spendingPrediction,
            budgetRecommendation = budgetRecommendation,
            anomalies = anomalies,
            insights = insights,
            monthlyIncome = monthlyIncome,
            monthlySpending = monthlySpending,
            savingsRate = if (monthlyIncome > 0) (monthlyIncome - monthlySpending) / monthlyIncome else 0.0
        )
    }

    /**
     * Calculate comprehensive health score (0-100)
     */
    private fun calculateHealthScore(
        income: Double,
        spending: Double,
        savingsRate: Double,
        budgetAdherence: Double,
        billsOnTime: Double
    ): HealthScore {
        var score = 0.0
        val components = mutableListOf<ScoreComponent>()

        // Savings Rate (35 points)
        val savingsScore = when {
            savingsRate >= 0.30 -> 35.0
            savingsRate >= 0.20 -> 30.0
            savingsRate >= 0.10 -> 20.0
            savingsRate >= 0 -> 10.0
            else -> 0.0
        }
        score += savingsScore
        components.add(ScoreComponent(
            name = "Savings Rate",
            score = savingsScore,
            maxScore = 35.0,
            status = when {
                savingsScore >= 30 -> ComponentStatus.EXCELLENT
                savingsScore >= 20 -> ComponentStatus.GOOD
                savingsScore >= 10 -> ComponentStatus.FAIR
                else -> ComponentStatus.POOR
            }
        ))

        // Budget Adherence (25 points)
        val budgetScore = budgetAdherence * 25
        score += budgetScore
        components.add(ScoreComponent(
            name = "Budget Adherence",
            score = budgetScore,
            maxScore = 25.0,
            status = when {
                budgetScore >= 22 -> ComponentStatus.EXCELLENT
                budgetScore >= 18 -> ComponentStatus.GOOD
                budgetScore >= 12 -> ComponentStatus.FAIR
                else -> ComponentStatus.POOR
            }
        ))

        // Bill Payments (20 points)
        val billScore = billsOnTime * 20
        score += billScore
        components.add(ScoreComponent(
            name = "Bill Payments",
            score = billScore,
            maxScore = 20.0,
            status = when {
                billScore >= 18 -> ComponentStatus.EXCELLENT
                billScore >= 14 -> ComponentStatus.GOOD
                billScore >= 10 -> ComponentStatus.FAIR
                else -> ComponentStatus.POOR
            }
        ))

        // Spending Control (20 points)
        val spendingRatio = if (income > 0) spending / income else 1.0
        val spendingScore = when {
            spendingRatio <= 0.7 -> 20.0
            spendingRatio <= 0.8 -> 15.0
            spendingRatio <= 0.9 -> 10.0
            spendingRatio <= 1.0 -> 5.0
            else -> 0.0
        }
        score += spendingScore
        components.add(ScoreComponent(
            name = "Spending Control",
            score = spendingScore,
            maxScore = 20.0,
            status = when {
                spendingScore >= 15 -> ComponentStatus.EXCELLENT
                spendingScore >= 10 -> ComponentStatus.GOOD
                spendingScore >= 5 -> ComponentStatus.FAIR
                else -> ComponentStatus.POOR
            }
        ))

        val status = when {
            score >= 80 -> OverallStatus.EXCELLENT
            score >= 60 -> OverallStatus.GOOD
            score >= 40 -> OverallStatus.FAIR
            else -> OverallStatus.NEEDS_WORK
        }

        return HealthScore(
            overall = score.toInt(),
            status = status,
            components = components,
            trend = HealthTrend.STABLE // Can be calculated with historical data
        )
    }

    private fun calculateBudgetAdherence(
        budgets: List<com.example.budgie.data.model.Budget>,
        expenses: List<Expense>
    ): Double {
        if (budgets.isEmpty()) return 0.8 // Default if no budgets

        var totalAdherence = 0.0
        budgets.forEach { budget ->
            val spent = expenses.filter { it.category == budget.category }.sumOf { it.amount }
            val adherence = if (budget.limit > 0) {
                min(1.0, budget.limit / max(spent, 0.01))
            } else 1.0
            totalAdherence += adherence
        }

        return totalAdherence / budgets.size
    }

    /**
     * Generate smart, personalized insights
     */
    private fun generateSmartInsights(
        expenses: List<Expense>,
        income: Double,
        prediction: SpendingPrediction,
        anomalies: List<AnomalyResult>,
        healthScore: HealthScore
    ): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()

        val spending = expenses.sumOf { it.amount }
        val savingsRate = if (income > 0) (income - spending) / income else 0.0

        // Health score insight
        insights.add(SmartInsight(
            id = "health_score",
            type = SmartInsightType.SCORE,
            title = "Financial Health: ${healthScore.overall}/100",
            description = when (healthScore.status) {
                OverallStatus.EXCELLENT -> "Outstanding! You're in top financial shape."
                OverallStatus.GOOD -> "Good job! A few tweaks could make it excellent."
                OverallStatus.FAIR -> "Room for improvement. Focus on weak areas."
                OverallStatus.NEEDS_WORK -> "Needs attention. Let's create an action plan."
            },
            priority = if (healthScore.overall < 50) Priority.HIGH else Priority.MEDIUM,
            actionable = healthScore.overall < 80
        ))

        // Prediction insight
        if (prediction.predictedAmount > 0) {
            val changePercent = if (spending > 0) {
                ((prediction.predictedAmount - spending) / spending * 100).toInt()
            } else 0

            insights.add(SmartInsight(
                id = "prediction",
                type = SmartInsightType.PREDICTION,
                title = "Next Month: $${String.format("%.0f", prediction.predictedAmount)}",
                description = when {
                    changePercent > 10 -> "⚠️ Predicted $changePercent% increase. Plan ahead!"
                    changePercent < -10 -> "📉 Predicted $${abs(changePercent)}% decrease. Great trend!"
                    else -> "Spending expected to remain stable."
                },
                priority = if (changePercent > 20) Priority.HIGH else Priority.LOW,
                actionable = changePercent > 10
            ))
        }

        // Savings insight
        insights.add(SmartInsight(
            id = "savings",
            type = SmartInsightType.SAVINGS,
            title = "Savings Rate: ${(savingsRate * 100).toInt()}%",
            description = when {
                savingsRate >= 0.30 -> "🌟 Excellent! You're saving like a pro."
                savingsRate >= 0.20 -> "✅ Great! You're hitting the 20% target."
                savingsRate >= 0.10 -> "📊 Good start. Aim for 20%."
                savingsRate >= 0 -> "⚠️ Low savings. Review your expenses."
                else -> "🚨 Spending exceeds income!"
            },
            priority = if (savingsRate < 0.10) Priority.HIGH else Priority.MEDIUM,
            actionable = savingsRate < 0.20
        ))

        // Trend insight
        insights.add(SmartInsight(
            id = "trend",
            type = SmartInsightType.TREND,
            title = "Spending Trend: ${prediction.trend.name.lowercase().replaceFirstChar { it.uppercase() }}",
            description = when (prediction.trend) {
                SpendingTrend.INCREASING -> "📈 Your spending is trending up. Time to review budgets."
                SpendingTrend.DECREASING -> "📉 Great! Your spending is going down."
                SpendingTrend.STABLE -> "➡️ Your spending is consistent and predictable."
            },
            priority = if (prediction.trend == SpendingTrend.INCREASING) Priority.MEDIUM else Priority.LOW,
            actionable = prediction.trend == SpendingTrend.INCREASING
        ))

        // Anomaly insight
        if (anomalies.isNotEmpty()) {
            val highSeverity = anomalies.count { it.severity == AnomalySeverity.HIGH }
            insights.add(SmartInsight(
                id = "anomalies",
                type = SmartInsightType.ANOMALY,
                title = "${anomalies.size} Unusual Transaction(s) Found",
                description = if (highSeverity > 0) {
                    "🔍 $highSeverity high-severity anomalies detected. Review recommended."
                } else {
                    "🔍 Minor spending anomalies detected."
                },
                priority = if (highSeverity > 0) Priority.HIGH else Priority.LOW,
                actionable = true
            ))
        }

        // Top spending category insight
        val categorySpending = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }

        val topCategory = categorySpending.maxByOrNull { it.value }
        if (topCategory != null && spending > 0) {
            val percentage = (topCategory.value / spending * 100).toInt()
            insights.add(SmartInsight(
                id = "top_category",
                type = SmartInsightType.CATEGORY,
                title = "Top Spending: ${topCategory.key.displayName}",
                description = "${topCategory.key.displayName} is $percentage% of your spending ($${String.format("%.0f", topCategory.value)})",
                priority = if (percentage > 40) Priority.MEDIUM else Priority.LOW,
                actionable = percentage > 40
            ))
        }

        return insights.sortedByDescending { it.priority.ordinal }
    }

    /**
     * Get quick AI response for chat
     */
    suspend fun getQuickInsight(query: String): String = withContext(Dispatchers.Default) {
        val analysis = generateComprehensiveAnalysis()

        val lowerQuery = query.lowercase()

        when {
            lowerQuery.contains("health") || lowerQuery.contains("score") -> {
                "Your Financial Health Score is ${analysis.healthScore.overall}/100 (${analysis.healthScore.status.name}). " +
                analysis.healthScore.components.minByOrNull { it.score / it.maxScore }?.let {
                    "Focus on improving ${it.name} for the biggest impact."
                }
            }

            lowerQuery.contains("predict") || lowerQuery.contains("next month") -> {
                "Based on AI analysis, your predicted spending for next month is $${String.format("%.0f", analysis.spendingPrediction.predictedAmount)} " +
                "(${(analysis.spendingPrediction.confidence * 100).toInt()}% confidence). " +
                "Trend: ${analysis.spendingPrediction.trend.name.lowercase()}."
            }

            lowerQuery.contains("save") || lowerQuery.contains("saving") -> {
                val potentialSavings = analysis.budgetRecommendation.potentialMonthlySavings
                "Your current savings rate is ${(analysis.savingsRate * 100).toInt()}%. " +
                if (potentialSavings > 0) {
                    "You could save an extra $${String.format("%.0f", potentialSavings)}/month by following AI recommendations."
                } else {
                    "Great job on your savings!"
                }
            }

            lowerQuery.contains("budget") -> {
                "AI Budget Analysis: " +
                "Needs: $${String.format("%.0f", analysis.budgetRecommendation.allocationBreakdown.needs)}, " +
                "Wants: $${String.format("%.0f", analysis.budgetRecommendation.allocationBreakdown.wants)}, " +
                "Savings: $${String.format("%.0f", analysis.budgetRecommendation.allocationBreakdown.savings)}. " +
                analysis.budgetRecommendation.insights.firstOrNull()?.message ?: ""
            }

            lowerQuery.contains("anomal") || lowerQuery.contains("unusual") -> {
                if (analysis.anomalies.isEmpty()) {
                    "No unusual transactions detected this month. Your spending patterns look normal."
                } else {
                    "Found ${analysis.anomalies.size} unusual transaction(s). " +
                    analysis.anomalies.firstOrNull()?.let {
                        "Highest: ${it.expense.category.displayName} - $${String.format("%.2f", it.expense.amount)} (${it.severity} severity)"
                    }
                }
            }

            else -> {
                "Quick Summary: Health ${analysis.healthScore.overall}/100, " +
                "Spending $${String.format("%.0f", analysis.monthlySpending)}, " +
                "Savings ${(analysis.savingsRate * 100).toInt()}%. " +
                analysis.insights.firstOrNull()?.description ?: "Everything looks good!"
            }
        }
    }

    fun cleanup() {
        spendingPredictor.close()
    }
}

// Data classes for comprehensive analysis
data class ComprehensiveAnalysis(
    val healthScore: HealthScore,
    val spendingPrediction: SpendingPrediction,
    val budgetRecommendation: BudgetRecommendation,
    val anomalies: List<AnomalyResult>,
    val insights: List<SmartInsight>,
    val monthlyIncome: Double,
    val monthlySpending: Double,
    val savingsRate: Double
)

data class HealthScore(
    val overall: Int,
    val status: OverallStatus,
    val components: List<ScoreComponent>,
    val trend: HealthTrend
)

data class ScoreComponent(
    val name: String,
    val score: Double,
    val maxScore: Double,
    val status: ComponentStatus
)

enum class OverallStatus {
    EXCELLENT, GOOD, FAIR, NEEDS_WORK
}

enum class ComponentStatus {
    EXCELLENT, GOOD, FAIR, POOR
}

enum class HealthTrend {
    IMPROVING, STABLE, DECLINING
}

data class SmartInsight(
    val id: String,
    val type: SmartInsightType,
    val title: String,
    val description: String,
    val priority: Priority,
    val actionable: Boolean
)

enum class SmartInsightType {
    SCORE, PREDICTION, SAVINGS, TREND, ANOMALY, CATEGORY, BUDGET, BILL
}

enum class Priority {
    LOW, MEDIUM, HIGH
}

