package com.example.budgie.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE DIMENSIONS SYSTEM FOR DASHBOARD
   Adapts to all screen sizes from compact phones to tablets
═══════════════════════════════════════════════════════════════════ */

enum class DashboardScreenSize {
    COMPACT,    // Small phones (< 360dp width, < 640dp height)
    MEDIUM,     // Regular phones (360-400dp width, 640-800dp height)
    LARGE,      // Large phones/Small tablets (> 400dp width, > 800dp height)
    EXPANDED    // Tablets (> 600dp width)
}

data class DashboardDimens(
    // Main layout
    val horizontalPadding: Dp,
    val verticalSpacing: Dp,
    val sectionSpacing: Dp,
    val contentPadding: Dp,

    // Header dimensions
    val headerPaddingHorizontal: Dp,
    val headerPaddingVertical: Dp,
    val headerCornerRadius: Dp,
    val greetingFontSize: Int,
    val subtitleFontSize: Int,

    // Card dimensions
    val cardCornerRadius: Dp,
    val cardPadding: Dp,
    val cardElevation: Dp,
    val cardBorderWidth: Dp,
    val smallCardPadding: Dp,

    // Net Savings Card
    val netSavingsCardPadding: Dp,
    val netSavingsAmountSize: Int,
    val netSavingsLabelSize: Int,

    // Metric Cards
    val metricCardHeight: Dp,
    val metricCardPadding: Dp,
    val metricIconSize: Dp,
    val metricValueSize: Int,
    val metricLabelSize: Int,

    // Action Buttons
    val actionButtonHeight: Dp,
    val actionButtonCornerRadius: Dp,
    val actionButtonIconSize: Dp,
    val actionButtonFontSize: Int,
    val actionButtonSpacing: Dp,

    // Grid Action Buttons
    val gridButtonHeight: Dp,
    val gridButtonIconSize: Dp,
    val gridButtonFontSize: Int,

    // FAB dimensions
    val fabSize: Dp,
    val fabIconSize: Dp,
    val miniFabSize: Dp,
    val miniFabIconSize: Dp,

    // Transaction/Bill Items
    val itemCardHeight: Dp,
    val itemCardPadding: Dp,
    val itemIconSize: Dp,
    val itemTitleSize: Int,
    val itemSubtitleSize: Int,
    val itemAmountSize: Int,

    // Insights Card
    val insightCardPadding: Dp,
    val insightIconSize: Dp,
    val insightTitleSize: Int,
    val insightMessageSize: Int,

    // Section Headers
    val sectionIconSize: Dp,
    val sectionTitleSize: Int,
    val sectionIconContainerSize: Dp,

    // Empty State
    val emptyStateIconSize: Dp,
    val emptyStateTitleSize: Int,
    val emptyStateSubtitleSize: Int,

    // Dialog
    val dialogWidth: Float,
    val dialogPadding: Dp,
    val dialogIconSize: Dp,
    val dialogTitleSize: Int,
    val dialogMessageSize: Int,
    val dialogButtonHeight: Dp,

    // Menu
    val menuWidth: Dp,
    val menuItemIconSize: Dp,
    val menuItemFontSize: Int,

    // Top Bar
    val topBarLogoSize: Dp,
    val topBarTitleSize: Int,
    val notificationBadgeSize: Int,

    // Income Dialog
    val incomeDialogInputHeight: Dp,

    // Charts/Graphs
    val chartHeight: Dp,
    val chartBarWidth: Dp
)

@Composable
fun getDashboardScreenSize(): DashboardScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    return when {
        screenWidthDp >= 600 -> DashboardScreenSize.EXPANDED
        screenWidthDp > 400 && screenHeightDp > 800 -> DashboardScreenSize.LARGE
        screenWidthDp >= 360 && screenHeightDp >= 640 -> DashboardScreenSize.MEDIUM
        else -> DashboardScreenSize.COMPACT
    }
}

