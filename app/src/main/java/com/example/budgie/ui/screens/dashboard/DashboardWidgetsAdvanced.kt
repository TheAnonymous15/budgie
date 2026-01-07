package com.example.budgie.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ═══════════════════════════════════════════════════════════════════
   ADVANCED DASHBOARD WIDGETS
   Animated backgrounds, swipeable actions, and more
═══════════════════════════════════════════════════════════════════ */

// ══════════════════════════════════════════════════════════════════
// 8. ANIMATED PARTICLE BACKGROUND
// Subtle floating particles for premium feel
// ══════════════════════════════════════════════════════════════════

data class Particle(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float,
    val color: Color
)

@Composable
fun AnimatedParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    particleColors: List<Color> = listOf(
        DashboardEmerald.copy(alpha = 0.3f),
        DashboardCyan.copy(alpha = 0.2f),
        DashboardTeal.copy(alpha = 0.25f)
    )
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val particles = remember(size) {
        if (size.width > 0 && size.height > 0) {
            List(particleCount) {
                Particle(
                    x = Random.nextFloat() * size.width,
                    y = Random.nextFloat() * size.height,
                    size = Random.nextFloat() * 4f + 2f,
                    speed = Random.nextFloat() * 0.5f + 0.2f,
                    alpha = Random.nextFloat() * 0.4f + 0.1f,
                    color = particleColors.random()
                )
            }
        } else emptyList()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleAnimation"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        particles.forEachIndexed { index, particle ->
            val offsetY = (animationProgress * this.size.height * particle.speed) % this.size.height
            val newY = (particle.y + offsetY) % this.size.height

            // Floating motion
            val floatOffset = sin(animationProgress * 2 * PI + index) * 20f

            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.size,
                center = Offset(
                    x = particle.x + floatOffset.toFloat(),
                    y = newY
                )
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 9. ANIMATED GRADIENT MESH BACKGROUND
// Premium animated gradient for dashboard
// ══════════════════════════════════════════════════════════════════

@Composable
fun AnimatedGradientMesh(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradientMesh")

    val color1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )

    val color2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )

    val color3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color3"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    DashboardNavy,
                    DashboardNavyLight.copy(alpha = 0.95f),
                    DashboardNavy
                )
            )
        )

        // Animated color spots
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DashboardEmerald.copy(alpha = 0.08f * (1 - color1Offset)),
                    Color.Transparent
                ),
                radius = size.minDimension * 0.6f
            ),
            center = Offset(
                x = size.width * (0.2f + color1Offset * 0.3f),
                y = size.height * (0.3f + color1Offset * 0.2f)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DashboardCyan.copy(alpha = 0.06f * color2Offset),
                    Color.Transparent
                ),
                radius = size.minDimension * 0.5f
            ),
            center = Offset(
                x = size.width * (0.8f - color2Offset * 0.3f),
                y = size.height * (0.7f - color2Offset * 0.2f)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DashboardTeal.copy(alpha = 0.05f * (1 - color3Offset)),
                    Color.Transparent
                ),
                radius = size.minDimension * 0.4f
            ),
            center = Offset(
                x = size.width * (0.5f + color3Offset * 0.2f),
                y = size.height * (0.5f)
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 10. SWIPEABLE QUICK ACTIONS
// Horizontal swipeable action cards
// ══════════════════════════════════════════════════════════════════

data class QuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String? = null,
    val isPrimary: Boolean = false
)

