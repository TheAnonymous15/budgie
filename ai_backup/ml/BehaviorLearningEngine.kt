package com.example.budgie.ai.ml

import android.content.Context
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.RiskLevel
import com.example.budgie.ai.AnomalySeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.*

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * BUDGIE BEHAVIOR LEARNING ENGINE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * A fully offline, multimodal ML system that learns customer financial behavior
 * and makes intelligent predictions.
 *
 * Architecture:
 * 1. Feature Engineering Layer - Transform raw data into learnable signals
 * 2. Behavior Clustering (K-Means) - Group similar spending patterns
 * 3. Anomaly Detection (Isolation Forest) - Detect unusual transactions
 * 4. Time Series Forecasting (ARIMA-like) - Predict future spending
 * 5. Pattern Recognition - Learn recurring behaviors
 * 6. Risk Assessment - Evaluate financial health risks
 *
 * All models run completely offline on-device.
 * ════════════════════════════════════════════════════════════════════════════════
 */

class BehaviorLearningEngine(private val context: Context) {

    // Model components
    private val featureExtractor = FeatureExtractor()
    private val behaviorClusterer = BehaviorClusterer()
    private val anomalyDetector = AnomalyDetector()
    private val timeSeriesPredictor = TimeSeriesPredictor()
    private val patternRecognizer = PatternRecognizer()
    private val riskAssessor = RiskAssessor()

    // Learned state
    private var userProfile: UserBehaviorProfile? = null
    private var isModelTrained = false

    /**
     * Train the model on user's historical data
     */
    suspend fun trainModel(
        expenses: List<Expense>,
        incomes: List<Income>
    ): TrainingResult = withContext(Dispatchers.Default) {
        if (expenses.isEmpty() && incomes.isEmpty()) {
            return@withContext TrainingResult(
                success = false,
                message = "Not enough data to train the model",
                metrics = emptyMap()
            )
        }

        try {
            // Step 1: Extract features
            val expenseFeatures = expenses.map { featureExtractor.extractExpenseFeatures(it) }
            val incomeFeatures = incomes.map { featureExtractor.extractIncomeFeatures(it) }
            val aggregatedFeatures = featureExtractor.aggregateFeatures(expenses, incomes)

            // Step 2: Train behavior clustering
            val clusteringResult = behaviorClusterer.train(expenseFeatures)

            // Step 3: Train anomaly detector
            anomalyDetector.train(expenseFeatures)

            // Step 4: Train time series predictor
            val dailySpending = aggregateDailySpending(expenses)
            timeSeriesPredictor.train(dailySpending)

            // Step 5: Train pattern recognizer
            patternRecognizer.learn(expenses, incomes)

            // Step 6: Build user profile
            userProfile = buildUserProfile(expenses, incomes, aggregatedFeatures, clusteringResult)

            isModelTrained = true

            TrainingResult(
                success = true,
                message = "Model trained successfully on ${expenses.size} expenses and ${incomes.size} incomes",
                metrics = mapOf(
                    "total_transactions" to (expenses.size + incomes.size).toDouble(),
                    "clusters_found" to clusteringResult.clusterCount.toDouble(),
                    "patterns_detected" to patternRecognizer.getPatternCount().toDouble(),
                    "model_confidence" to calculateModelConfidence(expenses.size)
                )
            )
        } catch (e: Exception) {
            TrainingResult(
                success = false,
                message = "Training failed: ${e.message}",
                metrics = emptyMap()
            )
        }
    }

    /**
     * Predict spending for the next period
     */
    suspend fun predictSpending(
        daysAhead: Int = 30,
        expenses: List<Expense>
    ): BehaviorSpendingPrediction = withContext(Dispatchers.Default) {
        if (!isModelTrained || expenses.isEmpty()) {
            return@withContext BehaviorSpendingPrediction(
                predictedTotal = 0.0,
                confidence = 0.0,
                categoryBreakdown = emptyMap(),
                trend = TrendDirection.STABLE,
                insights = listOf("Not enough data for predictions")
            )
        }

        // Get daily spending data
        val dailySpending = aggregateDailySpending(expenses)

        // Predict using time series model
        val predictions = timeSeriesPredictor.predict(daysAhead, dailySpending)

        // Calculate category breakdown based on historical patterns
        val categoryRatios = calculateCategoryRatios(expenses)
        val predictedTotal = predictions.sum()
        val categoryBreakdown = categoryRatios.mapValues { it.value * predictedTotal }

        // Determine trend
        val recentAvg = dailySpending.takeLast(7).average()
        val historicalAvg = dailySpending.average()
        val trend = when {
            recentAvg > historicalAvg * 1.1 -> TrendDirection.INCREASING
            recentAvg < historicalAvg * 0.9 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }

        // Calculate confidence based on data quality
        val confidence = calculatePredictionConfidence(dailySpending.size, daysAhead)

        // Generate insights
        val insights = generatePredictionInsights(predictedTotal, trend, categoryBreakdown, expenses)

        BehaviorSpendingPrediction(
            predictedTotal = predictedTotal,
            confidence = confidence,
            categoryBreakdown = categoryBreakdown,
            dailyPredictions = predictions,
            trend = trend,
            insights = insights
        )
    }

