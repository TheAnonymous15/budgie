package com.example.budgie.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.budgie.MainActivity
import com.example.budgie.R
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.*
import com.example.budgie.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "BudgieNotificationManager"

/**
 * Manages in-app notifications and system push notifications for Budgie
 */
class BudgieNotificationManager private constructor(private val context: Context) {

    private val database = BudgieDatabase.getDatabase(context)
    private val notificationDao = database.notificationDao()
    private val notificationRepository = NotificationRepository.getInstance(context)
    private val notificationPrefs = NotificationPreferences.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // Notification Channels
        const val CHANNEL_BILLS = "budgie_bills"
        const val CHANNEL_INSIGHTS = "budgie_insights"
        const val CHANNEL_GOALS = "budgie_goals"
        const val CHANNEL_GENERAL = "budgie_general"

        @Volatile
        private var INSTANCE: BudgieNotificationManager? = null

        fun getInstance(context: Context): BudgieNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BudgieNotificationManager(context.applicationContext).also {
                    INSTANCE = it
                    it.createNotificationChannels()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION CHANNELS (for system notifications)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Bills Channel - High Priority
            // Note: Sound and vibration are controlled programmatically based on user preferences
            val billsChannel = NotificationChannel(
                CHANNEL_BILLS,
                "Bills & Payments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for bills and payment due dates"
                // Don't set default sound/vibration - we control these in code
                setSound(null, null)
                enableVibration(false)
                enableLights(true)
            }

            // Insights Channel - Default Priority
            val insightsChannel = NotificationChannel(
                CHANNEL_INSIGHTS,
                "Financial Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily tips and spending insights"
                setSound(null, null)
                enableVibration(false)
            }

            // Goals Channel - Default Priority
            val goalsChannel = NotificationChannel(
                CHANNEL_GOALS,
                "Goals & Savings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on your financial goals"
                setSound(null, null)
                enableVibration(false)
            }

            // General Channel - Low Priority
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General updates and reminders"
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannels(
                listOf(billsChannel, insightsChannel, goalsChannel, generalChannel)
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IN-APP NOTIFICATION CREATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Create and save an in-app notification
     */
    suspend fun createNotification(
        title: String,
        message: String,
        type: NotificationType,
        priority: NotificationPriority = NotificationPriority.MEDIUM,
        actionRoute: String? = null,
        actionData: String? = null,
        expiresInHours: Int? = null,
        showSystemNotification: Boolean = false
    ): Long {
        val notification = BudgieNotification(
            title = title,
            message = message,
            type = type,
            priority = priority,
            actionRoute = actionRoute,
            actionData = actionData,
            expiresAt = expiresInHours?.let {
                System.currentTimeMillis() + (it * 60 * 60 * 1000L)
            }
        )

        val id = notificationDao.insertNotification(notification)
        Log.d(TAG, "Created notification: $title (ID: $id)")

        // Optionally show system notification
        if (showSystemNotification) {
            showSystemNotification(notification.copy(id = id))
        }

        return id
    }

    /**
     * Create a welcome notification for new users
     */
    suspend fun createWelcomeNotification(userName: String) {
        createNotification(
            title = "Welcome to Budgie, $userName! 🎉",
            message = "Your financial journey begins now. Start by adding your first income or expense to see the magic happen!",
            type = NotificationType.WELCOME,
            priority = NotificationPriority.HIGH,
            actionRoute = "dashboard"
        )
    }

    /**
     * Create a bill reminder notification
     */
    suspend fun createBillReminder(
        billTitle: String,
        amount: Double,
        dueInDays: Int
    ) {
        val urgency = when {
            dueInDays <= 0 -> "is overdue"
            dueInDays == 1 -> "is due tomorrow"
            else -> "is due in $dueInDays days"
        }

        val priority = when {
            dueInDays <= 0 -> NotificationPriority.URGENT
            dueInDays <= 2 -> NotificationPriority.HIGH
            else -> NotificationPriority.MEDIUM
        }

        val type = if (dueInDays <= 0) NotificationType.BILL_OVERDUE else NotificationType.BILL_REMINDER

        createNotification(
            title = "💳 $billTitle $urgency",
            message = "Amount: $${String.format("%.2f", amount)}. Don't forget to pay on time!",
            type = type,
            priority = priority,
            actionRoute = "bills",
            showSystemNotification = dueInDays <= 1
        )
    }

    /**
     * Create a budget warning notification
     */
    suspend fun createBudgetWarning(
        category: String,
        percentUsed: Int,
        remaining: Double
    ) {
        val (title, message, type, priority) = when {
            percentUsed >= 100 -> {
                listOf(
                    "🚨 $category Budget Exceeded!",
                    "You've exceeded your budget. Consider reducing spending in this category.",
                    NotificationType.BUDGET_EXCEEDED,
                    NotificationPriority.HIGH
                )
            }
            percentUsed >= 90 -> {
                listOf(
                    "⚠️ $category Budget Almost Full",
                    "You've used $percentUsed% of your budget. Only $${String.format("%.2f", remaining)} remaining.",
                    NotificationType.BUDGET_WARNING,
                    NotificationPriority.HIGH
                )
            }
            else -> {
                listOf(
                    "📊 $category Budget Update",
                    "You've used $percentUsed% of your budget. $${String.format("%.2f", remaining)} remaining.",
                    NotificationType.BUDGET_WARNING,
                    NotificationPriority.MEDIUM
                )
            }
        }

        createNotification(
            title = title as String,
            message = message as String,
            type = type as NotificationType,
            priority = priority as NotificationPriority,
            actionRoute = "budget"
        )
    }

    /**
     * Create a goal progress notification
     */
    suspend fun createGoalProgress(
        goalName: String,
        percentComplete: Int,
        isCompleted: Boolean = false
    ) {
        if (isCompleted) {
            createNotification(
                title = "🎉 Goal Achieved: $goalName!",
                message = "Congratulations! You've reached your savings goal. Keep up the amazing financial discipline!",
                type = NotificationType.GOAL_ACHIEVED,
                priority = NotificationPriority.HIGH,
                actionRoute = "goals",
                showSystemNotification = true
            )
        } else {
            val milestone = (percentComplete / 25) * 25 // 25, 50, 75
            if (milestone > 0 && percentComplete >= milestone && percentComplete < milestone + 5) {
                createNotification(
                    title = "🎯 $goalName: ${milestone}% Complete!",
                    message = "You're making great progress! Keep saving to reach your goal.",
                    type = NotificationType.GOAL_PROGRESS,
                    priority = NotificationPriority.MEDIUM,
                    actionRoute = "goals"
                )
            }
        }
    }

    /**
     * Create a daily insight notification
     */
    suspend fun createDailyInsight(insight: String, tip: String? = null) {
        createNotification(
            title = "💡 Daily Financial Insight",
            message = insight + (tip?.let { "\n\nTip: $it" } ?: ""),
            type = NotificationType.DAILY_INSIGHT,
            priority = NotificationPriority.LOW,
            expiresInHours = 24
        )
    }

    /**
     * Create morning greeting notification
     */
    suspend fun createMorningGreeting(userName: String, insight: String) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        createNotification(
            title = "☀️ $greeting, $userName!",
            message = insight,
            type = NotificationType.DAILY_INSIGHT,
            priority = NotificationPriority.LOW,
            expiresInHours = 12,
            showSystemNotification = true
        )
    }

    /**
     * Create evening reminder notification
     */
    suspend fun createEveningReminder(userName: String) {
        createNotification(
            title = "🌙 Time to Log Today's Expenses",
            message = "Hey $userName! Don't forget to record any expenses from today. Keeping track helps you stay on budget!",
            type = NotificationType.REMINDER,
            priority = NotificationPriority.MEDIUM,
            expiresInHours = 12,
            actionRoute = "add_expense",
            showSystemNotification = true
        )
    }

    /**
     * Create a loan payment reminder
     */
    suspend fun createLoanPaymentReminder(
        loanTitle: String,
        amount: Double,
        dueInDays: Int
    ) {
        createNotification(
            title = "💰 Loan Payment Due: $loanTitle",
            message = "Payment of $${String.format("%.2f", amount)} is due in $dueInDays days. Stay on track with your repayment!",
            type = NotificationType.LOAN_PAYMENT_DUE,
            priority = if (dueInDays <= 3) NotificationPriority.HIGH else NotificationPriority.MEDIUM,
            actionRoute = "loans",
            showSystemNotification = dueInDays <= 1
        )
    }

    /**
     * Create spending alert notification
     */
    suspend fun createSpendingAlert(category: String, amount: Double, comparison: String) {
        createNotification(
            title = "📈 Unusual Spending in $category",
            message = "You spent $${String.format("%.2f", amount)} on $category today, which is $comparison. Consider reviewing your spending habits.",
            type = NotificationType.SPENDING_ALERT,
            priority = NotificationPriority.MEDIUM,
            actionRoute = "expenses"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYSTEM NOTIFICATION (Push Notifications)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun showSystemNotification(notification: BudgieNotification) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted")
            return
        }

        val channelId = when (notification.type) {
            NotificationType.BILL_REMINDER, NotificationType.BILL_OVERDUE,
            NotificationType.LOAN_PAYMENT_DUE -> CHANNEL_BILLS
            NotificationType.DAILY_INSIGHT, NotificationType.WEEKLY_SUMMARY,
            NotificationType.MONTHLY_REPORT, NotificationType.SPENDING_ALERT -> CHANNEL_INSIGHTS
            NotificationType.GOAL_PROGRESS, NotificationType.GOAL_ACHIEVED,
            NotificationType.SAVINGS_TIP -> CHANNEL_GOALS
            else -> CHANNEL_GENERAL
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_route", notification.actionRoute)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(when (notification.priority) {
                NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
                NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
                NotificationPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
                NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notification.id.toInt(), builder.build())
            Log.d(TAG, "System notification shown: ${notification.title}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show notification: ${e.message}")
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA ACCESS METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    fun getAllNotifications(): Flow<List<BudgieNotification>> = notificationDao.getAllNotifications()

    fun getUnreadNotifications(): Flow<List<BudgieNotification>> = notificationDao.getUnreadNotifications()

    fun getUnreadCount(): Flow<Int> = notificationDao.getUnreadCount()

    suspend fun markAsRead(id: Long) = notificationDao.markAsRead(id)

    suspend fun markAllAsRead() = notificationDao.markAllAsRead()

    suspend fun archiveNotification(id: Long) = notificationDao.archiveNotification(id)

    suspend fun deleteNotification(id: Long) = notificationDao.deleteById(id)

    suspend fun deleteAllRead() = notificationDao.deleteAllRead()

    suspend fun cleanupExpired() = notificationDao.deleteExpired()

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get time ago string for a notification
     */
    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
                "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST NOTIFICATION METHODS (for development/demo purposes)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Create test in-app notification #1 - Financial Insight
     * Creates both in-app notification (shows in NotificationsScreen) and push notification
     */
    suspend fun createTestNotification1() {
        Log.d(TAG, "createTestNotification1 called")

        // Check if all notifications are disabled
        if (!notificationPrefs.allNotificationsEnabled) {
            Log.d(TAG, "All notifications are DISABLED - skipping test notification #1")
            return
        }

        val insights = listOf(
            Triple("💰 Saving Tip", "You've spent 15% less on dining this week compared to last week. Great progress!", NotificationCategory.INSIGHT),
            Triple("📊 Budget Update", "Your entertainment budget is 70% used. Consider limiting subscriptions to stay on track.", NotificationCategory.BUDGET),
            Triple("🎯 Goal Milestone", "You're 60% closer to your vacation fund! Keep saving $50/week to reach it by March.", NotificationCategory.GOAL),
            Triple("💡 Smart Insight", "Based on your spending patterns, you could save an extra $120/month by reducing takeout orders.", NotificationCategory.INSIGHT),
            Triple("📈 Investment Alert", "Your investment portfolio has grown 5.2% this month. Time to review your strategy!", NotificationCategory.INVESTMENT)
        )

        val (title, message, category) = insights.random()
        Log.d(TAG, "Creating notification: $title")

        try {
            // Create in-app notification using NotificationRepository (shows in NotificationsScreen)
            val notificationId = notificationRepository.createQuickNotification(
                title = title,
                message = message,
                category = category,
                priority = AppNotificationPriority.MEDIUM,
                actionRoute = when (category) {
                    NotificationCategory.INSIGHT -> "insights"
                    NotificationCategory.BUDGET -> "budget"
                    NotificationCategory.GOAL -> "goals"
                    NotificationCategory.INVESTMENT -> "investments"
                    else -> "dashboard"
                },
                iconType = when (category) {
                    NotificationCategory.INSIGHT -> "lightbulb"
                    NotificationCategory.BUDGET -> "account_balance_wallet"
                    NotificationCategory.GOAL -> "flag"
                    NotificationCategory.INVESTMENT -> "trending_up"
                    else -> "notifications"
                },
                accentColor = "#0FAE96"
            )
            Log.d(TAG, "In-app notification created with ID: $notificationId")

            // Also show system push notification
            showPushNotification(
                id = notificationId.toInt(),
                title = title,
                message = message,
                channelId = CHANNEL_INSIGHTS
            )

            Log.d(TAG, "Test notification #1 completed: $title (ID: $notificationId)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create test notification #1: ${e.message}", e)
        }
    }

    /**
     * Create test in-app notification #2 - Bill/Budget Alert
     * Creates both in-app notification and push notification
     */
    suspend fun createTestNotification2() {
        Log.d(TAG, "createTestNotification2 called")

        // Check if all notifications are disabled
        if (!notificationPrefs.allNotificationsEnabled) {
            Log.d(TAG, "All notifications are DISABLED - skipping test notification #2")
            return
        }

        val alerts = listOf(
            Triple("⚠️ Bill Due Tomorrow", "Your electricity bill of KES 2,500 is due tomorrow. Don't forget to pay!", NotificationCategory.BILL),
            Triple("🚨 Budget Alert", "You've reached 90% of your Food budget. Only KES 1,200 remaining for the month.", NotificationCategory.BUDGET),
            Triple("💳 Payment Reminder", "Your internet subscription of KES 3,000 renews in 3 days. Ensure sufficient balance.", NotificationCategory.BILL),
            Triple("📊 Weekly Summary", "This week you spent KES 8,500 - 12% less than last week. Well done!", NotificationCategory.SYSTEM),
            Triple("🎉 Achievement Unlocked", "You've maintained your savings streak for 30 days! Financial discipline pays off.", NotificationCategory.GOAL)
        )

        val (title, message, category) = alerts.random()
        Log.d(TAG, "Creating notification: $title")

        val priority = when (category) {
            NotificationCategory.BILL -> AppNotificationPriority.HIGH
            NotificationCategory.BUDGET -> AppNotificationPriority.HIGH
            NotificationCategory.GOAL -> AppNotificationPriority.MEDIUM
            else -> AppNotificationPriority.MEDIUM
        }

        // Create in-app notification using NotificationRepository
        val notificationId = notificationRepository.createQuickNotification(
            title = title,
            message = message,
            category = category,
            priority = priority,
            actionRoute = when (category) {
                NotificationCategory.BILL -> "bills"
                NotificationCategory.BUDGET -> "budget"
                NotificationCategory.GOAL -> "goals"
                else -> "dashboard"
            },
            iconType = when (category) {
                NotificationCategory.BILL -> "receipt"
                NotificationCategory.BUDGET -> "account_balance_wallet"
                NotificationCategory.GOAL -> "flag"
                else -> "notifications"
            },
            accentColor = when (category) {
                NotificationCategory.BILL -> "#E57373"
                NotificationCategory.BUDGET -> "#FFB74D"
                NotificationCategory.GOAL -> "#0FAE96"
                else -> "#5C9CE5"
            }
        )

        // Also show system push notification
        val channelId = when (category) {
            NotificationCategory.BILL -> CHANNEL_BILLS
            NotificationCategory.BUDGET -> CHANNEL_INSIGHTS
            NotificationCategory.GOAL -> CHANNEL_GOALS
            else -> CHANNEL_GENERAL
        }

        showPushNotification(
            id = notificationId.toInt(),
            title = title,
            message = message,
            channelId = channelId
        )

        Log.d(TAG, "Test notification #2 created: $title (ID: $notificationId)")
    }

    /**
     * Show a system push notification (appears in notification tray)
     */
    private fun showPushNotification(
        id: Int,
        title: String,
        message: String,
        channelId: String
    ) {
        Log.d(TAG, "showPushNotification called - ID: $id, Channel: $channelId")

        // Check if all notifications are disabled
        if (!notificationPrefs.allNotificationsEnabled) {
            Log.d(TAG, "All notifications are DISABLED - skipping push notification")
            return
        }

        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted - skipping push notification")
            return
        }

        // Check if we're in quiet hours
        if (notificationPrefs.isInQuietHours()) {
            Log.d(TAG, "In quiet hours - showing silent notification")
        }

        Log.d(TAG, "Notification permission granted, all notifications enabled")

        // Ensure notification channels are created
        createNotificationChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get app icon as large icon for notification
        val largeIcon = android.graphics.BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.ic_launcher
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Get sound mode from preferences
        val currentSoundMode = notificationPrefs.soundMode

        Log.d(TAG, "Current Sound Mode: $currentSoundMode")

        // Handle each sound mode explicitly
        when (currentSoundMode) {
            NotificationSoundMode.SILENT -> {
                // SILENT: No sound, no vibration
                builder.setSilent(true)
                builder.setSound(null)
                builder.setVibrate(null)
                builder.setDefaults(0)
                Log.d(TAG, "SILENT MODE - No sound, no vibration")
            }

            NotificationSoundMode.VIBRATION_ONLY -> {
                // VIBRATION ONLY: No sound, only vibrate
                builder.setSound(null)
                builder.setDefaults(0) // Clear defaults to prevent sound
                // Vibration pattern: wait 0ms, vibrate 300ms, wait 200ms, vibrate 300ms, wait 200ms, vibrate 300ms
                val vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 300)
                builder.setVibrate(vibrationPattern)
                Log.d(TAG, "VIBRATION ONLY MODE - No sound, vibration enabled")
            }

            NotificationSoundMode.SOUND_ONLY -> {
                // SOUND ONLY: Play sound, no vibration
                builder.setVibrate(null)
                applyNotificationSound(builder)
                Log.d(TAG, "SOUND ONLY MODE - Sound enabled, no vibration")
            }

            NotificationSoundMode.SOUND_AND_VIBRATION -> {
                // SOUND + VIBRATION: Both enabled
                applyNotificationSound(builder)
                val vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 300)
                builder.setVibrate(vibrationPattern)
                Log.d(TAG, "SOUND + VIBRATION MODE - Both enabled")
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
            Log.d(TAG, "Push notification shown successfully: $title")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show push notification: ${e.message}")
        }
    }

