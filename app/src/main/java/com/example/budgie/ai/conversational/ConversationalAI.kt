package com.example.budgie.ai.conversational

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.NumberFormat
import java.util.*

/**
 * Conversational AI System for Budgie
 *
 * Architecture:
 * 1. Input Processing → Tokenization, Entity Extraction
 * 2. Intent Classification → DistilBERT-style classification
 * 3. Entity Extraction → NER for amounts, dates, categories
 * 4. Memory Layer → Context persistence
 * 5. Reasoning Engine → Rule-based planning
 * 6. Finance Execution → Integration with ML pipeline
 * 7. Response Planning → Safety & relevance filtering
 * 8. NLG → Template-based generation
 */

// ============= Data Models =============

enum class UserIntent(val confidence: Float = 0.9f) {
    // Spending queries
    QUERY_SPENDING_TOTAL,
    QUERY_SPENDING_CATEGORY,
    QUERY_SPENDING_PERIOD,
    QUERY_SPENDING_COMPARISON,

    // Income queries
    QUERY_INCOME_TOTAL,
    QUERY_INCOME_SOURCE,

    // Analysis queries
    QUERY_TRENDS,
    QUERY_INSIGHTS,
    QUERY_ANOMALIES,
    QUERY_PREDICTIONS,

    // Budget queries
    QUERY_BUDGET_STATUS,
    QUERY_BUDGET_REMAINING,

    // Goals & Loans
    QUERY_GOALS_PROGRESS,
    QUERY_LOANS_STATUS,

    // Actions
    ACTION_SET_BUDGET,
    ACTION_CREATE_GOAL,

    // Conversation
    GREETING,
    FAREWELL,
    THANKS,
    HELP,

    // Fallback
    OUT_OF_SCOPE,
    UNKNOWN
}

data class ExtractedEntity(
    val type: EntityType,
    val value: String,
    val normalizedValue: Any?,
    val confidence: Float,
    val startPos: Int,
    val endPos: Int
)

enum class EntityType {
    AMOUNT,
    DATE,
    DATE_RANGE,
    CATEGORY,
    PERCENTAGE,
    COMPARISON_OPERATOR,
    TIME_PERIOD
}

data class TimePeriod(
    val type: PeriodType,
    val startDate: Long,
    val endDate: Long
)

enum class PeriodType {
    TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK,
    THIS_MONTH, LAST_MONTH, THIS_YEAR, LAST_YEAR,
    CUSTOM
}

data class ConversationContext(
    val sessionId: String = UUID.randomUUID().toString(),
    val turnCount: Int = 0,
    val lastIntent: UserIntent? = null,
    val lastEntities: List<ExtractedEntity> = emptyList(),
    val topicStack: MutableList<String> = mutableListOf(),
    val userPreferences: MutableMap<String, Any> = mutableMapOf()
)

data class ProcessedInput(
    val originalText: String,
    val normalizedText: String,
    val tokens: List<String>,
    val intent: UserIntent,
    val intentConfidence: Float,
    val entities: List<ExtractedEntity>,
    val context: ConversationContext
)

data class ResponsePlan(
    val intent: UserIntent,
    val dataRequired: List<String>,
    val templateKey: String,
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val followUpSuggestions: List<String> = emptyList()
)

data class AIResponse(
    val text: String,
    val confidence: Float,
    val intent: UserIntent,
    val suggestions: List<String> = emptyList(),
    val data: Map<String, Any>? = null
)

// ============= Main Conversational AI Class =============

class ConversationalAI(private val context: Context) {

    private val intentClassifier = IntentClassifier()
    private val entityExtractor = EntityExtractor()
    private val memoryManager = MemoryManager(context)
    private val reasoningEngine = ReasoningEngine()
    private val responseGenerator = ResponseGenerator()

    private var conversationContext = ConversationContext()

