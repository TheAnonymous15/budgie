package com.example.budgie.ui.components

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Theme colors
private val RingtoneNavy = Color(0xFF0A1929)
private val RingtoneNavyLight = Color(0xFF1A3A4A)
private val RingtoneEmerald = Color(0xFF0FAE96)
private val RingtoneSoftWhite = Color(0xFFF0F4F8)
private val RingtoneAmber = Color(0xFFFFB74D)

data class RingtoneItem(
    val name: String,
    val uri: Uri?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtonePickerDialog(
    currentRingtoneUri: Uri?,
    onRingtoneSelected: (Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var ringtones by remember { mutableStateOf<List<RingtoneItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedUri by remember { mutableStateOf(currentRingtoneUri) }
    var currentlyPlayingRingtone by remember { mutableStateOf<android.media.Ringtone?>(null) }

    // Load ringtones
    LaunchedEffect(Unit) {
        ringtones = withContext(Dispatchers.IO) {
            loadSystemRingtones(context)
        }
        isLoading = false
    }

    // Stop playing ringtone when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            currentlyPlayingRingtone?.stop()
        }
    }

    FixedSizeDialog(onDismissRequest = {
        currentlyPlayingRingtone?.stop()
        onDismiss()
    }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 550.dp),
            colors = CardDefaults.cardColors(
                containerColor = RingtoneNavy
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    RingtoneEmerald.copy(alpha = 0.3f),
                                    RingtoneNavyLight
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(RingtoneEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = RingtoneEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Select Notification Sound",
                                    color = RingtoneSoftWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "Choose a sound for all Budgie notifications",
                                    color = RingtoneSoftWhite.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                currentlyPlayingRingtone?.stop()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(RingtoneSoftWhite.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = RingtoneSoftWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RingtoneEmerald)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ringtones) { ringtone ->
                            RingtoneItemRow(
                                ringtone = ringtone,
                                isSelected = selectedUri == ringtone.uri ||
                                            (selectedUri == null && ringtone.uri == null),
                                onSelect = {
                                    // Stop previous playing
                                    currentlyPlayingRingtone?.stop()

                                    selectedUri = ringtone.uri

                                    // Play preview
                                    if (ringtone.uri != null) {
                                        try {
                                            val newRingtone = RingtoneManager.getRingtone(context, ringtone.uri)
                                            newRingtone?.play()
                                            currentlyPlayingRingtone = newRingtone
                                        } catch (e: Exception) {
                                            // Ignore playback errors
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Footer with buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RingtoneNavyLight)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentlyPlayingRingtone?.stop()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RingtoneSoftWhite
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    RingtoneSoftWhite.copy(alpha = 0.3f),
                                    RingtoneSoftWhite.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            currentlyPlayingRingtone?.stop()
                            onRingtoneSelected(selectedUri)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RingtoneEmerald
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Select", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RingtoneItemRow(
    ringtone: RingtoneItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                RingtoneEmerald.copy(alpha = 0.15f)
            else
                RingtoneNavyLight.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radio indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) RingtoneEmerald
                        else RingtoneSoftWhite.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Icon
            Icon(
                if (ringtone.uri == null) Icons.Default.NotificationsOff
                else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isSelected) RingtoneEmerald else RingtoneSoftWhite.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )

            // Name
            Text(
                text = ringtone.name,
                color = if (isSelected) RingtoneEmerald else RingtoneSoftWhite,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            // Play indicator for selected
            if (isSelected && ringtone.uri != null) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Playing",
                    tint = RingtoneEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun loadSystemRingtones(context: Context): List<RingtoneItem> {
    val ringtones = mutableListOf<RingtoneItem>()

    // Add default/silent option
    ringtones.add(RingtoneItem("Default system sound", null))

    // Load notification sounds
    try {
        val notificationManager = RingtoneManager(context)
        notificationManager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val notificationCursor = notificationManager.cursor

        if (notificationCursor.moveToFirst()) {
            do {
                val title = notificationCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = notificationManager.getRingtoneUri(notificationCursor.position)
                ringtones.add(RingtoneItem(title, uri))
            } while (notificationCursor.moveToNext())
        }
    } catch (e: Exception) {
        // Ignore errors loading notification sounds
    }

    // Load ringtones as well
    try {
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
        val ringtoneCursor = ringtoneManager.cursor

        if (ringtoneCursor.moveToFirst()) {
            do {
                val title = ringtoneCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneManager.getRingtoneUri(ringtoneCursor.position)
                // Avoid duplicates
                if (ringtones.none { it.uri == uri }) {
                    ringtones.add(RingtoneItem("♪ $title", uri))
                }
            } while (ringtoneCursor.moveToNext())
        }
    } catch (e: Exception) {
        // Ignore errors loading ringtones
    }

    // Load alarm sounds
    try {
        val alarmManager = RingtoneManager(context)
        alarmManager.setType(RingtoneManager.TYPE_ALARM)
        val alarmCursor = alarmManager.cursor

        if (alarmCursor.moveToFirst()) {
            do {
                val title = alarmCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = alarmManager.getRingtoneUri(alarmCursor.position)
                // Avoid duplicates
                if (ringtones.none { it.uri == uri }) {
                    ringtones.add(RingtoneItem("⏰ $title", uri))
                }
            } while (alarmCursor.moveToNext())
        }
    } catch (e: Exception) {
        // Ignore errors loading alarm sounds
    }

    return ringtones
}
