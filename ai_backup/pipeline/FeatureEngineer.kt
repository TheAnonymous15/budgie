package com.example.budgie.ai.pipeline

import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.Income
import java.util.Calendar

/**
 * Stage 1 & 2: Data Collection & Feature Engineering
 *
 * NO ML - Pure Kotlin logic
 * Converts raw transactions → learnable feature vectors
 *
 * This stage determines 90% of insight quality
 */
class FeatureEngineer {

    companion object {
        const val ROLLING_WINDOW_7 = 7
        const val ROLLING_WINDOW_30 = 30
        const val CATEGORY_COUNT = 12
    }

    /**
     * Generate comprehensive feature vector for a time period
     */
    fun generateFeatures(
        expenses: List<Expense>,
        incomes: List<Income>,
        periodStartMs: Long,
        periodEndMs: Long
    ): FinancialFeatureVector {
        val periodExpenses = expenses.filter { it.date in periodStartMs until periodEndMs }
        val periodIncome = incomes.filter { it.date in periodStartMs until periodEndMs }

        val totalSpend = periodExpenses.sumOf { it.amount }
        val totalIncome = periodIncome.sumOf { it.amount }

        return FinancialFeatureVector(
            // Time-series features
            dailySpendVector = generateDailySpendVector(periodExpenses, periodStartMs, periodEndMs),

            // Rolling statistics
            rollingStats = generateRollingStats(expenses, periodEndMs),

            // Ratio features
            ratioFeatures = generateRatioFeatures(totalSpend, totalIncome),

            // Frequency features
            frequencyFeatures = generateFrequencyFeatures(periodExpenses, periodStartMs, periodEndMs),

            // Seasonality features
            seasonalityFeatures = generateSeasonalityFeatures(periodExpenses),

            // Category distribution
            categoryVector = generateCategoryVector(periodExpenses, totalSpend),

            // Metadata
            periodStart = periodStartMs,
            periodEnd = periodEndMs,
            transactionCount = periodExpenses.size,
            totalSpend = totalSpend,
            totalIncome = totalIncome
        )
    }

    /**
     * Generate daily spend vector (time-series input)
     */
    private fun generateDailySpendVector(
        expenses: List<Expense>,
        startMs: Long,
        endMs: Long
    ): FloatArray {
        val calendar = Calendar.getInstance()
        val days = ((endMs - startMs) / (24 * 60 * 60 * 1000)).toInt().coerceIn(1, 365)
        val dailySpend = FloatArray(days)

        expenses.forEach { expense ->
            val dayIndex = ((expense.date - startMs) / (24 * 60 * 60 * 1000)).toInt()
            if (dayIndex in 0 until days) {
                dailySpend[dayIndex] += expense.amount.toFloat()
            }
        }

        return dailySpend
    }

    /**
     * Generate rolling statistics (7-day, 30-day)
     */
    private fun generateRollingStats(
        allExpenses: List<Expense>,
        currentTimeMs: Long
    ): RollingStats {
        val day7Start = currentTimeMs - (7L * 24 * 60 * 60 * 1000)
        val day30Start = currentTimeMs - (30L * 24 * 60 * 60 * 1000)

        val last7Days = allExpenses.filter { it.date in day7Start until currentTimeMs }
        val last30Days = allExpenses.filter { it.date in day30Start until currentTimeMs }

        return RollingStats(
            avg7Day = if (last7Days.isNotEmpty()) last7Days.sumOf { it.amount } / 7.0 else 0.0,
            avg30Day = if (last30Days.isNotEmpty()) last30Days.sumOf { it.amount } / 30.0 else 0.0,
            std7Day = calculateStdDev(last7Days.map { it.amount }),
            std30Day = calculateStdDev(last30Days.map { it.amount }),
            max7Day = last7Days.maxOfOrNull { it.amount } ?: 0.0,
            max30Day = last30Days.maxOfOrNull { it.amount } ?: 0.0,
            min7Day = last7Days.minOfOrNull { it.amount } ?: 0.0,
            min30Day = last30Days.minOfOrNull { it.amount } ?: 0.0,
            count7Day = last7Days.size,
            count30Day = last30Days.size
        )
    }

