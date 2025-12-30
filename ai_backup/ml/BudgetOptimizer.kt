package com.example.budgie.ai.ml

import android.content.Context
import com.example.budgie.data.model.Budget
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.Income
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * On-device ML Budget Optimizer
 * Uses rule-based AI and statistical analysis for budget recommendations
 * Runs completely offline
 */
class BudgetOptimizer(private val context: Context) {

    companion object {
        // 50/30/20 Rule percentages
        private const val NEEDS_PERCENTAGE = 0.50
        private const val WANTS_PERCENTAGE = 0.30
        private const val SAVINGS_PERCENTAGE = 0.20

        // Category classifications
        private val NEEDS_CATEGORIES = setOf(
            ExpenseCategory.RENT,
            ExpenseCategory.UTILITIES,
            ExpenseCategory.FOOD,
            ExpenseCategory.TRANSPORT,
            ExpenseCategory.HEALTH,
            ExpenseCategory.INSURANCE
        )

        private val WANTS_CATEGORIES = setOf(
            ExpenseCategory.ENTERTAINMENT,
            ExpenseCategory.SHOPPING,
            ExpenseCategory.EDUCATION
        )

        private val SAVINGS_CATEGORIES = setOf(
            ExpenseCategory.SAVINGS,
            ExpenseCategory.INVESTMENT
        )

        @Volatile
        private var INSTANCE: BudgetOptimizer? = null

        fun getInstance(context: Context): BudgetOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BudgetOptimizer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Generate optimized budget recommendations based on:
     * - Income level
     * - Spending history
     * - Financial goals
     * - 50/30/20 rule
     */
    suspend fun generateOptimizedBudget(
        monthlyIncome: Double,
        expenses: List<Expense>,
        currentBudgets: List<Budget>,
        savingsGoal: Double = 0.20
    ): BudgetRecommendation = withContext(Dispatchers.Default) {

        // Analyze current spending patterns
        val spendingByCategory = analyzeSpendingPatterns(expenses)

        // Calculate ideal allocations based on 50/30/20 rule
        val idealAllocations = calculateIdealAllocations(monthlyIncome, savingsGoal)

        // Generate category-specific budgets
        val categoryBudgets = generateCategoryBudgets(
            monthlyIncome,
            spendingByCategory,
            idealAllocations
        )

        // Calculate potential savings
        val currentTotal = spendingByCategory.values.sum()
        val optimizedTotal = categoryBudgets.values.sum()
        val potentialSavings = max(0.0, currentTotal - optimizedTotal)

        // Generate insights
        val insights = generateBudgetInsights(
            spendingByCategory,
            categoryBudgets,
            monthlyIncome
        )

        BudgetRecommendation(
            recommendedBudgets = categoryBudgets,
            totalBudget = optimizedTotal,
            potentialMonthlySavings = potentialSavings,
            savingsRate = if (monthlyIncome > 0) (monthlyIncome - optimizedTotal) / monthlyIncome else 0.0,
            insights = insights,
            allocationBreakdown = AllocationBreakdown(
                needs = categoryBudgets.filterKeys { it in NEEDS_CATEGORIES }.values.sum(),
                wants = categoryBudgets.filterKeys { it in WANTS_CATEGORIES }.values.sum(),
                savings = categoryBudgets.filterKeys { it in SAVINGS_CATEGORIES }.values.sum()
            )
        )
    }

    /**
     * Analyze historical spending patterns
     */
    private fun analyzeSpendingPatterns(expenses: List<Expense>): Map<ExpenseCategory, Double> {
        val calendar = Calendar.getInstance()

        // Get last 3 months average
        val monthlyTotals = mutableMapOf<ExpenseCategory, MutableList<Double>>()

        for (i in 2 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, -i)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis

            val monthExpenses = expenses.filter { it.date in monthStart until monthEnd }

            ExpenseCategory.entries.forEach { category ->
                val categoryTotal = monthExpenses
                    .filter { it.category == category }
                    .sumOf { it.amount }

                monthlyTotals.getOrPut(category) { mutableListOf() }.add(categoryTotal)
            }
        }

        // Calculate weighted average (recent months weighted more)
        return monthlyTotals.mapValues { (_, values) ->
            if (values.isEmpty()) 0.0
            else {
                val weights = listOf(0.2, 0.3, 0.5).take(values.size)
                values.zip(weights) { v, w -> v * w }.sum() / weights.sum()
            }
        }
    }

