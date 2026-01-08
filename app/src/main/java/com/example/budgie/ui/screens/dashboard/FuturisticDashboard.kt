package com.example.budgie.ui.screens.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgie.R
import com.example.budgie.data.model.FinancialSummary
import com.example.budgie.data.model.GoalsSummary
import com.example.budgie.data.model.LoanSummary
import com.example.budgie.ui.components.MagicalModal
import com.example.budgie.ui.screens.*
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ui.viewmodel.ModelDownloadViewModel
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* ═══════════════════════════════════════════════════════════════════════════════
   ███████╗██╗   ██╗████████╗██╗   ██╗██████╗ ██╗███████╗████████╗██╗ ██████╗
   ██╔════╝██║   ██║╚══██╔══╝██║   ██║██╔══██╗██║██╔════╝╚══██╔══╝██║██╔════╝
   █████╗  ██║   ██║   ██║   ██║   ██║██████╔╝██║███████╗   ██║   ██║██║
   ██╔══╝  ██║   ██║   ██║   ██║   ██║██╔══██╗██║╚════██║   ██║   ██║██║
   ██║     ╚██████╔╝   ██║   ╚██████╔╝██║  ██║██║███████║   ██║   ██║╚██████╗
   ╚═╝      ╚═════╝    ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚═╝╚══════╝   ╚═╝   ╚═╝ ╚═════╝

   FUTURISTIC VERTICAL DASHBOARD DESIGN
   ┌──────────────────────────────┐
   │   ⭕ Context Menu (Top)      │  <- Radial menu for main actions
   ├──────────────────────────────┤
   │  📊 Central Data Panel       │  <- Graphs, charts, visualizations
   │     (Graphs & Analytics)     │
   ├──────────────────────────────┤
   │   ⭕ Other Menus (Bottom)    │  <- Secondary actions menu
   └──────────────────────────────┘

   Premium wealth management interface with 3D effects and animations
═══════════════════════════════════════════════════════════════════════════════ */

