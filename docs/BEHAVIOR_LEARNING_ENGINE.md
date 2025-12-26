# Budgie Behavior Learning Engine

## Overview

The **Behavior Learning Engine** is a fully offline, multimodal Machine Learning system designed to learn customer financial behavior patterns and make intelligent predictions. It runs entirely on-device without requiring any external API calls, ensuring user privacy and data security.

**File Location:** `app/src/main/java/com/example/budgie/ai/ml/BehaviorLearningEngine.kt`

**Created:** December 26, 2025

---

## Architecture

The engine consists of six main components working together in a pipeline:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BEHAVIOR LEARNING ENGINE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐               │
│  │   Feature     │───▶│   Behavior    │───▶│   Anomaly     │               │
│  │  Extractor    │    │  Clusterer    │    │  Detector     │               │
│  └───────────────┘    └───────────────┘    └───────────────┘               │
│         │                    │                    │                         │
│         ▼                    ▼                    ▼                         │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐               │
│  │  Time Series  │    │   Pattern     │    │    Risk       │               │
│  │  Predictor    │    │  Recognizer   │    │   Assessor    │               │
│  └───────────────┘    └───────────────┘    └───────────────┘               │
│         │                    │                    │                         │
│         └────────────────────┼────────────────────┘                         │
│                              ▼                                              │
│                    ┌───────────────────┐                                    │
│                    │  User Behavior    │                                    │
│                    │     Profile       │                                    │
│                    └───────────────────┘                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Components

### 1. Feature Extractor

**Purpose:** Transform raw transaction data into learnable numerical features.

**Expense Features Extracted:**
| Feature | Description | Normalization |
|---------|-------------|---------------|
| `amount` | Raw transaction amount | None |
| `normalizedAmount` | Log-normalized amount | ln(amount+1) / ln(10000) |
| `category` | Expense category as ordinal | 0-12 |
| `dayOfWeek` | Day of week (1-7) | None |
| `dayOfMonth` | Day of month (1-31) | None |
| `hourOfDay` | Hour of transaction | 0-23 |
| `isWeekend` | Weekend flag | Boolean |
| `monthOfYear` | Month (0-11) | None |
| `isRecurring` | Recurring expense flag | Boolean |

**Income Features Extracted:**
| Feature | Description |
|---------|-------------|
| `amount` | Raw income amount |
| `normalizedAmount` | Log-normalized amount |
| `source` | Income source as ordinal |
| `dayOfMonth` | Typical payment day |
| `monthOfYear` | Month received |
| `isRecurring` | Regular income flag |

**Aggregated Features:**
| Feature | Description |
|---------|-------------|
| `avgDailySpending` | Average spending per day |
| `avgTransactionAmount` | Average per transaction |
| `spendingStdDev` | Standard deviation of spending |
| `spendingConsistency` | 1 - coefficient of variation |
| `incomeToExpenseRatio` | Income / Expenses |
| `categoryDiversity` | Number of unique categories |
| `transactionFrequency` | Transactions per day |

---

### 2. Behavior Clusterer (K-Means)

**Algorithm:** K-Means++ with 4 clusters

**Purpose:** Group users into behavioral archetypes based on spending patterns.

**Cluster Types:**
| Cluster | Description |
|---------|-------------|
| `FRUGAL` | Minimal spending, high savings |
| `REGULAR` | Balanced, predictable spending |
| `SPLURGER` | Occasional large purchases |
| `IMPULSIVE` | Unpredictable spending patterns |

**Implementation Details:**
- Uses K-Means++ initialization for better convergence
- 50 iterations maximum
- Euclidean distance metric
- Features used: normalized amount, day of week, day of month, weekend flag, category

**Output:**
```kotlin
data class ClusteringResult(
    val clusterCount: Int,
    val dominantCluster: BehaviorCluster,
    val clusterDistribution: Map<BehaviorCluster, Double>
)
```

---

### 3. Anomaly Detector (Isolation Forest)

**Algorithm:** Isolation Forest

**Purpose:** Detect unusual transactions that deviate from normal patterns.

**How It Works:**
1. Builds 100 random isolation trees
2. Each tree randomly splits data points
3. Anomalies require fewer splits to isolate
4. Anomaly score = 2^(-avgPathLength / expectedPathLength)

**Parameters:**
| Parameter | Value |
|-----------|-------|
| Number of Trees | 100 |
| Sample Size | 256 |
| Max Tree Depth | log₂(sampleSize) |

**Severity Levels:**
| Score | Severity |
|-------|----------|
| > 0.9 | HIGH |
| > 0.8 | MEDIUM |
| > 0.7 | LOW |