    /**
     * Main entry point for processing user input
     */
    fun processInput(
        userInput: String,
        financialData: FinancialDataProvider
    ): Flow<AIResponse> = flow {
        // 1. Preprocess input
        val normalizedInput = preprocessInput(userInput)

        // 2. Classify intent
        val (intent, intentConfidence) = intentClassifier.classify(normalizedInput)

        // 3. Extract entities
        val entities = entityExtractor.extract(normalizedInput)

        // 4. Build processed input
        val processedInput = ProcessedInput(
            originalText = userInput,
            normalizedText = normalizedInput,
            tokens = tokenize(normalizedInput),
            intent = intent,
            intentConfidence = intentConfidence,
            entities = entities,
            context = conversationContext
        )

        // 5. Update memory/context
        updateContext(processedInput)

        // 6. Plan response
        val responsePlan = reasoningEngine.planResponse(processedInput, financialData)

        // 7. Execute financial queries if needed
        val queryResults = executeFinancialQueries(responsePlan, financialData)
        responsePlan.variables.putAll(queryResults)

        // 8. Generate response
        val response = responseGenerator.generate(responsePlan, processedInput)

        emit(response)
    }

    private fun preprocessInput(input: String): String {
        return input
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9\\s$%.,?!]"), "")
    }

    private fun tokenize(text: String): List<String> {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    private fun updateContext(input: ProcessedInput) {
        conversationContext = conversationContext.copy(
            turnCount = conversationContext.turnCount + 1,
            lastIntent = input.intent,
            lastEntities = input.entities
        )

        // Update topic stack
        when (input.intent) {
            UserIntent.QUERY_SPENDING_TOTAL,
            UserIntent.QUERY_SPENDING_CATEGORY,
            UserIntent.QUERY_SPENDING_PERIOD -> {
                if (!conversationContext.topicStack.contains("spending")) {
                    conversationContext.topicStack.add(0, "spending")
                }
            }
            UserIntent.QUERY_INCOME_TOTAL,
            UserIntent.QUERY_INCOME_SOURCE -> {
                if (!conversationContext.topicStack.contains("income")) {
                    conversationContext.topicStack.add(0, "income")
                }
            }
            else -> {}
        }

        // Keep topic stack manageable
        if (conversationContext.topicStack.size > 5) {
            conversationContext.topicStack.removeLast()
        }
    }

    private fun executeFinancialQueries(
        plan: ResponsePlan,
        data: FinancialDataProvider
    ): Map<String, Any> {
        val results = mutableMapOf<String, Any>()

        plan.dataRequired.forEach { requirement ->
            when (requirement) {
                "total_spending" -> results["totalSpending"] = data.getTotalExpenses()
                "total_income" -> results["totalIncome"] = data.getTotalIncome()
                "net_savings" -> results["netSavings"] = data.getNetSavings()
                "savings_rate" -> results["savingsRate"] = data.getSavingsRate()
                "spending_by_category" -> results["categoryBreakdown"] = data.getExpensesByCategory()
                "top_category" -> {
                    val top = data.getTopSpendingCategory()
                    if (top != null) {
                        results["topCategory"] = top
                    }
                }
                "expense_count" -> results["expenseCount"] = data.getExpenseCount()
                "recent_expenses" -> results["recentExpenses"] = data.getRecentExpenses(5)
                "monthly_trend" -> results["monthlyTrend"] = data.getMonthlyTrend()
                "budget_status" -> results["budgetStatus"] = data.getBudgetStatus()
                "goals_progress" -> results["goalsProgress"] = data.getGoalsProgress()
                "loans_status" -> results["loansStatus"] = data.getLoansStatus()
            }
        }

        return results
    }

    fun resetConversation() {
        conversationContext = ConversationContext()
    }
}

// ============= Intent Classifier =============

class IntentClassifier {

