package com.example.budgie.data.preferences

import android.content.Context
import com.example.budgie.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserPreferencesManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _userProfile = MutableStateFlow<UserProfile?>(loadUserProfile())
    val userProfile: Flow<UserProfile?> = _userProfile.asStateFlow()

    fun saveUserProfile(profile: UserProfile) {
        val jsonString = json.encodeToString(profile)
        preferences.edit()
            .putString(KEY_USER_PROFILE, jsonString)
            .apply()
        _userProfile.value = profile
    }

    fun getUserProfile(): UserProfile? {
        return loadUserProfile()
    }

    fun hasCompletedOnboarding(): Boolean {
        return getUserProfile() != null
    }

    /**
     * Check if birthday celebration was shown this year
     */
    fun wasBirthdayCelebrationShownThisYear(): Boolean {
        val lastShownYear = preferences.getInt(KEY_BIRTHDAY_SHOWN_YEAR, 0)
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return lastShownYear == currentYear
    }

    /**
     * Mark birthday celebration as shown for this year
     */
    fun markBirthdayCelebrationShown() {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        preferences.edit()
            .putInt(KEY_BIRTHDAY_SHOWN_YEAR, currentYear)
            .apply()
    }

    /**
     * Reset birthday celebration flag (for testing or next year)
     */
    fun resetBirthdayCelebration() {
        preferences.edit()
            .remove(KEY_BIRTHDAY_SHOWN_YEAR)
            .apply()
    }

    // ========== NOTIFICATION SETTINGS ==========

    fun getNotificationsEnabled(): Boolean = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getDailyRemindersEnabled(): Boolean = preferences.getBoolean(KEY_DAILY_REMINDERS, true)
    fun setDailyRemindersEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DAILY_REMINDERS, enabled).apply()
    }

    fun getBillRemindersEnabled(): Boolean = preferences.getBoolean(KEY_BILL_REMINDERS, true)
    fun setBillRemindersEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BILL_REMINDERS, enabled).apply()
    }

    fun getDailyInsightsEnabled(): Boolean = preferences.getBoolean(KEY_DAILY_INSIGHTS, true)
    fun setDailyInsightsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DAILY_INSIGHTS, enabled).apply()
    }

    // ========== CURRENCY SETTINGS ==========

    fun getCurrencySymbol(): String = preferences.getString(KEY_CURRENCY_SYMBOL, "$") ?: "$"
    fun setCurrencySymbol(symbol: String) {
        preferences.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    // ========== AUTO-LOCK SETTINGS ==========

    fun getAutoLockEnabled(): Boolean = preferences.getBoolean(KEY_AUTO_LOCK_ENABLED, true)
    fun setAutoLockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_LOCK_ENABLED, enabled).apply()
    }

    // ========== CLEAR ALL PREFERENCES ==========

    fun clearAllPreferences() {
        preferences.edit().clear().apply()
        _userProfile.value = null
    }

    private fun loadUserProfile(): UserProfile? {
        val jsonString = preferences.getString(KEY_USER_PROFILE, null)
        return jsonString?.let {
            try {
                json.decodeFromString<UserProfile>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "budgie_user_prefs"
        private const val KEY_USER_PROFILE = "user_profile"
        private const val KEY_BIRTHDAY_SHOWN_YEAR = "birthday_celebration_shown_year"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DAILY_REMINDERS = "daily_reminders_enabled"
        private const val KEY_BILL_REMINDERS = "bill_reminders_enabled"
        private const val KEY_DAILY_INSIGHTS = "daily_insights_enabled"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_AUTO_LOCK_ENABLED = "auto_lock_enabled"

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

