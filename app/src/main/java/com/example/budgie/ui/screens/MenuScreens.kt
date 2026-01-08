package com.example.budgie.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.data.preferences.CurrencyManager
import com.example.budgie.data.preferences.Currency
import com.example.budgie.data.preferences.UserPreferencesManager
import com.example.budgie.security.SecurityManager
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ui.components.formatCurrency
import com.example.budgie.ui.components.CurrencyConverterModal
import com.example.budgie.ui.components.CalculatorModal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Color Theme
private val MenuNavy = Color(0xFF0B1F2A)
private val MenuNavyLight = Color(0xFF142D3C)
private val MenuEmerald = Color(0xFF0FAE96)
private val MenuTeal = Color(0xFF0B8F7A)
private val MenuGold = Color(0xFFC9A14A)
private val MenuSoftWhite = Color(0xFFE6F1F0)
private val MenuMutedRed = Color(0xFFE57373)
private val MenuAmber = Color(0xFFFFB74D)
private val MenuBlue = Color(0xFF5C9CE5)
private val MenuPurple = Color(0xFF9575CD)

// Glassmorphism helpers
private fun Modifier.menuGlassCard(cornerRadius: Int = 16) = this
    .shadow(8.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))),
        RoundedCornerShape(cornerRadius.dp)
    )

