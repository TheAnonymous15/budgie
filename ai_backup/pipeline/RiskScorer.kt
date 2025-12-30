package com.example.budgie.ai.pipeline

import android.content.Context
import com.example.budgie.data.model.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

/**
 * Stage 6 & 7: Goal/Budget Risk Scoring + Multimodal Fusion
 *
 * Risk Scoring: Rule engine + ML outputs (NO pure ML - intentional for explainability)
 * Fusion Layer: Logistic Regression / Small MLP to combine signals
 *
 * Purpose: Convert predictions → actionable risk scores with confidence
 */
class RiskScorer(private val context: Context) {

    // Fusion layer weights (learned from data patterns)
    // Input: [behaviorScore, anomalyScore, forecastDelta, budgetAdherence, savingsRate, trendScore]
    private val fusionWeights = floatArrayOf(0.15f, 0.25f, 0.20f, 0.20f, 0.15f, 0.05f)
    private val fusionBias = -0.3f

    // Category risk multipliers (some categories are inherently riskier)
    private val categoryRiskMultipliers = mapOf(
        ExpenseCategory.ENTERTAINMENT to 1.3f,
        ExpenseCategory.SHOPPING to 1.2f,
        ExpenseCategory.FOOD to 1.1f,
        ExpenseCategory.TRANSPORT to 1.0f,
        ExpenseCategory.UTILITIES to 0.9f,
        ExpenseCategory.RENT to 0.8f,
        ExpenseCategory.HEALTH to 0.9f,
        ExpenseCategory.EDUCATION to 0.95f,
        ExpenseCategory.INSURANCE to 0.8f,
        ExpenseCategory.INVESTMENT to 0.7f,
        ExpenseCategory.SAVINGS to 0.5f,
        ExpenseCategory.OTHER to 1.1f
    )

