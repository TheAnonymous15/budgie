package com.example.budgie.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.budgie.security.SecurityManager
import com.example.budgie.ui.screens.lockscreen.*
import kotlinx.coroutines.delay

/* ═══════════════════════════════════════════════════════════════════
   BUDGIE PREMIUM LOCK SCREEN
   Bank-Grade Security with AI-Powered Visual Design
   Fully Responsive for All Screen Sizes
═══════════════════════════════════════════════════════════════════ */

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
    val dimens = getLockScreenDimens()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

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

    val shieldPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldPulse"
    )

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    val counterRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRing"
    )

    val aiPresence by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiPresence"
    )

    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

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
        // Animated background
        PremiumBackground(
            particleDrift = particleDrift,
            glowIntensity = glowIntensity
        )

        // Main content - scrollable for small screens
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = dimens.horizontalPadding)
                .padding(
                    top = if (isLandscape) 16.dp else dimens.topToAiStatus,
                    bottom = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // AI Status Indicator
            AIGuardianStatus(
                dimens = dimens,
                aiPresence = aiPresence
            )

            Spacer(Modifier.height(dimens.aiStatusToCard))

            // Main Lock Card
            Card(
                modifier = Modifier
                    .widthIn(max = dimens.cardMaxWidth)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SoftWhite.copy(alpha = 0.1f),
                                Emerald.copy(alpha = 0.2f),
                                SoftWhite.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(dimens.cardCornerRadius)
                    ),
                shape = RoundedCornerShape(dimens.cardCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.03f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(dimens.cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Security Shield
                    AnimatedSecurityShield(
                        dimens = dimens,
                        shieldPulse = shieldPulse,
                        successScale = successScale,
                        glowIntensity = glowIntensity,
                        ringRotation = ringRotation,
                        counterRingRotation = counterRingRotation,
                        showSuccessAnimation = showSuccessAnimation,
                        authenticating = authenticating
                    )

                    Spacer(Modifier.height(dimens.shieldToTitle))

                    // Title
                    Text(
                        text = when {
                            showSuccessAnimation -> "Access Granted"
                            authenticating -> "Verifying..."
                            else -> "Budgie Locked"
                        },
                        fontSize = dimens.titleSize.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showSuccessAnimation) Gold else SoftWhite
                    )

                    Spacer(Modifier.height(dimens.titleToSubtitle))

                    // Subtitle
                    Text(
                        text = when {
                            showPin -> "ENTER YOUR SECURITY PIN"
                            showSuccessAnimation -> "WELCOME BACK"
                            else -> "VERIFY YOUR IDENTITY"
                        },
                        fontSize = dimens.subtitleSize.sp,
                        letterSpacing = dimens.subtitleLetterSpacing.sp,
                        color = CoolGray.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(dimens.subtitleToInput))

                    // PIN Input or Biometric Button
                    if (showPin || securityType == SecurityManager.SECURITY_PIN) {
                        ResponsivePinInput(
                            pin = pin,
                            error = error,
                            dimens = dimens,
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
                        // Biometric authentication button
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            ResponsiveBiometricAuthButton(
                                onClick = { launchBiometric() },
                                isLoading = authenticating,
                                dimens = dimens
                            )
                        }

                        // Switch to PIN option
                        Spacer(Modifier.height(dimens.verticalSpacing * 1.5f))

                        TextButton(
                            onClick = { showPin = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = CoolGray
                            )
                        ) {
                            Icon(
                                Icons.Default.Dialpad,
                                contentDescription = null,
                                modifier = Modifier.size(dimens.trustBadgeIconSize + 4.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Use PIN Instead",
                                fontSize = dimens.labelSize.sp
                            )
                        }
                    }

                    // Error message
                    error?.let { errorMessage ->
                        Spacer(Modifier.height(dimens.errorMessagePadding))
                        ErrorMessage(
                            message = errorMessage,
                            dimens = dimens
                        )
                    }

                    // Remaining attempts warning
                    if (biometricFails in 1 until maxAttempts) {
                        Spacer(Modifier.height(dimens.verticalSpacing))
                        Text(
                            "${maxAttempts - biometricFails} attempts remaining",
                            color = WarningAmber,
                            fontSize = dimens.labelSize.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimens.cardToTrustBadge))

            // Trust badge
            TrustBadge(dimens = dimens)
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   SUPPORTING COMPONENTS
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun ErrorMessage(
    message: String,
    dimens: LockScreenDimens
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                ErrorRed.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = dimens.errorMessagePadding,
                vertical = dimens.errorMessagePadding / 2
            )
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(dimens.trustBadgeIconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            color = ErrorRed,
            fontSize = dimens.labelSize.sp
        )
    }
}

@Composable
private fun TrustBadge(dimens: LockScreenDimens) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .alpha(0.6f)
            .background(
                Color.White.copy(alpha = 0.03f),
                RoundedCornerShape(20.dp)
            )
            .padding(
                horizontal = dimens.trustBadgePaddingHorizontal,
                vertical = dimens.trustBadgePaddingVertical
            )
    ) {
        Icon(
            Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = Emerald,
            modifier = Modifier.size(dimens.trustBadgeIconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Bank-Grade Encryption · Stored Locally",
            fontSize = dimens.trustBadgeFontSize.sp,
            color = SoftWhite.copy(alpha = 0.6f)
        )
    }
}

/* ─────────────────────────────────────────────── */

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

