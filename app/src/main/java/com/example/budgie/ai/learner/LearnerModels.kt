package com.example.budgie.ai.learner

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * BUDGIE SUPER LEARNER - DATA MODELS
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Lightweight data structures optimized for mobile inference.
 * No external ML dependencies - pure Kotlin mathematics.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// CORE FINANCIAL STATE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Complete user financial state - the output of all learning
 * This is what the chatbot queries to answer user questions
 */
data class UserFinancialProfile(
    // Basic metrics
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val dailyAverageSpend: Double = 0.0,
    val savingsRate: Double = 0.0,

    // Category breakdown
    val categorySpending: List<CategorySpendProfile> = emptyList(),

    // Behavioral profile
    val spenderType: SpenderType = SpenderType.UNKNOWN,
    val riskProfile: FinancialRiskProfile = FinancialRiskProfile(),

    // Patterns detected
    val spendingPatterns: List<DetectedPattern> = emptyList(),
    val anomalies: List<SpendingAnomaly> = emptyList(),

    // Predictions
    val predictions: FinancialPredictions = FinancialPredictions(),

    // Bills & recurring
    val upcomingBills: List<PredictedBill> = emptyList(),

    // Metadata
    val lastUpdated: Long = System.currentTimeMillis(),
    val dataQuality: DataQuality = DataQuality.INSUFFICIENT
)

data class CategorySpendProfile(
    val category: String,
    val totalAmount: Double,
    val percentage: Double,
    val average: Double,
    val stdDev: Double,
    val trend: Trend = Trend.STABLE,
    val anomalyScore: Float = 0f
)

enum class SpenderType(val display: String, val emoji: String) {
    SAVER("Disciplined Saver", "💰"),
    BALANCED("Balanced Spender", "⚖️"),
    MODERATE("Moderate Spender", "📊"),
    HIGH_SPENDER("High Spender", "📈"),
    UNKNOWN("Not Enough Data", "❓")
}

enum class Trend(val display: String, val multiplier: Float) {
    DECREASING("Decreasing", -1f),
    STABLE("Stable", 0f),
    INCREASING("Increasing", 1f),
    VOLATILE("Volatile", 0.5f)
}

enum class DataQuality(val minDays: Int) {
    INSUFFICIENT(0),      // < 7 days
    MINIMAL(7),           // 7-14 days
    MODERATE(14),         // 14-30 days
    GOOD(30),             // 30-60 days
    EXCELLENT(60)         // 60+ days
}

// ═══════════════════════════════════════════════════════════════════════════════
// RISK & PREDICTIONS
// ═══════════════════════════════════════════════════════════════════════════════

data class FinancialRiskProfile(
    val overallScore: Float = 0f,           // 0-100 (0=low risk, 100=high risk)
    val level: RiskLevel = RiskLevel.UNKNOWN,
    val factors: List<RiskFactor> = emptyList(),
    val recommendations: List<String> = emptyList()
)

enum class RiskLevel(val display: String, val color: Long) {
    LOW("Low Risk", 0xFF4CAF50),
    MODERATE("Moderate", 0xFFFF9800),
    HIGH("High Risk", 0xFFF44336),
    CRITICAL("Critical", 0xFFD32F2F),
    UNKNOWN("Unknown", 0xFF9E9E9E)
}

data class RiskFactor(
    val name: String,
    val score: Float,           // 0-1 contribution to risk
    val description: String,
    val actionable: String
)

data class FinancialPredictions(
    // Short-term
    val nextDaySpend: Double = 0.0,
    val nextWeekSpend: Double = 0.0,
    val nextMonthSpend: Double = 0.0,

    // Balance predictions
    val endOfMonthBalance: Double = 0.0,
    val endOfWeekBalance: Double = 0.0,

    // Probability predictions
    val budgetOverrunProbability: Float = 0f,
    val savingsGoalProbability: Float = 0f,

    // Confidence
    val confidence: Float = 0f
)

data class PredictedBill(
    val name: String,
    val amount: Double,
    val predictedDate: Long,
    val daysUntilDue: Int,
    val confidence: Float,
    val isRecurring: Boolean = true
)

// ═══════════════════════════════════════════════════════════════════════════════
// PATTERN DETECTION
// ═══════════════════════════════════════════════════════════════════════════════

