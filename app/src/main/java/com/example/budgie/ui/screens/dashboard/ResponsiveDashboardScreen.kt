package com.example.budgie.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    Scaffold(
        containerColor = DashboardNavy,
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
                onNavigateToShoppingList = onNavigateToShoppingList
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(dimens.sectionSpacing)
        ) {
            // Header Section
            item {
                ResponsiveDashboardHeader(
                    dimens = dimens,
                    greeting = greeting,
                    displayName = displayName,
                    currentMonth = currentMonth
                )
            }

            // Financial Summary Cards
            item {
                ResponsiveFinancialSummary(
                    dimens = dimens,
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

