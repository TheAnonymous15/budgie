package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ═══════════════════════════════════════════════════════════════════════════════
// NOTIFICATION ENUMS
// ═══════════════════════════════════════════════════════════════════════════════

enum class NotificationCategory(val displayName: String, val icon: String) {
    BILL("Bills", "receipt"),
    BUDGET("Budget", "account_balance_wallet"),
    GOAL("Goals", "flag"),
    LOAN("Loans", "credit_card"),
    EXPENSE("Expenses", "shopping_cart"),
    INCOME("Income", "attach_money"),
    SECURITY("Security", "security"),
    SYSTEM("System", "settings"),
    SHOPPING("Shopping", "shopping_bag"),
    INVESTMENT("Investments", "trending_up"),
    INSIGHT("Insights", "lightbulb")
}

enum class AppNotificationPriority(val level: Int, val displayName: String) {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High"),
    URGENT(3, "Urgent")
}

enum class NotificationStatus {
    UNREAD,
    READ,
    DISMISSED,
    ACTED_ON,
    ARCHIVED
}

// Bill Notification Types
enum class BillNotificationType(val displayName: String) {
    DUE_REMINDER("Due Reminder"),
    DUE_TODAY("Due Today"),
    OVERDUE("Overdue"),
    PAID_CONFIRMATION("Paid"),
    RECURRING_UPCOMING("Recurring Bill"),
    AMOUNT_CHANGED("Amount Changed"),
    PAYMENT_FAILED("Payment Failed")
}

// Budget Notification Types
enum class BudgetNotificationType(val displayName: String) {
    THRESHOLD_50("50% Used"),
    THRESHOLD_75("75% Used"),
    THRESHOLD_90("90% Warning"),
    EXCEEDED("Exceeded"),
    RESET("Period Reset"),
    UNDER_BUDGET("Under Budget"),
    CATEGORY_SPIKE("Category Spike"),
    NEW_BUDGET_SET("New Budget Set")
}

// Goal Notification Types
enum class GoalNotificationType(val displayName: String) {
    MILESTONE_25("25% Complete"),
    MILESTONE_50("50% Complete"),
    MILESTONE_75("75% Complete"),
    GOAL_ACHIEVED("Goal Achieved"),
    CONTRIBUTION_REMINDER("Contribution Reminder"),
    CONTRIBUTION_MADE("Contribution Made"),
    DEADLINE_APPROACHING("Deadline Approaching"),
    DEADLINE_MISSED("Deadline Missed"),
    ON_TRACK("On Track"),
    BEHIND_SCHEDULE("Behind Schedule"),
    GOAL_CREATED("Goal Created")
}

// Loan Notification Types
enum class LoanNotificationType(val displayName: String) {
    PAYMENT_DUE("Payment Due"),
    PAYMENT_DUE_TODAY("Due Today"),
    PAYMENT_OVERDUE("Overdue"),
    PAYMENT_CONFIRMED("Payment Made"),
    INTEREST_ACCRUED("Interest Added"),
    LOAN_PAID_OFF("Loan Paid Off"),
    BALANCE_UPDATE("Balance Update"),
    HIGH_INTEREST_WARNING("High Interest"),
    LOAN_CREATED("Loan Created"),
    PAYMENT_MILESTONE("Payment Milestone")
}

// Expense Notification Types
enum class ExpenseNotificationType(val displayName: String) {
    LARGE_EXPENSE("Large Expense"),
    ANOMALY_DETECTED("Anomaly Detected"),
    RECURRING_DETECTED("Recurring Detected"),
    DUPLICATE_WARNING("Duplicate Warning"),
    CATEGORY_INSIGHT("Category Insight"),
    DAILY_LIMIT_WARNING("Daily Limit"),
    EXPENSE_ADDED("Expense Added"),
    UNUSUAL_MERCHANT("Unusual Merchant")
}

// Income Notification Types
enum class IncomeNotificationType(val displayName: String) {
    INCOME_RECEIVED("Income Received"),
    SALARY_EXPECTED("Salary Expected"),
    INCOME_LOWER("Lower Than Usual"),
    INCOME_HIGHER("Higher Than Usual"),
    RECURRING_MISSING("Missing Income"),
    TAX_REMINDER("Tax Reminder"),
    INCOME_ADDED("Income Added")
}

