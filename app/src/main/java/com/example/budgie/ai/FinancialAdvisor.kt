package com.example.budgie.ai

import android.content.Context
import com.example.budgie.data.model.*

/**
 * FinancialAdvisor - Provides basic financial insights
 * Stub implementation with rule-based logic
 */
class FinancialAdvisor(private val context: Context) {

    /**
     * Generate spending insights based on financial data
     */
    fun generateSpendingInsights(
        summary: FinancialSummary,
        previousSummary: FinancialSummary?,
        budgets: List<Budget>,
        expenses: List<Expense>
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()

        // Savings rate insight
        if (summary.totalIncome > 0) {
            val savingsRate = summary.savingsRate
            insights.add(
                SpendingInsight(
                    type = InsightType.TREND_ANALYSIS,
                    title = "Savings Rate",
                    description = if (savingsRate >= 20)
                        "Great job! You're saving ${String.format("%.1f", savingsRate)}% of your income."
                    else
                        "Your savings rate is ${String.format("%.1f", savingsRate)}%. Consider aiming for 20%.",
                    actionable = if (savingsRate < 20) "Review spending to find savings" else "Keep up the great work!",
                    priority = if (savingsRate < 10) InsightPriority.HIGH
                              else if (savingsRate < 20) InsightPriority.MEDIUM
                              else InsightPriority.LOW
                )
            )
        }

        // Budget alerts
        budgets.forEach { budget ->
            val spent = expenses.filter { expense ->
                expense.category == budget.category
            }.sumOf { it.amount }
            val percentage = if (budget.limit > 0) (spent / budget.limit) * 100 else 0.0

            if (percentage >= 90) {
                insights.add(
                    SpendingInsight(
                        type = InsightType.BUDGET_WARNING,
                        title = "${budget.category} Budget Alert",
                        description = "You've used ${String.format("%.0f", percentage)}% of your ${budget.category} budget.",
                        actionable = "Review ${budget.category} spending",
                        priority = if (percentage >= 100) InsightPriority.HIGH else InsightPriority.MEDIUM
                    )
                )
            }
        }

        // Top spending category
        val categorySpending = expenses.groupBy { it.category }
            .mapValues { (_, exps) -> exps.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        if (categorySpending.isNotEmpty()) {
            val topCategory = categorySpending.first()
            insights.add(
                SpendingInsight(
                    type = InsightType.TREND_ANALYSIS,
                    title = "Top Spending: ${topCategory.first}",
                    description = "You spent $${String.format("%.2f", topCategory.second)} on ${topCategory.first} this month.",
                    actionable = "Review ${topCategory.first} expenses",
                    priority = InsightPriority.LOW
                )
            )
        }

        return insights.take(5)
    }

    /**
     * Generate investment suggestions
     */
    fun generateInvestmentSuggestions(
        summary: FinancialSummary,
        currentSavings: Double,
        riskTolerance: RiskLevel,
        age: Int
    ): List<InvestmentSuggestion> {
        val suggestions = mutableListOf<InvestmentSuggestion>()

        // Emergency fund suggestion
        val monthlyExpenses = summary.totalExpenses
        val emergencyFundTarget = monthlyExpenses * 6

        suggestions.add(
            InvestmentSuggestion(
                type = InvestmentType.EMERGENCY_FUND,
                title = "Emergency Fund",
                description = "Build 6 months of expenses (\$${String.format("%.0f", emergencyFundTarget)})",
                riskLevel = RiskLevel.LOW,
                expectedReturn = "2-3%",
                minimumAmount = 100.0,
                suitabilityScore = 95
            )
        )

        // Based on risk tolerance
        if (currentSavings >= emergencyFundTarget && summary.netSavings > 0) {
            when (riskTolerance) {
                RiskLevel.LOW -> {
                    suggestions.add(
                        InvestmentSuggestion(
                            type = InvestmentType.SAVINGS_ACCOUNT,
                            title = "High-Yield Savings",
                            description = "Safe returns with easy access",
                            riskLevel = RiskLevel.LOW,
                            expectedReturn = "4-5%",
                            minimumAmount = 100.0,
                            suitabilityScore = 90
                        )
                    )
                }
                RiskLevel.MEDIUM -> {
                    suggestions.add(
                        InvestmentSuggestion(
                            type = InvestmentType.ETF,
                            title = "Index Funds",
                            description = "Diversified stock market exposure",
                            riskLevel = RiskLevel.MEDIUM,
                            expectedReturn = "7-10%",
                            minimumAmount = 500.0,
                            suitabilityScore = 85
                        )
                    )
                }
                RiskLevel.HIGH -> {
                    suggestions.add(
                        InvestmentSuggestion(
                            type = InvestmentType.STOCKS,
                            title = "Growth Stocks",
                            description = "Higher risk, higher potential returns",
                            riskLevel = RiskLevel.HIGH,
                            expectedReturn = "10-15%",
                            minimumAmount = 1000.0,
                            suitabilityScore = 75
                        )
                    )
                }
            }
        }

        return suggestions
    }

    /**
     * Calculate wealth projection
     */
    fun calculateWealthProjection(
        currentNetWorth: Double,
        monthlyContribution: Double,
        annualReturnRate: Double = 0.07,
        yearsToProject: Int = 30
    ): WealthProjection {
        val projections = mutableMapOf<Int, Double>()
        var balance = currentNetWorth
        val monthlyRate = annualReturnRate / 12

        for (year in 1..yearsToProject) {
            for (month in 1..12) {
                balance = balance * (1 + monthlyRate) + monthlyContribution
            }
            projections[year] = balance
        }

        return WealthProjection(
            currentNetWorth = currentNetWorth,
            projectedNetWorth = projections,
            monthlyContribution = monthlyContribution,
            assumedReturnRate = annualReturnRate
        )
    }
}

