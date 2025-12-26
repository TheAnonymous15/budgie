package com.example.budgie.ai.ml

import android.content.Context
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Calendar

/**
 * TensorFlow Lite based Spending Predictor
 * Runs completely offline on-device
 *
 * Uses a simple neural network for:
 * - Next month spending prediction
 * - Category-wise spending prediction
 * - Anomaly detection
 */
class TFLiteSpendingPredictor(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false

    // Model configuration
    companion object {
        private const val MODEL_FILE = "spending_predictor.tflite"
        private const val INPUT_SIZE = 12 // 12 months of historical data
        private const val OUTPUT_SIZE = 1 // Predicted next month
        private const val CATEGORY_COUNT = 12 // Number of expense categories

        // Feature indices
        private const val FEATURE_AMOUNT = 0
        private const val FEATURE_DAY_OF_WEEK = 1
        private const val FEATURE_DAY_OF_MONTH = 2
        private const val FEATURE_MONTH = 3
        private const val FEATURE_CATEGORY = 4

        @Volatile
        private var INSTANCE: TFLiteSpendingPredictor? = null

        fun getInstance(context: Context): TFLiteSpendingPredictor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TFLiteSpendingPredictor(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Initialize the TFLite model
     * If no pre-trained model exists, use the built-in statistical predictor
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val modelBuffer = loadModelFile()
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
            }
        } catch (e: Exception) {
            // Model file doesn't exist - use statistical fallback
            isModelLoaded = false
        }
    }

    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Predict next month's spending using TFLite or fallback
     */
    suspend fun predictNextMonthSpending(
        historicalExpenses: List<Expense>
    ): SpendingPrediction = withContext(Dispatchers.Default) {
        if (isModelLoaded && interpreter != null) {
            predictWithTFLite(historicalExpenses)
        } else {
            predictWithStatistics(historicalExpenses)
        }
    }

    /**
     * TensorFlow Lite based prediction
     */
    private fun predictWithTFLite(expenses: List<Expense>): SpendingPrediction {
        val calendar = Calendar.getInstance()

        // Prepare input: last 12 months of spending
        val monthlyTotals = FloatArray(INPUT_SIZE)
        for (i in 0 until INPUT_SIZE) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, -(INPUT_SIZE - 1 - i))
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis

            monthlyTotals[i] = expenses
                .filter { it.date in monthStart until monthEnd }
                .sumOf { it.amount }
                .toFloat()
        }

        // Normalize input
        val maxValue = monthlyTotals.maxOrNull() ?: 1f
        val normalizedInput = monthlyTotals.map { it / maxValue }.toFloatArray()

        // Create input buffer
        val inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * 4).apply {
            order(ByteOrder.nativeOrder())
            normalizedInput.forEach { putFloat(it) }
        }

        // Create output buffer
        val outputBuffer = ByteBuffer.allocateDirect(OUTPUT_SIZE * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Run inference
        interpreter?.run(inputBuffer, outputBuffer)

        // Get prediction
        outputBuffer.rewind()
        val normalizedPrediction = outputBuffer.float
        val predictedAmount = normalizedPrediction * maxValue

        // Calculate confidence based on data consistency
        val variance = calculateVariance(monthlyTotals)
        val confidence = calculateConfidence(variance, maxValue)

        return SpendingPrediction(
            predictedAmount = predictedAmount.toDouble(),
            confidence = confidence,
            trend = determineTrend(monthlyTotals),
            categoryPredictions = predictCategorySpending(expenses),
            anomalyScore = calculateAnomalyScore(monthlyTotals)
        )
    }

    /**
     * Statistical fallback prediction (when TFLite model not available)
     * Uses exponential moving average and linear regression
     */
    private fun predictWithStatistics(expenses: List<Expense>): SpendingPrediction {
        val calendar = Calendar.getInstance()

        // Get monthly totals
        val monthlyTotals = mutableListOf<Double>()
        for (i in 5 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, -i)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis

            monthlyTotals.add(
                expenses.filter { it.date in monthStart until monthEnd }.sumOf { it.amount }
            )
        }

        if (monthlyTotals.isEmpty() || monthlyTotals.all { it == 0.0 }) {
            return SpendingPrediction(
                predictedAmount = 0.0,
                confidence = 0.0,
                trend = SpendingTrend.STABLE,
                categoryPredictions = emptyMap(),
                anomalyScore = 0.0
            )
        }

        // Exponential Moving Average prediction
        val alpha = 0.3 // Smoothing factor
        var ema = monthlyTotals.first()
        monthlyTotals.forEach { value ->
            ema = alpha * value + (1 - alpha) * ema
        }

        // Linear regression for trend
        val n = monthlyTotals.size
        val sumX = (0 until n).sum().toDouble()
        val sumY = monthlyTotals.sum()
        val sumXY = monthlyTotals.mapIndexed { i, y -> i * y }.sum()
        val sumX2 = (0 until n).sumOf { it * it }.toDouble()

        val slope = if (n * sumX2 - sumX * sumX != 0.0) {
            (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        } else 0.0

        val intercept = (sumY - slope * sumX) / n

        // Predict next month (index = n)
        val linearPrediction = slope * n + intercept

        // Combine EMA and linear prediction
        val predictedAmount = (ema + linearPrediction) / 2

        // Calculate confidence
        val mean = monthlyTotals.average()
        val variance = monthlyTotals.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val cv = if (mean > 0) stdDev / mean else 1.0
        val confidence = kotlin.math.max(0.3, kotlin.math.min(0.95, 1.0 - cv))

        // Determine trend
        val trend = when {
            slope > mean * 0.05 -> SpendingTrend.INCREASING
            slope < -mean * 0.05 -> SpendingTrend.DECREASING
            else -> SpendingTrend.STABLE
        }

        return SpendingPrediction(
            predictedAmount = kotlin.math.max(0.0, predictedAmount),
            confidence = confidence,
            trend = trend,
            categoryPredictions = predictCategorySpending(expenses),
            anomalyScore = calculateAnomalyScore(monthlyTotals.map { it.toFloat() }.toFloatArray())
        )
    }

    /**
     * Predict spending by category
     */
    private fun predictCategorySpending(expenses: List<Expense>): Map<ExpenseCategory, Double> {
        val calendar = Calendar.getInstance()
        val predictions = mutableMapOf<ExpenseCategory, Double>()

        ExpenseCategory.entries.forEach { category ->
            val categoryExpenses = expenses.filter { it.category == category }

            if (categoryExpenses.isNotEmpty()) {
                // Get last 3 months average for this category
                val monthlyAverages = mutableListOf<Double>()
                for (i in 2 downTo 0) {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.MONTH, -i)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val monthStart = calendar.timeInMillis
                    calendar.add(Calendar.MONTH, 1)
                    val monthEnd = calendar.timeInMillis

                    val monthTotal = categoryExpenses
                        .filter { it.date in monthStart until monthEnd }
                        .sumOf { it.amount }
                    monthlyAverages.add(monthTotal)
                }

                // Weighted average (recent months weighted more)
                val weights = listOf(0.2, 0.3, 0.5)
                val weightedAvg = monthlyAverages.zip(weights) { avg, weight -> avg * weight }.sum()
                predictions[category] = weightedAvg
            }
        }

        return predictions
    }

    /**
     * Detect spending anomalies using Z-score
     */
    fun detectAnomalies(expenses: List<Expense>): List<AnomalyResult> {
        val anomalies = mutableListOf<AnomalyResult>()

        // Group by category
        val categoryGroups = expenses.groupBy { it.category }

        categoryGroups.forEach { (category, categoryExpenses) ->
            if (categoryExpenses.size >= 3) {
                val amounts = categoryExpenses.map { it.amount }
                val mean = amounts.average()
                val stdDev = kotlin.math.sqrt(amounts.map { (it - mean) * (it - mean) }.average())

                categoryExpenses.forEach { expense ->
                    val zScore = if (stdDev > 0) (expense.amount - mean) / stdDev else 0.0

                    if (kotlin.math.abs(zScore) > 2.0) {
                        anomalies.add(AnomalyResult(
                            expense = expense,
                            zScore = zScore,
                            expectedRange = Pair(
                                kotlin.math.max(0.0, mean - 2 * stdDev),
                                mean + 2 * stdDev
                            ),
                            severity = when {
                                kotlin.math.abs(zScore) > 3.5 -> AnomalySeverity.HIGH
                                kotlin.math.abs(zScore) > 2.5 -> AnomalySeverity.MEDIUM
                                else -> AnomalySeverity.LOW
                            }
                        ))
                    }
                }
            }
        }

        return anomalies.sortedByDescending { kotlin.math.abs(it.zScore) }
    }

    /**
     * Classify expense category using simple on-device ML
     */
    fun suggestCategory(title: String, amount: Double): ExpenseCategory {
        val lowerTitle = title.lowercase()

        // Keyword-based classification (can be replaced with TFLite text model)
        return when {
            // Food
            lowerTitle.containsAny("food", "restaurant", "cafe", "coffee", "lunch", "dinner",
                "breakfast", "grocery", "supermarket", "pizza", "burger", "meal") -> ExpenseCategory.FOOD

            // Transport
            lowerTitle.containsAny("uber", "lyft", "taxi", "gas", "fuel", "petrol", "bus",
                "train", "metro", "parking", "car", "transport") -> ExpenseCategory.TRANSPORT

            // Utilities
            lowerTitle.containsAny("electric", "water", "gas bill", "internet", "wifi",
                "phone bill", "utility", "power") -> ExpenseCategory.UTILITIES

            // Entertainment
            lowerTitle.containsAny("movie", "netflix", "spotify", "game", "concert",
                "entertainment", "streaming", "subscription") -> ExpenseCategory.ENTERTAINMENT

            // Shopping
            lowerTitle.containsAny("amazon", "shop", "store", "mall", "clothes",
                "electronics", "purchase") -> ExpenseCategory.SHOPPING

            // Health
            lowerTitle.containsAny("doctor", "hospital", "medicine", "pharmacy",
                "health", "medical", "clinic", "dental") -> ExpenseCategory.HEALTH

            // Education
            lowerTitle.containsAny("school", "college", "university", "course",
                "book", "education", "tuition", "class") -> ExpenseCategory.EDUCATION

            // Rent
            lowerTitle.containsAny("rent", "lease", "mortgage", "housing", "apartment") -> ExpenseCategory.RENT

            // Insurance
            lowerTitle.containsAny("insurance", "premium", "policy") -> ExpenseCategory.INSURANCE

            // Investment
            lowerTitle.containsAny("invest", "stock", "crypto", "trading", "fund") -> ExpenseCategory.INVESTMENT

            // Savings
            lowerTitle.containsAny("saving", "deposit", "emergency fund") -> ExpenseCategory.SAVINGS

            else -> ExpenseCategory.OTHER
        }
    }

    // Helper functions
    private fun calculateVariance(data: FloatArray): Float {
        if (data.isEmpty()) return 0f
        val mean = data.average().toFloat()
        return data.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun calculateConfidence(variance: Float, maxValue: Float): Double {
        if (maxValue == 0f) return 0.5
        val cv = kotlin.math.sqrt(variance.toDouble()) / maxValue
        return kotlin.math.max(0.3, kotlin.math.min(0.95, 1.0 - cv))
    }

    private fun determineTrend(data: FloatArray): SpendingTrend {
        if (data.size < 2) return SpendingTrend.STABLE

        val recentAvg = data.takeLast(3).average()
        val olderAvg = data.take(3).average()
        val change = (recentAvg - olderAvg) / (olderAvg.takeIf { it > 0 } ?: 1.0)

        return when {
            change > 0.1 -> SpendingTrend.INCREASING
            change < -0.1 -> SpendingTrend.DECREASING
            else -> SpendingTrend.STABLE
        }
    }

    private fun calculateAnomalyScore(data: FloatArray): Double {
        if (data.size < 3) return 0.0

        val mean = data.average()
        val stdDev = kotlin.math.sqrt(data.map { (it - mean) * (it - mean) }.average())

        // Score based on how many values are outliers
        val outliers = data.count { kotlin.math.abs((it - mean) / (stdDev.takeIf { it > 0 } ?: 1.0)) > 2.0 }
        return outliers.toDouble() / data.size
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}

// Data classes for ML results
data class SpendingPrediction(
    val predictedAmount: Double,
    val confidence: Double,
    val trend: SpendingTrend,
    val categoryPredictions: Map<ExpenseCategory, Double>,
    val anomalyScore: Double
)

enum class SpendingTrend {
    INCREASING, DECREASING, STABLE
}

data class AnomalyResult(
    val expense: Expense,
    val zScore: Double,
    val expectedRange: Pair<Double, Double>,
    val severity: AnomalySeverity
)

enum class AnomalySeverity {
    LOW, MEDIUM, HIGH
}

