package com.example.budgie.notifications

import android.content.Context
import androidx.work.*
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager workers for scheduled notifications
 */

// Morning Greeting Worker - Runs at 8 AM daily
class MorningGreetingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferencesManager = UserPreferencesManager.getInstance(applicationContext)
        val userProfile = preferencesManager.userProfile.firstOrNull()

        userProfile?.let { profile ->
            // Check if it's user's birthday
            val today = Calendar.getInstance()
            val birthday = parseBirthday(profile.birthday)

            if (birthday != null &&
                today.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH)) {
                // It's their birthday!
                val age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR)
                NotificationHelper.showBirthdayNotification(applicationContext, profile.name, age)
            } else {
                // Regular morning greeting
                NotificationHelper.showMorningGreeting(applicationContext, profile.name)
            }
        }

        return Result.success()
    }

    private fun parseBirthday(birthday: String): Calendar? {
        return try {
            val parts = birthday.split("/", "-")
            if (parts.size == 3) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, parts[0].toInt())
                    set(Calendar.MONTH, parts[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

// Evening Expense Reminder Worker - Runs at 8 PM daily
class EveningReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferencesManager = UserPreferencesManager.getInstance(applicationContext)
        val userProfile = preferencesManager.userProfile.firstOrNull()

        userProfile?.let { profile ->
            NotificationHelper.showEveningExpenseReminder(applicationContext, profile.name)
        }

        return Result.success()
    }
}

// Bill Reminder Worker - Checks bills daily
class BillReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = BudgieDatabase.getDatabase(applicationContext)
        val billDao = database.billDao()

        val today = Calendar.getInstance()
        val todayMillis = today.timeInMillis

        // Get all unpaid bills
        billDao.getUnpaidBills().firstOrNull()?.forEach { bill ->
            val daysUntilDue = ((bill.dueDate - todayMillis) / (1000 * 60 * 60 * 24)).toInt()

            // Smart reminder logic based on bill type/amount
            val shouldRemind = when {
                daysUntilDue <= 0 -> true // Overdue
                daysUntilDue == 1 -> true // Due tomorrow
                daysUntilDue == 2 && bill.amount >= 100 -> true // 2 days for large bills
                daysUntilDue == 3 && bill.amount >= 500 -> true // 3 days for very large bills
                daysUntilDue == 7 && bill.amount >= 1000 -> true // Week notice for huge bills
                daysUntilDue == bill.reminderDaysBefore -> true // User-set reminder
                else -> false
            }

            if (shouldRemind) {
                NotificationHelper.showBillReminder(
                    context = applicationContext,
                    billName = bill.title,
                    amount = bill.amount,
                    daysUntilDue = daysUntilDue,
                    billId = bill.id
                )
            }
        }

        return Result.success()
    }
}

// Weekly Insight Worker - Runs every Sunday
class WeeklyInsightWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = BudgieDatabase.getDatabase(applicationContext)
        val expenseDao = database.expenseDao()

        // Get last 7 days expenses
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.timeInMillis

        val weeklyExpenses = expenseDao.getExpensesByDateRange(startDate, endDate).firstOrNull() ?: emptyList()
        val totalSpent = weeklyExpenses.sumOf { it.amount }

        // Find top category
        val topCategory = weeklyExpenses
            .groupBy { it.category }
            .maxByOrNull { it.value.sumOf { expense -> expense.amount } }
            ?.key?.displayName ?: "N/A"

        // Generate smart tip
        val tips = listOf(
            "💡 Try meal prepping to cut food costs!",
            "💰 Consider a no-spend day this week!",
            "📊 Review subscriptions you might not need.",
            "🎯 Set a weekly spending limit for extras.",
            "✨ Small changes lead to big savings!"
        )

        NotificationHelper.showWeeklyInsight(
            context = applicationContext,
            totalSpent = totalSpent,
            topCategory = topCategory,
            savingsTip = tips.random()
        )

        return Result.success()
    }
}

// Monthly Summary Worker - Runs on the 1st of each month
class MonthlySummaryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = BudgieDatabase.getDatabase(applicationContext)
        val expenseDao = database.expenseDao()
        val incomeDao = database.incomeDao()

        // Get last month's data
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endDate = calendar.timeInMillis

        val totalExpenses = expenseDao.getTotalExpensesByDateRange(startDate, endDate).firstOrNull() ?: 0.0
        val totalIncome = incomeDao.getTotalIncomeByDateRange(startDate, endDate).firstOrNull() ?: 0.0
        val netSavings = totalIncome - totalExpenses

        NotificationHelper.showMonthlySummary(
            context = applicationContext,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings
        )

        return Result.success()
    }
}

// Seasonal Notification Worker - Runs daily to check for seasonal tips
class SeasonalNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferencesManager = UserPreferencesManager.getInstance(applicationContext)
        val userProfile = preferencesManager.userProfile.firstOrNull()

        userProfile?.let { profile ->
            NotificationHelper.showSeasonalNotification(applicationContext, profile.name)
        }

        return Result.success()
    }
}

