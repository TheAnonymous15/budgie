package com.example.budgie.data.repository

import com.example.budgie.data.local.BillDao
import com.example.budgie.data.local.BudgetDao
import com.example.budgie.data.local.CategorySum
import com.example.budgie.data.local.ExpenseDao
import com.example.budgie.data.local.IncomeDao
import com.example.budgie.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.*

class FinanceRepository(
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao
) {
    // Expense operations
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)

    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)

    fun getTotalExpensesByDateRange(startDate: Long, endDate: Long): Flow<Double> =
        expenseDao.getTotalExpensesByDateRange(startDate, endDate).map { it ?: 0.0 }

    fun getExpenseSumByCategory(startDate: Long, endDate: Long): Flow<List<CategorySum>> =
        expenseDao.getExpenseSumByCategory(startDate, endDate)

    suspend fun addExpense(expense: Expense) = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // Income operations
    fun getAllIncomes(): Flow<List<Income>> = incomeDao.getAllIncomes()

    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double> =
        incomeDao.getTotalIncomeByDateRange(startDate, endDate).map { it ?: 0.0 }

    fun getTotalRecurringIncome(): Flow<Double> =
        incomeDao.getTotalRecurringIncome().map { it ?: 0.0 }

    suspend fun addIncome(income: Income) = incomeDao.insertIncome(income)

    suspend fun updateIncome(income: Income) = incomeDao.updateIncome(income)

    suspend fun deleteIncome(income: Income) = incomeDao.deleteIncome(income)

    // Bill operations
    fun getAllBills(): Flow<List<Bill>> = billDao.getAllBills()

    fun getUnpaidBills(): Flow<List<Bill>> = billDao.getUnpaidBills()

    fun getPaidBills(): Flow<List<Bill>> = billDao.getPaidBills()

    fun getOverdueBills(): Flow<List<Bill>> = billDao.getOverdueBills(System.currentTimeMillis())

    fun getTotalUnpaidBills(): Flow<Double> = billDao.getTotalUnpaidBills().map { it ?: 0.0 }

    suspend fun addBill(bill: Bill) = billDao.insertBill(bill)

    suspend fun updateBill(bill: Bill) = billDao.updateBill(bill)

    suspend fun deleteBill(bill: Bill) = billDao.deleteBill(bill)

    suspend fun deleteBillById(billId: String) {
        billDao.getBillById(billId)?.let { billDao.deleteBill(it) }
    }

    suspend fun getBillById(billId: String): Bill? = billDao.getBillById(billId)

    suspend fun markBillAsPaid(billId: String, isPaid: Boolean) =
        billDao.updateBillPaidStatus(billId, isPaid)

    // Budget operations
    fun getBudgetsByMonth(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsByMonth(month, year)

    suspend fun addBudget(budget: Budget) = budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    // Financial Summary
    fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> {
        return combine(
            getTotalIncomeByDateRange(startDate, endDate),
            getTotalExpensesByDateRange(startDate, endDate),
            getExpenseSumByCategory(startDate, endDate)
        ) { totalIncome, totalExpenses, categoryExpenses ->
            val netSavings = totalIncome - totalExpenses
            val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) * 100 else 0.0

            val expensesByCategory = categoryExpenses.associate { it.category to it.total }

            FinancialSummary(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netSavings = netSavings,
                savingsRate = savingsRate,
                expensesByCategory = expensesByCategory,
                monthlyAvgExpense = totalExpenses,
                projectedYearlyExpense = totalExpenses * 12
            )
        }
    }

    // Helper functions
    companion object {
        fun getMonthStartEnd(month: Int, year: Int): Pair<Long, Long> {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val end = calendar.timeInMillis

            return Pair(start, end)
        }
    }
}