    /**
     * Predict income for the next period
     */
    suspend fun predictIncome(
        daysAhead: Int = 30,
        incomes: List<Income>
    ): IncomePrediction = withContext(Dispatchers.Default) {
        if (incomes.isEmpty()) {
            return@withContext IncomePrediction(
                predictedTotal = 0.0,
                confidence = 0.0,
                sourceBreakdown = emptyMap(),
                expectedDates = emptyList()
            )
        }

        // Analyze income patterns
        val patterns = patternRecognizer.getIncomePatterns()
        val regularIncomes = detectRegularIncomes(incomes)

        // Predict based on patterns
        val predictedTotal = regularIncomes.sumOf { it.expectedAmount }
        val sourceBreakdown = regularIncomes.groupBy { it.source }
            .mapValues { it.value.sumOf { inc -> inc.expectedAmount } }

        // Predict expected dates
        val expectedDates = regularIncomes.map { it.expectedNextDate }

        // Calculate confidence
        val confidence = if (regularIncomes.isNotEmpty()) 0.85 else 0.3

        IncomePrediction(
            predictedTotal = predictedTotal,
            confidence = confidence,
            sourceBreakdown = sourceBreakdown,
            expectedDates = expectedDates,
            regularIncomes = regularIncomes
        )
    }

    /**
     * Detect anomalies in recent transactions
     */
    suspend fun detectAnomalies(
        expenses: List<Expense>
    ): List<AnomalyReport> = withContext(Dispatchers.Default) {
        if (!isModelTrained || expenses.isEmpty()) {
            return@withContext emptyList()
        }

        val anomalies = mutableListOf<AnomalyReport>()

        expenses.forEach { expense ->
            val features = featureExtractor.extractExpenseFeatures(expense)
            val anomalyScore = anomalyDetector.score(features)

            if (anomalyScore > 0.7) {
                val reason = determineAnomalyReason(expense, expenses)
                anomalies.add(AnomalyReport(
                    expense = expense,
                    anomalyScore = anomalyScore,
                    reason = reason,
                    severity = when {
                        anomalyScore > 0.9 -> AnomalySeverity.HIGH
                        anomalyScore > 0.8 -> AnomalySeverity.MEDIUM
                        else -> AnomalySeverity.LOW
                    }
                ))
            }
        }

        anomalies.sortedByDescending { it.anomalyScore }
    }

    /**
     * Get user behavior profile
     */
    fun getUserProfile(): UserBehaviorProfile? = userProfile

    /**
     * Predict behavior cluster for new transaction
     */
    suspend fun predictCluster(expense: Expense): BehaviorCluster = withContext(Dispatchers.Default) {
        val features = featureExtractor.extractExpenseFeatures(expense)
        behaviorClusterer.predict(features)
    }

    /**
     * Assess financial risk
     */
    suspend fun assessRisk(
        expenses: List<Expense>,
        incomes: List<Income>
    ): RiskAssessment = withContext(Dispatchers.Default) {
        riskAssessor.assess(expenses, incomes, userProfile)
    }

    /**
     * Get spending patterns
     */
    fun getSpendingPatterns(): List<SpendingPattern> {
        return patternRecognizer.getSpendingPatterns()
    }

    /**
     * Predict probability of completing a financial goal
     */
    suspend fun predictGoalCompletion(
        targetAmount: Double,
        currentAmount: Double,
        monthsRemaining: Int,
        expenses: List<Expense>,
        incomes: List<Income>
    ): GoalPrediction = withContext(Dispatchers.Default) {
        val remainingAmount = targetAmount - currentAmount
        val monthlyIncome = incomes.filter { isCurrentMonth(it.date) }.sumOf { it.amount }
        val monthlyExpenses = expenses.filter { isCurrentMonth(it.date) }.sumOf { it.amount }
        val monthlySavings = monthlyIncome - monthlyExpenses

        // Calculate required monthly savings
        val requiredMonthlySavings = remainingAmount / monthsRemaining.coerceAtLeast(1)

        // Calculate probability based on current behavior
        val probability = when {
            monthlySavings <= 0 -> 0.05
            monthlySavings >= requiredMonthlySavings * 1.2 -> 0.95
            monthlySavings >= requiredMonthlySavings -> 0.80
            monthlySavings >= requiredMonthlySavings * 0.8 -> 0.60
            monthlySavings >= requiredMonthlySavings * 0.5 -> 0.35
            else -> 0.15
        }

        // Generate recommendations
        val recommendations = mutableListOf<String>()
        if (probability < 0.5) {
            val gap = requiredMonthlySavings - monthlySavings
            recommendations.add("Increase monthly savings by ${formatCurrency(gap)}")

            // Find categories to cut
            val categorySpending = expenses.groupBy { it.category }
                .mapValues { it.value.sumOf { exp -> exp.amount } }
            val topCategory = categorySpending.maxByOrNull { it.value }
            if (topCategory != null) {
                recommendations.add("Consider reducing ${topCategory.key.displayName} spending")
            }
        }

        GoalPrediction(
            probability = probability,
            projectedMonths = if (monthlySavings > 0)
                (remainingAmount / monthlySavings).toInt() else Int.MAX_VALUE,
            requiredMonthlySavings = requiredMonthlySavings,
            currentMonthlySavings = monthlySavings,
            recommendations = recommendations,
            riskLevel = when {
                probability >= 0.8 -> "LOW"
                probability >= 0.5 -> "MEDIUM"
                else -> "HIGH"
            }
        )
    }

