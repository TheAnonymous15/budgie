package com.example.budgie.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.budgie.data.model.Bill
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.SpendingInsight
import com.example.budgie.ui.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

/* ═══════════════════════════════════════════════════════════════════
   DETAIL MODALS - Magical popup details for dashboard items
   Next-level UI with glassmorphism and stunning animations
═══════════════════════════════════════════════════════════════════ */

@Composable
fun ExpenseDetailModal(
    expense: Expense?,
    onDismiss: () -> Unit,
    onEdit: (Expense) -> Unit = {},
    onDelete: (Expense) -> Unit = {}
) {
    if (expense == null) return

    MagicalDetailDialog(
        isVisible = true,
        onDismiss = onDismiss,
        accentColor = DashboardMutedRed,
        icon = Icons.Default.Receipt,
        title = "Expense Details"
    ) {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Amount Display - Large and prominent
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DashboardMutedRed.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, DashboardMutedRed.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Amount Spent",
                        fontSize = 14.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatCurrency(expense.amount),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardMutedRed
                    )
                }
            }

            // Details Grid
            DetailRow(
                label = "Title",
                value = expense.title,
                icon = Icons.Default.Title
            )

            DetailRow(
                label = "Category",
                value = expense.category.displayName,
                icon = Icons.Default.Category
            )

            DetailRow(
                label = "Date",
                value = dateFormat.format(Date(expense.date)),
                icon = Icons.Default.CalendarToday
            )

            DetailRow(
                label = "Time",
                value = timeFormat.format(Date(expense.date)),
                icon = Icons.Default.Schedule
            )

            if (expense.notes.isNotBlank()) {
                DetailRow(
                    label = "Notes",
                    value = expense.notes,
                    icon = Icons.Default.Notes,
                    isMultiline = true
                )
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(expense) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DashboardEmerald
                    ),
                    border = BorderStroke(1.5.dp, DashboardEmerald)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { onDelete(expense) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DashboardMutedRed
                    ),
                    border = BorderStroke(1.5.dp, DashboardMutedRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun BillDetailModal(
    bill: Bill?,
    onDismiss: () -> Unit,
    onPay: (Bill) -> Unit = {},
    onEdit: (Bill) -> Unit = {},
    onDelete: (Bill) -> Unit = {}
) {
    if (bill == null) return

    val isPastDue = bill.dueDate < System.currentTimeMillis() && !bill.isPaid

    MagicalDetailDialog(
        isVisible = true,
        onDismiss = onDismiss,
        accentColor = if (isPastDue) DashboardMutedRed else DashboardBlue,
        icon = if (isPastDue) Icons.Default.Warning else Icons.Default.Receipt,
        title = if (isPastDue) "Overdue Bill" else "Bill Details"
    ) {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status Badge
            if (bill.isPaid) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DashboardEmerald.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DashboardEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "PAID",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardEmerald
                        )
                    }
                }
            } else if (isPastDue) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DashboardMutedRed.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = DashboardMutedRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "OVERDUE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardMutedRed
                        )
                    }
                }
            }

            // Amount Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPastDue)
                        DashboardMutedRed.copy(alpha = 0.1f)
                    else
                        DashboardBlue.copy(alpha = 0.1f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isPastDue)
                        DashboardMutedRed.copy(alpha = 0.3f)
                    else
                        DashboardBlue.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Amount Due",
                        fontSize = 14.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatCurrency(bill.amount),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPastDue) DashboardMutedRed else DashboardAmber
                    )
                }
            }

            // Details
            DetailRow(
                label = "Bill Name",
                value = bill.title,
                icon = Icons.Default.Title
            )

            DetailRow(
                label = "Due Date",
                value = dateFormat.format(Date(bill.dueDate)),
                icon = Icons.Default.CalendarToday
            )


            // Action Buttons
            Spacer(modifier = Modifier.height(8.dp))
            if (!bill.isPaid) {
                Button(
                    onClick = { onPay(bill) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardEmerald
                    )
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Pay Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(bill) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DashboardEmerald
                    ),
                    border = BorderStroke(1.5.dp, DashboardEmerald)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { onDelete(bill) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DashboardMutedRed
                    ),
                    border = BorderStroke(1.5.dp, DashboardMutedRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun InsightDetailModal(
    insight: SpendingInsight?,
    onDismiss: () -> Unit,
    onTakeAction: () -> Unit = {}
) {
    if (insight == null) return

    val insightColor = when (insight.type) {
        com.example.budgie.data.model.InsightType.BUDGET_WARNING,
        com.example.budgie.data.model.InsightType.OVERSPENDING -> DashboardAmber
        com.example.budgie.data.model.InsightType.GOOD_HABIT,
        com.example.budgie.data.model.InsightType.SAVING_OPPORTUNITY -> DashboardEmerald
        com.example.budgie.data.model.InsightType.TREND_ANALYSIS,
        com.example.budgie.data.model.InsightType.COMPARISON -> DashboardBlue
    }

    MagicalDetailDialog(
        isVisible = true,
        onDismiss = onDismiss,
        accentColor = insightColor,
        icon = when (insight.type) {
            com.example.budgie.data.model.InsightType.BUDGET_WARNING,
            com.example.budgie.data.model.InsightType.OVERSPENDING -> Icons.Default.Warning
            com.example.budgie.data.model.InsightType.GOOD_HABIT,
            com.example.budgie.data.model.InsightType.SAVING_OPPORTUNITY -> Icons.Default.CheckCircle
            com.example.budgie.data.model.InsightType.TREND_ANALYSIS,
            com.example.budgie.data.model.InsightType.COMPARISON -> Icons.Default.TrendingUp
        },
        title = "AI Insight"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Insight Title
            Text(
                insight.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardSoftWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = insightColor.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, insightColor.copy(alpha = 0.3f))
            ) {
                Text(
                    insight.description,
                    fontSize = 16.sp,
                    color = DashboardSoftWhite.copy(alpha = 0.9f),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }

            // Recommendation (if exists)
            if (insight.actionable.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DashboardEmerald.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, DashboardEmerald.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = DashboardGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Recommended Action",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashboardGold
                            )
                            Text(
                                "Take action to improve your finances",
                                fontSize = 12.sp,
                                color = DashboardSoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onTakeAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardEmerald
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Take Action", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MAGICAL DETAIL DIALOG - Reusable stunning dialog container
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun MagicalDetailDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    accentColor: Color,
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(200)
        )
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clickable(onClick = {}, enabled = false), // Prevent click-through
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DashboardNavy
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Animated Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.2f),
                                            accentColor.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Animated Icon
                                var iconScale by remember { mutableStateOf(0f) }

                                LaunchedEffect(Unit) {
                                    iconScale = 1f
                                }

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(iconScale)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Text(
                                    title,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DashboardSoftWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Content
                        content()

                        // Close Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DashboardSoftWhite
                                ),
                                border = BorderStroke(1.dp, DashboardSoftWhite.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Close", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// DETAIL ROW - Reusable detail field
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    isMultiline: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.03f)
        ),
        border = BorderStroke(1.dp, DashboardSoftWhite.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DashboardEmerald.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = DashboardEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = DashboardSoftWhite.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    value,
                    fontSize = 15.sp,
                    color = DashboardSoftWhite,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (isMultiline) 20.sp else 15.sp
                )
            }
        }
    }
}

