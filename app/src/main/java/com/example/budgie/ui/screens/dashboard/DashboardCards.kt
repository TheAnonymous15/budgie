package com.example.budgie.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.data.model.FinancialSummary
import com.example.budgie.data.model.GoalsSummary
import com.example.budgie.data.model.LoanSummary
import com.example.budgie.ui.components.formatCurrency

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE DASHBOARD HEADER & FINANCIAL CARDS
   Premium wealth display with responsive sizing
═══════════════════════════════════════════════════════════════════ */

@Composable
fun ResponsiveDashboardHeader(
    dimens: DashboardDimens,
    greeting: String,
    displayName: String?,
    currentMonth: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(
                bottomStart = dimens.headerCornerRadius,
                bottomEnd = dimens.headerCornerRadius
            ))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DashboardNavy,
                        Color(0xFF0D2E3D),
                        DashboardTeal.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(
                horizontal = dimens.headerPaddingHorizontal,
                vertical = dimens.headerPaddingVertical
            )
    ) {
        Column {
            Text(
                "$greeting, ${displayName ?: "there"}",
                fontSize = dimens.greetingFontSize.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardSoftWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$currentMonth Financial Snapshot",
                fontSize = dimens.subtitleFontSize.sp,
                color = DashboardSoftWhite.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ResponsiveFinancialSummary(
    dimens: DashboardDimens,
    summary: FinancialSummary,
    totalUnpaidBills: Double,
    totalBudget: Double,
    goalsSummary: GoalsSummary,
    loanSummary: LoanSummary,
    onNavigateToGoals: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = dimens.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
    ) {
        // Net Savings Card
        ResponsiveNetSavingsCard(
            dimens = dimens,
            summary = summary
        )

        // Metric Cards Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonSpacing)
        ) {
            ResponsiveMetricCard(
                dimens = dimens,
                label = "Income",
                amount = summary.totalIncome,
                icon = Icons.Default.TrendingUp,
                accentColor = DashboardEmerald,
                hasData = summary.totalIncome > 0,
                modifier = Modifier.weight(1f)
            )
            ResponsiveMetricCard(
                dimens = dimens,
                label = "Expenses",
                amount = summary.totalExpenses,
                icon = Icons.Default.TrendingDown,
                accentColor = DashboardMutedRed,
                hasData = summary.totalExpenses > 0,
                modifier = Modifier.weight(1f)
            )
        }

        // Metric Cards Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonSpacing)
        ) {
            ResponsiveMetricCard(
                dimens = dimens,
                label = "Bills Due",
                amount = totalUnpaidBills,
                icon = Icons.Default.Receipt,
                accentColor = DashboardBlue,
                hasData = totalUnpaidBills > 0,
                modifier = Modifier.weight(1f)
            )
            ResponsiveMetricCard(
                dimens = dimens,
                label = "Budget",
                amount = totalBudget,
                icon = Icons.Default.PieChart,
                accentColor = DashboardCyan,
                hasData = totalBudget > 0,
                modifier = Modifier.weight(1f)
            )
        }

        // Goals/Loans/Analytics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonSpacing)
        ) {
            ResponsiveSummaryActionButton(
                dimens = dimens,
                icon = Icons.Default.Analytics,
                label = "Analytics",
                subtitle = null,
                accentColor = DashboardEmerald,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            ResponsiveSummaryActionButton(
                dimens = dimens,
                icon = Icons.Default.Flag,
                label = "Goals",
                subtitle = if (goalsSummary.activeGoals > 0) "${goalsSummary.activeGoals} active" else null,
                accentColor = DashboardGold,
                onClick = onNavigateToGoals,
                modifier = Modifier.weight(1f)
            )
            ResponsiveSummaryActionButton(
                dimens = dimens,
                icon = Icons.Default.AccountBalanceWallet,
                label = "Loans",
                subtitle = if (loanSummary.activeLoans > 0) "${loanSummary.activeLoans} active" else null,
                accentColor = DashboardBlue,
                onClick = onNavigateToLoans,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Grid Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonSpacing)
        ) {
            ResponsiveGridActionButton(
                dimens = dimens,
                icon = Icons.Default.Receipt,
                label = "Expenses",
                onClick = onNavigateToExpenses,
                modifier = Modifier.weight(1f)
            )
            ResponsiveGridActionButton(
                dimens = dimens,
                icon = Icons.Default.AccountBalance,
                label = "Income",
                onClick = onNavigateToIncome,
                modifier = Modifier.weight(1f)
            )
            ResponsiveGridActionButton(
                dimens = dimens,
                icon = Icons.Default.Payment,
                label = "Bills",
                onClick = onNavigateToBills,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Grid Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonSpacing)
        ) {
            ResponsiveGridActionButton(
                dimens = dimens,
                icon = Icons.Default.PieChart,
                label = "Budget",
                onClick = onNavigateToBudget,
                modifier = Modifier.weight(1f)
            )
            ResponsiveGridActionButton(
                dimens = dimens,
                icon = Icons.Default.TrendingUp,
                label = "Invest",
                onClick = onNavigateToInvestments,
                modifier = Modifier.weight(1f)
            )
            ResponsiveGridActionButton(
                dimens = dimens,
                icon = Icons.Default.FileUpload,
                label = "Export",
                onClick = onNavigateToExport,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResponsiveNetSavingsCard(
    dimens: DashboardDimens,
    summary: FinancialSummary
) {
    val netSavings = summary.totalIncome - summary.totalExpenses
    val hasData = summary.totalIncome > 0 || summary.totalExpenses > 0
    val accentColor = if (netSavings >= 0 && hasData) DashboardEmerald else DashboardMutedRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(accentColor, dimens.cardCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.netSavingsCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Net Savings",
                fontSize = dimens.netSavingsLabelSize.sp,
                color = DashboardSoftWhite.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (hasData) formatCurrency(netSavings) else "No data yet",
                fontSize = dimens.netSavingsAmountSize.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            if (hasData && summary.totalIncome > 0) {
                val savingsRate = (netSavings / summary.totalIncome) * 100
                Text(
                    "Savings rate: ${String.format("%.1f", savingsRate)}%",
                    fontSize = (dimens.netSavingsLabelSize - 1).sp,
                    color = DashboardSoftWhite.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ResponsiveMetricCard(
    dimens: DashboardDimens,
    label: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimens.metricCardHeight)
            .glassmorphicCard(dimens.cardCornerRadius, dimens.cardBorderWidth)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimens.metricCardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonSpacing)
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.metricIconSize + 12.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(dimens.metricIconSize)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = dimens.metricLabelSize.sp,
                    color = DashboardSoftWhite.copy(alpha = 0.7f)
                )
                Text(
                    if (hasData) formatCurrency(amount) else "--",
                    fontSize = dimens.metricValueSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasData) accentColor else DashboardSoftWhite.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ResponsiveSummaryActionButton(
    dimens: DashboardDimens,
    icon: ImageVector,
    label: String,
    subtitle: String?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimens.actionButtonHeight + 16.dp)
            .clip(RoundedCornerShape(dimens.actionButtonCornerRadius))
            .background(accentColor.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(dimens.smallCardPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(dimens.actionButtonIconSize)
            )
            Text(
                label,
                fontSize = dimens.actionButtonFontSize.sp,
                fontWeight = FontWeight.Medium,
                color = DashboardSoftWhite
            )
            subtitle?.let {
                Text(
                    it,
                    fontSize = (dimens.actionButtonFontSize - 2).sp,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun ResponsiveGridActionButton(
    dimens: DashboardDimens,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(dimens.gridButtonHeight)
            .glassmorphicCard(dimens.cardCornerRadius, dimens.cardBorderWidth)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = DashboardEmerald,
                modifier = Modifier.size(dimens.gridButtonIconSize)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontSize = dimens.gridButtonFontSize.sp,
                fontWeight = FontWeight.Medium,
                color = DashboardSoftWhite,
                textAlign = TextAlign.Center
            )
        }
    }
}