// Premium color palette
private val FuturisticCyan = Color(0xFF00D9FF)
private val FuturisticEmerald = Color(0xFF10B981)
private val FuturisticPurple = Color(0xFF8B5CF6)
private val FuturisticPink = Color(0xFFFF6B9D)
private val FuturisticOrange = Color(0xFFFF8C42)
private val FuturisticBlue = Color(0xFF3B82F6)
private val FuturisticGold = Color(0xFFFFD700)
private val FuturisticRed = Color(0xFFEF4444)
private val DarkBase = Color(0xFF0A0F1E)
private val DarkPanel = Color(0xFF0D1526)
private val DarkCard = Color(0xFF12182a)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturisticDashboard(
    viewModel: MainViewModel,
    userName: String? = null,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToShoppingList: () -> Unit,
    onNavigateToAIChat: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onLogout: () -> Unit = {},
    onExit: () -> Unit = {},
    modelDownloadViewModel: ModelDownloadViewModel? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // ViewModel setup
    val downloadViewModel = modelDownloadViewModel ?: viewModel(
        factory = ModelDownloadViewModel.Factory(context)
    )

    // Notification setup
    val notificationManager = remember {
        com.example.budgie.notifications.BudgieNotificationManager.getInstance(context)
    }
    val notificationRepository = remember {
        com.example.budgie.data.repository.NotificationRepository.getInstance(context)
    }
    val unreadNotificationCount by notificationRepository.getUnreadCount().collectAsState(initial = 0)

    // Modal state management
    var activeModal by remember { mutableStateOf<DashboardModal?>(null) }

    LaunchedEffect(Unit) {
        notificationManager.createTestNotification1()
        notificationManager.startTestNotificationLoop()
        downloadViewModel.checkAndStartDownload()
    }

    // Collect financial data
    val summary by viewModel.financialSummary.collectAsState()
    val unpaidBills by viewModel.unpaidBills.collectAsState()
    val currentExpenses by viewModel.currentMonthExpenses.collectAsState()
    val goalsSummary by viewModel.goalsSummary.collectAsState()
    val loanSummary by viewModel.loanSummary.collectAsState()

    // Greeting logic
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour < 12 -> "Good Morning"
        currentHour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val displayName = userName?.replaceFirstChar { it.uppercase() } ?: "User"

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "dashboard_anims")

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )

    // Menu items
    val topMenuActions = remember {
        listOf(
            RadialMenuItem("income", "Income", Icons.AutoMirrored.Filled.TrendingUp, FuturisticEmerald),
            RadialMenuItem("expense", "Expense", Icons.Default.Receipt, FuturisticPink),
            RadialMenuItem("bills", "Bills", Icons.Default.Payment, FuturisticCyan),
            RadialMenuItem("budget", "Budget", Icons.Default.PieChart, FuturisticPurple),
            RadialMenuItem("goals", "Goals", Icons.Default.Star, FuturisticGold),
            RadialMenuItem("loans", "Loans", Icons.Default.AccountBalanceWallet, FuturisticBlue),
        )
    }

    val bottomMenuActions = remember {
        listOf(
            RadialMenuItem("insights", "Insights", Icons.Default.AutoAwesome, FuturisticPurple),
            RadialMenuItem("shopping", "Shopping", Icons.Default.ShoppingCart, FuturisticCyan),
            RadialMenuItem("export", "Export", Icons.Default.FileUpload, FuturisticOrange),
            RadialMenuItem("notifications", "Notifications", Icons.Default.Notifications, FuturisticPink),
            RadialMenuItem("chat", "AI Chat", Icons.AutoMirrored.Filled.Chat, FuturisticGold),
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
        if (backPressedOnce) onExit() else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // Main UI - Vertical Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBase,
                        Color(0xFF0D1526),
                        Color(0xFF0F1B2E),
                        DarkBase
                    )
                )
            )
    ) {
        // Animated background particles
        FuturisticParticleBackground(glowPulse)

        // Adaptive spacing
        val isSmallScreen = screenHeight < 700.dp
        val sectionSpacing = if (isSmallScreen) 12.dp else 24.dp
        val itemSpacing = if (isSmallScreen) 4.dp else 8.dp

        // Main scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = if (isSmallScreen) 8.dp else 16.dp)
        ) {
            // ═══════════════════════════════════════════════════════════
            // TOP BAR
            // ═══════════════════════════════════════════════════════════
            item {
                FuturisticTopBar(
                    displayName = displayName,
                    greeting = greeting,
                    unreadNotifications = unreadNotificationCount,
                    onNotificationsClick = onNavigateToNotifications,
                    onProfileClick = onNavigateToProfile,
                    onSettingsClick = onNavigateToSettings
                )
                Spacer(modifier = Modifier.height(sectionSpacing))
            }

            // ═══════════════════════════════════════════════════════════
            // TOP CIRCULAR MENU (Context Menu - Current)
            // ═══════════════════════════════════════════════════════════
            item {
                CircularRadialMenu(
                    title = "QUICK ACTIONS",
                    actions = topMenuActions,
                    summary = summary,
                    glowPulse = glowPulse,
                    ringRotation = ringRotation,
                    onActionClick = { actionId ->
                        when (actionId) {
                            "income" -> activeModal = DashboardModal.INCOME
                            "expense" -> activeModal = DashboardModal.EXPENSES
                            "bills" -> activeModal = DashboardModal.BILLS
                            "budget" -> activeModal = DashboardModal.BUDGET
                            "goals" -> activeModal = DashboardModal.GOALS
                            "loans" -> activeModal = DashboardModal.LOANS
                        }
                    }
                )
                Spacer(modifier = Modifier.height(itemSpacing))
            }

            // ═══════════════════════════════════════════════════════════
            // GLOWING BRIDGE - TOP TO CENTER (Shorter & Thicker)
            // ═══════════════════════════════════════════════════════════
            item {
                GlowingBridge(
                    glowPulse = glowPulse,
                    isTopBridge = true
                )
                Spacer(modifier = Modifier.height(itemSpacing))
            }

            // ═══════════════════════════════════════════════════════════
            // CENTER PANEL - GRAPHS & VISUALIZATIONS
            // ═══════════════════════════════════════════════════════════
            item {
                CentralDataPanel(
                    summary = summary,
                    goalsSummary = goalsSummary,
                    loanSummary = loanSummary,
                    expenses = currentExpenses,
                    unpaidBillsCount = unpaidBills.size,
                    glowPulse = glowPulse,
                    onOpenModal = { modal -> activeModal = modal }
                )
                Spacer(modifier = Modifier.height(itemSpacing))
            }

            // ═══════════════════════════════════════════════════════════
            // GLOWING BRIDGE - CENTER TO BOTTOM (Shorter & Thicker)
            // ═══════════════════════════════════════════════════════════
            item {
                GlowingBridge(
                    glowPulse = glowPulse,
                    isTopBridge = false
                )
                Spacer(modifier = Modifier.height(itemSpacing))
            }

            // ═══════════════════════════════════════════════════════════
            // BOTTOM CIRCULAR MENU (Other Menus)
            // ═══════════════════════════════════════════════════════════
            item {
                CircularRadialMenu(
                    title = "MORE OPTIONS",
                    actions = bottomMenuActions,
                    summary = summary,
                    glowPulse = glowPulse,
                    ringRotation = -ringRotation, // Rotate in opposite direction
                    onActionClick = { actionId ->
                        when (actionId) {
                            "insights" -> activeModal = DashboardModal.INSIGHTS
                            "shopping" -> activeModal = DashboardModal.SHOPPING
                            "export" -> activeModal = DashboardModal.EXPORT
                            "notifications" -> onNavigateToNotifications()
                            "chat" -> onNavigateToAIChat()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(if (isSmallScreen) 60.dp else 100.dp))
            }
        }

        // ═══════════════════════════════════════════════════════════
        // MODAL POPUPS - Render on top of dashboard
        // ═══════════════════════════════════════════════════════════

        // Income Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.INCOME,
            onDismiss = { activeModal = null },
            title = "Income Management",
            accentColor = FuturisticEmerald,
            maxHeightFraction = 0.85f
        ) {
            IncomeScreen(
                viewModel = viewModel,
                onAddIncome = { /* Inline add */ },
                onBack = { activeModal = null }
            )
        }

        // Expenses Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.EXPENSES,
            onDismiss = { activeModal = null },
            title = "Expense Tracker",
            accentColor = FuturisticPink
        ) {
            ExpensesScreen(
                viewModel = viewModel,
                onAddExpense = { /* Inline add */ },
                onBack = { activeModal = null }
            )
        }

        // Bills Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.BILLS,
            onDismiss = { activeModal = null },
            title = "Bills Management",
            accentColor = FuturisticBlue
        ) {
            BillsScreen(
                viewModel = viewModel,
                onAddBill = { /* Inline add */ },
                onBack = { activeModal = null }
            )
        }

        // Budget Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.BUDGET,
            onDismiss = { activeModal = null },
            title = "Budget Planner",
            accentColor = FuturisticCyan
        ) {
            BudgetScreen(
                viewModel = viewModel,
                onAddBudget = { /* Inline add */ },
                onBack = { activeModal = null }
            )
        }

        // Goals Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.GOALS,
            onDismiss = { activeModal = null },
            title = "Financial Goals",
            accentColor = FuturisticGold
        ) {
            val goals by viewModel.goals.collectAsState()
            GoalsScreen(
                goals = goals,
                onAddGoal = { viewModel.addGoal(it) },
                onUpdateGoal = { viewModel.updateGoal(it) },
                onDeleteGoal = { viewModel.deleteGoal(it) },
                onAddContribution = { goalId, amount -> viewModel.addGoalContribution(goalId, amount) },
                onNavigateBack = { activeModal = null }
            )
        }

        // Loans Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.LOANS,
            onDismiss = { activeModal = null },
            title = "Loan Portfolio",
            accentColor = FuturisticPurple
        ) {
            val loans by viewModel.loans.collectAsState()
            val loanPayments by viewModel.loanPayments.collectAsState()
            val loanSummary by viewModel.loanSummary.collectAsState()
            LoansScreen(
                loans = loans,
                loanPayments = loanPayments,
                loanSummary = loanSummary,
                financialSummary = summary,
                onAddLoan = { viewModel.addLoan(it) },
                onUpdateLoan = { viewModel.updateLoan(it) },
                onDeleteLoan = { viewModel.deleteLoan(it) },
                onAddPayment = { loanId, amount, ref -> viewModel.addLoanPayment(loanId, amount, ref) },
                onNavigateBack = { activeModal = null }
            )
        }

        // Shopping Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.SHOPPING,
            onDismiss = { activeModal = null },
            title = "Shopping Lists",
            accentColor = FuturisticOrange
        ) {
            val shoppingLists by viewModel.shoppingListsWithItems.collectAsState()
            ShoppingListScreen(
                shoppingLists = shoppingLists,
                monthlyIncome = summary.totalIncome,
                monthlyExpenses = summary.totalExpenses,
                onCreateList = { viewModel.createShoppingList(it) },
                onUpdateList = { viewModel.updateShoppingList(it) },
                onDeleteList = { viewModel.deleteShoppingList(it) },
                onAddItem = { viewModel.addShoppingItem(it) },
                onUpdateItem = { viewModel.updateShoppingItem(it) },
                onDeleteItem = { viewModel.deleteShoppingItem(it) },
                onAnalyzeList = { listId -> viewModel.analyzeShoppingList(listId) },
                onAddToBudget = { listId, amount -> viewModel.addShoppingListToBudget(listId, amount) },
                onNavigateBack = { activeModal = null }
            )
        }

        // Insights Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.INSIGHTS,
            onDismiss = { activeModal = null },
            title = "AI Insights",
            accentColor = FuturisticPurple
        ) {
            InsightsScreen(
                viewModel = viewModel,
                onBack = { activeModal = null }
            )
        }

        // Export Modal
        MagicalModal(
            isVisible = activeModal == DashboardModal.EXPORT,
            onDismiss = { activeModal = null },
            title = "Export Data",
            accentColor = FuturisticGold
        ) {
            ExportScreen(
                viewModel = viewModel,
                userName = userName,
                onBack = { activeModal = null }
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   FUTURISTIC TOP BAR
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun FuturisticTopBar(
    displayName: String,
    greeting: String,
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo and greeting
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "Budgie",
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = greeting,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Notifications
            IconButton(onClick = onNotificationsClick) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(containerColor = FuturisticPink) {
                                Text(
                                    text = if (unreadNotifications > 99) "99+" else unreadNotifications.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }

            // Settings
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   CIRCULAR RADIAL MENU (Used for both top and bottom circles)
   Fully responsive and centered on all screen sizes
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun CircularRadialMenu(
    title: String,
    actions: List<RadialMenuItem>,
    summary: FinancialSummary,
    glowPulse: Float,
    ringRotation: Float,
    onActionClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Adaptive sizing: min 250dp, max 340dp, 80% of screen width
    val circleSize = minOf(maxOf(screenWidth.value * 0.80f, 250f), 340f)
    val isSmallScreen = screenHeight < 700.dp || screenWidth < 400.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(circleSize.dp)
                .padding(if (isSmallScreen) 8.dp else 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer ring - Animated (adaptive border width)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(ringRotation * 0.25f)
                    .border(
                        width = if (isSmallScreen) 1.5.dp else 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                FuturisticCyan.copy(alpha = 0f),
                                FuturisticCyan.copy(alpha = 0.7f),
                                FuturisticEmerald.copy(alpha = 0.7f),
                                FuturisticCyan.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Inner ring - Counter-rotating (adaptive border width)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .rotate(-ringRotation * 0.35f)
                    .border(
                        width = if (isSmallScreen) 1.5.dp else 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                FuturisticPurple.copy(alpha = 0f),
                                FuturisticPink.copy(alpha = 0.6f),
                                FuturisticOrange.copy(alpha = 0.6f),
                                FuturisticPurple.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Radial action items
            val radius = circleSize * 0.36f
            actions.forEachIndexed { index, item ->
                val angle = (360.0 / actions.size) * index - 90
                RadialActionItem(
                    item = item,
                    angle = angle,
                    radius = radius,
                    index = index,
                    glowPulse = glowPulse,
                    onClick = { onActionClick(item.id) }
                )
            }

            // Center hub
            CenterFinancialHub(
                summary = summary,
                glowPulse = glowPulse,
                ringRotation = ringRotation
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   RADIAL ACTION ITEM (3D Button)
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadialActionItem(
    item: RadialMenuItem,
    angle: Double,
    radius: Float,
    index: Int,
    glowPulse: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radial_${item.id}")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + (index * 100), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_${item.id}"
    )

    val angleRad = angle * PI / 180.0
    val x = (cos(angleRad) * radius).toFloat()
    val y = (sin(angleRad) * radius).toFloat()

    Box(
        modifier = Modifier
            .offset(x = x.dp, y = (y + floatOffset).dp)
            .graphicsLayer {
                rotationX = (y / radius) * 8f
                rotationY = -(x / radius) * 8f
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 3D Button
            Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                // Shadow
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = 3.dp)
                        .blur(6.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                )

                // Glow
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .blur(10.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    item.color.copy(alpha = 0.5f * glowPulse),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Button
                Card(
                    onClick = onClick,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(8.dp, CircleShape, ambientColor = item.color.copy(alpha = 0.3f)),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(item.color, item.color.copy(alpha = 0.7f))
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Label
            Text(
                text = item.label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   CENTER FINANCIAL HUB
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun CenterFinancialHub(
    summary: FinancialSummary,
    glowPulse: Float,
    ringRotation: Float
) {
    val netSavings = summary.totalIncome - summary.totalExpenses
    val isPositive = netSavings >= 0

    Box(
        modifier = Modifier.size(115.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating ring around center
        Box(
            modifier = Modifier
                .size(113.dp)
                .rotate(ringRotation * 0.5f)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FuturisticCyan.copy(alpha = 0f),
                            FuturisticCyan.copy(alpha = 0.8f),
                            FuturisticEmerald.copy(alpha = 0.8f),
                            FuturisticCyan.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Counter-rotating inner ring
        Box(
            modifier = Modifier
                .size(102.dp)
                .rotate(-ringRotation * 0.4f)
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FuturisticPurple.copy(alpha = 0f),
                            FuturisticPurple.copy(alpha = 0.7f),
                            FuturisticPink.copy(alpha = 0.7f),
                            FuturisticPurple.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Glow
        Box(
            modifier = Modifier
                .size(96.dp)
                .blur(14.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isPositive) FuturisticEmerald else FuturisticRed).copy(alpha = 0.4f * glowPulse),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main hub
        Surface(
            modifier = Modifier
                .size(92.dp)
                .shadow(12.dp, CircleShape),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E3A5F),
                                Color(0xFF0D1B2A)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                FuturisticCyan.copy(alpha = 0.5f),
                                FuturisticEmerald.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "NET",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatCurrency(netSavings),
                        color = if (isPositive) FuturisticEmerald else FuturisticRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isPositive) FuturisticEmerald else FuturisticRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   CENTRAL DATA PANEL - COMPACT VISUALIZATIONS ONLY (6 items in 2 rows)
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun CentralDataPanel(
    summary: FinancialSummary,
    goalsSummary: GoalsSummary,
    loanSummary: LoanSummary,
    expenses: List<com.example.budgie.data.model.Expense>,
    unpaidBillsCount: Int,
    glowPulse: Float,
    onOpenModal: (DashboardModal) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Match circle diameter: 80% of screen width, max 340dp, min 280dp
    val panelWidth = minOf(maxOf(screenWidth.value * 0.80f, 280f), 340f).dp

    // Adaptive sizing based on screen size
    val isSmallScreen = screenHeight < 700.dp
    val rowHeight = if (isSmallScreen) 70.dp else 90.dp
    val totalHeight = rowHeight * 2 + 12.dp // Two rows + spacing

    Column(
        modifier = Modifier
            .width(panelWidth)
            .height(totalHeight)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // First Row - 3 visualizations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Financial Health Mini Graph - Opens INSIGHTS modal
            CompactVisualizationCard(
                title = "Health",
                progress = if (summary.totalIncome > 0) {
                    ((summary.totalIncome - summary.totalExpenses) / summary.totalIncome).toFloat().coerceIn(0f, 1f)
                } else 0f,
                color = FuturisticEmerald,
                glowPulse = glowPulse,
                onClick = { onOpenModal(DashboardModal.INSIGHTS) },
                modifier = Modifier.weight(1f),
                isSmallScreen = isSmallScreen
            )

            // 2. Goals Progress Mini Graph - Opens GOALS modal
            CompactVisualizationCard(
                title = "Goals",
                progress = (goalsSummary.overallProgress / 100.0).toFloat().coerceIn(0f, 1f),
                color = FuturisticGold,
                glowPulse = glowPulse,
                onClick = { onOpenModal(DashboardModal.GOALS) },
                modifier = Modifier.weight(1f),
                isSmallScreen = isSmallScreen
            )

            // 3. Loans Progress Mini Graph - Opens LOANS modal
            CompactVisualizationCard(
                title = "Loans",
                progress = if (loanSummary.totalLoans > 0) {
                    val total = loanSummary.totalRepaid + loanSummary.totalRemaining
                    if (total > 0) (loanSummary.totalRepaid / total).toFloat() else 0f
                } else 0f,
                color = FuturisticBlue,
                glowPulse = glowPulse,
                onClick = { onOpenModal(DashboardModal.LOANS) },
                modifier = Modifier.weight(1f),
                isSmallScreen = isSmallScreen
            )
        }

        // Second Row - 3 more visualizations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 4. Savings Rate - Opens BUDGET modal
            CompactVisualizationCard(
                title = "Savings",
                progress = if (summary.totalIncome > 0) {
                    (summary.netSavings / summary.totalIncome).toFloat().coerceIn(0f, 1f)
                } else 0f,
                color = FuturisticPurple,
                glowPulse = glowPulse,
                onClick = { onOpenModal(DashboardModal.BUDGET) },
                modifier = Modifier.weight(1f),
                isSmallScreen = isSmallScreen
            )

            // 5. Bills Status - Opens BILLS modal
            CompactVisualizationCard(
                title = "Bills",
                progress = if (unpaidBillsCount > 0) 0.3f else 1.0f, // Show as incomplete if unpaid bills exist
                color = if (unpaidBillsCount > 0) FuturisticOrange else FuturisticCyan,
                glowPulse = glowPulse,
                onClick = { onOpenModal(DashboardModal.BILLS) },
                modifier = Modifier.weight(1f),
                isSmallScreen = isSmallScreen
            )

            // 6. Monthly Trend - Opens EXPENSES modal
            CompactVisualizationCard(
                title = "Trend",
                progress = if (summary.totalIncome > 0 && summary.totalExpenses > 0) {
                    val efficiency = (summary.totalIncome - summary.totalExpenses) / summary.totalIncome
                    efficiency.toFloat().coerceIn(0f, 1f)
                } else 0.5f,
                color = FuturisticPink,
                glowPulse = glowPulse,
                onClick = { onOpenModal(DashboardModal.EXPENSES) },
                modifier = Modifier.weight(1f),
                isSmallScreen = isSmallScreen
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   COMPACT VISUALIZATION CARD - Only circular ring, no background box
   Fully responsive based on screen size
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactVisualizationCard(
    title: String,
    progress: Float,
    color: Color,
    glowPulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSmallScreen: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz_$title")

    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Adaptive sizing
    val circleSize = if (isSmallScreen) 45.dp else 55.dp
    val strokeWidth = if (isSmallScreen) 5.dp else 6.dp
    val titleFontSize = if (isSmallScreen) 9.sp else 10.sp
    val progressFontSize = if (isSmallScreen) 11.sp else 13.sp

    // Clickable container - NO BACKGROUND
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            // Circular progress indicator (adaptive size)
            Box(
                modifier = Modifier.size(circleSize),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = color.copy(alpha = 0.2f),
                        radius = size.minDimension / 2,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.toPx())
                    )
                }

                // Progress circle with glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = color.copy(alpha = 0.9f * glowPulse),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                // Glow effect
                Canvas(modifier = Modifier.fillMaxSize().blur(8.dp)) {
                    drawArc(
                        color = color.copy(alpha = 0.6f * glowPulse),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 8.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                // Percentage text (adaptive)
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = progressFontSize,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 2.dp else 4.dp))

            // Title below the circle (adaptive)
            Text(
                text = title,
                color = color.copy(alpha = 0.8f),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Medium,
                letterSpacing = if (isSmallScreen) 0.5.sp else 1.sp,
                maxLines = 1
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   HELPER COMPOSABLES
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinancialMetricCard(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    glowPulse: Float,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {},
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            color.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    1.dp,
                    color.copy(alpha = 0.4f * glowPulse),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = color.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressCard(
    label: String,
    progress: Float,
    info: String,
    color: Color,
    glowPulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(85.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCard)
                .border(1.dp, color.copy(alpha = 0.3f * glowPulse), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text(text = info, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    message: String,
    color: Color,
    icon: ImageVector,
    glowPulse: Float
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, color.copy(alpha = 0.5f * glowPulse), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QuickStatsRow(
    summary: FinancialSummary,
    goalsSummary: GoalsSummary,
    loanSummary: LoanSummary,
    glowPulse: Float
) {
    val savingsRate = if (summary.totalIncome > 0) {
        ((summary.totalIncome - summary.totalExpenses) / summary.totalIncome * 100).toInt()
    } else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickStatItem("Savings Rate", "$savingsRate%", FuturisticEmerald, glowPulse)
        QuickStatItem("Active Goals", "${goalsSummary.activeGoals}", FuturisticGold, glowPulse)
        QuickStatItem("Active Loans", "${loanSummary.activeLoans}", FuturisticBlue, glowPulse)
    }
}

@Composable
private fun QuickStatItem(
    label: String,
    value: String,
    color: Color,
    glowPulse: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                color.copy(alpha = 0.3f * glowPulse),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SpendingTrendChart(
    expenses: List<com.example.budgie.data.model.Expense>,
    glowPulse: Float
) {
    val last7Days = remember(expenses) {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        val sevenDaysAgo = today - (7 * 24 * 60 * 60 * 1000)

        expenses.filter { it.date >= sevenDaysAgo }
            .groupBy {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.date
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries
            .sortedBy { it.key }
            .take(7)
    }

    if (last7Days.isNotEmpty()) {
        val maxAmount = last7Days.maxOfOrNull { it.value } ?: 1000.0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    FuturisticCyan.copy(alpha = 0.3f * glowPulse),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "7-Day Spending Trend",
                color = FuturisticCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Simple bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                last7Days.forEachIndexed { index, entry ->
                    val barHeight = if (maxAmount > 0) {
                        ((entry.value / maxAmount) * 120).dp
                    } else 0.dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Amount label
                        if (barHeight > 20.dp) {
                            Text(
                                text = "${entry.value.toInt()}",
                                color = FuturisticCyan.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(barHeight.coerceAtLeast(4.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            FuturisticCyan,
                                            FuturisticCyan.copy(alpha = 0.6f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                                .shadow(4.dp, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Day label
                        Text(
                            text = "D${index + 1}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   ANIMATED BACKGROUND
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun FuturisticParticleBackground(glowPulse: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // Create subtle floating particles
    repeat(8) { i ->
        val offsetX by infiniteTransition.animateFloat(
            initialValue = (i * 50).toFloat(),
            targetValue = (i * 50 + 100).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(5000 + (i * 500), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particle_x_$i"
        )

        val offsetY by infiniteTransition.animateFloat(
            initialValue = (i * 80).toFloat(),
            targetValue = (i * 80 + 50).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4000 + (i * 300), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particle_y_$i"
        )

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .size((6 + i).dp)
                .blur(8.dp)
                .background(
                    color = listOf(FuturisticCyan, FuturisticEmerald, FuturisticPurple)[i % 3]
                        .copy(alpha = 0.08f * glowPulse),
                    shape = CircleShape
                )
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════
   GLOWING BRIDGE CONNECTOR (Shorter & Thicker) - Adaptive
═══════════════════════════════════════════════════════════════════ */

@Composable
private fun GlowingBridge(
    glowPulse: Float,
    isTopBridge: Boolean
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isSmallScreen = screenHeight < 700.dp

    val infiniteTransition = rememberInfiniteTransition(label = "bridge")

    // Animated gradient flow
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_flow"
    )

    // Pulsing width
    val pulseWidth by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_width"
    )

    // Adaptive dimensions
    val bridgeHeight = if (isSmallScreen) 20.dp else 30.dp
    val bridgeBaseWidth = if (isSmallScreen) 5.dp else 6.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bridgeHeight),
        contentAlignment = Alignment.Center
    ) {
        // Main bridge connector (Thicker, adaptive)
        Box(
            modifier = Modifier
                .width((bridgeBaseWidth.value * pulseWidth).dp)
                .height(bridgeHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isTopBridge) {
                            listOf(
                                FuturisticCyan.copy(alpha = 0.7f * glowPulse),
                                FuturisticEmerald.copy(alpha = 0.9f * glowPulse),
                                FuturisticPurple.copy(alpha = 0.7f * glowPulse),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                FuturisticPurple.copy(alpha = 0.7f * glowPulse),
                                FuturisticPink.copy(alpha = 0.9f * glowPulse),
                                FuturisticOrange.copy(alpha = 0.7f * glowPulse)
                            )
                        },
                        startY = if (gradientOffset < 0.5f) 0f else Float.POSITIVE_INFINITY,
                        endY = if (gradientOffset < 0.5f) Float.POSITIVE_INFINITY else 0f
                    )
                )
                .blur(10.dp)
        )

        // Outer glow effect
        Box(
            modifier = Modifier
                .width((10 * pulseWidth).dp)
                .height(30.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isTopBridge) {
                            listOf(
                                FuturisticCyan.copy(alpha = 0.3f * glowPulse),
                                FuturisticEmerald.copy(alpha = 0.4f * glowPulse),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                FuturisticPink.copy(alpha = 0.4f * glowPulse),
                                FuturisticOrange.copy(alpha = 0.3f * glowPulse)
                            )
                        }
                    )
                )
                .blur(16.dp)
        )

        // Energy particles flowing through
        repeat(4) { index ->
            val delay = index * 200L
            val nodeOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 30f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing, delayMillis = delay.toInt()),
                    repeatMode = RepeatMode.Restart
                ),
                label = "node_$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = nodeOffset.dp)
                    .background(
                        color = listOf(
                            FuturisticCyan,
                            FuturisticEmerald,
                            FuturisticPurple,
                            FuturisticPink
                        )[index].copy(alpha = 0.95f * glowPulse),
                        shape = CircleShape
                    )
                    .blur(8.dp)
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
   HELPER FUNCTIONS & DATA CLASSES
═══════════════════════════════════════════════════════════════════ */

private data class RadialMenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

private fun formatCurrency(amount: Double): String {
    return if (amount >= 0) {
        "KES ${String.format(Locale.US, "%,.0f", amount)}"
    } else {
        "-KES ${String.format(Locale.US, "%,.0f", -amount)}"
    }
}

