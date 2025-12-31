package com.example.budgie.ui.screens.lockscreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE DIMENSIONS SYSTEM
   Adapts to all screen sizes from compact phones to tablets
═══════════════════════════════════════════════════════════════════ */

enum class LockScreenSize {
    COMPACT,    // Small phones (< 360dp width, < 640dp height)
    MEDIUM,     // Regular phones (360-400dp width, 640-800dp height)
    LARGE,      // Large phones/Small tablets (> 400dp width, > 800dp height)
    EXPANDED    // Tablets (> 600dp width)
}

data class LockScreenDimens(
    // Main layout
    val horizontalPadding: Dp,
    val verticalSpacing: Dp,
    val cardMaxWidth: Dp,
    val cardPadding: Dp,
    val cardCornerRadius: Dp,

    // Shield/Icon dimensions
    val shieldContainerSize: Dp,
    val shieldOuterRingSize: Dp,
    val shieldInnerRingSize: Dp,
    val shieldIconContainerSize: Dp,
    val shieldIconSize: Dp,

    // Typography
    val titleSize: Int,
    val subtitleSize: Int,
    val labelSize: Int,
    val subtitleLetterSpacing: Float,

    // PIN Input
    val pinDotSize: Dp,
    val pinDotSpacing: Dp,
    val pinKeySize: Dp,
    val pinKeySpacing: Dp,
    val pinKeyFontSize: Int,

    // Biometric button
    val biometricButtonWidth: Float,
    val biometricButtonHeight: Dp,
    val biometricButtonCornerRadius: Dp,
    val biometricIconSize: Dp,
    val biometricFontSize: Int,

    // AI Status indicator
    val aiDotSize: Dp,
    val aiTextSize: Int,
    val aiLetterSpacing: Float,

    // Trust badge
    val trustBadgePaddingHorizontal: Dp,
    val trustBadgePaddingVertical: Dp,
    val trustBadgeIconSize: Dp,
    val trustBadgeFontSize: Int,

    // Spacing
    val topToAiStatus: Dp,
    val aiStatusToCard: Dp,
    val shieldToTitle: Dp,
    val titleToSubtitle: Dp,
    val subtitleToInput: Dp,
    val cardToTrustBadge: Dp,
    val errorMessagePadding: Dp
)

@Composable
fun getLockScreenSize(): LockScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    return when {
        screenWidthDp >= 600 -> LockScreenSize.EXPANDED
        screenWidthDp > 400 && screenHeightDp > 800 -> LockScreenSize.LARGE
        screenWidthDp >= 360 && screenHeightDp >= 640 -> LockScreenSize.MEDIUM
        else -> LockScreenSize.COMPACT
    }
}

@Composable
fun getLockScreenDimens(): LockScreenDimens {
    val screenSize = getLockScreenSize()

    return when (screenSize) {
        LockScreenSize.COMPACT -> compactDimens()
        LockScreenSize.MEDIUM -> mediumDimens()
        LockScreenSize.LARGE -> largeDimens()
        LockScreenSize.EXPANDED -> expandedDimens()
    }
}

private fun compactDimens() = LockScreenDimens(
    horizontalPadding = 16.dp,
    verticalSpacing = 8.dp,
    cardMaxWidth = 300.dp,
    cardPadding = 20.dp,
    cardCornerRadius = 20.dp,
    shieldContainerSize = 100.dp,
    shieldOuterRingSize = 95.dp,
    shieldInnerRingSize = 80.dp,
    shieldIconContainerSize = 65.dp,
    shieldIconSize = 28.dp,
    titleSize = 18,
    subtitleSize = 10,
    labelSize = 10,
    subtitleLetterSpacing = 1.2f,
    pinDotSize = 12.dp,
    pinDotSpacing = 10.dp,
    pinKeySize = 48.dp,
    pinKeySpacing = 12.dp,
    pinKeyFontSize = 18,
    biometricButtonWidth = 0.7f,
    biometricButtonHeight = 42.dp,
    biometricButtonCornerRadius = 10.dp,
    biometricIconSize = 18.dp,
    biometricFontSize = 14,
    aiDotSize = 5.dp,
    aiTextSize = 9,
    aiLetterSpacing = 1.5f,
    trustBadgePaddingHorizontal = 12.dp,
    trustBadgePaddingVertical = 6.dp,
    trustBadgeIconSize = 12.dp,
    trustBadgeFontSize = 10,
    topToAiStatus = 16.dp,
    aiStatusToCard = 24.dp,
    shieldToTitle = 20.dp,
    titleToSubtitle = 6.dp,
    subtitleToInput = 20.dp,
    cardToTrustBadge = 20.dp,
    errorMessagePadding = 12.dp
)