@Composable
fun getDashboardDimens(): DashboardDimens {
    val screenSize = getDashboardScreenSize()

    return when (screenSize) {
        DashboardScreenSize.COMPACT -> compactDimens()
        DashboardScreenSize.MEDIUM -> mediumDimens()
        DashboardScreenSize.LARGE -> largeDimens()
        DashboardScreenSize.EXPANDED -> expandedDimens()
    }
}

private fun compactDimens() = DashboardDimens(
    // Main layout
    horizontalPadding = 12.dp,
    verticalSpacing = 12.dp,
    sectionSpacing = 16.dp,
    contentPadding = 12.dp,

    // Header
    headerPaddingHorizontal = 14.dp,
    headerPaddingVertical = 16.dp,
    headerCornerRadius = 24.dp,
    greetingFontSize = 18,
    subtitleFontSize = 12,

    // Cards
    cardCornerRadius = 14.dp,
    cardPadding = 12.dp,
    cardElevation = 8.dp,
    cardBorderWidth = 1.dp,
    smallCardPadding = 10.dp,

    // Net Savings
    netSavingsCardPadding = 14.dp,
    netSavingsAmountSize = 24,
    netSavingsLabelSize = 11,

    // Metric Cards
    metricCardHeight = 80.dp,
    metricCardPadding = 10.dp,
    metricIconSize = 20.dp,
    metricValueSize = 16,
    metricLabelSize = 10,

    // Action Buttons
    actionButtonHeight = 38.dp,
    actionButtonCornerRadius = 10.dp,
    actionButtonIconSize = 16.dp,
    actionButtonFontSize = 11,
    actionButtonSpacing = 8.dp,

    // Grid Buttons
    gridButtonHeight = 56.dp,
    gridButtonIconSize = 20.dp,
    gridButtonFontSize = 10,

    // FAB
    fabSize = 48.dp,
    fabIconSize = 22.dp,
    miniFabSize = 40.dp,
    miniFabIconSize = 18.dp,

    // Transaction Items
    itemCardHeight = 64.dp,
    itemCardPadding = 12.dp,
    itemIconSize = 36.dp,
    itemTitleSize = 13,
    itemSubtitleSize = 11,
    itemAmountSize = 14,

    // Insights
    insightCardPadding = 12.dp,
    insightIconSize = 18.dp,
    insightTitleSize = 12,
    insightMessageSize = 11,

    // Section Headers
    sectionIconSize = 16.dp,
    sectionTitleSize = 14,
    sectionIconContainerSize = 28.dp,

    // Empty State
    emptyStateIconSize = 40.dp,
    emptyStateTitleSize = 14,
    emptyStateSubtitleSize = 12,

    // Dialog
    dialogWidth = 0.9f,
    dialogPadding = 16.dp,
    dialogIconSize = 48.dp,
    dialogTitleSize = 16,
    dialogMessageSize = 11,
    dialogButtonHeight = 38.dp,

    // Menu
    menuWidth = 180.dp,
    menuItemIconSize = 18.dp,
    menuItemFontSize = 13,

    // Top Bar
    topBarLogoSize = 32.dp,
    topBarTitleSize = 18,
    notificationBadgeSize = 9,

    // Income Dialog
    incomeDialogInputHeight = 48.dp,

    // Charts
    chartHeight = 160.dp,
    chartBarWidth = 24.dp
)

