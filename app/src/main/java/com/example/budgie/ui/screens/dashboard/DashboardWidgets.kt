package com.example.budgie.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.data.model.FinancialSummary
import com.example.budgie.data.model.GoalsSummary
import com.example.budgie.data.model.LoanSummary

/* ═══════════════════════════════════════════════════════════════════
   PROFESSIONAL DASHBOARD WIDGETS
   Premium financial widgets for a futuristic dashboard
═══════════════════════════════════════════════════════════════════ */


// ══════════════════════════════════════════════════════════════════
// 1. FINANCIAL HEALTH SCORE WIDGET
// Circular progress showing overall financial health (0-100)
// ══════════════════════════════════════════════════════════════════

@Composable
fun FinancialHealthScoreWidget(
    summary: FinancialSummary,
    goalsSummary: GoalsSummary,
    loanSummary: LoanSummary,
    unpaidBillsCount: Int,
    totalBudget: Double,
    modifier: Modifier = Modifier
) {
    // Calculate health score (0-100)
    val healthScore = remember(summary, goalsSummary, loanSummary, unpaidBillsCount) {
        calculateFinancialHealthScore(summary, goalsSummary, loanSummary, unpaidBillsCount, totalBudget)
    }

    val animatedScore by animateFloatAsState(
        targetValue = healthScore.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "healthScore"
    )

    val scoreColor = when {
        healthScore >= 80 -> DashboardEmerald
        healthScore >= 60 -> DashboardGold
        healthScore >= 40 -> DashboardAmber
        else -> DashboardMutedRed
    }

    val scoreLabel = when {
        healthScore >= 80 -> "Excellent"
        healthScore >= 60 -> "Good"
        healthScore >= 40 -> "Fair"
        else -> "Needs Attention"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = scoreColor.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DashboardNavyLight.copy(alpha = 0.95f),
                        DashboardNavy.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        scoreColor.copy(alpha = 0.4f),
                        scoreColor.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score Circle
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = DashboardSoftWhite.copy(alpha = 0.1f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Progress arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                scoreColor.copy(alpha = 0.3f),
                                scoreColor,
                                scoreColor.copy(alpha = 0.8f)
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = (animatedScore / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Score text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${animatedScore.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "/ 100",
                        fontSize = 12.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.5f)
                    )
                }
            }

            // Score details
            Column(
                modifier = Modifier.weight(1f).padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Financial Health",
                    fontSize = 14.sp,
                    color = DashboardSoftWhite.copy(alpha = 0.7f)
                )
                Text(
                    text = scoreLabel,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mini stats
                HealthStatRow(
                    icon = Icons.Default.Savings,
                    label = "Savings Rate",
                    value = if (summary.totalIncome > 0) "${((summary.totalIncome - summary.totalExpenses) / summary.totalIncome * 100).toInt()}%" else "N/A",
                    color = DashboardEmerald
                )
                HealthStatRow(
                    icon = Icons.Default.Receipt,
                    label = "Bills Status",
                    value = if (unpaidBillsCount == 0) "All Paid" else "$unpaidBillsCount Due",
                    color = if (unpaidBillsCount == 0) DashboardEmerald else DashboardAmber
                )
                HealthStatRow(
                    icon = Icons.Default.Flag,
                    label = "Goals Progress",
                    value = "${goalsSummary.completedGoals}/${goalsSummary.totalGoals}",
                    color = DashboardCyan
                )
            }
        }
    }
}

