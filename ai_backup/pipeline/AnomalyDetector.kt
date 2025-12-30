package com.example.budgie.ai.pipeline

import android.content.Context
import com.example.budgie.data.model.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Stage 4: Anomaly Detection
 *
 * Model: Isolation Forest (Pure Kotlin Implementation)
 *
 * Purpose: Detect "something unusual"
 * Input: Transaction-level features
 * Output: Anomaly score (0-1)
 *
 * Insights:
 * - Unusual merchant
 * - Category spikes
 * - Possible fraud/subscription creep
 *
 * Runs: Per transaction (real-time)
 */
class AnomalyDetector(private val context: Context) {

    private var forest: List<IsolationTree>? = null
    private var isTraining = false

    companion object {
        private const val NUM_TREES = 100
        private const val SAMPLE_SIZE = 256
        private const val MAX_DEPTH = 8

        // Anomaly threshold
        private const val ANOMALY_THRESHOLD = 0.65f
        private const val HIGH_ANOMALY_THRESHOLD = 0.80f

        // Feature configuration
        private const val FEATURE_DIM = 7

        @Volatile
        private var INSTANCE: AnomalyDetector? = null

        fun getInstance(context: Context): AnomalyDetector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnomalyDetector(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Train Isolation Forest on historical transactions
     */
    suspend fun train(
        historicalExpenses: List<Expense>,
        featureEngineer: FeatureEngineer
    ) = withContext(Dispatchers.Default) {
        if (isTraining || historicalExpenses.size < 10) return@withContext

        isTraining = true
        try {
            // Convert expenses to feature vectors
            val featureVectors = historicalExpenses.map { expense ->
                featureEngineer.generateTransactionFeatures(expense, historicalExpenses)
                    .toModelInput()
            }

            // Build isolation forest
            forest = buildForest(featureVectors)
        } finally {
            isTraining = false
        }
    }

    /**
     * Score a single transaction for anomaly
     */
    suspend fun scoreTransaction(
        expense: Expense,
        historicalExpenses: List<Expense>,
        featureEngineer: FeatureEngineer
    ): AnomalyScore = withContext(Dispatchers.Default) {

        val features = featureEngineer.generateTransactionFeatures(expense, historicalExpenses)
        val featureVector = features.toModelInput()

        // If forest not trained, use statistical fallback
        val currentForest = forest
        val score = if (currentForest != null && currentForest.isNotEmpty()) {
            calculateIsolationScore(featureVector, currentForest)
        } else {
            calculateStatisticalScore(features)
        }

        // Determine severity
        val severity = when {
            score >= HIGH_ANOMALY_THRESHOLD -> AnomalySeverity.HIGH
            score >= ANOMALY_THRESHOLD -> AnomalySeverity.MEDIUM
            score >= 0.5f -> AnomalySeverity.LOW
            else -> AnomalySeverity.NONE
        }

        // Generate explanation
        val explanation = generateExplanation(features, score)

        AnomalyScore(
            score = score,
            severity = severity,
            isAnomaly = score >= ANOMALY_THRESHOLD,
            explanation = explanation,
            contributingFactors = identifyContributingFactors(features),
            expense = expense
        )
    }

    /**
     * Batch score multiple transactions
     */
    suspend fun scoreTransactions(
        expenses: List<Expense>,
        historicalExpenses: List<Expense>,
        featureEngineer: FeatureEngineer
    ): List<AnomalyScore> = withContext(Dispatchers.Default) {
        expenses.map { expense ->
            scoreTransaction(expense, historicalExpenses, featureEngineer)
        }
    }

    /**
     * Build Isolation Forest
     */
    private fun buildForest(data: List<FloatArray>): List<IsolationTree> {
        if (data.isEmpty()) return emptyList()

        return (0 until NUM_TREES).map {
            val sample = if (data.size <= SAMPLE_SIZE) {
                data
            } else {
                data.shuffled().take(SAMPLE_SIZE)
            }
            buildTree(sample, 0)
        }
    }

    /**
     * Build single Isolation Tree recursively
     */
    private fun buildTree(data: List<FloatArray>, depth: Int): IsolationTree {
        // Terminal conditions
        if (depth >= MAX_DEPTH || data.size <= 1) {
            return IsolationTree.Leaf(data.size)
        }

        // Select random feature
        val featureIdx = Random.nextInt(FEATURE_DIM)

        // Get min/max for this feature
        val values = data.map { it[featureIdx] }
        val minVal = values.minOrNull() ?: 0f
        val maxVal = values.maxOrNull() ?: 0f

        if (maxVal - minVal < 0.0001f) {
            return IsolationTree.Leaf(data.size)
        }

        // Random split point
        val splitValue = minVal + Random.nextFloat() * (maxVal - minVal)

        // Split data
        val leftData = data.filter { it[featureIdx] < splitValue }
        val rightData = data.filter { it[featureIdx] >= splitValue }

        if (leftData.isEmpty() || rightData.isEmpty()) {
            return IsolationTree.Leaf(data.size)
        }

        return IsolationTree.Node(
            featureIdx = featureIdx,
            splitValue = splitValue,
            left = buildTree(leftData, depth + 1),
            right = buildTree(rightData, depth + 1)
        )
    }

    /**
     * Calculate isolation score for a data point
     */
    private fun calculateIsolationScore(point: FloatArray, trees: List<IsolationTree>): Float {
        val avgPathLength = trees.map { pathLength(point, it, 0) }.average()

        // Normalize using expected path length
        val c = expectedPathLength(SAMPLE_SIZE)
        return (2.0.pow(-avgPathLength / c)).toFloat()
    }

    /**
     * Calculate path length for a point in a tree
     */
    private fun pathLength(point: FloatArray, tree: IsolationTree, currentDepth: Int): Double {
        return when (tree) {
            is IsolationTree.Leaf -> {
                currentDepth + expectedPathLength(tree.size)
            }
            is IsolationTree.Node -> {
                if (point[tree.featureIdx] < tree.splitValue) {
                    pathLength(point, tree.left, currentDepth + 1)
                } else {
                    pathLength(point, tree.right, currentDepth + 1)
                }
            }
        }
    }

    /**
     * Expected path length for a BST
     */
    private fun expectedPathLength(n: Int): Double {
        if (n <= 1) return 0.0
        return 2.0 * (ln(n - 1.0) + 0.5772156649) - (2.0 * (n - 1.0) / n)
    }

    /**
     * Statistical fallback when forest not trained
     */
    private fun calculateStatisticalScore(features: TransactionFeatures): Float {
        // Use Z-score based anomaly detection
        val zScore = abs(features.zScore)

        // Convert Z-score to probability-like score
        return when {
            zScore >= 3.0f -> 0.95f
            zScore >= 2.5f -> 0.85f
            zScore >= 2.0f -> 0.70f
            zScore >= 1.5f -> 0.55f
            zScore >= 1.0f -> 0.40f
            else -> 0.2f
        }
    }

    /**
     * Generate human-readable explanation
     */
    private fun generateExplanation(features: TransactionFeatures, score: Float): String {
        val reasons = mutableListOf<String>()

        // Check amount anomaly
        if (features.zScore > 2.0f) {
            reasons.add("Amount is ${String.format("%.1f", features.zScore)}x higher than usual for this category")
        } else if (features.zScore < -2.0f) {
            reasons.add("Amount is unusually low for this category")
        }

        // Check time anomaly
        if (features.hourOfDay < 6 || features.hourOfDay > 22) {
            reasons.add("Unusual transaction time")
        }

        // Weekend spending
        if (features.isWeekend && features.normalizedAmount > 1.5f) {
            reasons.add("High weekend spending")
        }

        return if (reasons.isEmpty()) {
            if (score >= ANOMALY_THRESHOLD) {
                "Transaction pattern differs from your typical behavior"
            } else {
                "Normal transaction"
            }
        } else {
            reasons.joinToString(". ")
        }
    }

    /**
     * Identify which factors contributed to anomaly
     */
    private fun identifyContributingFactors(features: TransactionFeatures): List<AnomalyFactor> {
        val factors = mutableListOf<AnomalyFactor>()

        if (abs(features.zScore) > 1.5f) {
            factors.add(AnomalyFactor(
                name = "Amount",
                contribution = abs(features.zScore) / 3f,
                direction = if (features.zScore > 0) "higher" else "lower"
            ))
        }

        if (features.hourOfDay < 6 || features.hourOfDay > 22) {
            factors.add(AnomalyFactor(
                name = "Time",
                contribution = 0.3f,
                direction = if (features.hourOfDay < 6) "early morning" else "late night"
            ))
        }

        if (features.normalizedAmount > 2.0f) {
            factors.add(AnomalyFactor(
                name = "Relative Amount",
                contribution = (features.normalizedAmount - 1f) / 3f,
                direction = "above average"
            ))
        }

        return factors.sortedByDescending { it.contribution }
    }

    private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Isolation Forest Data Structures
// ═══════════════════════════════════════════════════════════════════════════════

sealed class IsolationTree {
    data class Node(
        val featureIdx: Int,
        val splitValue: Float,
        val left: IsolationTree,
        val right: IsolationTree
    ) : IsolationTree()

    data class Leaf(val size: Int) : IsolationTree()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Anomaly Detection Results
// ═══════════════════════════════════════════════════════════════════════════════

data class AnomalyScore(
    val score: Float,
    val severity: AnomalySeverity,
    val isAnomaly: Boolean,
    val explanation: String,
    val contributingFactors: List<AnomalyFactor>,
    val expense: Expense
)

enum class AnomalySeverity {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

data class AnomalyFactor(
    val name: String,
    val contribution: Float,
    val direction: String
)