@Composable
fun SwipeableQuickActions(
    actions: List<QuickAction>,
    onActionClick: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DashboardSoftWhite
            )

            // Scroll indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(minOf(5, (actions.size + 2) / 3)) { index ->
                    val isActive = listState.firstVisibleItemIndex / 3 == index
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) DashboardEmerald
                                else DashboardSoftWhite.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(actions) { action ->
                QuickActionCard(
                    action = action,
                    onClick = { onActionClick(action) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit
) {
    val interactionScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .width(100.dp)
            .shadow(
                elevation = if (action.isPrimary) 12.dp else 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = action.color.copy(alpha = 0.2f)
            )
            .scale(interactionScale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = if (action.isPrimary) {
                    Brush.verticalGradient(
                        colors = listOf(
                            action.color.copy(alpha = 0.3f),
                            action.color.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            DashboardNavyLight.copy(alpha = 0.9f),
                            DashboardNavy.copy(alpha = 0.95f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        action.color.copy(alpha = if (action.isPrimary) 0.5f else 0.3f),
                        action.color.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Badge
            if (action.badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(8.dp))
                        .background(action.color)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = action.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(action.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = action.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = action.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DashboardSoftWhite,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 11. PULL TO REFRESH INDICATOR
// Custom branded refresh indicator
// ══════════════════════════════════════════════════════════════════

@Composable
fun BudgiePullToRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(200)
        },
        label = "rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else pullProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DashboardEmerald.copy(alpha = 0.2f),
                            DashboardEmerald.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = DashboardEmerald.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = DashboardEmerald,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Pull to refresh",
                    tint = DashboardEmerald,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation * pullProgress)
                )
            }
        }

        // Loading text
        if (isRefreshing) {
            Text(
                text = "Refreshing...",
                fontSize = 12.sp,
                color = DashboardEmerald,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 12. BUDGET PROGRESS RING
// Animated circular budget progress
// ══════════════════════════════════════════════════════════════════

@Composable
fun BudgetProgressRing(
    spent: Double,
    budget: Double,
    categoryName: String,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1.5f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "budgetProgress"
    )

    val progressColor = when {
        progress > 1f -> DashboardMutedRed
        progress > 0.8f -> DashboardAmber
        else -> categoryColor
    }

    val glowPulse = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowPulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect for overspent
            if (progress > 1f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DashboardMutedRed.copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2 + 10.dp.toPx()
                    )
                }
            }

            // Background ring
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                drawArc(
                    color = DashboardSoftWhite.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }

            // Progress ring
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            progressColor.copy(alpha = 0.5f),
                            progressColor,
                            progressColor.copy(alpha = 0.8f)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = minOf(animatedProgress * 360f, 360f),
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }

            // Center percentage
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = categoryName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DashboardSoftWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${formatCompactCurrency(spent)} / ${formatCompactCurrency(budget)}",
            fontSize = 10.sp,
            color = DashboardSoftWhite.copy(alpha = 0.6f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 13. WEEKLY SUMMARY CARD
// Compact weekly overview
// ══════════════════════════════════════════════════════════════════

@Composable
fun WeeklySummaryCard(
    weeklyIncome: Double,
    weeklyExpenses: Double,
    dailyAverage: Double,
    bestDay: String,
    worstDay: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
                color = DashboardSoftWhite.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = DashboardCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "This Week",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashboardSoftWhite
                    )
                }

                val weeklySavings = weeklyIncome - weeklyExpenses
                Text(
                    text = formatCompactCurrency(weeklySavings),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (weeklySavings >= 0) DashboardEmerald else DashboardMutedRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeeklyStatItem(
                    label = "Income",
                    value = formatCompactCurrency(weeklyIncome),
                    color = DashboardEmerald
                )
                WeeklyStatItem(
                    label = "Expenses",
                    value = formatCompactCurrency(weeklyExpenses),
                    color = DashboardMutedRed
                )
                WeeklyStatItem(
                    label = "Daily Avg",
                    value = formatCompactCurrency(dailyAverage),
                    color = DashboardCyan
                )
            }

            if (bestDay.isNotEmpty() || worstDay.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = DashboardSoftWhite.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (bestDay.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = DashboardEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Best: $bestDay",
                                fontSize = 11.sp,
                                color = DashboardEmerald
                            )
                        }
                    }
                    if (worstDay.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = null,
                                tint = DashboardAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Most spent: $worstDay",
                                fontSize = 11.sp,
                                color = DashboardAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = DashboardSoftWhite.copy(alpha = 0.6f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ══════════════════════════════════════════════════════════════════

private fun formatCompactCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    val prefix = if (amount < 0) "-" else ""
    return when {
        absAmount >= 1_000_000 -> "$prefix$${"%.1f".format(absAmount / 1_000_000)}M"
        absAmount >= 1_000 -> "$prefix$${"%.1f".format(absAmount / 1_000)}K"
        else -> "$prefix$${"%.0f".format(absAmount)}"
    }
}

