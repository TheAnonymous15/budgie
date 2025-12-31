package com.example.budgie.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive Security Selection Section component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveSecuritySelectionSection(
    securityType: String,
    onSecurityTypeChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    pinError: String?,
    onPinErrorChange: (String?) -> Unit,
    dimens: ResponsiveDimens,
    screenSize: ScreenSize
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(dimens.iconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Step 2 of 2 - Protect your finances",
                fontSize = dimens.labelFontSize.sp,
                color = Color(0xFF0FAE96)
            )
        }

        // PIN + Biometrics Card
        ResponsiveSecurityOptionCard(
            isSelected = securityType == "pin_biometric",
            onClick = { onSecurityTypeChange("pin_biometric") },
            icon = Icons.Filled.Fingerprint,
            secondaryIcon = Icons.Filled.Pin,
            title = "Biometrics + PIN",
            subtitle = if (screenSize == ScreenSize.COMPACT) "Recommended" else "Fingerprint + PIN backup",
            isRecommended = true,
            accentColor = Color(0xFF10B981),
            dimens = dimens,
            screenSize = screenSize
        )

        // PIN Only Card
        ResponsiveSecurityOptionCard(
            isSelected = securityType == "pin",
            onClick = { onSecurityTypeChange("pin") },
            icon = Icons.Filled.Lock,
            title = "PIN Only",
            subtitle = "5-digit code",
            isRecommended = false,
            accentColor = Color(0xFF3B82F6),
            dimens = dimens,
            screenSize = screenSize
        )

        // PIN Entry Field
        if (securityType == "pin" || securityType == "pin_biometric") {
            PinEntryCard(
                pin = pin,
                onPinChange = { onPinChange(it); onPinErrorChange(null) },
                confirmPin = confirmPin,
                onConfirmPinChange = onConfirmPinChange,
                pinError = pinError,
                onPinErrorChange = onPinErrorChange,
                dimens = dimens,
                screenSize = screenSize
            )
        }

        // Skip Security option
        SkipSecurityOption(
            isSelected = securityType == "none",
            onClick = { onSecurityTypeChange("none") }
        )
    }
}

@Composable
private fun PinEntryCard(
    pin: String,
    onPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    pinError: String?,
    onPinErrorChange: (String?) -> Unit,
    dimens: ResponsiveDimens,
    screenSize: ScreenSize
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1F2A).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(dimens.cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0FAE96).copy(alpha = 0.5f),
                    Color(0xFF0FAE96).copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create your PIN",
                fontSize = if (screenSize == ScreenSize.COMPACT) 13.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE6F1F0)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter a 5-digit PIN",
                fontSize = if (screenSize == ScreenSize.COMPACT) 11.sp else 12.sp,
                color = Color(0xFFE6F1F0).copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val pinStrength = remember(pin) {
                if (pin.length == 5) analyzePinStrength(pin) else null
            }

            // PIN Input Field
            OutlinedTextField(
                value = pin,
                onValueChange = { newPin ->
                    if (newPin.all { it.isDigit() } && newPin.length <= 5) {
                        onPinChange(newPin)
                    }
                },
                label = { Text("Enter PIN", color = Color(0xFFE6F1F0).copy(alpha = 0.8f), fontSize = dimens.labelFontSize.sp) },
                placeholder = { Text("5 digits", color = Color(0xFFE6F1F0).copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (pinStrength != null)
                        getPinStrengthColor(pinStrength.strength) else Color(0xFF0FAE96),
                    unfocusedBorderColor = Color(0xFFE6F1F0).copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFF0FAE96),
                    cursorColor = Color(0xFF0FAE96),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(dimens.cornerRadius - 4.dp),
                trailingIcon = {
                    if (pin.length == 5) {
                        Icon(
                            imageVector = if (pinStrength?.strength == PinStrength.STRONG)
                                Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = getPinStrengthColor(pinStrength?.strength ?: PinStrength.MODERATE)
                        )
                    }
                }
            )

            // PIN Strength Indicator
            if (pin.isNotEmpty()) {
                PinStrengthIndicator(pin = pin, pinStrength = pinStrength, screenSize = screenSize)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm PIN Field
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { newConfirmPin ->
                    if (newConfirmPin.all { it.isDigit() } && newConfirmPin.length <= 5) {
                        onConfirmPinChange(newConfirmPin)
                        if (newConfirmPin.length == 5 && pin.length == 5) {
                            if (newConfirmPin != pin) {
                                onPinErrorChange("PINs do not match")
                            } else {
                                onPinErrorChange(null)
                            }
                        }
                    }
                },
                label = { Text("Confirm PIN", color = Color(0xFFE6F1F0).copy(alpha = 0.8f), fontSize = dimens.labelFontSize.sp) },
                placeholder = { Text("Re-enter PIN", color = Color(0xFFE6F1F0).copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = pin.length == 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmPin.length == 5 && confirmPin == pin)
                        Color(0xFF0FAE96) else Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE6F1F0).copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFF0FAE96),
                    cursorColor = Color(0xFF0FAE96),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledBorderColor = Color(0xFFE6F1F0).copy(alpha = 0.2f),
                    disabledTextColor = Color(0xFFE6F1F0).copy(alpha = 0.5f),
                    disabledLabelColor = Color(0xFFE6F1F0).copy(alpha = 0.5f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(dimens.cornerRadius - 4.dp),
                trailingIcon = {
                    if (confirmPin.length == 5 && confirmPin == pin) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0FAE96)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status message
            PinStatusMessage(
                pin = pin,
                confirmPin = confirmPin,
                pinError = pinError
            )
        }
    }
}

