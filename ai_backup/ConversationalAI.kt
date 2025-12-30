package com.example.budgie.ai

import android.content.Context
import android.util.Log
import com.example.budgie.data.preferences.UserPreferencesManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "ConversationalAI"

/**
 * =====================================================
 * CONVERSATIONAL AI - THE CHATBOT INTERFACE
 * =====================================================
 *
 * This is a THIN INTERFACE layer that:
 * 1. Detects the user's language
 * 2. Translates non-English to English
 * 3. Understands intent and context
 * 4. Passes the query to FinancialLearner (the brain)
 * 5. Receives structured response from Learner
 * 6. Formats the response professionally & age-appropriately
 * 7. Translates response back to user's language
 *
 * THE CHATBOT DOES NOT DO ANY FINANCIAL ANALYSIS!
 * All intelligence comes from the FinancialLearner.
 */
class ConversationalAI(context: Context) {

    private val preferencesManager = UserPreferencesManager.getInstance(context)
    private val learner = FinancialLearner.getInstance(context)
    private val languageIdentifier = LanguageIdentification.getClient()

    // Conversation memory (short-term)
    private var conversationHistory = mutableListOf<ConversationTurn>()
    private var userLanguage = "en"
    private var userAge = 30 // Default
    private var userName = "there"

    companion object {
        @Volatile
        private var INSTANCE: ConversationalAI? = null

        fun getInstance(context: Context): ConversationalAI {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConversationalAI(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Initialize the chatbot
     */
    suspend fun initialize() {
        learner.initialize()
        loadUserProfile()
    }

    private suspend fun loadUserProfile() {
        try {
            val profile = preferencesManager.userProfile.first()
            userName = profile?.name ?: "there"
            userAge = calculateAge(profile?.birthday) ?: 30
            Log.d(TAG, "Loaded user profile: $userName, age $userAge")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile: ${e.message}")
        }
    }

    private fun calculateAge(birthday: String?): Int? {
        if (birthday.isNullOrEmpty()) return null
        return try {
            val parts = birthday.split("/", "-")
            if (parts.size >= 3) {
                val birthYear = parts[0].toIntOrNull() ?: return null
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                currentYear - birthYear
            } else null
        } catch (e: Exception) { null }
    }

    /**
     * =====================================================
     * MAIN ENTRY POINT - Process user message
     * =====================================================
     */
    suspend fun processMessage(userMessage: String): ChatMessage = withContext(Dispatchers.Default) {
        try {
            // Step 1: Detect language
            val detectedLanguage = detectLanguage(userMessage)
            userLanguage = detectedLanguage

            // Step 2: Translate to English if needed
            val englishMessage = if (detectedLanguage != "en") {
                translateToEnglish(userMessage, detectedLanguage)
            } else {
                userMessage
            }

            Log.d(TAG, "Processing: '$userMessage' -> '$englishMessage' (lang: $detectedLanguage)")

            // Step 3: Understand intent and extract entities
            val understanding = understandMessage(englishMessage)

            // Step 4: Build query for the Learner
            val query = buildLearnerQuery(understanding, englishMessage)

            // Step 5: Get response from the Learner (THE BRAIN!)
            val learnerResponse = learner.answerQuestion(query)

            // Step 6: Format response professionally
            val formattedResponse = formatResponse(learnerResponse, understanding, userAge)

            // Step 7: Translate back to user's language if needed
            val finalResponse = if (detectedLanguage != "en") {
                translateFromEnglish(formattedResponse, detectedLanguage)
            } else {
                formattedResponse
            }

            // Step 8: Generate suggestions
            val suggestions = generateSuggestions(learnerResponse.suggestions, detectedLanguage)

            // Update conversation history
            conversationHistory.add(ConversationTurn(
                userMessage = userMessage,
                intent = understanding.intent,
                response = finalResponse
            ))

            // Keep only last 5 turns
            if (conversationHistory.size > 5) {
                conversationHistory = conversationHistory.takeLast(5).toMutableList()
            }

            ChatMessage(
                id = UUID.randomUUID().toString(),
                content = finalResponse,
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                suggestions = suggestions
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing message: ${e.message}", e)
            ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "I'm having trouble understanding that. Could you rephrase your question?",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                suggestions = listOf("How am I doing?", "Show my spending", "Check my loans")
            )
        }
    }

    // =====================================================
    // LANGUAGE DETECTION & TRANSLATION
    // =====================================================

    private suspend fun detectLanguage(text: String): String = suspendCancellableCoroutine { cont ->
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                val result = if (languageCode == "und") "en" else languageCode
                cont.resume(result)
            }
            .addOnFailureListener {
                cont.resume("en")
            }
    }

    private suspend fun translateToEnglish(text: String, sourceLanguage: String): String {
        return try {
            val sourceLang = mapLanguageCode(sourceLanguage)
            if (sourceLang == null) return text

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()

            val translator = Translation.getClient(options)

            suspendCancellableCoroutine { cont ->
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        translator.translate(text)
                            .addOnSuccessListener { translated ->
                                translator.close()
                                cont.resume(translated)
                            }
                            .addOnFailureListener {
                                translator.close()
                                cont.resume(text)
                            }
                    }
                    .addOnFailureListener {
                        translator.close()
                        cont.resume(text)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Translation error: ${e.message}")
            text
        }
    }

    private suspend fun translateFromEnglish(text: String, targetLanguage: String): String {
        return try {
            val targetLang = mapLanguageCode(targetLanguage)
            if (targetLang == null) return text

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLang)
                .build()

            val translator = Translation.getClient(options)

            suspendCancellableCoroutine { cont ->
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        translator.translate(text)
                            .addOnSuccessListener { translated ->
                                translator.close()
                                cont.resume(translated)
                            }
                            .addOnFailureListener {
                                translator.close()
                                cont.resume(text)
                            }
                    }
                    .addOnFailureListener {
                        translator.close()
                        cont.resume(text)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Translation error: ${e.message}")
            text
        }
    }

    private fun mapLanguageCode(code: String): String? {
        return when (code) {
            "en" -> TranslateLanguage.ENGLISH
            "sw" -> TranslateLanguage.SWAHILI
            "es" -> TranslateLanguage.SPANISH
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "pt" -> TranslateLanguage.PORTUGUESE
            "zh" -> TranslateLanguage.CHINESE
            "ar" -> TranslateLanguage.ARABIC
            "hi" -> TranslateLanguage.HINDI
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "it" -> TranslateLanguage.ITALIAN
            "ru" -> TranslateLanguage.RUSSIAN
            else -> null
        }
    }

    // =====================================================
    // INTENT UNDERSTANDING (Context + Intent + Entities)
    // =====================================================

    private fun understandMessage(message: String): MessageUnderstanding {
        val msg = message.lowercase().trim()

        // Social intents (handle first - don't need Learner)
        if (isSocialIntent(msg)) {
            return MessageUnderstanding(
                intent = Intent.SOCIAL,
                socialType = detectSocialType(msg),
                rawMessage = message
            )
        }

        // Extract numerical amounts
        val amounts = extractAmounts(msg)

        // Extract timeframes
        val timeframe = extractTimeframe(msg)

        // Extract item/category names
        val itemName = extractItemName(msg)

        // Determine main intent
        val intent = detectIntent(msg)

        // Determine query category for Learner
        val queryCategory = mapIntentToQueryCategory(intent, msg)

        return MessageUnderstanding(
            intent = intent,
            queryCategory = queryCategory,
            amounts = amounts,
            timeframeMonths = timeframe,
            itemName = itemName,
            rawMessage = message,
            previousContext = conversationHistory.lastOrNull()?.intent
        )
    }

    private fun isSocialIntent(msg: String): Boolean {
        return msg.matches(Regex("^(hi|hello|hey|good morning|good afternoon|good evening|what's up|whats up|howdy).*")) ||
               msg.contains(Regex("^(thank|thanks|appreciate)")) ||
               msg.contains(Regex("^(bye|goodbye|see you|later|goodnight)")) ||
               msg in listOf("yes", "yeah", "yep", "sure", "ok", "okay", "no", "nope", "not now", "cancel")
    }

    private fun detectSocialType(msg: String): SocialType {
        return when {
            msg.matches(Regex("^(hi|hello|hey|good morning|good afternoon|good evening|what's up|whats up|howdy).*")) -> SocialType.GREETING
            msg.contains(Regex("thank|thanks|appreciate")) -> SocialType.THANKS
            msg.contains(Regex("bye|goodbye|see you|later|goodnight")) -> SocialType.FAREWELL
            msg in listOf("yes", "yeah", "yep", "sure", "ok", "okay", "please", "go ahead") -> SocialType.AFFIRM
            msg in listOf("no", "nope", "not now", "cancel", "never mind") -> SocialType.DENY
            else -> SocialType.GREETING
        }
    }

    private fun extractAmounts(msg: String): List<Double> {
        val amounts = mutableListOf<Double>()

        // Handle "X million" pattern
        Regex("(\\d+(?:\\.\\d+)?)\\s*millions?", RegexOption.IGNORE_CASE).findAll(msg).forEach { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null) amounts.add(num * 1_000_000)
        }

        // Handle "X k" or "Xk" pattern
        Regex("(\\d+(?:\\.\\d+)?)\\s*k\\b", RegexOption.IGNORE_CASE).findAll(msg).forEach { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null) amounts.add(num * 1000)
        }

        val patterns = listOf(
            Regex("\\$\\s*([0-9,]+\\.?[0-9]*)"),           // $30000
            Regex("([0-9,]+\\.?[0-9]*)\\s*(?:dollars?|usd|\\$)"), // 30000 dollars
            Regex("(?:worth|cost|costs|price|afford|need|save|spend|spending)\\s+(?:about\\s+)?([0-9,]+\\.?[0-9]*)"), // afford 30000
            Regex("\\b([0-9]{4,})\\b") // Any 4+ digit number (but not if already captured)
        )

        for (pattern in patterns) {
            pattern.findAll(msg).forEach { match ->
                val numStr = match.groupValues[1].replace(",", "")
                numStr.toDoubleOrNull()?.let { num ->
                    if (num > 0 && num !in amounts) {
                        amounts.add(num)
                    }
                }
            }
        }

        return amounts.distinct()
    }

    private fun extractTimeframe(msg: String): Int? {
        // "in X years" pattern - convert to months
        Regex("in\\s+(\\d+)\\s*years?").find(msg)?.let {
            val years = it.groupValues[1].toIntOrNull() ?: return null
            return years * 12
        }

        // "X years" pattern - convert to months
        Regex("(\\d+)\\s*years?").find(msg)?.let {
            val years = it.groupValues[1].toIntOrNull() ?: return null
            return years * 12
        }

        // "in X months" pattern
        Regex("in\\s+(\\d+)\\s*months?").find(msg)?.let {
            return it.groupValues[1].toIntOrNull()
        }

        // "X months" at end
        Regex("(\\d+)\\s*months?").find(msg)?.let {
            return it.groupValues[1].toIntOrNull()
        }

        // Word-based years
        if (msg.contains("ten years")) return 120
        if (msg.contains("five years")) return 60
        if (msg.contains("two years")) return 24
        if (msg.contains("one year") || msg.contains("a year")) return 12

        // Word-based months
        if (msg.contains("next year")) return 12
        if (msg.contains("next month")) return 1
        if (msg.contains("6 months") || msg.contains("six months")) return 6
        if (msg.contains("3 months") || msg.contains("three months")) return 3
        if (msg.contains("four months")) return 4

        return null
    }

    private fun extractItemName(msg: String): String? {
        // Extract quoted names
        Regex("\"([^\"]+)\"").find(msg)?.let { return it.groupValues[1] }
        Regex("'([^']+)'").find(msg)?.let { return it.groupValues[1] }

        // "a new X" pattern
        Regex("(?:a new|new|buy|purchase|get)\\s+(\\w+(?:\\s+\\w+)?)").find(msg)?.let {
            val item = it.groupValues[1]
            if (item !in listOf("loan", "goal", "budget", "bill", "expense")) {
                return item
            }
        }

        return null
    }

    private fun detectIntent(msg: String): Intent {
        // CRITICAL: First check if this is a FUTURE PURCHASE query with amount/timeframe
        // Pattern like "I want to buy a phone in 4 months worth 30000"
        val hasFutureTimeframe = msg.contains(Regex("in\\s+\\d+\\s*(year|month|week|day)")) ||
                msg.contains(Regex("\\d+\\s*(year|month)s?"))
        val hasAmount = msg.contains(Regex("\\d{4,}|worth|cost|\\$\\d+|\\d+\\s*(k|million)", RegexOption.IGNORE_CASE))
        val hasPurchaseVerb = msg.contains(Regex("(want|like|need|plan|going)\\s+(to\\s+)?(buy|get|purchase|acquire|save for)"))

        // If there's a purchase verb + amount + timeframe, it's AFFORDABILITY
        if (hasPurchaseVerb && (hasAmount || hasFutureTimeframe)) {
            return Intent.AFFORDABILITY
        }

        return when {
            // Explicit affordability questions
            msg.contains(Regex("can i (afford|buy|get|purchase|save for)")) ||
            msg.contains(Regex("(afford|possible|able to)\\s+(to\\s+)?(buy|get|save|purchase)")) ||
            msg.contains(Regex("(enough|sufficient)\\s+(money|funds|savings)")) ||
            msg.contains(Regex("will i (be able|manage|have enough)")) -> Intent.AFFORDABILITY

            // Loan queries
            msg.contains(Regex("loan|debt|owe|borrow|mortgage|credit|repay")) -> Intent.LOAN

            // Goal queries - AFTER affordability check, and NOT if there's an amount (that's affordability)
            (msg.contains(Regex("goal|target|aim|dream")) && !hasAmount) ||
            (msg.contains(Regex("saving for")) && !hasAmount) ||
            msg.contains(Regex("(my|check|how are my|show)\\s*goals?")) -> Intent.GOAL

            // Bill queries
            msg.contains(Regex("bill|due|payment(?!.*loan)|utilities|rent payment")) -> Intent.BILL

            // Expense queries
            msg.contains(Regex("spent|spending|expense|cost|where.*(money|go)|how much.*(spend|cost)")) -> Intent.EXPENSE

            // Income queries
            msg.contains(Regex("income|earn|salary|wage|got paid|paycheck|make per")) -> Intent.INCOME

            // Budget queries
            msg.contains(Regex("budget|limit|allowance")) -> Intent.BUDGET

            // Savings queries
            msg.contains(Regex("saving|saved|savings|put aside|emergency fund")) -> Intent.SAVINGS

            // Shopping queries
            msg.contains(Regex("shopping|grocery|list")) -> Intent.SHOPPING

            // Health/Status queries
            msg.contains(Regex("health|score|status|how.*(am i|i'm|doing)|financial standing")) -> Intent.HEALTH

            // Prediction queries
            msg.contains(Regex("predict|forecast|next month|future|will i|expect")) -> Intent.PREDICTION

            // Advice queries
            msg.contains(Regex("advice|suggest|recommend|tips|help me|should i|what.*do|improve")) -> Intent.ADVICE

            // Comparison queries
            msg.contains(Regex("compare|vs|versus|last month|previous|change|trend")) -> Intent.COMPARISON

            // Overview queries
            msg.contains(Regex("overview|summary|everything|all|total|snapshot")) -> Intent.OVERVIEW

            else -> Intent.GENERAL
        }
    }

    private fun mapIntentToQueryCategory(intent: Intent, msg: String): QueryCategory {
        return when (intent) {
            Intent.AFFORDABILITY -> QueryCategory.AFFORDABILITY
            Intent.LOAN -> {
                if (msg.contains(Regex("afford|can i|new loan"))) QueryCategory.LOAN_AFFORDABILITY
                else QueryCategory.LOAN_INFO
            }
            Intent.GOAL -> {
                if (msg.contains(Regex("can i|possible|afford|achieve|reach"))) QueryCategory.GOAL_FEASIBILITY
                else QueryCategory.GOAL_INFO
            }
            Intent.BILL -> {
                if (msg.contains(Regex("next|upcoming|when|predict"))) QueryCategory.BILL_PREDICTION
                else QueryCategory.BILL_INFO
            }
            Intent.EXPENSE -> {
                if (msg.contains(Regex("where|why|analyze|breakdown"))) QueryCategory.EXPENSE_ANALYSIS
                else QueryCategory.EXPENSE_INFO
            }
            Intent.INCOME -> {
                if (msg.contains(Regex("analyze|stable|growth"))) QueryCategory.INCOME_ANALYSIS
                else QueryCategory.INCOME_INFO
            }
            Intent.BUDGET -> {
                if (msg.contains(Regex("status|how|track"))) QueryCategory.BUDGET_STATUS
                else QueryCategory.BUDGET_INFO
            }
            Intent.SAVINGS -> {
                if (msg.contains(Regex("can i|possible|afford"))) QueryCategory.SAVINGS_POTENTIAL
                else QueryCategory.SAVINGS_INFO
            }
            Intent.SHOPPING -> QueryCategory.SHOPPING_INFO
            Intent.HEALTH -> QueryCategory.HEALTH_CHECK
            Intent.PREDICTION -> QueryCategory.PREDICTION
            Intent.ADVICE -> QueryCategory.ADVICE
            Intent.COMPARISON -> QueryCategory.COMPARISON
            Intent.OVERVIEW -> QueryCategory.GENERAL
            else -> QueryCategory.GENERAL
        }
    }

    // =====================================================
    // BUILD QUERY FOR LEARNER
    // =====================================================

    private fun buildLearnerQuery(understanding: MessageUnderstanding, englishMessage: String): LearnerQuery {
        return LearnerQuery(
            category = understanding.queryCategory ?: QueryCategory.GENERAL,
            rawQuestion = englishMessage,
            extractedAmount = understanding.amounts.firstOrNull(),
            extractedTimeframe = understanding.timeframeMonths,
            extractedCategory = null, // Could extract from message
            extractedItemName = understanding.itemName,
            userAge = userAge,
            additionalContext = mapOf(
                "previousIntent" to (understanding.previousContext?.name ?: ""),
                "conversationLength" to conversationHistory.size
            )
        )
    }

    // =====================================================
    // FORMAT RESPONSE (Age-Appropriate, Professional)
    // =====================================================

    private fun formatResponse(
        response: LearnerResponse,
        understanding: MessageUnderstanding,
        age: Int
    ): String {
        // Handle social intents locally (no Learner needed)
        if (understanding.intent == Intent.SOCIAL) {
            return formatSocialResponse(understanding.socialType, age)
        }

        // Parse the structured response from Learner
        val lines = response.answer.split("\n")
        val data = mutableMapOf<String, String>()
        var section = "MAIN"
        val sections = mutableMapOf<String, MutableList<String>>()

        for (line in lines) {
            when {
                line.startsWith("---") && line.endsWith("---") -> {
                    section = line.replace("-", "").trim()
                    sections.getOrPut(section) { mutableListOf() }
                }
                line.contains(":") && !line.startsWith(" ") -> {
                    val (key, value) = line.split(":", limit = 2)
                    data[key.trim()] = value.trim()
                }
                else -> {
                    sections.getOrPut(section) { mutableListOf() }.add(line)
                }
            }
        }

        // Build human-friendly response based on category
        return when (understanding.queryCategory) {
            QueryCategory.AFFORDABILITY -> formatAffordabilityResponse(data, sections, age)
            QueryCategory.GOAL_FEASIBILITY -> formatGoalFeasibilityResponse(data, sections, age)
            QueryCategory.LOAN_INFO -> formatLoanInfoResponse(data, sections, age)
            QueryCategory.LOAN_AFFORDABILITY -> formatLoanAffordabilityResponse(data, sections, age)
            QueryCategory.GOAL_INFO -> formatGoalInfoResponse(data, sections, age)
            QueryCategory.BILL_INFO -> formatBillInfoResponse(data, sections, age)
            QueryCategory.EXPENSE_INFO, QueryCategory.EXPENSE_ANALYSIS -> formatExpenseResponse(data, sections, age)
            QueryCategory.INCOME_INFO, QueryCategory.INCOME_ANALYSIS -> formatIncomeResponse(data, sections, age)
            QueryCategory.SAVINGS_INFO -> formatSavingsResponse(data, sections, age)
            QueryCategory.HEALTH_CHECK -> formatHealthResponse(data, sections, age)
            QueryCategory.ADVICE -> formatAdviceResponse(data, sections, age)
            QueryCategory.PREDICTION -> formatPredictionResponse(data, sections, age)
            QueryCategory.BUDGET_INFO, QueryCategory.BUDGET_STATUS -> formatBudgetResponse(data, sections, age)
            else -> formatGeneralResponse(data, sections, age)
        }
    }

    private fun formatSocialResponse(type: SocialType?, age: Int): String {
        val greeting = getAgeAppropriateGreeting(age)

        return when (type) {
            SocialType.GREETING -> {
                """
$greeting! I'm Budgie, your financial companion.

I'm here to help you:
• Understand your spending patterns
• Track bills and savings
• Get personalized insights

🔒 100% private - everything stays on your device.

What would you like to know?
                """.trimIndent()
            }
            SocialType.THANKS -> {
                when {
                    age < 25 -> "You're welcome! Keep building those good financial habits! 💪"
                    age < 40 -> "Happy to help! Your financial awareness is key to success."
                    age < 55 -> "You're welcome! Staying on top of finances is wisdom in action."
                    else -> "My pleasure! Your financial diligence is admirable."
                }
            }
            SocialType.FAREWELL -> {
                when {
                    age < 30 -> "Take care! Remember - every dollar saved today grows tomorrow! 💰"
                    age < 50 -> "Goodbye! Keep making smart financial decisions!"
                    else -> "Take care! Your financial security is well-managed."
                }
            }
            SocialType.AFFIRM -> "Great! Here are some things I can help you with:\n\n" +
                    "• View your spending breakdown\n• Check upcoming bills\n• Get savings tips\n• Analyze spending patterns\n\n" +
                    "What would you like to explore?"
            SocialType.DENY -> "No problem! Is there anything else I can help you with?"
            else -> "How can I help you today?"
        }
    }

    private fun getAgeAppropriateGreeting(age: Int): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        return when {
            age < 25 -> "$timeGreeting"
            age < 40 -> "$timeGreeting, $userName"
            else -> "$timeGreeting, $userName"
        }
    }

