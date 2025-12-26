package com.example.budgie.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/* ───────────────────────────────────────────────
   PREMIUM AI FINTECH COLOR SYSTEM
─────────────────────────────────────────────── */

private val NavyDark = Color(0xFF050F14)
private val NavyMid = Color(0xFF0A1A24)
private val Navy = Color(0xFF0B1F2A)

private val Emerald = Color(0xFF0FAE96)
private val Teal = Color(0xFF0B8F7A)
private val SoftWhite = Color(0xFFE6F1F0)
private val Gold = Color(0xFFBFA25A)
private val ErrorRed = Color(0xFFE57373)

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

    val maxAttempts = 5
    val fallbackToPin = biometricFails >= maxAttempts

    /* ───────── Animations ───────── */

    val infinite = rememberInfiniteTransition(label = "guardian")

    val pulse by infinite.animateFloat(
        0.96f, 1.04f,
        infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    val glow by infinite.animateFloat(
        0.18f, 0.38f,
        infiniteRepeatable(tween(3600), RepeatMode.Reverse),
        label = "glow"
    )

    val aiDot by infinite.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "aiDot"
    )

    val gridShift by infinite.animateFloat(
        0f, 80f,
        infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "grid"
    )

    /* ───────── Biometric Trigger ───────── */

    fun launchBiometric() {
        val activity = context.findFragmentActivity() ?: run {
            error = "Biometric unavailable"
            showPin = true
            return
        }

        error = null
        showBiometricPrompt(
            activity,
            securityManager,
            onSuccess = {
                authenticating = true
                onUnlocked()
            },
            onError = { error = it },
            onFailed = {
                biometricFails++
                if (biometricFails >= maxAttempts) {
                    showPin = true
                    error = "Biometric limit reached"
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (securityType == SecurityManager.SECURITY_PIN_BIOMETRIC && !fallbackToPin) {
            delay(900)
            if (!prompted) {
                prompted = true
                launchBiometric()
            }
        }
    }

    /* ───────── UI ───────── */

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(NavyDark, NavyMid, Navy))
        ),
        contentAlignment = Alignment.Center
    ) {
        GuardianBackground(gridShift)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            /* AI Presence Indicator */
            Box(
                Modifier.size(6.dp)
                    .alpha(aiDot)
                    .background(Emerald, CircleShape)
            )

            Spacer(Modifier.height(28.dp))

            Text(
                "BUDGIE",
                letterSpacing = 5.sp,
                color = SoftWhite.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(Color.White.copy(alpha = 0.035f)),
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                SoftWhite.copy(0.15f),
                                SoftWhite.copy(0.04f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    Modifier.padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(Modifier.scale(pulse)) {
                        Box(
                            Modifier.size(100.dp).blur(26.dp)
                                .background(Emerald.copy(alpha = glow), CircleShape)
                        )

                        Box(
                            Modifier.size(82.dp)
                                .border(
                                    2.dp,
                                    Brush.linearGradient(listOf(Emerald, Teal)),
                                    CircleShape
                                )
                                .background(Emerald.copy(0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (authenticating) Icons.Default.LockOpen else Icons.Default.Shield,
                                null,
                                tint = Emerald,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        if (authenticating) "Verifying identity…" else "Protected by Budgie AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftWhite
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        if (showPin) "PIN VERIFICATION REQUIRED"
                        else "IDENTITY CONFIRMATION",
                        letterSpacing = 1.6.sp,
                        color = SoftWhite.copy(0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(Modifier.height(30.dp))

                    if (showPin || securityType == SecurityManager.SECURITY_PIN) {
                        PremiumPin(
                            pin,
                            error,
                            onPin = {
                                pin = it
                                error = null
                                if (it.length == 5) {
                                    if (securityManager.verifyPin(it)) {
                                        authenticating = true
                                        onUnlocked()
                                    } else {
                                        pin = ""
                                        error = "Invalid PIN"
                                    }
                                }
                            }
                        )
                    } else {
                        TextButton(onClick = { launchBiometric() }) {
                            Icon(Icons.Default.Fingerprint, null, tint = Emerald)
                            Spacer(Modifier.width(8.dp))
                            Text("Touch sensor to verify", color = Emerald)
                        }
                    }

                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }

                    if (biometricFails in 1 until maxAttempts) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${maxAttempts - biometricFails} attempts remaining",
                            color = Gold.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.55f)) {
                Icon(Icons.Default.VerifiedUser, null, tint = Emerald, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "End-to-end encrypted · Stored locally",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftWhite.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────── */

@Composable
private fun GuardianBackground(offset: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val spacing = 64f
        for (i in 0..(size.width / spacing).toInt()) {
            val x = i * spacing + offset % spacing
            drawLine(
                SoftWhite.copy(0.014f),
                Offset(x, 0f),
                Offset(x, size.height),
                0.6f
            )
        }
    }
}

/* ─────────────────────────────────────────────── */

@Composable
private fun PremiumPin(
    pin: String,
    error: String?,
    onPin: (String) -> Unit
) {
    OutlinedTextField(
        value = pin,
        onValueChange = {
            if (it.all(Char::isDigit) && it.length <= 5) onPin(it)
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("•••••", color = SoftWhite.copy(0.3f)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald,
            unfocusedBorderColor = SoftWhite.copy(0.2f),
            focusedTextColor = SoftWhite,
            unfocusedTextColor = SoftWhite
        ),
        shape = RoundedCornerShape(14.dp),
        isError = error != null
    )
}

/* ─────────────────────────────────────────────── */

private fun showBiometricPrompt(
    activity: FragmentActivity,
    securityManager: SecurityManager,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFailed: () -> Unit
) {
    val manager = BiometricManager.from(activity)
    if (manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) != BiometricManager.BIOMETRIC_SUCCESS
    ) {
        onError("Biometric unavailable")
        return
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
        }
    )

    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm identity")
            .setSubtitle("Budgie AI is verifying you")
            .setNegativeButtonText("Use PIN")
            .build()
    )
}

/* ─────────────────────────────────────────────── */

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
