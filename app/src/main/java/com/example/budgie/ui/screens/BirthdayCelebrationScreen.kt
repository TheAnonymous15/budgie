package com.example.budgie.ui.screens

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.R
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Balloon(
    val x: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val swayOffset: Float
)

data class Confetti(
    val x: Float,
    val y: Float,
    val color: Color,
    val rotation: Float,
    val size: Float,
    val speedY: Float,
    val speedX: Float
)

@Composable
fun BirthdayCelebrationScreen(
    userName: String,
    age: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Auto-dismiss state - fade out in last 6 seconds of song
    var shouldFadeOut by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    var songDuration by remember { mutableStateOf(0) }

    // Fade animation for auto-dismiss (6 seconds fade)
    val screenAlpha by animateFloatAsState(
        targetValue = if (shouldFadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 6000, easing = EaseOutCubic),
        finishedListener = {
            if (shouldFadeOut && !dismissed) {
                dismissed = true
                onDismiss()
            }
        },
        label = "screenFade"
    )

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "birthday")

    // Cake bounce animation
    val cakeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cakeScale"
    )

    // Text glow animation
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    // Balloons
    val balloons = remember {
        List(15) {
            Balloon(
                x = Random.nextFloat(),
                color = listOf(
                    Color(0xFFFF6B6B), // Red
                    Color(0xFF4ECDC4), // Teal
                    Color(0xFFFFE66D), // Yellow
                    Color(0xFF95E1D3), // Mint
                    Color(0xFFFF8B94), // Pink
                    Color(0xFFA8E6CF), // Light Green
                    Color(0xFFDDA0DD), // Plum
                    Color(0xFF87CEEB)  // Sky Blue
                ).random(),
                size = Random.nextFloat() * 30 + 40,
                speed = Random.nextFloat() * 2 + 1,
                swayOffset = Random.nextFloat() * 360
            )
        }
    }

    // Confetti
    val confettiList = remember {
        List(50) {
            Confetti(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                color = listOf(
                    Color(0xFFFF6B6B),
                    Color(0xFF4ECDC4),
                    Color(0xFFFFE66D),
                    Color(0xFFFF8B94),
                    Color(0xFFA8E6CF),
                    Color(0xFFDDA0DD),
                    Color(0xFFFFD700)
                ).random(),
                rotation = Random.nextFloat() * 360,
                size = Random.nextFloat() * 10 + 5,
                speedY = Random.nextFloat() * 3 + 1,
                speedX = Random.nextFloat() * 2 - 1
            )
        }
    }

    var animationTime by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60fps
            animationTime += 0.016f
        }
    }

    // Play birthday song and monitor position to trigger fade in last 6 seconds
    LaunchedEffect(Unit) {
        try {
            val player = MediaPlayer.create(context, R.raw.birthday)
            if (player != null) {
                mediaPlayer = player
                songDuration = player.duration
                mediaPlayer?.isLooping = false
                mediaPlayer?.start()
                isPlaying = true

                // Monitor playback position to trigger fade in last 6 seconds
                while (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                    val currentPos = mediaPlayer?.currentPosition ?: 0
                    val remaining = songDuration - currentPos

                    // Start fade when 6 seconds remaining
                    if (remaining <= 6000 && !shouldFadeOut) {
                        shouldFadeOut = true
                    }
                    delay(100) // Check every 100ms
                }

                // If song completed without triggering fade (shouldn't happen but just in case)
                if (!shouldFadeOut) {
                    shouldFadeOut = true
                }
            } else {
                // No song file - show for 10 seconds then fade
                delay(10000)
                shouldFadeOut = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On error, show for 10 seconds then fade
            delay(10000)
            shouldFadeOut = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        // Animated Confetti
        Canvas(modifier = Modifier.fillMaxSize()) {
            confettiList.forEach { confetti ->
                val yPos = ((confetti.y + animationTime * confetti.speedY) % 1.2f) * size.height
                val xPos = (confetti.x + kotlin.math.sin(animationTime * 2 + confetti.x * 10) * 0.05f) * size.width

                rotate(confetti.rotation + animationTime * 100, Offset(xPos, yPos)) {
                    drawRect(
                        color = confetti.color,
                        topLeft = Offset(xPos - confetti.size / 2, yPos - confetti.size / 2),
                        size = androidx.compose.ui.geometry.Size(confetti.size, confetti.size * 0.6f)
                    )
                }
            }
        }

        // Animated Balloons
        Canvas(modifier = Modifier.fillMaxSize()) {
            balloons.forEach { balloon ->
                val yOffset = ((1.2f - animationTime * balloon.speed * 0.1f) % 1.4f - 0.2f) * size.height
                val xOffset = balloon.x * size.width + kotlin.math.sin(animationTime * 2 + balloon.swayOffset) * 30

                // Balloon body
                drawCircle(
                    color = balloon.color,
                    radius = balloon.size,
                    center = Offset(xOffset, yOffset)
                )

                // Balloon highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = balloon.size * 0.3f,
                    center = Offset(xOffset - balloon.size * 0.3f, yOffset - balloon.size * 0.3f)
                )

                // Balloon string
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(xOffset, yOffset + balloon.size),
                    end = Offset(xOffset + kotlin.math.sin(animationTime + balloon.swayOffset) * 10, yOffset + balloon.size + 60),
                    strokeWidth = 2f
                )
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Birthday Cake Emoji with animation
            Text(
                text = "🎂",
                fontSize = 100.sp,
                modifier = Modifier.scale(cakeScale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Happy Birthday Text with glow
            Text(
                text = "🎉 Happy Birthday! 🎉",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD700).copy(alpha = textAlpha),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User's name
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Age badge
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD700)
                )
            ) {
                Text(
                    text = "🎈 $age Years Young! 🎈",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Birthday message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌟 From Your Financial Bestie 🌟",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Wishing you a year filled with prosperity, smart financial decisions, and all your dreams coming true! May your savings grow and your worries shrink! 💰✨",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Music control
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Play/Pause button
                FloatingActionButton(
                    onClick = {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                        } else {
                            mediaPlayer?.start()
                        }
                        isPlaying = !isPlaying
                    },
                    containerColor = Color(0xFFFFD700)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF1A1A2E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4ECDC4)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "🎁 Open My Gifts (Continue to App)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Sparkles in corners
        SparkleEffect(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        )
        SparkleEffect(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        )
        SparkleEffect(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        )
        SparkleEffect(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}

@Composable
fun SparkleEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )

    Text(
        text = "✨",
        fontSize = 30.sp,
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
    )
}