    private fun formatAffordabilityResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val item = data["ITEM"] ?: "this"
        val amount = formatCurrency(data["AMOUNT"]?.toDoubleOrNull() ?: 0.0)
        val timeframe = data["TIMEFRAME"]?.replace(" months", "") ?: "?"
        val probability = data["PROBABILITY"]?.replace("%", "")?.toIntOrNull() ?: 0
        val verdict = data["VERDICT"] ?: "UNKNOWN"
        val monthlyNeeded = formatCurrency(data["MONTHLY_NEEDED"]?.toDoubleOrNull() ?: 0.0)
        val monthlySavings = formatCurrency(data["MONTHLY_AVAILABLE"]?.toDoubleOrNull() ?: 0.0)

        val emoji = when {
            probability >= 80 -> "✅"
            probability >= 50 -> "🟡"
            probability >= 20 -> "⚠️"
            else -> "🔴"
        }

        val sb = StringBuilder()
        sb.appendLine("$emoji **Can you afford $item?**\n")
        sb.appendLine("**Target:** $amount in $timeframe months")
        sb.appendLine("**Monthly needed:** $monthlyNeeded")
        sb.appendLine("**Your monthly savings:** $monthlySavings")
        sb.appendLine("**Probability of success:** $probability%\n")

        // Verdict message
        val verdictMessage = sections["VERDICT"]?.find { it.startsWith("MESSAGE:") }?.removePrefix("MESSAGE:") ?: ""
        if (verdictMessage.isNotEmpty()) {
            sb.appendLine("$verdictMessage\n")
        }