@Composable
private fun PinStrengthIndicator(
    pin: String,
    pinStrength: PinStrengthResult?,
    screenSize: ScreenSize
) {
    Spacer(modifier = Modifier.height(8.dp))

    // Strength bar
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val strengthLevel = when {
            pin.length < 5 -> 0
            pinStrength?.strength == PinStrength.VERY_WEAK -> 1
            pinStrength?.strength == PinStrength.WEAK -> 2
            pinStrength?.strength == PinStrength.MODERATE -> 3
            pinStrength?.strength == PinStrength.STRONG -> 4
            else -> 0
        }

        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < strengthLevel) {
                            when (strengthLevel) {
                                1 -> Color(0xFFEF4444)
                                2 -> Color(0xFFF97316)
                                3 -> Color(0xFFEAB308)
                                4 -> Color(0xFF10B981)
                                else -> Color.Gray.copy(alpha = 0.3f)
                            }
                        } else {
                            Color.White.copy(alpha = 0.1f)
                        }
                    )
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Strength label and warnings
    if (pin.length == 5 && pinStrength != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (pinStrength.strength) {
                    PinStrength.VERY_WEAK -> "Very Weak"
                    PinStrength.WEAK -> "Weak"
                    PinStrength.MODERATE -> "Moderate"
                    PinStrength.STRONG -> "Strong"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = getPinStrengthColor(pinStrength.strength)
            )
            Text(
                text = "${pinStrength.score}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE6F1F0).copy(alpha = 0.6f)
            )
        }

        // Show warnings for weak PINs
        if (pinStrength.warnings.isNotEmpty() && pinStrength.strength != PinStrength.STRONG) {
            Spacer(modifier = Modifier.height(8.dp))
            PinWarningCard(pinStrength = pinStrength)
        }
    } else if (pin.length < 5) {
        Text(
            text = "Enter ${5 - pin.length} more digit${if (5 - pin.length > 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE6F1F0).copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun PinWarningCard(pinStrength: PinStrengthResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getPinStrengthColor(pinStrength.strength).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = getPinStrengthColor(pinStrength.strength),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (pinStrength.strength == PinStrength.VERY_WEAK)
                        "Weak PIN detected!" else "PIN could be stronger",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = getPinStrengthColor(pinStrength.strength)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Show first 2 warnings max
            pinStrength.warnings.take(2).forEach { warning ->
                Text(
                    text = "• $warning",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE6F1F0).copy(alpha = 0.7f)
                )
            }

            if (pinStrength.suggestion != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pinStrength.suggestion,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE6F1F0).copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PinStatusMessage(
    pin: String,
    confirmPin: String,
    pinError: String?
) {
    val finalPinStrength = remember(pin) {
        if (pin.length == 5) analyzePinStrength(pin) else null
    }

    when {
        pin.length == 5 && confirmPin.length == 5 && pin == confirmPin -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF0FAE96),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PIN set successfully!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0FAE96),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Show warning if weak PIN is confirmed
                if (finalPinStrength != null &&
                    (finalPinStrength.strength == PinStrength.VERY_WEAK ||
                     finalPinStrength.strength == PinStrength.WEAK)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Consider using a stronger PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF97316)
                        )
                    }
                }
            }
        }
        pinError != null -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = pinError,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444)
                )
            }
        }
        pin.length < 5 -> {
            Text(
                text = "Enter ${5 - pin.length} more digit${if (5 - pin.length > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE6F1F0).copy(alpha = 0.6f)
            )
        }
        confirmPin.length < 5 -> {
            Text(
                text = "Now confirm your PIN",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF0FAE96)
            )
        }
    }
}

@Composable
private fun SkipSecurityOption(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFFE6F1F0).copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = "Set up later",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE6F1F0).copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (!isSelected) {
            Text(
                text = " (not recommended)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE6F1F0).copy(alpha = 0.3f)
            )
        }
    }
}

