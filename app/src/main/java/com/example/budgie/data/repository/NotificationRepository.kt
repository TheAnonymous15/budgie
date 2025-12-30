package com.example.budgie.data.repository

import android.content.Context
import com.example.budgie.data.local.AppNotificationDao
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing all notification operations
 */
class NotificationRepository private constructor(context: Context) {

    private val database = BudgieDatabase.getDatabase(context)
    private val notificationDao: AppNotificationDao = database.appNotificationDao()

    companion object {
        @Volatile
        private var INSTANCE: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    fun getAllActiveNotifications(): Flow<List<AppNotification>> =
        notificationDao.getAllActiveNotifications()

    suspend fun getAllActiveNotificationsOnce(): List<AppNotification> =
        notificationDao.getAllActiveNotificationsOnce()

    fun getUnreadNotifications(): Flow<List<AppNotification>> =
        notificationDao.getUnreadNotifications()

    suspend fun getUnreadNotificationsOnce(): List<AppNotification> =
        notificationDao.getUnreadNotificationsOnce()

    fun getUnreadCount(): Flow<Int> =
        notificationDao.getUnreadCount()

    suspend fun getUnreadCountOnce(): Int =
        notificationDao.getUnreadCountOnce()

    fun getNotificationsByCategory(category: NotificationCategory): Flow<List<AppNotification>> =
        notificationDao.getNotificationsByCategory(category)

    fun getHighPriorityNotifications(): Flow<List<AppNotification>> =
        notificationDao.getHighPriorityNotifications()

    fun getUnreadCountByAllCategories(): Flow<List<CategoryNotificationCount>> =
        notificationDao.getUnreadCountByAllCategories()

    fun getRecentNotifications(since: Long): Flow<List<AppNotification>> =
        notificationDao.getRecentNotifications(since)

    // ═══════════════════════════════════════════════════════════════════════════════
    // WRITE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun markAsRead(id: Long) =
        notificationDao.markAsRead(id)

    suspend fun markAllAsRead() =
        notificationDao.markAllAsRead()

    suspend fun markCategoryAsRead(category: NotificationCategory) =
        notificationDao.markCategoryAsRead(category)

    suspend fun dismissNotification(id: Long) =
        notificationDao.dismissNotification(id)

    suspend fun archiveNotification(id: Long) =
        notificationDao.archiveNotification(id)

    suspend fun deleteNotification(id: Long) =
        notificationDao.deleteById(id)

    suspend fun deleteAllRead() =
        notificationDao.deleteAllRead()

    suspend fun deleteExpiredNotifications() =
        notificationDao.deleteExpiredNotifications()

    suspend fun deleteAll() =
        notificationDao.deleteAll()

    // ═══════════════════════════════════════════════════════════════════════════════
    // QUICK NOTIFICATION CREATION (for testing and simple notifications)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Create a simple notification without detailed tracking
     * Useful for test notifications and quick alerts
     */
    suspend fun createQuickNotification(
        title: String,
        message: String,
        category: NotificationCategory = NotificationCategory.SYSTEM,
        priority: AppNotificationPriority = AppNotificationPriority.MEDIUM,
        actionRoute: String? = null,
        iconType: String = "notifications",
        accentColor: String = "#0FAE96"
    ): Long {
        val notification = AppNotification(
            category = category,
            title = title,
            message = message,
            priority = priority,
            actionRoute = actionRoute,
            iconType = iconType,
            accentColor = accentColor
        )
        return notificationDao.insertNotification(notification)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE BILL NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createBillNotification(
        billId: Long,
        type: BillNotificationType,
        billName: String,
        dueDate: Long,
        amount: Double,
        daysUntilDue: Int,
        isRecurring: Boolean = false,
        isPaid: Boolean = false
    ): Long {
        val (title, message, priority) = when (type) {
            BillNotificationType.DUE_REMINDER -> Triple(
                "📋 Bill Due Soon",
                "$billName is due in $daysUntilDue days. Amount: $${"%.2f".format(amount)}",
                if (daysUntilDue <= 3) AppNotificationPriority.HIGH else AppNotificationPriority.MEDIUM
            )
            BillNotificationType.DUE_TODAY -> Triple(
                "⚠️ Bill Due Today",
                "$billName is due today! Amount: $${"%.2f".format(amount)}",
                AppNotificationPriority.URGENT
            )
            BillNotificationType.OVERDUE -> Triple(
                "🚨 Bill Overdue",
                "$billName is ${-daysUntilDue} days overdue. Amount: $${"%.2f".format(amount)}",
                AppNotificationPriority.URGENT
            )
            BillNotificationType.PAID_CONFIRMATION -> Triple(
                "✅ Bill Paid",
                "$billName has been marked as paid. Amount: $${"%.2f".format(amount)}",
                AppNotificationPriority.LOW
            )
            BillNotificationType.RECURRING_UPCOMING -> Triple(
                "🔄 Recurring Bill",
                "Your recurring bill $billName is coming up in $daysUntilDue days",
                AppNotificationPriority.MEDIUM
            )
            BillNotificationType.AMOUNT_CHANGED -> Triple(
                "💰 Bill Amount Changed",
                "$billName amount has changed to $${"%.2f".format(amount)}",
                AppNotificationPriority.MEDIUM
            )
            BillNotificationType.PAYMENT_FAILED -> Triple(
                "❌ Payment Failed",
                "Payment for $billName failed. Please check your payment method",
                AppNotificationPriority.HIGH
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.BILL,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "bills",
            iconType = "receipt",
            accentColor = if (type == BillNotificationType.OVERDUE) "#E57373" else "#FFB74D"
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = BillNotificationDetail(
            notificationId = notificationId,
            billId = billId,
            type = type,
            billName = billName,
            dueDate = dueDate,
            amount = amount,
            daysUntilDue = daysUntilDue,
            isRecurring = isRecurring,
            isPaid = isPaid
        )
        notificationDao.insertBillNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE BUDGET NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createBudgetNotification(
        budgetId: Long,
        type: BudgetNotificationType,
        categoryName: String,
        budgetLimit: Double,
        currentSpend: Double
    ): Long {
        val percentageUsed = (currentSpend / budgetLimit) * 100
        val remainingAmount = budgetLimit - currentSpend

        val (title, message, priority) = when (type) {
            BudgetNotificationType.THRESHOLD_50 -> Triple(
                "📊 Budget 50% Used",
                "You've used half of your $categoryName budget. $${"%.2f".format(remainingAmount)} remaining",
                AppNotificationPriority.LOW
            )
            BudgetNotificationType.THRESHOLD_75 -> Triple(
                "⚠️ Budget 75% Used",
                "You've used 75% of your $categoryName budget. $${"%.2f".format(remainingAmount)} remaining",
                AppNotificationPriority.MEDIUM
            )
            BudgetNotificationType.THRESHOLD_90 -> Triple(
                "🔴 Budget Almost Full",
                "You've used 90% of your $categoryName budget! Only $${"%.2f".format(remainingAmount)} left",
                AppNotificationPriority.HIGH
            )
            BudgetNotificationType.EXCEEDED -> Triple(
                "🚨 Budget Exceeded",
                "You've exceeded your $categoryName budget by $${"%.2f".format(-remainingAmount)}",
                AppNotificationPriority.URGENT
            )
            BudgetNotificationType.RESET -> Triple(
                "🔄 Budget Reset",
                "Your $categoryName budget has been reset for the new period",
                AppNotificationPriority.LOW
            )
            BudgetNotificationType.UNDER_BUDGET -> Triple(
                "🎉 Under Budget!",
                "Great job! You stayed under your $categoryName budget. Saved $${"%.2f".format(remainingAmount)}",
                AppNotificationPriority.LOW
            )
            BudgetNotificationType.CATEGORY_SPIKE -> Triple(
                "📈 Spending Spike",
                "Unusual spending detected in $categoryName category",
                AppNotificationPriority.MEDIUM
            )
            BudgetNotificationType.NEW_BUDGET_SET -> Triple(
                "✅ Budget Set",
                "Your $categoryName budget has been set to $${"%.2f".format(budgetLimit)}",
                AppNotificationPriority.LOW
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.BUDGET,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "budget",
            iconType = "account_balance_wallet",
            accentColor = when {
                percentageUsed >= 100 -> "#E57373"
                percentageUsed >= 90 -> "#FFB74D"
                else -> "#0FAE96"
            }
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = BudgetNotificationDetail(
            notificationId = notificationId,
            budgetId = budgetId,
            type = type,
            categoryName = categoryName,
            budgetLimit = budgetLimit,
            currentSpend = currentSpend,
            percentageUsed = percentageUsed,
            remainingAmount = remainingAmount
        )
        notificationDao.insertBudgetNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE GOAL NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createGoalNotification(
        goalId: Long,
        type: GoalNotificationType,
        goalName: String,
        targetAmount: Double,
        currentAmount: Double,
        daysRemaining: Int? = null,
        targetDate: Long? = null
    ): Long {
        val percentageComplete = (currentAmount / targetAmount) * 100

        val (title, message, priority) = when (type) {
            GoalNotificationType.MILESTONE_25 -> Triple(
                "🎯 25% Milestone!",
                "You're 25% of the way to your $goalName goal!",
                AppNotificationPriority.LOW
            )
            GoalNotificationType.MILESTONE_50 -> Triple(
                "🎯 Halfway There!",
                "Amazing! You've reached 50% of your $goalName goal!",
                AppNotificationPriority.MEDIUM
            )
            GoalNotificationType.MILESTONE_75 -> Triple(
                "🎯 75% Complete!",
                "Almost there! You're 75% of the way to $goalName!",
                AppNotificationPriority.MEDIUM
            )
            GoalNotificationType.GOAL_ACHIEVED -> Triple(
                "🎉 Goal Achieved!",
                "Congratulations! You've reached your $goalName goal of $${"%.2f".format(targetAmount)}!",
                AppNotificationPriority.HIGH
            )
            GoalNotificationType.CONTRIBUTION_REMINDER -> Triple(
                "💰 Contribution Reminder",
                "Time to make a contribution to your $goalName goal",
                AppNotificationPriority.MEDIUM
            )
            GoalNotificationType.CONTRIBUTION_MADE -> Triple(
                "✅ Contribution Added",
                "Contribution added to $goalName. Now at ${"%.1f".format(percentageComplete)}%",
                AppNotificationPriority.LOW
            )
            GoalNotificationType.DEADLINE_APPROACHING -> Triple(
                "⏰ Deadline Approaching",
                "$goalName deadline is in $daysRemaining days. ${"%.1f".format(percentageComplete)}% complete",
                AppNotificationPriority.HIGH
            )
            GoalNotificationType.DEADLINE_MISSED -> Triple(
                "⚠️ Deadline Missed",
                "Your $goalName goal deadline has passed. ${"%.1f".format(percentageComplete)}% achieved",
                AppNotificationPriority.MEDIUM
            )
            GoalNotificationType.ON_TRACK -> Triple(
                "✅ On Track",
                "You're on track to reach your $goalName goal!",
                AppNotificationPriority.LOW
            )
            GoalNotificationType.BEHIND_SCHEDULE -> Triple(
                "📉 Behind Schedule",
                "You're behind on your $goalName goal. Consider increasing contributions",
                AppNotificationPriority.MEDIUM
            )
            GoalNotificationType.GOAL_CREATED -> Triple(
                "🎯 New Goal Created",
                "Goal '$goalName' created with target $${"%.2f".format(targetAmount)}",
                AppNotificationPriority.LOW
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.GOAL,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "goals",
            iconType = "flag",
            accentColor = if (type == GoalNotificationType.GOAL_ACHIEVED) "#4CAF50" else "#0FAE96"
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = GoalNotificationDetail(
            notificationId = notificationId,
            goalId = goalId,
            type = type,
            goalName = goalName,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            percentageComplete = percentageComplete,
            daysRemaining = daysRemaining,
            targetDate = targetDate
        )
        notificationDao.insertGoalNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE LOAN NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createLoanNotification(
        loanId: Long,
        type: LoanNotificationType,
        loanName: String,
        remainingBalance: Double,
        lenderName: String? = null,
        paymentAmount: Double? = null,
        nextPaymentDate: Long? = null,
        daysUntilPayment: Int? = null,
        totalPaid: Double = 0.0,
        interestRate: Double? = null
    ): Long {
        val (title, message, priority) = when (type) {
            LoanNotificationType.PAYMENT_DUE -> Triple(
                "💳 Loan Payment Due",
                "$loanName payment of $${"%.2f".format(paymentAmount ?: 0.0)} due in $daysUntilPayment days",
                if ((daysUntilPayment ?: 0) <= 3) AppNotificationPriority.HIGH else AppNotificationPriority.MEDIUM
            )
            LoanNotificationType.PAYMENT_DUE_TODAY -> Triple(
                "⚠️ Payment Due Today",
                "$loanName payment of $${"%.2f".format(paymentAmount ?: 0.0)} is due today!",
                AppNotificationPriority.URGENT
            )
            LoanNotificationType.PAYMENT_OVERDUE -> Triple(
                "🚨 Payment Overdue",
                "$loanName payment is overdue by ${-(daysUntilPayment ?: 0)} days",
                AppNotificationPriority.URGENT
            )
            LoanNotificationType.PAYMENT_CONFIRMED -> Triple(
                "✅ Payment Made",
                "Payment of $${"%.2f".format(paymentAmount ?: 0.0)} received for $loanName",
                AppNotificationPriority.LOW
            )
            LoanNotificationType.INTEREST_ACCRUED -> Triple(
                "💰 Interest Added",
                "Interest has been added to $loanName. New balance: $${"%.2f".format(remainingBalance)}",
                AppNotificationPriority.LOW
            )
            LoanNotificationType.LOAN_PAID_OFF -> Triple(
                "🎉 Loan Paid Off!",
                "Congratulations! $loanName has been fully paid off!",
                AppNotificationPriority.HIGH
            )
            LoanNotificationType.BALANCE_UPDATE -> Triple(
                "📊 Balance Update",
                "$loanName remaining balance: $${"%.2f".format(remainingBalance)}",
                AppNotificationPriority.LOW
            )
            LoanNotificationType.HIGH_INTEREST_WARNING -> Triple(
                "⚠️ High Interest Alert",
                "$loanName has a high interest rate of ${"%.1f".format(interestRate ?: 0.0)}%",
                AppNotificationPriority.MEDIUM
            )
            LoanNotificationType.LOAN_CREATED -> Triple(
                "📝 Loan Added",
                "New loan '$loanName' added with balance $${"%.2f".format(remainingBalance)}",
                AppNotificationPriority.LOW
            )
            LoanNotificationType.PAYMENT_MILESTONE -> Triple(
                "🎯 Payment Milestone",
                "You've paid off ${"%.0f".format((totalPaid / (totalPaid + remainingBalance)) * 100)}% of $loanName!",
                AppNotificationPriority.LOW
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.LOAN,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "loans",
            iconType = "credit_card",
            accentColor = when (type) {
                LoanNotificationType.LOAN_PAID_OFF -> "#4CAF50"
                LoanNotificationType.PAYMENT_OVERDUE -> "#E57373"
                else -> "#5C9CE5"
            }
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = LoanNotificationDetail(
            notificationId = notificationId,
            loanId = loanId,
            type = type,
            loanName = loanName,
            lenderName = lenderName,
            paymentAmount = paymentAmount,
            remainingBalance = remainingBalance,
            totalPaid = totalPaid,
            nextPaymentDate = nextPaymentDate,
            daysUntilPayment = daysUntilPayment,
            interestRate = interestRate
        )
        notificationDao.insertLoanNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE EXPENSE NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createExpenseNotification(
        type: ExpenseNotificationType,
        amount: Double,
        expenseId: Long? = null,
        categoryName: String? = null,
        merchantName: String? = null,
        description: String? = null,
        averageForCategory: Double? = null
    ): Long {
        val deviationPercent = if (averageForCategory != null && averageForCategory > 0) {
            ((amount - averageForCategory) / averageForCategory) * 100
        } else null

        val (title, message, priority) = when (type) {
            ExpenseNotificationType.LARGE_EXPENSE -> Triple(
                "💸 Large Expense",
                "$${"%.2f".format(amount)} spent${categoryName?.let { " on $it" } ?: ""}",
                AppNotificationPriority.MEDIUM
            )
            ExpenseNotificationType.ANOMALY_DETECTED -> Triple(
                "🔍 Unusual Expense",
                "This ${categoryName ?: "expense"} is ${"%.0f".format(deviationPercent ?: 0.0)}% higher than usual",
                AppNotificationPriority.HIGH
            )
            ExpenseNotificationType.RECURRING_DETECTED -> Triple(
                "🔄 Recurring Expense",
                "New recurring expense detected: ${merchantName ?: description ?: "$${"%.2f".format(amount)}"}",
                AppNotificationPriority.LOW
            )
            ExpenseNotificationType.DUPLICATE_WARNING -> Triple(
                "⚠️ Possible Duplicate",
                "This expense may be a duplicate. Please review",
                AppNotificationPriority.MEDIUM
            )
            ExpenseNotificationType.CATEGORY_INSIGHT -> Triple(
                "📊 Category Insight",
                "$categoryName spending: $${"%.2f".format(amount)}",
                AppNotificationPriority.LOW
            )
            ExpenseNotificationType.DAILY_LIMIT_WARNING -> Triple(
                "📈 High Daily Spending",
                "You've spent $${"%.2f".format(amount)} today",
                AppNotificationPriority.MEDIUM
            )
            ExpenseNotificationType.EXPENSE_ADDED -> Triple(
                "✅ Expense Logged",
                "$${"%.2f".format(amount)} expense added${categoryName?.let { " to $it" } ?: ""}",
                AppNotificationPriority.LOW
            )
            ExpenseNotificationType.UNUSUAL_MERCHANT -> Triple(
                "🏪 New Merchant",
                "First expense at ${merchantName ?: "this merchant"}",
                AppNotificationPriority.LOW
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.EXPENSE,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "expenses",
            iconType = "shopping_cart",
            accentColor = "#E57373"
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = ExpenseNotificationDetail(
            notificationId = notificationId,
            expenseId = expenseId,
            type = type,
            amount = amount,
            categoryName = categoryName,
            merchantName = merchantName,
            description = description,
            averageForCategory = averageForCategory,
            deviationPercent = deviationPercent
        )
        notificationDao.insertExpenseNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE INCOME NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createIncomeNotification(
        type: IncomeNotificationType,
        amount: Double? = null,
        incomeId: Long? = null,
        sourceName: String? = null,
        expectedDate: Long? = null,
        variance: Double? = null,
        isRecurring: Boolean = false
    ): Long {
        val (title, message, priority) = when (type) {
            IncomeNotificationType.INCOME_RECEIVED -> Triple(
                "💰 Income Received",
                "$${"%.2f".format(amount ?: 0.0)} received${sourceName?.let { " from $it" } ?: ""}",
                AppNotificationPriority.LOW
            )
            IncomeNotificationType.SALARY_EXPECTED -> Triple(
                "📅 Salary Expected",
                "${sourceName ?: "Salary"} expected soon",
                AppNotificationPriority.LOW
            )
            IncomeNotificationType.INCOME_LOWER -> Triple(
                "📉 Lower Income",
                "${sourceName ?: "Income"} is ${"%.0f".format(-(variance ?: 0.0))}% lower than usual",
                AppNotificationPriority.MEDIUM
            )
            IncomeNotificationType.INCOME_HIGHER -> Triple(
                "📈 Higher Income",
                "${sourceName ?: "Income"} is ${"%.0f".format(variance ?: 0.0)}% higher than usual!",
                AppNotificationPriority.LOW
            )
            IncomeNotificationType.RECURRING_MISSING -> Triple(
                "⚠️ Missing Income",
                "Expected ${sourceName ?: "recurring income"} has not arrived",
                AppNotificationPriority.HIGH
            )
            IncomeNotificationType.TAX_REMINDER -> Triple(
                "📋 Tax Reminder",
                "Remember to set aside funds for taxes",
                AppNotificationPriority.MEDIUM
            )
            IncomeNotificationType.INCOME_ADDED -> Triple(
                "✅ Income Logged",
                "$${"%.2f".format(amount ?: 0.0)} income added${sourceName?.let { " from $it" } ?: ""}",
                AppNotificationPriority.LOW
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.INCOME,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "income",
            iconType = "attach_money",
            accentColor = "#4CAF50"
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = IncomeNotificationDetail(
            notificationId = notificationId,
            incomeId = incomeId,
            type = type,
            amount = amount,
            sourceName = sourceName,
            expectedDate = expectedDate,
            variance = variance,
            isRecurring = isRecurring
        )
        notificationDao.insertIncomeNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE SECURITY NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createSecurityNotification(
        type: SecurityNotificationType,
        deviceInfo: String? = null,
        actionRequired: Boolean = false,
        securityLevel: String = "INFO",
        attemptCount: Int? = null
    ): Long {
        val (title, message, priority) = when (type) {
            SecurityNotificationType.LOGIN_SUCCESS -> Triple(
                "🔓 Login Successful",
                "You've been authenticated successfully",
                AppNotificationPriority.LOW
            )
            SecurityNotificationType.LOGIN_FAILED -> Triple(
                "🔒 Login Failed",
                "Authentication attempt failed${attemptCount?.let { " ($it attempts)" } ?: ""}",
                AppNotificationPriority.MEDIUM
            )
            SecurityNotificationType.BIOMETRIC_ENABLED -> Triple(
                "🔐 Biometric Enabled",
                "Biometric authentication has been enabled",
                AppNotificationPriority.LOW
            )
            SecurityNotificationType.BIOMETRIC_DISABLED -> Triple(
                "🔓 Biometric Disabled",
                "Biometric authentication has been disabled",
                AppNotificationPriority.MEDIUM
            )
            SecurityNotificationType.PIN_CHANGED -> Triple(
                "🔑 PIN Changed",
                "Your security PIN has been updated",
                AppNotificationPriority.MEDIUM
            )
            SecurityNotificationType.PIN_CREATED -> Triple(
                "🔑 PIN Created",
                "Security PIN has been set up",
                AppNotificationPriority.LOW
            )
            SecurityNotificationType.BACKUP_CREATED -> Triple(
                "💾 Backup Created",
                "Your data has been backed up successfully",
                AppNotificationPriority.LOW
            )
            SecurityNotificationType.BACKUP_RESTORED -> Triple(
                "📥 Backup Restored",
                "Your data has been restored from backup",
                AppNotificationPriority.MEDIUM
            )
            SecurityNotificationType.EXPORT_COMPLETED -> Triple(
                "📤 Export Complete",
                "Your data export has completed successfully",
                AppNotificationPriority.LOW
            )
            SecurityNotificationType.SUSPICIOUS_ACTIVITY -> Triple(
                "🚨 Suspicious Activity",
                "Unusual activity detected on your account",
                AppNotificationPriority.URGENT
            )
            SecurityNotificationType.SESSION_TIMEOUT -> Triple(
                "⏰ Session Expired",
                "Your session has expired for security",
                AppNotificationPriority.LOW
            )
            SecurityNotificationType.SECURITY_CHECK -> Triple(
                "🛡️ Security Check",
                "Regular security check completed",
                AppNotificationPriority.LOW
            )
            SecurityNotificationType.DATA_CLEARED -> Triple(
                "🗑️ Data Cleared",
                "Your app data has been cleared",
                AppNotificationPriority.HIGH
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.SECURITY,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "security",
            iconType = "security",
            accentColor = when (securityLevel) {
                "CRITICAL" -> "#E57373"
                "WARNING" -> "#FFB74D"
                else -> "#5C9CE5"
            }
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = SecurityNotificationDetail(
            notificationId = notificationId,
            type = type,
            deviceInfo = deviceInfo,
            actionRequired = actionRequired,
            securityLevel = securityLevel,
            attemptCount = attemptCount
        )
        notificationDao.insertSecurityNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE SYSTEM NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createSystemNotification(
        type: SystemNotificationType,
        customTitle: String? = null,
        customMessage: String? = null,
        version: String? = null,
        featureName: String? = null,
        actionUrl: String? = null,
        metadata: String? = null,
        summaryData: String? = null
    ): Long {
        val (title, message, priority) = when (type) {
            SystemNotificationType.APP_UPDATE -> Triple(
                customTitle ?: "🆕 Update Available",
                customMessage ?: "A new version${version?.let { " ($it)" } ?: ""} is available",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.FEATURE_ANNOUNCEMENT -> Triple(
                customTitle ?: "✨ New Feature",
                customMessage ?: "Check out ${featureName ?: "our new feature"}!",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.MAINTENANCE -> Triple(
                customTitle ?: "🔧 Maintenance",
                customMessage ?: "Scheduled maintenance in progress",
                AppNotificationPriority.MEDIUM
            )
            SystemNotificationType.DATA_SYNC -> Triple(
                customTitle ?: "🔄 Data Synced",
                customMessage ?: "Your data has been synchronized",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.STORAGE_WARNING -> Triple(
                customTitle ?: "💾 Storage Warning",
                customMessage ?: "Running low on storage space",
                AppNotificationPriority.MEDIUM
            )
            SystemNotificationType.PERFORMANCE_TIP -> Triple(
                customTitle ?: "⚡ Performance Tip",
                customMessage ?: "Optimize your app performance",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.WEEKLY_SUMMARY -> Triple(
                customTitle ?: "📊 Weekly Summary",
                customMessage ?: "Your weekly financial summary is ready",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.MONTHLY_REPORT -> Triple(
                customTitle ?: "📈 Monthly Report",
                customMessage ?: "Your monthly financial report is ready",
                AppNotificationPriority.MEDIUM
            )
            SystemNotificationType.YEAR_IN_REVIEW -> Triple(
                customTitle ?: "🎊 Year in Review",
                customMessage ?: "See your financial journey this year",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.WELCOME -> Triple(
                customTitle ?: "👋 Welcome to Budgie!",
                customMessage ?: "Let's start your financial journey",
                AppNotificationPriority.HIGH
            )
            SystemNotificationType.ONBOARDING_TIP -> Triple(
                customTitle ?: "💡 Pro Tip",
                customMessage ?: "Here's a tip to get started",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.FEEDBACK_REQUEST -> Triple(
                customTitle ?: "💬 Share Feedback",
                customMessage ?: "We'd love to hear from you!",
                AppNotificationPriority.LOW
            )
            SystemNotificationType.BIRTHDAY_GREETING -> Triple(
                customTitle ?: "🎂 Happy Birthday!",
                customMessage ?: "Wishing you a wonderful birthday!",
                AppNotificationPriority.HIGH
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.SYSTEM,
            title = title,
            message = message,
            priority = priority,
            actionRoute = null,
            iconType = "settings",
            accentColor = "#9575CD"
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = SystemNotificationDetail(
            notificationId = notificationId,
            type = type,
            version = version,
            featureName = featureName,
            actionUrl = actionUrl,
            metadata = metadata,
            summaryData = summaryData
        )
        notificationDao.insertSystemNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE INSIGHT NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createInsightNotification(
        type: InsightNotificationType,
        insightTitle: String,
        insightDetail: String,
        metric: String? = null,
        value: Double? = null,
        previousValue: Double? = null,
        trend: String? = null,
        confidence: Double? = null,
        category: String? = null,
        timeframe: String? = null
    ): Long {
        val priority = when (type) {
            InsightNotificationType.ANOMALY -> AppNotificationPriority.HIGH
            InsightNotificationType.ACHIEVEMENT -> AppNotificationPriority.MEDIUM
            else -> AppNotificationPriority.LOW
        }

        val notification = AppNotification(
            category = NotificationCategory.INSIGHT,
            title = insightTitle,
            message = insightDetail,
            priority = priority,
            actionRoute = "insights",
            iconType = "lightbulb",
            accentColor = "#5C9CE5"
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = InsightNotificationDetail(
            notificationId = notificationId,
            type = type,
            insightTitle = insightTitle,
            insightDetail = insightDetail,
            metric = metric,
            value = value,
            previousValue = previousValue,
            trend = trend,
            confidence = confidence,
            category = category,
            timeframe = timeframe
        )
        notificationDao.insertInsightNotification(detail)

        return notificationId
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CREATE SHOPPING NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    suspend fun createShoppingNotification(
        type: ShoppingNotificationType,
        listId: Long? = null,
        itemId: Long? = null,
        listName: String? = null,
        itemName: String? = null,
        itemCount: Int? = null,
        estimatedTotal: Double? = null,
        aiRecommendation: String? = null
    ): Long {
        val (title, message, priority) = when (type) {
            ShoppingNotificationType.ITEM_REMINDER -> Triple(
                "🛒 Shopping Reminder",
                "Don't forget to buy ${itemName ?: "your items"}",
                AppNotificationPriority.MEDIUM
            )
            ShoppingNotificationType.LIST_CREATED -> Triple(
                "📝 List Created",
                "${listName ?: "Shopping list"} has been created",
                AppNotificationPriority.LOW
            )
            ShoppingNotificationType.PRICE_ALERT -> Triple(
                "💰 Price Alert",
                "${itemName ?: "Item"} price has changed",
                AppNotificationPriority.MEDIUM
            )
            ShoppingNotificationType.ITEM_ADDED -> Triple(
                "➕ Item Added",
                "${itemName ?: "Item"} added to ${listName ?: "list"}",
                AppNotificationPriority.LOW
            )
            ShoppingNotificationType.LIST_COMPLETED -> Triple(
                "✅ List Completed",
                "${listName ?: "Shopping list"} has been completed!",
                AppNotificationPriority.LOW
            )
            ShoppingNotificationType.BUDGET_ESTIMATE -> Triple(
                "💵 Budget Estimate",
                "Estimated total: $${"%.2f".format(estimatedTotal ?: 0.0)}",
                AppNotificationPriority.MEDIUM
            )
            ShoppingNotificationType.RESTOCK_SUGGESTION -> Triple(
                "🔄 Restock Suggestion",
                "Time to restock ${itemName ?: "items"}",
                AppNotificationPriority.LOW
            )
            ShoppingNotificationType.AI_RECOMMENDATION -> Triple(
                "🤖 AI Recommendation",
                aiRecommendation ?: "Check AI suggestions for your list",
                AppNotificationPriority.LOW
            )
        }

        val notification = AppNotification(
            category = NotificationCategory.SHOPPING,
            title = title,
            message = message,
            priority = priority,
            actionRoute = "shopping_list",
            iconType = "shopping_bag",
            accentColor = "#FF7043"
        )
        val notificationId = notificationDao.insertNotification(notification)

        val detail = ShoppingNotificationDetail(
            notificationId = notificationId,
            listId = listId,
            itemId = itemId,
            type = type,
            listName = listName,
            itemName = itemName,
            itemCount = itemCount,
            estimatedTotal = estimatedTotal,
            aiRecommendation = aiRecommendation
        )
        notificationDao.insertShoppingNotification(detail)

        return notificationId
    }
}