// Security Notification Types
enum class SecurityNotificationType(val displayName: String) {
    LOGIN_SUCCESS("Login Success"),
    LOGIN_FAILED("Login Failed"),
    BIOMETRIC_ENABLED("Biometric Enabled"),
    BIOMETRIC_DISABLED("Biometric Disabled"),
    PIN_CHANGED("PIN Changed"),
    PIN_CREATED("PIN Created"),
    BACKUP_CREATED("Backup Created"),
    BACKUP_RESTORED("Backup Restored"),
    EXPORT_COMPLETED("Export Completed"),
    SUSPICIOUS_ACTIVITY("Suspicious Activity"),
    SESSION_TIMEOUT("Session Timeout"),
    SECURITY_CHECK("Security Check"),
    DATA_CLEARED("Data Cleared")
}

// System Notification Types
enum class SystemNotificationType(val displayName: String) {
    APP_UPDATE("App Update"),
    FEATURE_ANNOUNCEMENT("New Feature"),
    MAINTENANCE("Maintenance"),
    DATA_SYNC("Data Sync"),
    STORAGE_WARNING("Storage Warning"),
    PERFORMANCE_TIP("Performance Tip"),
    WEEKLY_SUMMARY("Weekly Summary"),
    MONTHLY_REPORT("Monthly Report"),
    YEAR_IN_REVIEW("Year in Review"),
    WELCOME("Welcome"),
    ONBOARDING_TIP("Onboarding Tip"),
    FEEDBACK_REQUEST("Feedback Request"),
    BIRTHDAY_GREETING("Birthday Greeting")
}

// Shopping Notification Types
enum class ShoppingNotificationType(val displayName: String) {
    ITEM_REMINDER("Item Reminder"),
    LIST_CREATED("List Created"),
    PRICE_ALERT("Price Alert"),
    ITEM_ADDED("Item Added"),
    LIST_COMPLETED("List Completed"),
    BUDGET_ESTIMATE("Budget Estimate"),
    RESTOCK_SUGGESTION("Restock Suggestion"),
    AI_RECOMMENDATION("AI Recommendation")
}

// Investment Notification Types
enum class InvestmentNotificationType(val displayName: String) {
    PORTFOLIO_UPDATE("Portfolio Update"),
    DIVIDEND_RECEIVED("Dividend Received"),
    REBALANCE_SUGGESTION("Rebalance Suggestion"),
    GOAL_ALLOCATION("Goal Allocation"),
    MARKET_INSIGHT("Market Insight"),
    CONTRIBUTION_REMINDER("Contribution Reminder"),
    PERFORMANCE_SUMMARY("Performance Summary")
}

// Insight Notification Types
enum class InsightNotificationType(val displayName: String) {
    SPENDING_PATTERN("Spending Pattern"),
    SAVINGS_OPPORTUNITY("Savings Opportunity"),
    TREND_ALERT("Trend Alert"),
    ACHIEVEMENT("Achievement"),
    TIP_OF_THE_DAY("Daily Tip"),
    COMPARISON("Comparison"),
    PREDICTION("Prediction"),
    ANOMALY("Anomaly"),
    AI_INSIGHT("AI Insight")
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN NOTIFICATION ENTITY
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: NotificationCategory,
    val title: String,
    val message: String,
    val shortMessage: String? = null,
    val priority: AppNotificationPriority = AppNotificationPriority.MEDIUM,
    val status: NotificationStatus = NotificationStatus.UNREAD,
    val actionRoute: String? = null,
    val actionData: String? = null,
    val iconType: String? = null,
    val accentColor: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null,
    val expiresAt: Long? = null,
    val isArchived: Boolean = false,
    val groupId: String? = null  // For grouping related notifications
)

// ═══════════════════════════════════════════════════════════════════════════════
// BILL NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "bill_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId"), Index("billId")]
)
data class BillNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val billId: Long,
    val type: BillNotificationType,
    val billName: String,
    val dueDate: Long,
    val amount: Double,
    val daysUntilDue: Int,
    val isRecurring: Boolean = false,
    val isPaid: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════════
// BUDGET NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "budget_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId"), Index("budgetId")]
)
data class BudgetNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val budgetId: Long,
    val type: BudgetNotificationType,
    val categoryName: String,
    val budgetLimit: Double,
    val currentSpend: Double,
    val percentageUsed: Double,
    val remainingAmount: Double
)

