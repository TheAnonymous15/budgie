package com.example.budgie.ai.pipeline

import android.content.Context
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.Income
import com.example.budgie.data.preferences.UserPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * BUDGIE AI PIPELINE ORCHESTRATOR
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Full Offline AI Pipeline for Financial Insights
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                              TRANSACTIONS                                    │
 * │                                    ↓                                         │
 * │                     Feature Engineering (Rules)                              │
 * │                                    ↓                                         │
 * │        ┌────────────────┬─────────────────┬────────────────┐                │
 * │        │   Clustering   │    Anomaly      │   Forecasting  │                │
 * │        │   (K-Means)    │ (Isolation      │  (GRU/TCN)     │                │
 * │        │                │  Forest)        │                │                │
 * │        └───────┬────────┴────────┬────────┴───────┬────────┘                │
 * │                │                 │                │                          │
 * │                └─────────────────┼────────────────┘                          │
 * │                                  ↓                                           │
 * │                      ┌─────────────────────┐                                 │
 * │                      │   Risk Scoring      │                                 │
 * │                      │ (Fusion + Rules)    │                                 │
 * │                      └──────────┬──────────┘                                 │
 * │                                 ↓                                            │
 * │                      ┌─────────────────────┐                                 │
 * │                      │  Insight Generator  │                                 │
 * │                      │  (Template NLG)     │                                 │
 * │                      └──────────┬──────────┘                                 │
 * │                                 ↓                                            │
 * │                           INSIGHTS                                           │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 100% OFFLINE - NO CLOUD - NO LLM - FULLY PRIVATE
 */
class FinancialAIPipeline private constructor(private val context: Context) {

    // Pipeline components
    private val database = BudgieDatabase.getDatabase(context)
    private val preferencesManager = UserPreferencesManager.getInstance(context)

    // Stage 1 & 2: Feature Engineering
    private val featureEngineer = FeatureEngineer()

    // Stage 3: Behavior Clustering
    private val behaviorClusterer = BehaviorClusterer.getInstance(context)

    // Stage 4: Anomaly Detection
    private val anomalyDetector = AnomalyDetector.getInstance(context)

    // Stage 5: Trend Forecasting
    private val trendForecaster = TrendForecaster.getInstance(context)

    // Stage 6 & 7: Risk Scoring & Fusion
    private val riskScorer = RiskScorer.getInstance(context)

    // Stage 8: Insight Generation
    private val insightGenerator = InsightGenerator.getInstance()

    // Cache for expensive computations
    private var cachedAnalysis: PipelineAnalysis? = null
    private var cacheTimestamp: Long = 0
    private val cacheValidityMs = 5 * 60 * 1000L // 5 minutes

