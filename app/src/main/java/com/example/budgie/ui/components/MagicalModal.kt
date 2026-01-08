package com.example.budgie.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/* ═══════════════════════════════════════════════════════════════════
   MAGICAL MODAL - SUPERIOR GLASSMORPHIC POPUP DESIGN

   Features:
   - Stunning glassmorphic design with multi-layer blur
   - Smooth slide-in animations from bottom
   - Particle effects and floating orbs
   - Drag-to-dismiss gesture
   - Auto-adaptive sizing (50-95% screen height)
   - Neon glow borders with pulsing animation
   - Holographic shimmer overlay
   - Spring physics for natural motion
═══════════════════════════════════════════════════════════════════ */

@Composable
fun MagicalModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    accentColor: Color = Color(0xFF00F5A0),
    maxHeightFraction: Float = 0.90f,
    enableDragToDismiss: Boolean = true,
    showParticles: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!isVisible) return

    // Animation states
    var isEntering by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Drag state for swipe-to-dismiss
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Backdrop blur animation
    val backdropAlpha by animateFloatAsState(
        targetValue = if (isEntering) 0.95f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "backdrop"
    )

    // Modal slide animation
    val slideOffset by animateFloatAsState(
        targetValue = if (isEntering) 0f else screenHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slide"
    )

    // call onDismiss after exit animation completes
    LaunchedEffect(isEntering) {
        if (!isEntering) {
            // give time for exit animation to play
            delay(350)
            onDismiss()
        }
    }

    // Scale animation for entrance
    val scale by animateFloatAsState(
        targetValue = if (isEntering) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Glow pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "modal_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Shimmer animation
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Particle rotation
    val particleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            isEntering = true
        }
    }

    Dialog(
        onDismissRequest = {
            isEntering = false
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha * 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isEntering = false }
                )
        ) {
            // Floating particles background (conditional)
            if (showParticles) {
                FloatingParticles(
                    particleRotation = particleRotation,
                    glowPulse = glowPulse,
                    accentColor = accentColor
                )
            }

            // Main modal container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(maxHeightFraction)
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(
                            0,
                            (slideOffset + offsetY).roundToInt()
                        )
                    }
                    .scale(scale)
                    .then(
                        if (enableDragToDismiss) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = {
                                        isDragging = false
                                        if (offsetY > 200) {
                                            isEntering = false
                                        } else {
                                            offsetY = 0f
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f)
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Prevent dismiss when clicking modal */ }
                    )
            ) {
                // Multi-layer glassmorphic background with 3D depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .graphicsLayer {
                            // 3D perspective transformation
                            shadowElevation = 30f
                            translationY = -4f
                        }
                ) {
                    // Layer 0: Deep shadow for 3D depth
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = 4.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .blur(12.dp)
                    )

                    // Layer 1: Premium dark glass with wealth gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0D1526).copy(alpha = 0.98f), // Deep navy
                                        Color(0xFF1A2744).copy(alpha = 0.95f), // Rich blue
                                        Color(0xFF0F1B2E).copy(alpha = 0.97f)  // Dark wealth
                                    )
                                )
                            )
                            .blur(1.dp)
                    )

                    // Layer 2: Dual radial gradient (wealth spotlight effect)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.2f * glowPulse),
                                        accentColor.copy(alpha = 0.12f * glowPulse),
                                        Color.Transparent
                                    ),
                                    center = Offset(300f, 150f),
                                    radius = 600f
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.08f * glowPulse), // Gold accent
                                        Color.Transparent
                                    ),
                                    center = Offset(700f, 500f),
                                    radius = 500f
                                )
                            )
                    )

                    // Layer 3: Premium metallic shimmer (money feel)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFFFD700).copy(alpha = 0.08f), // Gold shimmer
                                        Color.White.copy(alpha = 0.12f),        // Platinum shimmer
                                        Color(0xFF00F5A0).copy(alpha = 0.08f), // Emerald shimmer
                                        Color.Transparent
                                    ),
                                    start = Offset(shimmerOffset - 200f, -300f),
                                    end = Offset(shimmerOffset + 200f, 600f)
                                )
                            )
                    )

                    // Layer 4: Holographic light refraction
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00D9FF).copy(alpha = 0.06f),
                                        Color.Transparent,
                                        Color(0xFFFF6B9D).copy(alpha = 0.06f),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, shimmerOffset * 0.5f),
                                    end = Offset(1000f, shimmerOffset * 0.5f + 400f)
                                )
                            )
                    )

                    // Premium multi-layer neon border with 3D effect
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.9f * glowPulse),
                                        Color(0xFFFFD700).copy(alpha = 0.6f * glowPulse), // Gold
                                        accentColor.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        accentColor.copy(alpha = 0.4f),
                                        Color(0xFFFFD700).copy(alpha = 0.6f * glowPulse), // Gold
                                        accentColor.copy(alpha = 0.9f * glowPulse)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            )
                    )

                    // Outer glow (wealth aura)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.3f * glowPulse),
                                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                            )
                    )

                    // Content area
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Premium drag indicator with 3D effect
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        ) {
                            // Shadow layer
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(5.dp)
                                    .offset(y = 2.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .blur(3.dp)
                            )
                            // Main indicator with metallic gradient
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                accentColor.copy(alpha = 0.6f),
                                                Color(0xFFFFD700).copy(alpha = 0.4f * glowPulse), // Gold
                                                accentColor.copy(alpha = 0.6f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Premium header with 3D text effect
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // 3D layered title effect
                                Box {
                                    // Shadow layer 1 (far)
                                    Text(
                                        text = title.uppercase(),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp
                                        ),
                                        color = Color.Black.copy(alpha = 0.4f),
                                        modifier = Modifier.offset(x = 3.dp, y = 3.dp)
                                    )
                                    // Shadow layer 2 (near)
                                    Text(
                                        text = title.uppercase(),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp
                                        ),
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.offset(x = 1.5.dp, y = 1.5.dp)
                                    )
                                    // Main title with premium gradient
                                    Text(
                                        text = title.uppercase(),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    accentColor,
                                                    Color(0xFFFFD700).copy(alpha = 0.9f), // Gold
                                                    accentColor,
                                                    Color.White.copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                    )
                                }

                                // Premium multi-layer accent underline
                                Spacer(modifier = Modifier.height(12.dp))
                                Box {
                                    // Glow layer
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(4.dp)
                                            .offset(y = 1.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(accentColor.copy(alpha = 0.3f * glowPulse))
                                            .blur(4.dp)
                                    )
                                    // Main underline with wealth gradient
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        accentColor.copy(alpha = 0.95f),
                                                        Color(0xFFFFD700).copy(alpha = 0.8f), // Gold
                                                        accentColor.copy(alpha = 0.6f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 0.5.dp,
                                                color = Color.White.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }

                            // Magical close button
                            MagicalCloseButton(
                                accentColor = accentColor,
                                glowPulse = glowPulse,
                                onClick = { isEntering = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Content
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun MagicalCloseButton(
    accentColor: Color,
    glowPulse: Float,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "close_scale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
    ) {
        // 3D shadow layers
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 3.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .blur(5.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 1.5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .blur(3.dp)
        )

        // Main button with premium gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A2744).copy(alpha = 0.95f),
                            Color(0xFF0D1526).copy(alpha = 0.98f),
                            accentColor.copy(alpha = 0.2f)
                        )
                    )
                )
                // Outer glow
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.7f * glowPulse),
                            Color(0xFFFFD700).copy(alpha = 0.5f * glowPulse), // Gold
                            accentColor.copy(alpha = 0.7f * glowPulse)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                // Inner highlight
                .padding(2.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isPressed = true
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            // Radial glow background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.5f * glowPulse),
                                Color(0xFFFFD700).copy(alpha = 0.3f * glowPulse), // Gold
                                Color.Transparent
                            )
                        )
                    )
            )

            // Premium 3D icon with metallic effect
            Box {
                // Shadow layer
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 1.dp, y = 1.dp)
                )
                // Main icon with gradient (simulated via tint + glow)
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(24.dp)
                )
                // Accent overlay
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingParticles(
    particleRotation: Float,
    glowPulse: Float,
    accentColor: Color
) {
    // Lightweight decorative orbs - simple and safe
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(40.dp, 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .offset(140.dp, 30.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .offset(240.dp, 120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════
   MODAL CONTENT HELPERS
═══════════════════════════════════════════════════════════════════ */

@Composable
fun MagicalModalScrollableContent(
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
fun MagicalModalSection(
    title: String,
    accentColor: Color = Color(0xFF00F5A0),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = accentColor.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ENHANCED MODAL HELPERS
// ═══════════════════════════════════════════════════════════════════

@Composable
fun MagicalModalActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00F5A0),
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "button_scale"
    )

    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(alpha = 0.2f),
            contentColor = accentColor,
            disabledContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
fun MagicalModalTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00F5A0)
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = accentColor.copy(alpha = 0.8f)
        )
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun MagicalModalDivider(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00F5A0)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun MagicalModalInfoCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00F5A0)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        accentColor.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FixedSizeDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    // Calculate 80% of screen but ensure min sizes for small screens
    val maxWidth = screenWidth * 0.8f
    val maxHeight = screenHeight * 0.8f

    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxHeight)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}