    companion object {
        // Risk thresholds
        private const val LOW_RISK_THRESHOLD = 0.3f
        private const val MEDIUM_RISK_THRESHOLD = 0.6f
        private const val HIGH_RISK_THRESHOLD = 0.8f

        @Volatile
        private var INSTANCE: RiskScorer? = null

        fun getInstance(context: Context): RiskScorer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RiskScorer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Calculate comprehensive risk score combining all ML outputs
     */
    suspend fun calculateRisk(
        behaviorProfile: BehaviorProfile,
        anomalyScores: List<AnomalyScore>,
        forecast: SpendingForecast,
        featureVector: FinancialFeatureVector,
        userGoals: UserGoals?
    ): RiskAssessment = withContext(Dispatchers.Default) {

        // Extract signals
        val behaviorSignal = calculateBehaviorSignal(behaviorProfile)
        val anomalySignal = calculateAnomalySignal(anomalyScores)
        val forecastSignal = calculateForecastSignal(forecast)
        val budgetSignal = calculateBudgetSignal(featureVector)
        val savingsSignal = calculateSavingsSignal(featureVector)
        val trendSignal = calculateTrendSignal(forecast)

        // Fusion layer: Weighted combination
        val fusionInput = floatArrayOf(
            behaviorSignal,
            anomalySignal,
            forecastSignal,
            budgetSignal,
            savingsSignal,
            trendSignal
        )

        val rawRiskScore = fusionLayer(fusionInput)

        // Apply goal-based adjustments
        val goalAdjustedScore = applyGoalAdjustments(rawRiskScore, featureVector, userGoals)

        // Determine risk level
        val riskLevel = when {
            goalAdjustedScore >= HIGH_RISK_THRESHOLD -> RiskLevel.HIGH
            goalAdjustedScore >= MEDIUM_RISK_THRESHOLD -> RiskLevel.MEDIUM
            goalAdjustedScore >= LOW_RISK_THRESHOLD -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }

        // Calculate confidence
        val confidence = calculateConfidence(fusionInput)

        // Identify risk factors
        val riskFactors = identifyRiskFactors(
            fusionInput,
            behaviorProfile,
            anomalyScores,
            forecast
        )

        // Generate category-specific risks
        val categoryRisks = calculateCategoryRisks(featureVector)

        RiskAssessment(
            overallScore = goalAdjustedScore,
            riskLevel = riskLevel,
            confidence = confidence,
            riskFactors = riskFactors,
            categoryRisks = categoryRisks,
            signals = RiskSignals(
                behaviorSignal = behaviorSignal,
                anomalySignal = anomalySignal,
                forecastSignal = forecastSignal,
                budgetSignal = budgetSignal,
                savingsSignal = savingsSignal,
                trendSignal = trendSignal
            ),
            recommendations = generateRiskRecommendations(riskLevel, riskFactors)
        )
    }

    /**
     * Fusion layer: Logistic regression combining all signals
     */
    private fun fusionLayer(inputs: FloatArray): Float {
        var sum = fusionBias
        for (i in inputs.indices) {
            sum += inputs[i] * fusionWeights[i]
        }
        return sigmoid(sum)
    }

    /**
     * Calculate behavior risk signal
     */
    private fun calculateBehaviorSignal(profile: BehaviorProfile): Float {
        // High spender profiles have higher risk
        val clusterRisk = when (profile.clusterId) {
            0 -> 0.1f  // Super Saver - low risk
            1 -> 0.3f  // Balanced - moderate
            2 -> 0.6f  // Active Spender - higher
            3 -> 0.4f  // Needs-Focused - moderate
            4 -> 0.7f  // Lifestyle - high
            else -> 0.5f
        }

        // Adjust by confidence
        return clusterRisk * profile.confidence + 0.5f * (1 - profile.confidence)
    }

    /**
     * Calculate anomaly risk signal
     */
    private fun calculateAnomalySignal(anomalies: List<AnomalyScore>): Float {
        if (anomalies.isEmpty()) return 0f

        // Weight by severity
        val weightedSum = anomalies.sumOf { anomaly ->
            val severityWeight = when (anomaly.severity) {
                AnomalySeverity.HIGH -> 1.0
                AnomalySeverity.MEDIUM -> 0.6
                AnomalySeverity.LOW -> 0.3
                AnomalySeverity.NONE -> 0.0
            }
            anomaly.score.toDouble() * severityWeight
        }

        // Normalize
        return (weightedSum / anomalies.size).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Calculate forecast risk signal
     */
    private fun calculateForecastSignal(forecast: SpendingForecast): Float {
        // Higher overrun probability = higher risk
        return forecast.budgetOverrunProbability * forecast.confidence
    }

    /**
     * Calculate budget adherence signal
     */
    private fun calculateBudgetSignal(fv: FinancialFeatureVector): Float {
        val spendRatio = fv.ratioFeatures.spendToIncomeRatio.toFloat()

        return when {
            spendRatio > 1.2f -> 1.0f   // Overspending
            spendRatio > 1.0f -> 0.8f   // At/over limit
            spendRatio > 0.9f -> 0.5f   // Close to limit
            spendRatio > 0.8f -> 0.3f   // Healthy
            else -> 0.1f                 // Very good
        }
    }

    /**
     * Calculate savings risk signal
     */
    private fun calculateSavingsSignal(fv: FinancialFeatureVector): Float {
        val savingsRate = fv.ratioFeatures.savingsRate.toFloat()

        return when {
            savingsRate < 0 -> 1.0f      // Negative savings
            savingsRate < 0.05f -> 0.8f  // Very low
            savingsRate < 0.10f -> 0.6f  // Low
            savingsRate < 0.20f -> 0.3f  // Below target
            else -> 0.1f                  // Good
        }
    }

    /**
     * Calculate trend risk signal
     */
    private fun calculateTrendSignal(forecast: SpendingForecast): Float {
        return when (forecast.trend) {
            ForecastTrend.INCREASING_FAST -> 0.9f
            ForecastTrend.INCREASING -> 0.6f
            ForecastTrend.STABLE -> 0.3f
            ForecastTrend.DECREASING -> 0.1f
            ForecastTrend.DECREASING_FAST -> 0.05f
            ForecastTrend.UNKNOWN -> 0.5f
        }
    }

    /**
     * Apply goal-based adjustments
     */
    private fun applyGoalAdjustments(
        baseScore: Float,
        fv: FinancialFeatureVector,
        goals: UserGoals?
    ): Float {
        if (goals == null) return baseScore

        var adjustment = 0f

        // Check savings goal
        if (goals.targetSavingsRate > 0) {
            val actualRate = fv.ratioFeatures.savingsRate.toFloat()
            if (actualRate < goals.targetSavingsRate) {
                adjustment += (goals.targetSavingsRate - actualRate) * 0.5f
            }
        }

        // Check spending limit goal
        if (goals.monthlySpendingLimit > 0) {
            val actual = fv.totalSpend.toFloat()
            if (actual > goals.monthlySpendingLimit) {
                adjustment += ((actual - goals.monthlySpendingLimit) / goals.monthlySpendingLimit) * 0.3f
            }
        }

        return (baseScore + adjustment).coerceIn(0f, 1f)
    }

    /**
     * Calculate confidence in risk assessment
     */
    private fun calculateConfidence(signals: FloatArray): Float {
        // Higher when signals agree
        val mean = signals.average().toFloat()
        val variance = signals.map { (it - mean) * (it - mean) }.average().toFloat()

        // Lower variance = higher confidence
        return (1f - kotlin.math.sqrt(variance)).coerceIn(0.4f, 0.95f)
    }

    /**
     * Identify specific risk factors
     */
    private fun identifyRiskFactors(
        signals: FloatArray,
        behavior: BehaviorProfile,
        anomalies: List<AnomalyScore>,
        forecast: SpendingForecast
    ): List<RiskFactor> {
        val factors = mutableListOf<RiskFactor>()

        // Check each signal
        if (signals[0] > 0.6f) { // Behavior
            factors.add(RiskFactor(
                name = "Spending Pattern",
                severity = if (signals[0] > 0.8f) FactorSeverity.HIGH else FactorSeverity.MEDIUM,
                description = "Your \"${behavior.clusterName}\" profile indicates higher spending tendency",
                contribution = signals[0] * fusionWeights[0]
            ))
        }

        if (signals[1] > 0.5f) { // Anomaly
            val highAnomalies = anomalies.count { it.severity == AnomalySeverity.HIGH }
            factors.add(RiskFactor(
                name = "Unusual Transactions",
                severity = if (highAnomalies > 0) FactorSeverity.HIGH else FactorSeverity.MEDIUM,
                description = "$highAnomalies high-severity anomalies detected",
                contribution = signals[1] * fusionWeights[1]
            ))
        }

        if (signals[2] > 0.6f) { // Forecast
            factors.add(RiskFactor(
                name = "Spending Forecast",
                severity = if (signals[2] > 0.8f) FactorSeverity.HIGH else FactorSeverity.MEDIUM,
                description = "Predicted to exceed typical spending by ${(forecast.percentageChange).toInt()}%",
                contribution = signals[2] * fusionWeights[2]
            ))
        }

        if (signals[3] > 0.6f) { // Budget
            factors.add(RiskFactor(
                name = "Budget Pressure",
                severity = if (signals[3] > 0.8f) FactorSeverity.HIGH else FactorSeverity.MEDIUM,
                description = "Spending rate is high relative to income",
                contribution = signals[3] * fusionWeights[3]
            ))
        }

        if (signals[4] > 0.6f) { // Savings
            factors.add(RiskFactor(
                name = "Low Savings",
                severity = if (signals[4] > 0.8f) FactorSeverity.HIGH else FactorSeverity.MEDIUM,
                description = "Savings rate below recommended 20%",
                contribution = signals[4] * fusionWeights[4]
            ))
        }

        return factors.sortedByDescending { it.contribution }
    }

    /**
     * Calculate per-category risk
     */
    private fun calculateCategoryRisks(fv: FinancialFeatureVector): Map<ExpenseCategory, Float> {
        val risks = mutableMapOf<ExpenseCategory, Float>()

        ExpenseCategory.entries.forEachIndexed { index, category ->
            val proportion = fv.categoryVector.distribution.getOrElse(index) { 0f }
            val multiplier = categoryRiskMultipliers[category] ?: 1f

            // Risk based on proportion and category type
            val risk = when {
                proportion > 0.4f -> 0.8f * multiplier
                proportion > 0.25f -> 0.5f * multiplier
                proportion > 0.15f -> 0.3f * multiplier
                else -> 0.1f * multiplier
            }

            risks[category] = risk.coerceIn(0f, 1f)
        }

        return risks
    }

    /**
     * Generate actionable recommendations based on risk
     */
    private fun generateRiskRecommendations(
        level: RiskLevel,
        factors: List<RiskFactor>
    ): List<RiskRecommendation> {
        val recommendations = mutableListOf<RiskRecommendation>()

        when (level) {
            RiskLevel.HIGH -> {
                recommendations.add(RiskRecommendation(
                    priority = 1,
                    action = "Review all non-essential spending immediately",
                    impact = "Could improve your financial health significantly"
                ))
            }
            RiskLevel.MEDIUM -> {
                recommendations.add(RiskRecommendation(
                    priority = 2,
                    action = "Set spending alerts for high-risk categories",
                    impact = "Helps prevent budget overruns"
                ))
            }
            RiskLevel.LOW -> {
                recommendations.add(RiskRecommendation(
                    priority = 3,
                    action = "Continue monitoring spending trends",
                    impact = "Maintain your good financial habits"
                ))
            }
            RiskLevel.MINIMAL -> {
                recommendations.add(RiskRecommendation(
                    priority = 4,
                    action = "Consider increasing savings allocation",
                    impact = "Build wealth faster"
                ))
            }
        }

        // Add factor-specific recommendations
        factors.take(2).forEach { factor ->
            when (factor.name) {
                "Unusual Transactions" -> recommendations.add(RiskRecommendation(
                    priority = 1,
                    action = "Review flagged transactions for accuracy",
                    impact = "Catch errors or fraud early"
                ))
                "Low Savings" -> recommendations.add(RiskRecommendation(
                    priority = 2,
                    action = "Set up automatic savings transfer",
                    impact = "Build emergency fund systematically"
                ))
                "Spending Forecast" -> recommendations.add(RiskRecommendation(
                    priority = 2,
                    action = "Plan meals and reduce dining out this week",
                    impact = "Quick way to reduce variable expenses"
                ))
            }
        }

        return recommendations.sortedBy { it.priority }.take(3)
    }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))
}

// ═══════════════════════════════════════════════════════════════════════════════
// Risk Assessment Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

data class RiskAssessment(
    val overallScore: Float,
    val riskLevel: RiskLevel,
    val confidence: Float,
    val riskFactors: List<RiskFactor>,
    val categoryRisks: Map<ExpenseCategory, Float>,
    val signals: RiskSignals,
    val recommendations: List<RiskRecommendation>
)

enum class RiskLevel {
    MINIMAL, LOW, MEDIUM, HIGH
}

data class RiskFactor(
    val name: String,
    val severity: FactorSeverity,
    val description: String,
    val contribution: Float
)

enum class FactorSeverity {
    LOW, MEDIUM, HIGH
}

data class RiskSignals(
    val behaviorSignal: Float,
    val anomalySignal: Float,
    val forecastSignal: Float,
    val budgetSignal: Float,
    val savingsSignal: Float,
    val trendSignal: Float
)

data class RiskRecommendation(
    val priority: Int,
    val action: String,
    val impact: String
)

data class UserGoals(
    val targetSavingsRate: Float = 0.20f,
    val monthlySpendingLimit: Float = 0f,
    val emergencyFundTarget: Float = 0f,
    val debtPayoffTarget: Float = 0f
)

