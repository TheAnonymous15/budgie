package com.example.budgie.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ═══════════════════════════════════════════════════════════════════
   GLASSMORPHIC MODIFIERS FOR DASHBOARD
   Premium glass effects with neon glow
═══════════════════════════════════════════════════════════════════ */

/**
 * Ultra Premium Glass Card with Neon Glow
 */
fun Modifier.ultraGlassCard(
    accentColor: Color,
    cornerRadius: Dp = 20.dp,
    glowStrength: Float = 0.25f
) = this
    .shadow(
        elevation = 24.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = accentColor.copy(alpha = glowStrength),
        spotColor = accentColor.copy(alpha = glowStrength * 0.7f)
    )
    .clip(RoundedCornerShape(cornerRadius))
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
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Standard Glassmorphic Card
 */
fun Modifier.glassmorphicCard(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp
) = this
    .shadow(
        elevation = 12.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = Color.Black.copy(alpha = 0.4f)
    )
    .clip(RoundedCornerShape(cornerRadius))
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
        width = borderWidth,
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassBorder,
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Accent Glass Card with colored glow
 */
fun Modifier.glassmorphicAccentCard(
    accentColor: Color,
    cornerRadius: Dp = 16.dp
) = this
    .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = accentColor.copy(alpha = 0.25f),
        spotColor = accentColor.copy(alpha = 0.2f)
    )
    .clip(RoundedCornerShape(cornerRadius))
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
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Holographic shimmer card
 */
fun Modifier.holographicShimmer(
    primaryColor: Color,
    secondaryColor: Color = Color.White,
    cornerRadius: Dp = 16.dp
) = this
    .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = primaryColor.copy(alpha = 0.3f)
    )
    .clip(RoundedCornerShape(cornerRadius))
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
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Neon glow button modifier
 */
fun Modifier.neonGlowButton(
    color: Color,
    cornerRadius: Dp = 16.dp
) = this
    .shadow(
        elevation = 12.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = color.copy(alpha = 0.5f),
        spotColor = color.copy(alpha = 0.4f)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.horizontalGradient(
            colors = listOf(
                color,
                color.copy(alpha = 0.85f)
            )
        )
    )

/**
 * Subtle glass button
 */
fun Modifier.subtleGlassButton(
    cornerRadius: Dp = 12.dp
) = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color.White.copy(alpha = 0.05f))
    .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(cornerRadius)
    )

