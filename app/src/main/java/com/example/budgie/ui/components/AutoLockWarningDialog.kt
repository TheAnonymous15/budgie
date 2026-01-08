package com.example.budgie.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AutoLockWarningDialog(
    countdownSeconds: Int,
    onStayActive: () -> Unit,
    onLockNow: () -> Unit
) {
    // Pulsing animation for urgency
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Colors
    val navyDeep = Color(0xFF0A1128)
    val emeraldGlow = Color(0xFF10B981)
    val amber = Color(0xFFFBBF24)
    val softWhite = Color(0xFFF8FAFC)

    Dialog(
        onDismissRequest = { /* Cannot dismiss by clicking outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = navyDeep.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Animated icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        amber.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = amber
                        )
                    }

                    // Title
                    Text(
                        text = "Inactivity Detected",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = softWhite
                    )

                    // Message
                    Text(
                        text = "Budgie will lock automatically to protect your financial data",
                        fontSize = 16.sp,
                        color = softWhite.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Countdown display
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        emeraldGlow.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Circle progress indicator
                        CircularProgressIndicator(
                            progress = { countdownSeconds / 10f },
                            modifier = Modifier.fillMaxSize(),
                            color = if (countdownSeconds > 5) emeraldGlow else Color(0xFFEF4444),
                            strokeWidth = 6.dp,
                            trackColor = softWhite.copy(alpha = 0.1f)
                        )

                        // Countdown number
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = countdownSeconds.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (countdownSeconds > 5) emeraldGlow else Color(0xFFEF4444)
                            )
                            Text(
                                text = "seconds",
                                fontSize = 12.sp,
                                color = softWhite.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stay Active button
                        Button(
                            onClick = onStayActive,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldGlow,
                                contentColor = navyDeep
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Stay Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Lock Now button
                        OutlinedButton(
                            onClick = onLockNow,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = softWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                softWhite.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lock Now",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Info text
                    Text(
                        text = "💡 Tip: Any interaction resets the timer",
                        fontSize = 12.sp,
                        color = softWhite.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