private fun mediumDimens() = LockScreenDimens(
    horizontalPadding = 24.dp,
    verticalSpacing = 12.dp,
    cardMaxWidth = 340.dp,
    cardPadding = 28.dp,
    cardCornerRadius = 24.dp,
    shieldContainerSize = 120.dp,
    shieldOuterRingSize = 115.dp,
    shieldInnerRingSize = 95.dp,
    shieldIconContainerSize = 78.dp,
    shieldIconSize = 34.dp,
    titleSize = 20,
    subtitleSize = 11,
    labelSize = 11,
    subtitleLetterSpacing = 1.3f,
    pinDotSize = 14.dp,
    pinDotSpacing = 12.dp,
    pinKeySize = 56.dp,
    pinKeySpacing = 16.dp,
    pinKeyFontSize = 20,
    biometricButtonWidth = 0.55f,
    biometricButtonHeight = 46.dp,
    biometricButtonCornerRadius = 11.dp,
    biometricIconSize = 20.dp,
    biometricFontSize = 15,
    aiDotSize = 6.dp,
    aiTextSize = 10,
    aiLetterSpacing = 1.8f,
    trustBadgePaddingHorizontal = 14.dp,
    trustBadgePaddingVertical = 7.dp,
    trustBadgeIconSize = 13.dp,
    trustBadgeFontSize = 11,
    topToAiStatus = 24.dp,
    aiStatusToCard = 32.dp,
    shieldToTitle = 24.dp,
    titleToSubtitle = 7.dp,
    subtitleToInput = 26.dp,
    cardToTrustBadge = 26.dp,
    errorMessagePadding = 14.dp
)

private fun largeDimens() = LockScreenDimens(
    horizontalPadding = 32.dp,
    verticalSpacing = 16.dp,
    cardMaxWidth = 380.dp,
    cardPadding = 32.dp,
    cardCornerRadius = 28.dp,
    shieldContainerSize = 140.dp,
    shieldOuterRingSize = 130.dp,
    shieldInnerRingSize = 110.dp,
    shieldIconContainerSize = 90.dp,
    shieldIconSize = 40.dp,
    titleSize = 22,
    subtitleSize = 12,
    labelSize = 12,
    subtitleLetterSpacing = 1.5f,
    pinDotSize = 16.dp,
    pinDotSpacing = 16.dp,
    pinKeySize = 64.dp,
    pinKeySpacing = 20.dp,
    pinKeyFontSize = 22,
    biometricButtonWidth = 0.50f,
    biometricButtonHeight = 48.dp,
    biometricButtonCornerRadius = 12.dp,
    biometricIconSize = 22.dp,
    biometricFontSize = 16,
    aiDotSize = 6.dp,
    aiTextSize = 11,
    aiLetterSpacing = 2f,
    trustBadgePaddingHorizontal = 16.dp,
    trustBadgePaddingVertical = 8.dp,
    trustBadgeIconSize = 14.dp,
    trustBadgeFontSize = 12,
    topToAiStatus = 32.dp,
    aiStatusToCard = 40.dp,
    shieldToTitle = 28.dp,
    titleToSubtitle = 8.dp,
    subtitleToInput = 32.dp,
    cardToTrustBadge = 32.dp,
    errorMessagePadding = 16.dp
)

private fun expandedDimens() = LockScreenDimens(
    horizontalPadding = 48.dp,
    verticalSpacing = 20.dp,
    cardMaxWidth = 450.dp,
    cardPadding = 40.dp,
    cardCornerRadius = 32.dp,
    shieldContainerSize = 160.dp,
    shieldOuterRingSize = 150.dp,
    shieldInnerRingSize = 125.dp,
    shieldIconContainerSize = 100.dp,
    shieldIconSize = 48.dp,
    titleSize = 26,
    subtitleSize = 14,
    labelSize = 14,
    subtitleLetterSpacing = 1.8f,
    pinDotSize = 18.dp,
    pinDotSpacing = 20.dp,
    pinKeySize = 72.dp,
    pinKeySpacing = 24.dp,
    pinKeyFontSize = 26,
    biometricButtonWidth = 0.45f,
    biometricButtonHeight = 56.dp,
    biometricButtonCornerRadius = 14.dp,
    biometricIconSize = 26.dp,
    biometricFontSize = 18,
    aiDotSize = 8.dp,
    aiTextSize = 13,
    aiLetterSpacing = 2.2f,
    trustBadgePaddingHorizontal = 20.dp,
    trustBadgePaddingVertical = 10.dp,
    trustBadgeIconSize = 16.dp,
    trustBadgeFontSize = 13,
    topToAiStatus = 40.dp,
    aiStatusToCard = 48.dp,
    shieldToTitle = 32.dp,
    titleToSubtitle = 10.dp,
    subtitleToInput = 40.dp,
    cardToTrustBadge = 40.dp,
    errorMessagePadding = 20.dp
)

