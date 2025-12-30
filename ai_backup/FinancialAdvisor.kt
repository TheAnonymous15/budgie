package com.example.budgie.ai

import android.content.Context
import com.example.budgie.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

/**
 * Financial Advisor - Comprehensive financial guidance engine
 * Uses rule-based analysis and template-based NLG for reliable insights
 */
class FinancialAdvisor(private val context: Context) {

    fun getSystemPrompt(data: String): String {
        return """
        You are a helpful and friendly AI assistant for an application called "Budgie".
        Your mission is to provide financial advice and answer questions based on the user's spending data.

        You should be able to:
        - Analyze spending patterns and identify trends.
        - Identify areas where the user can save money.
        - Answer questions about the user's spending history.

        Here is the user's financial data:
        $data
        """
    }

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
        val savingsRate = if (summary.totalIncome > 0) {
            (summary.netSavings / summary.totalIncome) * 100
        } else 0.0

        insights.add(
            when {
                savingsRate < 0 -> SpendingInsight(
                    type = InsightType.OVERSPENDING,
                    title = "Overspending Alert",
                    description = "You're spending more than you earn this month.",
                    actionable = "Review your expenses and find areas to cut back.",
                    priority = InsightPriority.CRITICAL
                )
                savingsRate < 10 -> SpendingInsight(
                    type = InsightType.SAVING_OPPORTUNITY,
                    title = "Low Savings Rate",
                    description = "Your savings rate is ${String.format("%.1f", savingsRate)}%",
                    actionable = "Try to save at least 20% of your income.",
                    priority = InsightPriority.HIGH
                )
                savingsRate < 20 -> SpendingInsight(
                    type = InsightType.TREND_ANALYSIS,
                    title = "Good Progress",
                    description = "Savings rate: ${String.format("%.1f", savingsRate)}%",
                    actionable = "You're doing well! Aim for 20% to build wealth faster.",
                    priority = InsightPriority.MEDIUM
                )
                else -> SpendingInsight(
                    type = InsightType.GOOD_HABIT,
                    title = "Excellent Savings!",
                    description = "Savings rate: ${String.format("%.1f", savingsRate)}%",
                    actionable = "Great job! Consider investing your surplus.",
                    priority = InsightPriority.LOW
                )
            }
        )

