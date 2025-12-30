package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.ShoppingList
import com.example.budgie.data.model.ShoppingItem
import com.example.budgie.data.model.ShoppingListWithItems
import com.example.budgie.data.model.ShoppingRecommendation
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    // ==================== Shopping List Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(list: ShoppingList): Long

    @Update
    suspend fun updateShoppingList(list: ShoppingList)

    @Delete
    suspend fun deleteShoppingList(list: ShoppingList)

    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun getAllShoppingLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveShoppingLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getShoppingListById(listId: Long): ShoppingList?

    @Query("SELECT MAX(listNumber) FROM shopping_lists")
    suspend fun getMaxListNumber(): Int?

    @Transaction
    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun getAllShoppingListsWithItems(): Flow<List<ShoppingListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveShoppingListsWithItems(): Flow<List<ShoppingListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getShoppingListWithItems(listId: Long): ShoppingListWithItems?

    @Query("UPDATE shopping_lists SET isCompleted = 1, completedAt = :completedAt WHERE id = :listId")
    suspend fun markListCompleted(listId: Long, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_lists SET actualTotal = :total WHERE id = :listId")
    suspend fun updateListTotal(listId: Long, total: Double)

    // ==================== Shopping Item Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingItem>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY needPriority ASC, name ASC")
    fun getItemsByListId(listId: Long): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE id = :itemId")
    suspend fun getShoppingItemById(itemId: Long): ShoppingItem?

    @Query("UPDATE shopping_items SET isPurchased = :isPurchased, actualPrice = :actualPrice WHERE id = :itemId")
    suspend fun markItemPurchased(itemId: Long, isPurchased: Boolean, actualPrice: Double?)

    @Query("UPDATE shopping_items SET aiRecommendation = :recommendation, aiSuggestedQuantity = :suggestedQty, aiReason = :reason WHERE id = :itemId")
    suspend fun updateItemAIAnalysis(itemId: Long, recommendation: ShoppingRecommendation, suggestedQty: Int?, reason: String?)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: Long)

    // ==================== Summary Queries ====================

    @Query("SELECT COUNT(*) FROM shopping_lists WHERE isCompleted = 0")
    fun getActiveListCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalBudget), 0) FROM shopping_lists WHERE isCompleted = 0")
    fun getTotalBudgeted(): Flow<Double>

    @Query("SELECT COALESCE(SUM(actualTotal), 0) FROM shopping_lists WHERE isCompleted = 1")
    fun getTotalSpent(): Flow<Double>

    @Query("""
        SELECT COUNT(*) FROM shopping_items si
        INNER JOIN shopping_lists sl ON si.listId = sl.id
        WHERE sl.isCompleted = 0 AND si.isPurchased = 1
    """)
    fun getItemsPurchasedCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM shopping_items si
        INNER JOIN shopping_lists sl ON si.listId = sl.id
        WHERE sl.isCompleted = 0
    """)
    fun getTotalItemsCount(): Flow<Int>

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllItems()
}

