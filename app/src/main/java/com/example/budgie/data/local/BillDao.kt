package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.Bill
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    suspend fun getAllBillsOnce(): List<Bill>

    @Query("SELECT * FROM bills WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE isPaid = 0 ORDER BY dueDate ASC")
    suspend fun getUnpaidBillsOnce(): List<Bill>

    @Query("SELECT * FROM bills WHERE isPaid = 1 ORDER BY dueDate DESC LIMIT 20")
    fun getPaidBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC")
    fun getBillsByDateRange(startDate: Long, endDate: Long): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE isPaid = 0 AND dueDate <= :date ORDER BY dueDate ASC")
    fun getOverdueBills(date: Long): Flow<List<Bill>>

    @Query("SELECT SUM(amount) FROM bills WHERE isPaid = 0")
    fun getTotalUnpaidBills(): Flow<Double?>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): Bill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("UPDATE bills SET isPaid = :isPaid WHERE id = :id")
    suspend fun updateBillPaidStatus(id: Long, isPaid: Boolean)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()
}

