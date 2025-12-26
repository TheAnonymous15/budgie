package com.example.budgie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.R
import kotlinx.coroutines.delay

// Premium Wealth Colors - Refined
private val WealthNavy = Color(0xFF0B1F2A)
private val WealthNavyDark = Color(0xFF051015) // Darker, more premium
private val WealthNavyMid = Color(0xFF0A1A24)
private val WealthEmerald = Color(0xFF0FAE96)
private val WealthTeal = Color(0xFF0B8F7A)
private val WealthGold = Color(0xFFC9A14A)
private val WealthSoftWhite = Color(0xFFE6F1F0)

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var showTagline by remember { mutableStateOf(false) }
    var showTrustBadge by remember { mutableStateOf(false) }
    var showLoader by remember { mutableStateOf(false) }
    var currentLoadingStep by remember { mutableStateOf(0) }
    var showBottomContent by remember { mutableStateOf(false) }

    // Loading steps for intelligent feedback
    val loadingSteps = listOf(
        "Securing your data",
        "Analyzing patterns",
        "Building insights"
    )

    // Trigger animations - total 5 seconds for premium feel
    LaunchedEffect(Unit) {
        delay(400)
        showTagline = true
        delay(300)
        showTrustBadge = true
        delay(300)
        showLoader = true
        showBottomContent = true

        // Cycle through loading steps
        for (i in loadingSteps.indices) {
            currentLoadingStep = i
            delay(1000)
        }
        delay(500)
        onSplashComplete()
    }

    // Animation values
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Logo scale - starts visible
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    // Subtle glow pulse (slower, 3.5s loop)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f, // Reduced opacity for premium feel
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Inner ring pulse (slow, 4s)
    val innerRingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_ring"
    )

    // Outer ring rotation (clockwise) - faster
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Middle ring rotation - counter-clockwise for tech feel (faster)
    val middleRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "middle_ring_rotation"
    )

    // Inner ring rotation - clockwise (fastest)
    val innerRingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring_rotation"
    )

    // Particle float
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle"
    )

    // Text animations
    val taglineAlpha by animateFloatAsState(
        targetValue = if (showTagline) 1f else 0f,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "tagline_alpha"
    )

    val trustBadgeAlpha by animateFloatAsState(
        targetValue = if (showTrustBadge) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "trust_alpha"
    )

    val bottomAlpha by animateFloatAsState(
        targetValue = if (showBottomContent) 0.6f else 0f, // Reduced opacity for premium
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "bottom_alpha"
    )

    // Loading step animation
    val loadingStepAlpha by animateFloatAsState(
        targetValue = if (showLoader) 1f else 0f,
        animationSpec = tween(400),
        label = "loading_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WealthNavyDark,
                        WealthNavyMid,
                        WealthNavy,
                        WealthNavyMid
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background elements
        SplashBackgroundElements(
            particleOffset = particleOffset,
            ringRotation = ringRotation
        )

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // Premium logo container with depth
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(logoScale)
            ) {
                // Radial blur background for depth
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .blur(40.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    WealthEmerald.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Outer glow ring (clockwise) - reduced opacity
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .rotate(ringRotation)
                        .alpha(0.6f)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    WealthEmerald.copy(alpha = 0.8f),
                                    WealthTeal.copy(alpha = 0.1f),
                                    Color.Transparent,
                                    WealthTeal.copy(alpha = 0.1f),
                                    WealthEmerald.copy(alpha = 0.8f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Middle ring (counter-clockwise) - tech style
                Box(
                    modifier = Modifier
                        .size(155.dp)
                        .rotate(middleRingRotation)
                        .alpha(0.5f)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    WealthGold.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    WealthGold.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    WealthGold.copy(alpha = 0.7f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Inner pulsing ring (clockwise - faster)
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(innerRingRotation)
                        .scale(innerRingScale)
                        .alpha(0.5f)
                        .border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    WealthEmerald.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    WealthEmerald.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    WealthEmerald.copy(alpha = 0.6f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Subtle glow effect
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    WealthEmerald.copy(alpha = glowAlpha),
                                    WealthEmerald.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Logo
                Image(
                    painter = painterResource(R.drawable.icon),
                    contentDescription = "Budgie Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    WealthEmerald.copy(alpha = 0.8f),
                                    WealthTeal.copy(alpha = 0.6f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // App name - refined typography
            Text(
                text = "BUDGIE",
                style = MaterialTheme.typography.displaySmall.copy(
                    letterSpacing = 10.sp // Increased letter spacing
                ),
                fontWeight = FontWeight.SemiBold, // Semi-bold, not bold
                color = WealthSoftWhite
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Single tagline - tighter, smaller
            Text(
                text = "Your financial future, simplified",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = WealthSoftWhite.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Trust cues - single line
            Text(
                text = "AI-driven · Secure · Private",
                style = MaterialTheme.typography.bodySmall,
                color = WealthEmerald.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(trustBadgeAlpha)
            )

            Spacer(modifier = Modifier.height(50.dp))

            // Intelligent loading feedback
            if (showLoader) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(loadingStepAlpha)
                ) {
                    // Progress dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        loadingSteps.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentLoadingStep) 8.dp else 6.dp)
                                    .alpha(if (index <= currentLoadingStep) 1f else 0.3f)
                                    .background(
                                        if (index <= currentLoadingStep) WealthEmerald
                                        else WealthSoftWhite.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current loading step text
                    Text(
                        text = loadingSteps.getOrElse(currentLoadingStep) { loadingSteps.last() },
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Bottom content - refined
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .alpha(bottomAlpha)
        ) {
            // Single line features - cleaner
            Text(
                text = "Track · Analyze · Grow",
                style = MaterialTheme.typography.labelMedium,
                color = WealthSoftWhite,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Security badge with icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = WealthEmerald.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Bank-Grade Security · Encrypted Locally",
                    style = MaterialTheme.typography.labelSmall,
                    color = WealthSoftWhite.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun SplashBackgroundElements(
    particleOffset: Float,
    ringRotation: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Subtle floating orbs (very low opacity for depth)
        drawCircle(
            color = WealthEmerald.copy(alpha = 0.04f),
            radius = 80f,
            center = Offset(centerX + 140, centerY - 280 + particleOffset)
        )

        drawCircle(
            color = WealthTeal.copy(alpha = 0.03f),
            radius = 50f,
            center = Offset(centerX - 160, centerY + 220 - particleOffset)
        )

        drawCircle(
            color = WealthGold.copy(alpha = 0.02f),
            radius = 35f,
            center = Offset(centerX + 170, centerY + 200 + particleOffset * 0.5f)
        )

        // Very subtle grid lines for futuristic depth
        for (i in 0..12) {
            val yPos = (size.height / 12) * i
            drawLine(
                color = WealthSoftWhite.copy(alpha = 0.015f),
                start = Offset(0f, yPos),
                end = Offset(size.width, yPos),
                strokeWidth = 0.5f
            )
        }

        // Subtle diagonal accent
        drawLine(
            color = WealthEmerald.copy(alpha = 0.03f),
            start = Offset(0f, size.height * 0.25f),
            end = Offset(size.width * 0.25f, 0f),
            strokeWidth = 0.5f
        )

        drawLine(
            color = WealthTeal.copy(alpha = 0.02f),
            start = Offset(size.width, size.height * 0.75f),
            end = Offset(size.width * 0.75f, size.height),
            strokeWidth = 0.5f
        )
    }
}

// Custom easings
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

