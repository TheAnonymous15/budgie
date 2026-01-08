package com.example.budgie.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgie.ui.components.CurrencyConverterModal
import com.example.budgie.ui.components.CalculatorModal
import com.example.budgie.ui.components.ExpenditureSection
import com.example.budgie.ui.components.MagicalModal
import com.example.budgie.ui.components.MagicalModalScrollableContent
import com.example.budgie.ui.components.MagicalModalSection
import com.example.budgie.ui.screens.IncomeScreen
import com.example.budgie.ui.screens.ExpensesScreen
import com.example.budgie.ui.screens.BillsScreen
import com.example.budgie.ui.screens.BudgetScreen
import com.example.budgie.ui.screens.InsightsScreen
import com.example.budgie.ui.screens.GoalsScreen
import com.example.budgie.ui.screens.LoansScreen
import com.example.budgie.ui.screens.ShoppingListScreen
import com.example.budgie.ui.screens.NotificationsScreen
import com.example.budgie.ui.screens.AIChatScreen
import com.example.budgie.ui.screens.AnalyticsScreen
import com.example.budgie.ui.screens.SettingsScreen
import com.example.budgie.ui.theme.WealthTheme
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ui.viewmodel.ModelDownloadViewModel
import com.example.budgie.ui.components.formatCurrency
import com.example.budgie.ai.ConversationalAI
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

    // Modal States - Track which popup is currently open
    var activeModal by remember { mutableStateOf<DashboardModal?>(null) }


    // Color definitions
    val DashboardPink = Color(0xFFFF006E)

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
                    onNavigateToNotifications = { activeModal = DashboardModal.NOTIFICATIONS },
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
                    onNavigateToAIChat = { activeModal = DashboardModal.AI_CHAT },
                    onShowIncomeDialog = { activeModal = DashboardModal.INCOME },
                    onAddExpense = { activeModal = DashboardModal.EXPENSES },
                    onNavigateToBills = { activeModal = DashboardModal.BILLS },
                    onNavigateToGoals = { activeModal = DashboardModal.GOALS },
                    onNavigateToAnalytics = { activeModal = DashboardModal.INSIGHTS }, // Analytics opens Insights
                    onNavigateToShoppingList = { activeModal = DashboardModal.SHOPPING },
                    onNavigateToExport = { activeModal = DashboardModal.EXPORT },
                    onNavigateToSettings = { activeModal = DashboardModal.SETTINGS },
                    onNavigateToNotifications = { activeModal = DashboardModal.NOTIFICATIONS }
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
                // COMPACT QUICK ACTIONS (Non-scrolling grid) - NOW OPENS MODALS!
                // ═══════════════════════════════════════════════════════════
                item {
                    Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                        CompactQuickActionsGrid(
                            actions = quickActions,
                            onActionClick = { action ->
                                when (action.id) {
                                    "expense" -> activeModal = DashboardModal.EXPENSES
                                    "income" -> activeModal = DashboardModal.INCOME
                                    "bills" -> activeModal = DashboardModal.BILLS
                                    "budget" -> activeModal = DashboardModal.BUDGET
                                    "goals" -> activeModal = DashboardModal.GOALS
                                    "loans" -> activeModal = DashboardModal.LOANS
                                }
                            }
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // SPENDING BREAKDOWN CHART (NEW) - Click opens Expenses Modal!
                // ═══════════════════════════════════════════════════════════
                if (spendingCategories.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = dimens.horizontalPadding)) {
                            SpendingBreakdownChart(
                                categories = spendingCategories,
                                totalSpent = summary.totalExpenses,
                                onCategoryClick = { activeModal = DashboardModal.EXPENSES }
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

                // AI Insights Preview - Click opens modal!
                if (insights.isNotEmpty()) {
                    item {
                        ResponsiveInsightsHeader(
                            dimens = dimens,
                            onNavigateToInsights = { activeModal = DashboardModal.INSIGHTS }
                        )
                    }
                    items(insights.take(2)) { insight ->
                        ResponsiveInsightCard(
                            dimens = dimens,
                            insight = insight,
                            onClick = { activeModal = DashboardModal.INSIGHTS }
                        )
                    }
                }

                // Recent Transactions - Click opens modal!
                item {
                    ResponsiveTransactionsHeader(
                        dimens = dimens,
                        hasExpenses = currentExpenses.isNotEmpty(),
                        onNavigateToExpenses = { activeModal = DashboardModal.EXPENSES }
                    )
                }

                if (currentExpenses.isEmpty()) {
                    item {
                        ResponsiveEmptyTransactions(
                            dimens = dimens,
                            onAddExpense = { activeModal = DashboardModal.EXPENSES }
                        )
                    }
                } else {
                    items(currentExpenses.take(3)) { expense ->
                        ResponsiveExpenseItem(
                            dimens = dimens,
                            expense = expense,
                            onClick = { activeModal = DashboardModal.EXPENSES },
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }

                // Upcoming Bills - Click opens modal!
                if (unpaidBills.isNotEmpty()) {
                    item {
                        ResponsiveBillsHeader(
                            dimens = dimens,
                            onNavigateToBills = { activeModal = DashboardModal.BILLS }
                        )
                    }
                    items(unpaidBills.take(3)) { bill ->
                        ResponsiveBillItem(
                            dimens = dimens,
                            bill = bill,
                            onPaidToggle = { viewModel.markBillAsPaid(bill.id, it) },
                            onClick = { activeModal = DashboardModal.BILLS }
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

        // ═══════════════════════════════════════════════════════════════════
        //  MAGICAL MODAL SYSTEM - SUPERIOR GLASSMORPHIC POPUPS
        // ═══════════════════════════════════════════════════════════════════

        // Income Modal - Optimized for modal display
        MagicalModal(
            isVisible = activeModal == DashboardModal.INCOME,
            onDismiss = { activeModal = null },
            title = "Income Management",
            accentColor = DashboardEmerald,
            maxHeightFraction = 0.85f
        ) {
            IncomeModalContent(
                viewModel = viewModel,
                onDismiss = { activeModal = null }
            )
        }

    // Expenses Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.EXPENSES,
        onDismiss = { activeModal = null },
        title = "Expense Tracker",
        accentColor = DashboardMutedRed
    ) {
        ExpensesScreen(
            viewModel = viewModel,
            onAddExpense = { /* Allow adding expense inline */ },
            onBack = { activeModal = null }
        )
    }

    // Bills Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.BILLS,
        onDismiss = { activeModal = null },
        title = "Bills Management",
        accentColor = DashboardBlue,
        maxHeightFraction = 0.92f
    ) {
        BillsScreen(
            viewModel = viewModel,
            onAddBill = { /* Open add bill flow */ },
            onBack = { activeModal = null }
        )
    }

    // Budget Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.BUDGET,
        onDismiss = { activeModal = null },
        title = "Budget Planner",
        accentColor = DashboardCyan
    ) {
        BudgetScreen(
            viewModel = viewModel,
            onAddBudget = { /* Allow adding budget inline */ },
            onBack = { activeModal = null }
        )
    }

    // Goals Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.GOALS,
        onDismiss = { activeModal = null },
        title = "Financial Goals",
        accentColor = DashboardGold,
        maxHeightFraction = 0.95f
    ) {
        val goals by viewModel.goals.collectAsState()

        GoalsScreen(
            goals = goals,
            onAddGoal = { goal -> viewModel.addGoal(goal) },
            onUpdateGoal = { goal -> viewModel.updateGoal(goal) },
            onDeleteGoal = { goal -> viewModel.deleteGoal(goal) },
            onAddContribution = { goalId, amount -> viewModel.addGoalContribution(goalId, amount) },
            onNavigateBack = { activeModal = null }
        )
    }

    // Loans Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.LOANS,
        onDismiss = { activeModal = null },
        title = "Loan Portfolio",
        accentColor = DashboardPurple,
        maxHeightFraction = 0.95f
    ) {
        val loans by viewModel.loans.collectAsState()
        val loanPayments by viewModel.loanPayments.collectAsState()
        val loanSummary by viewModel.loanSummary.collectAsState()

        LoansScreen(
            loans = loans,
            loanPayments = loanPayments,
            loanSummary = loanSummary,
            financialSummary = summary,
            onAddLoan = { loan -> viewModel.addLoan(loan) },
            onUpdateLoan = { loan -> viewModel.updateLoan(loan) },
            onDeleteLoan = { loan -> viewModel.deleteLoan(loan) },
            onAddPayment = { loanId, amount, method -> viewModel.addLoanPayment(loanId, amount, method) },
            onNavigateBack = { activeModal = null }
        )
    }

    // Shopping List Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.SHOPPING,
        onDismiss = { activeModal = null },
        title = "Smart Shopping List",
        accentColor = Color(0xFF00D4FF),
        maxHeightFraction = 0.95f
    ) {
        val shoppingLists by viewModel.shoppingListsWithItems.collectAsState()

        ShoppingListScreen(
            shoppingLists = shoppingLists,
            monthlyIncome = summary.totalIncome,
            monthlyExpenses = summary.totalExpenses,
            onCreateList = { list -> viewModel.createShoppingList(list) },
            onUpdateList = { list -> viewModel.updateShoppingList(list) },
            onDeleteList = { list -> viewModel.deleteShoppingList(list) },
            onAddItem = { item -> viewModel.addShoppingItem(item) },
            onUpdateItem = { item -> viewModel.updateShoppingItem(item) },
            onDeleteItem = { item -> viewModel.deleteShoppingItem(item) },
            onAnalyzeList = { listId -> viewModel.analyzeShoppingList(listId) },
            onAddToBudget = { listId, amount -> viewModel.addShoppingListToBudget(listId, amount) },
            onNavigateBack = { activeModal = null }
        )
    }

    // Notifications Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.NOTIFICATIONS,
        onDismiss = { activeModal = null },
        title = "Notifications",
        accentColor = DashboardPink
    ) {
        NotificationsScreen(
            onBack = { activeModal = null }
        )
    }

    // Insights Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.INSIGHTS,
        onDismiss = { activeModal = null },
        title = "AI Insights",
        accentColor = DashboardPurple,
        maxHeightFraction = 0.95f
    ) {
        InsightsScreen(
            viewModel = viewModel,
            onBack = { activeModal = null }
        )
    }

    // AI Chat Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.AI_CHAT,
        onDismiss = { activeModal = null },
        title = "Budgie AI Assistant",
        accentColor = Color(0xFF00F5A0),
        maxHeightFraction = 0.95f
    ) {
        AIChatScreen(
            viewModel = viewModel,
            onBack = { activeModal = null }
        )
    }

    // Export Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.EXPORT,
        onDismiss = { activeModal = null },
        title = "Export Data",
        accentColor = Color(0xFFFFD93D),
        maxHeightFraction = 0.92f
    ) {
        com.example.budgie.ui.screens.ExportScreen(
            viewModel = viewModel,
            userName = userName,
            onBack = { activeModal = null }
        )
    }

    // Bills Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.BILLS,
        onDismiss = { activeModal = null },
        title = "Bills Management",
        accentColor = Color(0xFFFF6B6B),
        maxHeightFraction = 0.95f
    ) {
        BillsScreen(
            viewModel = viewModel,
            onAddBill = { /* Open add bill dialog */ },
            onBack = { activeModal = null }
        )
    }

    // Analytics Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.ANALYTICS,
        onDismiss = { activeModal = null },
        title = "Analytics Dashboard",
        accentColor = Color(0xFF00D4FF),
        maxHeightFraction = 0.95f
    ) {
        AnalyticsScreen(
            viewModel = viewModel,
            onBack = { activeModal = null }
        )
    }

    // Settings Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.SETTINGS,
        onDismiss = { activeModal = null },
        title = "Settings",
        accentColor = Color(0xFF9B59B6),
        maxHeightFraction = 0.92f
    ) {
        SettingsScreen(
            onBack = { activeModal = null }
        )
    }

    // Currency Converter Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.CURRENCY_CONVERTER,
        onDismiss = { activeModal = null },
        title = "Currency Converter",
        accentColor = DashboardGold,
        maxHeightFraction = 0.75f
    ) {
        CurrencyConverterModal(
            onDismiss = { activeModal = null }
        )
    }

    // Calculator Modal
    MagicalModal(
        isVisible = activeModal == DashboardModal.CALCULATOR,
        onDismiss = { activeModal = null },
        title = "Calculator",
        accentColor = Color(0xFF00F5A0),
        maxHeightFraction = 0.80f
    ) {
        CalculatorModal(
            onDismiss = { activeModal = null }
        )
    }
    } // Close main Box container
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


