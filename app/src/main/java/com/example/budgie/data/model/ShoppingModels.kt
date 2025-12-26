package com.example.budgie.data.model

import androidx.room.*
import java.util.Date

/**
 * Shopping List Entity
 */
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listNumber: Int = 1,
    val title: String = "Shopping List",
    val totalBudget: Double = 0.0,
    val actualTotal: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

/**
 * Shopping Item Entity
 */
@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Int = 1,
    val unit: String = "pcs", // pcs, kg, liters, etc.
    val estimatedPrice: Double = 0.0,
    val actualPrice: Double? = null,
    val needPriority: Int = 3, // 1 (Essential) to 5 (Nice to have)
    val category: ShoppingCategory = ShoppingCategory.OTHER,
    val isPurchased: Boolean = false,
    val aiRecommendation: ShoppingRecommendation = ShoppingRecommendation.OK,
    val aiSuggestedQuantity: Int? = null,
    val aiReason: String? = null,
    val notes: String = ""
)

/**
 * Shopping Category
 */
enum class ShoppingCategory(val displayName: String, val icon: String) {
    GROCERIES("Groceries", "🛒"),
    HOUSEHOLD("Household", "🏠"),
    PERSONAL_CARE("Personal Care", "🧴"),
    ELECTRONICS("Electronics", "📱"),
    CLOTHING("Clothing", "👕"),
    HEALTH("Health", "💊"),
    STATIONERY("Stationery", "📝"),
    CLEANING("Cleaning", "🧹"),
    SNACKS("Snacks & Drinks", "🍿"),
    BABY("Baby Items", "👶"),
    PET("Pet Supplies", "🐕"),
    OTHER("Other", "📦")
}

/**
 * Shopping Recommendation Status
 */
enum class ShoppingRecommendation(val displayName: String, val colorHex: Long) {
    OK("Approved", 0xFF4CAF50),           // Green - Good to buy
    REDUCE("Consider Reducing", 0xFFFF9800), // Amber - Opportunity to reduce
    REMOVE("Needs Review", 0xFFF44336)     // Red - May need to remove/reduce significantly
}

/**
 * Shopping List with Items
 */
data class ShoppingListWithItems(
    @Embedded
    val shoppingList: ShoppingList,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val items: List<ShoppingItem>
) {
    val totalEstimated: Double
        get() = items.sumOf { it.estimatedPrice * it.quantity }

    val itemCount: Int
        get() = items.size

    val purchasedCount: Int
        get() = items.count { it.isPurchased }

    val essentialItems: List<ShoppingItem>
        get() = items.filter { it.needPriority <= 2 }

    val optionalItems: List<ShoppingItem>
        get() = items.filter { it.needPriority >= 4 }
}

/**
 * Shopping Summary for dashboard
 */
data class ShoppingSummary(
    val activeListCount: Int = 0,
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val itemsPurchased: Int = 0,
    val totalItems: Int = 0
)

/**
 * AI Analysis Result for Shopping List
 */
data class ShoppingAIAnalysis(
    val originalTotal: Double,
    val suggestedTotal: Double,
    val potentialSavings: Double,
    val itemAnalysis: List<ItemAnalysis>,
    val overallAdvice: String,
    val budgetHealthScore: Float // 0-1
)

data class ItemAnalysis(
    val itemId: Long,
    val recommendation: ShoppingRecommendation,
    val suggestedQuantity: Int?,
    val reason: String,
    val potentialSaving: Double
)

