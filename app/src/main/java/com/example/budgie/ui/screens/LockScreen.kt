package com.example.budgie.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.budgie.security.SecurityManager
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/* ═══════════════════════════════════════════════════════════════════
   BUDGIE PREMIUM LOCK SCREEN
   Bank-Grade Security with AI-Powered Visual Design
═══════════════════════════════════════════════════════════════════ */

// Premium Color Palette - Wealth & Trust
private val DeepNavy = Color(0xFF030B10)
private val MidnightNavy = Color(0xFF071620)
private val Navy = Color(0xFF0B1F2A)
private val NavyLight = Color(0xFF122A38)

private val Emerald = Color(0xFF0FAE96)
private val EmeraldBright = Color(0xFF10D9B8)
private val Teal = Color(0xFF0B8F7A)
private val TealDark = Color(0xFF086658)

private val SoftWhite = Color(0xFFE6F1F0)
private val CoolGray = Color(0xFF94A3B8)
private val Gold = Color(0xFFD4AF37)
private val GoldMuted = Color(0xFFBFA25A)
private val ErrorRed = Color(0xFFEF5350)
private val WarningAmber = Color(0xFFFFB74D)

/* ─────────────────────────────────────────────── */

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun LockScreen(
    securityManager: SecurityManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val securityType = securityManager.getSecurityType()

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var biometricFails by remember { mutableStateOf(0) }
    var showPin by remember { mutableStateOf(securityType == SecurityManager.SECURITY_PIN) }
    var prompted by remember { mutableStateOf(false) }
    var authenticating by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val maxAttempts = 5
    val fallbackToPin = biometricFails >= maxAttempts

    /* ═══════════ ANIMATIONS ═══════════ */

    val infiniteTransition = rememberInfiniteTransition(label = "lockscreen")

    // Shield pulse animation
    val shieldPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldPulse"
    )

    // Glow intensity animation
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Ring rotation animation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // Counter ring rotation
    val counterRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRing"
    )

    // AI presence dot
    val aiPresence by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiPresence"
    )

    // Particle drift
    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    // Success animation
    val successScale by animateFloatAsState(
        targetValue = if (showSuccessAnimation) 1.3f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "success"
    )

    /* ═══════════ BIOMETRIC FUNCTIONS ═══════════ */

    fun launchBiometric() {
        val activity = context.findFragmentActivity() ?: run {
            error = "Authentication unavailable"
            showPin = true
            return
        }

        error = null
        showBiometricPrompt(
            activity,
            securityManager,
            onSuccess = {
                showSuccessAnimation = true
                authenticating = true
                onUnlocked()
            },
            onError = { error = it },
            onFailed = {
                biometricFails++
                if (biometricFails >= maxAttempts) {
                    showPin = true
                    error = "Please use PIN"
                }
            }
        )
    }

    // Auto-launch biometric
    LaunchedEffect(Unit) {
        if (securityType == SecurityManager.SECURITY_PIN_BIOMETRIC && !fallbackToPin) {
            delay(800)
            if (!prompted) {
                prompted = true
                launchBiometric()
            }
        }
    }

    /* ═══════════ MAIN UI ═══════════ */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepNavy, MidnightNavy, Navy),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background elements
        PremiumBackground(
            particleDrift = particleDrift,
            glowIntensity = glowIntensity
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // AI Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(0.7f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(aiPresence)
                        .background(Emerald, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI GUARDIAN ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 2.sp,
                    color = SoftWhite.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(40.dp))

            // Main Lock Card
            Card(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SoftWhite.copy(alpha = 0.1f),
                                Emerald.copy(alpha = 0.2f),
                                SoftWhite.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.03f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shield Icon with Animated Rings
                    Box(
                        modifier = Modifier
                            .scale(shieldPulse * successScale)
                            .size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow
                        Box(
                            modifier = Modifier
                                .size(140.dp)
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
                                .size(130.dp)
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
                                .size(110.dp)
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
                                .size(90.dp)
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
                                imageVector = if (showSuccessAnimation)
                                    Icons.Default.LockOpen
                                else if (authenticating)
                                    Icons.Default.Sync
                                else
                                    Icons.Default.Shield,
                                contentDescription = "Security Shield",
                                tint = if (showSuccessAnimation) Gold else Emerald,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Title
                    Text(
                        text = when {
                            showSuccessAnimation -> "Access Granted"
                            authenticating -> "Verifying Identity..."
                            else -> "Budgie Locked"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showSuccessAnimation) Gold else SoftWhite
                    )

                    Spacer(Modifier.height(8.dp))

                    // Subtitle
                    Text(
                        text = when {
                            showPin -> "ENTER YOUR SECURITY PIN"
                            showSuccessAnimation -> "WELCOME BACK"
                            else -> "VERIFY YOUR IDENTITY"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.5.sp,
                        color = CoolGray.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(32.dp))

                    // PIN Input or Biometric Button
                    if (showPin || securityType == SecurityManager.SECURITY_PIN) {
                        PremiumPinInput(
                            pin = pin,
                            error = error,
                            onPinChange = { newPin ->
                                pin = newPin
                                error = null
                                if (newPin.length == 5) {
                                    if (securityManager.verifyPin(newPin)) {
                                        showSuccessAnimation = true
                                        authenticating = true
                                        onUnlocked()
                                    } else {
                                        pin = ""
                                        error = "Incorrect PIN"
                                    }
                                }
                            }
                        )
                    } else {
                        // Biometric authentication button - centered
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            BiometricAuthButton(
                                onClick = { launchBiometric() },
                                isLoading = authenticating
                            )
                        }

                        // Switch to PIN option
                        Spacer(Modifier.height(20.dp))

                        TextButton(
                            onClick = { showPin = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = CoolGray
                            )
                        ) {
                            Icon(
                                Icons.Default.Dialpad,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Use PIN Instead",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Error message
                    error?.let { errorMessage ->
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    ErrorRed.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                errorMessage,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Remaining attempts warning
                    if (biometricFails in 1 until maxAttempts) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${maxAttempts - biometricFails} attempts remaining",
                            color = WarningAmber,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Trust badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(0.6f)
                    .background(
                        Color.White.copy(alpha = 0.03f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Emerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bank-Grade Encryption · Stored Locally",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftWhite.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   SUPPORTING COMPONENTS
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun PremiumBackground(
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

@Composable
private fun PremiumPinInput(
    pin: String,
    error: String?,
    onPinChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN dots display
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(5) { index ->
                val isFilled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            width = 2.dp,
                            color = when {
                                error != null -> ErrorRed
                                isFilled -> Emerald
                                else -> CoolGray.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                        .background(
                            color = if (isFilled) Emerald else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }

        // Hidden text field for keyboard input
        OutlinedTextField(
            value = pin,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                    onPinChange(newValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f)
                .height(1.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation()
        )

        // Numeric keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            ).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(64.dp))
                        } else {
                            PinKey(
                                key = key,
                                onClick = {
                                    when (key) {
                                        "⌫" -> if (pin.isNotEmpty()) onPinChange(pin.dropLast(1))
                                        else -> if (pin.length < 5) onPinChange(pin + key)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    key: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                Color.White.copy(alpha = 0.05f),
                CircleShape
            )
            .border(
                width = 1.dp,
                color = SoftWhite.copy(alpha = 0.1f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Emerald),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") {
            Icon(
                Icons.Default.Backspace,
                contentDescription = "Delete",
                tint = SoftWhite,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = key,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = SoftWhite
            )
        }
    }
}

@Composable
private fun BiometricAuthButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "biometricButton")
    val buttonGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.50f)  // Reduced width to 75%
            .height(48.dp)  // Reduced height from 60dp to 48dp
            .clip(RoundedCornerShape(12.dp))  // Slightly smaller corner radius
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Emerald.copy(alpha = buttonGlow),
                        Teal.copy(alpha = buttonGlow)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(EmeraldBright, Emerald)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),  // Slightly smaller
                    color = SoftWhite,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    tint = SoftWhite,
                    modifier = Modifier.size(22.dp)  // Reduced from 28dp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (isLoading) "Authenticating..." else "Use Fingerprint",
                style = MaterialTheme.typography.bodyLarge,  // Smaller text style
                fontWeight = FontWeight.Medium,
                color = SoftWhite
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   BIOMETRIC PROMPT HANDLER
═══════════════════════════════════════════════════════════════════ */

private fun showBiometricPrompt(
    activity: FragmentActivity,
    securityManager: SecurityManager,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFailed: () -> Unit
) {
    val manager = BiometricManager.from(activity)
    val strongAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

    // Check if any biometric is available
    if (strongAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        val weakAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (weakAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometric authentication not available")
            return
        }
    }

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                securityManager.setAuthenticated(true)
                onSuccess()
            }

            override fun onAuthenticationFailed() = onFailed()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        }
    )

    try {
        val promptInfo = if (strongAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Budgie")
                .setSubtitle("Verify your identity to continue")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Use PIN")
                .build()
        } else {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Budgie")
                .setSubtitle("Verify your identity to continue")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        }

        prompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onError("Authentication error: ${e.message}")
    }
}

/* ─────────────────────────────────────────────── */

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