**Output:**
```kotlin
data class AnomalyReport(
    val expense: Expense,
    val anomalyScore: Double,  // 0.0 to 1.0
    val reason: String,
    val severity: AnomalySeverity
)
```

---

### 4. Time Series Predictor

**Algorithm:** Auto-Regressive with Seasonality

**Purpose:** Predict future spending based on historical patterns.

**Components:**
1. **Trend Component:** Linear trend from first to last observation
2. **Seasonality Component:** Weekly (7-day) seasonal factors
3. **AR Component:** Weighted average of last 7 days

**Prediction Formula:**
```
prediction[t] = Σ(weights[i] * history[t-i]) + trend + seasonality[t % 7]
```

**Weights:** Exponentially decaying: `weight[i] = 1/(i+1) / Σ(1/k)`

**Output:**
```kotlin
data class BehaviorSpendingPrediction(
    val predictedTotal: Double,
    val confidence: Double,
    val categoryBreakdown: Map<ExpenseCategory, Double>,
    val dailyPredictions: List<Double>,
    val trend: TrendDirection,
    val insights: List<String>
)
```

---

### 5. Pattern Recognizer

**Purpose:** Identify recurring patterns in financial behavior.

**Patterns Detected:**

| Pattern Type | Detection Method |
|--------------|------------------|
| `RECURRING` | Flagged recurring expenses |
| `DAY_OF_WEEK` | Days with 30%+ higher spending |
| `HIGH_CATEGORY` | Categories with >25% of total |
| `SEASONAL` | Month-over-month patterns |

**Income Pattern Detection:**
- Groups incomes by source
- Calculates average interval between payments
- Classifies frequency: Weekly, Bi-weekly, Monthly, Irregular

**Output:**
```kotlin
data class SpendingPattern(
    val type: PatternType,
    val category: ExpenseCategory?,
    val averageAmount: Double,
    val frequency: String,
    val description: String,
    val confidence: Double
)
```

---

### 6. Risk Assessor

**Purpose:** Evaluate overall financial health and identify risk factors.

**Risk Factors Evaluated:**
| Factor | Trigger | Risk Contribution |
|--------|---------|-------------------|
| Negative Savings | savingsRate < 0 | +0.30 |
| Low Savings | savingsRate < 10% | +0.20 |
| Category Concentration | >50% in one category | +0.15 |
| High Volatility | CV > 1.0 | +0.10 |
| Unstable Income | stability < 0.7 | +0.15 |

**Risk Levels:**
| Overall Risk | Level |
|--------------|-------|
| > 0.50 | HIGH |
| > 0.25 | MEDIUM |
| ≤ 0.25 | LOW |

**Output:**
```kotlin
data class RiskAssessment(
    val overallRisk: Double,
    val riskLevel: RiskLevel,
    val factors: List<RiskFactor>,
    val recommendations: List<String>,
    val savingsRate: Double,
    val incomeStability: Double
)
```

---

## User Behavior Profile

The engine builds a comprehensive user profile:

```kotlin
data class UserBehaviorProfile(
    val spenderType: SpenderType,        // SAVER, BALANCED, MODERATE_SPENDER, HIGH_SPENDER
    val averageDailySpending: Double,
    val savingsRate: Double,
    val topSpendingCategories: List<ExpenseCategory>,
    val behaviorCluster: BehaviorCluster,
    val riskScore: Double,
    val consistencyScore: Double,
    val lastUpdated: Long
)
```

**Spender Type Classification:**
| Savings Rate | Type |
|--------------|------|
| > 30% | SAVER |
| > 15% | BALANCED |
| > 0% | MODERATE_SPENDER |
| ≤ 0% | HIGH_SPENDER |

---

## API Reference

### Training

```kotlin
suspend fun trainModel(
    expenses: List<Expense>,
    incomes: List<Income>
): TrainingResult
```

**Returns:**
```kotlin
data class TrainingResult(
    val success: Boolean,
    val message: String,
    val metrics: Map<String, Double>  // total_transactions, clusters_found, patterns_detected, model_confidence
)
```

### Predictions

**Spending Prediction:**
```kotlin
suspend fun predictSpending(
    daysAhead: Int = 30,
    expenses: List<Expense>
): BehaviorSpendingPrediction
```

**Income Prediction:**
```kotlin
suspend fun predictIncome(
    daysAhead: Int = 30,
    incomes: List<Income>
): IncomePrediction
```

**Goal Completion Prediction:**
```kotlin
suspend fun predictGoalCompletion(
    targetAmount: Double,
    currentAmount: Double,
    monthsRemaining: Int,
    expenses: List<Expense>,
    incomes: List<Income>
): GoalPrediction
```

