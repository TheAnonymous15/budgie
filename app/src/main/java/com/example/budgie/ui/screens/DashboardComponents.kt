package com.example.budgie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.ui.components.formatCurrency
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ==================== FUTURISTIC DASHBOARD THEME ====================
// Core Navy Spectrum
internal val DashboardNavy = Color(0xFF0A1628)
internal val DashboardNavyLight = Color(0xFF101D35)
internal val DashboardNavyDark = Color(0xFF060E1A)

// Neon Accent Palette
internal val DashboardEmerald = Color(0xFF00F5A0)
internal val DashboardTeal = Color(0xFF0B8F7A)
internal val DashboardGold = Color(0xFFFFD93D)
internal val DashboardGoldLight = Color(0xFFFFE066)
internal val DashboardSoftWhite = Color(0xFFE8F1F2)
internal val DashboardMutedRed = Color(0xFFFF6B6B)
internal val DashboardAmber = Color(0xFFFFAA33)
internal val DashboardBlue = Color(0xFF00D4FF)
internal val DashboardCyan = Color(0xFF00BCD4)
internal val DashboardPurple = Color(0xFFB24BF3)
internal val DashboardPink = Color(0xFFFF006E)

// Glass & Glow Effects
private val GlassCore = Color(0xFF1A2744)
private val GlassHighlight = Color.White.copy(alpha = 0.12f)
private val GlassBorder = Color.White.copy(alpha = 0.15f)

// ==================== ADVANCED GLASSMORPHISM MODIFIERS ====================

// Ultra Premium Glass Card with Neon Glow
private fun Modifier.ultraGlassCard(
    accentColor: Color,
    cornerRadius: Int = 20,
    glowStrength: Float = 0.25f
) = this
    .shadow(
        elevation = 24.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = accentColor.copy(alpha = glowStrength),
        spotColor = accentColor.copy(alpha = glowStrength * 0.7f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassCore.copy(alpha = 0.9f),
                DashboardNavyLight.copy(alpha = 0.95f),
                DashboardNavy.copy(alpha = 0.98f)
            )
        )
    )
    .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.6f),
                accentColor.copy(alpha = 0.2f),
                Color.White.copy(alpha = 0.1f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// Holographic Shimmer Effect
private fun Modifier.holographicShimmer(
    primaryColor: Color,
    secondaryColor: Color = Color.White,
    cornerRadius: Int = 16
) = this
    .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = primaryColor.copy(alpha = 0.3f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.sweepGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.15f),
                secondaryColor.copy(alpha = 0.08f),
                primaryColor.copy(alpha = 0.12f),
                secondaryColor.copy(alpha = 0.06f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.5f),
                secondaryColor.copy(alpha = 0.2f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// Standard Glass Card
private fun Modifier.glassmorphicCardDashboard(
    cornerRadius: Int = 16,
    borderWidth: Float = 1f
) = this
    .shadow(
        elevation = 12.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = Color.Black.copy(alpha = 0.4f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassHighlight,
                GlassCore.copy(alpha = 0.85f),
                DashboardNavy.copy(alpha = 0.9f)
            )
        )
    )
    .border(
        width = borderWidth.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassBorder,
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// Accent Glass Card
private fun Modifier.glassmorphicAccentCardDashboard(
    accentColor: Color,
    cornerRadius: Int = 16
) = this
    .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = accentColor.copy(alpha = 0.25f),
        spotColor = accentColor.copy(alpha = 0.2f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.18f),
                accentColor.copy(alpha = 0.08f),
                DashboardNavy.copy(alpha = 0.95f)
            )
        )
    )
    .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.5f),
                accentColor.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.08f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// ==================== ANIMATED COMPONENTS ====================

// Pulsing Glow Effect
@Composable
private fun PulsingGlow(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .drawBehind {
                drawCircle(
                    color = color.copy(alpha = alpha * 0.5f),
                    radius = size.minDimension * 0.7f
                )
                drawCircle(
                    color = color.copy(alpha = alpha * 0.3f),
                    radius = size.minDimension * 0.9f
                )
            }
    )
}

// Orbiting Particles Background
@Composable
fun OrbitingParticles(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension * 0.35f

        // Draw orbital path (subtle dotted line)
        drawCircle(
            color = primaryColor.copy(alpha = 0.1f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        )

        // Draw orbiting particles
        repeat(3) { i ->
            val angle = Math.toRadians((rotation + i * 120).toDouble())
            val particleX = centerX + (radius * cos(angle)).toFloat()
            val particleY = centerY + (radius * sin(angle)).toFloat()

            drawCircle(
                color = if (i % 2 == 0) primaryColor else secondaryColor,
                radius = 4.dp.toPx(),
                center = Offset(particleX, particleY)
            )
        }
    }
}