// ══════════════════════════════════════════════════════════════════════════════
//  INCOME MODAL CONTENT - Optimized for Modal Display
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeModalContent(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val incomes by viewModel.incomes.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total Income Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DashboardEmerald.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Income This Month",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatCurrency(summary.totalIncome),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DashboardEmerald
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(DashboardEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = DashboardEmerald,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Add Income Button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddIncomeDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = DashboardEmerald
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add New Income",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Income List Header
            if (incomes.isNotEmpty()) {
                item {
                    Text(
                        text = "All Income (${incomes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthTheme.SoftWhite
                    )
                }
            }

            // Income List Items
            items(incomes) { income ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DashboardNavy.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DashboardEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = income.source.icon,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column {
                                Text(
                                    text = income.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WealthTheme.SoftWhite
                                )
                                Text(
                                    text = income.source.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = formatCurrency(income.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DashboardEmerald
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (income.isRecurring) {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = DashboardEmerald.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Recurring",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DashboardEmerald.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty State
            if (incomes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(DashboardEmerald.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = DashboardEmerald.copy(alpha = 0.4f)
                                )
                            }
                            Text(
                                text = "No income recorded yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap 'Add New Income' to track your earnings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Bottom spacing for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Income Dialog
    if (showAddIncomeDialog) {
        AddIncomeModalDialog(
            viewModel = viewModel,
            onDismiss = { showAddIncomeDialog = false }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ADD INCOME MODAL DIALOG - Compact dialog for adding income
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeModalDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(com.example.budgie.data.model.IncomeSource.SALARY) }
    var isRecurring by remember { mutableStateOf(true) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = DashboardNavy
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Income",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthTheme.SoftWhite
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = WealthTheme.SoftWhite
                        )
                    }
                }

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Income Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DashboardEmerald,
                        focusedTextColor = WealthTheme.SoftWhite,
                        unfocusedTextColor = WealthTheme.SoftWhite,
                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f)
                    )
                )

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DashboardEmerald,
                        focusedTextColor = WealthTheme.SoftWhite,
                        unfocusedTextColor = WealthTheme.SoftWhite,
                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f)
                    )
                )

                // Recurring toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recurring Income",
                        color = WealthTheme.SoftWhite
                    )
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DashboardEmerald,
                            checkedTrackColor = DashboardEmerald.copy(alpha = 0.5f)
                        )
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && amount.isNotBlank()) {
                                viewModel.addIncome(
                                    com.example.budgie.data.model.Income(
                                        id = "",
                                        title = title,
                                        amount = amount.toDoubleOrNull() ?: 0.0,
                                        source = selectedSource,
                                        date = System.currentTimeMillis(),
                                        isRecurring = isRecurring
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DashboardEmerald
                        ),
                        enabled = title.isNotBlank() && amount.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

