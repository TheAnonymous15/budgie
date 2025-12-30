package com.example.budgie.ai

import android.util.Log
import org.json.JSONObject

/**
 * Intent & Entity Extraction
 * Uses Qwen with forced JSON schema output
 * Machine-safe, deterministic, easy to validate
 */
class IntentExtractor {

    enum class Intent {
        QUERY,          // User asking a question
        ACTION,         // User wants to do something
        ADVICE,         // User asking for recommendation
        PREDICTION,     // User asking about future
        UNKNOWN
    }

    data class Entity(
        val amount: Double? = null,
        val currency: String? = null,
        val timeframe: String? = null,
        val category: String? = null,
        val item: String? = null,
        val goalType: String? = null
    )

    data class ExtractedIntent(
        val intent: Intent,
        val entities: Entity,
        val originalQuery: String,
        val confidence: Float
    )

    /**
     * Build system prompt for intent extraction
     */
    fun buildIntentExtractionPrompt(userQuery: String): String {
        return """You are an intent parser for a financial assistant app.

Return JSON ONLY. No explanation. No markdown.

Schema:
{
  "intent": "query|action|advice|prediction",
  "entities": {
    "amount": number or null,
    "currency": "string or null",
    "timeframe": "string or null",
    "category": "string or null",
    "item": "string or null",
    "goalType": "string or null"
  }
}

Categories: food, transport, utilities, entertainment, healthcare, education, shopping, bills, rent, other

User: $userQuery

JSON:"""
    }

    /**
     * Parse LLM response into structured intent
     */
    fun parseIntentResponse(response: String, originalQuery: String): ExtractedIntent {
        return try {
            Log.d("IntentExtractor", "Parsing response: $response")

            // Clean response - remove markdown if present
            val cleanJson = response
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleanJson)

            val intentStr = json.optString("intent", "unknown")
            val intent = when (intentStr.lowercase()) {
                "query" -> Intent.QUERY
                "action" -> Intent.ACTION
                "advice" -> Intent.ADVICE
                "prediction" -> Intent.PREDICTION
                else -> Intent.UNKNOWN
            }

            val entitiesJson = json.optJSONObject("entities")
            val entities = Entity(
                amount = entitiesJson?.optDouble("amount")?.takeIf { !it.isNaN() },
                currency = entitiesJson?.optString("currency")?.takeIf { it.isNotBlank() },
                timeframe = entitiesJson?.optString("timeframe")?.takeIf { it.isNotBlank() },
                category = entitiesJson?.optString("category")?.takeIf { it.isNotBlank() },
                item = entitiesJson?.optString("item")?.takeIf { it.isNotBlank() },
                goalType = entitiesJson?.optString("goalType")?.takeIf { it.isNotBlank() }
            )

            ExtractedIntent(
                intent = intent,
                entities = entities,
                originalQuery = originalQuery,
                confidence = 0.85f
            )
        } catch (e: Exception) {
            Log.e("IntentExtractor", "Error parsing intent: ${e.message}")

            // Fallback to rule-based extraction
            extractWithRules(originalQuery)
        }
    }

    /**
     * Fallback rule-based extraction if LLM fails
     */
    private fun extractWithRules(query: String): ExtractedIntent {
        val normalized = query.lowercase()

        // Detect intent
        val intent = when {
            normalized.contains("can i") || normalized.contains("will i") ||
            normalized.contains("able to") -> Intent.PREDICTION

            normalized.contains("should i") || normalized.contains("recommend") ||
            normalized.contains("advice") || normalized.contains("suggest") -> Intent.ADVICE

            normalized.contains("add") || normalized.contains("create") ||
            normalized.contains("delete") || normalized.contains("update") -> Intent.ACTION

            normalized.contains("?") || normalized.contains("what") ||
            normalized.contains("how") || normalized.contains("when") -> Intent.QUERY

            else -> Intent.UNKNOWN
        }

        // Extract amount (simple regex)
        val amountRegex = """\$?\d+(?:,\d{3})*(?:\.\d{2})?""".toRegex()
        val amountMatch = amountRegex.find(query)
        val amount = amountMatch?.value?.replace("$", "")?.replace(",", "")?.toDoubleOrNull()

        // Extract timeframe
        val timeframe = when {
            normalized.contains("month") -> {
                val months = """\d+\s*month""".toRegex().find(normalized)?.value
                months ?: "1 month"
            }
            normalized.contains("year") -> {
                val years = """\d+\s*year""".toRegex().find(normalized)?.value
                years ?: "1 year"
            }
            normalized.contains("week") -> {
                val weeks = """\d+\s*week""".toRegex().find(normalized)?.value
                weeks ?: "1 week"
            }
            normalized.contains("day") -> {
                val days = """\d+\s*day""".toRegex().find(normalized)?.value
                days ?: "1 day"
            }
            else -> null
        }

        // Extract category
        val category = listOf(
            "food", "transport", "utilities", "entertainment", "healthcare",
            "education", "shopping", "bills", "rent", "savings", "loan", "goal"
        ).find { normalized.contains(it) }

        return ExtractedIntent(
            intent = intent,
            entities = Entity(
                amount = amount,
                currency = if (amount != null) "USD" else null,
                timeframe = timeframe,
                category = category
            ),
            originalQuery = query,
            confidence = 0.6f
        )
    }
}

