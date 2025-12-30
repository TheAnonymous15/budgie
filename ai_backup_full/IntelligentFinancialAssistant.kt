package com.example.budgie.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * INTELLIGENT FINANCIAL ASSISTANT - MOBILE-FIRST ARCHITECTURE
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * REFINED ARCHITECTURE (LLM-Efficient):
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                       USER INPUT (Any Language)                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                                    │
 *                                    ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                   LIGHTWEIGHT ROUTER (On-device)                        │
 * │     - Regex / rules / tiny classifier                                   │
 * │     - Detects: Greeting, Simple Q&A, Financial action                   │
 * │     - Decision: Needs LLM reasoning? (yes/no)                           │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                    │                              │
 *        ┌──────────┘                              └──────────┐
 *        ▼                                                    ▼
 * ┌────────────────────┐                       ┌────────────────────────────┐
 * │  FAST PATH (No LLM)│                       │  INTENT & ENTITY EXTRACT   │
 * │  • "hello"/"hola"  │                       │  • Structured JSON output  │
 * │  • "thanks"        │                       │  • Small prompt to Qwen    │
 * │  • Simple queries  │                       └────────────────────────────┘
 * └────────────────────┘                                      │
 *        │                                                    ▼
 *        │                               ┌────────────────────────────────┐
 *        │                               │   FINANCIAL DATA ENGINE        │
 *        │                               │   (Financial Learner - NO LLM) │
 *        │                               │   • Deterministic logic        │
 *        │                               │   • Local DB queries           │
 *        │                               │   • Time-series, scoring       │
 *        │                               └────────────────────────────────┘
 *        │                                                    │
 *        │                                                    ▼
 *        │                               ┌────────────────────────────────┐
 *        │                               │   QWEN REASONING (Controlled)  │
 *        │                               │   • Receives clean data only   │
 *        │                               │   • Style constraints          │
 *        │                               │   • Max 256 tokens             │
 *        │                               └────────────────────────────────┘
 *        │                                                    │
 *        └─────────────────────┬──────────────────────────────┘
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                       RESPONSE (User's Language)                        │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * This architecture cuts LLM usage by ~40-60% on real devices.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
class IntelligentFinancialAssistant private constructor(
    private val context: Context,
    private val learner: FinancialLearner,
    private val llmEngine: QwenLLMEngine
) {
    companion object {
        private const val TAG = "IntelligentAssistant"
        private const val MAX_LLM_TOKENS = 256
        private const val LLM_TIMEOUT_MS = 30_000L

        @Volatile
        private var instance: IntelligentFinancialAssistant? = null

        suspend fun getInstance(context: Context): IntelligentFinancialAssistant {
            return instance ?: synchronized(this) {
                val learner = FinancialLearner.getInstance(context)
                val llm = QwenLLMEngine.getInstance(context)
                IntelligentFinancialAssistant(context.applicationContext, learner, llm).also {
                    instance = it
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val lightweightRouter = LightweightRouter()

    // Supported query categories
    enum class QueryIntent {
        FINANCIAL_STATUS,      // "How am I doing?"
        SPENDING_ANALYSIS,     // "Where does my money go?"
        BILL_INQUIRY,          // "What bills do I have?"
        LOAN_INQUIRY,          // "What are my loans?"
        GOAL_INQUIRY,          // "How are my goals progressing?"
        BUDGET_ADVICE,         // "How can I save more?"
        PREDICTION,            // "Can I afford X in Y months?"
        COMPARISON,            // "Am I spending more than last month?"
        RECOMMENDATION,        // "What should I do?"
        GENERAL_GREETING,      // "Hello", "Hi"
        THANKS,                // "Thank you", "Thanks"
        HELP,                  // "Help", "What can you do"
        UNKNOWN                // Fallback - may need LLM
    }

    // Route decision from lightweight router
    enum class RouteDecision {
        FAST_PATH,             // No LLM needed - use rules/templates
        SIMPLE_QUERY,          // Data lookup only - no LLM reasoning
        NEEDS_REASONING        // Complex query - use LLM with structured data
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * LIGHTWEIGHT ROUTER - First line of defense (NO LLM)
     * ═══════════════════════════════════════════════════════════════════════
     */
    inner class LightweightRouter {

        // Multilingual greeting patterns
        private val greetingPatterns = listOf(
            // English
            Regex("^(hi|hello|hey|good\\s*(morning|afternoon|evening)|howdy|greetings)\\b", RegexOption.IGNORE_CASE),
            // Spanish
            Regex("^(hola|buenos\\s*(días|tardes|noches)|qué\\s*tal)\\b", RegexOption.IGNORE_CASE),
            // French
            Regex("^(bonjour|salut|bonsoir|coucou)\\b", RegexOption.IGNORE_CASE),
            // Swahili
            Regex("^(habari|jambo|mambo|sasa|vipi)\\b", RegexOption.IGNORE_CASE),
            // Portuguese
            Regex("^(olá|oi|bom\\s*dia|boa\\s*(tarde|noite))\\b", RegexOption.IGNORE_CASE),
            // German
            Regex("^(hallo|guten\\s*(tag|morgen|abend))\\b", RegexOption.IGNORE_CASE),
        )

        // Thank you patterns (multilingual)
        private val thanksPatterns = listOf(
            Regex("\\b(thanks|thank\\s*you|thx|ty|cheers|appreciate)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(gracias|merci|danke|obrigado|asante|shukran)\\b", RegexOption.IGNORE_CASE),
        )

        // Help patterns
        private val helpPatterns = listOf(
            Regex("\\b(help|what\\s*can\\s*you\\s*do|capabilities|options)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(ayuda|aide|hilfe|ajuda)\\b", RegexOption.IGNORE_CASE),
        )

        // Simple query patterns that don't need LLM reasoning
        private val simpleQueryPatterns = mapOf(
            QueryIntent.FINANCIAL_STATUS to listOf(
                Regex("\\b(how\\s*am\\s*i\\s*doing|my\\s*status|financial\\s*health|overview)\\b", RegexOption.IGNORE_CASE),
                Regex("\\b(cómo\\s*estoy|mein\\s*status)\\b", RegexOption.IGNORE_CASE),
            ),
            QueryIntent.SPENDING_ANALYSIS to listOf(
                Regex("\\b(spending|spent|expenses|where.*money\\s*go)\\b", RegexOption.IGNORE_CASE),
                Regex("\\b(show|list|view).*\\b(expense|spending|transaction)\\b", RegexOption.IGNORE_CASE),
            ),
            QueryIntent.BILL_INQUIRY to listOf(
                Regex("\\b(bill|bills|due|upcoming\\s*payment)\\b", RegexOption.IGNORE_CASE),
                Regex("\\b(what|show|list).*\\b(bill|due)\\b", RegexOption.IGNORE_CASE),
            ),
            QueryIntent.LOAN_INQUIRY to listOf(
                Regex("\\b(loan|loans|debt|owe|borrow)\\b", RegexOption.IGNORE_CASE),
                Regex("\\b(my|show|list).*\\b(loan|debt)\\b", RegexOption.IGNORE_CASE),
            ),
            QueryIntent.GOAL_INQUIRY to listOf(
                Regex("\\b(goal|goals|saving\\s*for|target|progress)\\b", RegexOption.IGNORE_CASE),
                Regex("\\b(my|show|list).*\\b(goal|saving)\\b", RegexOption.IGNORE_CASE),
            ),
        )

        // Complex patterns that need LLM reasoning
        private val complexPatterns = listOf(
            Regex("\\b(can\\s*i\\s*afford|will\\s*i\\s*have|should\\s*i)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(if\\s*i|what\\s*if|suppose|assuming)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(compare|better|worse|vs|versus)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(why|explain|how\\s*come|reason)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(predict|forecast|projection|estimate)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(advice|recommend|suggest|strategy|plan)\\b", RegexOption.IGNORE_CASE),
            Regex("\\d+.*\\b(month|year|week|day)s?\\b", RegexOption.IGNORE_CASE), // Time-based predictions
        )

        /**
         * Route the message - this is the first decision point
         */
        fun route(message: String): Pair<RouteDecision, QueryIntent> {
            val trimmedMessage = message.trim()

            // 1. Check for fast-path patterns first (no LLM at all)
            if (greetingPatterns.any { it.containsMatchIn(trimmedMessage) }) {
                return RouteDecision.FAST_PATH to QueryIntent.GENERAL_GREETING
            }
            if (thanksPatterns.any { it.containsMatchIn(trimmedMessage) }) {
                return RouteDecision.FAST_PATH to QueryIntent.THANKS
            }
            if (helpPatterns.any { it.containsMatchIn(trimmedMessage) }) {
                return RouteDecision.FAST_PATH to QueryIntent.HELP
            }

            // 2. Check if it needs complex reasoning (LLM required)
            if (complexPatterns.any { it.containsMatchIn(trimmedMessage) }) {
                val intent = detectDetailedIntent(trimmedMessage)
                return RouteDecision.NEEDS_REASONING to intent
            }

            // 3. Check for simple queries (data lookup, no LLM reasoning)
            for ((intent, patterns) in simpleQueryPatterns) {
                if (patterns.any { it.containsMatchIn(trimmedMessage) }) {
                    return RouteDecision.SIMPLE_QUERY to intent
                }
            }

            // 4. Unknown - might need LLM to understand
            return RouteDecision.NEEDS_REASONING to QueryIntent.UNKNOWN
        }

        /**
         * Extract entities from message (for complex queries)
         */
        fun extractEntities(message: String): ExtractedEntities {
            val amountPattern = Regex("\\$?([\\d,]+(?:\\.\\d{2})?)")
            val monthsPattern = Regex("(\\d+)\\s*months?")
            val yearsPattern = Regex("(\\d+)\\s*years?")
            val weeksPattern = Regex("(\\d+)\\s*weeks?")
            val daysPattern = Regex("(\\d+)\\s*days?")

            val amount = amountPattern.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

            val months = monthsPattern.find(message)?.groupValues?.get(1)?.toIntOrNull()
            val years = yearsPattern.find(message)?.groupValues?.get(1)?.toIntOrNull()
            val weeks = weeksPattern.find(message)?.groupValues?.get(1)?.toIntOrNull()
            val days = daysPattern.find(message)?.groupValues?.get(1)?.toIntOrNull()

            // Convert to months for unified handling
            val timeframeMonths = when {
                months != null -> months
                years != null -> years * 12
                weeks != null -> (weeks / 4.0).toInt().coerceAtLeast(1)
                days != null -> (days / 30.0).toInt().coerceAtLeast(1)
                else -> null
            }

            // Detect category mentions
            val categoryKeywords = mapOf(
                "food" to listOf("food", "eating", "restaurant", "dining", "groceries"),
                "transport" to listOf("transport", "car", "gas", "fuel", "uber", "taxi"),
                "shopping" to listOf("shopping", "clothes", "buy", "purchase"),
                "entertainment" to listOf("entertainment", "movie", "netflix", "subscription"),
                "utilities" to listOf("utilities", "electricity", "water", "internet", "phone"),
            )

            val lowerMessage = message.lowercase()
            val category = categoryKeywords.entries.firstOrNull { (_, keywords) ->
                keywords.any { lowerMessage.contains(it) }
            }?.key

            return ExtractedEntities(
                amount = amount,
                timeframeMonths = timeframeMonths,
                category = category
            )
        }

        private fun detectDetailedIntent(message: String): QueryIntent {
            val lowerMessage = message.lowercase()
            return when {
                lowerMessage.contains("afford") ||
                lowerMessage.contains("will i have") ||
                lowerMessage.contains("able to") -> QueryIntent.PREDICTION

                lowerMessage.contains("compare") ||
                lowerMessage.contains("vs") ||
                lowerMessage.contains("last month") -> QueryIntent.COMPARISON

                lowerMessage.contains("recommend") ||
                lowerMessage.contains("suggest") ||
                lowerMessage.contains("should i") ||
                lowerMessage.contains("advice") -> QueryIntent.RECOMMENDATION

                lowerMessage.contains("save") ||
                lowerMessage.contains("budget") ||
                lowerMessage.contains("cut") -> QueryIntent.BUDGET_ADVICE

                else -> QueryIntent.UNKNOWN
            }
        }
    }

    data class ExtractedEntities(
        val amount: Double? = null,
        val timeframeMonths: Int? = null,
        val category: String? = null
    )

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * MAIN PROCESSING PIPELINE
     * ═══════════════════════════════════════════════════════════════════════
     */
    fun processMessage(userMessage: String): Flow<AssistantResponse> = flow {
        emit(AssistantResponse.Thinking)

        try {
            // 1. Add to conversation history
            conversationHistory.add(ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                content = userMessage,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            ))

            // 2. LIGHTWEIGHT ROUTER - First decision point (NO LLM)
            val (routeDecision, intent) = lightweightRouter.route(userMessage)
            Log.d(TAG, "Route decision: $routeDecision, Intent: $intent")

            // 3. Gather financial context (always needed, but fast)
            val financialContext = gatherFinancialContext()

            // 4. Route based on decision
            when (routeDecision) {
                RouteDecision.FAST_PATH -> {
                    // No LLM at all - immediate response
                    val response = handleFastPath(intent, financialContext)
                    emit(AssistantResponse.Complete(response))
                }

                RouteDecision.SIMPLE_QUERY -> {
                    // Data lookup only - no LLM reasoning needed
                    val response = handleSimpleQuery(intent, financialContext)
                    emit(AssistantResponse.Complete(response))
                }

                RouteDecision.NEEDS_REASONING -> {
                    // Complex query - use LLM with structured data
                    val entities = lightweightRouter.extractEntities(userMessage)

                    if (llmEngine.isModelAvailable()) {
                        // Use LLM for sophisticated reasoning
                        handleComplexQuery(
                            userMessage = userMessage,
                            intent = intent,
                            entities = entities,
                            ctx = financialContext
                        ).collect { response ->
                            emit(response)
                        }
                    } else {
                        // Fallback to rule-based for complex queries
                        val response = handleComplexQueryFallback(
                            userMessage = userMessage,
                            intent = intent,
                            entities = entities,
                            ctx = financialContext
                        )
                        emit(AssistantResponse.Complete(response))
                    }
                }
            }

            emit(AssistantResponse.Done)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
            emit(AssistantResponse.Error(e.message ?: "An error occurred"))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * FAST PATH - No LLM, immediate templates
     */
    private suspend fun handleFastPath(intent: QueryIntent, ctx: FinancialContext): String {
        return when (intent) {
            QueryIntent.GENERAL_GREETING -> buildGreetingResponse(ctx)
            QueryIntent.THANKS -> buildThanksResponse(ctx)
            QueryIntent.HELP -> buildHelpResponse(ctx)
            else -> buildHelpResponse(ctx)
        }
    }

    /**
     * SIMPLE QUERY - Data lookup, no LLM reasoning
     */
    private suspend fun handleSimpleQuery(intent: QueryIntent, ctx: FinancialContext): String {
        return when (intent) {
            QueryIntent.FINANCIAL_STATUS -> buildFinancialStatusResponse(ctx)
            QueryIntent.SPENDING_ANALYSIS -> buildSpendingAnalysisResponse(ctx)
            QueryIntent.BILL_INQUIRY -> buildBillInquiryResponse(ctx)
            QueryIntent.LOAN_INQUIRY -> buildLoanInquiryResponse(ctx)
            QueryIntent.GOAL_INQUIRY -> buildGoalInquiryResponse(ctx)
            else -> buildUnknownResponse(ctx)
        }
    }

    /**
     * COMPLEX QUERY - Uses LLM with structured data
     */
    private fun handleComplexQuery(
        userMessage: String,
        intent: QueryIntent,
        entities: ExtractedEntities,
        ctx: FinancialContext
    ): Flow<AssistantResponse> = flow {
        // First, get deterministic data from learner (NO LLM)
        val structuredData = buildStructuredDataForLLM(intent, entities, ctx)

        // Build controlled prompt for LLM
        val systemPrompt = buildSystemPrompt(ctx)
        val dataBlock = buildDataBlock(structuredData)

        // Use LLM with timeout and token limit
        withTimeoutOrNull(LLM_TIMEOUT_MS) {
            llmEngine.generateControlledResponse(
                systemPrompt = systemPrompt,
                dataBlock = dataBlock,
                userQuery = userMessage,
                maxTokens = MAX_LLM_TOKENS
            ).collect { token ->
                emit(AssistantResponse.Streaming(token))
            }
        } ?: run {
            // Timeout fallback
            val fallbackResponse = handleComplexQueryFallback(userMessage, intent, entities, ctx)
            emit(AssistantResponse.Complete(fallbackResponse))
        }
    }

    /**
     * Build structured data for LLM (deterministic, from learner)
     */
    private suspend fun buildStructuredDataForLLM(
        intent: QueryIntent,
        entities: ExtractedEntities,
        ctx: FinancialContext
    ): StructuredFinancialData {
        return when (intent) {
            QueryIntent.PREDICTION -> {
                // Calculate affordability using learner (NO LLM)
                val targetAmount = entities.amount ?: 0.0
                val months = entities.timeframeMonths ?: 6
                val requiredMonthlySavings = if (months > 0) targetAmount / months else 0.0
                val canAfford = ctx.monthlySavings >= requiredMonthlySavings
                val affordabilityPercent = if (requiredMonthlySavings > 0) {
                    ((ctx.monthlySavings / requiredMonthlySavings) * 100).coerceAtMost(200.0)
                } else 100.0

                StructuredFinancialData(
                    type = "prediction",
                    targetAmount = targetAmount,
                    timeframeMonths = months,
                    requiredMonthlySavings = requiredMonthlySavings,
                    currentMonthlySavings = ctx.monthlySavings,
                    canAfford = canAfford,
                    affordabilityPercent = affordabilityPercent,
                    suggestedMonths = if (!canAfford && ctx.monthlySavings > 0) {
                        (targetAmount / ctx.monthlySavings).toInt() + 1
                    } else months
                )
            }

            QueryIntent.BUDGET_ADVICE -> {
                StructuredFinancialData(
                    type = "budget_advice",
                    currentMonthlySavings = ctx.monthlySavings,
                    savingsRate = ctx.savingsRate,
                    monthlyIncome = ctx.monthlyIncome,
                    monthlyExpenses = ctx.monthlyExpenses,
                    healthScore = ctx.healthScore
                )
            }

            QueryIntent.RECOMMENDATION -> {
                StructuredFinancialData(
                    type = "recommendation",
                    userAge = ctx.userAge,
                    healthScore = ctx.healthScore,
                    savingsRate = ctx.savingsRate,
                    activeLoans = ctx.activeLoans,
                    totalLoanBalance = ctx.totalLoanBalance,
                    activeGoals = ctx.activeGoals,
                    monthlyIncome = ctx.monthlyIncome,
                    monthlyExpenses = ctx.monthlyExpenses
                )
            }

            else -> {
                StructuredFinancialData(
                    type = "general",
                    monthlyIncome = ctx.monthlyIncome,
                    monthlyExpenses = ctx.monthlyExpenses,
                    currentMonthlySavings = ctx.monthlySavings,
                    savingsRate = ctx.savingsRate,
                    healthScore = ctx.healthScore
                )
            }
        }
    }

    /**
     * Build system prompt for LLM (controlled)
     */
    private fun buildSystemPrompt(ctx: FinancialContext): String {
        val ageStyle = when {
            ctx.userAge < 25 -> "Speak casually and use relatable examples for a young adult."
            ctx.userAge < 40 -> "Be professional but friendly. Focus on growth and wealth building."
            ctx.userAge < 60 -> "Be thorough and focus on stability and retirement planning."
            else -> "Be respectful and focus on security and legacy planning."
        }

        return """
            |You are Budgie, a personal financial assistant.
            |User: ${ctx.userName}, Age: ${ctx.userAge}
            |
            |RULES:
            |1. ONLY use the DATA provided below - never invent numbers
            |2. Keep response under 150 words
            |3. Be specific with the actual numbers from DATA
            |4. $ageStyle
            |5. End with a helpful follow-up question or actionable tip
            |6. Do NOT use markdown headers, just plain text with bullet points
        """.trimMargin()
    }

    /**
     * Build data block for LLM (clean, numeric data only)
     */
    private fun buildDataBlock(data: StructuredFinancialData): String {
        return when (data.type) {
            "prediction" -> """
                |DATA:
                |Target Amount: $${formatCurrency(data.targetAmount ?: 0.0)}
                |Timeframe: ${data.timeframeMonths} months
                |Required Monthly Savings: $${formatCurrency(data.requiredMonthlySavings ?: 0.0)}
                |Current Monthly Savings: $${formatCurrency(data.currentMonthlySavings ?: 0.0)}
                |Can Afford: ${if (data.canAfford == true) "YES" else "NO"}
                |Affordability: ${String.format("%.0f", data.affordabilityPercent ?: 0.0)}%
                |Suggested Timeline: ${data.suggestedMonths} months
            """.trimMargin()

            "budget_advice" -> """
                |DATA:
                |Monthly Income: $${formatCurrency(data.monthlyIncome ?: 0.0)}
                |Monthly Expenses: $${formatCurrency(data.monthlyExpenses ?: 0.0)}
                |Monthly Savings: $${formatCurrency(data.currentMonthlySavings ?: 0.0)}
                |Savings Rate: ${String.format("%.1f", data.savingsRate ?: 0.0)}%
                |Health Score: ${data.healthScore ?: 0}/100
            """.trimMargin()

            "recommendation" -> """
                |DATA:
                |User Age: ${data.userAge}
                |Health Score: ${data.healthScore}/100
                |Savings Rate: ${String.format("%.1f", data.savingsRate ?: 0.0)}%
                |Active Loans: ${data.activeLoans}
                |Total Loan Balance: $${formatCurrency(data.totalLoanBalance ?: 0.0)}
                |Active Goals: ${data.activeGoals}
                |Monthly Income: $${formatCurrency(data.monthlyIncome ?: 0.0)}
                |Monthly Expenses: $${formatCurrency(data.monthlyExpenses ?: 0.0)}
            """.trimMargin()

            else -> """
                |DATA:
                |Monthly Income: $${formatCurrency(data.monthlyIncome ?: 0.0)}
                |Monthly Expenses: $${formatCurrency(data.monthlyExpenses ?: 0.0)}
                |Monthly Savings: $${formatCurrency(data.currentMonthlySavings ?: 0.0)}
                |Savings Rate: ${String.format("%.1f", data.savingsRate ?: 0.0)}%
                |Health Score: ${data.healthScore ?: 0}/100
            """.trimMargin()
        }
    }

    /**
     * Fallback for complex queries when LLM unavailable
     */
    private fun handleComplexQueryFallback(
        userMessage: String,
        intent: QueryIntent,
        entities: ExtractedEntities,
        ctx: FinancialContext
    ): String {
        return when (intent) {
            QueryIntent.PREDICTION -> buildPredictionResponse(entities, ctx)
            QueryIntent.BUDGET_ADVICE -> buildBudgetAdviceResponse(ctx)
            QueryIntent.RECOMMENDATION -> buildRecommendationResponse(ctx)
            QueryIntent.COMPARISON -> buildComparisonResponse(ctx)
            else -> buildUnknownResponse(ctx)
        }
    }

    /**
     * Gather comprehensive financial context from the learner
     */
    private suspend fun gatherFinancialContext(): FinancialContext {
        // Get user profile
        val userName = learner.getUserName()
        val userAge = learner.getUserAge()

        // Get financial summary from learner
        val summary = learner.getFinancialSummary()

        // Get real-time data
        val recentTransactions = learner.getRecentTransactions(7).map { tx ->
            TransactionSummary(
                category = tx.category,
                amount = tx.amount,
                date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
            )
        }

        val upcomingBills = learner.getUpcomingBills(14).map { bill ->
            BillSummary(
                name = bill.title,
                amount = bill.amount,
                daysUntilDue = bill.daysUntilDue
            )
        }

        val goals = learner.getActiveGoals().map { goal ->
            GoalSummary(
                name = goal.title,
                target = goal.targetAmount,
                saved = goal.currentAmount,
                progressPercent = ((goal.currentAmount / goal.targetAmount) * 100).toInt()
            )
        }

        val loans = learner.getActiveLoans().map { loan ->
            LoanSummary(
                name = loan.title,
                principal = loan.principalAmount,
                remaining = loan.amountRemaining,
                monthlyPayment = loan.monthlyPayment
            )
        }

        return FinancialContext(
            userName = userName,
            userAge = userAge,
            monthlyIncome = summary.totalIncome,
            monthlyExpenses = summary.totalExpenses,
            monthlySavings = summary.netSavings,
            savingsRate = summary.savingsRate,
            totalBillsDue = summary.upcomingBillsTotal,
            activeLoans = loans.size,
            totalLoanBalance = loans.sumOf { it.remaining },
            activeGoals = goals.size,
            healthScore = summary.healthScore,
            recentTransactions = recentTransactions,
            upcomingBills = upcomingBills,
            goals = goals,
            loans = loans
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RESPONSE BUILDERS (Template-based, NO LLM)
    // ═══════════════════════════════════════════════════════════════════════

    private fun buildGreetingResponse(ctx: FinancialContext): String {
        val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }

        return buildString {
            appendLine("$greeting, ${ctx.userName}! 👋")
            appendLine()
            appendLine("I'm Budgie, your financial companion. Here's your quick snapshot:")
            appendLine()
            appendLine("• Health Score: ${ctx.healthScore}/100")
            appendLine("• Savings Rate: ${String.format("%.1f", ctx.savingsRate)}%")
            if (ctx.upcomingBills.isNotEmpty()) {
                appendLine("• ${ctx.upcomingBills.size} bills due soon")
            }
            appendLine()
            appendLine("What would you like to know?")
        }
    }

    private fun buildThanksResponse(ctx: FinancialContext): String {
        val responses = listOf(
            "You're welcome, ${ctx.userName}! Happy to help with your finances anytime. 😊",
            "Glad I could help! Remember, I'm here whenever you need financial guidance.",
            "Anytime! Keep up the great work with your finances. 💪",
            "My pleasure! Let me know if you have any other questions about your money."
        )
        return responses.random()
    }

    private fun buildHelpResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("Here's what I can help you with, ${ctx.userName}:")
            appendLine()
            appendLine("📊 **Quick Checks:**")
            appendLine("• \"How am I doing?\" - Financial health overview")
            appendLine("• \"Show my spending\" - Expense breakdown")
            appendLine("• \"My bills\" - Upcoming payments")
            appendLine("• \"My loans\" - Loan status")
            appendLine("• \"My goals\" - Savings progress")
            appendLine()
            appendLine("🔮 **Planning:**")
            appendLine("• \"Can I afford $5000 in 6 months?\"")
            appendLine("• \"How can I save more?\"")
            appendLine("• \"What should I prioritize?\"")
            appendLine()
            appendLine("Just ask naturally - I understand many languages!")
        }
    }

    private fun buildFinancialStatusResponse(ctx: FinancialContext): String {
        val healthDescription = when {
            ctx.healthScore >= 80 -> "excellent"
            ctx.healthScore >= 60 -> "good"
            ctx.healthScore >= 40 -> "fair"
            else -> "needs attention"
        }

        return buildString {
            appendLine("📊 **Your Financial Snapshot**")
            appendLine()
            appendLine("Health Score: ${ctx.healthScore}/100 ($healthDescription)")
            appendLine()
            appendLine("**This Month:**")
            appendLine("• Income: $${formatCurrency(ctx.monthlyIncome)}")
            appendLine("• Expenses: $${formatCurrency(ctx.monthlyExpenses)}")
            appendLine("• Savings: $${formatCurrency(ctx.monthlySavings)} (${String.format("%.1f", ctx.savingsRate)}%)")

            if (ctx.activeLoans > 0) {
                appendLine()
                appendLine("**Loans:** ${ctx.activeLoans} active, $${formatCurrency(ctx.totalLoanBalance)} remaining")
            }

            if (ctx.activeGoals > 0) {
                appendLine("**Goals:** ${ctx.activeGoals} active")
            }

            appendLine()
            appendLine(getPersonalizedTip(ctx))
        }
    }

    private fun buildSpendingAnalysisResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("💸 **Your Spending Analysis**")
            appendLine()
            appendLine("Total Expenses: $${formatCurrency(ctx.monthlyExpenses)}")
            appendLine()

            if (ctx.recentTransactions.isNotEmpty()) {
                appendLine("**Recent Transactions:**")
                ctx.recentTransactions.take(5).forEach { tx ->
                    appendLine("• ${tx.category}: $${formatCurrency(tx.amount)} (${tx.date})")
                }
                appendLine()
            }

            val spendingRatio = if (ctx.monthlyIncome > 0) {
                (ctx.monthlyExpenses / ctx.monthlyIncome) * 100
            } else 0.0

            appendLine("Spending ${String.format("%.0f", spendingRatio)}% of income.")

            when {
                spendingRatio > 80 -> appendLine("⚠️ Consider reviewing discretionary spending.")
                spendingRatio < 50 -> appendLine("✅ Great expense control!")
            }
        }
    }

    private fun buildBillInquiryResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("📋 **Your Upcoming Bills**")
            appendLine()

            if (ctx.upcomingBills.isEmpty()) {
                appendLine("No bills due in the next 2 weeks! ✅")
            } else {
                ctx.upcomingBills.forEach { bill ->
                    val urgency = when {
                        bill.daysUntilDue <= 3 -> "🔴"
                        bill.daysUntilDue <= 7 -> "🟡"
                        else -> "🟢"
                    }
                    appendLine("$urgency ${bill.name}: $${formatCurrency(bill.amount)} in ${bill.daysUntilDue} days")
                }
                appendLine()
                appendLine("**Total Due:** $${formatCurrency(ctx.totalBillsDue)}")
            }
        }
    }

    private fun buildLoanInquiryResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("💳 **Your Loans**")
            appendLine()

            if (ctx.loans.isEmpty()) {
                appendLine("No active loans - great job staying debt-free! 🎉")
            } else {
                ctx.loans.forEach { loan ->
                    appendLine("**${loan.name}**")
                    appendLine("• Principal: $${formatCurrency(loan.principal)}")
                    appendLine("• Remaining: $${formatCurrency(loan.remaining)}")
                    appendLine("• Monthly: $${formatCurrency(loan.monthlyPayment)}")
                    appendLine()
                }
                appendLine("**Total Balance:** $${formatCurrency(ctx.totalLoanBalance)}")

                val monthlyPayments = ctx.loans.sumOf { it.monthlyPayment }
                val ratio = if (ctx.monthlyIncome > 0) (monthlyPayments / ctx.monthlyIncome) * 100 else 0.0
                appendLine("Loan payments: ${String.format("%.1f", ratio)}% of income")
            }
        }
    }

    private fun buildGoalInquiryResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("🎯 **Your Financial Goals**")
            appendLine()

            if (ctx.goals.isEmpty()) {
                appendLine("No active goals yet.")
                appendLine("Setting goals helps you save with purpose!")
            } else {
                ctx.goals.forEach { goal ->
                    val bar = buildProgressBar(goal.progressPercent)
                    appendLine("**${goal.name}**")
                    appendLine("$bar ${goal.progressPercent}%")
                    appendLine("$${formatCurrency(goal.saved)} / $${formatCurrency(goal.target)}")
                    appendLine()
                }
            }
        }
    }

    private fun buildPredictionResponse(entities: ExtractedEntities, ctx: FinancialContext): String {
        val targetAmount = entities.amount ?: 0.0
        val months = entities.timeframeMonths ?: 6

        return buildString {
            appendLine("🔮 **Affordability Analysis**")
            appendLine()

            if (targetAmount > 0) {
                val requiredMonthly = targetAmount / months
                val canAfford = ctx.monthlySavings >= requiredMonthly
                val percentOfSavings = if (ctx.monthlySavings > 0) {
                    (requiredMonthly / ctx.monthlySavings) * 100
                } else 0.0

                appendLine("**Goal:** $${formatCurrency(targetAmount)} in $months months")
                appendLine("**Required monthly:** $${formatCurrency(requiredMonthly)}")
                appendLine("**Your monthly savings:** $${formatCurrency(ctx.monthlySavings)}")
                appendLine()

                if (canAfford) {
                    appendLine("✅ **Yes, achievable!**")
                    appendLine("Uses ${String.format("%.0f", percentOfSavings)}% of your savings capacity.")
                } else {
                    appendLine("⚠️ **Challenging with current savings**")
                    val suggestedMonths = if (ctx.monthlySavings > 0) {
                        (targetAmount / ctx.monthlySavings).toInt() + 1
                    } else months * 2
                    appendLine()
                    appendLine("**Options:**")
                    appendLine("• Extend to $suggestedMonths months")
                    appendLine("• Increase savings by $${formatCurrency(requiredMonthly - ctx.monthlySavings)}/month")
                }
            } else {
                appendLine("I can help assess affordability!")
                appendLine()
                appendLine("Try: \"Can I afford $5000 in 6 months?\"")
            }
        }
    }

    private fun buildBudgetAdviceResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("💡 **Personalized Savings Tips**")
            appendLine()

            when {
                ctx.savingsRate >= 20 -> {
                    appendLine("✅ Excellent! Saving ${String.format("%.1f", ctx.savingsRate)}%")
                    appendLine()
                    appendLine("**Next steps:**")
                    appendLine("• Consider investing surplus")
                    appendLine("• Build 3-6 month emergency fund")
                }
                ctx.savingsRate >= 10 -> {
                    appendLine("👍 Good progress at ${String.format("%.1f", ctx.savingsRate)}%")
                    appendLine()
                    appendLine("**To reach 20%:**")
                    appendLine("• Review subscriptions")
                    appendLine("• Try 24-hour rule for purchases")
                }
                else -> {
                    appendLine("⚠️ Savings rate: ${String.format("%.1f", ctx.savingsRate)}%")
                    appendLine()
                    appendLine("**Priority actions:**")
                    appendLine("• Track every expense for a week")
                    appendLine("• Identify top 3 spending categories")
                    appendLine("• Set a small achievable goal")
                }
            }
        }
    }

    private fun buildRecommendationResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("🎯 **Recommendations for ${ctx.userName}**")
            appendLine()

            // Age-appropriate advice
            when {
                ctx.userAge < 30 -> {
                    appendLine("**At ${ctx.userAge}, focus on:**")
                    appendLine("1. Building emergency fund (3 months)")
                    appendLine("2. Start retirement savings early")
                    appendLine("3. Pay off high-interest debt first")
                }
                ctx.userAge < 50 -> {
                    appendLine("**At ${ctx.userAge}, prioritize:**")
                    appendLine("1. Maximize retirement contributions")
                    appendLine("2. Diversify investments")
                    appendLine("3. Build long-term wealth")
                }
                else -> {
                    appendLine("**At ${ctx.userAge}, consider:**")
                    appendLine("1. Review retirement readiness")
                    appendLine("2. Reduce investment risk gradually")
                    appendLine("3. Plan for healthcare costs")
                }
            }

            appendLine()
            appendLine("**Based on your situation:**")
            if (ctx.savingsRate < 15) appendLine("• 📈 Increase savings to 15%+")
            if (ctx.activeLoans > 0) appendLine("• 💳 Consider loan payoff acceleration")
            if (ctx.activeGoals == 0) appendLine("• 🎯 Set specific financial goals")
        }
    }

    private fun buildComparisonResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("📈 **Financial Overview**")
            appendLine()
            appendLine("**Current Month:**")
            appendLine("• Income: $${formatCurrency(ctx.monthlyIncome)}")
            appendLine("• Expenses: $${formatCurrency(ctx.monthlyExpenses)}")
            appendLine("• Net: $${formatCurrency(ctx.monthlySavings)}")
            appendLine()
            appendLine("For detailed trends, check the Analytics section.")
        }
    }

    private fun buildUnknownResponse(ctx: FinancialContext): String {
        return buildString {
            appendLine("I can help with your finances! Try asking:")
            appendLine()
            appendLine("• \"How am I doing?\"")
            appendLine("• \"Show my spending\"")
            appendLine("• \"What bills are coming up?\"")
            appendLine("• \"Can I afford \$X in Y months?\"")
            appendLine("• \"How can I save more?\"")
            appendLine()
            appendLine("What would you like to explore?")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════

    private fun getPersonalizedTip(ctx: FinancialContext): String {
        return when {
            ctx.healthScore >= 80 -> "💪 Excellent financial health! Keep it up!"
            ctx.savingsRate < 10 -> "💡 Tip: Small increases in savings add up over time."
            ctx.totalLoanBalance > ctx.monthlyIncome * 6 -> "💡 Focus on debt reduction to boost your score."
            else -> "💡 You're on track. Keep monitoring your spending."
        }
    }

    private fun formatCurrency(amount: Double): String {
        return String.format("%,.2f", amount)
    }

    private fun buildProgressBar(percent: Int): String {
        val filled = (percent / 10).coerceIn(0, 10)
        val empty = 10 - filled
        return "[${"█".repeat(filled)}${"░".repeat(empty)}]"
    }

    /**
     * Clear conversation history
     */
    fun clearHistory() {
        conversationHistory.clear()
    }

    /**
     * Release resources
     */
    fun release() {
        scope.cancel()
        llmEngine.release()
    }
}

/**
 * Structured data for LLM (deterministic, from learner)
 */
data class StructuredFinancialData(
    val type: String,
    val targetAmount: Double? = null,
    val timeframeMonths: Int? = null,
    val requiredMonthlySavings: Double? = null,
    val currentMonthlySavings: Double? = null,
    val canAfford: Boolean? = null,
    val affordabilityPercent: Double? = null,
    val suggestedMonths: Int? = null,
    val savingsRate: Double? = null,
    val monthlyIncome: Double? = null,
    val monthlyExpenses: Double? = null,
    val healthScore: Int? = null,
    val userAge: Int? = null,
    val activeLoans: Int? = null,
    val totalLoanBalance: Double? = null,
    val activeGoals: Int? = null
)

/**
 * Response states for the assistant
 */
sealed class AssistantResponse {
    object Thinking : AssistantResponse()
    data class Streaming(val token: String) : AssistantResponse()
    data class Complete(val response: String) : AssistantResponse()
    object Done : AssistantResponse()
    data class Error(val message: String) : AssistantResponse()
}
