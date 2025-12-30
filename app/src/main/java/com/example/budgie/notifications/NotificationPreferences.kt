package com.example.budgie.notifications

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

/**
 * Sound mode options for notifications
 */
enum class NotificationSoundMode {
    SOUND_ONLY,           // Play sound, no vibration
    SOUND_AND_VIBRATION,  // Play sound and vibrate
    VIBRATION_ONLY,       // No sound, only vibrate
    SILENT                // No sound, no vibration
}

/**
 * Manager for storing and retrieving notification preferences
 */
class NotificationPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "budgie_notification_prefs"

        // Keys
        private const val KEY_ALL_ENABLED = "all_notifications_enabled"
        private const val KEY_SOUND_MODE = "sound_mode"
        private const val KEY_USE_SYSTEM_SOUND = "use_system_sound"
        private const val KEY_CUSTOM_SOUND_URI = "custom_sound_uri"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START_HOUR = "quiet_start_hour"
        private const val KEY_QUIET_START_MINUTE = "quiet_start_minute"
        private const val KEY_QUIET_END_HOUR = "quiet_end_hour"
        private const val KEY_QUIET_END_MINUTE = "quiet_end_minute"

        // Category-specific keys
        private const val KEY_BILLS_ENABLED = "bills_notifications_enabled"
        private const val KEY_BUDGET_ENABLED = "budget_alerts_enabled"
        private const val KEY_GOALS_ENABLED = "goals_reminders_enabled"
        private const val KEY_LOANS_ENABLED = "loans_reminders_enabled"
        private const val KEY_EXPENSE_ENABLED = "expense_alerts_enabled"
        private const val KEY_INCOME_ENABLED = "income_notifications_enabled"
        private const val KEY_SECURITY_ENABLED = "security_alerts_enabled"
        private const val KEY_SYSTEM_ENABLED = "system_notifications_enabled"
        private const val KEY_INSIGHTS_ENABLED = "insights_notifications_enabled"

        // Legacy keys for backward compatibility
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        @Volatile
        private var INSTANCE: NotificationPreferences? = null

        fun getInstance(context: Context): NotificationPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MASTER CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════════

    var allNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ALL_ENABLED, value).apply()

    // ═══════════════════════════════════════════════════════════════════════════════
    // SOUND MODE SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    var soundMode: NotificationSoundMode
        get() {
            val modeString = prefs.getString(KEY_SOUND_MODE, null)
            return if (modeString != null) {
                try {
                    NotificationSoundMode.valueOf(modeString)
                } catch (e: Exception) {
                    // Migrate from old settings
                    migrateFromLegacySettings()
                }
            } else {
                // Migrate from old settings
                migrateFromLegacySettings()
            }
        }
        set(value) = prefs.edit().putString(KEY_SOUND_MODE, value.name).apply()

    private fun migrateFromLegacySettings(): NotificationSoundMode {
        val soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        val vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, false)

        val mode = when {
            soundEnabled && vibrationEnabled -> NotificationSoundMode.SOUND_AND_VIBRATION
            soundEnabled && !vibrationEnabled -> NotificationSoundMode.SOUND_ONLY
            !soundEnabled && vibrationEnabled -> NotificationSoundMode.VIBRATION_ONLY
            else -> NotificationSoundMode.SILENT
        }

        // Save the migrated mode
        soundMode = mode
        return mode
    }

    // Legacy getters/setters for backward compatibility
    var soundEnabled: Boolean
        get() = soundMode == NotificationSoundMode.SOUND_ONLY ||
                soundMode == NotificationSoundMode.SOUND_AND_VIBRATION
        set(value) {
            if (value) {
                if (vibrationEnabled) {
                    soundMode = NotificationSoundMode.SOUND_AND_VIBRATION
                } else {
                    soundMode = NotificationSoundMode.SOUND_ONLY
                }
            } else {
                if (vibrationEnabled) {
                    soundMode = NotificationSoundMode.VIBRATION_ONLY
                } else {
                    soundMode = NotificationSoundMode.SILENT
                }
            }
        }

    var vibrationEnabled: Boolean
        get() = soundMode == NotificationSoundMode.VIBRATION_ONLY ||
                soundMode == NotificationSoundMode.SOUND_AND_VIBRATION
        set(value) {
            if (value) {
                if (soundEnabled) {
                    soundMode = NotificationSoundMode.SOUND_AND_VIBRATION
                } else {
                    soundMode = NotificationSoundMode.VIBRATION_ONLY
                }
            } else {
                if (soundEnabled) {
                    soundMode = NotificationSoundMode.SOUND_ONLY
                } else {
                    soundMode = NotificationSoundMode.SILENT
                }
            }
        }

    var useSystemSound: Boolean
        get() = prefs.getBoolean(KEY_USE_SYSTEM_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_SYSTEM_SOUND, value).apply()

    var customSoundUri: Uri?
        get() {
            val uriString = prefs.getString(KEY_CUSTOM_SOUND_URI, null)
            return uriString?.let { Uri.parse(it) }
        }
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_SOUND_URI, value?.toString()).apply()
        }

    // ═══════════════════════════════════════════════════════════════════════════════
    // QUIET HOURS SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, value).apply()

    var quietStartHour: Int
        get() = prefs.getInt(KEY_QUIET_START_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_QUIET_START_HOUR, value).apply()

    var quietStartMinute: Int
        get() = prefs.getInt(KEY_QUIET_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_QUIET_START_MINUTE, value).apply()

    var quietEndHour: Int
        get() = prefs.getInt(KEY_QUIET_END_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_QUIET_END_HOUR, value).apply()

    var quietEndMinute: Int
        get() = prefs.getInt(KEY_QUIET_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_QUIET_END_MINUTE, value).apply()

    /**
     * Check if current time is within quiet hours
     */
    fun isInQuietHours(): Boolean {
        if (!quietHoursEnabled) return false

        val now = java.util.Calendar.getInstance()
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        val startTotalMinutes = quietStartHour * 60 + quietStartMinute
        val endTotalMinutes = quietEndHour * 60 + quietEndMinute

        return if (startTotalMinutes <= endTotalMinutes) {
            currentTotalMinutes in startTotalMinutes until endTotalMinutes
        } else {
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CATEGORY-SPECIFIC SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════════

    var billsNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BILLS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BILLS_ENABLED, value).apply()

    var budgetAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BUDGET_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BUDGET_ENABLED, value).apply()

    var goalsRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_GOALS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GOALS_ENABLED, value).apply()

    var loansRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOANS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LOANS_ENABLED, value).apply()

    var expenseAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXPENSE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_EXPENSE_ENABLED, value).apply()

    var incomeNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_INCOME_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_INCOME_ENABLED, value).apply()

    var securityAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SECURITY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SECURITY_ENABLED, value).apply()

    var systemNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYSTEM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SYSTEM_ENABLED, value).apply()

    var insightsNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_INSIGHTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_INSIGHTS_ENABLED, value).apply()

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    fun enableAllCategories() {
        billsNotificationsEnabled = true
        budgetAlertsEnabled = true
        goalsRemindersEnabled = true
        loansRemindersEnabled = true
        expenseAlertsEnabled = true
        incomeNotificationsEnabled = true
        securityAlertsEnabled = true
        systemNotificationsEnabled = true
        insightsNotificationsEnabled = true
        allNotificationsEnabled = true
    }

    fun disableAllCategories() {
        billsNotificationsEnabled = false
        budgetAlertsEnabled = false
        goalsRemindersEnabled = false
        loansRemindersEnabled = false
        expenseAlertsEnabled = false
        incomeNotificationsEnabled = false
        securityAlertsEnabled = false
        systemNotificationsEnabled = false
        insightsNotificationsEnabled = false
        allNotificationsEnabled = false
    }

    /**
     * Get the notification sound URI to use
     * Returns custom sound if set and not using system sound, otherwise null (uses system default)
     */
    fun getNotificationSoundUri(): Uri? {
        // If sound is not enabled in current mode, return null
        if (soundMode == NotificationSoundMode.VIBRATION_ONLY ||
            soundMode == NotificationSoundMode.SILENT) {
            return null
        }

        // If using system sound, return null to use default
        if (useSystemSound) return null

        // Return custom sound URI
        return customSoundUri
    }

    /**
     * Check if notifications should make sound based on settings and quiet hours
     */
    fun shouldPlaySound(): Boolean {
        if (isInQuietHours()) return false
        return soundMode == NotificationSoundMode.SOUND_ONLY ||
               soundMode == NotificationSoundMode.SOUND_AND_VIBRATION
    }

    /**
     * Check if notifications should vibrate based on settings and quiet hours
     */
    fun shouldVibrate(): Boolean {
        if (isInQuietHours()) return false
        return soundMode == NotificationSoundMode.VIBRATION_ONLY ||
               soundMode == NotificationSoundMode.SOUND_AND_VIBRATION
    }
}

