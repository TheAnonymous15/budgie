package com.example.budgie.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgie.ui.components.CurrencyConverterModal
import com.example.budgie.ui.components.CalculatorModal
import com.example.budgie.ui.components.ExpenditureSection
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ui.viewmodel.ModelDownloadViewModel
import kotlinx.coroutines.delay
import java.util.*

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE DASHBOARD SCREEN
   Premium wealth management dashboard with full responsiveness
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveDashboardScreen(
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
    modelDownloadViewModel: ModelDownloadViewModel? = null
) {
    val context = LocalContext.current

    // Get responsive dimensions
    val dimens = getDashboardDimens()

    // Use provided viewModel or create one
    val downloadViewModel = modelDownloadViewModel ?: viewModel(
        factory = ModelDownloadViewModel.Factory(context)
    )

    // Notification manager and repository
    val notificationManager = remember {
        com.example.budgie.notifications.BudgieNotificationManager.getInstance(context)
    }
    val notificationRepository = remember {
        com.example.budgie.data.repository.NotificationRepository.getInstance(context)
    }
    val unreadNotificationCount by notificationRepository.getUnreadCount().collectAsState(initial = 0)

    // Start test notification loop
    LaunchedEffect(Unit) {
        notificationManager.createTestNotification1()
        notificationManager.startTestNotificationLoop()
    }

    // Collect data
    val summary by viewModel.financialSummary.collectAsState()
    val insights by viewModel.spendingInsights.collectAsState()
    val unpaidBills by viewModel.unpaidBills.collectAsState()
    val totalUnpaidBills by viewModel.totalUnpaidBills.collectAsState()
    val currentExpenses by viewModel.currentMonthExpenses.collectAsState()
    val currentBudgets by viewModel.currentBudgets.collectAsState()
    val totalBudget = currentBudgets.sumOf { it.limit }
    val goalsSummary by viewModel.goalsSummary.collectAsState()
    val loanSummary by viewModel.loanSummary.collectAsState()

    // Check model download
    LaunchedEffect(Unit) {
        downloadViewModel.checkAndStartDownload()
    }

    // Calendar and greeting
    val calendar = Calendar.getInstance()
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val currentMonth = monthNames[calendar.get(Calendar.MONTH)]
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

    val greeting = when {
        currentHour < 12 -> "Good Morning"
        currentHour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val displayName = userName?.replaceFirstChar { it.uppercase() }

    // UI State
    var fabExpanded by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCurrencyConverter by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }
    var isNetWorthExpanded by remember { mutableStateOf(false) }

    // Prepare data for new widgets
    val spendingCategories = remember(currentExpenses) {
        currentExpenses
            .groupBy { it.category }
            .map { (category, expenses) ->
                SpendingCategory(
                    name = category.displayName,
                    amount = expenses.sumOf { it.amount },
                    color = getCategoryColor(category.name),
                    icon = getCategoryIcon(category.name)
                )
            }
            .sortedByDescending { it.amount }
    }

    // 7-day cash flow data
    val cashFlowData = remember(currentExpenses) {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val data = MutableList(7) { 0.0 }
        currentExpenses.take(20).forEachIndexed { index, expense ->
            val dayIndex = (dayOfWeek - 1 + index) % 7
            data[dayIndex] += expense.amount
        }
        data to weekDays
    }

    // Quick actions
    val quickActions = remember {
        listOf(
            QuickAction("expense", "Expenses", Icons.Default.List, DashboardMutedRed),
            QuickAction("income", "Income", Icons.Default.Add, DashboardEmerald, isPrimary = true),
            QuickAction("bills", "Bills", Icons.Default.CreditCard, DashboardBlue, badge = if (unpaidBills.isNotEmpty()) "${unpaidBills.size}" else null),
            QuickAction("budget", "Budget", Icons.Default.Assessment, DashboardCyan),
            QuickAction("goals", "Goals", Icons.Default.Star, DashboardGold),
            QuickAction("loans", "Loans", Icons.Default.AccountBalanceWallet, DashboardPurple)
        )
    }

    // Back press handling
    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(3000)
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

    // Animated background
    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient mesh background
        AnimatedGradientMesh()

        // Particle overlay (subtle)
        AnimatedParticleBackground(
            particleCount = 20,
            particleColors = listOf(
                DashboardEmerald.copy(alpha = 0.15f),
                DashboardCyan.copy(alpha = 0.1f)
            )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ResponsiveDashboardTopBar(
                    dimens = dimens,
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
                ResponsiveFloatingButtons(
                    dimens = dimens,
                    fabExpanded = fabExpanded,
                    onFabExpandedChange = { fabExpanded = it },
                    onNavigateToAIChat = onNavigateToAIChat,
                    onShowIncomeDialog = { showIncomeDialog = true },
                    onAddExpense = onAddExpense,
                    onNavigateToBills = onNavigateToBills,
                    onNavigateToShoppingList = onNavigateToShoppingList,
                    onNavigateToExport = onNavigateToExport
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(dimens.sectionSpacing)
            ) {
                // ═══════════════════════════════════════════════════════════
                // SMART GREETING (NEW)
                // ═══════════════════════════════════════════════════════════
                item {
                    SmartGreetingCard(
                        greeting = greeting,
                        displayName = displayName,
                        summary = summary,
                        unpaidBillsCount = unpaidBills.size,
                        goalsSummary = goalsSummary
                    )
                }

                // ═══════════════════════════════════════════════════════════
                // COMPACT TRIPLE GAUGE ROW (Financial Health, Goals, Loans)
                // ═══════════════════════════════════════════════════════════
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        CompactTripleGaugeRow(
                            summary = summary,
                            goalsSummary = goalsSummary,
                            loanSummary = loanSummary,
                            unpaidBillsCount = unpaidBills.size,
                            totalBudget = totalBudget
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // QUICK STATS BAR (NEW)
                // ═══════════════════════════════════════════════════════════
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        QuickStatsBar(
                            income = summary.totalIncome,
                            expenses = summary.totalExpenses,
                            savings = summary.totalIncome - summary.totalExpenses
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // COMPACT QUICK ACTIONS (Non-scrolling grid)
                // ═══════════════════════════════════════════════════════════
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        CompactQuickActionsGrid(
                            actions = quickActions,
                            onActionClick = { action ->
                                when (action.id) {
                                    "expense" -> onNavigateToExpenses()
                                    "income" -> showIncomeDialog = true
                                    "bills" -> onNavigateToBills()
                                    "budget" -> onNavigateToBudget()
                                    "goals" -> onNavigateToGoals()
                                    "loans" -> onNavigateToLoans()
                                }
                            }
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // SPENDING BREAKDOWN CHART (NEW)
                // ═══════════════════════════════════════════════════════════
                if (spendingCategories.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                            SpendingBreakdownChart(
                                categories = spendingCategories,
                                totalSpent = summary.totalExpenses,
                                onCategoryClick = { /* Navigate to category details */ }
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // CASH FLOW SPARKLINE (NEW)
                // ═══════════════════════════════════════════════════════════
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        CashFlowSparkline(
                            data = cashFlowData.first,
                            labels = cashFlowData.second,
                            title = "7-Day Cash Flow"
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // NET WORTH TRACKER (NEW)
                // ═══════════════════════════════════════════════════════════
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        NetWorthTracker(
                            totalAssets = summary.totalIncome, // Simplified - would need proper assets tracking
                            totalLiabilities = loanSummary.totalRemaining,
                            isExpanded = isNetWorthExpanded,
                            onToggleExpand = { isNetWorthExpanded = !isNetWorthExpanded }
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // WEEKLY SUMMARY (NEW)
                // ═══════════════════════════════════════════════════════════
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        WeeklySummaryCard(
                            weeklyIncome = summary.totalIncome * 0.25, // Approximation
                            weeklyExpenses = summary.totalExpenses * 0.25,
                            dailyAverage = summary.totalExpenses / 30,
                            bestDay = "Monday",
                            worstDay = "Saturday"
                        )
                    }
                }

                // Expenditure Overview Section
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        ExpenditureSection(expenses = currentExpenses)
                    }
                }

                // AI Insights Preview
                if (insights.isNotEmpty()) {
                    item {
                        ResponsiveInsightsHeader(
                            dimens = dimens,
                            onNavigateToInsights = onNavigateToInsights
                        )
                    }
                    items(insights.take(2)) { insight ->
                        ResponsiveInsightCard(dimens = dimens, insight = insight)
                    }
                }

                // Recent Transactions
                item {
                    ResponsiveTransactionsHeader(
                        dimens = dimens,
                        hasExpenses = currentExpenses.isNotEmpty(),
                        onNavigateToExpenses = onNavigateToExpenses
                    )
                }

                if (currentExpenses.isEmpty()) {
                    item {
                        ResponsiveEmptyTransactions(
                            dimens = dimens,
                            onAddExpense = onAddExpense
                        )
                    }
                } else {
                    items(currentExpenses.take(3)) { expense ->
                        ResponsiveExpenseItem(
                            dimens = dimens,
                            expense = expense,
                            onClick = { },
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }

                // Upcoming Bills
                if (unpaidBills.isNotEmpty()) {
                    item {
                        ResponsiveBillsHeader(
                            dimens = dimens,
                            onNavigateToBills = onNavigateToBills
                        )
                    }
                    items(unpaidBills.take(3)) { bill ->
                        ResponsiveBillItem(
                            dimens = dimens,
                            bill = bill,
                            onPaidToggle = { viewModel.markBillAsPaid(bill.id, it) },
                            onClick = { }
                        )
                    }
                }

                // Wealth Building Tip
                if (summary.totalIncome == 0.0 && summary.totalExpenses == 0.0) {
                    item {
                        ResponsiveWealthTip(dimens = dimens)
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    // Add Income Dialog
    if (showIncomeDialog) {
        ResponsiveAddIncomeDialog(
            dimens = dimens,
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

// Helper function to get category color
private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food", "groceries", "dining" -> Color(0xFFFF6B6B)
        "transport", "transportation", "fuel" -> Color(0xFF4ECDC4)
        "utilities", "bills" -> Color(0xFF45B7D1)
        "entertainment", "leisure" -> Color(0xFFFFA07A)
        "shopping" -> Color(0xFFDDA0DD)
        "health", "medical" -> Color(0xFF98D8C8)
        "education" -> Color(0xFF87CEEB)
        "savings", "investment" -> Color(0xFF10B981)
        else -> Color(0xFF9B59B6)
    }
}

// Helper function to get category icon
private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "food", "groceries", "dining" -> Icons.Default.Fastfood
        "transport", "transportation", "fuel" -> Icons.Default.LocalShipping
        "utilities", "bills" -> Icons.Default.Home
        "entertainment", "leisure" -> Icons.Default.Celebration
        "shopping" -> Icons.Default.ShoppingBag
        "health", "medical" -> Icons.Default.Favorite
        "education" -> Icons.Default.MenuBook
        "savings", "investment" -> Icons.Default.AttachMoney
        else -> Icons.Default.MoreHoriz
    }
}

