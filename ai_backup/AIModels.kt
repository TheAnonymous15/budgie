package com.example.budgie.ai

import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.InsightPriority

/**
 * AI Analysis Models for Budgie Financial Advisor
 */

// Financial Health Score
data class FinancialHealthScore(
    val overallScore: Int,
    val status: HealthStatus,
    val factors: List<ScoreFactor>,
    val savingsRate: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double
)

data class ScoreFactor(
    val name: String,
    val score: Double,
    val maxScore: Double,
    val description: String,
    val status: FactorStatus
)

enum class HealthStatus {
    EXCELLENT, GOOD, FAIR, NEEDS_ATTENTION
}

enum class FactorStatus {
    EXCELLENT, GOOD, FAIR, POOR
}

// Spending Patterns
data class SpendingPatterns(
    val categoryBreakdown: Map<ExpenseCategory, Double>,
    val dailyAverage: Double,
    val weeklyAverage: Double,
    val monthlyAverage: Double,
    val peakSpendingDays: List<String>,
    val peakSpendingCategories: List<ExpenseCategory>,
    val trendDirection: TrendDirection,
    val volatility: Double
)

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

// Anomaly Detection
data class SpendingAnomaly(
    val expense: Expense,
    val zScore: Double,
    val expectedAmount: Double,
    val severity: AnomalySeverity,
    val type: AnomalyType,
    val message: String
)

enum class AnomalySeverity {
    LOW, MEDIUM, HIGH
}

enum class AnomalyType {
    UNUSUALLY_HIGH, UNUSUALLY_LOW
}

// Predictions
data class FinancialPredictions(
    val predictedMonthlyExpenses: Double,
    val expenseConfidence: Double,
    val predictedMonthlyIncome: Double,
    val incomeConfidence: Double,
    val predictedSavings: Double,
    val categoryPredictions: Map<ExpenseCategory, Double>,
    val historicalExpenses: List<Double>,
    val historicalIncome: List<Double>
)

// AI Insights
data class AIInsight(
    val id: String,
    val title: String,
    val message: String,
    val category: InsightCategory,
    val priority: InsightPriority,
    val actionable: Boolean,
    val action: String?
)

enum class InsightCategory {
    SAVINGS, SPENDING, CATEGORY_ANALYSIS, ANOMALY, BILLS, STABILITY, PATTERNS, PREDICTION, ACHIEVEMENT
}

// InsightPriority is defined in data.model.FinancialModels

// Recommendations
data class AIRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val impact: RecommendationImpact,
    val difficulty: RecommendationDifficulty,
    val timeframe: String
)

enum class RecommendationImpact {
    LOW, MEDIUM, HIGH
}

enum class RecommendationDifficulty {
    EASY, MEDIUM, HARD
}

// Complete Analysis
data class FinancialAnalysis(
    val healthScore: FinancialHealthScore,
    val spendingPatterns: SpendingPatterns,
    val anomalies: List<SpendingAnomaly>,
    val predictions: FinancialPredictions,
    val insights: List<AIInsight>,
    val recommendations: List<AIRecommendation>,
    val timestamp: Long
)

// Chat Message
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val suggestions: List<String> = emptyList()
)

