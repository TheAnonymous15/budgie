package com.example.budgie.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen size categories for responsive design
 */
enum class ScreenSize {
    COMPACT,    // < 360dp width (small phones)
    MEDIUM,     // 360dp - 411dp (most phones)
    EXPANDED    // > 411dp (large phones, tablets)
}

/**
 * Get current screen size category based on screen width
 */
@Composable
fun getScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth < 360 -> ScreenSize.COMPACT
        screenWidth <= 411 -> ScreenSize.MEDIUM
        else -> ScreenSize.EXPANDED
    }
}

/**
 * Responsive spacing and sizing values based on screen size
 */
data class ResponsiveDimens(
    val horizontalPadding: Dp,
    val verticalSpacing: Dp,
    val sectionSpacing: Dp,
    val cardPadding: Dp,
    val logoSize: Dp,
    val logoContainerSize: Dp,
    val titleFontSize: Int,
    val subtitleFontSize: Int,
    val bodyFontSize: Int,
    val labelFontSize: Int,
    val iconSize: Dp,
    val smallIconSize: Dp,
    val buttonHeight: Dp,
    val inputHeight: Dp,
    val cornerRadius: Dp,
    val topSpacing: Dp,
    val chipPadding: Dp,
    val badgeTextSize: Int
)

/**
 * Get responsive dimensions based on screen size
 */
@Composable
fun getResponsiveDimens(): ResponsiveDimens {
    val screenSize = getScreenSize()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp

    // Also factor in screen height for very short screens
    val isShortScreen = screenHeight < 700

    return when (screenSize) {
        ScreenSize.COMPACT -> ResponsiveDimens(
            horizontalPadding = 12.dp,
            verticalSpacing = 8.dp,
            sectionSpacing = 12.dp,
            cardPadding = 14.dp,
            logoSize = if (isShortScreen) 60.dp else 70.dp,
            logoContainerSize = if (isShortScreen) 80.dp else 95.dp,
            titleFontSize = if (isShortScreen) 22 else 24,
            subtitleFontSize = if (isShortScreen) 13 else 14,
            bodyFontSize = 12,
            labelFontSize = 10,
            iconSize = 18.dp,
            smallIconSize = 14.dp,
            buttonHeight = 44.dp,
            inputHeight = 48.dp,
            cornerRadius = 12.dp,
            topSpacing = if (isShortScreen) 20.dp else 30.dp,
            chipPadding = 6.dp,
            badgeTextSize = 8
        )
        ScreenSize.MEDIUM -> ResponsiveDimens(
            horizontalPadding = 16.dp,
            verticalSpacing = 12.dp,
            sectionSpacing = 16.dp,
            cardPadding = 18.dp,
            logoSize = if (isShortScreen) 75.dp else 85.dp,
            logoContainerSize = if (isShortScreen) 100.dp else 115.dp,
            titleFontSize = if (isShortScreen) 26 else 28,
            subtitleFontSize = if (isShortScreen) 14 else 16,
            bodyFontSize = 14,
            labelFontSize = 11,
            iconSize = 20.dp,
            smallIconSize = 16.dp,
            buttonHeight = 48.dp,
            inputHeight = 52.dp,
            cornerRadius = 14.dp,
            topSpacing = if (isShortScreen) 30.dp else 45.dp,
            chipPadding = 7.dp,
            badgeTextSize = 9
        )
        ScreenSize.EXPANDED -> ResponsiveDimens(
            horizontalPadding = 20.dp,
            verticalSpacing = 14.dp,
            sectionSpacing = 20.dp,
            cardPadding = 20.dp,
            logoSize = 90.dp,
            logoContainerSize = 120.dp,
            titleFontSize = 30,
            subtitleFontSize = 17,
            bodyFontSize = 15,
            labelFontSize = 12,
            iconSize = 22.dp,
            smallIconSize = 18.dp,
            buttonHeight = 52.dp,
            inputHeight = 56.dp,
            cornerRadius = 16.dp,
            topSpacing = 50.dp,
            chipPadding = 8.dp,
            badgeTextSize = 10
        )
    }
}

