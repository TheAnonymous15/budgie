# Budgie - AI-Powered Personal Finance App

## Technical Documentation Index

Welcome to the Budgie technical documentation. This index provides an overview of all major implementations and their documentation.

---

## 📚 Documentation Files

| Document | Description | Location |
|----------|-------------|----------|
| [Behavior Learning Engine](./BEHAVIOR_LEARNING_ENGINE.md) | Offline ML system for behavior prediction | `docs/BEHAVIOR_LEARNING_ENGINE.md` |
| [Conversational AI](./CONVERSATIONAL_AI.md) | NLU chat system documentation | `docs/CONVERSATIONAL_AI.md` |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BUDGIE APP                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         UI LAYER (Jetpack Compose)                   │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │Dashboard │ │ Expenses │ │  Income  │ │  Budget  │ │ AI Chat  │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │  Goals   │ │  Loans   │ │ Shopping │ │ Insights │ │  Export  │  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐ │
│  │                    VIEWMODEL LAYER                                     │ │
│  │                         MainViewModel                                  │ │
│  │  • Financial Data Management                                           │ │
│  │  • AI/ML Integration                                                   │ │
│  │  • State Management                                                    │ │
│  └─────────────────────────────────┼─────────────────────────────────────┘ │
│                                    │                                        │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐ │
│  │                       AI/ML LAYER                                      │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐                     │ │
│  │  │   Conversational    │  │ Behavior Learning   │                     │ │
│  │  │        AI           │  │      Engine         │                     │ │
│  │  │  • Intent Classify  │  │  • K-Means Cluster  │                     │ │
│  │  │  • Entity Extract   │  │  • Isolation Forest │                     │ │
│  │  │  • NLG Templates    │  │  • Time Series Pred │                     │ │
│  │  └─────────────────────┘  └─────────────────────┘                     │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐                     │ │
│  │  │  Financial Advisor  │  │  Shopping Analyzer  │                     │ │
│  │  │  • Insights Gen     │  │  • List Analysis    │                     │ │
│  │  │  • Investment Tips  │  │  • Recommendations  │                     │ │
│  │  └─────────────────────┘  └─────────────────────┘                     │ │
│  └─────────────────────────────────┼─────────────────────────────────────┘ │
│                                    │                                        │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐ │
│  │                      DATA LAYER                                        │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐                     │ │
│  │  │   Room Database     │  │  SharedPreferences  │                     │ │
│  │  │  • Expenses         │  │  • User Profile     │                     │ │
│  │  │  • Income           │  │  • Security PIN     │                     │ │
│  │  │  • Bills            │  │  • Onboarding       │                     │ │
│  │  │  • Budget           │  │  • AI Memory        │                     │ │
│  │  │  • Goals            │  │                     │                     │ │
│  │  │  • Loans            │  │                     │                     │ │
│  │  │  • Shopping Lists   │  │                     │                     │ │
│  │  └─────────────────────┘  └─────────────────────┘                     │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🤖 AI/ML Components

### 1. Behavior Learning Engine
**Location:** `app/src/main/java/com/example/budgie/ai/ml/BehaviorLearningEngine.kt`

Fully offline ML system that learns user financial behavior:
- **K-Means Clustering:** User behavior classification
- **Isolation Forest:** Anomaly detection
- **Time Series Prediction:** Future spending forecasts
- **Pattern Recognition:** Recurring expense detection
- **Risk Assessment:** Financial health scoring

[📖 Full Documentation](./BEHAVIOR_LEARNING_ENGINE.md)

### 2. Conversational AI
**Location:** `app/src/main/java/com/example/budgie/ai/conversational/ConversationalAI.kt`

Natural language chat interface:
- **Intent Classification:** Regex-based NLU
- **Entity Extraction:** NER for amounts, dates, categories
- **Memory Management:** Context persistence
- **Template NLG:** Natural response generation

[📖 Full Documentation](./CONVERSATIONAL_AI.md)

### 3. Financial Advisor
**Location:** `app/src/main/java/com/example/budgie/ai/FinancialAdvisor.kt`

Financial guidance engine:
- Spending insights generation
- Investment suggestions
- Wealth projections
- Budget recommendations

### 4. Shopping List Analyzer
**Location:** `app/src/main/java/com/example/budgie/ai/ShoppingListAnalyzer.kt`

Shopping optimization:
- Item necessity scoring
- Budget impact analysis
- Reduction recommendations

---

