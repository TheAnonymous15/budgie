package com.example.budgie.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.ai.pipeline.*

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

/**
 * AI-Powered Dashboard Summary Card
 * Shows financial health score, behavior profile, and top insights
 */
@Composable
fun AIDashboardCard(
    dashboardSummary: DashboardSummary?,
    aiAnalysis: PipelineAnalysis?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with AI branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // AI Icon with pulse animation
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(if (isLoading) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(WealthEmerald, WealthTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "AI",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Budgie AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        Text(
                            text = "Powered by on-device ML",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.5f)
                        )
                    }
                }

                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = WealthEmerald,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = WealthSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dashboardSummary != null) {
                // Health Score & Behavior Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Health Score Card
                    HealthScoreCard(
                        score = dashboardSummary.healthScore,
                        riskLevel = dashboardSummary.riskLevel,
                        modifier = Modifier.weight(1f)
                    )

                    // Behavior Profile Card
                    BehaviorProfileCard(
                        behaviorType = dashboardSummary.behaviorType,
                        savingsRate = dashboardSummary.savingsRate,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AI Insight Preview
                aiAnalysis?.insights?.insights?.firstOrNull()?.let { insight ->
                    InsightPreviewCard(
                        insight = insight,
                        onClick = onViewDetails
                    )
                }

                // Quick Stats Row
                if (dashboardSummary.anomalyCount > 0 || dashboardSummary.actionRequired) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (dashboardSummary.anomalyCount > 0) {
                            QuickStatChip(
                                icon = Icons.Default.Warning,
                                text = "${dashboardSummary.anomalyCount} unusual",
                                color = WealthAmber
                            )
                        }
                        if (dashboardSummary.actionRequired) {
                            QuickStatChip(
                                icon = Icons.Default.Flag,
                                text = "Action needed",
                                color = WealthMutedRed
                            )
                        }
                        TrendChip(trend = dashboardSummary.forecastTrend)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // View Details Button
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WealthEmerald.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "View Full AI Analysis",
                        color = WealthEmerald
                    )
                }
            } else if (!isLoading) {
                // No data state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = WealthSoftWhite.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start tracking to unlock AI insights",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WealthSoftWhite.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthScoreCard(
    score: Int,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val scoreColor = when (riskLevel) {
        RiskLevel.MINIMAL -> WealthEmerald
        RiskLevel.LOW -> WealthCyan
        RiskLevel.MEDIUM -> WealthAmber
        RiskLevel.HIGH -> WealthMutedRed
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Health Score",
                style = MaterialTheme.typography.labelSmall,
                color = WealthSoftWhite.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = "/100",
                style = MaterialTheme.typography.labelSmall,
                color = WealthSoftWhite.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = scoreColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = riskLevel.name.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
private fun BehaviorProfileCard(
    behaviorType: String,
    savingsRate: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Money Profile",
                style = MaterialTheme.typography.labelSmall,
                color = WealthSoftWhite.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                when {
                    behaviorType.contains("Saver") -> Icons.Default.Savings
                    behaviorType.contains("Balanced") -> Icons.Default.Balance
                    behaviorType.contains("Active") -> Icons.Default.Speed
                    behaviorType.contains("Essential") -> Icons.Default.Home
                    else -> Icons.Default.Paid
                },
                contentDescription = null,
                tint = WealthTeal,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = behaviorType,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = WealthSoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saving $savingsRate%",
                style = MaterialTheme.typography.labelSmall,
                color = if (savingsRate >= 20) WealthEmerald else WealthAmber
            )
        }
    }
}

@Composable
private fun InsightPreviewCard(
    insight: Insight,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.type) {
                InsightType.RISK -> WealthMutedRed.copy(alpha = 0.15f)
                InsightType.ANOMALY -> WealthAmber.copy(alpha = 0.15f)
                InsightType.SAVINGS -> WealthEmerald.copy(alpha = 0.15f)
                else -> WealthTeal.copy(alpha = 0.15f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = insight.emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = WealthSoftWhite
                )
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthSoftWhite.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = WealthSoftWhite.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun QuickStatChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun TrendChip(trend: ForecastTrend) {
    val (icon, text, color) = when (trend) {
        ForecastTrend.INCREASING_FAST -> Triple(Icons.Default.TrendingUp, "Rising fast", WealthMutedRed)
        ForecastTrend.INCREASING -> Triple(Icons.Default.TrendingUp, "Rising", WealthAmber)
        ForecastTrend.STABLE -> Triple(Icons.Default.TrendingFlat, "Stable", WealthCyan)
        ForecastTrend.DECREASING -> Triple(Icons.Default.TrendingDown, "Falling", WealthEmerald)
        ForecastTrend.DECREASING_FAST -> Triple(Icons.Default.TrendingDown, "Falling fast", WealthEmerald)
        ForecastTrend.UNKNOWN -> Triple(Icons.Default.Help, "Unknown", WealthSoftWhite)
    }

    QuickStatChip(icon = icon, text = text, color = color)
}