    /**
     * Generate ratio features (spend/income, etc.)
     */
    private fun generateRatioFeatures(
        totalSpend: Double,
        totalIncome: Double
    ): RatioFeatures {
        val spendToIncome = if (totalIncome > 0) totalSpend / totalIncome else 1.0
        val savingsRate = if (totalIncome > 0) (totalIncome - totalSpend) / totalIncome else 0.0

        return RatioFeatures(
            spendToIncomeRatio = spendToIncome.coerceIn(0.0, 2.0),
            savingsRate = savingsRate.coerceIn(-1.0, 1.0),
            discretionaryRatio = 0.0 // Calculated later with category data
        )
    }

    /**
     * Generate frequency features
     */
    private fun generateFrequencyFeatures(
        expenses: List<Expense>,
        startMs: Long,
        endMs: Long
    ): FrequencyFeatures {
        val days = ((endMs - startMs) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
        val transactionsPerDay = expenses.size.toDouble() / days

        // Group by day to find patterns
        val calendar = Calendar.getInstance()
        val dailyCounts = mutableMapOf<Int, Int>()
        expenses.forEach { expense ->
            calendar.timeInMillis = expense.date
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            dailyCounts[dayOfYear] = (dailyCounts[dayOfYear] ?: 0) + 1
        }

        val avgDailyCount = if (dailyCounts.isNotEmpty()) dailyCounts.values.average() else 0.0
        val maxDailyCount = dailyCounts.values.maxOrNull() ?: 0

        return FrequencyFeatures(
            transactionsPerDay = transactionsPerDay,
            avgDailyTransactions = avgDailyCount,
            maxDailyTransactions = maxDailyCount,
            activeDays = dailyCounts.size,
            totalDays = days
        )
    }

    /**
     * Generate seasonality features (weekday vs weekend, etc.)
     */
    private fun generateSeasonalityFeatures(expenses: List<Expense>): SeasonalityFeatures {
        val calendar = Calendar.getInstance()

        var weekdaySpend = 0.0
        var weekendSpend = 0.0
        var weekdayCount = 0
        var weekendCount = 0

        val dayOfWeekSpend = DoubleArray(7)
        val dayOfWeekCount = IntArray(7)

        expenses.forEach { expense ->
            calendar.timeInMillis = expense.date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            dayOfWeekSpend[dayOfWeek - 1] += expense.amount
            dayOfWeekCount[dayOfWeek - 1]++

            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                weekendSpend += expense.amount
                weekendCount++
            } else {
                weekdaySpend += expense.amount
                weekdayCount++
            }
        }

        // Normalize day-of-week spending
        val dayOfWeekAvg = FloatArray(7) { i ->
            if (dayOfWeekCount[i] > 0) (dayOfWeekSpend[i] / dayOfWeekCount[i]).toFloat() else 0f
        }

        return SeasonalityFeatures(
            weekdayAvgSpend = if (weekdayCount > 0) weekdaySpend / weekdayCount else 0.0,
            weekendAvgSpend = if (weekendCount > 0) weekendSpend / weekendCount else 0.0,
            weekendRatio = if (weekdaySpend + weekendSpend > 0) weekendSpend / (weekdaySpend + weekendSpend) else 0.0,
            dayOfWeekAvgSpend = dayOfWeekAvg,
            peakSpendingDay = dayOfWeekAvg.indices.maxByOrNull { dayOfWeekAvg[it] } ?: 0
        )
    }

    /**
     * Generate category distribution vector
     */
    private fun generateCategoryVector(
        expenses: List<Expense>,
        totalSpend: Double
    ): CategoryVector {
        val categorySpend = DoubleArray(ExpenseCategory.entries.size)
        val categoryCount = IntArray(ExpenseCategory.entries.size)

        expenses.forEach { expense ->
            val index = expense.category.ordinal
            categorySpend[index] += expense.amount
            categoryCount[index]++
        }

        // Normalize to percentages
        val categoryPercentages = FloatArray(ExpenseCategory.entries.size) { i ->
            if (totalSpend > 0) (categorySpend[i] / totalSpend).toFloat() else 0f
        }

        // Find dominant category
        val dominantCategoryIndex = categorySpend.indices.maxByOrNull { categorySpend[it] } ?: 0

        // Calculate needs vs wants
        val needsCategories = setOf(
            ExpenseCategory.RENT, ExpenseCategory.UTILITIES, ExpenseCategory.FOOD,
            ExpenseCategory.TRANSPORT, ExpenseCategory.HEALTH, ExpenseCategory.INSURANCE
        )
        val needsSpend = expenses.filter { it.category in needsCategories }.sumOf { it.amount }
        val wantsSpend = totalSpend - needsSpend

        return CategoryVector(
            distribution = categoryPercentages,
            categoryTotals = categorySpend,
            categoryCounts = categoryCount,
            dominantCategory = ExpenseCategory.entries[dominantCategoryIndex],
            needsRatio = if (totalSpend > 0) (needsSpend / totalSpend).toFloat() else 0.5f,
            wantsRatio = if (totalSpend > 0) (wantsSpend / totalSpend).toFloat() else 0.5f
        )
    }

