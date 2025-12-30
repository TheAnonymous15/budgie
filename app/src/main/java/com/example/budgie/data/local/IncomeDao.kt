package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.IncomeSource
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    suspend fun getAllIncomesOnce(): List<Income>

    @Query("SELECT * FROM incomes WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getIncomesByDateRange(startDate: Long, endDate: Long): Flow<List<Income>>

    @Query("SELECT SUM(amount) FROM incomes WHERE date >= :startDate AND date <= :endDate")
    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM incomes WHERE isRecurring = 1")
    fun getTotalRecurringIncome(): Flow<Double?>

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getIncomeById(id: Long): Income?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income): Long

    @Update
    suspend fun updateIncome(income: Income)

    @Delete
    suspend fun deleteIncome(income: Income)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Long)

    @Query("DELETE FROM incomes")
    suspend fun deleteAll()
}