    // Pattern-based classification with confidence scores
    private val intentPatterns = mapOf(
        // Spending queries
        UserIntent.QUERY_SPENDING_TOTAL to listOf(
            Regex("(how much|what).*(spend|spent|spending)"),
            Regex("(total|all).*(expense|spending)"),
            Regex("(show|tell).*(expense|spending)")
        ),
        UserIntent.QUERY_SPENDING_CATEGORY to listOf(
            Regex("(how much|what).*(spend|spent).*(on|for)\\s+(\\w+)"),
            Regex("(\\w+)\\s+(expense|spending|cost)"),
            Regex("(food|transport|shopping|entertainment|utilities|health|rent).*spend")
        ),
        UserIntent.QUERY_SPENDING_PERIOD to listOf(
            Regex("(this|last)\\s+(week|month|year).*(spend|expense)"),
            Regex("(spend|expense).*(this|last)\\s+(week|month|year)"),
            Regex("(yesterday|today).*(spend|expense)")
        ),
        UserIntent.QUERY_SPENDING_COMPARISON to listOf(
            Regex("(compare|comparison|versus|vs)"),
            Regex("(more|less|higher|lower)\\s+than"),
            Regex("(increase|decrease|change).*spend")
        ),

        // Income queries
        UserIntent.QUERY_INCOME_TOTAL to listOf(
            Regex("(how much|what).*(earn|income|make|salary)"),
            Regex("(total|all)\\s+income"),
            Regex("(show|tell).*income")
        ),

        // Analysis queries
        UserIntent.QUERY_TRENDS to listOf(
            Regex("(trend|pattern|habit)"),
            Regex("(how|what).*(usually|typically|normally)"),
            Regex("(analyze|analysis)")
        ),
        UserIntent.QUERY_INSIGHTS to listOf(
            Regex("(insight|advice|suggestion|recommend)"),
            Regex("(what|how).*(should|can|could)\\s+i"),
            Regex("(improve|better|optimize)")
        ),
        UserIntent.QUERY_ANOMALIES to listOf(
            Regex("(unusual|strange|weird|abnormal)"),
            Regex("(overspend|over\\s*spend|too much)"),
            Regex("(something|anything)\\s+wrong")
        ),
        UserIntent.QUERY_PREDICTIONS to listOf(
            Regex("(predict|forecast|expect|projection)"),
            Regex("(will|going to).*(spend|save)"),
            Regex("(end of|by)\\s+(month|year)")
        ),

        // Budget queries
        UserIntent.QUERY_BUDGET_STATUS to listOf(
            Regex("(how|what).*(budget)"),
            Regex("budget\\s+(status|progress|left|remaining)")
        ),

        // Goals & Loans
        UserIntent.QUERY_GOALS_PROGRESS to listOf(
            Regex("(goal|target|saving for)"),
            Regex("(how|what).*(progress|close)")
        ),
        UserIntent.QUERY_LOANS_STATUS to listOf(
            Regex("(loan|debt|owe|owing)"),
            Regex("(how much|what).*(repay|pay back)")
        ),

        // Conversation
        UserIntent.GREETING to listOf(
            Regex("^(hi|hello|hey|good\\s+(morning|afternoon|evening))"),
            Regex("^(what'?s up|howdy|yo|hiya|heya)"),
            Regex("^(how are you|how're you|how r u|how do you do)"),
            Regex("^(are you there|you there|anyone there)"),
            Regex("^(sup|wassup|whats up)"),
            Regex("^(greetings|salutations)")
        ),
        UserIntent.FAREWELL to listOf(
            Regex("(bye|goodbye|see you|later|cya|c ya)"),
            Regex("(thanks|thank you).*bye"),
            Regex("(good night|goodnight|gotta go|gtg)"),
            Regex("(talk later|catch you later|ttyl)")
        ),
        UserIntent.THANKS to listOf(
            Regex("^(thanks|thank you|thx|ty|cheers)"),
            Regex("(appreciate|helpful|great help|much appreciated)"),
            Regex("(you're the best|you rock|awesome)")
        ),
        UserIntent.HELP to listOf(
            Regex("^(help|what can you do|what do you do)"),
            Regex("(how|what).*(use|ask|work)"),
            Regex("(show me|tell me).*(can do|capable|features)"),
            Regex("(who are you|what are you)")
        )
    )

    fun classify(input: String): Pair<UserIntent, Float> {
        var bestIntent = UserIntent.UNKNOWN
        var bestScore = 0f

        for ((intent, patterns) in intentPatterns) {
            for (pattern in patterns) {
                if (pattern.containsMatchIn(input)) {
                    val score = calculateConfidence(input, pattern)
                    if (score > bestScore) {
                        bestScore = score
                        bestIntent = intent
                    }
                }
            }
        }

        // Check for out of scope
        if (bestScore < 0.3f && !isFinancialContext(input)) {
            return UserIntent.OUT_OF_SCOPE to 0.8f
        }

        return bestIntent to bestScore.coerceIn(0.5f, 0.95f)
    }

    private fun calculateConfidence(input: String, pattern: Regex): Float {
        val match = pattern.find(input) ?: return 0f
        val matchLength = match.value.length
        val inputLength = input.length

        // Higher confidence for longer matches relative to input
        val lengthScore = (matchLength.toFloat() / inputLength).coerceIn(0.3f, 1f)

        // Bonus for exact/near matches
        val exactBonus = if (matchLength > inputLength * 0.6) 0.2f else 0f

        return (lengthScore * 0.8f + exactBonus).coerceIn(0f, 1f)
    }

