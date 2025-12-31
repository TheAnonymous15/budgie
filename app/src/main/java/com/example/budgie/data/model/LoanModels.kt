package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Loan Model
 */
@Entity(tableName = "loans")
@Serializable
data class Loan(
    @PrimaryKey
    val id: String = BudgieIdGenerator.generateLoanId(),
    val title: String,
    val lenderName: String, // Bank, SACCO, Friend, etc.
    val loanType: LoanType,
    val principalAmount: Double,
    val interestRate: Double, // Annual interest rate in percentage
    val interestType: InterestType = InterestType.REDUCING_BALANCE,
    val totalAmount: Double, // Principal + Total Interest
    val amountPaid: Double = 0.0,
    val monthlyPayment: Double,
    val startDate: Long,
    val endDate: Long,
    val nextPaymentDate: Long,
    val paymentFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val linkedGoalId: String? = null, // If loan is for a goal (now String)
    val isGoalLoan: Boolean = false, // Whether this is a loan from goals
    val isLoanActive: Boolean = true, // Whether the goal loan is activated (started)
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val accountNumber: String = "", // Loan account number
    val collateral: String = "", // Collateral if any
    val fees: Double = 0.0 // Loan processing fees
) {
    val remainingAmount: Double
        get() = (totalAmount - amountPaid).coerceAtLeast(0.0)

    val progressPercentage: Double
        get() = if (totalAmount > 0) (amountPaid / totalAmount * 100).coerceIn(0.0, 100.0) else 0.0

    val remainingPayments: Int
        get() {
            if (monthlyPayment <= 0) return 0
            return (remainingAmount / monthlyPayment).toInt()
        }

    val isOverdue: Boolean
        get() = status == LoanStatus.ACTIVE && System.currentTimeMillis() > nextPaymentDate

    val totalInterest: Double
        get() = totalAmount - principalAmount
}

enum class LoanType(val displayName: String) {
    PERSONAL("Personal Loan"),
    MORTGAGE("Mortgage/Home Loan"),
    AUTO("Auto/Vehicle Loan"),
    EDUCATION("Education Loan"),
    BUSINESS("Business Loan"),
    CREDIT_CARD("Credit Card"),
    SACCO("SACCO Loan"),
    MOBILE("Mobile Loan"),
    FAMILY_FRIEND("Family/Friend Loan"),
    EMERGENCY("Emergency Loan"),
    OTHER("Other")
}

enum class InterestType(val displayName: String) {
    FLAT_RATE("Flat Rate"),
    REDUCING_BALANCE("Reducing Balance"),
    NO_INTEREST("No Interest")
}

enum class PaymentFrequency(val displayName: String, val daysInterval: Int) {
    WEEKLY("Weekly", 7),
    BIWEEKLY("Bi-Weekly", 14),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90)
}

enum class LoanStatus(val displayName: String) {
    ACTIVE("Active"),
    PAID_OFF("Paid Off"),
    DEFAULTED("Defaulted"),
    RESTRUCTURED("Restructured"),
    PENDING("Pending Disbursement")
}

/**
 * Loan Payment/Transaction Record
 */
@Entity(tableName = "loan_payments")
@Serializable
data class LoanPayment(
    @PrimaryKey
    val id: String = BudgieIdGenerator.generateLoanPaymentId(),
    val loanId: String, // Now String to match Loan.id
    val amount: Double,
    val principalPortion: Double = 0.0, // Portion going to principal
    val interestPortion: Double = 0.0, // Portion going to interest
    val paymentDate: Long = System.currentTimeMillis(),
    val dueDate: Long,
    val paymentMethod: String = "",
    val referenceNumber: String = "",
    val status: PaymentStatus = PaymentStatus.COMPLETED,
    val notes: String = "",
    val balanceAfterPayment: Double = 0.0
)

enum class PaymentStatus(val displayName: String) {
    COMPLETED("Completed"),
    PENDING("Pending"),
    FAILED("Failed"),
    REVERSED("Reversed")
}

/**
 * Loan Summary for Dashboard
 */
data class LoanSummary(
    val totalLoans: Int = 0,
    val activeLoans: Int = 0,
    val totalBorrowed: Double = 0.0,
    val totalRepaid: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val overdueLoans: Int = 0,
    val nextPaymentAmount: Double = 0.0,
    val nextPaymentDate: Long? = null
)

