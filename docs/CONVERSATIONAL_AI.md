# Budgie Conversational AI System

## Overview

The **Conversational AI System** is a multimodal, fully offline natural language processing system that powers the Budgie AI Chat feature. It enables users to interact with their financial data through natural language queries.

**File Location:** `app/src/main/java/com/example/budgie/ai/conversational/ConversationalAI.kt`

**UI Screen:** `app/src/main/java/com/example/budgie/ui/screens/AIChatScreen.kt`

**Created:** December 26, 2025

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        CONVERSATIONAL AI PIPELINE                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│    User Input                                                               │
│        │                                                                    │
│        ▼                                                                    │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                │
│  │  Preprocess  │────▶│   Intent     │────▶│   Entity     │                │
│  │    Input     │     │  Classifier  │     │  Extractor   │                │
│  └──────────────┘     └──────────────┘     └──────────────┘                │
│                              │                    │                         │
│                              ▼                    ▼                         │
│                       ┌──────────────┐     ┌──────────────┐                │
│                       │   Memory     │     │  Reasoning   │                │
│                       │   Manager    │     │   Engine     │                │
│                       └──────────────┘     └──────────────┘                │
│                                                   │                         │
│                                                   ▼                         │
│                       ┌──────────────┐     ┌──────────────┐                │
│                       │  Financial   │────▶│  Response    │                │
│                       │   Queries    │     │  Generator   │                │
│                       └──────────────┘     └──────────────┘                │
│                                                   │                         │
│                                                   ▼                         │
│                                            AI Response                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Components

### 1. Input Preprocessor

**Purpose:** Normalize and tokenize user input for consistent processing.

**Operations:**
1. Convert to lowercase
2. Trim whitespace
3. Collapse multiple spaces
4. Remove special characters (except $ % . , ? !)

**Example:**
```
Input:  "  How much did I SPEND on Food?!  "
Output: "how much did i spend on food"
```

---

### 2. Intent Classifier

**Purpose:** Determine what the user wants to know or do.

**Classification Method:** Regex-based pattern matching with confidence scoring.

**Supported Intents:**

| Category | Intent | Example Queries |
|----------|--------|-----------------|
| **Spending** | `QUERY_SPENDING_TOTAL` | "How much did I spend?" |
| | `QUERY_SPENDING_CATEGORY` | "Show my food expenses" |
| | `QUERY_SPENDING_PERIOD` | "What did I spend last month?" |
| | `QUERY_SPENDING_COMPARISON` | "Compare to last month" |
| **Income** | `QUERY_INCOME_TOTAL` | "What's my income?" |
| | `QUERY_INCOME_SOURCE` | "Show salary income" |
| **Analysis** | `QUERY_TRENDS` | "Show my spending trends" |
| | `QUERY_INSIGHTS` | "Give me financial advice" |
| | `QUERY_ANOMALIES` | "Any unusual spending?" |
| | `QUERY_PREDICTIONS` | "What will I spend next month?" |
| **Budget** | `QUERY_BUDGET_STATUS` | "How's my budget?" |
| | `QUERY_BUDGET_REMAINING` | "Budget remaining?" |
| **Goals** | `QUERY_GOALS_PROGRESS` | "How are my savings goals?" |
| | `QUERY_LOANS_STATUS` | "Show my loans" |
| **Conversation** | `GREETING` | "Hi", "Hello", "How are you?" |
| | `FAREWELL` | "Bye", "See you later" |
| | `THANKS` | "Thanks", "Thank you" |
| | `HELP` | "What can you do?" |
| **Fallback** | `OUT_OF_SCOPE` | Non-financial queries |
| | `UNKNOWN` | Unclear queries |

**Confidence Calculation:**
```kotlin
confidence = (matchLength / inputLength) * 0.8 + exactBonus
// exactBonus = 0.2 if match covers >60% of input
```

---

### 3. Entity Extractor (NER)

**Purpose:** Extract specific values from the user's query.

**Entities Extracted:**

| Entity Type | Pattern | Example |
|-------------|---------|---------|
| `AMOUNT` | `$?[0-9]+[,.]?[0-9]*` | "$500", "1,000" |
| `PERCENTAGE` | `[0-9]+\.?[0-9]*%` | "20%", "15.5%" |
| `DATE` | Keywords + patterns | "today", "last month" |
| `DATE_RANGE` | Period keywords | "this week", "last year" |
| `CATEGORY` | Category keywords | "food", "transport" |