        // Tips
        val tips = sections["TIPS"]?.filter { it.startsWith("TIP:") }?.map { it.removePrefix("TIP:") } ?: emptyList()
        if (tips.isNotEmpty()) {
            sb.appendLine("**💡 Tips:**")
            tips.take(3).forEach { sb.appendLine("• $it") }
            sb.appendLine()
        }

        // Age-specific advice
        val ageAdvice = sections["AGE_CONTEXT"]?.find { it.startsWith("AGE_ADVICE:") }?.removePrefix("AGE_ADVICE:")
        if (ageAdvice != null) {
            sb.appendLine("**📝 ${getAgeContextLabel(age)}:** $ageAdvice")
        }

        return sb.toString().trim()
    }

    private fun formatGoalFeasibilityResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val item = data["ITEM"] ?: "your goal"
        val probability = data["PROBABILITY"]?.replace("%", "")?.toIntOrNull() ?: 0
        val monthlyNeeded = formatCurrency(data["MONTHLY_NEEDED"]?.toDoubleOrNull() ?: 0.0)
        val monthlySavings = formatCurrency(data["CURRENT_MONTHLY_SAVINGS"]?.toDoubleOrNull() ?: 0.0)

        val emoji = when {
            probability >= 80 -> "🎯"
            probability >= 50 -> "📊"
            else -> "💡"
        }

        return buildString {
            appendLine("$emoji **Goal Analysis: $item**\n")
            appendLine("**Probability of success:** $probability%")
            appendLine("**Monthly savings needed:** $monthlyNeeded")
            appendLine("**Your current capacity:** $monthlySavings\n")

            // Analysis
            val analysis = sections["ANALYSIS"]?.filterNot { it.isBlank() } ?: emptyList()
            analysis.forEach { line ->
                if (line.startsWith("VERDICT:")) {
                    // Skip, we'll handle this differently
                } else if (line.startsWith("MESSAGE:")) {
                    appendLine(line.removePrefix("MESSAGE:") + "\n")
                }
            }

            // Tips
            val tips = sections["TIPS"]?.filter { it.startsWith("TIP:") }?.map { it.removePrefix("TIP:") } ?: emptyList()
            if (tips.isNotEmpty()) {
                appendLine("**Suggestions:**")
                tips.take(3).forEach { appendLine("• $it") }
            }
        }.trim()
    }

    private fun formatLoanInfoResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        if (data["TOTAL_LOANS"] == "0" || data.containsKey("NO_LOANS")) {
            return "📋 You don't have any loans recorded.\n\n${getAgeAppropriateNoLoanAdvice(age)}"
        }

        val totalLoans = data["ACTIVE_LOANS"] ?: "0"
        val totalRemaining = formatCurrency(data["TOTAL_REMAINING"]?.toDoubleOrNull() ?: 0.0)
        val totalPaid = formatCurrency(data["TOTAL_PAID"]?.toDoubleOrNull() ?: 0.0)
        val debtToIncome = data["DEBT_TO_INCOME"]?.toDoubleOrNull()?.toInt() ?: 0

        return buildString {
            appendLine("📋 **Your Loans Summary**\n")
            appendLine("**Active loans:** $totalLoans")
            appendLine("**Total remaining:** $totalRemaining")
            appendLine("**Total paid:** $totalPaid")
            appendLine("**Debt-to-income:** $debtToIncome%\n")

            // Loan details
            val details = sections["DETAILS"]?.filter { it.startsWith("LOAN:") } ?: emptyList()
            if (details.isNotEmpty()) {
                appendLine("**Loan Details:**")
                // Parse loan details
                val detailLines = sections["DETAILS"] ?: emptyList()
                var currentLoan = ""
                detailLines.forEach { line ->
                    when {
                        line.startsWith("LOAN:") -> {
                            currentLoan = line.removePrefix("LOAN:")
                            appendLine("\n• **$currentLoan**")
                        }
                        line.trim().startsWith("REMAINING:") -> {
                            val remaining = formatCurrency(line.substringAfter(":").trim().toDoubleOrNull() ?: 0.0)
                            appendLine("  Remaining: $remaining")
                        }
                        line.trim().startsWith("MONTHLY_PAYMENT:") -> {
                            val monthly = formatCurrency(line.substringAfter(":").trim().toDoubleOrNull() ?: 0.0)
                            appendLine("  Monthly payment: $monthly")
                        }
                        line.trim().startsWith("PAYMENTS_LEFT:") -> {
                            appendLine("  Payments left: ${line.substringAfter(":").trim()}")
                        }
                    }
                }
            }

            // Age advice
            val advice = sections["ADVICE"]?.firstOrNull { it.isNotBlank() }
            if (advice != null) {
                appendLine("\n**💡 Advice:** $advice")
            }
        }.trim()
    }

    private fun formatLoanAffordabilityResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val canAfford = data["CAN_AFFORD"]?.toBoolean() ?: false
        val loanAmount = formatCurrency(data["LOAN_AMOUNT"]?.toDoubleOrNull() ?: 0.0)
        val estimatedPayment = formatCurrency(data["ESTIMATED_MONTHLY"]?.toDoubleOrNull() ?: 0.0)
        val affordabilityScore = data["AFFORDABILITY_SCORE"]?.toIntOrNull() ?: 0

        val emoji = if (canAfford) "✅" else "⚠️"

        return buildString {
            appendLine("$emoji **Loan Affordability Analysis**\n")
            appendLine("**Loan amount:** $loanAmount")
            appendLine("**Estimated monthly:** $estimatedPayment")
            appendLine("**Affordability score:** $affordabilityScore%\n")

            // Verdict
            val analysisLines = sections["ANALYSIS"] ?: emptyList()
            analysisLines.forEach { line ->
                if (line.startsWith("VERDICT:") || line.isBlank()) return@forEach
                appendLine(line)
            }
        }.trim()
    }

    private fun formatGoalInfoResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        if (data.containsKey("NO_GOALS")) {
            return "🎯 You haven't set any financial goals yet.\n\n${getAgeAppropriateGoalAdvice(age)}"
        }

        val totalGoals = data["ACTIVE_GOALS"] ?: "0"
        val totalTarget = formatCurrency(data["TOTAL_TARGET"]?.toDoubleOrNull() ?: 0.0)
        val totalSaved = formatCurrency(data["TOTAL_SAVED"]?.toDoubleOrNull() ?: 0.0)
        val progress = data["OVERALL_PROGRESS"] ?: "0%"

        return buildString {
            appendLine("🎯 **Your Financial Goals**\n")
            appendLine("**Active goals:** $totalGoals")
            appendLine("**Total target:** $totalTarget")
            appendLine("**Total saved:** $totalSaved")
            appendLine("**Overall progress:** $progress\n")

            // Goal details
            val detailLines = sections["DETAILS"] ?: emptyList()
            if (detailLines.isNotEmpty()) {
                appendLine("**Goal Details:**")
                var currentGoal = ""
                detailLines.forEach { line ->
                    when {
                        line.startsWith("GOAL:") -> {
                            currentGoal = line.removePrefix("GOAL:")
                            appendLine("\n• **$currentGoal**")
                        }
                        line.trim().startsWith("PROGRESS:") -> {
                            appendLine("  Progress: ${line.substringAfter(":").trim()}")
                        }
                        line.trim().startsWith("DAYS_LEFT:") -> {
                            appendLine("  Days remaining: ${line.substringAfter(":").trim()}")
                        }
                        line.trim().startsWith("ON_TRACK:") -> {
                            val onTrack = line.substringAfter(":").trim().toBoolean()
                            appendLine("  Status: ${if (onTrack) "✅ On track" else "⚠️ Needs attention"}")
                        }
                    }
                }
            }
        }.trim()
    }

    private fun formatBillInfoResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        if (data.containsKey("NO_BILLS")) {
            return "📄 You don't have any bills recorded. Track your bills to stay on top of payments!"
        }

        val totalBills = data["TOTAL_BILLS"] ?: "0"
        val unpaid = data["UNPAID"] ?: "0"
        val overdue = data["OVERDUE"] ?: "0"
        val totalDue = formatCurrency(data["TOTAL_DUE"]?.toDoubleOrNull() ?: 0.0)

        return buildString {
            appendLine("📄 **Your Bills**\n")
            appendLine("**Total bills:** $totalBills")
            appendLine("**Unpaid:** $unpaid")
            if (overdue.toIntOrNull() ?: 0 > 0) {
                appendLine("**🔴 Overdue:** $overdue")
            }
            appendLine("**Total due:** $totalDue\n")

            // Overdue bills
            val overdueLines = sections["OVERDUE"]?.filter { it.startsWith("OVERDUE_BILL:") } ?: emptyList()
            if (overdueLines.isNotEmpty()) {
                appendLine("**⚠️ Overdue Bills:**")
                overdueLines.forEach { line ->
                    val parts = line.removePrefix("OVERDUE_BILL:").split("|")
                    if (parts.size >= 3) {
                        appendLine("• ${parts[0]}: ${formatCurrency(parts[1].toDoubleOrNull() ?: 0.0)} - ${parts[2]} overdue")
                    }
                }
                appendLine()
            }

            // Upcoming bills
            val upcomingLines = sections["UPCOMING"]?.filter { it.startsWith("UPCOMING_BILL:") } ?: emptyList()
            if (upcomingLines.isNotEmpty()) {
                appendLine("**📅 Upcoming:**")
                upcomingLines.take(5).forEach { line ->
                    val parts = line.removePrefix("UPCOMING_BILL:").split("|")
                    if (parts.size >= 4) {
                        val urgency = when (parts[3]) {
                            "URGENT" -> "🔴"
                            "SOON" -> "🟡"
                            else -> "🟢"
                        }
                        appendLine("$urgency ${parts[0]}: ${formatCurrency(parts[1].toDoubleOrNull() ?: 0.0)} in ${parts[2]} days")
                    }
                }
            }
        }.trim()
    }

    private fun formatExpenseResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        if (data.containsKey("NO_EXPENSES")) {
            return "💸 No expenses recorded yet. Start tracking to understand your spending!"
        }

        val monthlyTotal = formatCurrency(data["MONTHLY_TOTAL"]?.toDoubleOrNull() ?: 0.0)
        val dailyAvg = formatCurrency(data["DAILY_AVERAGE"]?.toDoubleOrNull() ?: 0.0)
        val trend = data["TREND"] ?: "Stable"

        return buildString {
            appendLine("💸 **Your Spending**\n")
            appendLine("**This month:** $monthlyTotal")
            appendLine("**Daily average:** $dailyAvg")
            appendLine("**Trend:** $trend\n")

            // By category
            val categoryLines = sections["BY_CATEGORY"]?.filter { it.startsWith("CATEGORY:") } ?: emptyList()
            if (categoryLines.isNotEmpty()) {
                appendLine("**By Category:**")
                categoryLines.take(5).forEach { line ->
                    val parts = line.removePrefix("CATEGORY:").split("|")
                    if (parts.size >= 3) {
                        appendLine("• ${parts[0]}: ${formatCurrency(parts[1].toDoubleOrNull() ?: 0.0)} (${parts[2]})")
                    }
                }
            }
        }.trim()
    }

    private fun formatIncomeResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        if (data.containsKey("NO_INCOME")) {
            return "💰 No income recorded. Add your income sources for better insights!"
        }

        val monthlyIncome = formatCurrency(data["MONTHLY_INCOME"]?.toDoubleOrNull() ?: 0.0)
        val totalRecorded = formatCurrency(data["TOTAL_RECORDED"]?.toDoubleOrNull() ?: 0.0)

        return buildString {
            appendLine("💰 **Your Income**\n")
            appendLine("**Monthly income:** $monthlyIncome")
            appendLine("**Total recorded:** $totalRecorded")

            // By source
            val sourceLines = sections["BY_SOURCE"]?.filter { it.startsWith("SOURCE:") } ?: emptyList()
            if (sourceLines.isNotEmpty()) {
                appendLine("\n**By Source:**")
                sourceLines.forEach { line ->
                    val parts = line.removePrefix("SOURCE:").split("|")
                    if (parts.size >= 2) {
                        appendLine("• ${parts[0]}: ${formatCurrency(parts[1].toDoubleOrNull() ?: 0.0)}")
                    }
                }
            }
        }.trim()
    }

    private fun formatSavingsResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val savingsRate = data["SAVINGS_RATE"]?.replace("%", "")?.toIntOrNull() ?: 0
        val monthlySavings = formatCurrency(data["MONTHLY_SAVINGS"]?.toDoubleOrNull() ?: 0.0)
        val status = data["STATUS"] ?: "UNKNOWN"

        val emoji = when {
            savingsRate >= 30 -> "🌟"
            savingsRate >= 20 -> "✅"
            savingsRate >= 10 -> "📊"
            else -> "⚠️"
        }

        return buildString {
            appendLine("$emoji **Your Savings**\n")
            appendLine("**Savings rate:** $savingsRate%")
            appendLine("**Monthly savings:** $monthlySavings")
            appendLine("**Status:** ${status.replace("_", " ").lowercase().capitalize()}\n")

            // Projections
            val projectionLines = sections["PROJECTIONS"] ?: emptyList()
            if (projectionLines.isNotEmpty()) {
                appendLine("**Projections:**")
                projectionLines.forEach { line ->
                    when {
                        line.startsWith("1_YEAR:") -> appendLine("• 1 year: ${formatCurrency(line.substringAfter(":").toDoubleOrNull() ?: 0.0)}")
                        line.startsWith("5_YEARS:") -> appendLine("• 5 years: ${formatCurrency(line.substringAfter(":").toDoubleOrNull() ?: 0.0)}")
                    }
                }
            }

            // Targets
            val targetLines = sections["TARGETS"] ?: emptyList()
            val targetRate = targetLines.find { it.startsWith("TARGET_RATE:") }?.substringAfter(":")?.trim()
            val focus = targetLines.find { it.startsWith("FOCUS:") }?.substringAfter(":")?.trim()

            if (targetRate != null) {
                appendLine("\n**${getAgeContextLabel(age)} target:** $targetRate")
            }
            if (focus != null) {
                appendLine("**Focus area:** $focus")
            }
        }.trim()
    }

    private fun formatHealthResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val score = data["SCORE"]?.toIntOrNull() ?: 0
        val riskLevel = data["RISK_LEVEL"] ?: "Unknown"
        val savingsRate = data["SAVINGS_RATE"]?.replace("%", "")?.toIntOrNull() ?: 0

        val emoji = when {
            score >= 80 -> "🌟"
            score >= 60 -> "✅"
            score >= 40 -> "📊"
            else -> "⚠️"
        }

        return buildString {
            appendLine("$emoji **Financial Health Score: $score/100**\n")
            appendLine("**Risk level:** $riskLevel")
            appendLine("**Savings rate:** $savingsRate%\n")

            // Breakdown
            val breakdown = sections["BREAKDOWN"] ?: emptyList()
            if (breakdown.isNotEmpty()) {
                appendLine("**Quick Stats:**")
                breakdown.forEach { line ->
                    when {
                        line.startsWith("INCOME:") -> appendLine("• Income: ${formatCurrency(line.substringAfter(":").toDoubleOrNull() ?: 0.0)}")
                        line.startsWith("EXPENSES:") -> appendLine("• Expenses: ${formatCurrency(line.substringAfter(":").toDoubleOrNull() ?: 0.0)}")
                        line.startsWith("ACTIVE_LOANS:") -> appendLine("• Active loans: ${line.substringAfter(":")}")
                        line.startsWith("ACTIVE_GOALS:") -> appendLine("• Active goals: ${line.substringAfter(":")}")
                    }
                }
            }

            // Rating
            val rating = sections["RATING"]?.firstOrNull()?.removePrefix("RATING:")?.trim()
            if (rating != null) {
                appendLine("\n**Assessment:** $rating")
            }
        }.trim()
    }

    private fun formatAdviceResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val priorities = sections["PRIORITIES"]?.filter { it.matches(Regex("\\d+\\..*")) } ?: emptyList()

        return buildString {
            appendLine("🎯 **Financial Advice for You**\n")

            if (priorities.isNotEmpty()) {
                appendLine("**Priorities:**")
                priorities.take(4).forEach { line ->
                    val priority = line.substringAfter(".")
                    val (level, advice) = if (priority.contains(":")) {
                        val parts = priority.split(":", limit = 2)
                        parts[0].trim() to parts[1].trim()
                    } else {
                        "INFO" to priority.trim()
                    }

                    val emoji = when (level) {
                        "URGENT" -> "🔴"
                        "HIGH" -> "🟠"
                        "MEDIUM" -> "🟡"
                        else -> "🟢"
                    }
                    appendLine("$emoji $advice")
                }
                appendLine()
            }

            // Age-specific
            val ageAdvice = sections["AGE_SPECIFIC"]?.filterNot { it.isBlank() } ?: emptyList()
            if (ageAdvice.isNotEmpty()) {
                appendLine("**${getAgeContextLabel(age)} Advice:**")
                ageAdvice.forEach { line ->
                    when {
                        line.startsWith("ADVICE:") -> appendLine("• ${line.removePrefix("ADVICE:")}")
                        line.startsWith("ACTION:") -> appendLine("• **Action:** ${line.removePrefix("ACTION:")}")
                    }
                }
            }
        }.trim()
    }

    private fun formatPredictionResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val predictedSpending = formatCurrency(data["PREDICTED_SPENDING"]?.toDoubleOrNull() ?: 0.0)
        val predictedSavings = formatCurrency(data["PREDICTED_SAVINGS"]?.toDoubleOrNull() ?: 0.0)
        val trend = data["SPENDING_TREND"] ?: "Stable"

        return buildString {
            appendLine("🔮 **Financial Forecast**\n")
            appendLine("**Predicted next month spending:** $predictedSpending")
            appendLine("**Expected savings:** $predictedSavings")
            appendLine("**Current trend:** $trend\n")

            // Analysis
            val analysis = sections["TREND_ANALYSIS"]?.firstOrNull { it.isNotBlank() }
            if (analysis != null) {
                appendLine("**Analysis:** $analysis")
            }
        }.trim()
    }

    private fun formatBudgetResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        if (data.containsKey("NO_BUDGETS")) {
            return "📊 No budgets set up yet. Budgets help control spending!"
        }

        val totalBudget = formatCurrency(data["TOTAL_BUDGET"]?.toDoubleOrNull() ?: 0.0)
        val utilization = data["AVERAGE_UTILIZATION"]?.replace("%", "")?.toIntOrNull() ?: 0
        val overBudget = data["OVER_BUDGET"]?.toIntOrNull() ?: 0

        return buildString {
            appendLine("📊 **Your Budgets**\n")
            appendLine("**Total budget:** $totalBudget")
            appendLine("**Average utilization:** $utilization%")
            if (overBudget > 0) {
                appendLine("**⚠️ Over budget in:** $overBudget categories")
            }

            // Categories
            val categoryLines = sections["CATEGORIES"]?.filter { it.startsWith("BUDGET:") } ?: emptyList()
            if (categoryLines.isNotEmpty()) {
                appendLine("\n**Budget Status:**")
                categoryLines.forEach { line ->
                    val parts = line.removePrefix("BUDGET:").split("|")
                    if (parts.size >= 5) {
                        val status = when (parts[4]) {
                            "OVER" -> "🔴"
                            "WARNING" -> "🟡"
                            else -> "🟢"
                        }
                        appendLine("$status ${parts[0]}: ${formatCurrency(parts[2].toDoubleOrNull() ?: 0.0)}/${formatCurrency(parts[1].toDoubleOrNull() ?: 0.0)} (${parts[3]})")
                    }
                }
            }
        }.trim()
    }

    private fun formatGeneralResponse(data: Map<String, String>, sections: Map<String, MutableList<String>>, age: Int): String {
        val healthScore = data["HEALTH_SCORE"]?.toIntOrNull() ?: 0
        val income = formatCurrency(data["INCOME"]?.toDoubleOrNull() ?: 0.0)
        val expenses = formatCurrency(data["EXPENSES"]?.toDoubleOrNull() ?: 0.0)
        val savingsRate = data["SAVINGS_RATE"]?.replace("%", "")?.toIntOrNull() ?: 0

        val emoji = when {
            healthScore >= 80 -> "🌟"
            healthScore >= 60 -> "✅"
            healthScore >= 40 -> "📊"
            else -> "⚠️"
        }

        return buildString {
            appendLine("$emoji **Your Financial Snapshot**\n")
            appendLine("**Health Score:** $healthScore/100")
            appendLine("**Monthly Income:** $income")
            appendLine("**Monthly Expenses:** $expenses")
            appendLine("**Savings Rate:** $savingsRate%\n")
            appendLine("What would you like to explore?")
        }.trim()
    }

    // =====================================================
    // HELPER FUNCTIONS
    // =====================================================

    private fun formatCurrency(amount: Double): String {
        return "$${String.format(Locale.US, "%,.2f", amount)}"
    }

    private fun getAgeContextLabel(age: Int): String {
        return when {
            age < 25 -> "For your 20s"
            age < 35 -> "For your early career"
            age < 45 -> "At mid-career"
            age < 55 -> "At your peak earning years"
            age < 65 -> "Approaching retirement"
            else -> "In retirement"
        }
    }

    private fun getAgeAppropriateNoLoanAdvice(age: Int): String {
        return when {
            age < 25 -> "Being debt-free at your age is a great start! If you need to borrow in the future, compare rates carefully and avoid high-interest debt."
            age < 40 -> "Being debt-free gives you flexibility. When considering debt (like a mortgage), ensure it fits your long-term plans."
            age < 55 -> "Being debt-free at this stage is excellent! It allows you to maximize retirement contributions."
            else -> "Being debt-free provides great security for retirement. Keep it that way if possible!"
        }
    }

    private fun getAgeAppropriateGoalAdvice(age: Int): String {
        return when {
            age < 25 -> "Start with an emergency fund (3 months expenses) and begin retirement savings - time is your greatest asset!"
            age < 35 -> "Key goals at this stage: Emergency fund, retirement savings, and saving for major purchases like a home."
            age < 50 -> "Focus on maximizing retirement contributions, paying off debt, and children's education if applicable."
            age < 60 -> "Accelerate retirement savings with catch-up contributions. Ensure healthcare costs are planned for."
            else -> "Ensure your retirement income and healthcare needs are secure. Consider legacy planning."
        }
    }

    private suspend fun generateSuggestions(baseSuggestions: List<String>, language: String): List<String> {
        val suggestions = baseSuggestions.ifEmpty {
            listOf("How am I doing?", "Show spending", "Get advice")
        }

        // Translate suggestions if needed
        return if (language != "en") {
            suggestions.take(3).map { translateFromEnglish(it, language) }
        } else {
            suggestions.take(3)
        }
    }

    // =====================================================
    // DATA CLASSES
    // =====================================================

    private data class ConversationTurn(
        val userMessage: String,
        val intent: Intent,
        val response: String
    )

    private data class MessageUnderstanding(
        val intent: Intent,
        val queryCategory: QueryCategory? = null,
        val socialType: SocialType? = null,
        val amounts: List<Double> = emptyList(),
        val timeframeMonths: Int? = null,
        val itemName: String? = null,
        val rawMessage: String,
        val previousContext: Intent? = null
    )

    private enum class Intent {
        SOCIAL,
        AFFORDABILITY,
        LOAN,
        GOAL,
        BILL,
        EXPENSE,
        INCOME,
        BUDGET,
        SAVINGS,
        SHOPPING,
        HEALTH,
        PREDICTION,
        ADVICE,
        COMPARISON,
        OVERVIEW,
        GENERAL
    }

    private enum class SocialType {
        GREETING, THANKS, FAREWELL, AFFIRM, DENY
    }
}

// ChatMessage is defined in AIModels.kt
