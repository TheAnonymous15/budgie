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
import com.example.budgie.ui.components.*
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ai.pipeline.PipelineAnalysis
import com.example.budgie.ai.pipeline.Insight
import com.example.budgie.ai.pipeline.ActionItem
import com.example.budgie.ai.pipeline.Urgency
import com.example.budgie.ai.pipeline.BehaviorProfile
import com.example.budgie.ai.pipeline.RiskAssessment
import com.example.budgie.ai.pipeline.SpendingForecast
import com.example.budgie.ai.pipeline.RiskLevel as PipelineRiskLevel
import com.example.budgie.ai.pipeline.InsightType as PipelineInsightType

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
    borderWidth: Float = 1f
) = this
    .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = Color.Black.copy(alpha = 0.3f),
        spotColor = Color.Black.copy(alpha = 0.3f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(GlassHighlight, GlassWhite, Color.White.copy(alpha = 0.05f))
        )
    )
    .border(
        width = borderWidth.dp,
        brush = Brush.verticalGradient(colors = listOf(GlassBorder, Color.White.copy(alpha = 0.05f))),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// Accent Glassmorphic Card
private fun Modifier.glassmorphicAccentCard(
    accentColor: Color,
    cornerRadius: Int = 16
) = this
    .shadow(6.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = accentColor.copy(alpha = 0.2f))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.08f), accentColor.copy(alpha = 0.05f))
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.3f), accentColor.copy(alpha = 0.1f))),
        RoundedCornerShape(cornerRadius.dp)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val summary by viewModel.financialSummary.collectAsState()

    // AI/ML Pipeline Data
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
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
                                tint = WealthEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "Budgie AI Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthSoftWhite
                            )
                            Text(
                                "AI-Powered Financial Insights",
                                style = MaterialTheme.typography.labelSmall,
                                color = WealthSoftWhite.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = WealthSoftWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAiAnalysis() }) {
                        Icon(
                            Icons.Default.Refresh,
                            "Refresh",
                            tint = if (isAiLoading) WealthAmber else WealthEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WealthNavy)
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
            // AI Status Card - Glassmorphic
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphicAccentCard(WealthEmerald, 20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(WealthEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isAiLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = WealthEmerald,
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = WealthEmerald,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isAiLoading) "AI Analyzing..." else "AI Analysis Ready",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthSoftWhite
                                )
                                Text(
                                    text = aiAnalysis?.insights?.greeting ?: "Personalized insights based on your data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthSoftWhite.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (aiAnalysis != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = aiAnalysis?.insights?.summary ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WealthSoftWhite.copy(alpha = 0.9f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // AI/ML Generated Insights
            aiAnalysis?.insights?.insights?.let { aiInsights ->
                if (aiInsights.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = WealthGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI-Generated Insights",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthSoftWhite
                                )
                            }
                            Text(
                                text = "${aiInsights.size} insights",
                                style = MaterialTheme.typography.labelMedium,
                                color = WealthEmerald
                            )
                        }
                    }

                    items(aiInsights) { insight ->
                        AIInsightCard(insight = insight)
                    }
                }
            }

            // Action Items from AI
            aiAnalysis?.insights?.actionItems?.let { actions ->
                if (actions.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WealthCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recommended Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthSoftWhite
                            )
                        }
                    }

                    items(actions) { action ->
                        ActionItemCard(action = action)
                    }
                }
            }

            // Behavior Analysis Card
            aiAnalysis?.behaviorProfile?.let { profile ->
                item {
                    BehaviorAnalysisCard(profile = profile)
                }
            }

            // Risk Assessment Card
            aiAnalysis?.riskAssessment?.let { risk ->
                item {
                    RiskAssessmentCard(risk = risk)
                }
            }

            // Forecast Card
            aiAnalysis?.forecast?.let { forecast ->
                item {
                    ForecastCard(forecast = forecast)
                }
            }

            // No Data State
            if (!hasData && aiAnalysis == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(20)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = WealthSoftWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Data for AI Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthSoftWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start tracking your income and expenses to unlock personalized AI insights.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WealthSoftWhite.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Ask Budgie AI Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AskBudgieCard()
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// AI Insight Card Component
@Composable
fun AIInsightCard(insight: Insight) {
    val priorityColor = when {
        insight.priority >= 8 -> WealthMutedRed
        insight.priority >= 5 -> WealthAmber
        else -> WealthEmerald
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(priorityColor, 16)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(priorityColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (insight.type) {
                        PipelineInsightType.RISK -> Icons.Default.Shield
                        PipelineInsightType.BEHAVIOR -> Icons.Default.Psychology
                        PipelineInsightType.FORECAST -> Icons.Default.Timeline
                        PipelineInsightType.ANOMALY -> Icons.Default.Warning
                        PipelineInsightType.SAVINGS -> Icons.Default.Savings
                        PipelineInsightType.CATEGORY -> Icons.Default.Category
                        PipelineInsightType.ACHIEVEMENT -> Icons.Default.EmojiEvents
                    },
                    contentDescription = null,
                    tint = priorityColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(insight.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WealthSoftWhite
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthSoftWhite.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
                if (insight.actionable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = WealthGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Action recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Action Item Card
@Composable
fun ActionItemCard(action: ActionItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(14)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (action.urgency) {
                            Urgency.HIGH -> WealthMutedRed.copy(alpha = 0.2f)
                            Urgency.MEDIUM -> WealthAmber.copy(alpha = 0.2f)
                            Urgency.LOW -> WealthCyan.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = when (action.urgency) {
                        Urgency.HIGH -> WealthMutedRed
                        Urgency.MEDIUM -> WealthAmber
                        Urgency.LOW -> WealthCyan
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.action,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WealthSoftWhite
                )
                Text(
                    text = action.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = WealthSoftWhite.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Behavior Analysis Card
@Composable
fun BehaviorAnalysisCard(profile: BehaviorProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(WealthBlue, 18)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = WealthBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Behavior Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthSoftWhite
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Cluster Label
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(WealthBlue.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = profile.clusterName,
                    style = MaterialTheme.typography.labelMedium,
                    color = WealthBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            if (profile.dominantTraits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                profile.dominantTraits.take(3).forEach { trait ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(WealthBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trait.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// Risk Assessment Card
@Composable
fun RiskAssessmentCard(risk: RiskAssessment) {
    val riskColor = when (risk.riskLevel) {
        PipelineRiskLevel.MINIMAL -> WealthEmerald
        PipelineRiskLevel.LOW -> WealthCyan
        PipelineRiskLevel.MEDIUM -> WealthAmber
        PipelineRiskLevel.HIGH -> WealthMutedRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(riskColor, 18)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Risk Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(riskColor.copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${risk.riskLevel.name} Risk",
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Risk Score Progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Risk Score", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                    Text("${(risk.overallScore * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = riskColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { risk.overallScore },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = riskColor,
                    trackColor = riskColor.copy(alpha = 0.2f)
                )
            }

            if (risk.riskFactors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Risk Factors:",
                    style = MaterialTheme.typography.labelMedium,
                    color = WealthSoftWhite.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                risk.riskFactors.take(3).forEach { factor ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = riskColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(factor.description, style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// Forecast Card
@Composable
fun ForecastCard(forecast: SpendingForecast) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(WealthTeal, 18)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    tint = WealthTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AI Spending Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthSoftWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Predicted Total", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                    Text(
                        formatCurrency(forecast.totalPredicted.toDouble()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthTeal
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Historical Avg", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                    Text(
                        formatCurrency(forecast.historicalAverage.toDouble()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Budget overrun probability
            val overrunColor = when {
                forecast.budgetOverrunProbability > 0.7f -> WealthMutedRed
                forecast.budgetOverrunProbability > 0.4f -> WealthAmber
                else -> WealthEmerald
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(overrunColor.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Budget Overrun Risk",
                    style = MaterialTheme.typography.labelMedium,
                    color = WealthSoftWhite.copy(alpha = 0.8f)
                )
                Text(
                    "${(forecast.budgetOverrunProbability * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = overrunColor
                )
            }

            // Show insight
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = WealthTeal, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(forecast.getInsight(), style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.8f))
            }
        }
    }
}

// KPI Status enum
enum class KPIStatus {
    EXCELLENT, GOOD, ATTENTION, NEUTRAL
}

@Composable
fun PremiumKPIChip(
    label: String,
    value: String,
    status: KPIStatus,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (status) {
        KPIStatus.EXCELLENT -> Pair(WealthEmerald, "Excellent")
        KPIStatus.GOOD -> Pair(WealthBlue, "Good")
        KPIStatus.ATTENTION -> Pair(WealthAmber, "Needs Attention")
        KPIStatus.NEUTRAL -> Pair(WealthSoftWhite.copy(alpha = 0.5f), "No data")
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            statusColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = WealthSoftWhite.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            if (status != KPIStatus.NEUTRAL) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PriorityInsightCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WealthNavy.copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            WealthAmber.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(WealthAmber, CircleShape)
                )
                Text(
                    text = "Priority Insight",
                    style = MaterialTheme.typography.labelMedium,
                    color = WealthAmber,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WealthSoftWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = WealthSoftWhite.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WealthAmber
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    actionLabel,
                    color = WealthNavy,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PremiumInsightCard(insight: SpendingInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            WealthSoftWhite.copy(alpha = 0.1f)
        )
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
                    .size(36.dp)
                    .background(
                        WealthEmerald.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = WealthEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WealthSoftWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthSoftWhite.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun BudgetRuleCard(
    income: Double,
    hasData: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            WealthEmerald.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = WealthEmerald,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "50 / 30 / 20 Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthSoftWhite
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A proven budgeting framework aligned with your income level.",
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Visual breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetRuleSegment(
                    label = "Needs",
                    percentage = 50,
                    amount = if (hasData) income * 0.5 else null,
                    color = WealthBlue,
                    modifier = Modifier.weight(5f)
                )
                BudgetRuleSegment(
                    label = "Wants",
                    percentage = 30,
                    amount = if (hasData) income * 0.3 else null,
                    color = WealthAmber,
                    modifier = Modifier.weight(3f)
                )
                BudgetRuleSegment(
                    label = "Save",
                    percentage = 20,
                    amount = if (hasData) income * 0.2 else null,
                    color = WealthEmerald,
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Customize */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WealthSoftWhite
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        WealthSoftWhite.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Customize", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = { /* Apply */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WealthEmerald
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Rule", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun BudgetRuleSegment(
    label: String,
    percentage: Int,
    amount: Double?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WealthSoftWhite.copy(alpha = 0.6f)
        )
        if (amount != null) {
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.labelSmall,
                color = WealthSoftWhite.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun RecommendationCard(
    icon: ImageVector,
    title: String,
    description: String,
    detail: String,
    actionLabel: String,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (expanded) accentColor.copy(alpha = 0.4f) else WealthSoftWhite.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                accentColor.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WealthSoftWhite
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = WealthSoftWhite.copy(alpha = 0.5f)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthSoftWhite.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            accentColor.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { /* Action */ },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = accentColor
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WealthProjectionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WealthEmerald.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            WealthEmerald.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = WealthEmerald,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your Potential",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WealthSoftWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "If you save \$200/month",
                style = MaterialTheme.typography.bodyMedium,
                color = WealthSoftWhite.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "\$2,400/year",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = WealthEmerald
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Start tracking to see your personalized projections",
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AskBudgieCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Open AI chat */ },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            Brush.linearGradient(
                colors = listOf(WealthEmerald, WealthGold)
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            WealthEmerald.copy(alpha = 0.15f),
                            WealthGold.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(WealthEmerald, WealthTeal)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Ask Budgie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        Text(
                            text = "\"How can I save more this month?\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = WealthEmerald,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    PremiumKPIChip(
        label = label,
        value = value,
        status = KPIStatus.NEUTRAL,
        icon = Icons.Default.Analytics,
        modifier = modifier
    )
}

@Composable
fun TipCard(
    title: String,
    description: String
) {
    RecommendationCard(
        icon = Icons.Default.Lightbulb,
        title = title,
        description = description,
        detail = "",
        actionLabel = "Learn More",
        accentColor = WealthGold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val suggestions by viewModel.investmentSuggestions.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val userSavings by viewModel.userSavings.collectAsState()
    val riskTolerance by viewModel.riskTolerance.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var savingsInput by remember { mutableStateOf(userSavings.toString()) }
    var selectedRisk by remember { mutableStateOf(riskTolerance) }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Investment Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = savingsInput,
                        onValueChange = { savingsInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Current Savings") },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Risk Tolerance")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RiskLevel.values().forEach { risk ->
                            FilterChip(
                                selected = selectedRisk == risk,
                                onClick = { selectedRisk = risk },
                                label = { Text(risk.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(risk.color).copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    savingsInput.toDoubleOrNull()?.let { viewModel.updateUserSavings(it) }
                    viewModel.updateRiskTolerance(selectedRisk)
                    showSettingsDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investment Suggestions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1565C0)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Monthly Investable Amount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatCurrency(summary.netSavings.coerceAtLeast(0.0)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Risk Profile: ${riskTolerance.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Personalized Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(suggestions) { suggestion ->
                InvestmentCard(suggestion = suggestion)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
