package com.example.budgie.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgie.R
import com.example.budgie.ui.viewmodel.ModelDownloadViewModel
import com.example.budgie.ai.DownloadState
import com.example.budgie.ui.components.*
import com.example.budgie.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.util.*

// Glassmorphism Colors for Dashboard
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassHighlight = Color.White.copy(alpha = 0.15f)

// Glassmorphism Card Modifier
private fun Modifier.glassmorphicAccentCard(
    accentColor: Color,
    cornerRadius: Int = 16
) = this
    .shadow(
        elevation = 6.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = accentColor.copy(alpha = 0.2f),
        spotColor = accentColor.copy(alpha = 0.2f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.15f),
                accentColor.copy(alpha = 0.08f),
                accentColor.copy(alpha = 0.05f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.3f),
                accentColor.copy(alpha = 0.1f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    userName: String? = null,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToWealthProjection: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToShoppingList: () -> Unit,
    onNavigateToAIChat: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    // Menu navigation callbacks
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onLogout: () -> Unit = {},
    onExit: () -> Unit = {},
    // Model Download ViewModel (injected from navigation)
    modelDownloadViewModel: ModelDownloadViewModel? = null
) {
    val context = LocalContext.current

    // Use provided viewModel or create one
    val downloadViewModel = modelDownloadViewModel ?: viewModel(
        factory = ModelDownloadViewModel.Factory(context)
    )

    // Notification manager and repository
    val notificationManager = remember { com.example.budgie.notifications.BudgieNotificationManager.getInstance(context) }
    val notificationRepository = remember { com.example.budgie.data.repository.NotificationRepository.getInstance(context) }
    // Use NotificationRepository for count (same source as NotificationsScreen)
    val unreadNotificationCount by notificationRepository.getUnreadCount().collectAsState(initial = 0)

    // Start test notification loop (sends 2 notifications every 30 seconds)
    // Also create one immediately on dashboard load
    LaunchedEffect(Unit) {
        // Create an immediate test notification
        notificationManager.createTestNotification1()
        // Then start the loop for additional notifications
        notificationManager.startTestNotificationLoop()
    }

    val summary by viewModel.financialSummary.collectAsState()
    val insights by viewModel.spendingInsights.collectAsState()
    val unpaidBills by viewModel.unpaidBills.collectAsState()
    val totalUnpaidBills by viewModel.totalUnpaidBills.collectAsState()
    val currentExpenses by viewModel.currentMonthExpenses.collectAsState()

    // Budget data
    val currentBudgets by viewModel.currentBudgets.collectAsState()
    val totalBudget = currentBudgets.sumOf { it.limit }

    // Goals & Loans Summary
    val goalsSummary by viewModel.goalsSummary.collectAsState()
    val loanSummary by viewModel.loanSummary.collectAsState()

    // AI Pipeline State
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    // Model download state
    val downloadState by downloadViewModel.downloadState.collectAsState()
    val downloadProgress by downloadViewModel.downloadProgress.collectAsState()
    val downloadedBytes by downloadViewModel.downloadedBytes.collectAsState()
    val totalBytes by downloadViewModel.totalBytes.collectAsState()
    val downloadSpeed by downloadViewModel.downloadSpeed.collectAsState()
    val isModelReady by downloadViewModel.isModelReady.collectAsState()

    // Start download check on first composition
    LaunchedEffect(Unit) {
        // Configure your model URL here
        // downloadViewModel.checkAndStartDownload("https://your-server.com/models/model.gguf")
        downloadViewModel.checkAndStartDownload()
    }

    val calendar = Calendar.getInstance()
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val currentMonth = monthNames[calendar.get(Calendar.MONTH)]
    val currentYear = calendar.get(Calendar.YEAR)
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

    val greeting = when {
        currentHour < 12 -> "Good Morning"
        currentHour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val displayName = userName?.replaceFirstChar { it.uppercase() }

    var fabExpanded by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCurrencyConverter by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }

    // Back press handling - double tap to exit
    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(3000) // Reset after 3 seconds
            backPressedOnce = false
        }
    }

    BackHandler {
        if (backPressedOnce) {
            onExit()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = DashboardNavy,
        topBar = {
            DashboardTopBar(
                showMenu = showMenu,
                onShowMenuChange = { showMenu = it },
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToSecurity = onNavigateToSecurity,
                onShowCurrencyConverter = { showCurrencyConverter = true },
                onShowCalculator = { showCalculator = true },
                onNavigateToHelp = onNavigateToHelp,
                onNavigateToAbout = onNavigateToAbout,
                onNavigateToNotifications = onNavigateToNotifications,
                unreadNotificationCount = unreadNotificationCount,
                onLogout = onLogout,
                onExit = onExit
            )
        },
        floatingActionButton = {
            DashboardFloatingButtons(
                fabExpanded = fabExpanded,
                onFabExpandedChange = { fabExpanded = it },
                onNavigateToAIChat = onNavigateToAIChat,
                onShowIncomeDialog = { showIncomeDialog = true },
                onAddExpense = onAddExpense,
                onNavigateToBills = onNavigateToBills,
                onNavigateToShoppingList = onNavigateToShoppingList
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Header Section
            item {
                DashboardHeader(
                    greeting = greeting,
                    displayName = displayName,
                    currentMonth = currentMonth
                )
            }

            // Financial Summary Cards
            item {
                DashboardFinancialSummary(
                    summary = summary,
                    totalUnpaidBills = totalUnpaidBills,
                    totalBudget = totalBudget,
                    goalsSummary = goalsSummary,
                    loanSummary = loanSummary,
                    onNavigateToGoals = onNavigateToGoals,
                    onNavigateToLoans = onNavigateToLoans,
                    onNavigateToExpenses = onNavigateToExpenses,
                    onNavigateToIncome = onNavigateToIncome,
                    onNavigateToBills = onNavigateToBills,
                    onNavigateToBudget = onNavigateToBudget,
                    onNavigateToInvestments = onNavigateToInvestments,
                    onNavigateToExport = onNavigateToExport
                )
            }

            // Expenditure Overview Section
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    ExpenditureSection(expenses = currentExpenses)
                }
            }


            // AI Insights Preview
            if (insights.isNotEmpty()) {
                item {
                    DashboardInsightsHeader(onNavigateToInsights = onNavigateToInsights)
                }
                items(insights.take(2)) { insight ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        InsightCard(insight = insight)
                    }
                }
            }


            // Recent Transactions
            item {
                DashboardTransactionsHeader(
                    hasExpenses = currentExpenses.isNotEmpty(),
                    onNavigateToExpenses = onNavigateToExpenses
                )
            }

            if (currentExpenses.isEmpty()) {
                item {
                    DashboardEmptyTransactions(onAddExpense = onAddExpense)
                }
            } else {
                items(currentExpenses.take(3)) { expense ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        ExpenseItem(
                            expense = expense,
                            onClick = { },
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }

            // Upcoming Bills
            if (unpaidBills.isNotEmpty()) {
                item {
                    DashboardBillsHeader(onNavigateToBills = onNavigateToBills)
                }
                items(unpaidBills.take(3)) { bill ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        BillItem(
                            bill = bill,
                            onPaidToggle = { viewModel.markBillAsPaid(bill.id, it) },
                            onClick = { }
                        )
                    }
                }
            }

            // Wealth Building Tip
            if (summary.totalIncome == 0.0 && summary.totalExpenses == 0.0) {
                item {
                    DashboardWealthTip()
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // Add Income Dialog
    if (showIncomeDialog) {
        DashboardAddIncomeDialog(
            viewModel = viewModel,
            onDismiss = { showIncomeDialog = false }
        )
    }

    // Currency Converter Modal
    if (showCurrencyConverter) {
        CurrencyConverterModal(onDismiss = { showCurrencyConverter = false })
    }

    // Calculator Modal
    if (showCalculator) {
        CalculatorModal(onDismiss = { showCalculator = false })
    }
}

// ==================== Dashboard Section Components ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onShowCurrencyConverter: () -> Unit,
    onShowCalculator: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    unreadNotificationCount: Int,
    onLogout: () -> Unit,
    onExit: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Premium Logout Dialog
    if (showLogoutDialog) {
        PremiumConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            icon = Icons.Default.Logout,
            iconColor = DashboardAmber,
            title = "Logout Session",
            subtitle = "Secure Sign Out",
            message = "Your session will be ended and you'll need to authenticate again to access your financial data.",
            confirmText = "Logout",
            confirmColor = DashboardAmber,
            cancelText = "Stay Logged In"
        )
    }

    // Premium Exit Dialog
    if (showExitDialog) {
        PremiumConfirmationDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = {
                showExitDialog = false
                onExit()
            },
            icon = Icons.Default.PowerSettingsNew,
            iconColor = DashboardMutedRed,
            title = "Close Budgie",
            subtitle = "Exit Application",
            message = "Your data is automatically saved. You can return anytime to continue managing your finances.",
            confirmText = "Exit App",
            confirmColor = DashboardMutedRed,
            cancelText = "Keep Open"
        )
    }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Real Budgie app logo - no click event
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Budgie Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    "Budgie",
                    fontWeight = FontWeight.Bold,
                    color = DashboardSoftWhite,
                    fontSize = 20.sp
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToNotifications) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(containerColor = DashboardMutedRed, contentColor = Color.White) {
                                Text(
                                    if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = DashboardSoftWhite)
                }
            }
            Box {
                IconButton(onClick = { onShowMenuChange(true) }) {
                    Icon(Icons.Default.MoreVert, "Menu", tint = DashboardSoftWhite)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { onShowMenuChange(false) },
                    modifier = Modifier.background(DashboardNavyLight).width(220.dp)
                ) {
                    // Profile
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Person, null, tint = DashboardEmerald)
                                Text("My Profile", color = DashboardSoftWhite)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToProfile()
                        }
                    )
                    // Settings
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Settings, null, tint = DashboardSoftWhite.copy(alpha = 0.7f))
                                Text("Settings", color = DashboardSoftWhite)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToSettings()
                        }
                    )
                    // Security
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Security, null, tint = DashboardSoftWhite.copy(alpha = 0.7f))
                                Text("Security", color = DashboardSoftWhite)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToSecurity()
                        }
                    )
                    // Currency Converter
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CurrencyExchange, null, tint = DashboardEmerald)
                                Text("Currency Converter", color = DashboardSoftWhite)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            onShowCurrencyConverter()
                        }
                    )
                    // Calculator
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Calculate, null, tint = DashboardAmber)
                                Text("Calculator", color = DashboardSoftWhite)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            onShowCalculator()
                        }
                    )
                    // Help & Support
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Help, null, tint = DashboardSoftWhite.copy(alpha = 0.7f))
                                Text("Help & Support", color = DashboardSoftWhite)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToHelp()
                        }
                    )
                    // About
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Info, null, tint = DashboardSoftWhite.copy(alpha = 0.7f))
                                Text("About Budgie", color = DashboardSoftWhite)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToAbout()
                        }
                    )

                    HorizontalDivider(color = DashboardSoftWhite.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    // Logout
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Logout, null, tint = DashboardAmber)
                                Text("Logout", color = DashboardAmber)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            showLogoutDialog = true
                        }
                    )

                    // Exit
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.ExitToApp, null, tint = DashboardMutedRed)
                                Text("Exit App", color = DashboardMutedRed)
                            }
                        },
                        onClick = {
                            onShowMenuChange(false)
                            showExitDialog = true
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DashboardNavy)
    )
}

