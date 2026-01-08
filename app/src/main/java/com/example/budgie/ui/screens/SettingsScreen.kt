package com.example.budgie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgie.data.preferences.UserPreferencesManager
import com.example.budgie.security.SecurityManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    securityManager: com.example.budgie.security.SecurityManager? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { UserPreferencesManager.getInstance(context) }

    // Get or create SecurityManager instance
    val actualSecurityManager = remember(securityManager) {
        securityManager ?: com.example.budgie.security.SecurityManager(context)
    }

    var selectedCurrency by remember { mutableStateOf("KES") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var darkModeEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Account Section
        item {
            SectionHeader("Account")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Profile",
                subtitle = "Manage your profile information",
                onClick = { /* TODO */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Security",
                subtitle = "PIN, biometrics, and privacy",
                onClick = { /* TODO */ }
            )
        }

        // Preferences Section
        item {
            SectionHeader("Preferences")
        }

        item {
            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Enable push notifications",
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    scope.launch {
                        // Save to preferences
                    }
                }
            )
        }

        item {
            SettingsToggleItem(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Auth",
                subtitle = "Use fingerprint/face unlock",
                checked = biometricEnabled,
                onCheckedChange = {
                    biometricEnabled = it
                    scope.launch {
                        // Save to preferences
                    }
                }
            )
        }


        item {
            SettingsToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "Use dark theme",
                checked = darkModeEnabled,
                onCheckedChange = {
                    darkModeEnabled = it
                    scope.launch {
                        // Save to preferences
                    }
                }
            )
        }

        // Data & Privacy Section
        item {
            SectionHeader("Data & Privacy")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Download,
                title = "Export Data",
                subtitle = "Download your financial data",
                onClick = { /* TODO */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Clear Data",
                subtitle = "Delete all local data",
                onClick = { /* TODO */ },
                tintColor = Color(0xFFEF4444)
            )
        }

        // About Section
        item {
            SectionHeader("About")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "App Version",
                subtitle = "1.0.0",
                onClick = { }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "Read our privacy policy",
                onClick = { /* TODO */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Gavel,
                title = "Terms of Service",
                subtitle = "Read terms and conditions",
                onClick = { /* TODO */ }
            )
        }

        // Spacer at bottom
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF00F5A0),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tintColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00F5A0),
                    checkedTrackColor = Color(0xFF00F5A0).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}

