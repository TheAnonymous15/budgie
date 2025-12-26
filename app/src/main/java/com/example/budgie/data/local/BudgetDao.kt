package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.Budget
import com.example.budgie.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsByMonth(month: Int, year: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE category = :category AND month = :month AND year = :year")
    suspend fun getBudgetByCategoryAndMonth(category: ExpenseCategory, month: Int, year: Int): Budget?

    @Query("SELECT SUM(`limit`) FROM budgets WHERE month = :month AND year = :year")
    fun getTotalBudgetForMonth(month: Int, year: Int): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)
}

