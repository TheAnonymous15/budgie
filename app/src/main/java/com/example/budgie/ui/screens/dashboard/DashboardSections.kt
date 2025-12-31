package com.example.budgie.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.data.model.Bill
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.SpendingInsight
import com.example.budgie.data.model.InsightType
import com.example.budgie.ui.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE DASHBOARD SECTIONS
   Transaction lists, insights, bills, and empty states
═══════════════════════════════════════════════════════════════════ */

@Composable
fun ResponsiveInsightsHeader(
    dimens: DashboardDimens,
    onNavigateToInsights: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dimens.sectionIconContainerSize)
                    .background(DashboardEmerald.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = DashboardEmerald,
                    modifier = Modifier.size(dimens.sectionIconSize)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "AI Insights",
                fontSize = dimens.sectionTitleSize.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardSoftWhite
            )
        }
        TextButton(onClick = onNavigateToInsights) {
            Text(
                "View All",
                color = DashboardEmerald,
                fontWeight = FontWeight.SemiBold,
                fontSize = (dimens.sectionTitleSize - 2).sp
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = DashboardEmerald,
                modifier = Modifier.size(dimens.sectionIconSize)
            )
        }
    }
}

@Composable
fun ResponsiveTransactionsHeader(
    dimens: DashboardDimens,
    hasExpenses: Boolean,
    onNavigateToExpenses: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Recent Transactions",
            fontSize = dimens.sectionTitleSize.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardSoftWhite
        )
        if (hasExpenses) {
            TextButton(onClick = onNavigateToExpenses) {
                Text(
                    "View All",
                    color = DashboardEmerald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (dimens.sectionTitleSize - 2).sp
                )
            }
        }
    }
}

@Composable
fun ResponsiveEmptyTransactions(
    dimens: DashboardDimens,
    onAddExpense: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, DashboardSoftWhite.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Receipt,
                contentDescription = null,
                tint = DashboardSoftWhite.copy(alpha = 0.3f),
                modifier = Modifier.size(dimens.emptyStateIconSize)
            )
            Spacer(modifier = Modifier.height(dimens.verticalSpacing))
            Text(
                "No transactions yet",
                fontSize = dimens.emptyStateTitleSize.sp,
                fontWeight = FontWeight.Medium,
                color = DashboardSoftWhite.copy(alpha = 0.7f)
            )
            Text(
                "Start tracking to unlock AI insights",
                fontSize = dimens.emptyStateSubtitleSize.sp,
                color = DashboardSoftWhite.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimens.verticalSpacing))
            Button(
                onClick = onAddExpense,
                colors = ButtonDefaults.buttonColors(containerColor = DashboardEmerald),
                shape = RoundedCornerShape(dimens.actionButtonCornerRadius)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(dimens.actionButtonIconSize)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Add First Transaction",
                    fontWeight = FontWeight.Medium,
                    fontSize = dimens.actionButtonFontSize.sp
                )
            }
        }
    }
}

@Composable
fun ResponsiveBillsHeader(
    dimens: DashboardDimens,
    onNavigateToBills: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Upcoming Bills",
            fontSize = dimens.sectionTitleSize.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardSoftWhite
        )
        TextButton(onClick = onNavigateToBills) {
            Text(
                "View All",
                color = DashboardEmerald,
                fontWeight = FontWeight.SemiBold,
                fontSize = (dimens.sectionTitleSize - 2).sp
            )
        }
    }
}