**Category Keywords:**
```kotlin
val categoryKeywords = mapOf(
    "food" to "FOOD",
    "eat" to "FOOD",
    "restaurant" to "FOOD",
    "transport" to "TRANSPORT",
    "uber" to "TRANSPORT",
    "shopping" to "SHOPPING",
    "entertainment" to "ENTERTAINMENT",
    "utilities" to "UTILITIES",
    "health" to "HEALTH",
    "rent" to "RENT"
    // ... more mappings
)
```

**Time Period Parsing:**
| Input | Parsed Period |
|-------|---------------|
| "today" | Start of today → now |
| "yesterday" | All of yesterday |
| "this week" | Start of week → now |
| "last week" | Previous full week |
| "this month" | Start of month → now |
| "last month" | Previous full month |
| "this year" | Start of year → now |
| "last year" | Previous full year |

---

### 4. Memory Manager

**Purpose:** Maintain conversation context and user preferences.

**Storage:**
- SharedPreferences for persistence
- In-memory for session state

**Managed Data:**
| Type | Storage | Purpose |
|------|---------|---------|
| User Preferences | SharedPrefs | Long-term settings |
| Conversation Facts | SharedPrefs | Learned information |
| Session Context | Memory | Current conversation |
| Topic Stack | Memory | Conversation focus |

**Context Structure:**
```kotlin
data class ConversationContext(
    val sessionId: String,
    val turnCount: Int,
    val lastIntent: UserIntent?,
    val lastEntities: List<ExtractedEntity>,
    val topicStack: MutableList<String>,
    val userPreferences: MutableMap<String, Any>
)
```

---

### 5. Reasoning Engine

**Purpose:** Plan the appropriate response based on intent and context.

**Response Planning:**
For each intent, the engine determines:
1. What data is required
2. Which template to use
3. What follow-up suggestions to offer

**Example Response Plan:**
```kotlin
UserIntent.QUERY_SPENDING_TOTAL -> ResponsePlan(
    intent = input.intent,
    dataRequired = listOf("total_spending", "expense_count", "spending_by_category"),
    templateKey = "spending_total",
    followUpSuggestions = listOf(
        "Show breakdown by category",
        "Compare to last month",
        "Show my trends"
    )
)
```

---

### 6. Response Generator (Template-based NLG)

**Purpose:** Generate natural language responses using templates.

**Template System:**
- Multiple templates per response type
- Random selection for variety
- Variable substitution

**Example Templates:**
```kotlin
"spending_total" to listOf(
    "You've spent {totalSpending} across {expenseCount} transactions...",
    "Your total spending is {totalSpending} ({expenseCount} transactions)..."
)

"greeting" to listOf(
    "Hello! 👋 I'm your Budgie AI financial advisor...",
    "Hi there! 😊 I'm doing wonderful, thanks for asking!...",
    "Hey! Great to hear from you! 👋..."
)
```

**Variable Substitution:**
| Variable | Source | Format |
|----------|--------|--------|
| `{totalSpending}` | Financial data | Currency |
| `{totalIncome}` | Financial data | Currency |
| `{netSavings}` | Financial data | Currency |
| `{savingsRate}` | Financial data | Percentage |
| `{expenseCount}` | Financial data | Number |
| `{categoryBreakdown}` | Computed | List with emojis |
| `{insight}` | Generated | Advisory text |
| `{recommendations}` | Generated | Action items |

---

## Financial Data Provider Interface

The AI connects to financial data through this interface:

```kotlin
interface FinancialDataProvider {
    fun getTotalExpenses(): Double
    fun getTotalIncome(): Double
    fun getNetSavings(): Double
    fun getSavingsRate(): Double
    fun getExpensesByCategory(): Map<String, Double>
    fun getTopSpendingCategory(): Pair<String, Double>?
    fun getExpenseCount(): Int
    fun getRecentExpenses(limit: Int): List<Any>
    fun getMonthlyTrend(): Map<String, Double>
    fun getBudgetStatus(): Map<String, Any>
    fun getGoalsProgress(): Map<String, Any>
    fun getLoansStatus(): Map<String, Any>
}
```