data class DetectedPattern(
    val type: PatternType,
    val description: String,
    val confidence: Float,
    val category: String? = null,
    val amount: Double? = null,
    val dayOfWeek: Int? = null,      // 1=Sunday, 7=Saturday
    val dayOfMonth: Int? = null,
    val frequency: PatternFrequency? = null
)

enum class PatternType {
    WEEKDAY_SPIKE,          // Spends more on certain days
    WEEKEND_SPIKE,          // Weekend spending pattern
    PAYDAY_SURGE,           // Spike after income
    END_OF_MONTH_SQUEEZE,   // Low spending at month end
    CATEGORY_ADDICTION,     // Unusually high in one category
    RECURRING_EXPENSE,      // Detected recurring payment
    SEASONAL,               // Seasonal pattern
    IMPULSE_BUYING          // Random high-value purchases
}

enum class PatternFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    IRREGULAR
}

// ═══════════════════════════════════════════════════════════════════════════════
// ANOMALY DETECTION
// ═══════════════════════════════════════════════════════════════════════════════

data class SpendingAnomaly(
    val type: AnomalyType,
    val category: String,
    val amount: Double,
    val expectedAmount: Double,
    val deviation: Float,           // Standard deviations from mean
    val severity: AnomalySeverity,
    val date: Long,
    val description: String
)

enum class AnomalyType {
    UNUSUAL_AMOUNT,         // Amount far from normal
    UNUSUAL_CATEGORY,       // Category user rarely uses
    UNUSUAL_FREQUENCY,      // Too many transactions
    UNUSUAL_TIME,           // Transaction at odd time
    POTENTIAL_FRAUD         // Suspicious pattern
}

enum class AnomalySeverity(val threshold: Float) {
    LOW(1.5f),              // 1.5 std deviations
    MEDIUM(2.0f),           // 2.0 std deviations
    HIGH(2.5f),             // 2.5 std deviations
    CRITICAL(3.0f)          // 3+ std deviations
}

// ═══════════════════════════════════════════════════════════════════════════════
// GOAL & LOAN PREDICTIONS
// ═══════════════════════════════════════════════════════════════════════════════

data class GoalFeasibility(
    val goalName: String,
    val targetAmount: Double,
    val currentSaved: Double,
    val monthlyRequired: Double,
    val probability: Float,             // 0-1 chance of success
    val estimatedCompletionDate: Long,
    val daysToGoal: Int,
    val feasibilityLevel: FeasibilityLevel,
    val recommendations: List<String>
)

enum class FeasibilityLevel(val display: String) {
    EASY("Easily Achievable"),
    MODERATE("Achievable with Discipline"),
    CHALLENGING("Challenging"),
    DIFFICULT("Very Difficult"),
    UNREALISTIC("Currently Unrealistic")
}

data class LoanAffordability(
    val loanAmount: Double,
    val monthlyPayment: Double,
    val affordabilityScore: Float,      // 0-100
    val debtToIncomeRatio: Float,
    val canAfford: Boolean,
    val riskLevel: RiskLevel,
    val maxAffordablePayment: Double,
    val recommendations: List<String>
)

// ═══════════════════════════════════════════════════════════════════════════════
// INTERNAL LEARNING STRUCTURES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Internal expense record for learning (lightweight)
 */
internal data class ExpenseRecord(
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val dayOfWeek: Int,         // 1-7
    val dayOfMonth: Int,        // 1-31
    val weekOfYear: Int,
    val month: Int,             // 0-11
    val hourOfDay: Int          // 0-23
)

/**
 * Internal income record for learning
 */
internal data class IncomeRecord(
    val amount: Double,
    val source: String,
    val timestamp: Long,
    val isRecurring: Boolean
)

/**
 * Statistical summary for a category
 */
internal data class CategoryStats(
    val category: String,
    val count: Int,
    val sum: Double,
    val mean: Double,
    val variance: Double,
    val stdDev: Double,
    val min: Double,
    val max: Double,
    val median: Double
)

/**
 * Time-series data point for predictions
 */
internal data class TimeSeriesPoint(
    val timestamp: Long,
    val value: Double,
    val smoothedValue: Double = value
)

