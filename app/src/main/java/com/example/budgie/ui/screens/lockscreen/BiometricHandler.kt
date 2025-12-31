package com.example.budgie.ui.screens.lockscreen

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.budgie.security.SecurityManager

/* ═══════════════════════════════════════════════════════════════════
   BIOMETRIC AUTHENTICATION HANDLER
   Manages biometric prompt and authentication flow
═══════════════════════════════════════════════════════════════════ */

/**
 * Shows the biometric authentication prompt
 */
fun showBiometricPrompt(
    activity: FragmentActivity,
    securityManager: SecurityManager,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFailed: () -> Unit
) {
    val manager = BiometricManager.from(activity)
    val strongAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

    // Check if any biometric is available
    if (strongAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        val weakAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (weakAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometric authentication not available")
            return
        }
    }

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                securityManager.setAuthenticated(true)
                onSuccess()
            }

            override fun onAuthenticationFailed() = onFailed()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        }
    )

    try {
        val promptInfo = if (strongAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Budgie")
                .setSubtitle("Verify your identity to continue")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Use PIN")
                .build()
        } else {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Budgie")
                .setSubtitle("Verify your identity to continue")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        }

        prompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onError("Authentication error: ${e.message}")
    }
}
