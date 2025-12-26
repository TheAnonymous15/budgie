package com.example.budgie.data.model

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val expensesByCategory: Map<ExpenseCategory, Double> = emptyMap(),
    val monthlyAvgExpense: Double = 0.0,
    val projectedYearlyExpense: Double = 0.0
)

data class WealthProjection(
    val currentNetWorth: Double = 0.0,
    val projectedNetWorth: Map<Int, Double> = emptyMap(),
    val monthlyContribution: Double = 0.0,
    val assumedReturnRate: Double = 0.07
)

data class InvestmentSuggestion(
    val type: InvestmentType,
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
    val expectedReturn: String,
    val minimumAmount: Double,
    val suitabilityScore: Int
)

enum class InvestmentType(val displayName: String, val icon: String) {
    STOCKS("Stocks", "📈"),
    BONDS("Bonds", "📊"),
    MUTUAL_FUNDS("Mutual Funds", "💹"),
    ETF("ETFs", "🔄"),
    REAL_ESTATE("Real Estate", "🏘️"),
    CRYPTO("Cryptocurrency", "₿"),
    SAVINGS_ACCOUNT("High-Yield Savings", "🏦"),
    RETIREMENT("Retirement Account", "👴"),
    EMERGENCY_FUND("Emergency Fund", "🆘")
}

enum class RiskLevel(val displayName: String, val color: Long) {
    LOW("Low Risk", 0xFF4CAF50),
    MEDIUM("Medium Risk", 0xFFFF9800),
    HIGH("High Risk", 0xFFF44336)
}

data class SpendingInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val actionable: String,
    val potentialSavings: Double = 0.0,
    val priority: InsightPriority = InsightPriority.MEDIUM
)

enum class InsightType {
    OVERSPENDING,
    GOOD_HABIT,
    SAVING_OPPORTUNITY,
    BUDGET_WARNING,
    TREND_ANALYSIS,
    COMPARISON
}

enum class InsightPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}
