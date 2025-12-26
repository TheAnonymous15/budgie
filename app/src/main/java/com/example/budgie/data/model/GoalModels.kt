package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Financial Goal Model
 */
@Entity(tableName = "goals")
@Serializable
data class FinancialGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val goalType: GoalType,
    val fundingMethod: FundingMethod,
    val savingFrequency: SavingFrequency? = null, // Only for SAVING funding method
    val savingAmount: Double? = null, // Amount to save per period (user input or calculated)
    val calculatedSavingAmount: Double? = null, // Auto-calculated amount per period
    val isVariableSaving: Boolean = false, // Variable or fixed saving
    val loanId: Long? = null, // Link to loan if funding via loan
    val savingStartDate: Long = System.currentTimeMillis(), // When to start saving
    val isSavingStarted: Boolean = false, // Whether saving has started
    val startDate: Long = System.currentTimeMillis(),
    val targetDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val category: GoalCategory = GoalCategory.OTHER,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val notes: String = ""
) {
    val progressPercentage: Double
        get() = if (targetAmount > 0) (currentAmount / targetAmount * 100).coerceIn(0.0, 100.0) else 0.0

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val isOnTrack: Boolean
        get() {
            if (isCompleted) return true
            val now = System.currentTimeMillis()
            val totalDuration = targetDate - startDate
            val elapsed = now - startDate
            val expectedProgress = if (totalDuration > 0) (elapsed.toDouble() / totalDuration) * 100 else 0.0
            return progressPercentage >= expectedProgress - 10 // 10% buffer
        }
}

enum class GoalType(val displayName: String, val durationMonths: IntRange) {
    SHORT_TERM("Short Term", 1..6),
    MEDIUM_TERM("Medium Term", 7..24),
    LONG_TERM("Long Term", 25..120)
}

enum class FundingMethod(val displayName: String) {
    SAVING("Save Up"),
    LOAN("Take a Loan"),
    MIXED("Save + Loan")
}

enum class SavingFrequency(val displayName: String, val periodDays: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    BIWEEKLY("Bi-Weekly", 14),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    ANNUALLY("Annually", 365)
}

enum class GoalCategory(val displayName: String, val emoji: String) {
    EMERGENCY_FUND("Emergency Fund", "🛡️"),
    HOME("Home/Property", "🏠"),
    VEHICLE("Vehicle", "🚗"),
    EDUCATION("Education", "📚"),
    TRAVEL("Travel", "✈️"),
    WEDDING("Wedding", "💒"),
    RETIREMENT("Retirement", "🏖️"),
    INVESTMENT("Investment", "📈"),
    GADGETS("Electronics/Gadgets", "📱"),
    HEALTH("Health/Medical", "🏥"),
    BUSINESS("Business", "💼"),
    DEBT_PAYOFF("Debt Payoff", "💳"),
    OTHER("Other", "🎯")
}

enum class GoalPriority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}

/**
 * Goal Contribution/Saving Record
 */
@Entity(tableName = "goal_contributions")
@Serializable
data class GoalContribution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isAutomatic: Boolean = false // From scheduled saving
)

/**
 * Goals Summary for Dashboard
 */
data class GoalsSummary(
    val activeGoals: Int = 0,
    val totalTargetAmount: Double = 0.0,
    val totalCurrentAmount: Double = 0.0,
    val overallProgress: Double = 0.0
)

