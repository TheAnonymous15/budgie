package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY targetDate ASC")
    fun getAllGoals(): Flow<List<FinancialGoal>>

    @Query("SELECT * FROM goals WHERE isCompleted = 0 ORDER BY targetDate ASC")
    fun getActiveGoals(): Flow<List<FinancialGoal>>

    @Query("SELECT * FROM goals WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedGoals(): Flow<List<FinancialGoal>>

    @Query("SELECT * FROM goals WHERE goalType = :type ORDER BY targetDate ASC")
    fun getGoalsByType(type: GoalType): Flow<List<FinancialGoal>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: String): FinancialGoal?

    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun getGoalByIdFlow(goalId: String): Flow<FinancialGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoal)

    @Update
    suspend fun updateGoal(goal: FinancialGoal)

    @Delete
    suspend fun deleteGoal(goal: FinancialGoal)

    @Query("UPDATE goals SET currentAmount = currentAmount + :amount WHERE id = :goalId")
    suspend fun addContribution(goalId: String, amount: Double)

    @Query("UPDATE goals SET isCompleted = 1, completedAt = :completedAt WHERE id = :goalId")
    suspend fun markGoalCompleted(goalId: String, completedAt: Long = System.currentTimeMillis())

    // Goal Contributions
    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    fun getContributionsByGoal(goalId: String): Flow<List<GoalContribution>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: GoalContribution)

    @Delete
    suspend fun deleteContribution(contribution: GoalContribution)

    @Query("SELECT SUM(amount) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun getTotalContributions(goalId: String): Double?

    // Summary queries
    @Query("SELECT COUNT(*) FROM goals WHERE isCompleted = 0")
    fun getActiveGoalsCount(): Flow<Int>

    @Query("SELECT SUM(targetAmount) FROM goals WHERE isCompleted = 0")
    fun getTotalTargetAmount(): Flow<Double?>

    @Query("SELECT SUM(currentAmount) FROM goals WHERE isCompleted = 0")
    fun getTotalCurrentAmount(): Flow<Double?>

    @Query("DELETE FROM goals")
    suspend fun deleteAll()

    @Query("DELETE FROM goal_contributions")
    suspend fun deleteAllContributions()
}

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY nextPaymentDate ASC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY nextPaymentDate ASC")
    fun getLoansByStatus(status: LoanStatus): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' ORDER BY nextPaymentDate ASC")
    fun getActiveLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: String): Loan?

    @Query("SELECT * FROM loans WHERE id = :loanId")
    fun getLoanByIdFlow(loanId: String): Flow<Loan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("UPDATE loans SET amountPaid = amountPaid + :amount WHERE id = :loanId")
    suspend fun addPayment(loanId: String, amount: Double)

    @Query("UPDATE loans SET status = :status WHERE id = :loanId")
    suspend fun updateLoanStatus(loanId: String, status: LoanStatus)

    @Query("UPDATE loans SET nextPaymentDate = :nextDate WHERE id = :loanId")
    suspend fun updateNextPaymentDate(loanId: String, nextDate: Long)

    // Loan Payments
    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    fun getPaymentsByLoan(loanId: String): Flow<List<LoanPayment>>

    @Query("SELECT * FROM loan_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<LoanPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: LoanPayment)

    @Delete
    suspend fun deletePayment(payment: LoanPayment)

    // Summary queries
    @Query("SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'")
    fun getActiveLoansCount(): Flow<Int>

    @Query("SELECT SUM(totalAmount) FROM loans")
    fun getTotalBorrowed(): Flow<Double?>

    @Query("SELECT SUM(amountPaid) FROM loans")
    fun getTotalRepaid(): Flow<Double?>

    @Query("SELECT SUM(totalAmount - amountPaid) FROM loans WHERE status = 'ACTIVE'")
    fun getTotalRemaining(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE' AND nextPaymentDate < :currentTime")
    fun getOverdueLoansCount(currentTime: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' ORDER BY nextPaymentDate ASC LIMIT 1")
    fun getNextDueLoan(): Flow<Loan?>

    @Query("DELETE FROM loans")
    suspend fun deleteAll()

    @Query("DELETE FROM loan_payments")
    suspend fun deleteAllPayments()
}