    companion object {
        @Volatile
        private var INSTANCE: FinancialAIPipeline? = null

        fun getInstance(context: Context): FinancialAIPipeline {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinancialAIPipeline(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Initialize the pipeline (call once at app startup)
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Initialize models that need setup
        trendForecaster.initialize()

        // Train anomaly detector on historical data
        val expenses = database.expenseDao().getAllExpenses().first()
        if (expenses.size >= 10) {
            anomalyDetector.train(expenses, featureEngineer)
        }
    }

    /**
     * Run full pipeline analysis
     * This is the main entry point for comprehensive AI analysis
     */
    suspend fun runFullAnalysis(forceRefresh: Boolean = false): PipelineAnalysis = withContext(Dispatchers.Default) {
        // Check cache
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedAnalysis != null && (now - cacheTimestamp) < cacheValidityMs) {
            return@withContext cachedAnalysis!!
        }

        // Fetch data
        val expenses = database.expenseDao().getAllExpenses().first()
        val incomes = database.incomeDao().getAllIncomes().first()
        val userProfile = preferencesManager.userProfile.first()

        // Define analysis period (last 30 days)
        val calendar = Calendar.getInstance()
        val periodEnd = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val periodStart = calendar.timeInMillis

        // ═══════════════════════════════════════════════════════════════════════
        // STAGE 1 & 2: Feature Engineering
        // ═══════════════════════════════════════════════════════════════════════
        val featureVector = featureEngineer.generateFeatures(
            expenses = expenses,
            incomes = incomes,
            periodStartMs = periodStart,
            periodEndMs = periodEnd
        )

        // Detect subscriptions
        val subscriptions = featureEngineer.detectSubscriptions(expenses)

        // ═══════════════════════════════════════════════════════════════════════
        // STAGE 3: Behavior Profiling (K-Means Clustering)
        // ═══════════════════════════════════════════════════════════════════════
        val behaviorProfile = behaviorClusterer.classifyBehavior(featureVector)

        // ═══════════════════════════════════════════════════════════════════════
        // STAGE 4: Anomaly Detection (Isolation Forest)
        // ═══════════════════════════════════════════════════════════════════════
        val recentExpenses = expenses.filter { it.date >= periodStart }
        val anomalyScores = anomalyDetector.scoreTransactions(
            expenses = recentExpenses,
            historicalExpenses = expenses,
            featureEngineer = featureEngineer
        )

        // ═══════════════════════════════════════════════════════════════════════
        // STAGE 5: Trend & Forecast Modeling (GRU/TCN)
        // ═══════════════════════════════════════════════════════════════════════
        val forecast = trendForecaster.forecast(featureVector, daysToForecast = 7)

        // ═══════════════════════════════════════════════════════════════════════
        // STAGE 6 & 7: Risk Scoring & Multimodal Fusion
        // ═══════════════════════════════════════════════════════════════════════
        val userGoals = UserGoals(
            targetSavingsRate = 0.20f,
            monthlySpendingLimit = (featureVector.totalIncome * 0.8).toFloat()
        )

        val riskAssessment = riskScorer.calculateRisk(
            behaviorProfile = behaviorProfile,
            anomalyScores = anomalyScores,
            forecast = forecast,
            featureVector = featureVector,
            userGoals = userGoals
        )

        // ═══════════════════════════════════════════════════════════════════════
        // STAGE 8: Insight Generation (Template NLG)
        // ═══════════════════════════════════════════════════════════════════════
        val userName = userProfile?.name ?: "there"
        val insights = insightGenerator.generateInsights(
            behaviorProfile = behaviorProfile,
            anomalyScores = anomalyScores,
            forecast = forecast,
            riskAssessment = riskAssessment,
            featureVector = featureVector,
            userName = userName
        )

        // Build final analysis
        val analysis = PipelineAnalysis(
            timestamp = now,
            featureVector = featureVector,
            behaviorProfile = behaviorProfile,
            anomalyScores = anomalyScores,
            forecast = forecast,
            riskAssessment = riskAssessment,
            insights = insights,
            subscriptions = subscriptions,
            metadata = AnalysisMetadata(
                transactionCount = recentExpenses.size,
                periodDays = 30,
                modelsUsed = listOf("K-Means", "Isolation Forest", "GRU", "Logistic Regression"),
                processingTimeMs = System.currentTimeMillis() - now
            )
        )

        // Cache result
        cachedAnalysis = analysis
        cacheTimestamp = now

        analysis
    }

    /**
     * Quick analysis for real-time transaction scoring
     */
    suspend fun analyzeTransaction(expense: Expense): TransactionAnalysis = withContext(Dispatchers.Default) {
        val expenses = database.expenseDao().getAllExpenses().first()

        // Score for anomaly
        val anomalyScore = anomalyDetector.scoreTransaction(
            expense = expense,
            historicalExpenses = expenses,
            featureEngineer = featureEngineer
        )

        // Suggest category if needed
        val suggestedCategory = featureEngineer.suggestCategory(expense)

        TransactionAnalysis(
            expense = expense,
            anomalyScore = anomalyScore,
            suggestedCategory = suggestedCategory,
            isUnusual = anomalyScore.isAnomaly
        )
    }

    /**
     * Get notification-ready insight
     */
    suspend fun getNotificationInsight(): String = withContext(Dispatchers.Default) {
        val analysis = runFullAnalysis()
        insightGenerator.generateNotificationInsight(
            riskAssessment = analysis.riskAssessment,
            forecast = analysis.forecast,
            featureVector = analysis.featureVector
        )
    }

    /**
     * Get quick summary for dashboard
     */
    suspend fun getDashboardSummary(): DashboardSummary = withContext(Dispatchers.Default) {
        val analysis = runFullAnalysis()

        DashboardSummary(
            healthScore = (100 - analysis.riskAssessment.overallScore * 100).toInt().coerceIn(0, 100),
            riskLevel = analysis.riskAssessment.riskLevel,
            savingsRate = (analysis.featureVector.ratioFeatures.savingsRate * 100).toInt(),
            behaviorType = analysis.behaviorProfile.clusterName,
            topInsight = analysis.insights.insights.firstOrNull()?.message ?: "No insights available",
            forecastTrend = analysis.forecast.trend,
            anomalyCount = analysis.anomalyScores.count { it.isAnomaly },
            actionRequired = analysis.riskAssessment.riskLevel >= RiskLevel.MEDIUM
        )
    }

    /**
     * Invalidate cache (call when data changes)
     */
    fun invalidateCache() {
        cachedAnalysis = null
        cacheTimestamp = 0
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        trendForecaster.close()
    }

    // Helper extension
    private fun FeatureEngineer.suggestCategory(expense: Expense): com.example.budgie.data.model.ExpenseCategory {
        val title = expense.title
        val amount = expense.amount

        val lowerTitle = title.lowercase()

        return when {
            lowerTitle.containsAny("food", "restaurant", "cafe", "coffee", "lunch", "dinner",
                "breakfast", "grocery", "supermarket", "pizza", "burger", "meal") ->
                com.example.budgie.data.model.ExpenseCategory.FOOD

            lowerTitle.containsAny("uber", "lyft", "taxi", "gas", "fuel", "petrol", "bus",
                "train", "metro", "parking", "car", "transport") ->
                com.example.budgie.data.model.ExpenseCategory.TRANSPORT

            lowerTitle.containsAny("electric", "water", "gas bill", "internet", "wifi",
                "phone bill", "utility", "power") ->
                com.example.budgie.data.model.ExpenseCategory.UTILITIES

            lowerTitle.containsAny("movie", "netflix", "spotify", "game", "concert",
                "entertainment", "streaming", "subscription") ->
                com.example.budgie.data.model.ExpenseCategory.ENTERTAINMENT

            lowerTitle.containsAny("amazon", "shop", "store", "mall", "clothes",
                "electronics", "purchase") ->
                com.example.budgie.data.model.ExpenseCategory.SHOPPING

            lowerTitle.containsAny("doctor", "hospital", "medicine", "pharmacy",
                "health", "medical", "clinic", "dental") ->
                com.example.budgie.data.model.ExpenseCategory.HEALTH

            lowerTitle.containsAny("school", "college", "university", "course",
                "book", "education", "tuition", "class") ->
                com.example.budgie.data.model.ExpenseCategory.EDUCATION

            lowerTitle.containsAny("rent", "lease", "mortgage", "housing", "apartment") ->
                com.example.budgie.data.model.ExpenseCategory.RENT

            lowerTitle.containsAny("insurance", "premium", "policy") ->
                com.example.budgie.data.model.ExpenseCategory.INSURANCE

            lowerTitle.containsAny("invest", "stock", "crypto", "trading", "fund") ->
                com.example.budgie.data.model.ExpenseCategory.INVESTMENT

            lowerTitle.containsAny("saving", "deposit", "emergency fund") ->
                com.example.budgie.data.model.ExpenseCategory.SAVINGS

            else -> expense.category
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Pipeline Output Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

data class PipelineAnalysis(
    val timestamp: Long,
    val featureVector: FinancialFeatureVector,
    val behaviorProfile: BehaviorProfile,
    val anomalyScores: List<AnomalyScore>,
    val forecast: SpendingForecast,
    val riskAssessment: RiskAssessment,
    val insights: GeneratedInsights,
    val subscriptions: List<DetectedSubscription>,
    val metadata: AnalysisMetadata
)

data class AnalysisMetadata(
    val transactionCount: Int,
    val periodDays: Int,
    val modelsUsed: List<String>,
    val processingTimeMs: Long
)

data class TransactionAnalysis(
    val expense: Expense,
    val anomalyScore: AnomalyScore,
    val suggestedCategory: com.example.budgie.data.model.ExpenseCategory,
    val isUnusual: Boolean
)

data class DashboardSummary(
    val healthScore: Int,
    val riskLevel: RiskLevel,
    val savingsRate: Int,
    val behaviorType: String,
    val topInsight: String,
    val forecastTrend: ForecastTrend,
    val anomalyCount: Int,
    val actionRequired: Boolean
)

