package com.example.budgie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.budgie.data.model.Expense
import com.example.budgie.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Premium Wealth Dashboard Colors
private val WealthNavy = Color(0xFF0B1F2A)
private val WealthEmerald = Color(0xFF0FAE96)
private val WealthTeal = Color(0xFF0B8F7A)
private val WealthGold = Color(0xFFC9A14A)
private val WealthSoftWhite = Color(0xFFE6F1F0)
private val WealthMutedRed = Color(0xFFE57373)

enum class ChartPeriod {
    DAILY, WEEKLY, MONTHLY, ANNUAL
}

data class DailyExpenditure(
    val date: Long,
    val amount: Double,
    val count: Int
)

@Composable
fun ExpenditureSection(
    expenses: List<Expense>,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(ChartPeriod.DAILY) }

    val dailyData = remember(expenses, selectedPeriod) {
        // Only include entries with actual data (amount > 0)
        groupExpensesByDay(expenses, selectedPeriod).filter { it.amount > 0 }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            WealthSoftWhite.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expenditure Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                    Text(
                        text = getPeriodDescription(selectedPeriod),
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthSoftWhite.copy(alpha = 0.6f)
                    )
                }
            }

            // Period Toggle Chips - Premium Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodChip(
                    label = "Daily",
                    icon = Icons.Filled.CalendarToday,
                    isSelected = selectedPeriod == ChartPeriod.DAILY,
                    onClick = { selectedPeriod = ChartPeriod.DAILY }
                )
                PeriodChip(
                    label = "Weekly",
                    icon = Icons.Filled.CalendarViewWeek,
                    isSelected = selectedPeriod == ChartPeriod.WEEKLY,
                    onClick = { selectedPeriod = ChartPeriod.WEEKLY }
                )
                PeriodChip(
                    label = "Monthly",
                    icon = Icons.Filled.CalendarMonth,
                    isSelected = selectedPeriod == ChartPeriod.MONTHLY,
                    onClick = { selectedPeriod = ChartPeriod.MONTHLY }
                )
                PeriodChip(
                    label = "Annual",
                    icon = Icons.Filled.CalendarViewMonth,
                    isSelected = selectedPeriod == ChartPeriod.ANNUAL,
                    onClick = { selectedPeriod = ChartPeriod.ANNUAL }
                )
            }

            // Expenditure Table
            ExpenditureTable(dailyData, selectedPeriod)

            // Expenditure Chart
            ExpenditureChart(dailyData, selectedPeriod)
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = WealthSoftWhite.copy(alpha = 0.6f),
            iconColor = WealthSoftWhite.copy(alpha = 0.6f),
            selectedContainerColor = WealthEmerald.copy(alpha = 0.2f),
            selectedLabelColor = WealthEmerald,
            selectedLeadingIconColor = WealthEmerald
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) WealthEmerald else WealthSoftWhite.copy(alpha = 0.2f),
            selectedBorderColor = WealthEmerald
        ),
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun ExpenditureTable(
    data: List<DailyExpenditure>,
    period: ChartPeriod
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WealthNavy.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            WealthSoftWhite.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = WealthEmerald.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthEmerald,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Txns",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthEmerald,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthEmerald,
                    modifier = Modifier.weight(1.5f)
                )
            }

            // Table Rows
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = WealthSoftWhite.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WealthSoftWhite.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Start tracking to see your spending patterns",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.3f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                data.take(7).forEach { item ->
                    ExpenditureRow(item, period)
                    if (item != data.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = WealthSoftWhite.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Total Row
            if (data.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = WealthEmerald.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = WealthEmerald.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthEmerald,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "${data.sumOf { it.count }}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthEmerald,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = formatCurrency(data.sumOf { it.amount }),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthMutedRed,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenditureRow(
    item: DailyExpenditure,
    period: ChartPeriod
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = formatDateForPeriod(item.date, period),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = WealthSoftWhite
            )
            Text(
                text = formatDayOfWeek(item.date),
                style = MaterialTheme.typography.labelSmall,
                color = WealthSoftWhite.copy(alpha = 0.5f)
            )
        }
        Box(
            modifier = Modifier
                .weight(0.8f)
                .background(
                    color = WealthEmerald.copy(alpha = 0.15f),
                    shape = CircleShape
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${item.count}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = WealthEmerald
            )
        }
        Text(
            text = formatCurrency(item.amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = WealthMutedRed,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
private fun ExpenditureChart(
    data: List<DailyExpenditure>,
    period: ChartPeriod
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WealthNavy.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            WealthSoftWhite.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WealthSoftWhite
                )
                if (data.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingDown,
                            contentDescription = null,
                            tint = WealthMutedRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Avg: ${formatCurrency(data.map { it.amount }.average())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthSoftWhite.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = null,
                            tint = WealthSoftWhite.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No data to display",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Chart Canvas with transparent background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        val maxAmount = (data.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
                        val chartWidth = size.width
                        val chartHeight = size.height

                        // Calculate bar dimensions - handle single data point case
                        val numBars = data.size.coerceAtLeast(1)
                        val maxBarWidth = 60f // Maximum bar width
                        val minBarWidth = 20f // Minimum bar width
                        val calculatedBarWidth = (chartWidth / numBars) * 0.6f
                        val barWidth = calculatedBarWidth.coerceIn(minBarWidth, maxBarWidth)
                        val totalBarsWidth = barWidth * numBars
                        val totalSpacing = chartWidth - totalBarsWidth
                        val spacing = if (numBars > 1) totalSpacing / (numBars + 1) else (chartWidth - barWidth) / 2

                        // Draw horizontal grid lines
                        for (i in 0..4) {
                            val y = (chartHeight * i / 4)
                            drawLine(
                                color = WealthSoftWhite.copy(alpha = 0.1f),
                                start = Offset(0f, y),
                                end = Offset(chartWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw bars with premium gradient
                        data.forEachIndexed { index, item ->
                            val barHeight = ((item.amount / maxAmount) * chartHeight * 0.85f).toFloat()
                                .coerceAtLeast(4f) // Minimum visible height
                            val x = spacing + index * (barWidth + spacing)
                            val y = chartHeight - barHeight

                            // Bar with gradient
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        WealthEmerald,
                                        WealthTeal.copy(alpha = 0.8f)
                                    ),
                                    startY = y,
                                    endY = chartHeight
                                ),
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )

                            // Top highlight for 3D effect
                            drawRoundRect(
                                color = WealthEmerald.copy(alpha = 0.3f),
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, 4f.coerceAtMost(barHeight)),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }

                        // Draw trend line only if more than 1 data point
                        if (data.size > 1) {
                            val path = Path()
                            data.forEachIndexed { index, item ->
                                val x = spacing + index * (barWidth + spacing) + barWidth / 2
                                val y = chartHeight - ((item.amount / maxAmount) * chartHeight * 0.85f).toFloat()

                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = WealthGold,
                                style = Stroke(width = 3f, cap = StrokeCap.Round)
                            )

                            // Draw dots on trend line
                            data.forEachIndexed { index, item ->
                                val x = spacing + index * (barWidth + spacing) + barWidth / 2
                                val y = chartHeight - ((item.amount / maxAmount) * chartHeight * 0.85f).toFloat()

                                drawCircle(
                                    color = WealthGold,
                                    radius = 5f,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = WealthNavy,
                                    radius = 2.5f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }

                // X-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.forEach { item ->
                        Text(
                            text = formatDateShort(item.date, period),
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthSoftWhite.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Helper functions
private fun groupExpensesByDay(expenses: List<Expense>, period: ChartPeriod): List<DailyExpenditure> {
    val calendar = Calendar.getInstance()
    val now = calendar.timeInMillis

    val daysToGroup = when (period) {
        ChartPeriod.DAILY -> 7
        ChartPeriod.WEEKLY -> 4 * 7
        ChartPeriod.MONTHLY -> 12 * 30
        ChartPeriod.ANNUAL -> 5 * 365
    }

    calendar.add(Calendar.DAY_OF_YEAR, -daysToGroup)
    val startDate = calendar.timeInMillis

    val filteredExpenses = expenses.filter { it.date >= startDate && it.date <= now }

    return when (period) {
        ChartPeriod.DAILY -> groupByDay(filteredExpenses, daysToGroup)
        ChartPeriod.WEEKLY -> groupByWeek(filteredExpenses, 4)
        ChartPeriod.MONTHLY -> groupByMonth(filteredExpenses, 12)
        ChartPeriod.ANNUAL -> groupByYear(filteredExpenses, 5)
    }
}

private fun groupByDay(expenses: List<Expense>, days: Int): List<DailyExpenditure> {
    val calendar = Calendar.getInstance()
    val grouped = mutableMapOf<String, MutableList<Expense>>()

    for (i in 0 until days) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        grouped[dateKey] = mutableListOf()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    expenses.forEach { expense ->
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(expense.date))
        grouped[dateKey]?.add(expense)
    }

    return grouped.entries.sortedBy { it.key }.map { entry ->
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.key)?.time ?: 0L
        DailyExpenditure(
            date = date,
            amount = entry.value.sumOf { it.amount },
            count = entry.value.size
        )
    }.reversed().take(7)
}

private fun groupByWeek(expenses: List<Expense>, weeks: Int): List<DailyExpenditure> {
    return groupByDay(expenses, weeks * 7).chunked(7).map { weekExpenses ->
        DailyExpenditure(
            date = weekExpenses.first().date,
            amount = weekExpenses.sumOf { it.amount },
            count = weekExpenses.sumOf { it.count }
        )
    }
}

private fun groupByMonth(expenses: List<Expense>, months: Int): List<DailyExpenditure> {
    val calendar = Calendar.getInstance()
    val grouped = mutableMapOf<String, MutableList<Expense>>()

    for (i in 0 until months) {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        grouped[monthKey] = mutableListOf()
        calendar.add(Calendar.MONTH, -1)
    }

    expenses.forEach { expense ->
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(expense.date))
        grouped[monthKey]?.add(expense)
    }

    return grouped.entries.sortedBy { it.key }.map { entry ->
        val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(entry.key)?.time ?: 0L
        DailyExpenditure(
            date = date,
            amount = entry.value.sumOf { it.amount },
            count = entry.value.size
        )
    }.reversed()
}

private fun groupByYear(expenses: List<Expense>, years: Int): List<DailyExpenditure> {
    val calendar = Calendar.getInstance()
    val grouped = mutableMapOf<String, MutableList<Expense>>()

    for (i in 0 until years) {
        val yearKey = SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)
        grouped[yearKey] = mutableListOf()
        calendar.add(Calendar.YEAR, -1)
    }

    expenses.forEach { expense ->
        val yearKey = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(expense.date))
        grouped[yearKey]?.add(expense)
    }

    return grouped.entries.sortedBy { it.key }.map { entry ->
        val date = SimpleDateFormat("yyyy", Locale.getDefault()).parse(entry.key)?.time ?: 0L
        DailyExpenditure(
            date = date,
            amount = entry.value.sumOf { it.amount },
            count = entry.value.size
        )
    }.reversed()
}

private fun formatDateForPeriod(timestamp: Long, period: ChartPeriod): String {
    val date = Date(timestamp)
    return when (period) {
        ChartPeriod.DAILY -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        ChartPeriod.WEEKLY -> "Week of ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)}"
        ChartPeriod.MONTHLY -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
        ChartPeriod.ANNUAL -> SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
    }
}

private fun formatDateShort(timestamp: Long, period: ChartPeriod): String {
    val date = Date(timestamp)
    return when (period) {
        ChartPeriod.DAILY -> SimpleDateFormat("dd", Locale.getDefault()).format(date)
        ChartPeriod.WEEKLY -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        ChartPeriod.MONTHLY -> SimpleDateFormat("MMM", Locale.getDefault()).format(date)
        ChartPeriod.ANNUAL -> SimpleDateFormat("yy", Locale.getDefault()).format(date)
    }
}

private fun formatDayOfWeek(timestamp: Long): String {
    return SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
}

private fun getPeriodDescription(period: ChartPeriod): String {
    return when (period) {
        ChartPeriod.DAILY -> "Last 7 days"
        ChartPeriod.WEEKLY -> "Last 4 weeks"
        ChartPeriod.MONTHLY -> "Last 12 months"
        ChartPeriod.ANNUAL -> "Last 5 years"
    }
}