    private fun isFinancialContext(input: String): Boolean {
        val financialTerms = listOf(
            "spend", "expense", "money", "cost", "pay", "income", "earn",
            "budget", "save", "saving", "bill", "loan", "debt", "goal",
            "invest", "dollar", "$", "percent", "%"
        )
        return financialTerms.any { input.contains(it) }
    }
}

// ============= Entity Extractor =============

class EntityExtractor {

    private val amountPattern = Regex("\\$?([0-9]+[,.]?[0-9]*)")
    private val percentPattern = Regex("([0-9]+\\.?[0-9]*)\\s*%")
    private val datePatterns = listOf(
        Regex("(today|yesterday|tomorrow)"),
        Regex("(this|last|next)\\s+(week|month|year)"),
        Regex("(january|february|march|april|may|june|july|august|september|october|november|december)"),
        Regex("([0-9]{1,2})/([0-9]{1,2})(/[0-9]{2,4})?"),
        Regex("([0-9]{4})-([0-9]{2})-([0-9]{2})")
    )
    private val categoryKeywords = mapOf(
        "food" to "FOOD",
        "eat" to "FOOD",
        "restaurant" to "FOOD",
        "dining" to "FOOD",
        "groceries" to "FOOD",
        "transport" to "TRANSPORT",
        "uber" to "TRANSPORT",
        "gas" to "TRANSPORT",
        "fuel" to "TRANSPORT",
        "car" to "TRANSPORT",
        "shopping" to "SHOPPING",
        "clothes" to "SHOPPING",
        "amazon" to "SHOPPING",
        "entertainment" to "ENTERTAINMENT",
        "movies" to "ENTERTAINMENT",
        "netflix" to "ENTERTAINMENT",
        "games" to "ENTERTAINMENT",
        "utilities" to "UTILITIES",
        "electric" to "UTILITIES",
        "water" to "UTILITIES",
        "internet" to "UTILITIES",
        "health" to "HEALTH",
        "medical" to "HEALTH",
        "doctor" to "HEALTH",
        "pharmacy" to "HEALTH",
        "rent" to "RENT",
        "housing" to "RENT",
        "mortgage" to "RENT"
    )

    fun extract(input: String): List<ExtractedEntity> {
        val entities = mutableListOf<ExtractedEntity>()

        // Extract amounts
        amountPattern.findAll(input).forEach { match ->
            val value = match.groupValues[1].replace(",", "")
            entities.add(ExtractedEntity(
                type = EntityType.AMOUNT,
                value = match.value,
                normalizedValue = value.toDoubleOrNull(),
                confidence = 0.9f,
                startPos = match.range.first,
                endPos = match.range.last
            ))
        }

        // Extract percentages
        percentPattern.findAll(input).forEach { match ->
            entities.add(ExtractedEntity(
                type = EntityType.PERCENTAGE,
                value = match.value,
                normalizedValue = match.groupValues[1].toDoubleOrNull(),
                confidence = 0.9f,
                startPos = match.range.first,
                endPos = match.range.last
            ))
        }

        // Extract dates/time periods
        datePatterns.forEach { pattern ->
            pattern.findAll(input).forEach { match ->
                val period = parseTimePeriod(match.value)
                entities.add(ExtractedEntity(
                    type = if (period != null) EntityType.DATE_RANGE else EntityType.DATE,
                    value = match.value,
                    normalizedValue = period,
                    confidence = 0.85f,
                    startPos = match.range.first,
                    endPos = match.range.last
                ))
            }
        }

        // Extract categories
        categoryKeywords.forEach { (keyword, category) ->
            val idx = input.indexOf(keyword)
            if (idx >= 0) {
                entities.add(ExtractedEntity(
                    type = EntityType.CATEGORY,
                    value = keyword,
                    normalizedValue = category,
                    confidence = 0.9f,
                    startPos = idx,
                    endPos = idx + keyword.length
                ))
            }
        }

        return entities.distinctBy { "${it.type}_${it.startPos}" }
    }

