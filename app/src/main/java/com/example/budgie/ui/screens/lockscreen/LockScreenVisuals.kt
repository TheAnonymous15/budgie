package com.example.budgie.ui.screens.lockscreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Premium animated background with holographic grid and wealth glow effects
 */
@Composable
fun PremiumBackground(
    particleDrift: Float,
    glowIntensity: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Modern hexagonal grid pattern (money/tech feel)
        val gridSpacing = 80f
        val gridAlpha = 0.025f

        for (i in 0..(size.width / gridSpacing).toInt() + 1) {
            val x = i * gridSpacing + (particleDrift % gridSpacing)
            drawLine(
                color = Emerald.copy(alpha = gridAlpha),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }

        for (i in 0..(size.height / gridSpacing).toInt() + 1) {
            val y = i * gridSpacing
            drawLine(
                color = SoftWhite.copy(alpha = gridAlpha * 0.6f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.8f
            )
        }

        // Top-left emerald glow (growth & prosperity)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Emerald.copy(alpha = glowIntensity * 0.18f),
                    Emerald.copy(alpha = glowIntensity * 0.08f),
                    Color.Transparent
                ),
                center = Offset(0f, 0f),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(0f, 0f)
        )

        // Bottom-right gold glow (luxury & wealth)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Gold.copy(alpha = glowIntensity * 0.12f),
                    GoldMuted.copy(alpha = glowIntensity * 0.06f),
                    Color.Transparent
                ),
                center = Offset(size.width, size.height),
                radius = size.width * 0.6f
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width, size.height)
        )

        // Center teal accent (professional authority)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Teal.copy(alpha = glowIntensity * 0.08f),
                    Color.Transparent
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width * 0.4f
            ),
            radius = size.width * 0.4f,
            center = Offset(size.width / 2, size.height / 2)
        )

        // Subtle diagonal metallic shimmer lines
        val shimmerOffset = particleDrift * 2f
        for (i in 0..5) {
            val startY = (i * size.height / 5) - shimmerOffset
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        SoftWhite.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    start = Offset(0f, startY),
                    end = Offset(size.width, startY + size.height * 0.2f)
                ),
                start = Offset(0f, startY),
                end = Offset(size.width, startY + size.height * 0.2f),
                strokeWidth = 60f
            )
        }
    }
}

