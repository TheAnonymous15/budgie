package com.example.budgie.ui.screens

import android.Manifest
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.budgie.data.model.*
import com.example.budgie.data.repository.NotificationRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Theme colors
private val NotifNavy = Color(0xFF0A1628)
private val NotifNavyLight = Color(0xFF1A2A44)
private val NotifEmerald = Color(0xFF0FAE96)
private val NotifSoftWhite = Color(0xFFF5F5F5)
private val NotifMutedRed = Color(0xFFE57373)
private val NotifAmber = Color(0xFFFFB74D)
private val NotifBlue = Color(0xFF5C9CE5)
private val NotifPurple = Color(0xFF9575CD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { NotificationRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    val allNotifications by repository.getAllActiveNotifications().collectAsState(initial = emptyList())
    val unreadCount by repository.getUnreadCount().collectAsState(initial = 0)
    val categoryCountsFlow by repository.getUnreadCountByAllCategories().collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf<NotificationCategory?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Notification permission state
    val hasNotificationPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val filteredNotifications = remember(allNotifications, selectedCategory) {
        if (selectedCategory == null) {
            allNotifications
        } else {
            allNotifications.filter { it.category == selectedCategory }
        }
    }

    val groupedNotifications = remember(filteredNotifications) {
        groupNotificationsByDate(filteredNotifications)
    }

    // Clear all dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = NotifNavyLight,
            title = {
                Text("Clear All Notifications", color = NotifSoftWhite)
            },
            text = {
                Text(
                    "Are you sure you want to mark all notifications as read?",
                    color = NotifSoftWhite.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.markAllAsRead()
                        }
                        showClearAllDialog = false
                    }
                ) {
                    Text("Mark All Read", color = NotifEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = NotifSoftWhite.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Notification preferences for ringtone selection
    val notificationPrefs = remember { com.example.budgie.notifications.NotificationPreferences.getInstance(context) }

    // State for ringtone picker dialog
    var showRingtonePicker by remember { mutableStateOf(false) }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                scope.launch {
                    repository.deleteAll()
                }
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // Ringtone Picker Dialog
    if (showRingtonePicker) {
        com.example.budgie.ui.components.RingtonePickerDialog(
            currentRingtoneUri = notificationPrefs.customSoundUri,
            onRingtoneSelected = { uri ->
                notificationPrefs.customSoundUri = uri
                notificationPrefs.useSystemSound = (uri == null)
            },
            onDismiss = { showRingtonePicker = false }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        NotificationSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onOpenRingtonePicker = {
                showRingtonePicker = true
            }
        )
    }

    Scaffold(
        containerColor = NotifNavy,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Notifications",
                            color = NotifSoftWhite,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Text(
                                "$unreadCount unread",
                                color = NotifEmerald,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NotifSoftWhite
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Mark all read",
                                tint = NotifEmerald
                            )
                        }
                    }
                    // Settings icon
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Notification Settings",
                            tint = NotifSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                    // Delete icon with confirmation
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Delete all notifications",
                            tint = NotifSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NotifNavy
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Filter Chips
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                categoryCounts = categoryCountsFlow.associate { it.category to it.count },
                onCategorySelected = { selectedCategory = it }
            )

            // Permission Banner (only on Android 13+)
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            // Open app notification settings
                            val intent = Intent().apply {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = NotifAmber.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = NotifAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Push notifications disabled",
                                color = NotifSoftWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Tap to enable push notifications for bills, insights & more",
                                color = NotifSoftWhite.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NotifAmber
                        )
                    }
                }
            }

            if (filteredNotifications.isEmpty()) {
                // Empty State
                EmptyNotificationsState(selectedCategory)
            } else {
                // Notifications List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedNotifications.forEach { group ->
                        item {
                            DateHeader(group.date)
                        }

                        items(
                            items = group.notifications,
                            key = { it.id }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkAsRead = {
                                    scope.launch {
                                        repository.markAsRead(notification.id)
                                    }
                                },
                                onDismiss = {
                                    scope.launch {
                                        repository.dismissNotification(notification.id)
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        repository.deleteNotification(notification.id)
                                    }
                                },
                                onNavigate = { route ->
                                    scope.launch {
                                        repository.markAsRead(notification.id)
                                    }
                                    onNavigateToRoute(route)
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
private fun CategoryFilterRow(
    selectedCategory: NotificationCategory?,
    categoryCounts: Map<NotificationCategory, Int>,
    onCategorySelected: (NotificationCategory?) -> Unit
) {
    // Sort categories: those with notifications first (highest to lowest), then others
    val sortedCategories = remember(categoryCounts) {
        NotificationCategory.entries.sortedByDescending { categoryCounts[it] ?: 0 }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All filter
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                leadingIcon = {
                    Icon(
                        Icons.Default.AllInbox,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = NotifNavyLight,
                    selectedContainerColor = NotifEmerald.copy(alpha = 0.2f),
                    labelColor = NotifSoftWhite,
                    selectedLabelColor = NotifEmerald
                )
            )
        }

        // Category filters - sorted by count (highest first)
        sortedCategories.forEach { category ->
            val count = categoryCounts[category] ?: 0
            item {
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.displayName)
                            if (count > 0) {
                                Badge(
                                    containerColor = NotifMutedRed,
                                    contentColor = Color.White
                                ) {
                                    Text(count.toString(), fontSize = 10.sp)
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            getCategoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = NotifNavyLight,
                        selectedContainerColor = getCategoryColor(category).copy(alpha = 0.2f),
                        labelColor = NotifSoftWhite,
                        selectedLabelColor = getCategoryColor(category)
                    )
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    Text(
        text = date,
        color = NotifSoftWhite.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationCard(
    notification: AppNotification,
    onMarkAsRead: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val isUnread = notification.status == NotificationStatus.UNREAD
    val categoryColor = getCategoryColor(notification.category)

    var expanded by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMarkAsRead()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> NotifEmerald
                SwipeToDismissBoxValue.EndToStart -> NotifMutedRed
                else -> Color.Transparent
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = Color.White)
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isUnread) onMarkAsRead()
                        // Navigate if actionRoute is available
                        notification.actionRoute?.let { route ->
                            onNavigate(route)
                        } ?: run {
                            expanded = !expanded
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnread)
                        NotifNavyLight
                    else
                        NotifNavyLight.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getCategoryIcon(notification.category),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = notification.title,
                                color = NotifSoftWhite,
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Unread indicator
                            if (isUnread) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NotifEmerald)
                                )
                            }
                        }

                        Text(
                            text = notification.message,
                            color = NotifSoftWhite.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category badge
                            Text(
                                text = notification.category.displayName,
                                color = categoryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Timestamp
                            Text(
                                text = formatTimestamp(notification.createdAt),
                                color = NotifSoftWhite.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }

                        // Priority indicator for high/urgent
                        if (notification.priority == AppNotificationPriority.HIGH ||
                            notification.priority == AppNotificationPriority.URGENT) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (notification.priority == AppNotificationPriority.URGENT)
                                        Icons.Default.Warning
                                    else
                                        Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = if (notification.priority == AppNotificationPriority.URGENT)
                                        NotifMutedRed
                                    else
                                        NotifAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = notification.priority.displayName,
                                    color = if (notification.priority == AppNotificationPriority.URGENT)
                                        NotifMutedRed
                                    else
                                        NotifAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun EmptyNotificationsState(selectedCategory: NotificationCategory?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .alpha(0.5f),
                tint = NotifSoftWhite
            )

            Text(
                text = if (selectedCategory == null)
                    "No Notifications"
                else
                    "No ${selectedCategory.displayName} Notifications",
                color = NotifSoftWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = if (selectedCategory == null)
                    "You're all caught up! Check back later for updates."
                else
                    "No notifications in this category yet.",
                color = NotifSoftWhite.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

// Helper Functions

private fun getCategoryIcon(category: NotificationCategory): ImageVector {
    return when (category) {
        NotificationCategory.BILL -> Icons.Default.Receipt
        NotificationCategory.BUDGET -> Icons.Default.AccountBalanceWallet
        NotificationCategory.GOAL -> Icons.Default.Flag
        NotificationCategory.LOAN -> Icons.Default.CreditCard
        NotificationCategory.EXPENSE -> Icons.Default.ShoppingCart
        NotificationCategory.INCOME -> Icons.Default.AttachMoney
        NotificationCategory.SECURITY -> Icons.Default.Security
        NotificationCategory.SYSTEM -> Icons.Default.Settings
        NotificationCategory.SHOPPING -> Icons.Default.ShoppingBag
        NotificationCategory.INVESTMENT -> Icons.Default.TrendingUp
        NotificationCategory.INSIGHT -> Icons.Default.Lightbulb
    }
}

private fun getCategoryColor(category: NotificationCategory): Color {
    return when (category) {
        NotificationCategory.BILL -> Color(0xFFFFB74D)
        NotificationCategory.BUDGET -> Color(0xFF0FAE96)
        NotificationCategory.GOAL -> Color(0xFF4CAF50)
        NotificationCategory.LOAN -> Color(0xFF5C9CE5)
        NotificationCategory.EXPENSE -> Color(0xFFE57373)
        NotificationCategory.INCOME -> Color(0xFF81C784)
        NotificationCategory.SECURITY -> Color(0xFF7986CB)
        NotificationCategory.SYSTEM -> Color(0xFF9575CD)
        NotificationCategory.SHOPPING -> Color(0xFFFF7043)
        NotificationCategory.INVESTMENT -> Color(0xFF4DD0E1)
        NotificationCategory.INSIGHT -> Color(0xFFFFD54F)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun groupNotificationsByDate(notifications: List<AppNotification>): List<NotificationGroup> {
    val now = System.currentTimeMillis()
    val todayStart = getStartOfDay(now)
    val yesterdayStart = todayStart - 86_400_000
    val weekStart = todayStart - (7 * 86_400_000)

    val groups = mutableListOf<NotificationGroup>()

    val today = notifications.filter { it.createdAt >= todayStart }
    if (today.isNotEmpty()) {
        groups.add(NotificationGroup("Today", today))
    }

    val yesterday = notifications.filter { it.createdAt >= yesterdayStart && it.createdAt < todayStart }
    if (yesterday.isNotEmpty()) {
        groups.add(NotificationGroup("Yesterday", yesterday))
    }

    val thisWeek = notifications.filter { it.createdAt >= weekStart && it.createdAt < yesterdayStart }
    if (thisWeek.isNotEmpty()) {
        groups.add(NotificationGroup("This Week", thisWeek))
    }

    val older = notifications.filter { it.createdAt < weekStart }
    if (older.isNotEmpty()) {
        groups.add(NotificationGroup("Older", older))
    }

    return groups
}

private fun getStartOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

// ═══════════════════════════════════════════════════════════════════════════════
// DELETE CONFIRMATION DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NotifNavyLight,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NotifMutedRed.copy(alpha = 0.3f),
                                NotifMutedRed.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = NotifMutedRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Delete All Notifications",
                color = NotifSoftWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Are you sure you want to permanently delete all notifications?",
                    color = NotifSoftWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                // Warning card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NotifMutedRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = NotifMutedRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "This action cannot be undone",
                            color = NotifMutedRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NotifMutedRed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Delete All", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NotifSoftWhite
                ),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                    brush = Brush.linearGradient(
                        colors = listOf(NotifSoftWhite.copy(alpha = 0.3f), NotifSoftWhite.copy(alpha = 0.3f))
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// NOTIFICATION SETTINGS DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NotificationSettingsDialog(
    onDismiss: () -> Unit,
    onOpenRingtonePicker: () -> Unit
) {
    val context = LocalContext.current
    val notificationPrefs = remember { com.example.budgie.notifications.NotificationPreferences.getInstance(context) }

    // Master controls - load from preferences
    var allNotificationsEnabled by remember { mutableStateOf(notificationPrefs.allNotificationsEnabled) }

    // Settings state - load from preferences
    var billNotifications by remember { mutableStateOf(notificationPrefs.billsNotificationsEnabled) }
    var budgetAlerts by remember { mutableStateOf(notificationPrefs.budgetAlertsEnabled) }
    var goalReminders by remember { mutableStateOf(notificationPrefs.goalsRemindersEnabled) }
    var loanReminders by remember { mutableStateOf(notificationPrefs.loansRemindersEnabled) }
    var expenseAlerts by remember { mutableStateOf(notificationPrefs.expenseAlertsEnabled) }
    var incomeNotifications by remember { mutableStateOf(notificationPrefs.incomeNotificationsEnabled) }
    var securityAlerts by remember { mutableStateOf(notificationPrefs.securityAlertsEnabled) }
    var systemNotifications by remember { mutableStateOf(notificationPrefs.systemNotificationsEnabled) }
    var insightNotifications by remember { mutableStateOf(notificationPrefs.insightsNotificationsEnabled) }
    var quietHoursEnabled by remember { mutableStateOf(notificationPrefs.quietHoursEnabled) }

    // Sound mode - use enum
    var soundMode by remember { mutableStateOf(notificationPrefs.soundMode) }

    // Sound options
    var useSystemSound by remember { mutableStateOf(notificationPrefs.useSystemSound) }
    var showSoundOptions by remember { mutableStateOf(false) }

    // Quiet hours time
    var quietStartHour by remember { mutableStateOf(notificationPrefs.quietStartHour) }
    var quietStartMinute by remember { mutableStateOf(notificationPrefs.quietStartMinute) }
    var quietEndHour by remember { mutableStateOf(notificationPrefs.quietEndHour) }
    var quietEndMinute by remember { mutableStateOf(notificationPrefs.quietEndMinute) }
    var showQuietHoursTimePicker by remember { mutableStateOf(false) }
    var editingStartTime by remember { mutableStateOf(true) }

    // Auto-save: Save settings whenever any value changes
    LaunchedEffect(
        allNotificationsEnabled,
        billNotifications,
        budgetAlerts,
        goalReminders,
        loanReminders,
        expenseAlerts,
        incomeNotifications,
        securityAlerts,
        systemNotifications,
        insightNotifications,
        soundMode,
        quietHoursEnabled,
        useSystemSound,
        quietStartHour,
        quietStartMinute,
        quietEndHour,
        quietEndMinute
    ) {
        // Auto-save all settings
        notificationPrefs.allNotificationsEnabled = allNotificationsEnabled
        notificationPrefs.billsNotificationsEnabled = billNotifications
        notificationPrefs.budgetAlertsEnabled = budgetAlerts
        notificationPrefs.goalsRemindersEnabled = goalReminders
        notificationPrefs.loansRemindersEnabled = loanReminders
        notificationPrefs.expenseAlertsEnabled = expenseAlerts
        notificationPrefs.incomeNotificationsEnabled = incomeNotifications
        notificationPrefs.securityAlertsEnabled = securityAlerts
        notificationPrefs.systemNotificationsEnabled = systemNotifications
        notificationPrefs.insightsNotificationsEnabled = insightNotifications
        notificationPrefs.soundMode = soundMode
        notificationPrefs.quietHoursEnabled = quietHoursEnabled
        notificationPrefs.useSystemSound = useSystemSound
        notificationPrefs.quietStartHour = quietStartHour
        notificationPrefs.quietStartMinute = quietStartMinute
        notificationPrefs.quietEndHour = quietEndHour
        notificationPrefs.quietEndMinute = quietEndMinute
    }

    // Enable/disable all helper functions
    fun enableAllNotifications() {
        allNotificationsEnabled = true
        billNotifications = true
        budgetAlerts = true
        goalReminders = true
        loanReminders = true
        expenseAlerts = true
        incomeNotifications = true
        securityAlerts = true
        systemNotifications = true
        insightNotifications = true
    }

    fun disableAllNotifications() {
        allNotificationsEnabled = false
        billNotifications = false
        budgetAlerts = false
        goalReminders = false
        loanReminders = false
        expenseAlerts = false
        incomeNotifications = false
        securityAlerts = false
        systemNotifications = false
        insightNotifications = false
    }

    // Format time helper
    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        ),
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 650.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NotifNavy
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        NotifEmerald.copy(alpha = 0.3f),
                                        NotifBlue.copy(alpha = 0.2f),
                                        NotifPurple.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Animated settings icon
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(NotifEmerald, NotifBlue)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            "Notification Settings",
                                            color = NotifSoftWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            "Customize your alerts",
                                            color = NotifSoftWhite.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(NotifSoftWhite.copy(alpha = 0.1f))
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = NotifSoftWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Settings Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Master Controls Section
                        item {
                            SettingsSectionHeader(
                                title = "Master Controls",
                                icon = Icons.Default.ToggleOn
                            )
                        }

                        // Enable All Notifications
                        item {
                            MasterControlButton(
                                icon = Icons.Default.NotificationsActive,
                                iconColor = NotifEmerald,
                                title = "Enable All Notifications",
                                subtitle = "Turn on all notification types",
                                isEnabled = allNotificationsEnabled,
                                onClick = { enableAllNotifications() }
                            )
                        }

                        // Disable All Notifications
                        item {
                            MasterControlButton(
                                icon = Icons.Default.NotificationsOff,
                                iconColor = NotifMutedRed,
                                title = "Disable All Notifications",
                                subtitle = "Mute all notifications",
                                isEnabled = !allNotificationsEnabled,
                                onClick = { disableAllNotifications() }
                            )
                        }

                        // Category Notifications Section
                        item {
                            Spacer(Modifier.height(8.dp))
                            SettingsSectionHeader(
                                title = "Category Notifications",
                                icon = Icons.Default.Category
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.Receipt,
                                iconColor = Color(0xFFFFB74D),
                                title = "Bill Reminders",
                                subtitle = "Due dates & payment alerts",
                                checked = billNotifications,
                                onCheckedChange = { billNotifications = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.AccountBalanceWallet,
                                iconColor = NotifEmerald,
                                title = "Budget Alerts",
                                subtitle = "Spending limits & warnings",
                                checked = budgetAlerts,
                                onCheckedChange = { budgetAlerts = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.Flag,
                                iconColor = Color(0xFF4CAF50),
                                title = "Goal Reminders",
                                subtitle = "Progress & milestones",
                                checked = goalReminders,
                                onCheckedChange = { goalReminders = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.CreditCard,
                                iconColor = NotifBlue,
                                title = "Loan Reminders",
                                subtitle = "Payment due dates",
                                checked = loanReminders,
                                onCheckedChange = { loanReminders = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.ShoppingCart,
                                iconColor = NotifMutedRed,
                                title = "Expense Alerts",
                                subtitle = "Large & unusual expenses",
                                checked = expenseAlerts,
                                onCheckedChange = { expenseAlerts = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.AttachMoney,
                                iconColor = Color(0xFF81C784),
                                title = "Income Notifications",
                                subtitle = "Received & expected income",
                                checked = incomeNotifications,
                                onCheckedChange = { incomeNotifications = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.Security,
                                iconColor = Color(0xFF7986CB),
                                title = "Security Alerts",
                                subtitle = "Login & security events",
                                checked = securityAlerts,
                                onCheckedChange = { securityAlerts = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.Lightbulb,
                                iconColor = Color(0xFFFFD54F),
                                title = "AI Insights",
                                subtitle = "Smart financial tips",
                                checked = insightNotifications,
                                onCheckedChange = { insightNotifications = it }
                            )
                        }

                        item {
                            SettingsToggleItem(
                                icon = Icons.Default.Settings,
                                iconColor = NotifPurple,
                                title = "System Updates",
                                subtitle = "App updates & announcements",
                                checked = systemNotifications,
                                onCheckedChange = { systemNotifications = it }
                            )
                        }

                        // Delivery Section
                        item {
                            Spacer(Modifier.height(16.dp))
                            SettingsSectionHeader(
                                title = "Delivery Settings",
                                icon = Icons.Default.Notifications
                            )
                        }

                        // Sound Mode Options
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    "Sound Mode",
                                    color = NotifSoftWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                )

                                // Sound Only Option
                                SoundModeOptionItem(
                                    icon = Icons.Default.VolumeUp,
                                    iconColor = NotifEmerald,
                                    title = "Sound Only",
                                    subtitle = "Play notification sound without vibration",
                                    isSelected = soundMode == com.example.budgie.notifications.NotificationSoundMode.SOUND_ONLY,
                                    onClick = {
                                        soundMode = com.example.budgie.notifications.NotificationSoundMode.SOUND_ONLY
                                        showSoundOptions = true
                                    }
                                )

                                Spacer(Modifier.height(4.dp))

                                // Sound + Vibration Option
                                SoundModeOptionItem(
                                    icon = Icons.Default.PhoneAndroid,
                                    iconColor = NotifBlue,
                                    title = "Sound + Vibration",
                                    subtitle = "Play sound and vibrate together",
                                    isSelected = soundMode == com.example.budgie.notifications.NotificationSoundMode.SOUND_AND_VIBRATION,
                                    onClick = {
                                        soundMode = com.example.budgie.notifications.NotificationSoundMode.SOUND_AND_VIBRATION
                                        showSoundOptions = true
                                    }
                                )

                                Spacer(Modifier.height(4.dp))

                                // Vibration Only Option
                                SoundModeOptionItem(
                                    icon = Icons.Default.Vibration,
                                    iconColor = NotifAmber,
                                    title = "Vibration Only",
                                    subtitle = "Silent with vibration alerts",
                                    isSelected = soundMode == com.example.budgie.notifications.NotificationSoundMode.VIBRATION_ONLY,
                                    onClick = {
                                        soundMode = com.example.budgie.notifications.NotificationSoundMode.VIBRATION_ONLY
                                        showSoundOptions = false
                                    }
                                )

                                Spacer(Modifier.height(4.dp))

                                // Silent Option
                                SoundModeOptionItem(
                                    icon = Icons.Default.NotificationsOff,
                                    iconColor = NotifSoftWhite.copy(alpha = 0.5f),
                                    title = "Silent",
                                    subtitle = "No sound, no vibration",
                                    isSelected = soundMode == com.example.budgie.notifications.NotificationSoundMode.SILENT,
                                    onClick = {
                                        soundMode = com.example.budgie.notifications.NotificationSoundMode.SILENT
                                        showSoundOptions = false
                                    }
                                )
                            }
                        }

                        // Sound Selection (only show when sound is enabled)
                        item {
                            AnimatedVisibility(
                                visible = (soundMode == com.example.budgie.notifications.NotificationSoundMode.SOUND_ONLY ||
                                          soundMode == com.example.budgie.notifications.NotificationSoundMode.SOUND_AND_VIBRATION),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        "Notification Sound",
                                        color = NotifSoftWhite.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // System sound option
                                    SoundOptionItem(
                                        title = "Default system sound",
                                        isSelected = useSystemSound,
                                        onClick = { useSystemSound = true }
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    // Custom sound option
                                    SoundOptionItem(
                                        title = "Choose custom sound",
                                        isSelected = !useSystemSound,
                                        onClick = {
                                            useSystemSound = false
                                            // Open in-app ringtone picker
                                            onOpenRingtonePicker()
                                        }
                                    )
                                }
                            }
                        }

                        // Quiet Hours Toggle with time picker
                        item {
                            Column {
                                SettingsToggleItem(
                                    icon = Icons.Default.Bedtime,
                                    iconColor = NotifBlue,
                                    title = "Quiet Hours",
                                    subtitle = if (quietHoursEnabled) {
                                        "${formatTime(quietStartHour, quietStartMinute)} - ${formatTime(quietEndHour, quietEndMinute)}"
                                    } else "No sound or vibration during set hours",
                                    checked = quietHoursEnabled,
                                    onCheckedChange = { quietHoursEnabled = it }
                                )

                                // Time picker when enabled
                                AnimatedVisibility(
                                    visible = quietHoursEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Start time
                                        TimePickerButton(
                                            label = "Start",
                                            time = formatTime(quietStartHour, quietStartMinute),
                                            onClick = {
                                                editingStartTime = true
                                                showQuietHoursTimePicker = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        // End time
                                        TimePickerButton(
                                            label = "End",
                                            time = formatTime(quietEndHour, quietEndMinute),
                                            onClick = {
                                                editingStartTime = false
                                                showQuietHoursTimePicker = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Footer with done button (settings auto-save)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NotifNavyLight)
                            .padding(16.dp)
                    ) {
                        Column {
                            // Auto-save indicator
                            Text(
                                "✓ Settings save automatically",
                                color = NotifEmerald.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NotifEmerald
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Done",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )

    // Time Picker Dialog
    if (showQuietHoursTimePicker) {
        QuietHoursTimePickerDialog(
            currentHour = if (editingStartTime) quietStartHour else quietEndHour,
            currentMinute = if (editingStartTime) quietStartMinute else quietEndMinute,
            title = if (editingStartTime) "Set Quiet Hours Start" else "Set Quiet Hours End",
            onTimeSelected = { hour, minute ->
                if (editingStartTime) {
                    quietStartHour = hour
                    quietStartMinute = minute
                } else {
                    quietEndHour = hour
                    quietEndMinute = minute
                }
                showQuietHoursTimePicker = false
            },
            onDismiss = { showQuietHoursTimePicker = false }
        )
    }
}

// Helper Composables for Notification Settings

@Composable
private fun MasterControlButton(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                iconColor.copy(alpha = 0.15f)
            else
                NotifNavyLight.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isEnabled) androidx.compose.foundation.BorderStroke(
            1.dp,
            iconColor.copy(alpha = 0.5f)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = if (isEnabled) 0.3f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NotifSoftWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = NotifSoftWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            if (isEnabled) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SoundOptionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                NotifEmerald.copy(alpha = 0.15f)
            else
                NotifNavyLight.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            1.dp,
            NotifEmerald.copy(alpha = 0.5f)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = NotifEmerald,
                    unselectedColor = NotifSoftWhite.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = title,
                color = if (isSelected) NotifEmerald else NotifSoftWhite.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )

            if (!isSelected && title.contains("custom", ignoreCase = true)) {
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = NotifSoftWhite.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SoundModeOptionItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                iconColor.copy(alpha = 0.15f)
            else
                NotifNavyLight.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            1.5.dp,
            iconColor.copy(alpha = 0.6f)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) iconColor
                        else NotifSoftWhite.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) iconColor else iconColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) iconColor else NotifSoftWhite,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = NotifSoftWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TimePickerButton(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = NotifNavyLight.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            NotifBlue.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = NotifSoftWhite.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = NotifBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = time,
                    color = NotifSoftWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietHoursTimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    title: String,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(currentHour) }
    var selectedMinute by remember { mutableStateOf(currentMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NotifNavy,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = NotifBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    color = NotifSoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hour and Minute selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", color = NotifSoftWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { selectedHour = (selectedHour - 1 + 24) % 24 },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NotifNavyLight)
                            ) {
                                Icon(Icons.Default.Remove, null, tint = NotifSoftWhite)
                            }

                            Text(
                                text = String.format("%02d", selectedHour),
                                color = NotifEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            )

                            IconButton(
                                onClick = { selectedHour = (selectedHour + 1) % 24 },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NotifNavyLight)
                            ) {
                                Icon(Icons.Default.Add, null, tint = NotifSoftWhite)
                            }
                        }
                    }

                    Text(
                        ":",
                        color = NotifSoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", color = NotifSoftWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { selectedMinute = (selectedMinute - 5 + 60) % 60 },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NotifNavyLight)
                            ) {
                                Icon(Icons.Default.Remove, null, tint = NotifSoftWhite)
                            }

                            Text(
                                text = String.format("%02d", selectedMinute),
                                color = NotifEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            )

                            IconButton(
                                onClick = { selectedMinute = (selectedMinute + 5) % 60 },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NotifNavyLight)
                            ) {
                                Icon(Icons.Default.Add, null, tint = NotifSoftWhite)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // AM/PM indicator
                val amPm = if (selectedHour >= 12) "PM" else "AM"
                val displayHour = when {
                    selectedHour == 0 -> 12
                    selectedHour > 12 -> selectedHour - 12
                    else -> selectedHour
                }
                Text(
                    text = "$displayHour:${String.format("%02d", selectedMinute)} $amPm",
                    color = NotifSoftWhite.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onTimeSelected(selectedHour, selectedMinute) },
                colors = ButtonDefaults.buttonColors(containerColor = NotifEmerald),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Set Time", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NotifSoftWhite.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = NotifEmerald,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = NotifEmerald,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            NotifEmerald.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = NotifNavyLight.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = NotifSoftWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = NotifSoftWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            // Toggle Switch
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NotifEmerald,
                    uncheckedThumbColor = NotifSoftWhite.copy(alpha = 0.6f),
                    uncheckedTrackColor = NotifNavyLight
                )
            )
        }
    }
}