@Composable
private fun HealthStatRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = DashboardSoftWhite.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun calculateFinancialHealthScore(
    summary: FinancialSummary,
    goalsSummary: GoalsSummary,
    loanSummary: LoanSummary,
    unpaidBillsCount: Int,
    totalBudget: Double
): Int {
    var score = 50 // Base score

    // Savings rate contribution (0-30 points)
    if (summary.totalIncome > 0) {
        val savingsRate = (summary.totalIncome - summary.totalExpenses) / summary.totalIncome
        score += (savingsRate * 30).toInt().coerceIn(0, 30)
    }

    // Bills status (0-20 points)
    score += when {
        unpaidBillsCount == 0 -> 20
        unpaidBillsCount <= 2 -> 10
        unpaidBillsCount <= 5 -> 5
        else -> 0
    }

    // Goals progress (0-15 points)
    if (goalsSummary.totalGoals > 0) {
        val goalProgress = goalsSummary.completedGoals.toFloat() / goalsSummary.totalGoals
        score += (goalProgress * 15).toInt()
    } else {
        score += 5 // Neutral if no goals
    }

    // Budget adherence (0-15 points)
    if (totalBudget > 0 && summary.totalExpenses <= totalBudget) {
        val adherence = 1 - (summary.totalExpenses / totalBudget)
        score += (adherence * 15).toInt().coerceIn(0, 15)
    }

    // Loan management (-20 to +10 points)
    if (loanSummary.totalLoans > 0 && loanSummary.totalBorrowed > 0) {
        val repaymentRatio = loanSummary.totalRepaid / loanSummary.totalBorrowed
        score += (repaymentRatio * 10).toInt().coerceIn(-20, 10)
    }

    return score.coerceIn(0, 100)
}

// ══════════════════════════════════════════════════════════════════
// 2. ANIMATED QUICK STATS BAR
// Real-time animated counters with trend indicators
// ══════════════════════════════════════════════════════════════════

