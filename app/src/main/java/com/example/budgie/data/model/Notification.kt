package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Notification types for categorizing different kinds of alerts
 */
enum class NotificationType(val displayName: String, val icon: String) {
    // Financial Alerts
    BILL_REMINDER("Bill Reminder", "receipt"),
    BILL_OVERDUE("Bill Overdue", "warning"),
    BUDGET_WARNING("Budget Warning", "trending_up"),
    BUDGET_EXCEEDED("Budget Exceeded", "error"),

    // Goal & Savings
    GOAL_PROGRESS("Goal Progress", "flag"),
    GOAL_ACHIEVED("Goal Achieved", "celebration"),
    SAVINGS_TIP("Savings Tip", "lightbulb"),

    // Loan Related
    LOAN_PAYMENT_DUE("Loan Payment Due", "payment"),
    LOAN_COMPLETED("Loan Completed", "check_circle"),

    // Insights & Tips
    DAILY_INSIGHT("Daily Insight", "insights"),
    WEEKLY_SUMMARY("Weekly Summary", "analytics"),
    MONTHLY_REPORT("Monthly Report", "assessment"),
    SPENDING_ALERT("Spending Alert", "notification_important"),

    // System
    WELCOME("Welcome", "waving_hand"),
    BIRTHDAY("Birthday", "cake"),
    REMINDER("Reminder", "alarm"),
    GENERAL("General", "notifications")
}

/**
 * Priority levels for notifications
 */
enum class NotificationPriority(val level: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    URGENT(3)
}

/**
 * Notification entity for Room database
 */
@Entity(tableName = "notifications")
data class BudgieNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val actionRoute: String? = null,  // Navigation route for action
    val actionData: String? = null,   // Additional data for action (JSON)
    val expiresAt: Long? = null       // Optional expiration timestamp
) {
    fun isExpired(): Boolean = expiresAt?.let { System.currentTimeMillis() > it } ?: false
}

/**
 * UI model for displaying notifications with additional computed properties
 */
data class NotificationUiModel(
    val notification: BudgieNotification,
    val timeAgo: String,
    val iconResId: Int? = null
)

