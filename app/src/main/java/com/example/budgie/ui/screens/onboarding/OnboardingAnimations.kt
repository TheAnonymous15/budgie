package com.example.budgie.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium animated background with floating orbs
 */
@Composable
fun PremiumAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    // Floating orb animations
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )

    // Rotating gradient
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Teal glow orb - top right
        Box(
            modifier = Modifier
                .offset(x = 200.dp + float1.dp, y = 80.dp)
                .size(180.dp)
                .blur(80.dp)
                .alpha(0.3f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0FAE96),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Blue accent orb - center left
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = 350.dp + float2.dp)
                .size(150.dp)
                .blur(70.dp)
                .alpha(0.25f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Gold accent orb - bottom right
        Box(
            modifier = Modifier
                .offset(x = 280.dp, y = 550.dp + float1.dp)
                .size(100.dp)
                .blur(50.dp)
                .alpha(0.2f)
                .rotate(rotation)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFC9A14A),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Subtle emerald glow - bottom center
        Box(
            modifier = Modifier
                .offset(x = 100.dp, y = 700.dp + float2.dp)
                .size(200.dp)
                .blur(90.dp)
                .alpha(0.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Smaller animated background for compact screens
 */
@Composable
fun CompactAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim_compact")

    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Single subtle teal glow
        Box(
            modifier = Modifier
                .offset(x = 150.dp + float1.dp, y = 100.dp)
                .size(120.dp)
                .blur(60.dp)
                .alpha(0.2f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0FAE96),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