@Composable
fun QuickStatsBar(
    income: Double,
    expenses: Double,
    savings: Double,
    previousIncome: Double = 0.0,
    previousExpenses: Double = 0.0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DashboardNavyLight.copy(alpha = 0.6f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AnimatedStatItem(
            label = "Income",
            value = income,
            previousValue = previousIncome,
            color = DashboardEmerald,
            icon = Icons.Default.TrendingUp,
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(
            modifier = Modifier.height(50.dp),
            color = DashboardSoftWhite.copy(alpha = 0.1f)
        )

        AnimatedStatItem(
            label = "Expenses",
            value = expenses,
            previousValue = previousExpenses,
            color = DashboardMutedRed,
            icon = Icons.Default.TrendingDown,
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(
            modifier = Modifier.height(50.dp),
            color = DashboardSoftWhite.copy(alpha = 0.1f)
        )

        AnimatedStatItem(
            label = "Savings",
            value = savings,
            previousValue = previousIncome - previousExpenses,
            color = if (savings >= 0) DashboardCyan else DashboardMutedRed,
            icon = Icons.Default.Savings,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AnimatedStatItem(
    label: String,
    value: Double,
    previousValue: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "value"
    )

    val trend = when {
        previousValue == 0.0 -> 0
        value > previousValue -> 1
        value < previousValue -> -1
        else -> 0
    }

    val trendPercent = if (previousValue > 0) {
        ((value - previousValue) / previousValue * 100).toInt()
    } else 0

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = DashboardSoftWhite.copy(alpha = 0.6f)
            )
        }

        Text(
            text = formatCompactCurrency(animatedValue.toDouble()),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        if (trend != 0 && previousValue > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = if (trend > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if ((trend > 0 && label != "Expenses") || (trend < 0 && label == "Expenses"))
                        DashboardEmerald else DashboardMutedRed,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "${kotlin.math.abs(trendPercent)}%",
                    fontSize = 10.sp,
                    color = if ((trend > 0 && label != "Expenses") || (trend < 0 && label == "Expenses"))
                        DashboardEmerald else DashboardMutedRed
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 3. SPENDING BREAKDOWN CHART
// Interactive donut chart showing category breakdown
// ══════════════════════════════════════════════════════════════════

data class SpendingCategory(
    val name: String,
    val amount: Double,
    val color: Color,
    val icon: ImageVector
)

@Composable
fun SpendingBreakdownChart(
    categories: List<SpendingCategory>,
    totalSpent: Double,
    onCategoryClick: (SpendingCategory) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "chartProgress"
    )

    var selectedCategory by remember { mutableStateOf<SpendingCategory?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DashboardCyan.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DashboardNavyLight.copy(alpha = 0.95f),
                        DashboardNavy.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        DashboardCyan.copy(alpha = 0.3f),
                        DashboardCyan.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Breakdown",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardSoftWhite
                )
                Text(
                    text = formatCurrency(totalSpent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DashboardMutedRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = DashboardSoftWhite.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No spending data yet",
                            color = DashboardSoftWhite.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            categories.forEach { category ->
                                val sweepAngle = (category.amount / totalSpent * 360f * animatedProgress).toFloat()
                                drawArc(
                                    color = category.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt),
                                    size = Size(size.width, size.height)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Center text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${categories.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashboardSoftWhite
                            )
                            Text(
                                text = "Categories",
                                fontSize = 10.sp,
                                color = DashboardSoftWhite.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(5).forEach { category ->
                            SpendingLegendItem(
                                category = category,
                                percentage = (category.amount / totalSpent * 100).toInt(),
                                isSelected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                    onCategoryClick(category)
                                }
                            )
                        }
                        if (categories.size > 5) {
                            Text(
                                text = "+${categories.size - 5} more",
                                fontSize = 11.sp,
                                color = DashboardSoftWhite.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingLegendItem(
    category: SpendingCategory,
    percentage: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) category.color.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(category.color)
        )
        Text(
            text = category.name,
            fontSize = 11.sp,
            color = DashboardSoftWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$percentage%",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = category.color
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 4. SMART CONTEXTUAL GREETING
// Greeting with personalized financial insights
// ══════════════════════════════════════════════════════════════════

@Composable
fun SmartGreetingCard(
    greeting: String,
    displayName: String?,
    summary: FinancialSummary,
    unpaidBillsCount: Int,
    goalsSummary: GoalsSummary,
    modifier: Modifier = Modifier
) {
    val contextMessage = remember(summary, unpaidBillsCount, goalsSummary) {
        generateContextualMessage(summary, unpaidBillsCount, goalsSummary)
    }

    // Animated gradient
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DashboardNavy,
                        DashboardTeal.copy(alpha = 0.3f + gradientOffset * 0.2f),
                        DashboardEmerald.copy(alpha = 0.2f + gradientOffset * 0.15f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 500f)
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            // Main greeting
            Text(
                text = "$greeting,",
                fontSize = 16.sp,
                color = DashboardSoftWhite.copy(alpha = 0.8f)
            )
            Text(
                text = displayName ?: "there",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardSoftWhite
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contextual insight card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DashboardSoftWhite.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(contextMessage.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = contextMessage.icon,
                        contentDescription = null,
                        tint = contextMessage.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contextMessage.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contextMessage.color
                    )
                    Text(
                        text = contextMessage.message,
                        fontSize = 12.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class ContextualMessage(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val color: Color
)

private fun generateContextualMessage(
    summary: FinancialSummary,
    unpaidBillsCount: Int,
    goalsSummary: GoalsSummary
): ContextualMessage {
    // Priority-based message selection
    return when {
        // Bill alerts (highest priority)
        unpaidBillsCount >= 3 -> ContextualMessage(
            title = "Bills Need Attention",
            message = "You have $unpaidBillsCount bills due. Consider paying them soon.",
            icon = Icons.Default.Warning,
            color = DashboardAmber
        )

        // Great savings
        summary.totalIncome > 0 && (summary.totalIncome - summary.totalExpenses) / summary.totalIncome > 0.2 -> ContextualMessage(
            title = "Great Savings!",
            message = "You've saved ${((summary.totalIncome - summary.totalExpenses) / summary.totalIncome * 100).toInt()}% this month. Keep it up!",
            icon = Icons.Default.Celebration,
            color = DashboardEmerald
        )

        // Goal completion
        goalsSummary.completedGoals > 0 -> ContextualMessage(
            title = "Goals Achieved!",
            message = "You've completed ${goalsSummary.completedGoals} goal${if (goalsSummary.completedGoals > 1) "s" else ""}. Amazing progress!",
            icon = Icons.Default.EmojiEvents,
            color = DashboardGold
        )

        // Active goals
        goalsSummary.activeGoals > 0 -> ContextualMessage(
            title = "Stay Focused",
            message = "You have ${goalsSummary.activeGoals} active goal${if (goalsSummary.activeGoals > 1) "s" else ""} in progress.",
            icon = Icons.Default.Flag,
            color = DashboardCyan
        )

        // Overspending warning
        summary.totalExpenses > summary.totalIncome && summary.totalIncome > 0 -> ContextualMessage(
            title = "Spending Alert",
            message = "Your expenses exceed income by ${formatCompactCurrency(summary.totalExpenses - summary.totalIncome)}",
            icon = Icons.Default.TrendingDown,
            color = DashboardMutedRed
        )

        // Default encouraging message
        else -> ContextualMessage(
            title = "Welcome Back",
            message = "Track your expenses to unlock personalized insights.",
            icon = Icons.Default.Insights,
            color = DashboardEmerald
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 5. CASH FLOW SPARKLINE
// 7-day or 30-day trend visualization
// ══════════════════════════════════════════════════════════════════

@Composable
fun CashFlowSparkline(
    data: List<Double>,
    labels: List<String>,
    title: String = "7-Day Cash Flow",
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "sparklineProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DashboardNavyLight.copy(alpha = 0.8f),
                        DashboardNavy.copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = DashboardSoftWhite.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DashboardSoftWhite
                )

                val total = data.sum()
                val trend = if (data.size >= 2) data.last() - data.first() else 0.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (trend >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (trend >= 0) DashboardEmerald else DashboardMutedRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatCompactCurrency(total),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trend >= 0) DashboardEmerald else DashboardMutedRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty() || data.all { it == 0.0 }) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        color = DashboardSoftWhite.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
            } else {
                // Sparkline chart
                val maxValue = data.maxOrNull() ?: 1.0
                val minValue = data.minOrNull() ?: 0.0
                val range = (maxValue - minValue).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (data.size - 1).coerceAtLeast(1)

                    // Draw gradient area
                    val path = Path()
                    data.forEachIndexed { index, value ->
                        val x = index * stepX * animatedProgress
                        val normalizedValue = ((value - minValue) / range).toFloat()
                        val y = height - (normalizedValue * height * 0.8f) - height * 0.1f

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Draw line
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                DashboardEmerald.copy(alpha = 0.6f),
                                DashboardCyan,
                                DashboardEmerald
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw points
                    data.forEachIndexed { index, value ->
                        val x = index * stepX * animatedProgress
                        val normalizedValue = ((value - minValue) / range).toFloat()
                        val y = height - (normalizedValue * height * 0.8f) - height * 0.1f

                        drawCircle(
                            color = DashboardEmerald,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = DashboardNavy,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                // Labels
                if (labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        labels.forEach { label ->
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = DashboardSoftWhite.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 6. NOTIFICATION PREVIEW CARDS
// Inline notification previews
// ══════════════════════════════════════════════════════════════════

data class NotificationPreview(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: String
)

enum class NotificationType {
    BILL_REMINDER,
    BUDGET_ALERT,
    GOAL_MILESTONE,
    SECURITY,
    INSIGHT
}

@Composable
fun NotificationPreviewSection(
    notifications: List<NotificationPreview>,
    onViewAll: () -> Unit,
    onNotificationClick: (NotificationPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) return

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DashboardAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = DashboardAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Recent Alerts",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DashboardSoftWhite
                )
            }
            TextButton(onClick = onViewAll) {
                Text(
                    text = "View All",
                    color = DashboardEmerald,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notification cards
        notifications.take(3).forEach { notification ->
            NotificationPreviewCard(
                notification = notification,
                onClick = { onNotificationClick(notification) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NotificationPreviewCard(
    notification: NotificationPreview,
    onClick: () -> Unit
) {
    val (icon, color) = when (notification.type) {
        NotificationType.BILL_REMINDER -> Icons.Default.Receipt to DashboardAmber
        NotificationType.BUDGET_ALERT -> Icons.Default.Warning to DashboardMutedRed
        NotificationType.GOAL_MILESTONE -> Icons.Default.EmojiEvents to DashboardGold
        NotificationType.SECURITY -> Icons.Default.Security to DashboardCyan
        NotificationType.INSIGHT -> Icons.Default.Insights to DashboardEmerald
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardNavyLight.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DashboardSoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notification.message,
                fontSize = 12.sp,
                color = DashboardSoftWhite.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = notification.timestamp,
            fontSize = 10.sp,
            color = DashboardSoftWhite.copy(alpha = 0.4f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 7. NET WORTH TRACKER
// Collapsible assets vs liabilities view
// ══════════════════════════════════════════════════════════════════

@Composable
fun NetWorthTracker(
    totalAssets: Double,
    totalLiabilities: Double,
    assetsTrend: Double = 0.0,
    liabilitiesTrend: Double = 0.0,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netWorth = totalAssets - totalLiabilities
    val netWorthColor = if (netWorth >= 0) DashboardEmerald else DashboardMutedRed

    val animatedNetWorth by animateFloatAsState(
        targetValue = netWorth.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "netWorth"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = netWorthColor.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DashboardNavyLight.copy(alpha = 0.95f),
                        DashboardNavy.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        netWorthColor.copy(alpha = 0.3f),
                        netWorthColor.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onToggleExpand() }
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Net Worth",
                        fontSize = 14.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCurrency(animatedNetWorth.toDouble()),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = netWorthColor
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = DashboardSoftWhite.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expanded details
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = DashboardSoftWhite.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Assets
                    NetWorthDetailRow(
                        label = "Total Assets",
                        value = totalAssets,
                        trend = assetsTrend,
                        color = DashboardEmerald,
                        icon = Icons.Default.TrendingUp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Liabilities
                    NetWorthDetailRow(
                        label = "Total Liabilities",
                        value = totalLiabilities,
                        trend = liabilitiesTrend,
                        color = DashboardMutedRed,
                        icon = Icons.Default.TrendingDown
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    if (totalAssets > 0 || totalLiabilities > 0) {
                        val total = totalAssets + totalLiabilities
                        val assetRatio = (totalAssets / total).toFloat()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(assetRatio.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(DashboardEmerald)
                            )
                            Box(
                                modifier = Modifier
                                    .weight((1f - assetRatio).coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(DashboardMutedRed)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Assets ${(assetRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = DashboardEmerald
                            )
                            Text(
                                text = "Liabilities ${((1f - assetRatio) * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = DashboardMutedRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetWorthDetailRow(
    label: String,
    value: Double,
    trend: Double,
    color: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = DashboardSoftWhite.copy(alpha = 0.8f)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCurrency(value),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            if (trend != 0.0) {
                Text(
                    text = "${if (trend > 0) "+" else ""}${formatCompactCurrency(trend)}",
                    fontSize = 11.sp,
                    color = if (trend > 0) DashboardEmerald else DashboardMutedRed
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ══════════════════════════════════════════════════════════════════

private fun formatCurrency(amount: Double): String {
    return if (amount < 0) {
        "-$${String.format("%,.2f", kotlin.math.abs(amount))}"
    } else {
        "$${String.format("%,.2f", amount)}"
    }
}

private fun formatCompactCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    val prefix = if (amount < 0) "-" else ""
    return when {
        absAmount >= 1_000_000 -> "$prefix$${"%.1f".format(absAmount / 1_000_000)}M"
        absAmount >= 1_000 -> "$prefix$${"%.1f".format(absAmount / 1_000)}K"
        else -> "$prefix$${"%.0f".format(absAmount)}"
    }
}

// ══════════════════════════════════════════════════════════════════
// COMPACT TRIPLE GAUGE ROW
// Three mini circular gauges for Financial Health, Goals, and Loans
// ══════════════════════════════════════════════════════════════════

@Composable
fun CompactTripleGaugeRow(
    summary: FinancialSummary,
    goalsSummary: GoalsSummary,
    loanSummary: LoanSummary,
    unpaidBillsCount: Int,
    totalBudget: Double,
    modifier: Modifier = Modifier
) {
    // Calculate financial health score
    val healthScore = remember(summary, goalsSummary, loanSummary, unpaidBillsCount) {
        calculateFinancialHealthScore(summary, goalsSummary, loanSummary, unpaidBillsCount, totalBudget)
    }

    // Goals progress
    val goalsProgress = if (goalsSummary.totalGoals > 0) {
        goalsSummary.completedGoals.toFloat() / goalsSummary.totalGoals.toFloat() * 100f
    } else 0f

    // Loans progress (repayment %)
    val loansProgress = if (loanSummary.totalBorrowed > 0) {
        (loanSummary.totalRepaid / loanSummary.totalBorrowed * 100).toFloat()
    } else 100f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DashboardEmerald.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DashboardNavyLight.copy(alpha = 0.95f),
                        DashboardNavy.copy(alpha = 0.98f),
                        DashboardNavyLight.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DashboardEmerald.copy(alpha = 0.3f),
                        DashboardCyan.copy(alpha = 0.2f),
                        DashboardGold.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Financial Health Gauge
        CompactGaugeItem(
            progress = healthScore.toFloat(),
            label = "Health",
            value = "${healthScore}%",
            color = when {
                healthScore >= 80 -> DashboardEmerald
                healthScore >= 60 -> DashboardGold
                healthScore >= 40 -> DashboardAmber
                else -> DashboardMutedRed
            },
            modifier = Modifier.weight(1f)
        )

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(60.dp)
                .background(DashboardSoftWhite.copy(alpha = 0.1f))
        )

        // Goals Progress Gauge
        CompactGaugeItem(
            progress = goalsProgress,
            label = "Goals",
            value = "${goalsSummary.completedGoals}/${goalsSummary.totalGoals}",
            color = DashboardCyan,
            modifier = Modifier.weight(1f)
        )

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(60.dp)
                .background(DashboardSoftWhite.copy(alpha = 0.1f))
        )

        // Loans Repayment Gauge
        CompactGaugeItem(
            progress = loansProgress,
            label = "Loans",
            value = if (loanSummary.activeLoans > 0) "${loansProgress.toInt()}%" else "None",
            color = when {
                loansProgress >= 80 -> DashboardEmerald
                loansProgress >= 50 -> DashboardGold
                loansProgress > 0 -> DashboardAmber
                else -> DashboardSoftWhite.copy(alpha = 0.5f)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactGaugeItem(
    progress: Float,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "gaugeProgress"
    )

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Mini circular gauge
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background arc
                drawArc(
                    color = DashboardSoftWhite.copy(alpha = 0.1f),
                    startAngle = -225f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                // Progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            color.copy(alpha = 0.4f),
                            color,
                            color.copy(alpha = 0.7f)
                        )
                    ),
                    startAngle = -225f,
                    sweepAngle = (animatedProgress / 100f) * 270f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Value inside gauge
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // Label below
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = DashboardSoftWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// COMPACT QUICK ACTIONS (NON-SCROLLING)
// 6 action buttons in 2 rows of 3
// ══════════════════════════════════════════════════════════════════

@Composable
fun CompactQuickActionsGrid(
    actions: List<QuickAction>,
    onActionClick: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = DashboardEmerald.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DashboardNavyLight.copy(alpha = 0.9f),
                        DashboardNavy.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        DashboardEmerald.copy(alpha = 0.2f),
                        DashboardCyan.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = "Quick Actions",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DashboardSoftWhite,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        // First row (3 actions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.take(3).forEach { action ->
                CompactActionButton(
                    action = action,
                    onClick = { onActionClick(action) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Second row (remaining actions)
        if (actions.size > 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions.drop(3).take(3).forEach { action ->
                    CompactActionButton(
                        action = action,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if less than 3 items
                repeat(3 - actions.drop(3).take(3).size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompactActionButton(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = if (action.isPrimary) {
                    Brush.verticalGradient(
                        colors = listOf(
                            action.color.copy(alpha = 0.25f),
                            action.color.copy(alpha = 0.12f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            DashboardSoftWhite.copy(alpha = 0.08f),
                            DashboardSoftWhite.copy(alpha = 0.03f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = action.color.copy(alpha = if (action.isPrimary) 0.4f else 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge indicator
            if (action.badge != null) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(DashboardMutedRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = action.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardSoftWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = action.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DashboardSoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