    /**
     * Helper to apply sound settings to notification builder
     */
    private fun applyNotificationSound(builder: NotificationCompat.Builder) {
        val customSoundUri = notificationPrefs.customSoundUri
        val useSystemSound = notificationPrefs.useSystemSound

        Log.d(TAG, "Sound settings - useSystemSound: $useSystemSound, customSoundUri: $customSoundUri")

        if (!useSystemSound && customSoundUri != null) {
            // Use custom sound - this is the selected ringtone
            builder.setSound(customSoundUri)
            Log.d(TAG, "Using CUSTOM notification sound: $customSoundUri")
        } else {
            // Use default system notification sound
            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            builder.setSound(defaultSoundUri)
            Log.d(TAG, "Using DEFAULT notification sound: $defaultSoundUri")
        }
    }

    /**
     * Start automatic test notifications (for demo purposes)
     * Sends 2 different notifications every 30 seconds
     */
    fun startTestNotificationLoop() {
        scope.launch {
            var counter = 0
            while (true) {
                kotlinx.coroutines.delay(30_000) // Wait 30 seconds

                // Alternate between the two test notifications
                if (counter % 2 == 0) {
                    createTestNotification1()
                } else {
                    createTestNotification2()
                }
                counter++

                Log.d(TAG, "Test notification loop iteration: $counter")
            }
        }
    }

    /**
     * Stop all test notifications (cleanup)
     */
    fun stopTestNotificationLoop() {
        // This would require storing the job reference to cancel it
        // For now, app restart will stop the loop
        Log.d(TAG, "Test notification loop will stop on next app restart")
    }
}