// ═══════════════════════════════════════════════════════════════════════════════
// GOAL NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "goal_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId"), Index("goalId")]
)
data class GoalNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val goalId: Long,
    val type: GoalNotificationType,
    val goalName: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val percentageComplete: Double,
    val daysRemaining: Int? = null,
    val targetDate: Long? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// LOAN NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "loan_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId"), Index("loanId")]
)
data class LoanNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val loanId: Long,
    val type: LoanNotificationType,
    val loanName: String,
    val lenderName: String? = null,
    val paymentAmount: Double? = null,
    val remainingBalance: Double,
    val totalPaid: Double = 0.0,
    val nextPaymentDate: Long? = null,
    val daysUntilPayment: Int? = null,
    val interestRate: Double? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// EXPENSE NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "expense_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId"), Index("expenseId")]
)
data class ExpenseNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val expenseId: Long? = null,
    val type: ExpenseNotificationType,
    val amount: Double,
    val categoryName: String? = null,
    val merchantName: String? = null,
    val description: String? = null,
    val averageForCategory: Double? = null,
    val deviationPercent: Double? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// INCOME NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "income_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId"), Index("incomeId")]
)
data class IncomeNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val incomeId: Long? = null,
    val type: IncomeNotificationType,
    val amount: Double? = null,
    val sourceName: String? = null,
    val expectedDate: Long? = null,
    val variance: Double? = null,
    val isRecurring: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════════
// SECURITY NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "security_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId")]
)
data class SecurityNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val type: SecurityNotificationType,
    val deviceInfo: String? = null,
    val ipAddress: String? = null,
    val location: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val actionRequired: Boolean = false,
    val securityLevel: String = "INFO",  // INFO, WARNING, CRITICAL
    val attemptCount: Int? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// SYSTEM NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "system_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId")]
)
data class SystemNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val type: SystemNotificationType,
    val version: String? = null,
    val featureName: String? = null,
    val actionUrl: String? = null,
    val metadata: String? = null,
    val summaryData: String? = null  // JSON for summary reports
)

// ═══════════════════════════════════════════════════════════════════════════════
// SHOPPING NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "shopping_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId"), Index("listId")]
)
data class ShoppingNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val listId: Long? = null,
    val itemId: Long? = null,
    val type: ShoppingNotificationType,
    val listName: String? = null,
    val itemName: String? = null,
    val itemCount: Int? = null,
    val estimatedTotal: Double? = null,
    val aiRecommendation: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// INVESTMENT NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "investment_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId")]
)
data class InvestmentNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val type: InvestmentNotificationType,
    val portfolioValue: Double? = null,
    val changeAmount: Double? = null,
    val changePercent: Double? = null,
    val assetName: String? = null,
    val recommendation: String? = null,
    val dividendAmount: Double? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// INSIGHT NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

@Entity(
    tableName = "insight_notifications",
    foreignKeys = [
        ForeignKey(
            entity = AppNotification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId")]
)
data class InsightNotificationDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationId: Long,
    val type: InsightNotificationType,
    val insightTitle: String,
    val insightDetail: String,
    val metric: String? = null,
    val value: Double? = null,
    val previousValue: Double? = null,
    val trend: String? = null,  // UP, DOWN, STABLE
    val confidence: Double? = null,
    val category: String? = null,
    val timeframe: String? = null  // DAILY, WEEKLY, MONTHLY
)

// ═══════════════════════════════════════════════════════════════════════════════
// UI HELPER MODELS
// ═══════════════════════════════════════════════════════════════════════════════

data class NotificationWithDetails(
    val notification: AppNotification,
    val billDetail: BillNotificationDetail? = null,
    val budgetDetail: BudgetNotificationDetail? = null,
    val goalDetail: GoalNotificationDetail? = null,
    val loanDetail: LoanNotificationDetail? = null,
    val expenseDetail: ExpenseNotificationDetail? = null,
    val incomeDetail: IncomeNotificationDetail? = null,
    val securityDetail: SecurityNotificationDetail? = null,
    val systemDetail: SystemNotificationDetail? = null,
    val shoppingDetail: ShoppingNotificationDetail? = null,
    val investmentDetail: InvestmentNotificationDetail? = null,
    val insightDetail: InsightNotificationDetail? = null
)

data class NotificationGroup(
    val date: String,
    val notifications: List<AppNotification>
)

data class CategoryNotificationCount(
    val category: NotificationCategory,
    val count: Int
)