private fun mediumDimens() = DashboardDimens(
    // Main layout
    horizontalPadding = 16.dp,
    verticalSpacing = 14.dp,
    sectionSpacing = 20.dp,
    contentPadding = 16.dp,

    // Header
    headerPaddingHorizontal = 18.dp,
    headerPaddingVertical = 20.dp,
    headerCornerRadius = 28.dp,
    greetingFontSize = 20,
    subtitleFontSize = 13,

    // Cards
    cardCornerRadius = 16.dp,
    cardPadding = 16.dp,
    cardElevation = 12.dp,
    cardBorderWidth = 1.dp,
    smallCardPadding = 12.dp,

    // Net Savings
    netSavingsCardPadding = 18.dp,
    netSavingsAmountSize = 28,
    netSavingsLabelSize = 12,

    // Metric Cards
    metricCardHeight = 88.dp,
    metricCardPadding = 12.dp,
    metricIconSize = 22.dp,
    metricValueSize = 18,
    metricLabelSize = 11,

    // Action Buttons
    actionButtonHeight = 42.dp,
    actionButtonCornerRadius = 12.dp,
    actionButtonIconSize = 18.dp,
    actionButtonFontSize = 12,
    actionButtonSpacing = 10.dp,

    // Grid Buttons
    gridButtonHeight = 64.dp,
    gridButtonIconSize = 22.dp,
    gridButtonFontSize = 11,

    // FAB
    fabSize = 54.dp,
    fabIconSize = 24.dp,
    miniFabSize = 44.dp,
    miniFabIconSize = 20.dp,

    // Transaction Items
    itemCardHeight = 72.dp,
    itemCardPadding = 14.dp,
    itemIconSize = 40.dp,
    itemTitleSize = 14,
    itemSubtitleSize = 12,
    itemAmountSize = 15,

    // Insights
    insightCardPadding = 14.dp,
    insightIconSize = 20.dp,
    insightTitleSize = 13,
    insightMessageSize = 12,

    // Section Headers
    sectionIconSize = 18.dp,
    sectionTitleSize = 15,
    sectionIconContainerSize = 32.dp,

    // Empty State
    emptyStateIconSize = 48.dp,
    emptyStateTitleSize = 15,
    emptyStateSubtitleSize = 13,

    // Dialog
    dialogWidth = 0.88f,
    dialogPadding = 20.dp,
    dialogIconSize = 56.dp,
    dialogTitleSize = 18,
    dialogMessageSize = 12,
    dialogButtonHeight = 42.dp,

    // Menu
    menuWidth = 200.dp,
    menuItemIconSize = 20.dp,
    menuItemFontSize = 14,

    // Top Bar
    topBarLogoSize = 36.dp,
    topBarTitleSize = 20,
    notificationBadgeSize = 10,

    // Income Dialog
    incomeDialogInputHeight = 52.dp,

    // Charts
    chartHeight = 180.dp,
    chartBarWidth = 28.dp
)

private fun largeDimens() = DashboardDimens(
    // Main layout
    horizontalPadding = 20.dp,
    verticalSpacing = 16.dp,
    sectionSpacing = 24.dp,
    contentPadding = 20.dp,

    // Header
    headerPaddingHorizontal = 22.dp,
    headerPaddingVertical = 24.dp,
    headerCornerRadius = 32.dp,
    greetingFontSize = 24,
    subtitleFontSize = 14,

    // Cards
    cardCornerRadius = 20.dp,
    cardPadding = 20.dp,
    cardElevation = 16.dp,
    cardBorderWidth = 1.5.dp,
    smallCardPadding = 14.dp,

    // Net Savings
    netSavingsCardPadding = 22.dp,
    netSavingsAmountSize = 32,
    netSavingsLabelSize = 13,

    // Metric Cards
    metricCardHeight = 96.dp,
    metricCardPadding = 14.dp,
    metricIconSize = 26.dp,
    metricValueSize = 20,
    metricLabelSize = 12,

    // Action Buttons
    actionButtonHeight = 46.dp,
    actionButtonCornerRadius = 14.dp,
    actionButtonIconSize = 20.dp,
    actionButtonFontSize = 13,
    actionButtonSpacing = 12.dp,

    // Grid Buttons
    gridButtonHeight = 72.dp,
    gridButtonIconSize = 26.dp,
    gridButtonFontSize = 12,

    // FAB
    fabSize = 58.dp,
    fabIconSize = 26.dp,
    miniFabSize = 48.dp,
    miniFabIconSize = 22.dp,

    // Transaction Items
    itemCardHeight = 80.dp,
    itemCardPadding = 16.dp,
    itemIconSize = 44.dp,
    itemTitleSize = 15,
    itemSubtitleSize = 13,
    itemAmountSize = 16,

    // Insights
    insightCardPadding = 16.dp,
    insightIconSize = 22.dp,
    insightTitleSize = 14,
    insightMessageSize = 13,

    // Section Headers
    sectionIconSize = 20.dp,
    sectionTitleSize = 16,
    sectionIconContainerSize = 36.dp,

    // Empty State
    emptyStateIconSize = 56.dp,
    emptyStateTitleSize = 16,
    emptyStateSubtitleSize = 14,

    // Dialog
    dialogWidth = 0.85f,
    dialogPadding = 24.dp,
    dialogIconSize = 64.dp,
    dialogTitleSize = 20,
    dialogMessageSize = 13,
    dialogButtonHeight = 46.dp,

    // Menu
    menuWidth = 220.dp,
    menuItemIconSize = 22.dp,
    menuItemFontSize = 15,

    // Top Bar
    topBarLogoSize = 40.dp,
    topBarTitleSize = 22,
    notificationBadgeSize = 10,

    // Income Dialog
    incomeDialogInputHeight = 56.dp,

    // Charts
    chartHeight = 200.dp,
    chartBarWidth = 32.dp
)

