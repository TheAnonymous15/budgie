package com.example.budgie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.ui.components.formatCurrency

// Dashboard Theme Colors (shared)
internal val DashboardNavy = Color(0xFF0B1F2A)
internal val DashboardNavyLight = Color(0xFF0D2E3D)
internal val DashboardEmerald = Color(0xFF0FAE96)
internal val DashboardTeal = Color(0xFF0B8F7A)
internal val DashboardGold = Color(0xFFC9A14A)
internal val DashboardGoldLight = Color(0xFFFFD166)
internal val DashboardSoftWhite = Color(0xFFE6F1F0)
internal val DashboardMutedRed = Color(0xFFE57373)
internal val DashboardAmber = Color(0xFFFFB74D)
internal val DashboardBlue = Color(0xFF5C9CE5)
internal val DashboardCyan = Color(0xFF26A69A)
internal val DashboardPurple = Color(0xFF9575CD)

// Glassmorphism Colors
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassHighlight = Color.White.copy(alpha = 0.15f)

// Glassmorphism Card Modifier for Dashboard
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
            colors = listOf(
                GlassHighlight,
                GlassWhite,
                Color.White.copy(alpha = 0.05f)
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

// Accent Glassmorphism Card Modifier
private fun Modifier.glassmorphicAccentCard(
    accentColor: Color,
    cornerRadius: Int = 16
) = this
    .shadow(
        elevation = 6.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = accentColor.copy(alpha = 0.2f),
        spotColor = accentColor.copy(alpha = 0.2f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.15f),
                accentColor.copy(alpha = 0.08f),
                accentColor.copy(alpha = 0.05f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.3f),
                accentColor.copy(alpha = 0.1f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// Premium Metric Card Component with Glassmorphic Design
@Composable
fun PremiumMetricCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.glassmorphicAccentCard(accentColor, 18)
    ) {
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = DashboardSoftWhite.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (hasData) {
                Text(
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            } else {
                Text(
                    text = "No data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = DashboardSoftWhite.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// Premium Action Chip Component
@Composable
fun PremiumActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isPremium: Boolean = false
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium)
                DashboardEmerald.copy(alpha = 0.15f)
            else
                Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPremium) DashboardEmerald.copy(alpha = 0.4f) else DashboardSoftWhite.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isPremium) DashboardEmerald else DashboardSoftWhite.copy(alpha = 0.7f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
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

// Grid Action Button for Quick Actions with Glassmorphic Design
@Composable
fun GridActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassmorphicCard(14)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DashboardEmerald.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = DashboardEmerald
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = DashboardSoftWhite.copy(alpha = 0.9f)
            )
        }
    }
}

// Goals/Loans Summary Button Component
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
    Box(
        modifier = modifier
            .glassmorphicAccentCard(accentColor, 14)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DashboardSoftWhite
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