    /**
     * Calculate ideal allocations based on 50/30/20 rule
     */
    private fun calculateIdealAllocations(
        monthlyIncome: Double,
        savingsGoal: Double
    ): Map<String, Double> {
        val adjustedSavings = max(SAVINGS_PERCENTAGE, savingsGoal)
        val remaining = 1.0 - adjustedSavings

        // Adjust needs and wants proportionally
        val needsRatio = NEEDS_PERCENTAGE / (NEEDS_PERCENTAGE + WANTS_PERCENTAGE)
        val wantsRatio = WANTS_PERCENTAGE / (NEEDS_PERCENTAGE + WANTS_PERCENTAGE)

        return mapOf(
            "needs" to monthlyIncome * remaining * needsRatio,
            "wants" to monthlyIncome * remaining * wantsRatio,
            "savings" to monthlyIncome * adjustedSavings
        )
    }

    /**
     * Generate category-specific budget recommendations
     */
    private fun generateCategoryBudgets(
        monthlyIncome: Double,
        currentSpending: Map<ExpenseCategory, Double>,
        idealAllocations: Map<String, Double>
    ): Map<ExpenseCategory, Double> {
        val budgets = mutableMapOf<ExpenseCategory, Double>()

        // Calculate total current spending by type
        val currentNeeds = currentSpending.filterKeys { it in NEEDS_CATEGORIES }.values.sum()
        val currentWants = currentSpending.filterKeys { it in WANTS_CATEGORIES }.values.sum()

        val idealNeeds = idealAllocations["needs"] ?: 0.0
        val idealWants = idealAllocations["wants"] ?: 0.0
        val idealSavings = idealAllocations["savings"] ?: 0.0

        // Distribute needs budget proportionally
        if (currentNeeds > 0) {
            NEEDS_CATEGORIES.forEach { category ->
                val currentAmount = currentSpending[category] ?: 0.0
                val proportion = currentAmount / currentNeeds
                // Allow slight flexibility for needs (up to 10% over ideal)
                val categoryBudget = min(
                    currentAmount * 1.1,  // Don't cut essential needs too drastically
                    idealNeeds * proportion * 1.1
                )
                budgets[category] = max(categoryBudget, currentAmount * 0.8) // At least 80% of current
            }
        }

        // Distribute wants budget proportionally (more aggressive cuts)
        if (currentWants > 0) {
            WANTS_CATEGORIES.forEach { category ->
                val currentAmount = currentSpending[category] ?: 0.0
                val proportion = currentAmount / currentWants
                // Be more aggressive with wants reduction
                val categoryBudget = idealWants * proportion
                budgets[category] = max(categoryBudget, currentAmount * 0.5) // At least 50% of current
            }
        }

        // Set savings/investment targets
        SAVINGS_CATEGORIES.forEach { category ->
            val currentAmount = currentSpending[category] ?: 0.0
            // Encourage increasing savings
            budgets[category] = max(
                currentAmount,
                idealSavings / SAVINGS_CATEGORIES.size
            )
        }

        // Handle OTHER category
        budgets[ExpenseCategory.OTHER] = (currentSpending[ExpenseCategory.OTHER] ?: 0.0) * 0.8

        return budgets
    }