@Composable
private fun DashboardFloatingButtons(
    fabExpanded: Boolean,
    onFabExpandedChange: (Boolean) -> Unit,
    onNavigateToAIChat: () -> Unit,
    onShowIncomeDialog: () -> Unit,
    onAddExpense: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToShoppingList: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // AI Chat FAB
        FloatingActionButton(
            onClick = onNavigateToAIChat,
            containerColor = DashboardPurple,
            modifier = Modifier.size(48.dp).shadow(8.dp, CircleShape)
        ) {
            Icon(Icons.Default.AutoAwesome, "AI Chat", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // Expandable FAB Menu
        AnimatedVisibility(visible = fabExpanded) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FabMenuItem("Add Income", DashboardEmerald, Icons.Default.TrendingUp) { onFabExpandedChange(false); onShowIncomeDialog() }
                FabMenuItem("Add Expense", DashboardMutedRed, Icons.Default.Receipt) { onFabExpandedChange(false); onAddExpense() }
                FabMenuItem("Manage Bills", DashboardBlue, Icons.Default.Payment) { onFabExpandedChange(false); onNavigateToBills() }
                FabMenuItem("Shopping List", DashboardPurple, Icons.Default.ShoppingCart) { onFabExpandedChange(false); onNavigateToShoppingList() }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { onFabExpandedChange(!fabExpanded) },
            containerColor = DashboardEmerald,
            modifier = Modifier.size(56.dp).shadow(12.dp, CircleShape)
        ) {
            Icon(
                if (fabExpanded) Icons.Default.Close else Icons.Default.GridView,
                "Actions", tint = Color.White, modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FabMenuItem(label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = DashboardNavy.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp)) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = DashboardSoftWhite, style = MaterialTheme.typography.labelMedium)
        }
        FloatingActionButton(onClick = onClick, containerColor = color, modifier = Modifier.size(48.dp)) {
            Icon(icon, label, tint = Color.White)
        }
    }
}

