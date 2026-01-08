package com.example.budgie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgie.ui.viewmodel.MainViewModel

@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Income",
                    value = "KES ${String.format("%,.2f", summary.totalIncome)}",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF10B981)
                )
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    title = "Expenses",
                    value = "KES ${String.format("%,.2f", summary.totalExpenses)}",
                    icon = Icons.Default.TrendingDown,
                    color = Color(0xFFEF4444)
                )
            }
        }

        // Spending by Category
        item {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            val categoryTotals = expenses.groupBy { it.category.name }
                .mapValues { (_, items) -> items.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1a1a2e),
                                Color(0xFF16213e)
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (categoryTotals.isEmpty()) {
                    Text(
                        text = "No expense data available",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    categoryTotals.forEach { (category, total) ->
                        CategoryBar(
                            category = category,
                            amount = total,
                            percentage = if (summary.totalExpenses > 0) (total / summary.totalExpenses * 100) else 0.0
                        )
                    }
                }
            }
        }

        // Monthly Trend
        item {
            Text(
                text = "Monthly Trend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1a1a2e),
                                Color(0xFF16213e)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📊 Chart visualization coming soon",
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CategoryBar(
    category: String,
    amount: Double,
    percentage: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "KES ${String.format("%,.2f", amount)} (${String.format("%.1f", percentage)}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percentage / 100).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00F5A0),
                                Color(0xFF00D9F5)
                            )
                        )
                    )
            )
        }
    }
}

