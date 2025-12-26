package com.example.budgie.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgie.data.model.*
import com.example.budgie.ui.components.*
import com.example.budgie.ui.theme.WealthTheme
import com.example.budgie.ui.viewmodel.MainViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthProjectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val projection by viewModel.wealthProjection.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val userSavings by viewModel.userSavings.collectAsState()

    var showInputDialog by remember { mutableStateOf(false) }
    var savingsInput by remember { mutableStateOf(userSavings.toString()) }
    var yearsToProject by remember { mutableStateOf(30) }
    var returnRate by remember { mutableStateOf(7.0) }

    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = {
                Text(
                    "Projection Settings",
                    color = WealthTheme.SoftWhite
                )
            },
            containerColor = WealthTheme.NavyMid,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = savingsInput,
                        onValueChange = { savingsInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Current Net Worth", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                        prefix = { Text("$", color = WealthTheme.Emerald) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WealthTheme.Emerald,
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        "Expected Annual Return: ${String.format("%.1f", returnRate)}%",
                        color = WealthTheme.SoftWhite
                    )
                    Slider(
                        value = returnRate.toFloat(),
                        onValueChange = { returnRate = it.toDouble() },
                        valueRange = 1f..15f,
                        steps = 27,
                        colors = SliderDefaults.colors(
                            thumbColor = WealthTheme.Emerald,
                            activeTrackColor = WealthTheme.Emerald
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    savingsInput.toDoubleOrNull()?.let { viewModel.updateUserSavings(it) }
                    showInputDialog = false
                }) {
                    Text("Apply", color = WealthTheme.Emerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text("Cancel", color = WealthTheme.SoftWhite.copy(alpha = 0.6f))
                }
            }
        )
    }

    Scaffold(
        containerColor = WealthTheme.Navy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wealth Projection",
                        color = WealthTheme.SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WealthTheme.SoftWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { showInputDialog = true }) {
                        Icon(Icons.Default.Edit, "Settings", tint = WealthTheme.Emerald)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WealthTheme.Navy
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Current Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            WealthTheme.Emerald.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WealthTheme.Emerald.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Current Net Worth",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatCurrency(projection.currentNetWorth),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.Emerald
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Contribution",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatCurrency(projection.monthlyContribution),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = WealthTheme.SoftWhite
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Expected Return",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${String.format("%.1f", projection.assumedReturnRate * 100)}% / year",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = WealthTheme.SoftWhite
                                )
                            }
                        }
                    }
                }
            }

            // Projection Chart
            item {
                Text(
                    text = "Wealth Growth Projection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .border(
                            1.dp,
                            WealthTheme.SoftWhite.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    WealthChart(
                        projections = projection.projectedNetWorth,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }

            // Milestone projections
            item {
                Text(
                    text = "Key Milestones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite
                )
            }

            val milestones = listOf(5, 10, 15, 20, 25, 30)
            items(milestones.filter { projection.projectedNetWorth.containsKey(it) }) { year ->
                PremiumMilestoneCard(
                    year = year,
                    amount = projection.projectedNetWorth[year] ?: 0.0,
                    currentNetWorth = projection.currentNetWorth
                )
            }

            // Projection explanation
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            WealthTheme.Gold.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WealthTheme.Gold.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = WealthTheme.Gold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How it works",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This projection assumes:\n" +
                                    "• Monthly savings of ${formatCurrency(projection.monthlyContribution)}\n" +
                                    "• Annual return rate of ${String.format("%.1f", projection.assumedReturnRate * 100)}%\n" +
                                    "• Compound interest calculated monthly\n\n" +
                                    "Actual results may vary based on market conditions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun WealthChart(
    projections: Map<Int, Double>,
    modifier: Modifier = Modifier
) {
    // Define colors as concrete Color objects (not using WealthTheme inside Canvas)
    val chartColor = Color(0xFF0FAE96) // Emerald
    val pointInnerColor = Color.White
    val gridColor = Color(0xFFE6F1F0).copy(alpha = 0.1f)
    val emptyTextColor = Color(0xFFE6F1F0).copy(alpha = 0.5f)

    if (projections.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Add income and expenses to see projections",
                style = MaterialTheme.typography.bodyMedium,
                color = emptyTextColor
            )
        }
        return
    }

    val sortedData = projections.toSortedMap()
    val maxValue = (sortedData.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val minValue = sortedData.values.minOrNull() ?: 0.0

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 20f

        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // Skip if dimensions are invalid
        if (chartWidth <= 0 || chartHeight <= 0 || sortedData.isEmpty()) return@Canvas

        // Draw grid lines
        for (i in 0..4) {
            val y = padding + (chartHeight * i / 4)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Calculate points
        val dataEntries = sortedData.entries.toList()
        val points = dataEntries.mapIndexed { index, entry ->
            val x = if (dataEntries.size == 1) {
                width / 2
            } else {
                padding + (index.toFloat() / (dataEntries.size - 1)) * chartWidth
            }
            val normalizedValue = if (maxValue > minValue) {
                (entry.value - minValue) / (maxValue - minValue)
            } else {
                0.5
            }
            val y = padding + chartHeight - (normalizedValue.toFloat() * chartHeight * 0.9f)
            Offset(x, y.coerceIn(padding, padding + chartHeight))
        }

        // Draw the line connecting points
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = path,
                color = chartColor,
                style = Stroke(width = 3f)
            )
        }

        // Draw points with glow effect
        points.forEach { point ->
            // Outer glow
            drawCircle(
                color = chartColor.copy(alpha = 0.3f),
                radius = 12f,
                center = point
            )
            // Main point
            drawCircle(
                color = chartColor,
                radius = 8f,
                center = point
            )
            // Inner highlight
            drawCircle(
                color = pointInnerColor,
                radius = 4f,
                center = point
            )
        }
    }
}

@Composable
fun PremiumMilestoneCard(
    year: Int,
    amount: Double,
    currentNetWorth: Double
) {
    val growth = if (currentNetWorth > 0) ((amount - currentNetWorth) / currentNetWorth) * 100 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                WealthTheme.SoftWhite.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Year $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite
                )
                Text(
                    text = "+${String.format("%.0f", growth)}% growth",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthTheme.Emerald
                )
            }
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WealthTheme.Emerald
            )
        }
    }
}

@Composable
fun MilestoneCard(
    year: Int,
    amount: Double,
    currentNetWorth: Double
) {
    PremiumMilestoneCard(year, amount, currentNetWorth)
}

