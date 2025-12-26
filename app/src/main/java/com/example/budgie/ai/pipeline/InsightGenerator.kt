package com.example.budgie.ai.pipeline

import java.util.Locale

/**
 * Stage 8: Insight Generation (Template-based NLG)
 *
 * NO LLM REQUIRED
 *
 * Purpose: Explain findings in human language
 * Method: Template-based Natural Language Generation
 *
 * Why not LLM?
 * - Offline
 * - Predictable
 * - Trustworthy
 * - No hallucinations
 */
class InsightGenerator {

    companion object {
        // Greeting templates based on time of day
        private val morningGreetings = listOf(
            "Good morning! Here's your financial snapshot.",
            "Rise and shine! Let's check your finances.",
            "Morning! Your money's been busy - here's what's happening."
        )

        private val afternoonGreetings = listOf(
            "Good afternoon! Here's your midday financial update.",
            "Hey there! Let's see how your day is going financially.",
            "Afternoon check-in: Your spending so far today."
        )

        private val eveningGreetings = listOf(
            "Good evening! Time for your daily financial wrap-up.",
            "Evening! Here's how your money moved today.",
            "End of day summary: Your financial highlights."
        )

        @Volatile
        private var INSTANCE: InsightGenerator? = null

        fun getInstance(): InsightGenerator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InsightGenerator().also { INSTANCE = it }
            }
        }
    }

    /**
     * Generate comprehensive insights from all ML outputs
     */
    fun generateInsights(
        behaviorProfile: BehaviorProfile,
        anomalyScores: List<AnomalyScore>,
        forecast: SpendingForecast,
        riskAssessment: RiskAssessment,
        featureVector: FinancialFeatureVector,
        userName: String = "there"
    ): GeneratedInsights {
        val insights = mutableListOf<Insight>()

        // 1. Primary insight based on risk level
        insights.add(generateRiskInsight(riskAssessment, userName))

        // 2. Behavior insight
        insights.add(generateBehaviorInsight(behaviorProfile))

        // 3. Forecast insight
        insights.add(generateForecastInsight(forecast))

        // 4. Anomaly insight (if any)
        generateAnomalyInsight(anomalyScores)?.let { insights.add(it) }

        // 5. Savings insight
        insights.add(generateSavingsInsight(featureVector))

        // 6. Category insight
        insights.add(generateCategoryInsight(featureVector))

        // Generate summary
        val summary = generateSummary(riskAssessment, forecast, featureVector, userName)

        // Get greeting based on time
        val greeting = getTimeBasedGreeting()

        return GeneratedInsights(
            greeting = greeting,
            summary = summary,
            insights = insights.sortedByDescending { it.priority },
            actionItems = generateActionItems(riskAssessment, anomalyScores, forecast)
        )
    }

    /**
     * Generate single-line insight for notifications
     */
    fun generateNotificationInsight(
        riskAssessment: RiskAssessment,
        forecast: SpendingForecast,
        featureVector: FinancialFeatureVector
    ): String {
        // Priority: High risk > Anomaly > Forecast > General
        return when {
            riskAssessment.riskLevel == RiskLevel.HIGH -> {
                "⚠️ Financial alert: ${riskAssessment.riskFactors.firstOrNull()?.description ?: "Review your spending"}"
            }
            forecast.budgetOverrunProbability > 0.7f -> {
                "📊 ${(forecast.budgetOverrunProbability * 100).toInt()}% chance of exceeding your budget this week"
            }
            featureVector.ratioFeatures.savingsRate < 0.1 -> {
                "💰 Your savings rate is ${(featureVector.ratioFeatures.savingsRate * 100).toInt()}% - aim for 20%"
            }
            forecast.trend == ForecastTrend.INCREASING_FAST -> {
                "📈 Spending trending up ${forecast.percentageChange.toInt()}% - time to review?"
            }
            else -> {
                "✅ Your finances look healthy today. Keep it up!"
            }
        }
    }

    private fun generateRiskInsight(risk: RiskAssessment, userName: String): Insight {
        val (title, message, emoji) = when (risk.riskLevel) {
            RiskLevel.HIGH -> Triple(
                "Attention Needed",
                "Hey $userName, your financial risk score is elevated at ${(risk.overallScore * 100).toInt()}%. " +
                "${risk.riskFactors.firstOrNull()?.description ?: "Multiple factors need attention."}",
                "🚨"
            )
            RiskLevel.MEDIUM -> Triple(
                "Room for Improvement",
                "$userName, your finances are okay but could be better. " +
                "Focus on ${risk.riskFactors.firstOrNull()?.name?.lowercase() ?: "your spending habits"}.",
                "⚠️"
            )
            RiskLevel.LOW -> Triple(
                "On Track",
                "Good job, $userName! You're managing your finances well. " +
                "Minor improvements possible in ${risk.riskFactors.firstOrNull()?.name?.lowercase() ?: "some areas"}.",
                "📊"
            )
            RiskLevel.MINIMAL -> Triple(
                "Excellent Financial Health",
                "Outstanding, $userName! Your financial health is excellent. " +
                "Keep up the great habits!",
                "🌟"
            )
        }

        return Insight(
            id = "risk_${risk.riskLevel.name.lowercase()}",
            type = InsightType.RISK,
            title = title,
            message = message,
            emoji = emoji,
            priority = when (risk.riskLevel) {
                RiskLevel.HIGH -> 10
                RiskLevel.MEDIUM -> 7
                RiskLevel.LOW -> 4
                RiskLevel.MINIMAL -> 2
            },
            actionable = risk.riskLevel >= RiskLevel.MEDIUM,
            confidence = risk.confidence
        )
    }

    private fun generateBehaviorInsight(profile: BehaviorProfile): Insight {
        val traits = profile.dominantTraits.take(2).map { it.description }

        return Insight(
            id = "behavior_${profile.clusterId}",
            type = InsightType.BEHAVIOR,
            title = "Your Money Personality: ${profile.clusterName}",
            message = if (traits.isNotEmpty()) {
                "${profile.description} ${traits.joinToString(". ")}."
            } else {
                profile.description
            },
            emoji = when (profile.clusterId) {
                0 -> "💎"  // Super Saver
                1 -> "⚖️"  // Balanced
                2 -> "🔄"  // Active
                3 -> "🏠"  // Needs-focused
                4 -> "✨"  // Lifestyle
                else -> "📊"
            },
            priority = 5,
            actionable = false,
            confidence = profile.confidence
        )
    }

    private fun generateForecastInsight(forecast: SpendingForecast): Insight {
        val changeDirection = if (forecast.percentageChange >= 0) "up" else "down"
        val changeAmount = kotlin.math.abs(forecast.percentageChange).toInt()

        val (title, message, emoji) = when {
            forecast.budgetOverrunProbability > 0.8f -> Triple(
                "Budget Warning Ahead",
                "Based on your patterns, there's an ${(forecast.budgetOverrunProbability * 100).toInt()}% chance " +
                "you'll exceed your typical spending in the next ${forecast.forecastDays} days. " +
                "Predicted: $${String.format(Locale.US, "%.0f", forecast.totalPredicted)}",
                "🚨"
            )
            forecast.budgetOverrunProbability > 0.5f -> Triple(
                "Spending Forecast",
                "You might spend $${String.format(Locale.US, "%.0f", forecast.totalPredicted)} " +
                "in the next week ($changeDirection $changeAmount% from average). " +
                "Consider reviewing discretionary expenses.",
                "📈"
            )
            forecast.trend == ForecastTrend.DECREASING_FAST -> Triple(
                "Great Progress!",
                "Your spending is trending down significantly. " +
                "Predicted: $${String.format(Locale.US, "%.0f", forecast.totalPredicted)} " +
                "($changeAmount% below average).",
                "📉"
            )
            else -> Triple(
                "Week Ahead Preview",
                "Expected spending: $${String.format(Locale.US, "%.0f", forecast.totalPredicted)} " +
                "over the next ${forecast.forecastDays} days. " +
                "${forecast.getInsight()}",
                "🔮"
            )
        }

        return Insight(
            id = "forecast_${forecast.trend.name.lowercase()}",
            type = InsightType.FORECAST,
            title = title,
            message = message,
            emoji = emoji,
            priority = if (forecast.budgetOverrunProbability > 0.7f) 8 else 5,
            actionable = forecast.budgetOverrunProbability > 0.5f,
            confidence = forecast.confidence
        )
    }

    private fun generateAnomalyInsight(anomalies: List<AnomalyScore>): Insight? {
        val significantAnomalies = anomalies.filter { it.severity >= AnomalySeverity.MEDIUM }
        if (significantAnomalies.isEmpty()) return null

        val highCount = significantAnomalies.count { it.severity == AnomalySeverity.HIGH }
        val top = significantAnomalies.maxByOrNull { it.score }

        return Insight(
            id = "anomaly_detection",
            type = InsightType.ANOMALY,
            title = if (highCount > 0) "Unusual Activity Detected" else "Transaction Review Suggested",
            message = when {
                highCount > 1 -> "$highCount transactions significantly differ from your normal patterns. " +
                    "Largest: $${String.format(Locale.US, "%.2f", top?.expense?.amount ?: 0.0)} " +
                    "in ${top?.expense?.category?.displayName ?: "Unknown"}."
                highCount == 1 -> "One unusual transaction detected: " +
                    "$${String.format(Locale.US, "%.2f", top?.expense?.amount ?: 0.0)} " +
                    "(${top?.explanation ?: "differs from pattern"})."
                else -> "${significantAnomalies.size} transactions are slightly unusual. " +
                    "Worth a quick review."
            },
            emoji = if (highCount > 0) "🔍" else "👀",
            priority = if (highCount > 0) 9 else 6,
            actionable = true,
            confidence = significantAnomalies.map { it.score }.average().toFloat()
        )
    }

    private fun generateSavingsInsight(fv: FinancialFeatureVector): Insight {
        val savingsRate = fv.ratioFeatures.savingsRate
        val savingsPercent = (savingsRate * 100).toInt()

        val (title, message, emoji, priority) = when {
            savingsRate >= 0.30 -> Quadruple(
                "Savings Superstar",
                "Amazing! You're saving $savingsPercent% of your income. " +
                "That's well above the recommended 20%. Your future self thanks you!",
                "🏆",
                3
            )
            savingsRate >= 0.20 -> Quadruple(
                "Savings On Target",
                "You're saving $savingsPercent% - right at the recommended level. " +
                "Keep this up and watch your wealth grow!",
                "✅",
                4
            )
            savingsRate >= 0.10 -> Quadruple(
                "Savings Building",
                "You're saving $savingsPercent% of your income. " +
                "Try to boost this to 20% by cutting discretionary spending.",
                "📈",
                6
            )
            savingsRate >= 0 -> Quadruple(
                "Savings Opportunity",
                "Your current savings rate is $savingsPercent%. " +
                "Even small increases compound over time. Can you save an extra 5%?",
                "💡",
                7
            )
            else -> Quadruple(
                "Spending Exceeds Income",
                "You're spending more than you earn. " +
                "This month's deficit: $${String.format(Locale.US, "%.0f", -savingsRate * fv.totalIncome)}. " +
                "Let's work on a plan to fix this.",
                "🚨",
                10
            )
        }

        return Insight(
            id = "savings_$savingsPercent",
            type = InsightType.SAVINGS,
            title = title,
            message = message,
            emoji = emoji,
            priority = priority,
            actionable = savingsRate < 0.20,
            confidence = 0.95f // High confidence for direct calculations
        )
    }

    private fun generateCategoryInsight(fv: FinancialFeatureVector): Insight {
        val dominant = fv.categoryVector.dominantCategory
        val dominantPercent = (fv.categoryVector.distribution[dominant.ordinal] * 100).toInt()
        val needsPercent = (fv.categoryVector.needsRatio * 100).toInt()
        val wantsPercent = (fv.categoryVector.wantsRatio * 100).toInt()

        return Insight(
            id = "category_${dominant.name.lowercase()}",
            type = InsightType.CATEGORY,
            title = "Spending Breakdown",
            message = "Top category: ${dominant.displayName} ($dominantPercent% of spending). " +
                "Overall split: $needsPercent% needs, $wantsPercent% wants. " +
                if (wantsPercent > 40) "Consider reducing discretionary spending."
                else "Good balance between needs and wants.",
            emoji = "📊",
            priority = if (wantsPercent > 40) 6 else 4,
            actionable = wantsPercent > 40,
            confidence = 0.9f
        )
    }

    private fun generateSummary(
        risk: RiskAssessment,
        forecast: SpendingForecast,
        fv: FinancialFeatureVector,
        userName: String
    ): String {
        val healthEmoji = when (risk.riskLevel) {
            RiskLevel.MINIMAL -> "🌟"
            RiskLevel.LOW -> "✅"
            RiskLevel.MEDIUM -> "⚠️"
            RiskLevel.HIGH -> "🚨"
        }

        val savingsRate = (fv.ratioFeatures.savingsRate * 100).toInt()
        val riskScore = (risk.overallScore * 100).toInt()

        return "$healthEmoji Hey $userName! " +
            "Your financial risk score is $riskScore/100 (${risk.riskLevel.name.lowercase()}). " +
            "Saving $savingsRate% this month. " +
            when (forecast.trend) {
                ForecastTrend.INCREASING_FAST -> "⚠️ Spending accelerating - watch closely!"
                ForecastTrend.INCREASING -> "📈 Spending trending up slightly."
                ForecastTrend.STABLE -> "➡️ Spending stable and predictable."
                ForecastTrend.DECREASING -> "📉 Nice! Spending is coming down."
                ForecastTrend.DECREASING_FAST -> "🎉 Great progress reducing spending!"
                ForecastTrend.UNKNOWN -> ""
            }
    }

    private fun generateActionItems(
        risk: RiskAssessment,
        anomalies: List<AnomalyScore>,
        forecast: SpendingForecast
    ): List<ActionItem> {
        val actions = mutableListOf<ActionItem>()

        // From risk recommendations
        risk.recommendations.forEach { rec ->
            actions.add(ActionItem(
                action = rec.action,
                reason = rec.impact,
                urgency = when (rec.priority) {
                    1 -> Urgency.HIGH
                    2 -> Urgency.MEDIUM
                    else -> Urgency.LOW
                }
            ))
        }

        // From anomalies
        if (anomalies.any { it.severity == AnomalySeverity.HIGH }) {
            actions.add(ActionItem(
                action = "Review flagged transactions",
                reason = "Ensure all charges are legitimate",
                urgency = Urgency.HIGH
            ))
        }

        // From forecast
        if (forecast.budgetOverrunProbability > 0.7f) {
            actions.add(ActionItem(
                action = "Plan reduced spending for rest of week",
                reason = "Prevent budget overrun",
                urgency = Urgency.MEDIUM
            ))
        }

        return actions.distinctBy { it.action }.take(3)
    }

    private fun getTimeBasedGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> morningGreetings.random()
            hour < 17 -> afternoonGreetings.random()
            else -> eveningGreetings.random()
        }
    }

    // Helper class for 4 values
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

// ═══════════════════════════════════════════════════════════════════════════════
// NLG Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

data class GeneratedInsights(
    val greeting: String,
    val summary: String,
    val insights: List<Insight>,
    val actionItems: List<ActionItem>
)

data class Insight(
    val id: String,
    val type: InsightType,
    val title: String,
    val message: String,
    val emoji: String,
    val priority: Int,
    val actionable: Boolean,
    val confidence: Float
)

enum class InsightType {
    RISK, BEHAVIOR, FORECAST, ANOMALY, SAVINGS, CATEGORY, ACHIEVEMENT
}

data class ActionItem(
    val action: String,
    val reason: String,
    val urgency: Urgency
)

enum class Urgency {
    LOW, MEDIUM, HIGH
}