@Composable
fun ResponsiveWealthTip(dimens: DashboardDimens) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = DashboardEmerald.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, DashboardEmerald.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.TipsAndUpdates,
                contentDescription = null,
                tint = DashboardGold,
                modifier = Modifier.size(dimens.sectionIconSize + 4.dp)
            )
            Spacer(modifier = Modifier.width(dimens.actionButtonSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Wealth Building Tip",
                    fontSize = (dimens.sectionTitleSize - 2).sp,
                    color = DashboardGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "If you save \$50/week → \$2,600/year",
                    fontSize = (dimens.sectionTitleSize - 3).sp,
                    color = DashboardSoftWhite.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ResponsiveInsightCard(
    dimens: DashboardDimens,
    insight: SpendingInsight
) {
    val insightColor = when (insight.type) {
        InsightType.BUDGET_WARNING, InsightType.OVERSPENDING -> DashboardAmber
        InsightType.GOOD_HABIT, InsightType.SAVING_OPPORTUNITY -> DashboardEmerald
        InsightType.TREND_ANALYSIS, InsightType.COMPARISON -> DashboardBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = insightColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, insightColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.insightCardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.insightIconSize + 12.dp)
                    .clip(CircleShape)
                    .background(insightColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (insight.type) {
                        InsightType.BUDGET_WARNING, InsightType.OVERSPENDING -> Icons.Default.Warning
                        InsightType.GOOD_HABIT, InsightType.SAVING_OPPORTUNITY -> Icons.Default.CheckCircle
                        InsightType.TREND_ANALYSIS, InsightType.COMPARISON -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = insightColor,
                    modifier = Modifier.size(dimens.insightIconSize)
                )
            }
            Spacer(modifier = Modifier.width(dimens.actionButtonSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    insight.title,
                    fontSize = dimens.insightTitleSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DashboardSoftWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    insight.description,
                    fontSize = dimens.insightMessageSize.sp,
                    color = DashboardSoftWhite.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ResponsiveExpenseItem(
    dimens: DashboardDimens,
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, DashboardSoftWhite.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.itemCardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(dimens.itemIconSize)
                    .clip(CircleShape)
                    .background(DashboardMutedRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(expense.category.displayName),
                    contentDescription = expense.category.displayName,
                    tint = DashboardMutedRed,
                    modifier = Modifier.size(dimens.itemIconSize * 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(dimens.actionButtonSpacing))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.title,
                    fontSize = dimens.itemTitleSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = DashboardSoftWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        expense.category.displayName,
                        fontSize = dimens.itemSubtitleSize.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.6f)
                    )
                    Text("•", color = DashboardSoftWhite.copy(alpha = 0.4f))
                    Text(
                        dateFormat.format(Date(expense.date)),
                        fontSize = dimens.itemSubtitleSize.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.5f)
                    )
                }
            }

            // Amount
            Text(
                "-${formatCurrency(expense.amount)}",
                fontSize = dimens.itemAmountSize.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardMutedRed
            )

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(dimens.itemIconSize * 0.8f)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = DashboardMutedRed.copy(alpha = 0.6f),
                    modifier = Modifier.size(dimens.itemSubtitleSize.dp + 4.dp)
                )
            }
        }
    }
}

@Composable
fun ResponsiveBillItem(
    dimens: DashboardDimens,
    bill: Bill,
    onPaidToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val isPastDue = bill.dueDate < System.currentTimeMillis() && !bill.isPaid

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isPastDue)
                DashboardMutedRed.copy(alpha = 0.08f)
            else
                Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            1.dp,
            if (isPastDue)
                DashboardMutedRed.copy(alpha = 0.3f)
            else
                DashboardSoftWhite.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.itemCardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bill Icon
            Box(
                modifier = Modifier
                    .size(dimens.itemIconSize)
                    .clip(CircleShape)
                    .background(
                        if (isPastDue)
                            DashboardMutedRed.copy(alpha = 0.15f)
                        else
                            DashboardBlue.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPastDue) Icons.Default.Warning else Icons.Default.Receipt,
                    contentDescription = bill.title,
                    tint = if (isPastDue) DashboardMutedRed else DashboardBlue,
                    modifier = Modifier.size(dimens.itemIconSize * 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(dimens.actionButtonSpacing))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bill.title,
                    fontSize = dimens.itemTitleSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = DashboardSoftWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Due: ${dateFormat.format(Date(bill.dueDate))}",
                        fontSize = dimens.itemSubtitleSize.sp,
                        color = if (isPastDue)
                            DashboardMutedRed
                        else
                            DashboardSoftWhite.copy(alpha = 0.6f)
                    )
                    if (isPastDue) {
                        Text(
                            "• OVERDUE",
                            fontSize = (dimens.itemSubtitleSize - 1).sp,
                            color = DashboardMutedRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Amount
            Text(
                formatCurrency(bill.amount),
                fontSize = dimens.itemAmountSize.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPastDue) DashboardMutedRed else DashboardAmber
            )

            // Paid Checkbox
            Checkbox(
                checked = bill.isPaid,
                onCheckedChange = onPaidToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = DashboardEmerald,
                    uncheckedColor = DashboardSoftWhite.copy(alpha = 0.4f)
                )
            )
        }
    }
}

// Helper function for category icons
private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "food", "groceries", "dining" -> Icons.Default.Restaurant
        "transport", "transportation", "travel" -> Icons.Default.DirectionsCar
        "utilities", "bills" -> Icons.Default.ElectricalServices
        "entertainment", "fun" -> Icons.Default.Movie
        "health", "medical" -> Icons.Default.LocalHospital
        "shopping", "retail" -> Icons.Default.ShoppingBag
        "education" -> Icons.Default.School
        "housing", "rent" -> Icons.Default.Home
        "personal" -> Icons.Default.Person
        "savings" -> Icons.Default.Savings
        else -> Icons.Default.Receipt
    }
}