    /**
     * Predict loan repayment probability
     */
    suspend fun predictLoanRepayment(
        loanAmount: Double,
        monthlyPayment: Double,
        interestRate: Double,
        months: Int,
        expenses: List<Expense>,
        incomes: List<Income>
    ): LoanRepaymentPrediction = withContext(Dispatchers.Default) {
        val monthlyIncome = incomes.filter { isCurrentMonth(it.date) }.sumOf { it.amount }
        val monthlyExpenses = expenses.filter { isCurrentMonth(it.date) }.sumOf { it.amount }
        val disposableIncome = monthlyIncome - monthlyExpenses

        // Debt-to-income ratio
        val dti = if (monthlyIncome > 0) monthlyPayment / monthlyIncome else 1.0

        // Calculate probability
        val probability = when {
            disposableIncome >= monthlyPayment * 1.5 -> 0.95
            disposableIncome >= monthlyPayment * 1.2 -> 0.85
            disposableIncome >= monthlyPayment -> 0.70
            disposableIncome >= monthlyPayment * 0.8 -> 0.50
            else -> 0.25
        }

        // Risk factors
        val riskFactors = mutableListOf<String>()
        if (dti > 0.4) riskFactors.add("High debt-to-income ratio (${String.format("%.1f", dti * 100)}%)")
        if (disposableIncome < monthlyPayment) riskFactors.add("Insufficient disposable income")

        // Calculate total cost
        val totalPayment = monthlyPayment * months
        val totalInterest = totalPayment - loanAmount

        LoanRepaymentPrediction(
            probability = probability,
            monthlyAffordability = disposableIncome - monthlyPayment,
            debtToIncomeRatio = dti,
            totalInterestCost = totalInterest,
            riskFactors = riskFactors,
            recommendation = when {
                probability >= 0.8 -> "This loan appears manageable with your current income."
                probability >= 0.5 -> "This loan is possible but may strain your finances."
                else -> "Consider a smaller loan or longer term to reduce monthly payments."
            }
        )
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Private Helper Methods
    // ════════════════════════════════════════════════════════════════════════════

    private fun aggregateDailySpending(expenses: List<Expense>): List<Double> {
        val calendar = Calendar.getInstance()
        val dailyMap = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            calendar.timeInMillis = expense.date
            val key = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
            dailyMap[key] = (dailyMap[key] ?: 0.0) + expense.amount
        }

        return dailyMap.values.toList().sorted()
    }

