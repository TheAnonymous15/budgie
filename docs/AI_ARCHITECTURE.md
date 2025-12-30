# Budgie AI Architecture Documentation

## Overview

Budgie uses a **learner-based** AI architecture where the chatbot does NOT generate its own financial data. Instead, it references structured insights from a Financial Learner Engine that learns from user data.

## Architecture (from ai.txt)

### 1. Financial Learner Engine (`FinancialLearnerEngine.kt`)

The brain that learns from user's financial data. It produces **structured insights** that the chatbot can reference.

#### Learning Models (Lightweight Kotlin implementations)
| Task | Model Equivalent |
|------|------------------|
| Spending clusters | MiniBatch K-Means |
| Bill prediction | LSTM-lite |
| Anomalies | Isolation Forest (Z-score) |
| Seasonality | Holt-Winters |
| Risk score | Logistic regression |

#### What It Learns
- Category spending patterns (mean, std deviation)
- Day-of-week spending patterns
- Day-of-month spending patterns
- Spending trends
- Anomaly detection thresholds

### 2. Memory System

#### Layer 1 - User Financial State (Structured)
```kotlin
data class UserFinancialState(
    val monthlyIncome: Double,
    val avgMonthlySpend: Double,
    val avgDailySpend: Double,
    val totalSavings: Double,
    val savingsRate: Double,
    val topCategories: List<CategorySpend>,
    val nextBill: NextBillInfo?,
    val upcomingBills: List<NextBillInfo>,
    val riskLevel: RiskLevel,
    val spendingTrend: SpendingTrend,
    val anomalies: List<SpendingAnomaly>,
    val predictions: FinancialPredictions
)
```

#### Layer 2 - User Preferences
```kotlin
data class UserPreferences(
    val tone: ResponseTone,      // FRIENDLY, PROFESSIONAL, CASUAL
    val detailLevel: DetailLevel, // SHORT, MEDIUM, DETAILED
    val language: String,
    val currency: String
)
```

#### Layer 3 - Short-term Chat Memory (3-5 turns)
```kotlin
data class ChatMemory(
    val recentTopics: List<String>,
    val recentQuestions: List<String>,
    val lastIntent: String?,
    val contextFlags: Map<String, Any>
)
```

### 3. Chat Response Engine (`Phi3InferenceEngine.kt`)

#### Flow
1. User asks question
2. Query learner models for data
3. Build clean prompt with context
4. Generate friendly response

#### Key Principle
**The LLM never sees raw data - only summaries from the learner.**

Example:
```
User: "Will I struggle to pay rent next month?"

System queries learner → Gets:
- User income: $3200
- Rent: $900
- Predicted expenses: $2100
- Savings trend: stable

Response: "You should be fine next month. After rent, you'll still have about $200 buffer. Want tips to grow it?"
```

### 4. UI Guidelines (from ai.txt)

- Max 3 bullets in responses
- Emojis sparingly (1-2 max)
- Use "suggested questions" chips
- Show privacy/trust signals

#### Suggested Question Chips
```kotlin
AssistChip("Can I afford this?")
AssistChip("Predict my bills")
AssistChip("How can I save?")
```

### 5. Privacy & Trust

Display clearly:
- 🔒 "Runs 100% on your device"
- 📴 "No internet required"
- 🧠 "Your data never leaves your phone"

### 6. Battery & Thermal Strategy

- Load LLM on demand
- Unload after response
- Cap context to 1-2 KB
- Use CPU only (no GPU lock)
- Disable background inference
- Learners run once per day

### 7. When NOT to Use AI

Use plain Kotlin logic for:
- Simple math
- Static rules
- Warnings

## File Structure

```
app/src/main/java/com/example/budgie/ai/
├── learner/
│   └── FinancialLearnerEngine.kt  # Learning models + state
├── phi3/
│   └── Phi3InferenceEngine.kt     # Chat response generation
├── ConversationalAI.kt            # Legacy (being replaced)
└── FinancialAIEngine.kt           # Legacy (being replaced)
```

## Data Flow

```
User Transaction Data
        ↓
FinancialLearnerEngine
        ↓
┌─────────────────────────────────────┐
│ Category Patterns                   │
│ Weekday Patterns                    │
│ Monthly Patterns                    │
│ Anomaly Detection                   │
│ Risk Scoring                        │
│ Predictions                         │
└─────────────────────────────────────┘
        ↓
UserFinancialState (structured)
        ↓
Phi3InferenceEngine
        ↓
Friendly Response to User
```

## Key Differences from Traditional Chatbots

1. **No data fabrication** - All numbers come from learner
2. **Structured context** - LLM sees summaries, not raw data
3. **Offline-first** - Everything runs on device
4. **Battery-aware** - Models loaded on demand
5. **Privacy-focused** - No data leaves the phone