    private fun parseTimePeriod(text: String): TimePeriod? {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        return when {
            text.contains("today") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                TimePeriod(PeriodType.TODAY, start, now)
            }
            text.contains("yesterday") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val end = calendar.timeInMillis
                TimePeriod(PeriodType.YESTERDAY, start, end)
            }
            text.contains("this week") -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val start = calendar.timeInMillis
                TimePeriod(PeriodType.THIS_WEEK, start, now)
            }
            text.contains("last week") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                val end = calendar.timeInMillis
                TimePeriod(PeriodType.LAST_WEEK, start, end)
            }
            text.contains("this month") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val start = calendar.timeInMillis
                TimePeriod(PeriodType.THIS_MONTH, start, now)
            }
            text.contains("last month") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                val end = calendar.timeInMillis
                TimePeriod(PeriodType.LAST_MONTH, start, end)
            }
            text.contains("this year") -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val start = calendar.timeInMillis
                TimePeriod(PeriodType.THIS_YEAR, start, now)
            }
            text.contains("last year") -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.MONTH, 11)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                val end = calendar.timeInMillis
                TimePeriod(PeriodType.LAST_YEAR, start, end)
            }
            else -> null
        }
    }
}

// ============= Memory Manager =============

class MemoryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("budgie_ai_memory", Context.MODE_PRIVATE)

    fun saveUserPreference(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getUserPreference(key: String): String? {
        return prefs.getString(key, null)
    }

    fun saveConversationFact(fact: String) {
        val facts = getConversationFacts().toMutableList()
        facts.add(fact)
        if (facts.size > 50) facts.removeAt(0)
        prefs.edit().putStringSet("conversation_facts", facts.toSet()).apply()
    }

    fun getConversationFacts(): List<String> {
        return prefs.getStringSet("conversation_facts", emptySet())?.toList() ?: emptyList()
    }
}

// ============= Reasoning Engine =============

class ReasoningEngine {

    fun planResponse(
        input: ProcessedInput,
        data: FinancialDataProvider
    ): ResponsePlan {
        return when (input.intent) {
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

            UserIntent.QUERY_SPENDING_CATEGORY -> {
                val category = input.entities
                    .find { it.type == EntityType.CATEGORY }
                    ?.normalizedValue as? String

                ResponsePlan(
                    intent = input.intent,
                    dataRequired = listOf("spending_by_category", "recent_expenses"),
                    templateKey = "spending_category",
                    variables = mutableMapOf("targetCategory" to (category ?: "all")),
                    followUpSuggestions = listOf(
                        "How does this compare to last month?",
                        "What's my overall spending?"
                    )
                )
            }

            UserIntent.QUERY_SPENDING_PERIOD -> {
                val period = input.entities
                    .find { it.type == EntityType.DATE_RANGE }
                    ?.normalizedValue as? TimePeriod

                ResponsePlan(
                    intent = input.intent,
                    dataRequired = listOf("total_spending", "spending_by_category"),
                    templateKey = "spending_period",
                    variables = mutableMapOf("period" to (period ?: TimePeriod(
                        PeriodType.THIS_MONTH,
                        System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                        System.currentTimeMillis()
                    )))
                )
            }

            UserIntent.QUERY_INCOME_TOTAL -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("total_income", "net_savings", "savings_rate"),
                templateKey = "income_total"
            )

            UserIntent.QUERY_TRENDS -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("monthly_trend", "spending_by_category", "top_category"),
                templateKey = "trends_analysis",
                followUpSuggestions = listOf(
                    "How can I improve?",
                    "Where am I overspending?"
                )
            )

            UserIntent.QUERY_INSIGHTS -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("total_spending", "total_income", "savings_rate", "top_category", "monthly_trend"),
                templateKey = "insights"
            )

            UserIntent.QUERY_ANOMALIES -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("spending_by_category", "monthly_trend"),
                templateKey = "anomalies"
            )

            UserIntent.QUERY_PREDICTIONS -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("monthly_trend", "total_spending"),
                templateKey = "predictions"
            )

            UserIntent.QUERY_BUDGET_STATUS -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("budget_status", "total_spending"),
                templateKey = "budget_status"
            )

            UserIntent.QUERY_GOALS_PROGRESS -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("goals_progress"),
                templateKey = "goals_progress"
            )

            UserIntent.QUERY_LOANS_STATUS -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("loans_status"),
                templateKey = "loans_status"
            )

            UserIntent.GREETING -> ResponsePlan(
                intent = input.intent,
                dataRequired = listOf("net_savings"),
                templateKey = "greeting"
            )

            UserIntent.HELP -> ResponsePlan(
                intent = input.intent,
                dataRequired = emptyList(),
                templateKey = "help"
            )

            UserIntent.THANKS -> ResponsePlan(
                intent = input.intent,
                dataRequired = emptyList(),
                templateKey = "thanks"
            )

            UserIntent.FAREWELL -> ResponsePlan(
                intent = input.intent,
                dataRequired = emptyList(),
                templateKey = "farewell"
            )

            UserIntent.OUT_OF_SCOPE -> ResponsePlan(
                intent = input.intent,
                dataRequired = emptyList(),
                templateKey = "out_of_scope"
            )

            else -> ResponsePlan(
                intent = input.intent,
                dataRequired = emptyList(),
                templateKey = "unknown"
            )
        }
    }
}