    private fun calculateCategoryRatios(expenses: List<Expense>): Map<ExpenseCategory, Double> {
        val total = expenses.sumOf { it.amount }
        if (total == 0.0) return emptyMap()

        return expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } / total }
    }

    private fun calculateModelConfidence(dataPoints: Int): Double {
        return when {
            dataPoints >= 100 -> 0.95
            dataPoints >= 50 -> 0.85
            dataPoints >= 20 -> 0.70
            dataPoints >= 10 -> 0.50
            else -> 0.30
        }
    }

    private fun calculatePredictionConfidence(dataPoints: Int, predictDays: Int): Double {
        val dataConfidence = calculateModelConfidence(dataPoints)
        val horizonPenalty = 1.0 - (predictDays / 365.0).coerceIn(0.0, 0.5)
        return dataConfidence * horizonPenalty
    }

    private fun buildUserProfile(
        expenses: List<Expense>,
        incomes: List<Income>,
        features: AggregatedFeatures,
        clusterResult: ClusteringResult
    ): UserBehaviorProfile {
        val totalExpenses = expenses.sumOf { it.amount }
        val totalIncome = incomes.sumOf { it.amount }
        val savingsRate = if (totalIncome > 0) (totalIncome - totalExpenses) / totalIncome else 0.0

        val spenderType = when {
            savingsRate > 0.3 -> SpenderType.SAVER
            savingsRate > 0.15 -> SpenderType.BALANCED
            savingsRate > 0 -> SpenderType.MODERATE_SPENDER
            else -> SpenderType.HIGH_SPENDER
        }

        val topCategories = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }

        return UserBehaviorProfile(
            spenderType = spenderType,
            averageDailySpending = features.avgDailySpending,
            savingsRate = savingsRate,
            topSpendingCategories = topCategories,
            behaviorCluster = clusterResult.dominantCluster,
            riskScore = calculateRiskScore(savingsRate, features),
            consistencyScore = features.spendingConsistency,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun calculateRiskScore(savingsRate: Double, features: AggregatedFeatures): Double {
        var risk = 0.5

        // Savings rate impact
        risk += when {
            savingsRate < 0 -> 0.3
            savingsRate < 0.1 -> 0.2
            savingsRate < 0.2 -> 0.1
            savingsRate > 0.3 -> -0.1
            else -> 0.0
        }

        // Spending consistency impact
        if (features.spendingConsistency < 0.5) risk += 0.1

        return risk.coerceIn(0.0, 1.0)
    }

    private fun generatePredictionInsights(
        predictedTotal: Double,
        trend: TrendDirection,
        categoryBreakdown: Map<ExpenseCategory, Double>,
        historicalExpenses: List<Expense>
    ): List<String> {
        val insights = mutableListOf<String>()

        // Trend insight
        insights.add(when (trend) {
            TrendDirection.INCREASING -> "⚠️ Your spending is trending upward"
            TrendDirection.DECREASING -> "✅ Great! Your spending is trending downward"
            TrendDirection.STABLE -> "📊 Your spending pattern is stable"
        })

        // Top category insight
        val topCategory = categoryBreakdown.maxByOrNull { it.value }
        if (topCategory != null) {
            insights.add("💰 Predicted highest spending: ${topCategory.key.displayName} (${formatCurrency(topCategory.value)})")
        }

        // Comparison to historical
        val historicalMonthly = historicalExpenses.sumOf { it.amount } /
            (historicalExpenses.map { it.date }.distinct().size / 30.0).coerceAtLeast(1.0)

        if (predictedTotal > historicalMonthly * 1.2) {
            insights.add("📈 Predicted spending is 20%+ higher than your average")
        } else if (predictedTotal < historicalMonthly * 0.8) {
            insights.add("📉 Predicted spending is 20%+ lower than your average")
        }

        return insights
    }

    private fun determineAnomalyReason(expense: Expense, allExpenses: List<Expense>): String {
        val categoryExpenses = allExpenses.filter { it.category == expense.category }
        val avgAmount = categoryExpenses.map { it.amount }.average()

        return when {
            expense.amount > avgAmount * 3 -> "Amount is 3x higher than usual for ${expense.category.displayName}"
            expense.amount > avgAmount * 2 -> "Amount is significantly higher than usual"
            else -> "Unusual spending pattern detected"
        }
    }

    private fun detectRegularIncomes(incomes: List<Income>): List<RegularIncome> {
        // Group by source and analyze timing
        val bySource = incomes.groupBy { it.source }
        val regularIncomes = mutableListOf<RegularIncome>()

        bySource.forEach { (source, sourceIncomes) ->
            if (sourceIncomes.size >= 2) {
                val amounts = sourceIncomes.map { it.amount }
                val avgAmount = amounts.average()
                val isRegular = amounts.all { abs(it - avgAmount) < avgAmount * 0.1 }

                if (isRegular) {
                    // Predict next date based on pattern
                    val dates = sourceIncomes.map { it.date }.sorted()
                    val avgInterval = if (dates.size >= 2) {
                        (dates.last() - dates.first()) / (dates.size - 1)
                    } else {
                        30L * 24 * 60 * 60 * 1000 // Default to monthly
                    }

                    regularIncomes.add(RegularIncome(
                        source = source,
                        expectedAmount = avgAmount,
                        expectedNextDate = dates.last() + avgInterval,
                        frequency = if (avgInterval < 15 * 24 * 60 * 60 * 1000) "Bi-weekly" else "Monthly"
                    ))
                }
            }
        }

        return regularIncomes
    }

    private fun isCurrentMonth(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.MONTH) == currentMonth &&
               calendar.get(Calendar.YEAR) == currentYear
    }

    private fun formatCurrency(amount: Double): String {
        return "$${String.format("%.2f", amount)}"
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Feature Extraction
// ════════════════════════════════════════════════════════════════════════════════

class FeatureExtractor {

    fun extractExpenseFeatures(expense: Expense): ExpenseFeatures {
        val calendar = Calendar.getInstance().apply { timeInMillis = expense.date }

        return ExpenseFeatures(
            amount = expense.amount,
            normalizedAmount = normalizeAmount(expense.amount),
            category = expense.category.ordinal,
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
            hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
            isWeekend = calendar.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY),
            monthOfYear = calendar.get(Calendar.MONTH),
            isRecurring = expense.isRecurring
        )
    }

    fun extractIncomeFeatures(income: Income): IncomeFeatures {
        val calendar = Calendar.getInstance().apply { timeInMillis = income.date }

        return IncomeFeatures(
            amount = income.amount,
            normalizedAmount = normalizeAmount(income.amount),
            source = income.source.ordinal,
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
            monthOfYear = calendar.get(Calendar.MONTH),
            isRecurring = income.isRecurring
        )
    }

    fun aggregateFeatures(expenses: List<Expense>, incomes: List<Income>): AggregatedFeatures {
        val totalExpenses = expenses.sumOf { it.amount }
        val totalIncome = incomes.sumOf { it.amount }
        val expenseAmounts = expenses.map { it.amount }

        return AggregatedFeatures(
            avgDailySpending = if (expenses.isNotEmpty()) totalExpenses / getDaySpan(expenses) else 0.0,
            avgTransactionAmount = expenseAmounts.average().takeIf { !it.isNaN() } ?: 0.0,
            spendingStdDev = calculateStdDev(expenseAmounts),
            spendingConsistency = calculateConsistency(expenseAmounts),
            incomeToExpenseRatio = if (totalExpenses > 0) totalIncome / totalExpenses else Double.MAX_VALUE,
            categoryDiversity = expenses.map { it.category }.distinct().size.toDouble(),
            transactionFrequency = expenses.size / getDaySpan(expenses).coerceAtLeast(1.0)
        )
    }

    private fun normalizeAmount(amount: Double): Double {
        // Log normalization to handle varying scales
        return ln(amount + 1) / ln(10000.0)
    }

    private fun getDaySpan(expenses: List<Expense>): Double {
        if (expenses.isEmpty()) return 1.0
        val dates = expenses.map { it.date }
        val span = (dates.maxOrNull()!! - dates.minOrNull()!!) / (24.0 * 60 * 60 * 1000)
        return span.coerceAtLeast(1.0)
    }

    private fun calculateStdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    private fun calculateConsistency(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        if (mean == 0.0) return 0.0
        val cv = calculateStdDev(values) / mean // Coefficient of variation
        return (1.0 - cv.coerceIn(0.0, 1.0))
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Behavior Clustering (K-Means Implementation)
// ════════════════════════════════════════════════════════════════════════════════

class BehaviorClusterer {

    private var centroids: List<DoubleArray> = emptyList()
    private var k = 4 // Number of clusters

    fun train(features: List<ExpenseFeatures>): ClusteringResult {
        if (features.size < k) {
            return ClusteringResult(
                clusterCount = 1,
                dominantCluster = BehaviorCluster.REGULAR,
                clusterDistribution = mapOf(BehaviorCluster.REGULAR to 1.0)
            )
        }

        // Convert features to vectors
        val vectors = features.map { featureToVector(it) }

        // Initialize centroids using k-means++
        centroids = initializeCentroids(vectors, k)

        // Run k-means iterations
        repeat(50) {
            val assignments = vectors.map { findNearestCentroid(it) }
            centroids = updateCentroids(vectors, assignments, k)
        }

        // Analyze cluster distribution
        val assignments = vectors.map { findNearestCentroid(it) }
        val distribution = assignments.groupingBy { it }.eachCount()
            .mapKeys { indexToCluster(it.key) }
            .mapValues { it.value.toDouble() / features.size }

        val dominantCluster = distribution.maxByOrNull { it.value }?.key ?: BehaviorCluster.REGULAR

        return ClusteringResult(
            clusterCount = k,
            dominantCluster = dominantCluster,
            clusterDistribution = distribution
        )
    }

    fun predict(features: ExpenseFeatures): BehaviorCluster {
        if (centroids.isEmpty()) return BehaviorCluster.REGULAR

        val vector = featureToVector(features)
        val clusterIndex = findNearestCentroid(vector)
        return indexToCluster(clusterIndex)
    }

    private fun featureToVector(features: ExpenseFeatures): DoubleArray {
        return doubleArrayOf(
            features.normalizedAmount,
            features.dayOfWeek.toDouble() / 7,
            features.dayOfMonth.toDouble() / 31,
            if (features.isWeekend) 1.0 else 0.0,
            features.category.toDouble() / 12
        )
    }

    private fun initializeCentroids(vectors: List<DoubleArray>, k: Int): List<DoubleArray> {
        val centroids = mutableListOf<DoubleArray>()
        centroids.add(vectors.random())

        repeat(k - 1) {
            val distances = vectors.map { v ->
                centroids.minOf { c -> euclideanDistance(v, c) }
            }
            val totalDist = distances.sum()
            val probs = distances.map { it / totalDist }

            var r = Math.random()
            var idx = 0
            for (i in probs.indices) {
                r -= probs[i]
                if (r <= 0) {
                    idx = i
                    break
                }
            }
            centroids.add(vectors[idx])
        }

        return centroids
    }

    private fun findNearestCentroid(vector: DoubleArray): Int {
        return centroids.indices.minByOrNull {
            euclideanDistance(vector, centroids[it])
        } ?: 0
    }

    private fun updateCentroids(
        vectors: List<DoubleArray>,
        assignments: List<Int>,
        k: Int
    ): List<DoubleArray> {
        val newCentroids = MutableList(k) { DoubleArray(vectors[0].size) }
        val counts = IntArray(k)

        vectors.forEachIndexed { i, vector ->
            val cluster = assignments[i]
            counts[cluster]++
            vector.forEachIndexed { j, v ->
                newCentroids[cluster][j] += v
            }
        }

        return newCentroids.mapIndexed { i, centroid ->
            if (counts[i] > 0) {
                centroid.map { it / counts[i] }.toDoubleArray()
            } else {
                centroids[i]
            }
        }
    }

    private fun euclideanDistance(a: DoubleArray, b: DoubleArray): Double {
        return sqrt(a.zip(b.toList()).sumOf { (x, y) -> (x - y).pow(2) })
    }

    private fun indexToCluster(index: Int): BehaviorCluster {
        return when (index) {
            0 -> BehaviorCluster.FRUGAL
            1 -> BehaviorCluster.REGULAR
            2 -> BehaviorCluster.SPLURGER
            3 -> BehaviorCluster.IMPULSIVE
            else -> BehaviorCluster.REGULAR
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Anomaly Detection (Isolation Forest Implementation)
// ════════════════════════════════════════════════════════════════════════════════

class AnomalyDetector {

    private var trees: List<IsolationTree> = emptyList()
    private var avgPathLength: Double = 0.0
    private val numTrees = 100
    private val sampleSize = 256

    fun train(features: List<ExpenseFeatures>) {
        if (features.isEmpty()) return

        val vectors = features.map { featureToVector(it) }
        val n = vectors.size.coerceAtMost(sampleSize)

        trees = (0 until numTrees).map {
            val sample = vectors.shuffled().take(n)
            buildTree(sample, 0, ceil(ln(n.toDouble()) / ln(2.0)).toInt())
        }

        avgPathLength = calculateAvgPathLength(n)
    }

    fun score(features: ExpenseFeatures): Double {
        if (trees.isEmpty()) return 0.0

        val vector = featureToVector(features)
        val avgPath = trees.map { getPathLength(vector, it, 0) }.average()

        return 2.0.pow(-avgPath / avgPathLength)
    }

    private fun featureToVector(features: ExpenseFeatures): DoubleArray {
        return doubleArrayOf(
            features.normalizedAmount,
            features.dayOfWeek.toDouble(),
            features.dayOfMonth.toDouble(),
            features.category.toDouble()
        )
    }

    private fun buildTree(data: List<DoubleArray>, depth: Int, maxDepth: Int): IsolationTree {
        if (depth >= maxDepth || data.size <= 1) {
            return IsolationTree(size = data.size)
        }

        val featureIndex = (0 until data[0].size).random()
        val values = data.map { it[featureIndex] }
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0

        if (min == max) {
            return IsolationTree(size = data.size)
        }

        val splitValue = min + Math.random() * (max - min)
        val left = data.filter { it[featureIndex] < splitValue }
        val right = data.filter { it[featureIndex] >= splitValue }

        return IsolationTree(
            featureIndex = featureIndex,
            splitValue = splitValue,
            left = buildTree(left, depth + 1, maxDepth),
            right = buildTree(right, depth + 1, maxDepth)
        )
    }

    private fun getPathLength(vector: DoubleArray, tree: IsolationTree, depth: Int): Double {
        if (tree.featureIndex == null || tree.left == null || tree.right == null) {
            return depth + calculateAvgPathLength(tree.size)
        }

        return if (vector[tree.featureIndex] < tree.splitValue) {
            getPathLength(vector, tree.left, depth + 1)
        } else {
            getPathLength(vector, tree.right, depth + 1)
        }
    }

    private fun calculateAvgPathLength(n: Int): Double {
        if (n <= 1) return 0.0
        val h = ln(n - 1.0) + 0.5772156649 // Euler's constant
        return 2 * h - 2 * (n - 1.0) / n
    }
}

data class IsolationTree(
    val featureIndex: Int? = null,
    val splitValue: Double = 0.0,
    val left: IsolationTree? = null,
    val right: IsolationTree? = null,
    val size: Int = 0
)

// ════════════════════════════════════════════════════════════════════════════════
// Time Series Prediction (ARIMA-like Implementation)
// ════════════════════════════════════════════════════════════════════════════════

class TimeSeriesPredictor {

    private var weights: DoubleArray = doubleArrayOf()
    private var trend: Double = 0.0
    private var seasonality: DoubleArray = DoubleArray(7) // Weekly seasonality

    fun train(dailyValues: List<Double>) {
        if (dailyValues.size < 14) return

        // Calculate trend
        trend = if (dailyValues.size >= 2) {
            (dailyValues.last() - dailyValues.first()) / dailyValues.size
        } else 0.0

        // Calculate weekly seasonality
        val weeklyAvg = dailyValues.average()
        for (i in 0..6) {
            val dayValues = dailyValues.filterIndexed { idx, _ -> idx % 7 == i }
            seasonality[i] = if (dayValues.isNotEmpty() && weeklyAvg > 0) {
                dayValues.average() / weeklyAvg
            } else 1.0
        }

        // Simple AR weights (last 7 days influence)
        weights = if (dailyValues.size >= 7) {
            DoubleArray(7) { i ->
                val weight = 1.0 / (i + 1)
                weight / (1..7).sumOf { 1.0 / it }
            }
        } else {
            doubleArrayOf(1.0)
        }
    }

    fun predict(daysAhead: Int, historicalData: List<Double>): List<Double> {
        if (historicalData.isEmpty()) return List(daysAhead) { 0.0 }

        val predictions = mutableListOf<Double>()
        val data = historicalData.toMutableList()

        repeat(daysAhead) { day ->
            // AR component
            var prediction = 0.0
            weights.forEachIndexed { i, w ->
                if (data.size > i) {
                    prediction += w * data[data.size - 1 - i]
                }
            }

            // Add trend
            prediction += trend

            // Apply seasonality
            val dayOfWeek = (day + historicalData.size) % 7
            prediction *= seasonality[dayOfWeek]

            predictions.add(prediction.coerceAtLeast(0.0))
            data.add(prediction)
        }

        return predictions
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Pattern Recognition
// ════════════════════════════════════════════════════════════════════════════════

class PatternRecognizer {

    private val spendingPatterns = mutableListOf<SpendingPattern>()
    private val incomePatterns = mutableListOf<IncomePattern>()

    fun learn(expenses: List<Expense>, incomes: List<Income>) {
        spendingPatterns.clear()
        incomePatterns.clear()

        // Detect recurring expenses
        detectRecurringExpenses(expenses)

        // Detect day-of-week patterns
        detectDayPatterns(expenses)

        // Detect category patterns
        detectCategoryPatterns(expenses)

        // Detect income patterns
        detectIncomePatterns(incomes)
    }

    fun getSpendingPatterns(): List<SpendingPattern> = spendingPatterns.toList()

    fun getIncomePatterns(): List<IncomePattern> = incomePatterns.toList()

    fun getPatternCount(): Int = spendingPatterns.size + incomePatterns.size

    private fun detectRecurringExpenses(expenses: List<Expense>) {
        val recurring = expenses.filter { it.isRecurring }
            .groupBy { it.category }

        recurring.forEach { (category, categoryExpenses) ->
            if (categoryExpenses.isNotEmpty()) {
                spendingPatterns.add(SpendingPattern(
                    type = PatternType.RECURRING,
                    category = category,
                    averageAmount = categoryExpenses.map { it.amount }.average(),
                    frequency = "Monthly",
                    confidence = 0.9
                ))
            }
        }
    }

    private fun detectDayPatterns(expenses: List<Expense>) {
        val calendar = Calendar.getInstance()
        val byDayOfWeek = expenses.groupBy {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        val avgByDay = byDayOfWeek.mapValues { it.value.sumOf { exp -> exp.amount } / it.value.size }
        val overallAvg = avgByDay.values.average()

        // Find high-spending days
        avgByDay.forEach { (day, avg) ->
            if (avg > overallAvg * 1.3) {
                val dayName = when (day) {
                    Calendar.SUNDAY -> "Sunday"
                    Calendar.MONDAY -> "Monday"
                    Calendar.TUESDAY -> "Tuesday"
                    Calendar.WEDNESDAY -> "Wednesday"
                    Calendar.THURSDAY -> "Thursday"
                    Calendar.FRIDAY -> "Friday"
                    Calendar.SATURDAY -> "Saturday"
                    else -> "Unknown"
                }
                spendingPatterns.add(SpendingPattern(
                    type = PatternType.DAY_OF_WEEK,
                    description = "Higher spending on $dayName",
                    averageAmount = avg,
                    frequency = "Weekly",
                    confidence = 0.7
                ))
            }
        }
    }

    private fun detectCategoryPatterns(expenses: List<Expense>) {
        val byCategory = expenses.groupBy { it.category }
        val total = expenses.sumOf { it.amount }

        byCategory.forEach { (category, categoryExpenses) ->
            val categoryTotal = categoryExpenses.sumOf { it.amount }
            val percentage = if (total > 0) categoryTotal / total else 0.0

            if (percentage > 0.25) {
                spendingPatterns.add(SpendingPattern(
                    type = PatternType.HIGH_CATEGORY,
                    category = category,
                    averageAmount = categoryExpenses.map { it.amount }.average(),
                    description = "${category.displayName} is ${String.format("%.0f", percentage * 100)}% of spending",
                    confidence = 0.85
                ))
            }
        }
    }

    private fun detectIncomePatterns(incomes: List<Income>) {
        val bySource = incomes.groupBy { it.source }

        bySource.forEach { (source, sourceIncomes) ->
            if (sourceIncomes.size >= 2) {
                incomePatterns.add(IncomePattern(
                    source = source,
                    averageAmount = sourceIncomes.map { it.amount }.average(),
                    frequency = detectFrequency(sourceIncomes.map { it.date }),
                    confidence = 0.8
                ))
            }
        }
    }

    private fun detectFrequency(dates: List<Long>): String {
        if (dates.size < 2) return "Unknown"

        val sortedDates = dates.sorted()
        val intervals = sortedDates.zipWithNext().map { (a, b) -> b - a }
        val avgInterval = intervals.average() / (24 * 60 * 60 * 1000) // Days

        return when {
            avgInterval < 8 -> "Weekly"
            avgInterval < 20 -> "Bi-weekly"
            avgInterval < 35 -> "Monthly"
            else -> "Irregular"
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Risk Assessment
// ════════════════════════════════════════════════════════════════════════════════

class RiskAssessor {

    fun assess(
        expenses: List<Expense>,
        incomes: List<Income>,
        profile: UserBehaviorProfile?
    ): RiskAssessment {
        val totalExpenses = expenses.sumOf { it.amount }
        val totalIncome = incomes.sumOf { it.amount }
        val savingsRate = if (totalIncome > 0) (totalIncome - totalExpenses) / totalIncome else 0.0

        val factors = mutableListOf<RiskFactor>()
        var overallRisk = 0.0

        // Savings rate risk
        if (savingsRate < 0) {
            factors.add(RiskFactor("Negative savings", "You're spending more than earning", 0.9))
            overallRisk += 0.3
        } else if (savingsRate < 0.1) {
            factors.add(RiskFactor("Low savings", "Savings rate below 10%", 0.7))
            overallRisk += 0.2
        }

        // Expense concentration risk
        val categoryConcentration = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } / totalExpenses }
            .values.maxOrNull() ?: 0.0

        if (categoryConcentration > 0.5) {
            factors.add(RiskFactor("Concentrated spending", "Over 50% in one category", 0.6))
            overallRisk += 0.15
        }

        // Spending volatility risk
        val amounts = expenses.map { it.amount }
        if (amounts.isNotEmpty()) {
            val stdDev = calculateStdDev(amounts)
            val mean = amounts.average()
            val cv = if (mean > 0) stdDev / mean else 0.0

            if (cv > 1.0) {
                factors.add(RiskFactor("High volatility", "Spending varies significantly", 0.5))
                overallRisk += 0.1
            }
        }

        // Income stability risk
        val incomeStability = calculateIncomeStability(incomes)
        if (incomeStability < 0.7) {
            factors.add(RiskFactor("Unstable income", "Income varies month to month", 0.6))
            overallRisk += 0.15
        }

        val riskLevel = when {
            overallRisk > 0.5 -> RiskLevel.HIGH
            overallRisk > 0.25 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return RiskAssessment(
            overallRisk = overallRisk.coerceIn(0.0, 1.0),
            riskLevel = riskLevel,
            factors = factors,
            recommendations = generateRecommendations(factors),
            savingsRate = savingsRate,
            incomeStability = incomeStability
        )
    }

    private fun calculateStdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return sqrt(values.map { (it - mean).pow(2) }.average())
    }

    private fun calculateIncomeStability(incomes: List<Income>): Double {
        if (incomes.size < 2) return 1.0

        val calendar = Calendar.getInstance()
        val monthlyIncome = incomes.groupBy {
            calendar.timeInMillis = it.date
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
        }.mapValues { it.value.sumOf { inc -> inc.amount } }

        if (monthlyIncome.size < 2) return 1.0

        val values = monthlyIncome.values.toList()
        val mean = values.average()
        val stdDev = calculateStdDev(values)

        return if (mean > 0) 1.0 - (stdDev / mean).coerceIn(0.0, 1.0) else 0.0
    }

    private fun generateRecommendations(factors: List<RiskFactor>): List<String> {
        val recommendations = mutableListOf<String>()

        factors.forEach { factor ->
            when (factor.name) {
                "Negative savings" -> recommendations.add("Create a strict budget to reduce expenses")
                "Low savings" -> recommendations.add("Aim to save at least 20% of income")
                "Concentrated spending" -> recommendations.add("Diversify spending to reduce dependency")
                "High volatility" -> recommendations.add("Plan expenses ahead to reduce surprises")
                "Unstable income" -> recommendations.add("Build an emergency fund for 6 months")
            }
        }

        return recommendations
    }
}

// RiskLevel imported from com.example.budgie.data.model

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

data class ExpenseFeatures(
    val amount: Double,
    val normalizedAmount: Double,
    val category: Int,
    val dayOfWeek: Int,
    val dayOfMonth: Int,
    val hourOfDay: Int,
    val isWeekend: Boolean,
    val monthOfYear: Int,
    val isRecurring: Boolean
)

data class IncomeFeatures(
    val amount: Double,
    val normalizedAmount: Double,
    val source: Int,
    val dayOfMonth: Int,
    val monthOfYear: Int,
    val isRecurring: Boolean
)

data class AggregatedFeatures(
    val avgDailySpending: Double,
    val avgTransactionAmount: Double,
    val spendingStdDev: Double,
    val spendingConsistency: Double,
    val incomeToExpenseRatio: Double,
    val categoryDiversity: Double,
    val transactionFrequency: Double
)

data class TrainingResult(
    val success: Boolean,
    val message: String,
    val metrics: Map<String, Double>
)

data class BehaviorSpendingPrediction(
    val predictedTotal: Double,
    val confidence: Double,
    val categoryBreakdown: Map<ExpenseCategory, Double>,
    val dailyPredictions: List<Double> = emptyList(),
    val trend: TrendDirection,
    val insights: List<String>
)

data class IncomePrediction(
    val predictedTotal: Double,
    val confidence: Double,
    val sourceBreakdown: Map<com.example.budgie.data.model.IncomeSource, Double>,
    val expectedDates: List<Long>,
    val regularIncomes: List<RegularIncome> = emptyList()
)

data class RegularIncome(
    val source: com.example.budgie.data.model.IncomeSource,
    val expectedAmount: Double,
    val expectedNextDate: Long,
    val frequency: String
)

enum class TrendDirection { INCREASING, DECREASING, STABLE }

data class AnomalyReport(
    val expense: Expense,
    val anomalyScore: Double,
    val reason: String,
    val severity: AnomalySeverity
)

// AnomalySeverity defined in AIModels.kt

data class UserBehaviorProfile(
    val spenderType: SpenderType,
    val averageDailySpending: Double,
    val savingsRate: Double,
    val topSpendingCategories: List<ExpenseCategory>,
    val behaviorCluster: BehaviorCluster,
    val riskScore: Double,
    val consistencyScore: Double,
    val lastUpdated: Long
)

enum class SpenderType { SAVER, BALANCED, MODERATE_SPENDER, HIGH_SPENDER }

enum class BehaviorCluster { FRUGAL, REGULAR, SPLURGER, IMPULSIVE }

data class ClusteringResult(
    val clusterCount: Int,
    val dominantCluster: BehaviorCluster,
    val clusterDistribution: Map<BehaviorCluster, Double>
)

data class SpendingPattern(
    val type: PatternType,
    val category: ExpenseCategory? = null,
    val averageAmount: Double = 0.0,
    val frequency: String = "",
    val description: String = "",
    val confidence: Double = 0.0
)

data class IncomePattern(
    val source: com.example.budgie.data.model.IncomeSource,
    val averageAmount: Double,
    val frequency: String,
    val confidence: Double
)

enum class PatternType { RECURRING, DAY_OF_WEEK, HIGH_CATEGORY, SEASONAL }

data class GoalPrediction(
    val probability: Double,
    val projectedMonths: Int,
    val requiredMonthlySavings: Double,
    val currentMonthlySavings: Double,
    val recommendations: List<String>,
    val riskLevel: String
)

data class LoanRepaymentPrediction(
    val probability: Double,
    val monthlyAffordability: Double,
    val debtToIncomeRatio: Double,
    val totalInterestCost: Double,
    val riskFactors: List<String>,
    val recommendation: String
)

data class RiskAssessment(
    val overallRisk: Double,
    val riskLevel: RiskLevel,
    val factors: List<RiskFactor>,
    val recommendations: List<String>,
    val savingsRate: Double,
    val incomeStability: Double
)

data class RiskFactor(
    val name: String,
    val description: String,
    val severity: Double
)