// ==================== PROFILE SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager.getInstance(context) }
    val userProfile by preferencesManager.userProfile.collectAsState(initial = null)
    val summary by viewModel.financialSummary.collectAsState()
    val goalsSummary by viewModel.goalsSummary.collectAsState()
    val loanSummary by viewModel.loanSummary.collectAsState()

    val scope = rememberCoroutineScope()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Calculate user stats
    val memberSince = userProfile?.let {
        try {
            val parts = it.birthday.split("/")
            "Member since ${Calendar.getInstance().get(Calendar.YEAR)}"
        } catch (e: Exception) {
            "Member"
        }
    } ?: "Member"

    val age = userProfile?.let {
        try {
            val parts = it.birthday.split("/")
            val birthYear = parts[0].toInt()
            Calendar.getInstance().get(Calendar.YEAR) - birthYear
        } catch (e: Exception) {
            null
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = MenuNavyLight,
            title = { Text("Edit Display Name", color = MenuSoftWhite) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MenuSoftWhite,
                        unfocusedTextColor = MenuSoftWhite,
                        focusedBorderColor = MenuEmerald,
                        unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        scope.launch {
                            userProfile?.let { profile ->
                                preferencesManager.saveUserProfile(profile.copy(name = newName))
                            }
                        }
                    }
                    showEditNameDialog = false
                }) {
                    Text("Save", color = MenuEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = MenuSoftWhite.copy(alpha = 0.7f))
                }
            }
        )
    }

    Scaffold(
        containerColor = MenuNavy,
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = MenuSoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MenuSoftWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        newName = userProfile?.name ?: ""
                        showEditNameDialog = true
                    }) {
                        Icon(Icons.Default.Edit, "Edit", tint = MenuEmerald)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MenuNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(MenuEmerald, MenuTeal)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile?.name?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = userProfile?.name?.replaceFirstChar { it.uppercase() } ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MenuSoftWhite
                        )

                        if (age != null) {
                            Text(
                                text = "$age years old",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MenuSoftWhite.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Member badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MenuEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MenuEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Premium Member",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MenuEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Financial Overview
            item {
                Text(
                    text = "Financial Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MenuSoftWhite
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(
                        icon = Icons.Default.TrendingUp,
                        label = "Total Income",
                        value = formatCurrency(summary.totalIncome),
                        color = MenuEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        icon = Icons.Default.TrendingDown,
                        label = "Total Spent",
                        value = formatCurrency(summary.totalExpenses),
                        color = MenuMutedRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(
                        icon = Icons.Default.Savings,
                        label = "Net Savings",
                        value = formatCurrency(summary.netSavings),
                        color = if (summary.netSavings >= 0) MenuEmerald else MenuMutedRed,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        icon = Icons.Default.Flag,
                        label = "Active Goals",
                        value = "${goalsSummary.activeGoals}",
                        color = MenuBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Achievements Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MenuSoftWhite
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(16)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AchievementItem(
                            icon = Icons.Default.Rocket,
                            title = "Early Adopter",
                            description = "Started financial tracking journey",
                            earned = true,
                            color = MenuGold
                        )
                        AchievementItem(
                            icon = Icons.Default.Savings,
                            title = "First Saver",
                            description = "Saved your first amount",
                            earned = summary.netSavings > 0,
                            color = MenuEmerald
                        )
                        AchievementItem(
                            icon = Icons.Default.Flag,
                            title = "Goal Setter",
                            description = "Created your first financial goal",
                            earned = goalsSummary.activeGoals > 0,
                            color = MenuBlue
                        )
                        AchievementItem(
                            icon = Icons.Default.TrendingUp,
                            title = "Budget Master",
                            description = "Stay within budget for a month",
                            earned = summary.savingsRate >= 0.2,
                            color = MenuPurple
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.menuGlassCard(14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MenuSoftWhite.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AchievementItem(
    icon: ImageVector,
    title: String,
    description: String,
    earned: Boolean,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (earned) color.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (earned) color else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (earned) MenuSoftWhite else MenuSoftWhite.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MenuSoftWhite.copy(alpha = if (earned) 0.6f else 0.3f)
            )
        }
        if (earned) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== SETTINGS SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    // Settings state - persisted
    var notificationsEnabled by remember { mutableStateOf(preferencesManager.getNotificationsEnabled()) }
    var dailyRemindersEnabled by remember { mutableStateOf(preferencesManager.getDailyRemindersEnabled()) }
    var billRemindersEnabled by remember { mutableStateOf(preferencesManager.getBillRemindersEnabled()) }
    var dailyInsightsEnabled by remember { mutableStateOf(preferencesManager.getDailyInsightsEnabled()) }

    // Currency Manager
    val currencyManager = remember { CurrencyManager.getInstance(context) }
    var selectedCurrency by remember { mutableStateOf(currencyManager.getSelectedCurrency()) }

    // Dialog states
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showResetAppDialog by remember { mutableStateOf(false) }
    var showDataStorageDialog by remember { mutableStateOf(false) }
    var currencySearchQuery by remember { mutableStateOf("") }

    val allCurrencies = CurrencyManager.currencies

    val filteredCurrencies = remember(currencySearchQuery, allCurrencies) {
        if (currencySearchQuery.isBlank()) {
            allCurrencies
        } else {
            allCurrencies.filter {
                it.code.contains(currencySearchQuery, ignoreCase = true) ||
                it.name.contains(currencySearchQuery, ignoreCase = true)
            }
        }
    }


    // Currency Dialog - Enhanced with search and full currency list
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = {
                showCurrencyDialog = false
                currencySearchQuery = ""
            },
            containerColor = MenuNavyLight,
            title = {
                Column {
                    Text("Select Currency", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currencySearchQuery,
                        onValueChange = { currencySearchQuery = it },
                        placeholder = { Text("Search currency...", color = MenuSoftWhite.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = MenuSoftWhite.copy(alpha = 0.5f))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MenuEmerald,
                            unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = MenuSoftWhite,
                            unfocusedTextColor = MenuSoftWhite,
                            cursorColor = MenuEmerald
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(filteredCurrencies) { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCurrency = currency
                                    currencyManager.setSelectedCurrency(currency)
                                    preferencesManager.setCurrencySymbol(currency.symbol)
                                    showCurrencyDialog = false
                                    currencySearchQuery = ""
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(currency.flag, fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${currency.code} - ${currency.symbol}",
                                    color = MenuSoftWhite,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    currency.name,
                                    color = MenuSoftWhite.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            if (selectedCurrency.code == currency.code) {
                                Icon(Icons.Default.CheckCircle, null, tint = MenuEmerald)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCurrencyDialog = false
                    currencySearchQuery = ""
                }) {
                    Text("Cancel", color = MenuSoftWhite.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Clear All Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = MenuNavyLight,
            icon = {
                Icon(Icons.Default.DeleteForever, null, tint = MenuMutedRed, modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Clear All Data?", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This will permanently delete all your financial records including:",
                        color = MenuSoftWhite.copy(alpha = 0.8f)
                    )
                    Text("• All expenses and income", color = MenuMutedRed)
                    Text("• All bills and budgets", color = MenuMutedRed)
                    Text("• All goals and loans", color = MenuMutedRed)
                    Text("• All shopping lists", color = MenuMutedRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ This action cannot be undone!",
                        color = MenuAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.clearAllData()
                            showClearDataDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MenuMutedRed)
                ) {
                    Text("Delete All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = MenuSoftWhite)
                }
            }
        )
    }

    // Reset App Dialog
    if (showResetAppDialog) {
        AlertDialog(
            onDismissRequest = { showResetAppDialog = false },
            containerColor = MenuNavyLight,
            icon = {
                Icon(Icons.Default.RestartAlt, null, tint = MenuMutedRed, modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Reset App?", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This will reset Budgie to factory settings:",
                        color = MenuSoftWhite.copy(alpha = 0.8f)
                    )
                    Text("• Delete all financial data", color = MenuMutedRed)
                    Text("• Remove your profile", color = MenuMutedRed)
                    Text("• Clear all settings", color = MenuMutedRed)
                    Text("• Remove PIN/biometric setup", color = MenuMutedRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ You will need to set up the app again!",
                        color = MenuAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.clearAllData()
                            preferencesManager.clearAllPreferences()
                            showResetAppDialog = false
                            // Navigate back to trigger onboarding
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MenuMutedRed)
                ) {
                    Text("Reset App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAppDialog = false }) {
                    Text("Cancel", color = MenuSoftWhite)
                }
            }
        )
    }

    // Data & Storage Dialog
    if (showDataStorageDialog) {
        AlertDialog(
            onDismissRequest = { showDataStorageDialog = false },
            containerColor = MenuNavyLight,
            icon = {
                Icon(Icons.Default.Storage, null, tint = MenuBlue, modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Data & Storage", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DataStorageRow("Local Database", "All your financial data", MenuEmerald)
                    DataStorageRow("Preferences", "App settings & profile", MenuBlue)
                    DataStorageRow("Cache", "Temporary files", MenuAmber)
                    HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Storage Used", color = MenuSoftWhite)
                        Text("~2 MB", color = MenuEmerald, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "💡 Your data is stored securely on this device only.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MenuSoftWhite.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDataStorageDialog = false }) {
                    Text("Close", color = MenuEmerald)
                }
            }
        )
    }

    Scaffold(
        containerColor = MenuNavy,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = MenuSoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MenuSoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MenuNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Notifications Section
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    color = MenuEmerald
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().menuGlassCard(16)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsToggleItem(
                            title = "Push Notifications",
                            description = "Enable all notifications",
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                preferencesManager.setNotificationsEnabled(it)
                                if (!it) {
                                    dailyRemindersEnabled = false
                                    billRemindersEnabled = false
                                    dailyInsightsEnabled = false
                                    preferencesManager.setDailyRemindersEnabled(false)
                                    preferencesManager.setBillRemindersEnabled(false)
                                    preferencesManager.setDailyInsightsEnabled(false)
                                }
                            }
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsToggleItem(
                            title = "Daily Reminders",
                            description = "Morning & evening expense reminders",
                            checked = dailyRemindersEnabled,
                            onCheckedChange = {
                                dailyRemindersEnabled = it
                                preferencesManager.setDailyRemindersEnabled(it)
                            },
                            enabled = notificationsEnabled
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsToggleItem(
                            title = "Bill Reminders",
                            description = "Get notified before bills are due",
                            checked = billRemindersEnabled,
                            onCheckedChange = {
                                billRemindersEnabled = it
                                preferencesManager.setBillRemindersEnabled(it)
                            },
                            enabled = notificationsEnabled
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsToggleItem(
                            title = "Daily Insights",
                            description = "AI-powered financial tips & insights",
                            checked = dailyInsightsEnabled,
                            onCheckedChange = {
                                dailyInsightsEnabled = it
                                preferencesManager.setDailyInsightsEnabled(it)
                            },
                            enabled = notificationsEnabled
                        )
                    }
                }
            }

            // General Section
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Settings,
                    title = "General",
                    color = MenuBlue
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().menuGlassCard(16)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickItem(
                            title = "Currency",
                            description = "${selectedCurrency.flag} ${selectedCurrency.code} - ${selectedCurrency.name}",
                            onClick = { showCurrencyDialog = true }
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsClickItem(
                            title = "Security",
                            description = "PIN, biometrics, app lock",
                            onClick = onNavigateToSecurity
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsClickItem(
                            title = "Data & Storage",
                            description = "Manage app data and cache",
                            onClick = { showDataStorageDialog = true }
                        )
                    }
                }
            }

            // Privacy Section
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy",
                    color = MenuEmerald
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().menuGlassCard(16)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsToggleItem(
                            title = "Analytics",
                            description = "Help improve accuracy of insights",
                            checked = false,
                            onCheckedChange = { }
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsClickItem(
                            title = "Privacy Policy",
                            description = "Read our privacy policy",
                            onClick = onNavigateToPrivacyPolicy
                        )
                    }
                }
            }

            // Danger Zone
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Warning,
                    title = "Danger Zone",
                    color = MenuMutedRed
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().menuGlassCard(16)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickItem(
                            title = "Clear All Data",
                            description = "Delete all financial records",
                            onClick = { showClearDataDialog = true },
                            textColor = MenuMutedRed
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsClickItem(
                            title = "Reset App",
                            description = "Reset app to factory settings",
                            onClick = { showResetAppDialog = true },
                            textColor = MenuMutedRed
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DataStorageRow(title: String, description: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MenuSoftWhite, style = MaterialTheme.typography.bodyMedium)
            Text(description, color = MenuSoftWhite.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MenuSoftWhite
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MenuSoftWhite else MenuSoftWhite.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MenuSoftWhite.copy(alpha = if (enabled) 0.6f else 0.3f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MenuEmerald,
                checkedTrackColor = MenuEmerald.copy(alpha = 0.5f),
                uncheckedThumbColor = MenuSoftWhite.copy(alpha = 0.5f),
                uncheckedTrackColor = MenuSoftWhite.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SettingsClickItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    textColor: Color = MenuSoftWhite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MenuSoftWhite.copy(alpha = 0.5f)
        )
    }
}

// ==================== SECURITY SETTINGS SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    securityManager: SecurityManager,
    onBack: () -> Unit,
    onNavigateToBiometricDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager.getInstance(context) }

    var currentSecurityType by remember { mutableStateOf(securityManager.getSecurityType()) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisableBiometricDialog by remember { mutableStateOf(false) }
    var showDisableAutoLockDialog by remember { mutableStateOf(false) }
    var autoLockEnabled by remember { mutableStateOf(preferencesManager.getAutoLockEnabled()) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Detect weak PIN
    fun isWeakPin(pin: String): Boolean {
        if (pin.length != 5) return true
        val repeating = pin.all { it == pin[0] }
        val sequential = "01234567890".contains(pin) || "09876543210".contains(pin)
        return repeating || sequential
    }

    // Disable Biometric Confirmation Dialog
    if (showDisableBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showDisableBiometricDialog = false },
            containerColor = MenuNavyLight,
            icon = {
                Icon(Icons.Default.Fingerprint, null, tint = MenuAmber, modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Disable Biometric?", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to disable biometric authentication?",
                        color = MenuSoftWhite.copy(alpha = 0.8f)
                    )
                    Text(
                        "You'll need to use your PIN to unlock the app.",
                        color = MenuSoftWhite.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newType = when (currentSecurityType) {
                            SecurityManager.SECURITY_PIN_BIOMETRIC -> SecurityManager.SECURITY_PIN
                            SecurityManager.SECURITY_BIOMETRIC -> SecurityManager.SECURITY_NONE
                            else -> currentSecurityType
                        }
                        securityManager.setSecurityType(newType)
                        currentSecurityType = newType
                        showDisableBiometricDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MenuAmber)
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableBiometricDialog = false }) {
                    Text("Cancel", color = MenuSoftWhite)
                }
            }
        )
    }

    // Disable Auto-Lock Confirmation Dialog
    if (showDisableAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showDisableAutoLockDialog = false },
            containerColor = MenuNavyLight,
            icon = {
                Icon(Icons.Default.LockOpen, null, tint = MenuMutedRed, modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Disable Auto-Lock?", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "⚠️ This is not recommended!",
                        color = MenuAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Disabling auto-lock means the app won't require authentication when you return to it.",
                        color = MenuSoftWhite.copy(alpha = 0.8f)
                    )
                    Text(
                        "Your financial data may be accessible to anyone who uses your phone.",
                        color = MenuMutedRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        autoLockEnabled = false
                        preferencesManager.setAutoLockEnabled(false)
                        showDisableAutoLockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MenuMutedRed)
                ) {
                    Text("Disable Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableAutoLockDialog = false }) {
                    Text("Keep Enabled", color = MenuEmerald)
                }
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                currentPin = ""
                newPin = ""
                confirmPin = ""
                pinError = null
            },
            containerColor = MenuNavyLight,
            title = { Text("Change PIN", color = MenuSoftWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (currentSecurityType != SecurityManager.SECURITY_NONE) {
                        OutlinedTextField(
                            value = currentPin,
                            onValueChange = { if (it.length <= 5) currentPin = it.filter { c -> c.isDigit() } },
                            label = { Text("Current PIN", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MenuSoftWhite,
                                unfocusedTextColor = MenuSoftWhite,
                                focusedBorderColor = MenuEmerald,
                                unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 5) newPin = it.filter { c -> c.isDigit() } },
                        label = { Text("New PIN (5 digits)", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MenuSoftWhite,
                            unfocusedTextColor = MenuSoftWhite,
                            focusedBorderColor = MenuEmerald,
                            unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 5) confirmPin = it.filter { c -> c.isDigit() } },
                        label = { Text("Confirm PIN", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MenuSoftWhite,
                            unfocusedTextColor = MenuSoftWhite,
                            focusedBorderColor = MenuEmerald,
                            unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (newPin.length == 5 && isWeakPin(newPin)) {
                        Text(
                            "⚠️ Weak PIN detected. Avoid repeating or sequential numbers.",
                            color = MenuAmber,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    pinError?.let {
                        Text(it, color = MenuMutedRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        currentSecurityType != SecurityManager.SECURITY_NONE && !securityManager.verifyPin(currentPin) -> {
                            pinError = "Current PIN is incorrect"
                        }
                        newPin.length != 5 -> {
                            pinError = "PIN must be 5 digits"
                        }
                        newPin != confirmPin -> {
                            pinError = "PINs do not match"
                        }
                        isWeakPin(newPin) -> {
                            pinError = "Please choose a stronger PIN"
                        }
                        else -> {
                            securityManager.savePin(newPin)
                            if (currentSecurityType == SecurityManager.SECURITY_NONE) {
                                securityManager.setSecurityType(SecurityManager.SECURITY_PIN)
                                currentSecurityType = SecurityManager.SECURITY_PIN
                            }
                            showChangePinDialog = false
                            currentPin = ""
                            newPin = ""
                            confirmPin = ""
                            pinError = null
                        }
                    }
                }) {
                    Text("Save", color = MenuEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePinDialog = false
                    currentPin = ""
                    newPin = ""
                    confirmPin = ""
                    pinError = null
                }) {
                    Text("Cancel", color = MenuSoftWhite.copy(alpha = 0.7f))
                }
            }
        )
    }

    Scaffold(
        containerColor = MenuNavy,
        topBar = {
            TopAppBar(
                title = { Text("Security", color = MenuSoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MenuSoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MenuNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Current Security Status
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    when (currentSecurityType) {
                                        SecurityManager.SECURITY_PIN_BIOMETRIC -> MenuEmerald.copy(alpha = 0.2f)
                                        SecurityManager.SECURITY_PIN, SecurityManager.SECURITY_BIOMETRIC -> MenuBlue.copy(alpha = 0.2f)
                                        else -> MenuMutedRed.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when (currentSecurityType) {
                                    SecurityManager.SECURITY_PIN_BIOMETRIC -> Icons.Default.VerifiedUser
                                    SecurityManager.SECURITY_PIN -> Icons.Default.Pin
                                    SecurityManager.SECURITY_BIOMETRIC -> Icons.Default.Fingerprint
                                    else -> Icons.Default.LockOpen
                                },
                                contentDescription = null,
                                tint = when (currentSecurityType) {
                                    SecurityManager.SECURITY_PIN_BIOMETRIC -> MenuEmerald
                                    SecurityManager.SECURITY_PIN, SecurityManager.SECURITY_BIOMETRIC -> MenuBlue
                                    else -> MenuMutedRed
                                },
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when (currentSecurityType) {
                                SecurityManager.SECURITY_PIN_BIOMETRIC -> "Maximum Protection"
                                SecurityManager.SECURITY_PIN -> "PIN Protected"
                                SecurityManager.SECURITY_BIOMETRIC -> "Biometric Protected"
                                else -> "No Protection"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MenuSoftWhite
                        )

                        Text(
                            text = when (currentSecurityType) {
                                SecurityManager.SECURITY_PIN_BIOMETRIC -> "Your data is secured with PIN + Biometrics"
                                SecurityManager.SECURITY_PIN -> "Your data is secured with a PIN"
                                SecurityManager.SECURITY_BIOMETRIC -> "Your data is secured with biometrics"
                                else -> "Consider enabling security for your data"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MenuSoftWhite.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Security Options
            item {
                Text(
                    text = "Security Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MenuSoftWhite
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().menuGlassCard(16)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickItem(
                            title = if (currentSecurityType == SecurityManager.SECURITY_NONE) "Set PIN" else "Change PIN",
                            description = "Manage your 5-digit security PIN",
                            onClick = { showChangePinDialog = true }
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsToggleItem(
                            title = "Biometric Authentication",
                            description = "Use fingerprint to unlock",
                            checked = currentSecurityType == SecurityManager.SECURITY_BIOMETRIC ||
                                     currentSecurityType == SecurityManager.SECURITY_PIN_BIOMETRIC,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val newType = when (currentSecurityType) {
                                        SecurityManager.SECURITY_PIN -> SecurityManager.SECURITY_PIN_BIOMETRIC
                                        SecurityManager.SECURITY_NONE -> SecurityManager.SECURITY_BIOMETRIC
                                        else -> currentSecurityType
                                    }
                                    securityManager.setSecurityType(newType)
                                    currentSecurityType = newType
                                } else {
                                    // Show confirmation dialog when disabling
                                    showDisableBiometricDialog = true
                                }
                            }
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsToggleItem(
                            title = "Auto-Lock",
                            description = "Lock app when minimized",
                            checked = autoLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    autoLockEnabled = true
                                    preferencesManager.setAutoLockEnabled(true)
                                } else {
                                    // Show confirmation dialog when disabling
                                    showDisableAutoLockDialog = true
                                }
                            }
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsClickItem(
                            title = "Biometric Diagnostics",
                            description = "Check face/fingerprint support",
                            onClick = onNavigateToBiometricDiagnostics
                        )
                    }
                }
            }

            // Security Tips
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(16)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, null, tint = MenuGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Security Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MenuSoftWhite)
                        }
                        Text("• Use PIN + Biometrics for maximum security", style = MaterialTheme.typography.bodySmall, color = MenuSoftWhite.copy(alpha = 0.7f))
                        Text("• Avoid using birthdays or simple sequences", style = MaterialTheme.typography.bodySmall, color = MenuSoftWhite.copy(alpha = 0.7f))
                        Text("• Your data is encrypted and stored locally", style = MaterialTheme.typography.bodySmall, color = MenuSoftWhite.copy(alpha = 0.7f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ==================== HELP & SUPPORT SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var expandedFaq by remember { mutableStateOf<Int?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showAIChatPopup by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    // Contact form state
    var contactName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactMessage by remember { mutableStateOf("") }

    // AI Chat state
    var aiChatInput by remember { mutableStateOf("") }
    var aiChatMessages by remember { mutableStateOf(listOf<Pair<Boolean, String>>()) } // true = user, false = AI

    val faqs = listOf(
        "How do I add an expense?" to "Tap the + button on the dashboard, then select 'Add Expense'. Enter the amount, category, and any notes. You can also add recurring expenses.",
        "How do I set a budget?" to "Navigate to Budget from the dashboard. Tap 'Add Budget' to create category-based budgets. You'll receive alerts when approaching limits.",
        "Is my data secure?" to "Yes! All your data is stored locally on your device with AES-256 encryption. We never upload your financial data to any server.",
        "How do I export my data?" to "Go to Export from the dashboard menu. Choose PDF or Excel format, select what to include, and generate your report.",
        "What is AI Insights?" to "Our AI analyzes your spending patterns to provide personalized recommendations, anomaly detection, and future predictions.",
        "How do I track goals?" to "Create financial goals from the dashboard. Set target amounts, deadlines, and funding methods (saving or loans).",
        "Can I change my PIN?" to "Yes! Go to Settings > Security > Change PIN. You'll need to enter your current PIN first.",
        "How do bill reminders work?" to "Add bills with due dates, and Budgie will notify you 2 days before payment is due."
    )

    // Contact Us Dialog
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            containerColor = MenuNavyLight,
            modifier = Modifier.fillMaxWidth(0.95f),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContactSupport, null, tint = MenuEmerald, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Us", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Your Name", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = MenuSoftWhite.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MenuSoftWhite,
                                unfocusedTextColor = MenuSoftWhite,
                                focusedBorderColor = MenuEmerald,
                                unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email Address", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = MenuSoftWhite.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MenuSoftWhite,
                                unfocusedTextColor = MenuSoftWhite,
                                focusedBorderColor = MenuEmerald,
                                unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone (Optional)", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = MenuSoftWhite.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MenuSoftWhite,
                                unfocusedTextColor = MenuSoftWhite,
                                focusedBorderColor = MenuEmerald,
                                unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = contactMessage,
                            onValueChange = { contactMessage = it },
                            label = { Text("Your Message", color = MenuSoftWhite.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MenuSoftWhite,
                                unfocusedTextColor = MenuSoftWhite,
                                focusedBorderColor = MenuEmerald,
                                unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                            ),
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            "Choose how to reach us:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MenuSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Email Button
                            ContactMethodButton(
                                icon = Icons.Default.Email,
                                label = "Email",
                                color = MenuBlue,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:ld@aplus.anonaddy.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Budgie Support: $contactName")
                                        putExtra(Intent.EXTRA_TEXT, "Name: $contactName\nEmail: $contactEmail\nPhone: $contactPhone\n\nMessage:\n$contactMessage")
                                    }
                                    context.startActivity(intent)
                                    showContactDialog = false
                                }
                            )
                            // WhatsApp Button
                            ContactMethodButton(
                                icon = Icons.Default.Chat,
                                label = "WhatsApp",
                                color = Color(0xFF25D366),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val message = "Hi Budgie Support!\n\nName: $contactName\nEmail: $contactEmail\n\n$contactMessage"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/254726781724?text=${Uri.encode(message)}")
                                    }
                                    context.startActivity(intent)
                                    showContactDialog = false
                                }
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // SMS Button
                            ContactMethodButton(
                                icon = Icons.Default.Sms,
                                label = "SMS",
                                color = MenuAmber,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val message = "Budgie Support: $contactName - $contactMessage"
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:+254726781724")
                                        putExtra("sms_body", message)
                                    }
                                    context.startActivity(intent)
                                    showContactDialog = false
                                }
                            )
                            // Call Button
                            ContactMethodButton(
                                icon = Icons.Default.Call,
                                label = "Call Now",
                                color = MenuEmerald,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:+254726781724")
                                    }
                                    context.startActivity(intent)
                                    showContactDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("Cancel", color = MenuSoftWhite.copy(alpha = 0.7f))
                }
            }
        )
    }

    // AI Chat Popup
    if (showAIChatPopup) {
        AlertDialog(
            onDismissRequest = { showAIChatPopup = false },
            containerColor = MenuNavyLight,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 500.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MenuPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartToy, null, tint = MenuPurple, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Budgie Assistant", color = MenuSoftWhite, fontWeight = FontWeight.Bold)
                        Text("Ask me anything about Budgie", style = MaterialTheme.typography.labelSmall, color = MenuSoftWhite.copy(alpha = 0.6f))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chat Messages Area
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MenuNavy.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (aiChatMessages.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.QuestionAnswer, null, tint = MenuSoftWhite.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Ask me anything about using Budgie!",
                                        color = MenuSoftWhite.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Try: \"How do I add an expense?\"",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MenuEmerald.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        items(aiChatMessages) { (isUser, message) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .clip(RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        ))
                                        .background(if (isUser) MenuEmerald else MenuNavyLight)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        message,
                                        color = if (isUser) MenuNavy else MenuSoftWhite,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Input Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiChatInput,
                            onValueChange = { aiChatInput = it },
                            placeholder = { Text("Type your question...", color = MenuSoftWhite.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MenuSoftWhite,
                                unfocusedTextColor = MenuSoftWhite,
                                focusedBorderColor = MenuEmerald,
                                unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        IconButton(
                            onClick = {
                                if (aiChatInput.isNotBlank()) {
                                    val userMessage = aiChatInput
                                    aiChatMessages = aiChatMessages + (true to userMessage)
                                    aiChatInput = ""

                                    // Generate AI response
                                    val response = generateHelpResponse(userMessage.lowercase())
                                    aiChatMessages = aiChatMessages + (false to response)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MenuEmerald)
                        ) {
                            Icon(Icons.Default.Send, null, tint = MenuNavy)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAIChatPopup = false
                    aiChatMessages = emptyList()
                }) {
                    Text("Close", color = MenuSoftWhite.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Rate Dialog
    if (showRateDialog) {
        var rating by remember { mutableStateOf(0) }
        var feedback by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            containerColor = MenuNavyLight,
            icon = {
                Icon(Icons.Default.Star, null, tint = MenuGold, modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Rate Budgie", color = MenuSoftWhite, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "How would you rate your experience?",
                        color = MenuSoftWhite.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    // Star Rating
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    if (star <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                                    null,
                                    tint = if (star <= rating) MenuGold else MenuSoftWhite.copy(alpha = 0.3f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    if (rating > 0) {
                        Text(
                            when (rating) {
                                1 -> "😔 We'll do better!"
                                2 -> "😕 Room for improvement"
                                3 -> "😊 Good experience"
                                4 -> "😃 Great app!"
                                5 -> "🤩 Absolutely love it!"
                                else -> ""
                            },
                            color = MenuSoftWhite.copy(alpha = 0.7f)
                        )
                    }

                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        label = { Text("Tell us more (optional)", color = MenuSoftWhite.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MenuSoftWhite,
                            unfocusedTextColor = MenuSoftWhite,
                            focusedBorderColor = MenuEmerald,
                            unfocusedBorderColor = MenuSoftWhite.copy(alpha = 0.3f)
                        ),
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // In production, send rating to analytics
                        showRateDialog = false
                    },
                    enabled = rating > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MenuEmerald)
                ) {
                    Text("Submit Rating")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text("Maybe Later", color = MenuSoftWhite.copy(alpha = 0.7f))
                }
            }
        )
    }

    Scaffold(
        containerColor = MenuNavy,
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", color = MenuSoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MenuSoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MenuNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Quick Help Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MenuEmerald,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "How can we help?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MenuSoftWhite
                        )
                        Text(
                            "Find answers to common questions below",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MenuSoftWhite.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Contact Options
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactCard(
                        icon = Icons.Default.ContactSupport,
                        title = "Contact Us",
                        color = MenuBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { showContactDialog = true }
                    )
                    ContactCard(
                        icon = Icons.Default.SmartToy,
                        title = "AI Chat",
                        color = MenuPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { showAIChatPopup = true }
                    )
                }
            }

            // FAQ Section
            item {
                Text(
                    "Frequently Asked Questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MenuSoftWhite
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().menuGlassCard(16)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        faqs.forEachIndexed { index, (question, answer) ->
                            FaqItem(
                                question = question,
                                answer = answer,
                                expanded = expandedFaq == index,
                                onClick = { expandedFaq = if (expandedFaq == index) null else index }
                            )
                            if (index < faqs.lastIndex) {
                                HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            // Feedback
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(16)
                        .clickable { showRateDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MenuGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, null, tint = MenuGold, modifier = Modifier.size(24.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rate Budgie", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MenuSoftWhite)
                            Text("Share your feedback", style = MaterialTheme.typography.bodySmall, color = MenuSoftWhite.copy(alpha = 0.6f))
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MenuSoftWhite.copy(alpha = 0.5f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ContactCard(
    icon: ImageVector,
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .menuGlassCard(14)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MenuSoftWhite)
        }
    }
}

@Composable
private fun ContactMethodButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

// AI Chat Help Response Generator
private fun generateHelpResponse(query: String): String {
    return when {
        query.contains("expense") || query.contains("add") && query.contains("spend") ->
            "To add an expense:\n\n1. Tap the + button on the dashboard\n2. Select 'Add Expense'\n3. Enter the amount and select a category\n4. Add any notes or receipt\n5. Tap 'Save'\n\nYou can also mark expenses as recurring for regular payments."

        query.contains("budget") ->
            "To set a budget:\n\n1. Go to Budget from the dashboard\n2. Tap 'Add Budget'\n3. Select a category (e.g., Food, Transport)\n4. Set your monthly limit\n5. Save your budget\n\nBudgie will alert you when you're approaching your limit!"

        query.contains("export") || query.contains("pdf") || query.contains("excel") ->
            "To export your data:\n\n1. Go to Export from the menu\n2. Choose PDF or Excel format\n3. Select what to include (expenses, income, etc.)\n4. Tap 'Generate Report'\n5. Your file will be saved and you can share it\n\nReports are protected with your PIN."

        query.contains("goal") || query.contains("save") && query.contains("money") ->
            "To create a financial goal:\n\n1. Tap 'My Budgie Goals' on dashboard\n2. Select goal type (Short/Medium/Long term)\n3. Enter target amount and deadline\n4. Choose funding method (Savings/Loan/Both)\n5. Track your progress on the dashboard!"

        query.contains("loan") ->
            "To track a loan:\n\n1. Go to 'My Loans' from dashboard\n2. Tap 'Add New Loan'\n3. Enter loan details (amount, interest, duration)\n4. Budgie calculates your monthly payments\n5. Track repayments and see your progress\n\nWe'll remind you before payments are due!"

        query.contains("bill") || query.contains("reminder") ->
            "To add a bill:\n\n1. Go to Bills from the menu\n2. Tap 'Add Bill'\n3. Enter bill name and amount\n4. Set the due date\n5. Enable reminders\n\nBudgie will notify you 2 days before each bill is due!"

        query.contains("secure") || query.contains("pin") || query.contains("password") || query.contains("biometric") ->
            "To manage security:\n\n1. Go to Settings > Security\n2. Set up a 5-digit PIN\n3. Enable biometric authentication\n4. Turn on Auto-Lock for extra safety\n\nYour data is encrypted and stored only on your device."

        query.contains("insight") || query.contains("ai") || query.contains("analysis") ->
            "Budgie's AI analyzes your spending to provide:\n\n• Spending pattern detection\n• Anomaly alerts for unusual expenses\n• Budget recommendations\n• Future spending predictions\n• Category-specific insights\n\nCheck the AI Insights section on your dashboard!"

        query.contains("shopping") || query.contains("list") ->
            "To create a shopping list:\n\n1. Tap the + button and select 'Shopping List'\n2. Add items with quantities and estimated prices\n3. Set priority levels (1-5)\n4. Our AI will suggest optimizations\n5. Total is added to your budget automatically!"

        query.contains("currency") ->
            "To change currency:\n\n1. Go to Settings\n2. Tap on 'Currency'\n3. Select your preferred currency\n\nSupported: USD, EUR, GBP, KES, INR, JPY, NGN, ZAR"

        query.contains("hello") || query.contains("hi") || query.contains("hey") ->
            "Hello! 👋 I'm your Budgie assistant. I can help you with:\n\n• Adding expenses & income\n• Setting budgets\n• Creating financial goals\n• Tracking loans\n• Exporting reports\n• Understanding AI insights\n\nWhat would you like to know?"

        query.contains("thank") ->
            "You're welcome! 😊 Is there anything else I can help you with? Feel free to ask about any Budgie feature!"

        else ->
            "I'm here to help! You can ask me about:\n\n• How to add expenses or income\n• Setting up budgets\n• Creating financial goals\n• Tracking loans\n• Exporting your data\n• Security settings\n• AI insights\n\nTry asking something like 'How do I add an expense?'"
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MenuSoftWhite,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = MenuSoftWhite.copy(alpha = 0.5f)
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MenuSoftWhite.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )
        }
    }
}

// ==================== ABOUT SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTerms: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MenuNavy,
        topBar = {
            TopAppBar(
                title = { Text("About Budgie", color = MenuSoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MenuSoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MenuNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // App Logo - Using Budgie Logo
            item {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MenuEmerald.copy(alpha = 0.3f),
                                    MenuNavyLight.copy(alpha = 0.8f),
                                    MenuNavy
                                )
                            )
                        )
                        .border(2.dp, MenuEmerald.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Try to load the app icon/logo
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.budgie.R.mipmap.ic_launcher),
                        contentDescription = "Budgie Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }

            item {
                Text(
                    "Budgie",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MenuSoftWhite
                )
            }

            item {
                Text(
                    "Your Financial Bestie",
                    style = MaterialTheme.typography.titleMedium,
                    color = MenuEmerald
                )
            }

            item {
                Text(
                    "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MenuSoftWhite.copy(alpha = 0.5f)
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Description Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(16)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Budgie is a comprehensive personal finance management app powered by on-device AI. Track expenses, manage budgets, set goals, and get personalized insights—all while keeping your data 100% private.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MenuSoftWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Features
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(16)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureItem(Icons.Default.Savings, "Expense & Income Tracking", MenuEmerald)
                        FeatureItem(Icons.Default.PieChart, "Smart Budgeting", MenuBlue)
                        FeatureItem(Icons.Default.Flag, "Financial Goals", MenuAmber)
                        FeatureItem(Icons.Default.Psychology, "AI-Powered Insights", MenuPurple)
                        FeatureItem(Icons.Default.Shield, "Bank-Grade Security", MenuGold)
                        FeatureItem(Icons.Default.CloudOff, "100% Offline & Private", MenuEmerald)
                    }
                }
            }

            // Legal Links
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().menuGlassCard(16)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickItem(
                            title = "Privacy Policy",
                            description = "How we protect your data",
                            onClick = onNavigateToPrivacyPolicy
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsClickItem(
                            title = "Terms of Service",
                            description = "Terms and conditions",
                            onClick = onNavigateToTerms
                        )
                        HorizontalDivider(color = MenuSoftWhite.copy(alpha = 0.1f))
                        SettingsClickItem(
                            title = "Open Source Licenses",
                            description = "Third-party libraries",
                            onClick = { }
                        )
                    }
                }
            }

            // Credits
            item {
                Text(
                    "Made with ❤️ in Kenya",
                    style = MaterialTheme.typography.bodySmall,
                    color = MenuSoftWhite.copy(alpha = 0.5f)
                )
            }

            item {
                Text(
                    "© ${Calendar.getInstance().get(Calendar.YEAR)} Budgie. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MenuSoftWhite.copy(alpha = 0.4f)
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MenuSoftWhite
        )
    }
}

// ==================== PRIVACY POLICY SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MenuNavy,
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", color = MenuSoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MenuSoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MenuNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(16)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = MenuEmerald, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Your Privacy Matters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MenuSoftWhite)
                        }
                        Text(
                            "Last updated: December 2024",
                            style = MaterialTheme.typography.labelMedium,
                            color = MenuSoftWhite.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                PolicySection(
                    title = "1. Data Collection",
                    content = """
                        Budgie is designed with privacy-first principles. We collect and process only the data you explicitly provide:
                        
                        • Personal Information: Name and date of birth (for personalization and birthday greetings)
                        • Financial Data: Income, expenses, bills, budgets, goals, and loans you manually enter
                        • App Usage: Local preferences and settings
                        
                        We DO NOT collect:
                        • Location data
                        • Contact information
                        • Device identifiers for tracking
                        • Any data from other apps
                    """.trimIndent()
                )
            }

            item {
                PolicySection(
                    title = "2. Data Storage",
                    content = """
                        All your data is stored locally on your device using encrypted databases. Your financial information never leaves your device unless you explicitly choose to export it.
                        
                        • Local Storage: SQLite database with Room persistence
                        • Encryption: Industry-standard AES-256 encryption for sensitive data
                        • No Cloud Sync: Your data is not uploaded to any server
                        • Your Control: You can export or delete your data at any time
                    """.trimIndent()
                )
            }

            item {
                PolicySection(
                    title = "3. AI & Analytics",
                    content = """
                        Our AI features run entirely on your device:
                        
                        • On-Device Processing: All AI analysis happens locally
                        • No Data Transmission: Your financial patterns are analyzed without sending data anywhere
                        • Behavioral Insights: Generated locally from your transaction history
                        • Predictions: Made using on-device machine learning models
                    """.trimIndent()
                )
            }

            item {
                PolicySection(
                    title = "4. Data Sharing",
                    content = """
                        We do not share your data with any third parties. Period.
                        
                        • No advertising partners
                        • No analytics companies
                        • No data brokers
                        • No government agencies (unless legally required)
                        
                        The only data that leaves your device is when YOU choose to export reports (PDF/Excel) for your own use.
                    """.trimIndent()
                )
            }

            item {
                PolicySection(
                    title = "5. Security",
                    content = """
                        We implement multiple layers of security:
                        
                        • App Lock: PIN and/or biometric authentication
                        • Encrypted Storage: All sensitive data is encrypted at rest
                        • No Network Access: Financial data never transmitted
                        • Auto-Lock: App locks when minimized
                    """.trimIndent()
                )
            }

            item {
                PolicySection(
                    title = "6. Your Rights",
                    content = """
                        You have complete control over your data:
                        
                        • Access: View all your data within the app
                        • Export: Download your data in PDF or Excel format
                        • Delete: Clear all data from Settings
                        • Portability: Export and use your data anywhere
                    """.trimIndent()
                )
            }

            item {
                PolicySection(
                    title = "7. Contact Us",
                    content = """
                        For privacy-related questions or concerns:
                        
                        Email: privacy@budgie.app
                        
                        We're committed to protecting your privacy and will respond to inquiries within 48 hours.
                    """.trimIndent()
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .menuGlassCard(14)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MenuEmerald
            )
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                color = MenuSoftWhite.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
        }
    }
}

// ==================== TERMS OF SERVICE SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MenuNavy,
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service", color = MenuSoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MenuSoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MenuNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuGlassCard(16)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = MenuBlue, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Terms of Service", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MenuSoftWhite)
                        }
                        Text(
                            "Last updated: December 2024",
                            style = MaterialTheme.typography.labelMedium,
                            color = MenuSoftWhite.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                TermsSection(
                    title = "1. Acceptance of Terms",
                    content = """
                        By downloading, installing, or using Budgie ("the App"), you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the App.
                        
                        These terms constitute a legally binding agreement between you and Budgie.
                    """.trimIndent()
                )
            }

            item {
                TermsSection(
                    title = "2. Description of Service",
                    content = """
                        Budgie is a personal finance management application that helps you:
                        
                        • Track income and expenses
                        • Create and manage budgets
                        • Set financial goals
                        • Receive AI-powered insights
                        • Export financial reports
                        
                        The App is provided "as is" for personal, non-commercial use only.
                    """.trimIndent()
                )
            }

            item {
                TermsSection(
                    title = "3. User Responsibilities",
                    content = """
                        You agree to:
                        
                        • Provide accurate information when using the App
                        • Keep your PIN and security credentials confidential
                        • Use the App only for lawful purposes
                        • Not attempt to reverse engineer or modify the App
                        • Be at least 18 years of age to use the App
                        
                        You are solely responsible for the accuracy of data you enter.
                    """.trimIndent()
                )
            }

            item {
                TermsSection(
                    title = "4. Financial Disclaimer",
                    content = """
                        IMPORTANT: Budgie is NOT a financial advisor.
                        
                        • AI insights are for informational purposes only
                        • We do not provide investment, legal, or tax advice
                        • Always consult qualified professionals for financial decisions
                        • Past spending patterns do not guarantee future results
                        • We are not responsible for financial decisions made based on App insights
                    """.trimIndent()
                )
            }

            item {
                TermsSection(
                    title = "5. Data Ownership",
                    content = """
                        You retain full ownership of your data:
                        
                        • All financial data you enter belongs to you
                        • We do not claim any rights to your data
                        • You can export or delete your data at any time
                        • Data is stored locally on your device
                        • We cannot access or recover your data if deleted
                    """.trimIndent()
                )
            }

            item {
                TermsSection(
                    title = "6. Limitation of Liability",
                    content = """
                        To the maximum extent permitted by law:
                        
                        • Budgie is provided without warranty of any kind
                        • We are not liable for any damages arising from App use
                        • We are not responsible for data loss due to device issues
                        • We do not guarantee the App will be error-free
                        • Our total liability shall not exceed the amount paid for the App
                    """.trimIndent()
                )
            }

            item {
                TermsSection(
                    title = "7. Modifications",
                    content = """
                        We reserve the right to:
                        
                        • Update these terms at any time
                        • Modify or discontinue the App
                        • Add or remove features
                        
                        Continued use after changes constitutes acceptance of new terms.
                    """.trimIndent()
                )
            }

            item {
                TermsSection(
                    title = "8. Governing Law",
                    content = """
                        These terms are governed by and construed in accordance with applicable laws. Any disputes shall be resolved through arbitration.
                        
                        For questions about these terms:
                        Email: legal@budgie.app
                    """.trimIndent()
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    content: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .menuGlassCard(14)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MenuBlue
            )
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                color = MenuSoftWhite.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
        }
    }
}