    /**
     * Generate transaction-level features for anomaly detection
     */
    fun generateTransactionFeatures(
        expense: Expense,
        historicalExpenses: List<Expense>
    ): TransactionFeatures {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = expense.date

        // Category statistics
        val categoryExpenses = historicalExpenses.filter { it.category == expense.category }
        val categoryMean = if (categoryExpenses.isNotEmpty()) categoryExpenses.map { it.amount }.average() else expense.amount
        val categoryStd = calculateStdDev(categoryExpenses.map { it.amount })

        // Time features
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Z-score
        val zScore = if (categoryStd > 0) (expense.amount - categoryMean) / categoryStd else 0.0

        return TransactionFeatures(
            amount = expense.amount.toFloat(),
            normalizedAmount = (expense.amount / (categoryMean.coerceAtLeast(1.0))).toFloat(),
            categoryIndex = expense.category.ordinal,
            hourOfDay = hourOfDay,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY,
            zScore = zScore.toFloat(),
            categoryMean = categoryMean.toFloat(),
            categoryStd = categoryStd.toFloat()
        )
    }

    /**
     * Detect recurring transactions (subscriptions)
     */
    fun detectSubscriptions(expenses: List<Expense>): List<DetectedSubscription> {
        val subscriptions = mutableListOf<DetectedSubscription>()

        // Group by similar amounts and categories
        val grouped = expenses.groupBy {
            Pair(it.category, (it.amount * 100).toLong() / 100) // Round to 2 decimals
        }

        grouped.forEach { (key, transactions) ->
            if (transactions.size >= 2) {
                // Check if roughly monthly interval
                val sorted = transactions.sortedBy { it.date }
                val intervals = sorted.zipWithNext { a, b ->
                    (b.date - a.date) / (24 * 60 * 60 * 1000)
                }

                if (intervals.isNotEmpty()) {
                    val avgInterval = intervals.average()
                    val stdInterval = calculateStdDev(intervals.map { it.toDouble() })

                    // Monthly: 28-31 days, Weekly: 6-8 days
                    val isMonthly = avgInterval in 25.0..35.0 && stdInterval < 5
                    val isWeekly = avgInterval in 5.0..9.0 && stdInterval < 2

                    if (isMonthly || isWeekly) {
                        subscriptions.add(DetectedSubscription(
                            category = key.first,
                            amount = key.second.toDouble(),
                            frequency = if (isMonthly) SubscriptionFrequency.MONTHLY else SubscriptionFrequency.WEEKLY,
                            confidence = 1.0 - (stdInterval / avgInterval).coerceIn(0.0, 0.5),
                            lastOccurrence = sorted.last().date,
                            occurrenceCount = transactions.size
                        ))
                    }
                }
            }
        }

        return subscriptions
    }