// ============= Response Generator (Template-based NLG) =============

class ResponseGenerator {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private val templates = mapOf(
        "spending_total" to listOf(
            "You've spent {totalSpending} across {expenseCount} transactions this period.\n\n**Top Categories:**\n{categoryBreakdown}\n\n{insight}",
            "Your total spending is {totalSpending} ({expenseCount} transactions).\n\nHere's how it breaks down:\n{categoryBreakdown}"
        ),
        "spending_category" to listOf(
            "For {category}, you've spent {categoryAmount} ({percentage}% of total).\n\n**Recent {category} expenses:**\n{recentList}\n\n{insight}",
            "Your {category} spending: {categoryAmount}\nThis is {percentage}% of your total expenses.\n\n{insight}"
        ),
        "spending_period" to listOf(
            "During {periodName}, you spent {totalSpending}.\n\n**Breakdown:**\n{categoryBreakdown}\n\n{comparison}"
        ),
        "income_total" to listOf(
            "Your total income is {totalIncome}.\n\n📊 **Financial Health:**\n• Net Savings: {netSavings}\n• Savings Rate: {savingsRate}%\n\n{insight}"
        ),
        "trends_analysis" to listOf(
            "**Your Spending Trends**\n\n{trendAnalysis}\n\n**Top Category:** {topCategory}\n\n{insight}"
        ),
        "insights" to listOf(
            "**Financial Insights**\n\n💰 Income: {totalIncome}\n💸 Spending: {totalSpending}\n📈 Savings Rate: {savingsRate}%\n\n**Key Observations:**\n{observations}\n\n**Recommendations:**\n{recommendations}"
        ),
        "anomalies" to listOf(
            "**Spending Analysis**\n\n{anomalyReport}\n\n{recommendations}"
        ),
        "predictions" to listOf(
            "**Spending Forecast**\n\n📈 Based on your current pattern:\n{predictions}\n\n{advice}"
        ),
        "budget_status" to listOf(
            "**Budget Status**\n\n{budgetSummary}\n\n{advice}"
        ),
        "goals_progress" to listOf(
            "**Goals Progress**\n\n{goalsSummary}\n\n{motivation}"
        ),
        "loans_status" to listOf(
            "**Loans Overview**\n\n{loansSummary}\n\n{advice}"
        ),
        "greeting" to listOf(
            "Hello! 👋 I'm your Budgie AI financial advisor.\n\nI'm doing great and ready to help you with your finances!\n\nYour current net position is {netSavings}.\n\nHow can I help you today? You can ask me about:\n• Your spending patterns\n• Income & savings\n• Budget status\n• Financial insights",
            "Hi there! 😊 I'm doing wonderful, thanks for asking!\n\nI'm Budgie, your personal finance companion.\n\nQuick snapshot: {netSavings} in savings.\n\nWhat would you like to know?",
            "Hey! Great to hear from you! 👋\n\nI'm your friendly financial advisor, ready to help!\n\nYour finances at a glance: {netSavings} net position.\n\nAsk me anything about your money!",
            "Hello! I'm doing fantastic and excited to help you! 🌟\n\nI'm Budgie - your AI-powered financial bestie.\n\nWhat can I help you with today?"
        ),
        "help" to listOf(
            "I'm Budgie, your AI financial advisor! 🤖💰\n\nHere's what I can do:\n\n💰 **Spending Analysis**\n• \"How much did I spend this month?\"\n• \"Show my food expenses\"\n• \"What's my biggest expense?\"\n\n📈 **Income & Savings**\n• \"What's my income?\"\n• \"Am I saving enough?\"\n\n📊 **Insights & Trends**\n• \"Show my spending trends\"\n• \"Any unusual spending?\"\n• \"Give me financial advice\"\n\n🎯 **Goals & Budget**\n• \"How are my goals?\"\n• \"Budget status?\"\n\n💬 **Or just chat!**\n• I'm always here to listen and help!"
        ),
        "thanks" to listOf(
            "You're welcome! 😊 Let me know if you need anything else!",
            "Happy to help! Feel free to ask more questions anytime. 💪",
            "My pleasure! That's what I'm here for! 🌟",
            "Anytime! Your financial success is my priority! 💰"
        ),
        "farewell" to listOf(
            "Goodbye! Keep tracking those expenses! 💪 See you soon!",
            "See you later! Remember: Every penny tracked is a penny saved! 💰",
            "Take care! Your finances are looking good! 👋",
            "Bye for now! Come back anytime you need financial guidance! 🌟"
        ),
        "out_of_scope" to listOf(
            "I'm focused on helping you with your finances. I can answer questions about:\n\n• Spending & expenses\n• Income & savings\n• Budgets & goals\n• Financial trends\n\nIs there something financial I can help with?",
            "That's outside my expertise! I'm your financial advisor, so I can help with spending, income, budgets, and financial insights. What would you like to know about your finances?"
        ),
        "unknown" to listOf(
            "I'm not quite sure what you're asking. Could you rephrase that? Or try asking:\n• \"How much did I spend?\"\n• \"What are my trends?\"\n• \"Give me financial advice\""
        )
    )