@Composable
private fun DashboardHeader(greeting: String, displayName: String?, currentMonth: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(brush = Brush.linearGradient(colors = listOf(DashboardNavy, Color(0xFF0D2E3D), DashboardTeal.copy(alpha = 0.8f))))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text("$greeting, ${displayName ?: "there"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DashboardSoftWhite)
            Text("$currentMonth Financial Snapshot", style = MaterialTheme.typography.bodyMedium, color = DashboardSoftWhite.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun DashboardFinancialSummary(
    summary: com.example.budgie.data.model.FinancialSummary,
    totalUnpaidBills: Double,
    totalBudget: Double,
    goalsSummary: com.example.budgie.data.model.GoalsSummary,
    loanSummary: com.example.budgie.data.model.LoanSummary,
    onNavigateToGoals: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Net Savings Card
        val netSavings = summary.totalIncome - summary.totalExpenses
        val hasData = summary.totalIncome > 0 || summary.totalExpenses > 0
        Box(modifier = Modifier.fillMaxWidth().glassmorphicAccentCard(if (netSavings >= 0 && hasData) DashboardEmerald else DashboardMutedRed, 20)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Net Savings", style = MaterialTheme.typography.labelMedium, color = DashboardSoftWhite.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (hasData) formatCurrency(netSavings) else "No data yet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netSavings >= 0 && hasData) DashboardEmerald else DashboardMutedRed
                )
                if (hasData && summary.totalIncome > 0) {
                    val savingsRate = (netSavings / summary.totalIncome) * 100
                    Text("Savings rate: ${String.format("%.1f", savingsRate)}%", style = MaterialTheme.typography.bodySmall, color = DashboardSoftWhite.copy(alpha = 0.6f))
                }
            }
        }

        // Metric Cards Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumMetricCard("Income", summary.totalIncome, Icons.Default.TrendingUp, DashboardEmerald, summary.totalIncome > 0, Modifier.weight(1f))
            PremiumMetricCard("Expenses", summary.totalExpenses, Icons.Default.TrendingDown, DashboardMutedRed, summary.totalExpenses > 0, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumMetricCard("Bills Due", totalUnpaidBills, Icons.Default.Receipt, DashboardBlue, totalUnpaidBills > 0, Modifier.weight(1f))
            PremiumMetricCard("Budget", totalBudget, Icons.Default.PieChart, DashboardCyan, totalBudget > 0, Modifier.weight(1f))
        }

        // Goals/Loans/Analytics Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryActionButton(Icons.Default.Analytics, "Analytics", null, DashboardEmerald, {}, Modifier.weight(1f))
            SummaryActionButton(Icons.Default.Flag, "Goals", if (goalsSummary.activeGoals > 0) "${goalsSummary.activeGoals} active" else null, DashboardGold, onNavigateToGoals, Modifier.weight(1f))
            SummaryActionButton(Icons.Default.AccountBalanceWallet, "Loans", if (loanSummary.activeLoans > 0) "${loanSummary.activeLoans} active" else null, DashboardBlue, onNavigateToLoans, Modifier.weight(1f))
        }

        // Action Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GridActionButton(Icons.Default.Receipt, "Expenses", onNavigateToExpenses, Modifier.weight(1f))
            GridActionButton(Icons.Default.AccountBalance, "Income", onNavigateToIncome, Modifier.weight(1f))
            GridActionButton(Icons.Default.Payment, "Bills", onNavigateToBills, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GridActionButton(Icons.Default.PieChart, "Budget", onNavigateToBudget, Modifier.weight(1f))
            GridActionButton(Icons.Default.TrendingUp, "Invest", onNavigateToInvestments, Modifier.weight(1f))
            GridActionButton(Icons.Default.FileUpload, "Export", onNavigateToExport, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardInsightsHeader(onNavigateToInsights: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(DashboardEmerald.copy(alpha = 0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = DashboardEmerald, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("AI Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DashboardSoftWhite)
        }
        TextButton(onClick = onNavigateToInsights) {
            Text("View All", color = DashboardEmerald, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, null, tint = DashboardEmerald, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DashboardTransactionsHeader(hasExpenses: Boolean, onNavigateToExpenses: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DashboardSoftWhite)
        if (hasExpenses) {
            TextButton(onClick = onNavigateToExpenses) {
                Text("View All", color = DashboardEmerald, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DashboardEmptyTransactions(onAddExpense: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardSoftWhite.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Receipt, null, tint = DashboardSoftWhite.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No transactions yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = DashboardSoftWhite.copy(alpha = 0.7f))
            Text("Start tracking to unlock AI insights", style = MaterialTheme.typography.bodyMedium, color = DashboardSoftWhite.copy(alpha = 0.5f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddExpense, colors = ButtonDefaults.buttonColors(containerColor = DashboardEmerald), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Transaction", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DashboardBillsHeader(onNavigateToBills: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Upcoming Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DashboardSoftWhite)
        TextButton(onClick = onNavigateToBills) {
            Text("View All", color = DashboardEmerald, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DashboardWealthTip() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardEmerald.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardEmerald.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TipsAndUpdates, null, tint = DashboardGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Wealth Building Tip", style = MaterialTheme.typography.labelMedium, color = DashboardGold, fontWeight = FontWeight.Bold)
                Text("If you save \$50/week → \$2,600/year", style = MaterialTheme.typography.bodyMedium, color = DashboardSoftWhite.copy(alpha = 0.8f))
            }
        }
    }
}

// Premium Glassmorphic Confirmation Dialog - Compact & Modern
@Composable
private fun PremiumConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    cancelText: String
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DashboardNavyLight.copy(alpha = 0.98f),
                            DashboardNavy.copy(alpha = 0.99f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            iconColor.copy(alpha = 0.4f),
                            iconColor.copy(alpha = 0.15f),
                            DashboardSoftWhite.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = iconColor.copy(alpha = 0.15f),
                    spotColor = iconColor.copy(alpha = 0.1f)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compact Icon with glow
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    iconColor.copy(alpha = 0.25f),
                                    iconColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        iconColor.copy(alpha = 0.2f),
                                        iconColor.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(1.dp, iconColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subtitle badge - smaller
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconColor.copy(alpha = 0.12f))
                        .border(1.dp, iconColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = subtitle.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title - smaller
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardSoftWhite
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message - more compact
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = DashboardSoftWhite.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons - more compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            DashboardSoftWhite.copy(alpha = 0.25f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DashboardSoftWhite.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            cancelText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }

                    // Confirm Button with gradient
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor
                        )
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            confirmText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Security note - more subtle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DashboardSoftWhite.copy(alpha = 0.04f))
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = DashboardEmerald.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "Your data is encrypted & secure",
                        fontSize = 10.sp,
                        color = DashboardSoftWhite.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

