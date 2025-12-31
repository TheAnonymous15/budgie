package com.example.budgie.ui.screens.lockscreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ═══════════════════════════════════════════════════════════════════
   PIN INPUT COMPONENTS
   Responsive PIN keypad and input display
═══════════════════════════════════════════════════════════════════ */

/**
 * Complete PIN input section with dots and keypad
 */
@Composable
fun ResponsivePinInput(
    pin: String,
    error: String?,
    dimens: LockScreenDimens,
    onPinChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN dots display
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.pinDotSpacing),
            modifier = Modifier.padding(bottom = dimens.subtitleToInput * 0.8f)
        ) {
            repeat(5) { index ->
                val isFilled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(dimens.pinDotSize)
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

        // Hidden text field for keyboard input (accessibility)
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
            verticalArrangement = Arrangement.spacedBy(dimens.pinKeySpacing * 0.6f)
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            ).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.pinKeySpacing)
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(dimens.pinKeySize))
                        } else {
                            ResponsivePinKey(
                                key = key,
                                size = dimens.pinKeySize,
                                fontSize = dimens.pinKeyFontSize,
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

/**
 * Individual PIN keypad key
 */
@Composable
fun ResponsivePinKey(
    key: String,
    size: Dp,
    fontSize: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
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
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Delete",
                tint = SoftWhite,
                modifier = Modifier.size(size * 0.375f)
            )
        } else {
            Text(
                text = key,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Medium,
                color = SoftWhite
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   BIOMETRIC AUTHENTICATION BUTTON
   Animated responsive biometric unlock button
═══════════════════════════════════════════════════════════════════ */

/**
 * Biometric authentication button with animated glow
 */
@Composable
fun ResponsiveBiometricAuthButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    dimens: LockScreenDimens
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
            .fillMaxWidth(dimens.biometricButtonWidth)
            .height(dimens.biometricButtonHeight)
            .clip(RoundedCornerShape(dimens.biometricButtonCornerRadius))
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
                shape = RoundedCornerShape(dimens.biometricButtonCornerRadius)
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
                    modifier = Modifier.size(dimens.biometricIconSize),
                    color = SoftWhite,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    tint = SoftWhite,
                    modifier = Modifier.size(dimens.biometricIconSize)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (isLoading) "Authenticating..." else "Use Fingerprint",
                fontSize = dimens.biometricFontSize.sp,
                fontWeight = FontWeight.Medium,
                color = SoftWhite
            )
        }
    }
}

/* ─────────────────────────────────────────────── */

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