    fun generate(plan: ResponsePlan, input: ProcessedInput): AIResponse {
        val templateList = templates[plan.templateKey] ?: templates["unknown"]!!
        val template = templateList.random()

        val text = fillTemplate(template, plan, input)

        return AIResponse(
            text = text,
            confidence = input.intentConfidence,
            intent = input.intent,
            suggestions = plan.followUpSuggestions,
            data = plan.variables.toMap()
        )
    }

    private fun fillTemplate(
        template: String,
        plan: ResponsePlan,
        input: ProcessedInput
    ): String {
        var result = template
        val vars = plan.variables

        // Format currency values
        vars["totalSpending"]?.let {
            result = result.replace("{totalSpending}", formatCurrency(it))
        }
        vars["totalIncome"]?.let {
            result = result.replace("{totalIncome}", formatCurrency(it))
        }
        vars["netSavings"]?.let {
            result = result.replace("{netSavings}", formatCurrency(it))
        }
        vars["categoryAmount"]?.let {
            result = result.replace("{categoryAmount}", formatCurrency(it))
        }

        // Format percentages
        vars["savingsRate"]?.let {
            val rate = (it as? Double) ?: 0.0
            result = result.replace("{savingsRate}", String.format("%.1f", rate))
        }
        vars["percentage"]?.let {
            result = result.replace("{percentage}", it.toString())
        }

        // Format counts
        vars["expenseCount"]?.let {
            result = result.replace("{expenseCount}", it.toString())
        }

        // Format category breakdown
        vars["categoryBreakdown"]?.let { breakdown ->
            @Suppress("UNCHECKED_CAST")
            val categories = breakdown as? Map<String, Double> ?: emptyMap()
            val formatted = categories.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapIndexed { idx, entry ->
                    val emoji = getCategoryEmoji(entry.key)
                    "$emoji ${entry.key}: ${formatCurrency(entry.value)}"
                }
                .joinToString("\n")
            result = result.replace("{categoryBreakdown}", formatted)
        }

        // Format top category
        vars["topCategory"]?.let {
            val cat = it as? Pair<*, *>
            if (cat != null) {
                result = result.replace("{topCategory}", "${cat.first} (${formatCurrency(cat.second)})")
            }
        }

        // Fill insights
        result = result.replace("{insight}", generateInsight(vars))
        result = result.replace("{observations}", generateObservations(vars))
        result = result.replace("{recommendations}", generateRecommendations(vars))
        result = result.replace("{advice}", generateAdvice(vars))
        result = result.replace("{motivation}", generateMotivation(vars))

        // Fill other dynamic content
        vars["periodName"]?.let {
            result = result.replace("{periodName}", it.toString())
        }
        vars["category"]?.let {
            result = result.replace("{category}", it.toString())
        }
        vars["recentList"]?.let {
            result = result.replace("{recentList}", it.toString())
        }
        vars["comparison"]?.let {
            result = result.replace("{comparison}", it.toString())
        }
        vars["trendAnalysis"]?.let {
            result = result.replace("{trendAnalysis}", it.toString())
        }
        vars["anomalyReport"]?.let {
            result = result.replace("{anomalyReport}", it.toString())
        }
        vars["predictions"]?.let {
            result = result.replace("{predictions}", it.toString())
        }
        vars["budgetSummary"]?.let {
            result = result.replace("{budgetSummary}", it.toString())
        }
        vars["goalsSummary"]?.let {
            result = result.replace("{goalsSummary}", it.toString())
        }
        vars["loansSummary"]?.let {
            result = result.replace("{loansSummary}", it.toString())
        }

        // Clean up any remaining placeholders
        result = result.replace(Regex("\\{[^}]+\\}"), "")

        return result.trim()
    }

