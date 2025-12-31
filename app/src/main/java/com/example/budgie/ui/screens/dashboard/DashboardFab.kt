package com.example.budgie.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE FLOATING ACTION BUTTONS
   Expandable FAB menu with responsive sizing
═══════════════════════════════════════════════════════════════════ */

@Composable
fun ResponsiveFloatingButtons(
    dimens: DashboardDimens,
    fabExpanded: Boolean,
    onFabExpandedChange: (Boolean) -> Unit,
    onNavigateToAIChat: () -> Unit,
    onShowIncomeDialog: () -> Unit,
    onAddExpense: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToShoppingList: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
    ) {
        // AI Chat FAB
        FloatingActionButton(
            onClick = onNavigateToAIChat,
            containerColor = DashboardPurple,
            modifier = Modifier
                .size(dimens.miniFabSize)
                .shadow(8.dp, CircleShape)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                "AI Chat",
                tint = Color.White,
                modifier = Modifier.size(dimens.miniFabIconSize)
            )
        }

        // Expandable FAB Menu
        AnimatedVisibility(
            visible = fabExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResponsiveFabMenuItem(
                    dimens = dimens,
                    label = "Add Income",
                    color = DashboardEmerald,
                    icon = Icons.Default.TrendingUp,
                    onClick = {
                        onFabExpandedChange(false)
                        onShowIncomeDialog()
                    }
                )
                ResponsiveFabMenuItem(
                    dimens = dimens,
                    label = "Add Expense",
                    color = DashboardMutedRed,
                    icon = Icons.Default.Receipt,
                    onClick = {
                        onFabExpandedChange(false)
                        onAddExpense()
                    }
                )
                ResponsiveFabMenuItem(
                    dimens = dimens,
                    label = "Manage Bills",
                    color = DashboardBlue,
                    icon = Icons.Default.Payment,
                    onClick = {
                        onFabExpandedChange(false)
                        onNavigateToBills()
                    }
                )
                ResponsiveFabMenuItem(
                    dimens = dimens,
                    label = "Shopping List",
                    color = DashboardPurple,
                    icon = Icons.Default.ShoppingCart,
                    onClick = {
                        onFabExpandedChange(false)
                        onNavigateToShoppingList()
                    }
                )
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { onFabExpandedChange(!fabExpanded) },
            containerColor = DashboardEmerald,
            modifier = Modifier
                .size(dimens.fabSize)
                .shadow(12.dp, CircleShape)
        ) {
            Icon(
                if (fabExpanded) Icons.Default.Close else Icons.Default.GridView,
                "Actions",
                tint = Color.White,
                modifier = Modifier.size(dimens.fabIconSize)
            )
        }
    }
}

@Composable
private fun ResponsiveFabMenuItem(
    dimens: DashboardDimens,
    label: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = DashboardNavy.copy(alpha = 0.95f),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                label,
                modifier = Modifier.padding(
                    horizontal = dimens.smallCardPadding,
                    vertical = dimens.smallCardPadding / 2
                ),
                color = DashboardSoftWhite,
                fontSize = dimens.actionButtonFontSize.sp,
                fontWeight = FontWeight.Medium
            )
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = color,
            modifier = Modifier.size(dimens.miniFabSize)
        ) {
            Icon(
                icon,
                label,
                tint = Color.White,
                modifier = Modifier.size(dimens.miniFabIconSize)
            )
        }
    }
}

