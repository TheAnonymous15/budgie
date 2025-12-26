package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startDate AND date <= :endDate")
    fun getTotalExpensesByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category AND date >= :startDate AND date <= :endDate")
    fun getTotalExpensesByCategoryAndDateRange(category: ExpenseCategory, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE date >= :startDate AND date <= :endDate GROUP BY category")
    fun getExpenseSumByCategory(startDate: Long, endDate: Long): Flow<List<CategorySum>>
}

data class CategorySum(
    val category: ExpenseCategory,
    val total: Double
)