    private fun formatCurrency(value: Any?): String {
        return when (value) {
            is Double -> currencyFormat.format(value)
            is Float -> currencyFormat.format(value.toDouble())
            is Int -> currencyFormat.format(value.toDouble())
            is Long -> currencyFormat.format(value.toDouble())
            else -> value?.toString() ?: "$0.00"
        }
    }

    private fun getCategoryEmoji(category: String): String {
        return when (category.uppercase()) {
            "FOOD" -> "🍔"
            "TRANSPORT" -> "🚗"
            "SHOPPING" -> "🛒"
            "ENTERTAINMENT" -> "🎬"
            "UTILITIES" -> "💡"
            "HEALTH" -> "🏥"
            "RENT" -> "🏠"
            "EDUCATION" -> "📚"
            "SAVINGS" -> "💰"
            "INVESTMENT" -> "📈"
            else -> "📦"
        }
    }

    private fun generateInsight(vars: Map<String, Any>): String {
        val savingsRate = (vars["savingsRate"] as? Double) ?: 0.0

        return when {
            savingsRate < 0 -> "⚠️ You're spending more than you earn. Consider reviewing your expenses."
            savingsRate < 10 -> "💡 Your savings rate is low. Aim for at least 20% for financial security."
            savingsRate < 20 -> "📊 Good progress! Try to boost your savings rate to 20% or more."
            savingsRate < 30 -> "✅ Great savings rate! You're on track for financial stability."
            else -> "🌟 Excellent! Your savings habits are exceptional."
        }
    }

    private fun generateObservations(vars: Map<String, Any>): String {
        val observations = mutableListOf<String>()

        val savingsRate = (vars["savingsRate"] as? Double) ?: 0.0
        if (savingsRate < 20) {
            observations.add("• Your savings rate is below the recommended 20%")
        }

        @Suppress("UNCHECKED_CAST")
        val categories = vars["categoryBreakdown"] as? Map<String, Double>
        if (categories != null) {
            val total = categories.values.sum()
            categories.forEach { (cat, amount) ->
                if (amount / total > 0.4) {
                    observations.add("• ${cat} spending is quite high (${String.format("%.0f", amount/total*100)}% of total)")
                }
            }
        }

        return if (observations.isEmpty()) "• Your spending looks balanced" else observations.joinToString("\n")
    }

    private fun generateRecommendations(vars: Map<String, Any>): String {
        val recommendations = mutableListOf<String>()

        val savingsRate = (vars["savingsRate"] as? Double) ?: 0.0

        if (savingsRate < 20) {
            recommendations.add("• Try the 50/30/20 rule: 50% needs, 30% wants, 20% savings")
        }

        @Suppress("UNCHECKED_CAST")
        val categories = vars["categoryBreakdown"] as? Map<String, Double>
        if (categories != null) {
            val total = categories.values.sum()
            val topCat = categories.maxByOrNull { it.value }
            if (topCat != null && topCat.value / total > 0.35) {
                recommendations.add("• Review your ${topCat.key} spending for potential savings")
            }
        }

        recommendations.add("• Set up automatic transfers to savings")

        return recommendations.take(3).joinToString("\n")
    }

    private fun generateAdvice(vars: Map<String, Any>): String {
        return "💡 Stay consistent with tracking your expenses for better insights!"
    }

    private fun generateMotivation(vars: Map<String, Any>): String {
        return "🎯 Every step towards your goal counts. Keep going!"
    }
}

// ============= Financial Data Provider Interface =============

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