// Smart Insight Worker - Analyzes spending and sends smart insights
class SmartInsightWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferencesManager = UserPreferencesManager.getInstance(applicationContext)
        val userProfile = preferencesManager.userProfile.firstOrNull() ?: return Result.success()

        val database = BudgieDatabase.getDatabase(applicationContext)
        val expenseDao = database.expenseDao()
        val incomeDao = database.incomeDao()

        // Get this month's data
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startOfMonth = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val monthlyExpenses = expenseDao.getExpensesByDateRange(startOfMonth, now).firstOrNull() ?: emptyList()
        val totalSpent = monthlyExpenses.sumOf { it.amount }

        val monthlyIncome = incomeDao.getTotalIncomeByDateRange(startOfMonth, now).firstOrNull() ?: 0.0

        // Generate smart insights based on spending patterns
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthProgress = dayOfMonth.toDouble() / daysInMonth

        val (title, message) = when {
            // Spending faster than income pace
            monthlyIncome > 0 && totalSpent > monthlyIncome * monthProgress * 1.2 -> {
                Pair(
                    "Spending Alert for ${userProfile.name}",
                    "You've spent $${String.format("%.2f", totalSpent)} so far this month. At this pace, you might exceed your income. Consider cutting back!"
                )
            }
            // Great saving rate
            monthlyIncome > 0 && totalSpent < monthlyIncome * monthProgress * 0.6 -> {
                Pair(
                    "Great Job, ${userProfile.name}! 🌟",
                    "You're spending wisely this month! You've only used $${String.format("%.2f", totalSpent)} - keep it up!"
                )
            }
            // Category-specific insights
            monthlyExpenses.isNotEmpty() -> {
                val topCategory = monthlyExpenses
                    .groupBy { it.category }
                    .maxByOrNull { it.value.sumOf { e -> e.amount } }

                if (topCategory != null) {
                    val categoryTotal = topCategory.value.sumOf { it.amount }
                    val categoryPercentage = (categoryTotal / totalSpent * 100).toInt()

                    if (categoryPercentage > 40) {
                        Pair(
                            "Spending Pattern Detected",
                            "${topCategory.key.displayName} makes up $categoryPercentage% of your spending ($${String.format("%.2f", categoryTotal)}). Consider if this aligns with your goals."
                        )
                    } else null
                } else null
            }
            else -> null
        } ?: return Result.success()

        NotificationHelper.showSmartInsight(
            context = applicationContext,
            userName = userProfile.name,
            insightTitle = title,
            insightMessage = message
        )

        return Result.success()
    }
}

/**
 * Notification Scheduler - Schedules all periodic notifications
 */
object NotificationScheduler {

    fun scheduleAllNotifications(context: Context) {
        scheduleMorningGreeting(context)
        scheduleEveningReminder(context)
        scheduleBillReminders(context)
        scheduleWeeklyInsight(context)
        scheduleMonthlySummary(context)
        scheduleSeasonalNotifications(context)
        scheduleSmartInsights(context)
    }

    private fun scheduleMorningGreeting(context: Context) {
        val delay = calculateDelayUntil(8, 0) // 8:00 AM

        val request = PeriodicWorkRequestBuilder<MorningGreetingWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("morning_greeting")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "morning_greeting",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun scheduleEveningReminder(context: Context) {
        val delay = calculateDelayUntil(20, 0) // 8:00 PM

        val request = PeriodicWorkRequestBuilder<EveningReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("evening_reminder")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "evening_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun scheduleBillReminders(context: Context) {
        val delay = calculateDelayUntil(9, 0) // 9:00 AM

        val request = PeriodicWorkRequestBuilder<BillReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("bill_reminder")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "bill_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun scheduleWeeklyInsight(context: Context) {
        // Calculate delay until next Sunday at 10 AM
        val calendar = Calendar.getInstance()
        val daysUntilSunday = (Calendar.SUNDAY - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
        calendar.add(Calendar.DAY_OF_YEAR, if (daysUntilSunday == 0) 7 else daysUntilSunday)
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val request = PeriodicWorkRequestBuilder<WeeklyInsightWorker>(
            7, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("weekly_insight")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "weekly_insight",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun scheduleMonthlySummary(context: Context) {
        // Calculate delay until the 1st of next month at 9 AM
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val request = PeriodicWorkRequestBuilder<MonthlySummaryWorker>(
            30, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("monthly_summary")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "monthly_summary",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun scheduleSeasonalNotifications(context: Context) {
        val delay = calculateDelayUntil(12, 0) // Noon

        val request = PeriodicWorkRequestBuilder<SeasonalNotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("seasonal_notification")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "seasonal_notification",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun scheduleSmartInsights(context: Context) {
        val delay = calculateDelayUntil(14, 0) // 2:00 PM

        val request = PeriodicWorkRequestBuilder<SmartInsightWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("smart_insight")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "smart_insight",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun calculateDelayUntil(targetHour: Int, targetMinute: Int): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // If target time has passed today, schedule for tomorrow
        if (currentHour > targetHour || (currentHour == targetHour && currentMinute >= targetMinute)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, targetMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis - System.currentTimeMillis()
    }

    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    /**
     * Trigger immediate test notification (for testing purposes)
     */
    fun triggerTestNotification(context: Context, type: String) {
        val oneTimeRequest = when (type) {
            "morning" -> OneTimeWorkRequestBuilder<MorningGreetingWorker>().build()
            "evening" -> OneTimeWorkRequestBuilder<EveningReminderWorker>().build()
            "bills" -> OneTimeWorkRequestBuilder<BillReminderWorker>().build()
            "weekly" -> OneTimeWorkRequestBuilder<WeeklyInsightWorker>().build()
            "seasonal" -> OneTimeWorkRequestBuilder<SeasonalNotificationWorker>().build()
            "smart" -> OneTimeWorkRequestBuilder<SmartInsightWorker>().build()
            else -> return
        }

        WorkManager.getInstance(context).enqueue(oneTimeRequest)
    }
}