// ==================== PREMIUM METRIC CARD ====================
@Composable
fun PremiumMetricCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "metric")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .ultraGlassCard(accentColor, 20, if (hasData) glowAlpha else 0.1f)
    ) {
        // Background shimmer effect
        if (hasData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(100f, 50f),
                            radius = 200f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title with subtle glow
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = DashboardSoftWhite.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                // Animated Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.3f),
                                    accentColor.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (hasData) {
                // Amount with premium styling
                Text(
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = accentColor
                )

                // Subtle indicator line
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            } else {
                // Empty state with style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DashboardSoftWhite.copy(alpha = 0.3f))
                    )
                    Text(
                        text = "No data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DashboardSoftWhite.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ==================== ACTION BUTTONS ====================

// Premium Action Chip with Hover Effect
@Composable
fun PremiumActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isPremium: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium)
                DashboardEmerald.copy(alpha = 0.15f)
            else
                GlassCore.copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isPremium)
                DashboardEmerald.copy(alpha = 0.5f)
            else
                DashboardSoftWhite.copy(alpha = 0.15f)
        ),
        modifier = Modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isPremium)
                            DashboardEmerald.copy(alpha = 0.2f)
                        else
                            DashboardSoftWhite.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isPremium) DashboardEmerald else DashboardSoftWhite.copy(alpha = 0.8f)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPremium) DashboardEmerald else DashboardSoftWhite.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    PremiumActionChip(icon = icon, label = label, onClick = onClick)
}

// Futuristic Grid Action Button
@Composable
fun GridActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "gridScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .glassmorphicCardDashboard(16)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    onClick()
                }
            )
    ) {
        // Subtle gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DashboardEmerald.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Premium icon container with glow
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = DashboardEmerald.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                DashboardEmerald.copy(alpha = 0.25f),
                                DashboardEmerald.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = DashboardEmerald.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = DashboardEmerald
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = DashboardSoftWhite.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

// Summary Action Button with Badge
@Composable
fun SummaryActionButton(
    icon: ImageVector,
    label: String,
    subLabel: String?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAnimated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "summary")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAnimated) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
            .glassmorphicAccentCardDashboard(accentColor, 16)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing icon container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = accentColor.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.35f),
                                accentColor.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DashboardSoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subLabel != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ==================== INSIGHT CARD ====================
@Composable
fun InsightCard(
    insight: com.example.budgie.data.model.SpendingInsight,
    modifier: Modifier = Modifier
) {
    val insightColor = when (insight.type) {
        com.example.budgie.data.model.InsightType.OVERSPENDING,
        com.example.budgie.data.model.InsightType.BUDGET_WARNING -> DashboardMutedRed
        com.example.budgie.data.model.InsightType.GOOD_HABIT -> DashboardEmerald
        com.example.budgie.data.model.InsightType.SAVING_OPPORTUNITY -> DashboardGold
        else -> DashboardAmber
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphicAccentCardDashboard(insightColor, 18)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Animated insight icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                insightColor.copy(alpha = 0.3f),
                                insightColor.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, insightColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = insightColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(insightColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = insight.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = insightColor,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DashboardSoftWhite.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ==================== BILL ITEM ====================
@Composable
fun BillItem(
    bill: com.example.budgie.data.model.Bill,
    onPaidToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaid = bill.isPaid
    val statusColor = if (isPaid) DashboardEmerald else DashboardMutedRed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphicAccentCardDashboard(statusColor, 16)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.3f),
                                    statusColor.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaid)
                            Icons.Default.Check
                        else
                            Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DashboardSoftWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPaid) "Paid" else "Due: ${formatDueDate(bill.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DashboardSoftWhite.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(bill.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )

                // Toggle button
                Switch(
                    checked = isPaid,
                    onCheckedChange = onPaidToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DashboardEmerald,
                        checkedTrackColor = DashboardEmerald.copy(alpha = 0.3f),
                        uncheckedThumbColor = DashboardMutedRed,
                        uncheckedTrackColor = DashboardMutedRed.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}

// Helper function for date formatting
private fun formatDueDate(date: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(date))
}
