package com.example.budgie.ui.screens

// Re-export from onboarding package for backward compatibility
import com.example.budgie.ui.screens.onboarding.OnboardingScreen as OnboardingScreenImpl

import androidx.compose.runtime.Composable

/**
 * Onboarding Screen - Re-exported for backward compatibility
 * The actual implementation is in the onboarding package with smaller files:
 * - OnboardingDimens.kt - Responsive dimensions
 * - OnboardingUtils.kt - PIN strength, date utilities
 * - OnboardingComponents.kt - Reusable UI components
 * - OnboardingAnimations.kt - Animated background
 * - OnboardingBirthdayPicker.kt - Birthday picker component
 * - OnboardingSecuritySection.kt - Security selection
 * - OnboardingDialogs.kt - Terms, Privacy dialogs
 * - OnboardingMainScreen.kt - Main screen composition
 */
@Composable
fun OnboardingScreen(
    onComplete: (name: String, birthday: String, securityType: String, pin: String) -> Unit
) {
    OnboardingScreenImpl(onComplete = onComplete)
}