        // Category-specific insights
        val categorySpending = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }

        val totalSpending = summary.totalExpenses

        categorySpending.forEach { (category, amount) ->
            val percentage = if (totalSpending > 0) (amount / totalSpending) * 100 else 0.0

            if (percentage > 40) {
                insights.add(SpendingInsight(
                    type = InsightType.OVERSPENDING,
                    title = "High ${category.displayName} Spending",
                    description = "You spent ${String.format("%.0f", percentage)}% of your total on ${category.displayName.lowercase()}.",
                    actionable = "Consider reviewing these expenses for potential savings.",
                    potentialSavings = amount * 0.2,
                    priority = InsightPriority.HIGH
                ))
            }
        }

        // Budget insights
        budgets.forEach { budget ->
            val spent = categorySpending[budget.category] ?: 0.0
            val utilization = if (budget.limit > 0) (spent / budget.limit) * 100 else 0.0

            when {
                utilization > 100 -> insights.add(SpendingInsight(
                    type = InsightType.BUDGET_WARNING,
                    title = "${budget.category.displayName} Budget Exceeded",
                    description = "You've spent ${String.format("%.0f", utilization)}% of your ${budget.category.displayName} budget.",
                    actionable = "Reduce spending in this category immediately.",
                    potentialSavings = spent - budget.limit,
                    priority = InsightPriority.CRITICAL
                ))
                utilization > 80 -> insights.add(SpendingInsight(
                    type = InsightType.BUDGET_WARNING,
                    title = "${budget.category.displayName} Budget Alert",
                    description = "You've used ${String.format("%.0f", utilization)}% of your ${budget.category.displayName} budget.",
                    actionable = "Be careful with remaining budget.",
                    priority = InsightPriority.MEDIUM
                ))
            }
        }

        // General tips
        if (insights.size < 3) {
            insights.add(SpendingInsight(
                type = InsightType.SAVING_OPPORTUNITY,
                title = "Financial Tip",
                description = "Track every expense to understand your spending habits better.",
                actionable = "Small expenses add up - consider using the 24-hour rule for non-essential purchases.",
                priority = InsightPriority.LOW
            ))
        }

        return insights.take(5)
    }

    /**
     * Generate investment suggestions based on user profile
     */
    fun generateInvestmentSuggestions(
        summary: FinancialSummary,
        currentSavings: Double,
        riskLevel: RiskLevel,
        userAge: Int
    ): List<InvestmentSuggestion> {
        val suggestions = mutableListOf<InvestmentSuggestion>()
        val monthlySurplus = summary.netSavings.coerceAtLeast(0.0)

        // Calculate recommended allocation based on age and risk
        val stockAllocation = when (riskLevel) {
            RiskLevel.LOW -> (100 - userAge - 10).coerceIn(20, 50)
            RiskLevel.MEDIUM -> (100 - userAge).coerceIn(30, 70)
            RiskLevel.HIGH -> (110 - userAge).coerceIn(40, 90)
        }

        if (monthlySurplus > 0) {
            suggestions.add(InvestmentSuggestion(
                type = InvestmentType.ETF,
                title = "Index Fund Portfolio",
                description = "Diversified stock index fund tracking the market.",
                riskLevel = riskLevel,
                expectedReturn = "8% annual",
                minimumAmount = monthlySurplus * (stockAllocation / 100.0),
                suitabilityScore = 85
            ))

            suggestions.add(InvestmentSuggestion(
                type = InvestmentType.BONDS,
                title = "Bond Fund",
                description = "Stable returns with lower volatility.",
                riskLevel = RiskLevel.LOW,
                expectedReturn = "4% annual",
                minimumAmount = monthlySurplus * ((100 - stockAllocation) / 100.0),
                suitabilityScore = 80
            ))
        }

        // Emergency fund suggestion
        val emergencyFundTarget = summary.totalExpenses * 6
        if (currentSavings < emergencyFundTarget) {
            suggestions.add(0, InvestmentSuggestion(
                type = InvestmentType.EMERGENCY_FUND,
                title = "Emergency Fund",
                description = "Build 6 months of expenses before investing.",
                riskLevel = RiskLevel.LOW,
                expectedReturn = "4% annual",
                minimumAmount = (emergencyFundTarget - currentSavings).coerceAtLeast(0.0),
                suitabilityScore = 95
            ))
        }

        return suggestions
    }

    /**
     * Calculate wealth projection based on current status
     */
    fun calculateWealthProjection(
        currentNetWorth: Double,
        monthlyContribution: Double,
        annualReturn: Double = 0.07
    ): WealthProjection {
        val monthlyReturn = annualReturn / 12

        fun projectValue(months: Int): Double {
            var value = currentNetWorth
            repeat(months) {
                value = value * (1 + monthlyReturn) + monthlyContribution
            }
            return value
        }

        val projections = mapOf(
            1 to projectValue(12),
            5 to projectValue(60),
            10 to projectValue(120)
        )

        return WealthProjection(
            currentNetWorth = currentNetWorth,
            projectedNetWorth = projections,
            monthlyContribution = monthlyContribution,
            assumedReturnRate = annualReturn
        )
    }

    /**
     * Process a question and generate a response using rule-based templates
     */
    fun ask(question: String): Flow<String> = flow {
        delay(500) // Simulate thinking time
        val response = generateResponse(question)
        emit(response)
    }

    private fun generateResponse(question: String): String {
        val q = question.lowercase()

        return when {
            q.contains("spend") || q.contains("expense") -> {
                "Based on your data, I can help analyze your spending patterns. " +
                "Use the dashboard to see detailed breakdowns by category."
            }
            q.contains("save") || q.contains("saving") -> {
                "Great question about savings! The key is to track your expenses " +
                "consistently and aim for the 50/30/20 rule: 50% needs, 30% wants, 20% savings."
            }
            q.contains("budget") -> {
                "Budgeting is essential for financial health. Start by listing your " +
                "fixed expenses, then allocate funds for variable expenses and savings."
            }
            q.contains("income") -> {
                "Your income forms the foundation of your financial plan. " +
                "Make sure to track all income sources for accurate insights."
            }
            q.contains("advice") || q.contains("tip") -> {
                "Here are some financial tips:\n" +
                "1. Track every expense\n" +
                "2. Build an emergency fund\n" +
                "3. Avoid unnecessary debt\n" +
                "4. Invest for the future\n" +
                "5. Review your finances monthly"
            }
            else -> {
                "I'm here to help with your financial questions. " +
                "Ask me about spending, saving, budgeting, or income!"
            }
        }
    }
}