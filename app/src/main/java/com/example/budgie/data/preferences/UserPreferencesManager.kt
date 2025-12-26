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

