package com.example.budgie.ai

import com.example.budgie.data.model.*
import kotlin.math.roundToInt

/**
 * AI Shopping List Analyzer
 * Analyzes shopping list items based on income, expenses, and priorities
 * Provides recommendations to optimize spending
 */
class ShoppingListAnalyzer {

    /**
     * Analyze a shopping list and provide recommendations
     */
    fun analyzeShoppingList(
        items: List<ShoppingItem>,
        budget: Double,
        monthlyIncome: Double,
        monthlyExpenses: Double,
        savingsRate: Double = 0.0
    ): ShoppingAIAnalysis {
        if (items.isEmpty()) {
            return ShoppingAIAnalysis(
                originalTotal = 0.0,
                suggestedTotal = 0.0,
                potentialSavings = 0.0,
                itemAnalysis = emptyList(),
                overallAdvice = "Add items to your shopping list to get AI recommendations.",
                budgetHealthScore = 1.0f
            )
        }

        val originalTotal = items.sumOf { it.estimatedPrice * it.quantity }
        val disposableIncome = monthlyIncome - monthlyExpenses

        // Calculate affordability ratio
        val affordabilityRatio = if (disposableIncome > 0) {
            originalTotal / disposableIncome
        } else {
            2.0 // High ratio if no disposable income
        }

        val itemAnalyses = mutableListOf<ItemAnalysis>()
        var suggestedTotal = 0.0

        items.forEach { item ->
            val analysis = analyzeItem(
                item = item,
                budget = budget,
                originalTotal = originalTotal,
                affordabilityRatio = affordabilityRatio,
                monthlyIncome = monthlyIncome
            )
            itemAnalyses.add(analysis)

            val suggestedQty = analysis.suggestedQuantity ?: item.quantity
            suggestedTotal += item.estimatedPrice * suggestedQty
        }

        val potentialSavings = originalTotal - suggestedTotal

        // Budget health score (0-1)
        val budgetHealthScore = when {
            budget <= 0 -> if (affordabilityRatio < 0.3) 0.8f else 0.5f
            originalTotal <= budget * 0.8 -> 1.0f
            originalTotal <= budget -> 0.8f
            originalTotal <= budget * 1.2 -> 0.5f
            else -> 0.3f
        }

        val overallAdvice = generateOverallAdvice(
            itemAnalyses = itemAnalyses,
            originalTotal = originalTotal,
            suggestedTotal = suggestedTotal,
            budget = budget,
            affordabilityRatio = affordabilityRatio
        )

        return ShoppingAIAnalysis(
            originalTotal = originalTotal,
            suggestedTotal = suggestedTotal,
            potentialSavings = potentialSavings,
            itemAnalysis = itemAnalyses,
            overallAdvice = overallAdvice,
            budgetHealthScore = budgetHealthScore
        )
    }