**Loan Repayment Prediction:**
```kotlin
suspend fun predictLoanRepayment(
    loanAmount: Double,
    monthlyPayment: Double,
    interestRate: Double,
    months: Int,
    expenses: List<Expense>,
    incomes: List<Income>
): LoanRepaymentPrediction
```

### Analysis

**Anomaly Detection:**
```kotlin
suspend fun detectAnomalies(
    expenses: List<Expense>
): List<AnomalyReport>
```

**Risk Assessment:**
```kotlin
suspend fun assessRisk(
    expenses: List<Expense>,
    incomes: List<Income>
): RiskAssessment
```

---

## Integration with ViewModel

### State Flows

```kotlin
// In MainViewModel
val userBehaviorProfile: StateFlow<UserBehaviorProfile?>
val spendingPrediction: StateFlow<BehaviorSpendingPrediction?>
val incomePrediction: StateFlow<IncomePrediction?>
val riskAssessment: StateFlow<RiskAssessment?>
val anomalies: StateFlow<List<AnomalyReport>>
val isBehaviorModelTrained: StateFlow<Boolean>
```

### Methods

```kotlin
// Train the model
fun trainBehaviorModel()

// Predict goal completion probability
suspend fun predictGoalCompletion(
    targetAmount: Double,
    currentAmount: Double,
    monthsRemaining: Int
): GoalPrediction

// Predict loan repayment success
suspend fun predictLoanRepayment(
    loanAmount: Double,
    monthlyPayment: Double,
    interestRate: Double,
    months: Int
): LoanRepaymentPrediction

// Get spending patterns
fun getSpendingPatterns(): List<SpendingPattern>

// Get user profile
fun getBehaviorProfile(): UserBehaviorProfile?
```

---

## Usage Example

```kotlin
// In a Composable or ViewModel
val viewModel: MainViewModel = viewModel()

// Train the model when data is available
LaunchedEffect(Unit) {
    viewModel.trainBehaviorModel()
}

// Observe predictions
val profile by viewModel.userBehaviorProfile.collectAsState()
val prediction by viewModel.spendingPrediction.collectAsState()
val anomalies by viewModel.anomalies.collectAsState()

// Display user type
profile?.let {
    Text("You are a ${it.spenderType.name}")
    Text("Risk Score: ${(it.riskScore * 100).toInt()}%")
}

// Display spending prediction
prediction?.let {
    Text("Predicted spending: $${it.predictedTotal}")
    Text("Trend: ${it.trend.name}")
}

// Show anomalies
anomalies.forEach { anomaly ->
    Card {
        Text("⚠️ ${anomaly.reason}")
        Text("Severity: ${anomaly.severity.name}")
    }
}

// Predict goal completion
val goalPrediction = viewModel.predictGoalCompletion(
    targetAmount = 5000.0,
    currentAmount = 1500.0,
    monthsRemaining = 6
)
Text("Success probability: ${(goalPrediction.probability * 100).toInt()}%")
```

---

## Model Confidence

The model's confidence is based on data quantity:

| Data Points | Confidence |
|-------------|------------|
| ≥ 100 | 95% |
| ≥ 50 | 85% |
| ≥ 20 | 70% |
| ≥ 10 | 50% |
| < 10 | 30% |

Prediction confidence also decreases with longer time horizons (up to 50% penalty for 1-year predictions).

---

## Privacy & Security

- **Fully Offline:** No data leaves the device
- **On-Device Processing:** All ML computations run locally
- **No External APIs:** No network calls required
- **Data Ownership:** User has complete control over their data

---

## Future Improvements

1. **Persistent Model Storage:** Save trained model to disk for faster startup
2. **Incremental Learning:** Update model with new data without full retraining
3. **Deep Learning Models:** Implement LSTM/GRU for better time series prediction
4. **Merchant Recognition:** Identify spending patterns by merchant
5. **Budget Recommendations:** Auto-generate budget suggestions based on patterns

---

## Dependencies

- Kotlin Coroutines for async processing
- No external ML libraries (pure Kotlin implementation)
- Room database for data access

---

## Testing

Run the model with test data:

```kotlin
@Test
fun testBehaviorEngine() = runTest {
    val engine = BehaviorLearningEngine(context)
    
    val expenses = listOf(/* test expenses */)
    val incomes = listOf(/* test incomes */)
    
    val result = engine.trainModel(expenses, incomes)
    assertTrue(result.success)
    
    val prediction = engine.predictSpending(30, expenses)
    assertTrue(prediction.confidence > 0)
}
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | Dec 26, 2025 | Initial implementation |

---

## Author

Budgie Development Team

---

## License

Proprietary - All rights reserved