## 📱 Key Features

| Feature | Description | AI Component |
|---------|-------------|--------------|
| Dashboard | Financial overview | Behavior Engine |
| Expense Tracking | Log & categorize expenses | Pattern Recognition |
| Income Management | Track income sources | Time Series |
| Budget Planning | Set & monitor budgets | Risk Assessment |
| AI Chat | Natural language queries | Conversational AI |
| Goals | Savings goal tracking | Goal Prediction |
| Loans | Loan management | Loan Prediction |
| Shopping Lists | Smart shopping | Shopping Analyzer |
| Insights | AI-powered analysis | Financial Advisor |
| Export | PDF/Excel reports | - |
| Notifications | Smart reminders | Behavior Engine |
| Birthday Celebration | User birthday feature | - |
| Security | PIN/Biometric lock | - |

---

## 🔐 Security Features

1. **App Lock**
   - PIN protection (5-digit)
   - Biometric authentication
   - Session management

2. **Data Privacy**
   - All data stored locally
   - No external API calls
   - User controls data export

3. **Encryption**
   - PIN stored encrypted
   - Sensitive data protected

---

## 📊 Data Models

### Core Entities

| Entity | Table | Key Fields |
|--------|-------|------------|
| Expense | `expenses` | id, amount, category, date, title |
| Income | `incomes` | id, amount, source, date |
| Bill | `bills` | id, amount, category, dueDate |
| Budget | `budgets` | id, category, limit, month, year |
| Goal | `goals` | id, name, targetAmount, currentAmount |
| Loan | `loans` | id, amount, interestRate, monthlyPayment |
| ShoppingList | `shopping_lists` | id, name, totalBudget |
| ShoppingItem | `shopping_items` | id, listId, name, quantity, price |

### AI Data Models

| Model | Purpose |
|-------|---------|
| `UserBehaviorProfile` | User spending classification |
| `BehaviorSpendingPrediction` | Future spending forecast |
| `AnomalyReport` | Unusual transaction alert |
| `RiskAssessment` | Financial health score |
| `SpendingPattern` | Detected spending patterns |
| `AIResponse` | Chat response structure |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material3 |
| Database | Room |
| Async | Kotlin Coroutines + Flow |
| DI | Manual (ViewModel) |
| Charts | Custom Compose |
| PDF Export | iText |
| Excel Export | Apache POI |
| ML | Pure Kotlin (no external libs) |

---

## 📁 Project Structure

```
app/src/main/java/com/example/budgie/
├── ai/
│   ├── conversational/
│   │   └── ConversationalAI.kt      # Chat AI system
│   ├── ml/
│   │   ├── BehaviorLearningEngine.kt # ML engine
│   │   ├── TFLiteSpendingPredictor.kt
│   │   └── BudgetOptimizer.kt
│   ├── pipeline/
│   │   ├── FinancialAIPipeline.kt
│   │   ├── AnomalyDetector.kt
│   │   └── RiskScorer.kt
│   ├── AIModels.kt
│   ├── FinancialAdvisor.kt
│   └── ShoppingListAnalyzer.kt
├── data/
│   ├── database/
│   │   ├── BudgieDatabase.kt
│   │   └── dao/
│   ├── model/
│   │   ├── Expense.kt
│   │   ├── Income.kt
│   │   └── ...
│   ├── preferences/
│   │   └── UserPreferences.kt
│   └── repository/
│       └── FinanceRepository.kt
├── ui/
│   ├── navigation/
│   │   └── BudgieNavigation.kt
│   ├── screens/
│   │   ├── DashboardScreen.kt
│   │   ├── AIChatScreen.kt
│   │   ├── OnboardingScreen.kt
│   │   └── ...
│   ├── theme/
│   │   └── Theme.kt
│   └── viewmodel/
│       └── MainViewModel.kt
├── notifications/
│   └── NotificationManager.kt
└── MainActivity.kt
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog+
- JDK 17
- Android SDK 34

### Build & Run
```bash
# Clone the repository
git clone <repo-url>

# Open in Android Studio
# OR build from command line:
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

---

## 📝 Changelog

| Version | Date | Major Changes |
|---------|------|---------------|
| 1.0.0 | Dec 25, 2025 | Initial release |
| 1.1.0 | Dec 26, 2025 | AI Chat, Behavior Learning Engine |

---

## 👥 Team

Budgie Development Team

---

## 📄 License

Proprietary - All rights reserved