    /**
     * Generate actionable budget insights
     */
    private fun generateBudgetInsights(
        currentSpending: Map<ExpenseCategory, Double>,
        recommendedBudgets: Map<ExpenseCategory, Double>,
        monthlyIncome: Double
    ): List<BudgetInsight> {
        val insights = mutableListOf<BudgetInsight>()

        // Check overall spending vs income
        val totalSpending = currentSpending.values.sum()
        val spendingRate = if (monthlyIncome > 0) totalSpending / monthlyIncome else 0.0

        if (spendingRate > 1.0) {
            insights.add(BudgetInsight(
                type = InsightType.CRITICAL,
                title = "Overspending Alert",
                message = "You're spending ${((spendingRate - 1) * 100).toInt()}% more than your income!",
                action = "Review all categories and cut non-essential spending immediately."
            ))
        } else if (spendingRate > 0.9) {
            insights.add(BudgetInsight(
                type = InsightType.WARNING,
                title = "Low Savings Warning",
                message = "You're only saving ${((1 - spendingRate) * 100).toInt()}% of your income.",
                action = "Aim for at least 20% savings rate."
            ))
        }

        // Find categories significantly over ideal
        currentSpending.forEach { (category, current) ->
            val recommended = recommendedBudgets[category] ?: current
            if (current > recommended * 1.2) { // More than 20% over recommended
                val overage = ((current - recommended) / recommended * 100).toInt()
                insights.add(BudgetInsight(
                    type = InsightType.SUGGESTION,
                    title = "${category.displayName} Optimization",
                    message = "Spending $overage% more than recommended in ${category.displayName}.",
                    action = "Reduce to $${String.format("%.0f", recommended)}/month to save $${String.format("%.0f", current - recommended)}."
                ))
            }
        }

        // Check savings categories
        val savingsSpending = currentSpending.filterKeys { it in SAVINGS_CATEGORIES }.values.sum()
        val idealSavings = monthlyIncome * SAVINGS_PERCENTAGE
        if (savingsSpending < idealSavings * 0.5) {
            insights.add(BudgetInsight(
                type = InsightType.SUGGESTION,
                title = "Boost Your Savings",
                message = "Currently saving ${if (monthlyIncome > 0) (savingsSpending / monthlyIncome * 100).toInt() else 0}% of income.",
                action = "Increase automatic savings to reach 20% goal."
            ))
        }

        return insights.take(5) // Limit to top 5 insights
    }

    /**
     * Predict budget adherence for next month
     */
    fun predictBudgetAdherence(
        budget: Budget,
        expenses: List<Expense>
    ): AdherencePrediction {
        val categoryExpenses = expenses.filter { it.category == budget.category }

        if (categoryExpenses.isEmpty()) {
            return AdherencePrediction(
                predictedSpending = 0.0,
                budgetLimit = budget.limit,
                predictedAdherence = 1.0,
                risk = AdherenceRisk.LOW
            )
        }

        // Calculate average daily spending
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis

        val currentMonthExpenses = categoryExpenses
            .filter { it.date >= monthStart }
            .sumOf { it.amount }

        val dailyRate = if (dayOfMonth > 0) currentMonthExpenses / dayOfMonth else 0.0
        val predictedTotal = dailyRate * daysInMonth

        val adherence = if (budget.limit > 0) {
            1.0 - min(1.0, max(0.0, (predictedTotal - budget.limit) / budget.limit))
        } else 1.0

        val risk = when {
            predictedTotal > budget.limit * 1.2 -> AdherenceRisk.HIGH
            predictedTotal > budget.limit -> AdherenceRisk.MEDIUM
            predictedTotal > budget.limit * 0.8 -> AdherenceRisk.LOW
            else -> AdherenceRisk.NONE
        }

        return AdherencePrediction(
            predictedSpending = predictedTotal,
            budgetLimit = budget.limit,
            predictedAdherence = adherence,
            risk = risk
        )
    }
}

// Data classes
data class BudgetRecommendation(
    val recommendedBudgets: Map<ExpenseCategory, Double>,
    val totalBudget: Double,
    val potentialMonthlySavings: Double,
    val savingsRate: Double,
    val insights: List<BudgetInsight>,
    val allocationBreakdown: AllocationBreakdown
)

data class AllocationBreakdown(
    val needs: Double,
    val wants: Double,
    val savings: Double
)

data class BudgetInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val action: String
)

enum class InsightType {
    CRITICAL, WARNING, SUGGESTION, POSITIVE
}

data class AdherencePrediction(
    val predictedSpending: Double,
    val budgetLimit: Double,
    val predictedAdherence: Double,
    val risk: AdherenceRisk
)

enum class AdherenceRisk {
    NONE, LOW, MEDIUM, HIGH
}