/**
 * Modern 3D security shield with premium materials and holographic rings
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
        // Deep 3D shadow layer
        Box(
            modifier = Modifier
                .size(dimens.shieldContainerSize * 0.95f)
                .offset(y = 6.dp)
                .blur(15.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Outer wealth glow (emerald + gold)
        Box(
            modifier = Modifier
                .size(dimens.shieldContainerSize)
                .blur(45.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (showSuccessAnimation) Gold else Emerald).copy(alpha = glowIntensity * 1.2f),
                            Emerald.copy(alpha = glowIntensity * 0.6f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Rotating holographic outer ring (premium metallic)
        Canvas(
            modifier = Modifier
                .size(dimens.shieldOuterRingSize)
                .graphicsLayer { rotationZ = ringRotation }
        ) {
            val strokeWidth = 3.dp.toPx()
            // Main gradient arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Emerald.copy(alpha = 0.9f),
                        Gold.copy(alpha = 0.7f),
                        Emerald.copy(alpha = 0.3f),
                        Color.Transparent,
                        Color.Transparent,
                        Emerald.copy(alpha = 0.3f),
                        Gold.copy(alpha = 0.7f),
                        Emerald.copy(alpha = 0.9f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Middle decorative ring (wealth accent)
        Canvas(
            modifier = Modifier
                .size(dimens.shieldOuterRingSize * 0.85f)
                .graphicsLayer { rotationZ = -ringRotation * 0.7f }
        ) {
            val strokeWidth = 1.5.dp.toPx()
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        GoldMuted.copy(alpha = 0.4f),
                        Color.Transparent,
                        GoldMuted.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2,
                style = Stroke(width = strokeWidth)
            )
        }

        // Counter-rotating inner ring (teal professional)
        Canvas(
            modifier = Modifier
                .size(dimens.shieldInnerRingSize)
                .graphicsLayer { rotationZ = counterRingRotation }
        ) {
            val strokeWidth = 2.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Teal.copy(alpha = 0.8f),
                        EmeraldBright.copy(alpha = 0.6f),
                        Color.Transparent,
                        Teal.copy(alpha = 0.4f),
                        Color.Transparent,
                        Teal.copy(alpha = 0.8f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 220f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // 3D shield container with depth
        Box(
            modifier = Modifier.size(dimens.shieldIconContainerSize)
        ) {
            // Shadow layer
            Box(
                modifier = Modifier
                    .size(dimens.shieldIconContainerSize * 0.95f)
                    .offset(y = 2.dp)
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .blur(5.dp)
            )

            // Main container with premium gradient border
            Box(
                modifier = Modifier
                    .size(dimens.shieldIconContainerSize)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Emerald,
                                EmeraldBright,
                                Gold,
                                Emerald
                            )
                        ),
                        shape = CircleShape
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MidnightNavy.copy(alpha = 0.95f),
                                DeepNavy.copy(alpha = 0.98f),
                                Emerald.copy(alpha = 0.12f)
                            )
                        ),
                        CircleShape
                    )
                    // Inner highlight for 3D effect
                    .border(
                        width = 1.dp,
                        color = SoftWhite.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Icon with 3D effect
                Box {
                    // Shadow layer for icon
                    Icon(
                        imageVector = when {
                            showSuccessAnimation -> Icons.Default.LockOpen
                            authenticating -> Icons.Default.Fingerprint
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(dimens.shieldIconSize)
                            .offset(x = 1.5.dp, y = 1.5.dp)
                    )

                    // Main icon with wealth gradient
                    Icon(
                        imageVector = when {
                            showSuccessAnimation -> Icons.Default.LockOpen
                            authenticating -> Icons.Default.Fingerprint
                            else -> Icons.Default.Lock
                        },
                        contentDescription = "Security Shield",
                        tint = if (showSuccessAnimation) Gold else SoftWhite,
                        modifier = Modifier.size(dimens.shieldIconSize)
                    )

                    // Accent glow overlay
                    Icon(
                        imageVector = when {
                            showSuccessAnimation -> Icons.Default.LockOpen
                            authenticating -> Icons.Default.Fingerprint
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = if (showSuccessAnimation) Gold.copy(alpha = 0.6f) else Emerald.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(dimens.shieldIconSize)
                            .blur(2.dp)
                    )
                }
            }
        }
    }
}

/**
 * AI Guardian status indicator - Modern premium design
 */
@Composable
fun AIGuardianStatus(
    dimens: LockScreenDimens,
    aiPresence: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer { alpha = 0.85f }
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MidnightNavy.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Pulsing AI indicator dot with glow
        Box(contentAlignment = Alignment.Center) {
            // Glow layer
            Box(
                modifier = Modifier
                    .size(dimens.aiDotSize * 2)
                    .graphicsLayer { alpha = aiPresence * 0.5f }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Emerald.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Main dot
            Box(
                modifier = Modifier
                    .size(dimens.aiDotSize)
                    .graphicsLayer { alpha = aiPresence }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EmeraldBright,
                                Emerald
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = SoftWhite.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        Spacer(Modifier.width(10.dp))

        androidx.compose.material3.Text(
            "AI GUARDIAN ACTIVE",
            fontSize = androidx.compose.ui.unit.TextUnit(dimens.aiTextSize.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
            letterSpacing = androidx.compose.ui.unit.TextUnit(dimens.aiLetterSpacing, androidx.compose.ui.unit.TextUnitType.Sp),
            color = Emerald.copy(alpha = 0.7f),
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
    }
}