    private fun calculateStdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Feature Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

data class FinancialFeatureVector(
    val dailySpendVector: FloatArray,
    val rollingStats: RollingStats,
    val ratioFeatures: RatioFeatures,
    val frequencyFeatures: FrequencyFeatures,
    val seasonalityFeatures: SeasonalityFeatures,
    val categoryVector: CategoryVector,
    val periodStart: Long,
    val periodEnd: Long,
    val transactionCount: Int,
    val totalSpend: Double,
    val totalIncome: Double
) {
    /**
     * Convert to flat array for ML model input
     */
    fun toModelInput(): FloatArray {
        val features = mutableListOf<Float>()

        // Rolling stats (10 features)
        features.add(rollingStats.avg7Day.toFloat())
        features.add(rollingStats.avg30Day.toFloat())
        features.add(rollingStats.std7Day.toFloat())
        features.add(rollingStats.std30Day.toFloat())
        features.add(rollingStats.max7Day.toFloat())
        features.add(rollingStats.max30Day.toFloat())
        features.add(rollingStats.min7Day.toFloat())
        features.add(rollingStats.min30Day.toFloat())
        features.add(rollingStats.count7Day.toFloat())
        features.add(rollingStats.count30Day.toFloat())

        // Ratio features (3 features)
        features.add(ratioFeatures.spendToIncomeRatio.toFloat())
        features.add(ratioFeatures.savingsRate.toFloat())
        features.add(ratioFeatures.discretionaryRatio.toFloat())

        // Frequency features (5 features)
        features.add(frequencyFeatures.transactionsPerDay.toFloat())
        features.add(frequencyFeatures.avgDailyTransactions.toFloat())
        features.add(frequencyFeatures.maxDailyTransactions.toFloat())
        features.add(frequencyFeatures.activeDays.toFloat())
        features.add(frequencyFeatures.totalDays.toFloat())

        // Seasonality features (4 features + 7 day-of-week)
        features.add(seasonalityFeatures.weekdayAvgSpend.toFloat())
        features.add(seasonalityFeatures.weekendAvgSpend.toFloat())
        features.add(seasonalityFeatures.weekendRatio.toFloat())
        features.add(seasonalityFeatures.peakSpendingDay.toFloat())
        features.addAll(seasonalityFeatures.dayOfWeekAvgSpend.toList())

        // Category distribution (12 features + 2)
        features.addAll(categoryVector.distribution.toList())
        features.add(categoryVector.needsRatio)
        features.add(categoryVector.wantsRatio)

        return features.toFloatArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FinancialFeatureVector) return false
        return periodStart == other.periodStart && periodEnd == other.periodEnd
    }

    override fun hashCode(): Int {
        return 31 * periodStart.hashCode() + periodEnd.hashCode()
    }
}

data class RollingStats(
    val avg7Day: Double,
    val avg30Day: Double,
    val std7Day: Double,
    val std30Day: Double,
    val max7Day: Double,
    val max30Day: Double,
    val min7Day: Double,
    val min30Day: Double,
    val count7Day: Int,
    val count30Day: Int
)

data class RatioFeatures(
    val spendToIncomeRatio: Double,
    val savingsRate: Double,
    val discretionaryRatio: Double
)

data class FrequencyFeatures(
    val transactionsPerDay: Double,
    val avgDailyTransactions: Double,
    val maxDailyTransactions: Int,
    val activeDays: Int,
    val totalDays: Int
)

data class SeasonalityFeatures(
    val weekdayAvgSpend: Double,
    val weekendAvgSpend: Double,
    val weekendRatio: Double,
    val dayOfWeekAvgSpend: FloatArray,
    val peakSpendingDay: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SeasonalityFeatures) return false
        return weekdayAvgSpend == other.weekdayAvgSpend && weekendAvgSpend == other.weekendAvgSpend
    }

    override fun hashCode(): Int {
        return 31 * weekdayAvgSpend.hashCode() + weekendAvgSpend.hashCode()
    }
}

data class CategoryVector(
    val distribution: FloatArray,
    val categoryTotals: DoubleArray,
    val categoryCounts: IntArray,
    val dominantCategory: ExpenseCategory,
    val needsRatio: Float,
    val wantsRatio: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CategoryVector) return false
        return dominantCategory == other.dominantCategory
    }

    override fun hashCode(): Int = dominantCategory.hashCode()
}

data class TransactionFeatures(
    val amount: Float,
    val normalizedAmount: Float,
    val categoryIndex: Int,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val dayOfMonth: Int,
    val isWeekend: Boolean,
    val zScore: Float,
    val categoryMean: Float,
    val categoryStd: Float
) {
    fun toModelInput(): FloatArray = floatArrayOf(
        normalizedAmount,
        categoryIndex.toFloat() / 12f, // Normalize category
        hourOfDay.toFloat() / 24f,
        dayOfWeek.toFloat() / 7f,
        dayOfMonth.toFloat() / 31f,
        if (isWeekend) 1f else 0f,
        zScore.coerceIn(-3f, 3f) / 3f // Normalize z-score
    )
}

data class DetectedSubscription(
    val category: ExpenseCategory,
    val amount: Double,
    val frequency: SubscriptionFrequency,
    val confidence: Double,
    val lastOccurrence: Long,
    val occurrenceCount: Int
)

enum class SubscriptionFrequency {
    WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY
}

