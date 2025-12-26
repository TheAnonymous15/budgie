package com.example.budgie.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.budgie.MainActivity
import com.example.budgie.R
import java.util.Calendar

object NotificationHelper {

    // Channel IDs
    const val CHANNEL_BILLS = "budgie_bills"
    const val CHANNEL_DAILY_REMINDER = "budgie_daily"
    const val CHANNEL_INSIGHTS = "budgie_insights"
    const val CHANNEL_BIRTHDAY = "budgie_birthday"
    const val CHANNEL_GREETINGS = "budgie_greetings"
    const val CHANNEL_SEASONAL = "budgie_seasonal"

    // Notification IDs
    const val NOTIFICATION_MORNING_GREETING = 1001
    const val NOTIFICATION_EVENING_REMINDER = 1002
    const val NOTIFICATION_BILL_REMINDER = 2000
    const val NOTIFICATION_WEEKLY_INSIGHT = 3001
    const val NOTIFICATION_MONTHLY_SUMMARY = 3002
    const val NOTIFICATION_BIRTHDAY = 4001
    const val NOTIFICATION_SEASONAL = 5001
    const val NOTIFICATION_SMART_INSIGHT = 6001

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Bills Channel - High Priority
        val billsChannel = NotificationChannel(
            CHANNEL_BILLS,
            "Bill Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming bills and payments"
            enableVibration(true)
            enableLights(true)
        }

        // Daily Reminder Channel
        val dailyChannel = NotificationChannel(
            CHANNEL_DAILY_REMINDER,
            "Daily Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily expense tracking reminders"
        }

        // Insights Channel
        val insightsChannel = NotificationChannel(
            CHANNEL_INSIGHTS,
            "Financial Insights",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Weekly and monthly financial insights"
        }

        // Birthday Channel - High Priority with sound
        val birthdayChannel = NotificationChannel(
            CHANNEL_BIRTHDAY,
            "Birthday Celebrations",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Birthday wishes and celebrations"
            enableVibration(true)
            enableLights(true)
        }

        // Greetings Channel
        val greetingsChannel = NotificationChannel(
            CHANNEL_GREETINGS,
            "Daily Greetings",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Morning greetings and motivational messages"
        }

        // Seasonal Channel
        val seasonalChannel = NotificationChannel(
            CHANNEL_SEASONAL,
            "Seasonal Tips",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Holiday and seasonal financial tips"
        }

