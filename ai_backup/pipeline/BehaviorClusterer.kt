package com.example.budgie.ai.pipeline

import android.content.Context
import com.example.budgie.data.model.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Stage 3: User Behavior Profiling (Unsupervised Learning)
 *
 * Model: MiniBatch K-Means Clustering
 *
 * Purpose: Learn how the user behaves
 * Input: Aggregated monthly feature vectors
 * Output: Behavior cluster + distance from typical
 *
 * Runs: Weekly/Monthly
 */
class BehaviorClusterer(private val context: Context) {

    // Cluster definitions (pre-defined behavior profiles)
    private val clusterCentroids = arrayOf(
        // Cluster 0: "Super Saver" - Low spend ratio, high savings
        floatArrayOf(0.4f, 0.35f, 0.15f, 0.6f, 0.2f, 0.15f, 0.05f),

        // Cluster 1: "Balanced" - Moderate spending, decent savings
        floatArrayOf(0.7f, 0.15f, 0.25f, 0.45f, 0.25f, 0.2f, 0.1f),

        // Cluster 2: "Active Spender" - High frequency, varied categories
        floatArrayOf(0.85f, 0.05f, 0.35f, 0.35f, 0.3f, 0.25f, 0.15f),

        // Cluster 3: "Needs-Focused" - High essential spending
        floatArrayOf(0.75f, 0.10f, 0.20f, 0.7f, 0.15f, 0.1f, 0.05f),

        // Cluster 4: "Lifestyle Spender" - High discretionary
        floatArrayOf(0.9f, 0.0f, 0.40f, 0.3f, 0.35f, 0.25f, 0.1f)
    )

    private val clusterNames = arrayOf(
        "Super Saver",
        "Balanced Manager",
        "Active Spender",
        "Essentials First",
        "Lifestyle Spender"
    )

    private val clusterDescriptions = arrayOf(
        "You prioritize saving and keep spending minimal. Great financial discipline!",
        "You maintain a healthy balance between spending and saving.",
        "You have frequent transactions across many categories.",
        "Most of your spending goes to essential needs like rent, food, and utilities.",
        "You enjoy discretionary spending on entertainment and lifestyle."
    )

