package com.example.budgie.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class SecurityManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "budgie_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_SECURITY_TYPE = "security_type"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"

        const val SECURITY_NONE = "none"
        const val SECURITY_PIN = "pin"
        const val SECURITY_BIOMETRIC = "biometric"
        const val SECURITY_PIN_BIOMETRIC = "pin_biometric"
    }

    // Save security type (none, pin, or biometric)
    fun setSecurityType(type: String) {
        encryptedPrefs.edit().putString(KEY_SECURITY_TYPE, type).apply()
    }

    fun getSecurityType(): String {
        return encryptedPrefs.getString(KEY_SECURITY_TYPE, SECURITY_NONE) ?: SECURITY_NONE
    }


    // Save PIN (hashed)
    fun savePin(pin: String) {
        val hashedPin = hashPin(pin)
        encryptedPrefs.edit().putString(KEY_PIN_HASH, hashedPin).apply()
    }

    // Verify PIN
    fun verifyPin(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_PIN_HASH, null) ?: return false
        val enteredHash = hashPin(pin)
        return storedHash == enteredHash
    }

    // Hash PIN using SHA-256
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Session management
    fun setAuthenticated(authenticated: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply()
    }

    fun isAuthenticated(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_AUTHENTICATED, false)
    }

    // Destroy session (on app exit)
    fun destroySession() {
        setAuthenticated(false)
    }

    // Clear security settings
    fun clearSecurity() {
        encryptedPrefs.edit().clear().apply()
    }
}

