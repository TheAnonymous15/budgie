package com.example.budgie.ui.screens.lockscreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/* ═══════════════════════════════════════════════════════════════════
   LOCK SCREEN VISUAL COMPONENTS
   Premium animated backgrounds and shield
═══════════════════════════════════════════════════════════════════ */

/**
 * Premium animated background with grid and glow effects
 */
@Composable
fun PremiumBackground(
    particleDrift: Float,
    glowIntensity: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle grid pattern
        val gridSpacing = 60f
        val gridAlpha = 0.02f

        for (i in 0..(size.width / gridSpacing).toInt() + 1) {
            val x = i * gridSpacing + (particleDrift % gridSpacing)
            drawLine(
                color = SoftWhite.copy(alpha = gridAlpha),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.5f
            )
        }

        for (i in 0..(size.height / gridSpacing).toInt() + 1) {
            val y = i * gridSpacing
            drawLine(
                color = SoftWhite.copy(alpha = gridAlpha * 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5f
            )
        }

        // Corner accent glows
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Emerald.copy(alpha = glowIntensity * 0.15f),
                    Color.Transparent
                ),
                center = Offset(0f, 0f),
                radius = size.width * 0.4f
            ),
            radius = size.width * 0.4f,
            center = Offset(0f, 0f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Teal.copy(alpha = glowIntensity * 0.1f),
                    Color.Transparent
                ),
                center = Offset(size.width, size.height),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width, size.height)
        )
    }
}

/**
 * Animated security shield with rotating rings
 */
@Composable
fun AnimatedSecurityShield(
    dimens: LockScreenDimens,
    shieldPulse: Float,
    successScale: Float,
    glowIntensity: Float,
    ringRotation: Float,
    counterRingRotation: Float,
    showSuccessAnimation: Boolean,
    authenticating: Boolean
) {
    Box(
        modifier = Modifier
            .scale(shieldPulse * successScale)
            .size(dimens.shieldContainerSize),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(dimens.shieldContainerSize)
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Emerald.copy(alpha = glowIntensity),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Rotating outer ring
        Canvas(
            modifier = Modifier
                .size(dimens.shieldOuterRingSize)
                .graphicsLayer { rotationZ = ringRotation }
        ) {
            val strokeWidth = 2.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Emerald.copy(alpha = 0.8f),
                        Emerald.copy(alpha = 0.1f),
                        Color.Transparent,
                        Emerald.copy(alpha = 0.1f),
                        Emerald.copy(alpha = 0.8f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Counter-rotating inner ring
        Canvas(
            modifier = Modifier
                .size(dimens.shieldInnerRingSize)
                .graphicsLayer { rotationZ = counterRingRotation }
        ) {
            val strokeWidth = 1.5.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Teal.copy(alpha = 0.6f),
                        Color.Transparent,
                        Teal.copy(alpha = 0.3f),
                        Color.Transparent,
                        Teal.copy(alpha = 0.6f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 200f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner shield container
        Box(
            modifier = Modifier
                .size(dimens.shieldIconContainerSize)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Emerald, EmeraldBright, Teal)
                    ),
                    shape = CircleShape
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Emerald.copy(alpha = 0.15f),
                            Emerald.copy(alpha = 0.05f)
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    showSuccessAnimation -> Icons.Default.LockOpen
                    authenticating -> Icons.Default.Sync
                    else -> Icons.Default.Shield
                },
                contentDescription = "Security Shield",
                tint = if (showSuccessAnimation) Gold else Emerald,
                modifier = Modifier.size(dimens.shieldIconSize)
            )
        }
    }
}

/**
 * AI Guardian status indicator
 */
@Composable
fun AIGuardianStatus(
    dimens: LockScreenDimens,
    aiPresence: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer { alpha = 0.7f }
    ) {
        Box(
            modifier = Modifier
                .size(dimens.aiDotSize)
                .graphicsLayer { alpha = aiPresence }
                .background(Emerald, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.Text(
            "AI GUARDIAN ACTIVE",
            fontSize = androidx.compose.ui.unit.TextUnit(dimens.aiTextSize.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
            letterSpacing = androidx.compose.ui.unit.TextUnit(dimens.aiLetterSpacing, androidx.compose.ui.unit.TextUnitType.Sp),
            color = SoftWhite.copy(alpha = 0.5f),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