        notificationManager.createNotificationChannels(
            listOf(billsChannel, dailyChannel, insightsChannel, birthdayChannel, greetingsChannel, seasonalChannel)
        )
    }

    /**
     * Show morning greeting with seasonal awareness and smart insights
     */
    fun showMorningGreeting(context: Context, userName: String, todaySpendingGoal: Double? = null) {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Check for special days/seasons
        val seasonalMessage = getSeasonalMorningMessage(month, dayOfMonth)

        val greetings = when (dayOfWeek) {
            Calendar.MONDAY -> listOf(
                "Good morning, $userName! 🌅 New week, new financial goals!",
                "Rise and shine, $userName! 💪 Let's conquer this week!",
                "Hey $userName! ☀️ Monday is perfect for fresh financial starts!"
            )
            Calendar.FRIDAY -> listOf(
                "Happy Friday, $userName! 🎉 Weekend's coming - plan your budget!",
                "TGIF, $userName! 🌟 Don't let weekend spending catch you off guard!",
                "Good morning, $userName! 🎊 Friday tip: Set a weekend spending limit!"
            )
            Calendar.SATURDAY, Calendar.SUNDAY -> listOf(
                "Good morning, $userName! ☕ Enjoy your weekend wisely!",
                "Hey $userName! 🌞 Weekends are great for reviewing your finances!",
                "Morning, $userName! 🌻 Relax, but stay mindful of spending!"
            )
            else -> listOf(
                "Good morning, $userName! 🌅 Ready to make smart money moves today?",
                "Rise and shine, $userName! 💪 Your financial goals are waiting!",
                "Hey $userName! ☀️ A new day means new opportunities to save!",
                "Good morning, $userName! 🌟 Let's make today count financially!",
                "Hello $userName! 🌻 Time to take control of your finances!"
            )
        }

        val motivationalQuotes = listOf(
            "💡 Tip: Small daily savings add up to big yearly gains!",
            "💰 Remember: Every dollar saved is a dollar earned!",
            "📊 Track your expenses today for a wealthier tomorrow!",
            "🎯 Stay focused on your financial goals!",
            "✨ Your future self will thank you for saving today!",
            "🧠 Smart spending today = Financial freedom tomorrow!",
            "💎 Budget like a pro - your wallet will thank you!"
        )

        val title = seasonalMessage ?: greetings.random()
        val message = if (todaySpendingGoal != null && todaySpendingGoal > 0) {
            "Today's budget: $${String.format("%.2f", todaySpendingGoal)} - ${motivationalQuotes.random()}"
        } else {
            motivationalQuotes.random()
        }

        showNotification(
            context = context,
            channelId = CHANNEL_GREETINGS,
            notificationId = NOTIFICATION_MORNING_GREETING,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground
        )
    }

    /**
     * Show evening reminder with smart insights
     */
    fun showEveningExpenseReminder(context: Context, userName: String, todaySpent: Double? = null, dailyBudget: Double? = null) {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val reminders = when (dayOfWeek) {
            Calendar.FRIDAY -> listOf(
                "Hey $userName! 🌙 Log expenses before the weekend starts!",
                "TGIF $userName! 📝 Don't forget to track today's spending!",
                "Evening $userName! 🎉 Record expenses before weekend fun begins!"
            )
            Calendar.SUNDAY -> listOf(
                "Hey $userName! 🌙 Week's almost over - let's get expenses in order!",
                "Sunday evening, $userName! 📊 Great time to review your week!",
                "Hey $userName! 🌃 Prep for Monday by updating expenses now!"
            )
            else -> listOf(
                "Hey $userName! 🌙 Don't forget to log today's expenses!",
                "Evening, $userName! 📝 Quick reminder to track your spending!",
                "$userName, time to update your expenses! 💼 Just takes a minute!",
                "Hi $userName! 🌃 How much did you spend today? Let's log it!",
                "Hey $userName! 📊 End your day right - record your expenses!"
            )
        }

        val title = reminders.random()
        val message = if (todaySpent != null && dailyBudget != null && dailyBudget > 0) {
            val remaining = dailyBudget - todaySpent
            if (remaining >= 0) {
                "You've spent $${String.format("%.2f", todaySpent)} today. $${String.format("%.2f", remaining)} left in budget! 💪"
            } else {
                "You've spent $${String.format("%.2f", todaySpent)} today - $${String.format("%.2f", -remaining)} over budget. Let's track it! 📊"
            }
        } else {
            "Tap to add your daily expenses and stay on track! 💪"
        }

        showNotification(
            context = context,
            channelId = CHANNEL_DAILY_REMINDER,
            notificationId = NOTIFICATION_EVENING_REMINDER,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground
        )
    }

    /**
     * Show smart bill reminder based on payment patterns
     */
    fun showBillReminder(context: Context, billName: String, amount: Double, daysUntilDue: Int, billId: Long, usualPayDay: Int? = null) {
        val urgency = when {
            daysUntilDue <= 0 -> "🚨 OVERDUE"
            daysUntilDue == 1 -> "⚠️ Due Tomorrow"
            daysUntilDue <= 3 -> "📅 Due Soon"
            else -> "📋 Upcoming"
        }

        val message = when {
            daysUntilDue <= 0 -> "$billName is overdue! Please pay $${String.format("%.2f", amount)} now to avoid late fees."
            daysUntilDue == 1 -> "$billName ($${String.format("%.2f", amount)}) is due tomorrow! Don't forget to pay."
            usualPayDay != null && daysUntilDue == usualPayDay ->
                "You usually pay $billName around now. $${String.format("%.2f", amount)} due in $daysUntilDue days."
            else -> "$billName ($${String.format("%.2f", amount)}) is due in $daysUntilDue days."
        }

        showNotification(
            context = context,
            channelId = CHANNEL_BILLS,
            notificationId = NOTIFICATION_BILL_REMINDER + billId.toInt(),
            title = "$urgency: $billName",
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            priority = if (daysUntilDue <= 1) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        )
    }

    /**
     * Show seasonal/holiday financial tips
     */
    fun showSeasonalNotification(context: Context, userName: String) {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val (title, message) = getSeasonalNotification(userName, month, dayOfMonth) ?: return

        showNotification(
            context = context,
            channelId = CHANNEL_SEASONAL,
            notificationId = NOTIFICATION_SEASONAL,
            title = title,
            message = message,
            icon = R.drawable.ic_launcher_foreground
        )
    }

    /**
     * Get seasonal morning message based on date
     */
    private fun getSeasonalMorningMessage(month: Int, dayOfMonth: Int): String? {
        return when {
            // Christmas Season (Dec 1-25)
            month == Calendar.DECEMBER && dayOfMonth <= 25 -> when (dayOfMonth) {
                25 -> "🎄 Merry Christmas! Enjoy the day, spend wisely! 🎁"
                24 -> "🎄 Christmas Eve! Last-minute gifts? Stick to your budget! 🎁"
                in 20..23 -> "🎄 Christmas is near! Track those holiday expenses! 🛍️"
                else -> "🎄 Holiday season tip: Set a gift budget and stick to it!"
            }
            // New Year (Dec 31 - Jan 1)
            month == Calendar.DECEMBER && dayOfMonth == 31 -> "🎆 Happy New Year's Eve! Time to set financial goals for next year!"
            month == Calendar.JANUARY && dayOfMonth == 1 -> "🎊 Happy New Year! Perfect time for fresh financial resolutions!"
            // Valentine's Day
            month == Calendar.FEBRUARY && dayOfMonth == 14 -> "💕 Happy Valentine's Day! Love doesn't need to break the bank! 💝"
            // Easter Season (approximate)
            month == Calendar.MARCH && dayOfMonth in 20..31 || month == Calendar.APRIL && dayOfMonth in 1..20 -> null
            // Black Friday (late November)
            month == Calendar.NOVEMBER && dayOfMonth in 24..30 -> "🛒 Black Friday season! Make a list, check it twice, stick to your budget!"
            // Back to School (August)
            month == Calendar.AUGUST -> "📚 Back to school season! Budget for supplies wisely!"
            // Tax Season (April)
            month == Calendar.APRIL && dayOfMonth <= 15 -> "📋 Tax season reminder: Have you filed yet?"
            else -> null
        }
    }

    /**
     * Get seasonal notification content
     */
    private fun getSeasonalNotification(userName: String, month: Int, dayOfMonth: Int): Pair<String, String>? {
        return when {
            // Christmas spending tips
            month == Calendar.DECEMBER && dayOfMonth in 1..24 -> {
                val tips = listOf(
                    Pair("🎄 Holiday Budget Check, $userName!", "The average person overspends by 30% during holidays. Set firm limits and track every purchase!"),
                    Pair("🎁 Smart Gift Shopping Tip!", "Consider DIY gifts or experiences over expensive items. Your wallet (and recipients) will thank you!"),
                    Pair("💰 Holiday Spending Alert!", "Before buying, ask: Is this a want or need? Can I afford it without credit? Will they actually use it?"),
                    Pair("🛍️ Avoid Holiday Debt, $userName!", "If you can't pay cash, consider skipping it. January credit card bills can be brutal!")
                )
                tips.random()
            }
            // New Year financial resolutions
            month == Calendar.JANUARY && dayOfMonth in 1..15 -> {
                val tips = listOf(
                    Pair("🎯 New Year, New Budget!", "Start the year right: Review last year's spending and set realistic goals for this year!"),
                    Pair("💪 Financial Resolution Time!", "Top tip: Automate your savings. Even small amounts add up over the year!"),
                    Pair("📊 Fresh Start, $userName!", "New year = clean slate. Review subscriptions and cancel what you don't use!")
                )
                tips.random()
            }
            // Black Friday / Cyber Monday
            month == Calendar.NOVEMBER && dayOfMonth in 20..30 -> {
                val tips = listOf(
                    Pair("⚠️ Black Friday Alert!", "Don't buy something just because it's on sale. A deal isn't a deal if you didn't need it!"),
                    Pair("🛒 Smart Shopping Reminder!", "Make a list BEFORE sales start. Impulse buys are budget killers!"),
                    Pair("💳 Cyber Monday Tip!", "Compare prices before buying. Many 'deals' aren't actually discounts!")
                )
                tips.random()
            }
            // Summer vacation season
            month in listOf(Calendar.JUNE, Calendar.JULY) -> {
                val tips = listOf(
                    Pair("🏖️ Summer Budget Tip!", "Vacation spending can sneak up on you. Set a daily spending limit!"),
                    Pair("☀️ Summer Savings Reminder!", "Plan activities in advance. Last-minute bookings often cost more!")
                )
                tips.random()
            }
            else -> null
        }
    }

    fun showWeeklyInsight(context: Context, totalSpent: Double, topCategory: String, savingsTip: String) {
        showNotification(
            context = context,
            channelId = CHANNEL_INSIGHTS,
            notificationId = NOTIFICATION_WEEKLY_INSIGHT,
            title = "📊 Your Weekly Spending Summary",
            message = "You spent $${String.format("%.2f", totalSpent)} this week. Top category: $topCategory. $savingsTip",
            icon = R.drawable.ic_launcher_foreground
        )
    }

    fun showMonthlySummary(context: Context, totalIncome: Double, totalExpenses: Double, netSavings: Double) {
        val status = if (netSavings >= 0) "🎉 Great job!" else "⚠️ You overspent!"
        val message = "Income: $${String.format("%.2f", totalIncome)} | Expenses: $${String.format("%.2f", totalExpenses)} | Savings: $${String.format("%.2f", netSavings)}"

        showNotification(
            context = context,
            channelId = CHANNEL_INSIGHTS,
            notificationId = NOTIFICATION_MONTHLY_SUMMARY,
            title = "📅 Monthly Financial Report $status",
            message = message,
            icon = R.drawable.ic_launcher_foreground
        )
    }

    /**
     * Show smart AI insight notification
     */
    fun showSmartInsight(context: Context, userName: String, insightTitle: String, insightMessage: String, priority: Int = NotificationCompat.PRIORITY_DEFAULT) {
        showNotification(
            context = context,
            channelId = CHANNEL_INSIGHTS,
            notificationId = NOTIFICATION_SMART_INSIGHT,
            title = "💡 $insightTitle",
            message = insightMessage,
            icon = R.drawable.ic_launcher_foreground,
            priority = priority
        )
    }

    fun showBirthdayNotification(context: Context, userName: String, age: Int) {
        val wishes = listOf(
            "🎂 Happy Birthday, $userName! 🎉",
            "🎈 It's your special day, $userName! 🎂",
            "🎊 Birthday wishes to you, $userName! 🎁",
            "🥳 Happy Birthday, amazing $userName! 🎂"
        )

        val message = "You're turning $age today! 🎈 Open Budgie for a special celebration! 🎶🎉"

        showNotification(
            context = context,
            channelId = CHANNEL_BIRTHDAY,
            notificationId = NOTIFICATION_BIRTHDAY,
            title = wishes.random(),
            message = message,
            icon = R.drawable.ic_launcher_foreground,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    /**
     * Check if we have notification permission
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        // Check notification permission for Android 13+
        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