    companion object {
        // Feature indices for clustering
        private const val SPEND_RATIO_IDX = 0
        private const val SAVINGS_RATE_IDX = 1
        private const val TXN_FREQUENCY_IDX = 2
        private const val NEEDS_RATIO_IDX = 3
        private const val WANTS_RATIO_IDX = 4
        private const val WEEKEND_RATIO_IDX = 5
        private const val CATEGORY_DIVERSITY_IDX = 6

        private const val FEATURE_COUNT = 7
        private const val CLUSTER_COUNT = 5

        @Volatile
        private var INSTANCE: BehaviorClusterer? = null

        fun getInstance(context: Context): BehaviorClusterer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BehaviorClusterer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Classify user behavior based on feature vector
     */
    suspend fun classifyBehavior(
        featureVector: FinancialFeatureVector
    ): BehaviorProfile = withContext(Dispatchers.Default) {

        // Extract clustering features
        val clusteringFeatures = extractClusteringFeatures(featureVector)

        // Find nearest cluster using K-Means assignment
        var minDistance = Float.MAX_VALUE
        var nearestCluster = 0
        val distances = FloatArray(CLUSTER_COUNT)

        for (i in 0 until CLUSTER_COUNT) {
            val distance = euclideanDistance(clusteringFeatures, clusterCentroids[i])
            distances[i] = distance
            if (distance < minDistance) {
                minDistance = distance
                nearestCluster = i
            }
        }

        // Calculate confidence (inverse of distance, normalized)
        val maxDistance = distances.maxOrNull() ?: 1f
        val confidence = 1f - (minDistance / maxDistance)

        // Calculate soft assignments (probability of belonging to each cluster)
        val softAssignments = calculateSoftAssignments(distances)

        // Detect behavior shift from previous profile
        val behaviorShift = detectBehaviorShift(clusteringFeatures)

        BehaviorProfile(
            clusterId = nearestCluster,
            clusterName = clusterNames[nearestCluster],
            description = clusterDescriptions[nearestCluster],
            confidence = confidence,
            distanceFromCenter = minDistance,
            softAssignments = softAssignments,
            dominantTraits = identifyDominantTraits(clusteringFeatures),
            behaviorShift = behaviorShift,
            rawFeatures = clusteringFeatures
        )
    }

    /**
     * Extract features relevant for clustering
     */
    private fun extractClusteringFeatures(fv: FinancialFeatureVector): FloatArray {
        val features = FloatArray(FEATURE_COUNT)

        // Spend ratio (normalized)
        features[SPEND_RATIO_IDX] = fv.ratioFeatures.spendToIncomeRatio.toFloat().coerceIn(0f, 1.5f) / 1.5f

        // Savings rate (normalized)
        features[SAVINGS_RATE_IDX] = (fv.ratioFeatures.savingsRate.toFloat() + 0.5f).coerceIn(0f, 1f)

        // Transaction frequency (normalized)
        features[TXN_FREQUENCY_IDX] = (fv.frequencyFeatures.transactionsPerDay.toFloat() / 5f).coerceIn(0f, 1f)

        // Needs ratio
        features[NEEDS_RATIO_IDX] = fv.categoryVector.needsRatio

        // Wants ratio
        features[WANTS_RATIO_IDX] = fv.categoryVector.wantsRatio

        // Weekend spending ratio
        features[WEEKEND_RATIO_IDX] = fv.seasonalityFeatures.weekendRatio.toFloat()

        // Category diversity (entropy-based)
        features[CATEGORY_DIVERSITY_IDX] = calculateCategoryDiversity(fv.categoryVector.distribution)

        return features
    }

    /**
     * Calculate category diversity using normalized entropy
     */
    private fun calculateCategoryDiversity(distribution: FloatArray): Float {
        var entropy = 0f
        distribution.forEach { p ->
            if (p > 0.001f) {
                entropy -= p * kotlin.math.ln(p)
            }
        }
        // Normalize by max possible entropy
        val maxEntropy = kotlin.math.ln(distribution.size.toFloat())
        return if (maxEntropy > 0) entropy / maxEntropy else 0f
    }

    /**
     * Calculate soft cluster assignments using softmax
     */
    private fun calculateSoftAssignments(distances: FloatArray): FloatArray {
        // Convert distances to similarities (negative distance)
        val similarities = distances.map { -it }

        // Softmax
        val maxSim = similarities.maxOrNull() ?: 0f
        val expSims = similarities.map { exp((it - maxSim).toDouble()).toFloat() }
        val sumExp = expSims.sum()

        return expSims.map { it / sumExp }.toFloatArray()
    }

    /**
     * Identify dominant behavioral traits
     */
    private fun identifyDominantTraits(features: FloatArray): List<BehaviorTrait> {
        val traits = mutableListOf<BehaviorTrait>()

        // Check each feature for notable traits
        if (features[SAVINGS_RATE_IDX] > 0.6f) {
            traits.add(BehaviorTrait.HIGH_SAVER)
        } else if (features[SAVINGS_RATE_IDX] < 0.3f) {
            traits.add(BehaviorTrait.LOW_SAVER)
        }

        if (features[SPEND_RATIO_IDX] > 0.8f) {
            traits.add(BehaviorTrait.HIGH_SPENDER)
        } else if (features[SPEND_RATIO_IDX] < 0.5f) {
            traits.add(BehaviorTrait.FRUGAL)
        }

        if (features[TXN_FREQUENCY_IDX] > 0.6f) {
            traits.add(BehaviorTrait.FREQUENT_TRANSACTOR)
        }

        if (features[NEEDS_RATIO_IDX] > 0.6f) {
            traits.add(BehaviorTrait.NEEDS_FOCUSED)
        }

        if (features[WANTS_RATIO_IDX] > 0.4f) {
            traits.add(BehaviorTrait.LIFESTYLE_ORIENTED)
        }

        if (features[WEEKEND_RATIO_IDX] > 0.4f) {
            traits.add(BehaviorTrait.WEEKEND_SPENDER)
        }

        if (features[CATEGORY_DIVERSITY_IDX] > 0.7f) {
            traits.add(BehaviorTrait.DIVERSE_SPENDER)
        }

        return traits
    }

    /**
     * Detect if behavior has shifted from typical pattern
     */
    private fun detectBehaviorShift(currentFeatures: FloatArray): BehaviorShift? {
        // In production, compare with stored historical profile
        // For now, return null (no shift detected)
        return null
    }

    /**
     * Update cluster centroids with new data (online learning)
     */
    fun updateCentroids(featureVector: FinancialFeatureVector, clusterId: Int, learningRate: Float = 0.1f) {
        val features = extractClusteringFeatures(featureVector)

        // Move centroid towards new point
        for (i in 0 until FEATURE_COUNT) {
            clusterCentroids[clusterId][i] =
                clusterCentroids[clusterId][i] * (1 - learningRate) + features[i] * learningRate
        }
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Behavior Profiling Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

data class BehaviorProfile(
    val clusterId: Int,
    val clusterName: String,
    val description: String,
    val confidence: Float,
    val distanceFromCenter: Float,
    val softAssignments: FloatArray,
    val dominantTraits: List<BehaviorTrait>,
    val behaviorShift: BehaviorShift?,
    val rawFeatures: FloatArray
) {
    /**
     * Get insight about behavior
     */
    fun getInsight(): String {
        val traitDescriptions = dominantTraits.map { it.description }
        return if (traitDescriptions.isNotEmpty()) {
            "You're a \"$clusterName\". ${traitDescriptions.joinToString(". ")}."
        } else {
            "You're a \"$clusterName\". $description"
        }
    }

    /**
     * Check if behavior is significantly different from center
     */
    fun isAtypical(): Boolean = distanceFromCenter > 0.5f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BehaviorProfile) return false
        return clusterId == other.clusterId
    }

    override fun hashCode(): Int = clusterId
}

enum class BehaviorTrait(val description: String) {
    HIGH_SAVER("You consistently save a significant portion of income"),
    LOW_SAVER("Your savings rate could use improvement"),
    HIGH_SPENDER("You tend to spend most of your income"),
    FRUGAL("You're careful with your spending"),
    FREQUENT_TRANSACTOR("You make many transactions"),
    NEEDS_FOCUSED("You prioritize essential expenses"),
    LIFESTYLE_ORIENTED("You enjoy discretionary spending"),
    WEEKEND_SPENDER("You spend more on weekends"),
    DIVERSE_SPENDER("You spread spending across many categories")
}

data class BehaviorShift(
    val previousClusterId: Int,
    val currentClusterId: Int,
    val magnitude: Float,
    val direction: ShiftDirection,
    val message: String
)

enum class ShiftDirection {
    TOWARD_SAVING,
    TOWARD_SPENDING,
    TOWARD_BALANCE,
    LATERAL
}

