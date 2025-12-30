package com.example.budgie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.data.model.*
import com.example.budgie.ui.viewmodel.MainViewModel

// Premium Wealth Dashboard Colors
private val WealthNavy = Color(0xFF0B1F2A)
private val WealthEmerald = Color(0xFF0FAE96)
private val WealthTeal = Color(0xFF0B8F7A)
private val WealthGold = Color(0xFFC9A14A)
private val WealthGoldLight = Color(0xFFFFD166)
private val WealthSoftWhite = Color(0xFFE6F1F0)
private val WealthMutedRed = Color(0xFFE57373)
private val WealthAmber = Color(0xFFFFB74D)
private val WealthBlue = Color(0xFF5C9CE5)
private val WealthCyan = Color(0xFF26A69A)

// Glassmorphism Colors
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassHighlight = Color.White.copy(alpha = 0.15f)

// Glassmorphic Card Modifier
private fun Modifier.glassmorphicCard(
    cornerRadius: Int = 16,
    accentColor: Color = WealthEmerald
) = this
    .shadow(8.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = accentColor.copy(alpha = 0.2f))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(GlassWhite, GlassWhite.copy(alpha = 0.04f))
        )
    )
    .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius.dp))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val summary by viewModel.financialSummary.collectAsState()
    val spendingInsights by viewModel.spendingInsights.collectAsState()
    val investmentSuggestions by viewModel.investmentSuggestions.collectAsState()
    val wealthProjection by viewModel.wealthProjection.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    val hasData = summary.totalIncome > 0 || summary.totalExpenses > 0

    // AI pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    Scaffold(
        containerColor = WealthNavy,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Animated AI Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            WealthEmerald.copy(alpha = pulseAlpha),
                                            WealthEmerald.copy(alpha = 0.1f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = WealthSoftWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "AI Insights",
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WealthSoftWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphicCard(accentColor = WealthEmerald),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WealthEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = WealthEmerald,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Budgie AI Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthSoftWhite
                            )
                            Text(
                                text = if (hasData) "Personalized insights based on your data"
                                       else "Start tracking to unlock AI insights",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthSoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Financial Summary Card
            item {
                FinancialSummaryCard(summary = summary)
            }

            // Spending Insights Section
            if (spendingInsights.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Spending Insights",
                        icon = Icons.Default.Lightbulb,
                        color = WealthGold
                    )
                }

                items(spendingInsights) { insight ->
                    SpendingInsightCard(insight = insight)
                }
            }

            // Investment Suggestions Section
            if (investmentSuggestions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Investment Ideas",
                        icon = Icons.Default.TrendingUp,
                        color = WealthEmerald
                    )
                }

                items(investmentSuggestions) { suggestion ->
                    InvestmentSuggestionCard(suggestion = suggestion)
                }
            }

            // Wealth Projection Section
            item {
                WealthProjectionCard(projection = wealthProjection)
            }

            // Empty State
            if (!hasData) {
                item {
                    EmptyStateCard()
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WealthSoftWhite
        )
    }
}

@Composable
private fun FinancialSummaryCard(summary: FinancialSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(accentColor = WealthTeal),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Financial Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WealthSoftWhite
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Income",
                    value = "$${String.format("%.0f", summary.totalIncome)}",
                    color = WealthEmerald
                )
                MetricItem(
                    label = "Expenses",
                    value = "$${String.format("%.0f", summary.totalExpenses)}",
                    color = WealthMutedRed
                )
                MetricItem(
                    label = "Savings",
                    value = "${String.format("%.1f", summary.savingsRate)}%",
                    color = WealthGold
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WealthSoftWhite.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SpendingInsightCard(insight: SpendingInsight) {
    val priorityColor = when (insight.priority) {
        InsightPriority.CRITICAL -> WealthMutedRed
        InsightPriority.HIGH -> WealthAmber
        InsightPriority.MEDIUM -> WealthGold
        InsightPriority.LOW -> WealthEmerald
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(accentColor = priorityColor),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(priorityColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (insight.type) {
                        InsightType.BUDGET_WARNING -> Icons.Default.Warning
                        InsightType.SAVING_OPPORTUNITY -> Icons.Default.Savings
                        InsightType.OVERSPENDING -> Icons.Default.TrendingDown
                        InsightType.GOOD_HABIT -> Icons.Default.ThumbUp
                        else -> Icons.Default.Lightbulb
                    },
                    contentDescription = null,
                    tint = priorityColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WealthSoftWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthSoftWhite.copy(alpha = 0.8f)
                )
                if (insight.actionable.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 ${insight.actionable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestmentSuggestionCard(suggestion: InvestmentSuggestion) {
    val riskColor = when (suggestion.riskLevel) {
        RiskLevel.LOW -> WealthEmerald
        RiskLevel.MEDIUM -> WealthAmber
        RiskLevel.HIGH -> WealthMutedRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(accentColor = WealthBlue),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text(
                        text = suggestion.type.icon,
                        fontSize = 24.sp
                    )
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = suggestion.riskLevel.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Expected: ${suggestion.expectedReturn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthEmerald
                )
                Text(
                    text = "Min: $${String.format("%.0f", suggestion.minimumAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthSoftWhite.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun WealthProjectionCard(projection: WealthProjection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(accentColor = WealthGold),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = WealthGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Wealth Projection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthSoftWhite
                )
            }

            Text(
                text = "Based on ${String.format("%.0f", projection.assumedReturnRate * 100)}% annual return",
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.6f)
            )

            val projectedIn10Years = projection.projectedNetWorth[10] ?: 0.0
            val projectedIn30Years = projection.projectedNetWorth[30] ?: 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProjectionItem(
                    label = "10 Years",
                    value = "$${String.format("%.0f", projectedIn10Years)}"
                )
                ProjectionItem(
                    label = "30 Years",
                    value = "$${String.format("%.0f", projectedIn30Years)}"
                )
            }

            if (projection.monthlyContribution > 0) {
                Text(
                    text = "Contributing $${String.format("%.0f", projection.monthlyContribution)}/month",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthEmerald,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProjectionItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = WealthGold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WealthSoftWhite.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                tint = WealthEmerald.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Start Your Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WealthSoftWhite
            )
            Text(
                text = "Add your income and expenses to unlock personalized AI insights and investment recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                color = WealthSoftWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

