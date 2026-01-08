package com.example.budgie.security

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * InactivityTimer - Monitors user activity and triggers auto-lock after inactivity
 *
 * Features:
 * - Detects 1 minute of inactivity
 * - Shows 10-second countdown warning dialog
 * - Allows user to cancel auto-lock
 * - Resets timer on any user interaction
 */
class InactivityTimer(
    private val onInactivityWarning: () -> Unit,
    private val onAutoLock: () -> Unit
) {
    companion object {
        private const val TAG = "InactivityTimer"
        private const val INACTIVITY_TIMEOUT_MS = 45_000L // 1 minute
        private const val WARNING_COUNTDOWN_MS = 10_000L // 10 seconds
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var inactivityJob: Job? = null
    private var countdownJob: Job? = null

    private val _isWarningShown = MutableStateFlow(false)
    val isWarningShown: StateFlow<Boolean> = _isWarningShown

    private val _countdownSeconds = MutableStateFlow(10)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds

    private var isEnabled = false
    private var isPaused = false

    /**
     * Start monitoring for inactivity
     */
    fun start() {
        if (isEnabled) return

        isEnabled = true
        isPaused = false
        Log.d(TAG, "✅ Inactivity timer STARTED")
        resetTimer()
    }

    /**
     * Stop monitoring (when app is backgrounded or user logs out)
     */
    fun stop() {
        isEnabled = false
        cancelAllTimers()
        Log.d(TAG, "🛑 Inactivity timer STOPPED")
    }

    /**
     * Pause monitoring temporarily (e.g., during video playback)
     */
    fun pause() {
        isPaused = true
        cancelAllTimers()
        Log.d(TAG, "⏸️ Inactivity timer PAUSED")
    }

    /**
     * Resume monitoring after pause
     */
    fun resume() {
        if (!isEnabled) return

        isPaused = false
        Log.d(TAG, "▶️ Inactivity timer RESUMED")
        resetTimer()
    }

    /**
     * Reset the inactivity timer (call on user interaction)
     */
    fun resetTimer() {
        if (!isEnabled || isPaused) return

        // Cancel existing timers
        inactivityJob?.cancel()
        countdownJob?.cancel()
        _isWarningShown.value = false

        // Start new inactivity timer
        inactivityJob = scope.launch {
            delay(INACTIVITY_TIMEOUT_MS)

            // Inactivity timeout reached - show warning
            if (isActive && isEnabled && !isPaused) {
                Log.d(TAG, "⚠️ INACTIVITY DETECTED - Starting 10s countdown")
                showWarning()
            }
        }
    }

    /**
     * Show warning dialog and start countdown
     */
    private suspend fun showWarning() {
        _isWarningShown.value = true
        _countdownSeconds.value = 10

        withContext(Dispatchers.Main) {
            onInactivityWarning()
        }

        // Start countdown
        countdownJob = scope.launch {
            repeat(10) { i ->
                if (!isActive) return@launch

                delay(1000L)
                _countdownSeconds.value = 10 - i - 1

                Log.d(TAG, "⏱️ Auto-lock countdown: ${10 - i - 1} seconds")
            }

            // Countdown finished - trigger auto-lock
            if (isActive && _isWarningShown.value) {
                Log.d(TAG, "🔒 AUTO-LOCK TRIGGERED")
                withContext(Dispatchers.Main) {
                    onAutoLock()
                }
                _isWarningShown.value = false
            }
        }
    }

    /**
     * Cancel auto-lock (user clicked "Stay Active")
     */
    fun cancelAutoLock() {
        Log.d(TAG, "❌ Auto-lock CANCELLED by user")
        countdownJob?.cancel()
        _isWarningShown.value = false
        resetTimer() // Start fresh timer
    }

    /**
     * Proceed with auto-lock immediately (user clicked "Lock Now")
     */
    fun lockNow() {
        Log.d(TAG, "🔒 Lock NOW triggered by user")
        cancelAllTimers()
        _isWarningShown.value = false
        scope.launch(Dispatchers.Main) {
            onAutoLock()
        }
    }

    /**
     * Cancel all running timers
     */
    private fun cancelAllTimers() {
        inactivityJob?.cancel()
        countdownJob?.cancel()
        _isWarningShown.value = false
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stop()
        scope.cancel()
        Log.d(TAG, "🧹 InactivityTimer cleaned up")
    }
}

