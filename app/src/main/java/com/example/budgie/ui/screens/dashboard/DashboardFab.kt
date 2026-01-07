package com.example.budgie.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.budgie.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* ═══════════════════════════════════════════════════════════════════════════════
   ██████╗ ██████╗ ███████╗███╗   ███╗██╗██╗   ██╗███╗   ███╗    ███████╗ █████╗ ██████╗
   ██╔══██╗██╔══██╗██╔════╝████╗ ████║██║██║   ██║████╗ ████║    ██╔════╝██╔══██╗██╔══██╗
   ██████╔╝██████╔╝█████╗  ██╔████╔██║██║██║   ██║██╔████╔██║    █████╗  ███████║██████╔╝
   ██╔═══╝ ██╔══██╗██╔══╝  ██║╚██╔╝██║██║██║   ██║██║╚██╔╝██║    ██╔══╝  ██╔══██║██╔══██╗
   ██║     ██║  ██║███████╗██║ ╚═╝ ██║██║╚██████╔╝██║ ╚═╝ ██║    ██║     ██║  ██║██████╔╝
   ╚═╝     ╚═╝  ╚═╝╚══════╝╚═╝     ╚═╝╚═╝ ╚═════╝ ╚═╝     ╚═╝    ╚═╝     ╚═╝  ╚═╝╚═════╝

   FUTURISTIC CENTERED RADIAL MENU
   - Full screen centered radial expansion
   - Non-overlapping circular layout
   - Pulsing glow effects
   - Glassmorphic design
   - Staggered item animations
   - Holographic overlays
═══════════════════════════════════════════════════════════════════════════════ */