---

## API Reference

### Main Entry Point

```kotlin
fun processInput(
    userInput: String,
    financialData: FinancialDataProvider
): Flow<AIResponse>
```

### Response Structure

```kotlin
data class AIResponse(
    val text: String,              // The response message
    val confidence: Float,         // How confident the AI is (0-1)
    val intent: UserIntent,        // Detected intent
    val suggestions: List<String>, // Follow-up suggestions
    val data: Map<String, Any>?    // Raw data (optional)
)
```

### Reset Conversation

```kotlin
fun resetConversation()
```

---

## UI Integration (AIChatScreen)

### Features

1. **Chat Interface**
   - Message bubbles (user/AI)
   - Typing indicator
   - Auto-scroll to bottom

2. **AI Avatar**
   - Pulse animation when active
   - Psychology icon

3. **Quick Suggestions**
   - Clickable chips
   - Context-aware suggestions

4. **Input Field**
   - Text input
   - Send button with animation
   - Keyboard actions

### Usage

```kotlin
@Composable
fun AIChatScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
)
```

### Theme Colors

```kotlin
private val WealthNavy = Color(0xFF0B1F2A)
private val WealthNavyLight = Color(0xFF0D2E3D)
private val WealthEmerald = Color(0xFF0FAE96)
private val WealthTeal = Color(0xFF0B8F7A)
private val WealthGold = Color(0xFFC9A14A)
private val WealthSoftWhite = Color(0xFFE6F1F0)
```

---

## Example Conversations

### Spending Query
```
User: "How much did I spend this month?"

AI: "You've spent $2,450.00 across 47 transactions this period.

**Top Categories:**
🍔 Food: $650.00
🚗 Transport: $420.00
🛒 Shopping: $380.00

📊 Your spending pattern is stable"
```

### Greeting
```
User: "Hi, how are you?"

AI: "Hello! 👋 I'm your Budgie AI financial advisor.

I'm doing great and ready to help you with your finances!

Your current net position is $1,250.00.

How can I help you today? You can ask me about:
• Your spending patterns
• Income & savings
• Budget status
• Financial insights"
```

### Out of Scope
```
User: "What's the weather today?"

AI: "I'm focused on helping you with your finances. I can answer questions about:

• Spending & expenses
• Income & savings
• Budgets & goals
• Financial trends

Is there something financial I can help with?"
```

---

## Supported Conversational Patterns

### Greetings
- hi, hello, hey
- good morning/afternoon/evening
- how are you, how're you
- what's up, sup, wassup
- howdy, yo, hiya

### Farewells
- bye, goodbye, see you
- good night, gotta go, gtg
- talk later, catch you later, ttyl
- cya, c ya

### Thanks
- thanks, thank you, thx, ty
- appreciate, helpful, cheers
- you're the best, awesome

### Help
- help, what can you do
- who are you, what are you
- show me what you can do

---

## Safety & Boundaries

The AI is designed with clear boundaries:

1. **Financial Focus Only**
   - Rejects non-financial queries politely
   - Redirects to financial topics

2. **No Speculation**
   - Only uses available data
   - Acknowledges when data is insufficient

3. **No Legal/Tax Advice**
   - Stays within general financial guidance
   - Recommends professionals for complex matters

4. **Privacy Aware**
   - All processing is local
   - No data sent to external servers

---

## Performance

| Metric | Value |
|--------|-------|
| Intent Classification | < 10ms |
| Entity Extraction | < 5ms |
| Response Generation | < 50ms |
| Total Response Time | < 100ms |

---

## Future Improvements

1. **Voice Input:** Add speech-to-text support
2. **Voice Output:** Add text-to-speech for responses
3. **Context Memory:** Remember previous conversations
4. **Proactive Insights:** Offer unprompted financial tips
5. **Multi-language:** Support additional languages
6. **Sentiment Analysis:** Detect user mood/stress

---

## Dependencies

- Kotlin Coroutines for async processing
- Jetpack Compose for UI
- Material3 for design system
- No external NLP libraries (pure Kotlin)

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | Dec 26, 2025 | Initial implementation |
| 1.1.0 | Dec 26, 2025 | Enhanced greeting patterns |

---

## Author

Budgie Development Team

---

## License

Proprietary - All rights reserved