    private fun analyzeItem(
        item: ShoppingItem,
        budget: Double,
        originalTotal: Double,
        affordabilityRatio: Double,
        monthlyIncome: Double
    ): ItemAnalysis {
        val itemTotal = item.estimatedPrice * item.quantity
        val itemShareOfTotal = if (originalTotal > 0) itemTotal / originalTotal else 0.0
        val itemShareOfIncome = if (monthlyIncome > 0) itemTotal / monthlyIncome else 0.0

        // Decision factors
        val priorityFactor = (6 - item.needPriority) / 5.0 // Higher for essential items
        val quantityFactor = when {
            item.quantity >= 10 -> 0.3 // High quantity items are candidates for reduction
            item.quantity >= 5 -> 0.5
            item.quantity >= 3 -> 0.7
            else -> 0.9
        }

        // Category-based factors
        val categoryFactor = when (item.category) {
            ShoppingCategory.GROCERIES, ShoppingCategory.HEALTH, ShoppingCategory.BABY -> 0.9
            ShoppingCategory.HOUSEHOLD, ShoppingCategory.CLEANING, ShoppingCategory.PERSONAL_CARE -> 0.8
            ShoppingCategory.SNACKS, ShoppingCategory.ELECTRONICS, ShoppingCategory.CLOTHING -> 0.5
            else -> 0.7
        }

        // Calculate recommendation score (0-1, higher = keep)
        val keepScore = (priorityFactor * 0.4 + quantityFactor * 0.3 + categoryFactor * 0.3)

        // Adjust based on budget situation
        val adjustedScore = if (affordabilityRatio > 0.5) {
            keepScore * 0.8 // Be more strict when budget is tight
        } else {
            keepScore
        }

        // Determine recommendation
        data class AnalysisResult(val rec: ShoppingRecommendation, val qty: Int?, val msg: String)

        val result: AnalysisResult = when {
            // Essential items with priority 1-2 are always OK
            item.needPriority <= 2 && item.quantity <= 3 -> {
                AnalysisResult(ShoppingRecommendation.OK, null, "Essential item - good to purchase")
            }

            // High affordability and low priority
            affordabilityRatio > 0.7 && item.needPriority >= 4 -> {
                val suggestedQty = (item.quantity * 0.5).roundToInt().coerceAtLeast(1)
                val msg = if (suggestedQty < item.quantity) {
                    "Budget is tight. Consider reducing to $suggestedQty ${item.unit}"
                } else {
                    "Budget is tight. Consider if this is necessary"
                }
                AnalysisResult(ShoppingRecommendation.REMOVE, suggestedQty, msg)
            }

            // High quantity items that could be reduced
            item.quantity >= 4 && adjustedScore < 0.7 -> {
                val suggestedQty = (item.quantity * 0.6).roundToInt().coerceAtLeast(1)
                AnalysisResult(
                    ShoppingRecommendation.REDUCE,
                    suggestedQty,
                    "Consider buying $suggestedQty instead of ${item.quantity} to save money"
                )
            }

            // Snacks and non-essentials when budget is tight
            (item.category == ShoppingCategory.SNACKS || item.category == ShoppingCategory.ELECTRONICS)
                    && affordabilityRatio > 0.4 -> {
                val suggestedQty = (item.quantity * 0.5).roundToInt().coerceAtLeast(1)
                AnalysisResult(
                    ShoppingRecommendation.REDUCE,
                    suggestedQty,
                    "Non-essential category. Consider reducing quantity"
                )
            }

            // Item takes large share of budget
            itemShareOfTotal > 0.2 && item.needPriority >= 3 -> {
                AnalysisResult(
                    ShoppingRecommendation.REDUCE,
                    (item.quantity * 0.7).roundToInt().coerceAtLeast(1),
                    "This item is ${(itemShareOfTotal * 100).roundToInt()}% of your total. Consider alternatives"
                )
            }

            // Good to go
            adjustedScore >= 0.7 -> {
                AnalysisResult(ShoppingRecommendation.OK, null, "Reasonable purchase based on priority and budget")
            }

            // Moderate concern
            adjustedScore >= 0.5 -> {
                AnalysisResult(
                    ShoppingRecommendation.REDUCE,
                    (item.quantity * 0.8).roundToInt().coerceAtLeast(1),
                    "Consider if full quantity is needed this time"
                )
            }

            // Low priority, consider removing
            else -> {
                AnalysisResult(
                    ShoppingRecommendation.REMOVE,
                    1,
                    "Low priority item. Consider skipping or buying minimum"
                )
            }
        }

        val potentialSaving = if (result.qty != null && result.qty < item.quantity) {
            item.estimatedPrice * (item.quantity - result.qty)
        } else {
            0.0
        }

        return ItemAnalysis(
            itemId = item.id,
            recommendation = result.rec,
            suggestedQuantity = result.qty,
            reason = result.msg,
            potentialSaving = potentialSaving
        )
    }

    private fun generateOverallAdvice(
        itemAnalyses: List<ItemAnalysis>,
        originalTotal: Double,
        suggestedTotal: Double,
        budget: Double,
        affordabilityRatio: Double
    ): String {
        val okCount = itemAnalyses.count { it.recommendation == ShoppingRecommendation.OK }
        val reduceCount = itemAnalyses.count { it.recommendation == ShoppingRecommendation.REDUCE }
        val removeCount = itemAnalyses.count { it.recommendation == ShoppingRecommendation.REMOVE }
        val potentialSavings = originalTotal - suggestedTotal

        return when {
            removeCount + reduceCount == 0 -> {
                "✨ Great list! All items look reasonable for your budget. Total: $${String.format("%.2f", originalTotal)}"
            }

            affordabilityRatio > 0.7 -> {
                "⚠️ Your shopping list is ${(affordabilityRatio * 100).roundToInt()}% of your disposable income. " +
                "Consider $reduceCount item(s) for reduction. Potential savings: $${String.format("%.2f", potentialSavings)}"
            }

            budget > 0 && originalTotal > budget -> {
                "📊 You're $${String.format("%.2f", originalTotal - budget)} over budget. " +
                "Follow our suggestions to save $${String.format("%.2f", potentialSavings)} and stay within budget."
            }

            potentialSavings > originalTotal * 0.1 -> {
                "💡 Found opportunities to optimize! You could save $${String.format("%.2f", potentialSavings)} " +
                "by adjusting $reduceCount item(s). Your essentials are covered."
            }

            else -> {
                "✅ Your list looks good! $okCount items approved, $reduceCount could be optimized. " +
                "Total: $${String.format("%.2f", originalTotal)}"
            }
        }
    }
}