private fun expandedDimens() = DashboardDimens(
    // Main layout
    horizontalPadding = 32.dp,
    verticalSpacing = 20.dp,
    sectionSpacing = 28.dp,
    contentPadding = 28.dp,

    // Header
    headerPaddingHorizontal = 28.dp,
    headerPaddingVertical = 28.dp,
    headerCornerRadius = 36.dp,
    greetingFontSize = 28,
    subtitleFontSize = 16,

    // Cards
    cardCornerRadius = 24.dp,
    cardPadding = 24.dp,
    cardElevation = 20.dp,
    cardBorderWidth = 2.dp,
    smallCardPadding = 18.dp,

    // Net Savings
    netSavingsCardPadding = 28.dp,
    netSavingsAmountSize = 38,
    netSavingsLabelSize = 14,

    // Metric Cards
    metricCardHeight = 110.dp,
    metricCardPadding = 18.dp,
    metricIconSize = 30.dp,
    metricValueSize = 24,
    metricLabelSize = 13,

    // Action Buttons
    actionButtonHeight = 52.dp,
    actionButtonCornerRadius = 16.dp,
    actionButtonIconSize = 24.dp,
    actionButtonFontSize = 14,
    actionButtonSpacing = 16.dp,

    // Grid Buttons
    gridButtonHeight = 84.dp,
    gridButtonIconSize = 30.dp,
    gridButtonFontSize = 14,

    // FAB
    fabSize = 64.dp,
    fabIconSize = 28.dp,
    miniFabSize = 54.dp,
    miniFabIconSize = 24.dp,

    // Transaction Items
    itemCardHeight = 90.dp,
    itemCardPadding = 20.dp,
    itemIconSize = 50.dp,
    itemTitleSize = 17,
    itemSubtitleSize = 14,
    itemAmountSize = 18,

    // Insights
    insightCardPadding = 20.dp,
    insightIconSize = 26.dp,
    insightTitleSize = 16,
    insightMessageSize = 14,

    // Section Headers
    sectionIconSize = 24.dp,
    sectionTitleSize = 18,
    sectionIconContainerSize = 42.dp,

    // Empty State
    emptyStateIconSize = 64.dp,
    emptyStateTitleSize = 18,
    emptyStateSubtitleSize = 15,

    // Dialog
    dialogWidth = 0.75f,
    dialogPadding = 32.dp,
    dialogIconSize = 72.dp,
    dialogTitleSize = 24,
    dialogMessageSize = 14,
    dialogButtonHeight = 52.dp,

    // Menu
    menuWidth = 260.dp,
    menuItemIconSize = 26.dp,
    menuItemFontSize = 16,

    // Top Bar
    topBarLogoSize = 48.dp,
    topBarTitleSize = 24,
    notificationBadgeSize = 12,

    // Income Dialog
    incomeDialogInputHeight = 60.dp,

    // Charts
    chartHeight = 240.dp,
    chartBarWidth = 40.dp
)