// Premium color palette for FAB
private val FabGlowCyan = Color(0xFF00D9FF)
private val FabGlowEmerald = Color(0xFF10B981)
private val FabGlowPurple = Color(0xFF8B5CF6)
private val FabGlowPink = Color(0xFFFF6B9D)
private val FabGlowOrange = Color(0xFFFF8C42)
private val FabGlowBlue = Color(0xFF3B82F6)
private val FabDarkBase = Color(0xFF0A0F1E)
private val FabGlassBorder = Color(0x40FFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveFloatingButtons(
    dimens: DashboardDimens,
    fabExpanded: Boolean,
    onFabExpandedChange: (Boolean) -> Unit,
    onNavigateToAIChat: () -> Unit,
    onShowIncomeDialog: () -> Unit,
    onAddExpense: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToShoppingList: () -> Unit,
    onNavigateToExport: () -> Unit = {}
) {
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "fab_glow")

    // Main FAB glow pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Ring animation for AI button
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )

    // Menu items configuration - evenly distributed around circle
    val menuItems = remember {
        listOf(
            FabMenuItem(
                id = "income",
                label = "Add Income",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = FabGlowEmerald
            ),
            FabMenuItem(
                id = "expense",
                label = "Add Expense",
                icon = Icons.Default.Receipt,
                color = FabGlowPink
            ),
            FabMenuItem(
                id = "bills",
                label = "Manage Bills",
                icon = Icons.Default.Payment,
                color = FabGlowCyan
            ),
            FabMenuItem(
                id = "shopping",
                label = "Shopping List",
                icon = Icons.Default.ShoppingCart,
                color = FabGlowPurple
            ),
            FabMenuItem(
                id = "export",
                label = "Export Data",
                icon = Icons.Default.FileUpload,
                color = FabGlowOrange
            ),
            FabMenuItem(
                id = "ai",
                label = "AI Assistant",
                icon = Icons.Default.AutoAwesome,
                color = FabGlowBlue
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // CENTERED RADIAL MENU DIALOG
    // ═══════════════════════════════════════════════════════════════════
    if (fabExpanded) {
        Dialog(
            onDismissRequest = { onFabExpandedChange(false) },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            CenteredRadialMenu(
                menuItems = menuItems,
                glowPulse = glowPulse,
                ringRotation = ringRotation,
                onDismiss = { onFabExpandedChange(false) },
                onItemClick = { itemId ->
                    onFabExpandedChange(false)
                    when (itemId) {
                        "income" -> onShowIncomeDialog()
                        "expense" -> onAddExpense()
                        "bills" -> onNavigateToBills()
                        "shopping" -> onNavigateToShoppingList()
                        "export" -> onNavigateToExport()
                        "ai" -> onNavigateToAIChat()
                    }
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MAIN FAB BUTTON (Bottom Right)
    // ═══════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        MainFabButton(
            glowPulse = glowPulse,
            ringRotation = ringRotation,
            onClick = { onFabExpandedChange(true) }
        )
    }

    // Use dimens for responsive sizing
    @Suppress("UNUSED_VARIABLE")
    val fabSizeFromDimens = dimens.fabSize
}

/* ═══════════════════════════════════════════════════════════════════
   CENTERED RADIAL MENU
   Full screen centered menu with radial item layout
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun CenteredRadialMenu(
    menuItems: List<FabMenuItem>,
    glowPulse: Float,
    ringRotation: Float,
    onDismiss: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Calculate radius based on screen size - ensure items don't overlap
    val radius = minOf(screenWidth.value, screenHeight.value) * 0.32f

    // Animation for menu appearance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val menuScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "menu_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // ═══════════════════════════════════════════════════════════
        // TWO ROTATING RINGS - OPPOSITE DIRECTIONS
        // ═══════════════════════════════════════════════════════════

        // Outer ring - Clockwise
        Box(
            modifier = Modifier
                .size((radius * 2.8f).dp)
                .scale(menuScale)
                .rotate(ringRotation * 0.3f)
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FabGlowCyan.copy(alpha = 0f),
                            FabGlowCyan.copy(alpha = 0.5f),
                            FabGlowEmerald.copy(alpha = 0.5f),
                            FabGlowPurple.copy(alpha = 0.5f),
                            FabGlowCyan.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner ring - Counter-clockwise
        Box(
            modifier = Modifier
                .size((radius * 2.5f).dp)
                .scale(menuScale)
                .rotate(-ringRotation * 0.4f)
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FabGlowPurple.copy(alpha = 0f),
                            FabGlowPink.copy(alpha = 0.5f),
                            FabGlowOrange.copy(alpha = 0.5f),
                            FabGlowPink.copy(alpha = 0.5f),
                            FabGlowPurple.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Radial menu items - evenly distributed
        menuItems.forEachIndexed { index, item ->
            val angle = (360.0 / menuItems.size) * index - 90 // Start from top
            RadialMenuItemCentered(
                item = item,
                index = index,
                totalItems = menuItems.size,
                angle = angle,
                radius = radius,
                menuScale = menuScale,
                glowPulse = glowPulse,
                onClick = { onItemClick(item.id) }
            )
        }

        // Center close button
        CenterCloseButton(
            menuScale = menuScale,
            ringRotation = ringRotation,
            glowPulse = glowPulse,
            onClick = onDismiss
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════
   RADIAL MENU ITEM (CENTERED VERSION)
   Individual item in the centered radial menu
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadialMenuItemCentered(
    item: FabMenuItem,
    index: Int,
    totalItems: Int,
    angle: Double,
    radius: Float,
    menuScale: Float,
    glowPulse: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "item_${item.id}")

    // Staggered animation
    val itemDelay = index * 60
    val itemScale by animateFloatAsState(
        targetValue = if (menuScale > 0.5f) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = itemDelay,
            easing = FastOutSlowInEasing
        ),
        label = "item_scale_${item.id}"
    )

    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500 + (index * 150)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_${item.id}"
    )

    // Floating animation for 3D effect
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + (index * 100), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_${item.id}"
    )

    // Calculate position
    val angleRad = angle * PI / 180.0
    val x = (cos(angleRad) * radius).toFloat()
    val y = (sin(angleRad) * radius).toFloat()

    Box(
        modifier = Modifier
            .offset(x = x.dp, y = (y + floatOffset).dp)
            .graphicsLayer {
                scaleX = itemScale * menuScale
                scaleY = itemScale * menuScale
                alpha = itemScale
                // 3D rotation based on position
                rotationX = (y / radius) * 8f
                rotationY = -(x / radius) * 8f
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 3D Button with multiple layers
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Layer 1: Deep shadow (bottom layer)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = 4.dp)
                        .blur(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )

                // Layer 2: Outer glow ring
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .blur(12.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    item.color.copy(alpha = glowAlpha * glowPulse * 0.8f),
                                    item.color.copy(alpha = glowAlpha * glowPulse * 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Layer 3: Outer ring border
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    item.color.copy(alpha = 0.8f),
                                    item.color.copy(alpha = 0.3f),
                                    item.color.copy(alpha = 0.8f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Layer 4: Main button with 3D gradient
                Card(
                    onClick = onClick,
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            ambientColor = item.color.copy(alpha = 0.4f),
                            spotColor = item.color.copy(alpha = 0.6f)
                        ),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        item.color.copy(alpha = 1f),
                                        item.color.copy(alpha = 0.85f),
                                        item.color.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner highlight (top shine for 3D effect)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.1f),
                                            Color.Transparent,
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY
                                    ),
                                    shape = CircleShape
                                )
                        )

                        // Inner border for depth
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.5f),
                                            Color.White.copy(alpha = 0.1f),
                                            Color.Black.copy(alpha = 0.1f),
                                            Color.Black.copy(alpha = 0.2f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )

                        // Icon with shadow for 3D pop
                        Box(contentAlignment = Alignment.Center) {
                            // Icon shadow
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(30.dp)
                                    .offset(x = 1.dp, y = 2.dp)
                            )
                            // Main icon
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Layer 5: Top shine reflection
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer {
                            clip = true
                            shape = CircleShape
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = (-8).dp, y = (-12).dp)
                            .blur(8.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Enhanced 3D Label
            Box {
                // Label shadow
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.offset(y = 2.dp)
                ) {
                    Text(
                        text = item.label,
                        color = Color.Transparent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Main label with gradient background
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1a1a2e),
                                        Color(0xFF0f0f1a)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.2f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Text(
                            text = item.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   CENTER CLOSE BUTTON
   Close button in the center of the radial menu
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CenterCloseButton(
    menuScale: Float,
    ringRotation: Float,
    glowPulse: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating ring - Clockwise
        Box(
            modifier = Modifier
                .size(98.dp)
                .scale(menuScale)
                .rotate(ringRotation * 0.8f)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FabGlowCyan.copy(alpha = 0f),
                            FabGlowCyan.copy(alpha = 0.9f),
                            FabGlowEmerald.copy(alpha = 0.9f),
                            FabGlowCyan.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner rotating ring - Counter-clockwise
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(menuScale)
                .rotate(-ringRotation * 0.6f)
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FabGlowPurple.copy(alpha = 0f),
                            FabGlowPurple.copy(alpha = 0.8f),
                            FabGlowPink.copy(alpha = 0.8f),
                            FabGlowPurple.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Glow effect
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(menuScale)
                .blur(20.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FabGlowEmerald.copy(alpha = 0.6f * glowPulse),
                            FabGlowCyan.copy(alpha = 0.3f * glowPulse),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Center button with Budgie logo
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(76.dp)
                .scale(menuScale)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = FabGlowEmerald.copy(alpha = 0.4f),
                    spotColor = FabGlowCyan.copy(alpha = 0.6f)
                ),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E3A5F),
                                Color(0xFF0D1B2A)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                FabGlowCyan.copy(alpha = 0.6f),
                                FabGlowEmerald.copy(alpha = 0.4f),
                                FabGlowCyan.copy(alpha = 0.6f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Budgie Logo
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "Budgie",
                    modifier = Modifier
                        .size(52.dp)
                        .scale(1f + (glowPulse - 0.8f) * 0.1f),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Tap to close hint
        Text(
            text = "",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 20.dp)
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════
   MAIN FAB BUTTON
   The trigger button shown at bottom right
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFabButton(
    glowPulse: Float,
    ringRotation: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(bottom = 8.dp, end = 8.dp)
            .size(70.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated ring
        Box(
            modifier = Modifier
                .size(68.dp)
                .rotate(ringRotation)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FabGlowCyan.copy(alpha = 0f),
                            FabGlowCyan,
                            FabGlowEmerald,
                            FabGlowCyan.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Pulsing glow base
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(glowPulse * 0.25f + 0.85f)
                .blur(18.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FabGlowEmerald.copy(alpha = 0.5f * glowPulse),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main button
        Card(
            onClick = onClick,
            modifier = Modifier
                .size(58.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = FabGlowEmerald.copy(alpha = 0.4f),
                    spotColor = FabGlowEmerald.copy(alpha = 0.6f)
                ),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                FabGlowEmerald,
                                Color(0xFF059669)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Open Menu",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   DATA CLASSES
═══════════════════════════════════════════════════════════════════ */

private data class FabMenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

