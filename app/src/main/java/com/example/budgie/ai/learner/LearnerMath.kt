package com.example.budgie.ai.learner

import kotlin.math.*

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * BUDGIE SUPER LEARNER - MATH UTILITIES
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Pure Kotlin math operations for ML algorithms.
 * No external dependencies - maximum efficiency.
 */
object LearnerMath {

    // ═══════════════════════════════════════════════════════════════════════════
    // BASIC STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate mean of a list of values
     */
    fun mean(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.sum() / values.size
    }

    /**
     * Calculate variance
     */
    fun variance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val avg = mean(values)
        return values.sumOf { (it - avg).pow(2) } / (values.size - 1)
    }

    /**
     * Calculate standard deviation
     */
    fun stdDev(values: List<Double>): Double {
        return sqrt(variance(values))
    }

    /**
     * Calculate median
     */
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }

    /**
     * Calculate percentile (0-100)
     */
    fun percentile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val index = (p / 100.0 * (sorted.size - 1)).toInt()
        return sorted[index.coerceIn(0, sorted.size - 1)]
    }

    /**
     * Calculate Z-score (how many std devs from mean)
     */
    fun zScore(value: Double, mean: Double, stdDev: Double): Double {
        if (stdDev == 0.0) return 0.0
        return (value - mean) / stdDev
    }

    /**
     * Calculate complete statistics for a list
     */
    fun statistics(values: List<Double>): Stats {
        if (values.isEmpty()) return Stats()
        val mean = mean(values)
        val variance = variance(values)
        return Stats(
            count = values.size,
            sum = values.sum(),
            mean = mean,
            variance = variance,
            stdDev = sqrt(variance),
            min = values.minOrNull() ?: 0.0,
            max = values.maxOrNull() ?: 0.0,
            median = median(values)
        )
    }

    data class Stats(
        val count: Int = 0,
        val sum: Double = 0.0,
        val mean: Double = 0.0,
        val variance: Double = 0.0,
        val stdDev: Double = 0.0,
        val min: Double = 0.0,
        val max: Double = 0.0,
        val median: Double = 0.0
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPONENTIAL SMOOTHING (for trend detection)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Simple Exponential Smoothing
     * Good for short-term forecasting without trend
     *
     * @param values Time series data
     * @param alpha Smoothing factor (0-1), higher = more weight on recent
     */
    fun exponentialSmoothing(values: List<Double>, alpha: Double = 0.3): List<Double> {
        if (values.isEmpty()) return emptyList()

        val smoothed = mutableListOf<Double>()
        smoothed.add(values.first())

        for (i in 1 until values.size) {
            val newValue = alpha * values[i] + (1 - alpha) * smoothed[i - 1]
            smoothed.add(newValue)
        }

        return smoothed
    }

    /**
     * Double Exponential Smoothing (Holt's method)
     * Captures both level and trend
     *
     * @param values Time series data
     * @param alpha Level smoothing (0-1)
     * @param beta Trend smoothing (0-1)
     * @param periods Number of periods to forecast
     */
    fun doubleExponentialSmoothing(
        values: List<Double>,
        alpha: Double = 0.3,
        beta: Double = 0.1,
        periods: Int = 7
    ): Pair<List<Double>, List<Double>> {
        if (values.size < 2) return Pair(values, emptyList())

        // Initialize
        var level = values.first()
        var trend = values[1] - values[0]

        val smoothed = mutableListOf<Double>()
        smoothed.add(level)

        // Smooth historical data
        for (i in 1 until values.size) {
            val newLevel = alpha * values[i] + (1 - alpha) * (level + trend)
            val newTrend = beta * (newLevel - level) + (1 - beta) * trend
            level = newLevel
            trend = newTrend
            smoothed.add(level)
        }

        // Forecast future
        val forecast = mutableListOf<Double>()
        for (i in 1..periods) {
            forecast.add(level + i * trend)
        }

        return Pair(smoothed, forecast)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOVING AVERAGES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Simple Moving Average
     */
    fun simpleMovingAverage(values: List<Double>, window: Int): List<Double> {
        if (values.size < window) return listOf(mean(values))

        return values.windowed(window) { it.average() }
    }

    /**
     * Weighted Moving Average (more weight on recent)
     */
    fun weightedMovingAverage(values: List<Double>, window: Int): List<Double> {
        if (values.size < window) return listOf(mean(values))

        return values.windowed(window) { windowValues ->
            var weightSum = 0.0
            var valueSum = 0.0
            windowValues.forEachIndexed { index, value ->
                val weight = index + 1.0
                valueSum += value * weight
                weightSum += weight
            }
            valueSum / weightSum
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ANOMALY DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Detect anomalies using Z-score method
     * Returns list of (index, zScore) pairs for anomalies
     *
     * @param threshold Number of standard deviations to consider anomaly
     */
    fun detectAnomaliesZScore(
        values: List<Double>,
        threshold: Double = 2.0
    ): List<Pair<Int, Double>> {
        if (values.size < 3) return emptyList()

        val mean = mean(values)
        val std = stdDev(values)
        if (std == 0.0) return emptyList()

        return values.mapIndexedNotNull { index, value ->
            val z = zScore(value, mean, std)
            if (abs(z) >= threshold) Pair(index, z) else null
        }
    }

    /**
     * Detect anomalies using IQR (Interquartile Range) method
     * More robust to outliers than Z-score
     */
    fun detectAnomaliesIQR(
        values: List<Double>,
        multiplier: Double = 1.5
    ): List<Pair<Int, Double>> {
        if (values.size < 4) return emptyList()

        val q1 = percentile(values, 25.0)
        val q3 = percentile(values, 75.0)
        val iqr = q3 - q1

        val lowerBound = q1 - multiplier * iqr
        val upperBound = q3 + multiplier * iqr

        return values.mapIndexedNotNull { index, value ->
            when {
                value < lowerBound -> Pair(index, (lowerBound - value) / iqr)
                value > upperBound -> Pair(index, (value - upperBound) / iqr)
                else -> null
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TREND DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Simple linear regression
     * Returns (slope, intercept, r-squared)
     */
    fun linearRegression(values: List<Double>): Triple<Double, Double, Double> {
        if (values.size < 2) return Triple(0.0, values.firstOrNull() ?: 0.0, 0.0)

        val n = values.size
        val x = (0 until n).map { it.toDouble() }

        val xMean = x.average()
        val yMean = values.average()

        var numerator = 0.0
        var denominator = 0.0

        for (i in values.indices) {
            numerator += (x[i] - xMean) * (values[i] - yMean)
            denominator += (x[i] - xMean).pow(2)
        }

        val slope = if (denominator != 0.0) numerator / denominator else 0.0
        val intercept = yMean - slope * xMean

        // Calculate R-squared
        val predictions = x.map { slope * it + intercept }
        val ssRes = values.zip(predictions).sumOf { (it.first - it.second).pow(2) }
        val ssTot = values.sumOf { (it - yMean).pow(2) }
        val rSquared = if (ssTot != 0.0) 1 - ssRes / ssTot else 0.0

        return Triple(slope, intercept, rSquared.coerceIn(0.0, 1.0))
    }

    /**
     * Detect trend direction from values
     */
    fun detectTrend(values: List<Double>): Trend {
        if (values.size < 3) return Trend.STABLE

        val (slope, _, rSquared) = linearRegression(values)

        // Check for volatility
        val cv = stdDev(values) / mean(values).coerceAtLeast(0.01)
        if (cv > 0.5) return Trend.VOLATILE

        // Check trend significance
        if (rSquared < 0.3) return Trend.STABLE

        val percentChange = if (values.first() != 0.0) {
            (values.last() - values.first()) / values.first() * 100
        } else 0.0

        return when {
            percentChange > 10 -> Trend.INCREASING
            percentChange < -10 -> Trend.DECREASING
            else -> Trend.STABLE
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROBABILITY & SCORING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sigmoid function - converts value to 0-1 probability
     */
    fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x))
    }

    /**
     * Normalize value to 0-1 range
     */
    fun normalize(value: Double, min: Double, max: Double): Double {
        if (max == min) return 0.5
        return ((value - min) / (max - min)).coerceIn(0.0, 1.0)
    }

    /**
     * Normalize to 0-100 score
     */
    fun normalizeScore(value: Double, min: Double, max: Double): Float {
        return (normalize(value, min, max) * 100).toFloat()
    }

    /**
     * Calculate probability from ratio with confidence
     */
    fun ratioProbability(
        current: Double,
        required: Double,
        confidence: Double = 0.8
    ): Float {
        if (required <= 0) return 1f
        val ratio = current / required
        val probability = sigmoid((ratio - 0.5) * 4) // Centered at 50%
        return (probability * confidence).toFloat().coerceIn(0f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLUSTERING (Simplified K-Means for spending patterns)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Simple 1D clustering using natural breaks
     * Returns cluster assignments (0, 1, 2, ..., k-1)
     */
    fun cluster1D(values: List<Double>, k: Int = 3): List<Int> {
        if (values.isEmpty()) return emptyList()
        if (values.size <= k) return values.indices.toList()

        // Find natural break points using percentiles
        val breakPoints = (1 until k).map { i ->
            percentile(values, (i * 100.0 / k))
        }

        return values.map { value ->
            breakPoints.indexOfFirst { value <= it }.let {
                if (it == -1) k - 1 else it
            }
        }
    }

    /**
     * Categorize spender type based on savings rate
     */
    fun categorizeSpender(savingsRate: Double): SpenderType {
        return when {
            savingsRate >= 30 -> SpenderType.SAVER
            savingsRate >= 20 -> SpenderType.BALANCED
            savingsRate >= 10 -> SpenderType.MODERATE
            savingsRate >= 0 -> SpenderType.HIGH_SPENDER
            else -> SpenderType.HIGH_SPENDER
        }
    }
}

