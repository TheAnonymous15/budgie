package com.example.budgie.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.budgie.data.model.*
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment as PdfTextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

// Premium Colors
private val WealthNavy = Color(0xFF0B1F2A)
private val WealthNavyLight = Color(0xFF0F2D3D)
private val WealthEmerald = Color(0xFF0FAE96)
private val WealthTeal = Color(0xFF0B8F7A)
private val WealthGold = Color(0xFFC9A14A)
private val WealthSoftWhite = Color(0xFFE6F1F0)
private val WealthMutedRed = Color(0xFFE57373)
private val WealthAmber = Color(0xFFFFB74D)
private val WealthBlue = Color(0xFF5C9CE5)
private val WealthCyan = Color(0xFF26A69A)
private val WealthPurple = Color(0xFF9575CD)

// Glassmorphism Colors
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassHighlight = Color.White.copy(alpha = 0.15f)

// Glassmorphism Card Modifier
private fun Modifier.glassmorphicCard(
    cornerRadius: Int = 20,
    borderWidth: Float = 1f
) = this
    .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        ambientColor = Color.Black.copy(alpha = 0.3f),
        spotColor = Color.Black.copy(alpha = 0.3f)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassHighlight,
                GlassWhite,
                Color.White.copy(alpha = 0.05f)
            )
        )
    )
    .border(
        width = borderWidth.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                GlassBorder,
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

// Accent Glassmorphism Card Modifier
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
                Color.White.copy(alpha = 0.03f)
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
fun GoalsScreen(
    goals: List<FinancialGoal>,
    currentSavings: Double = 0.0, // User's current savings balance
    monthlyIncome: Double = 0.0, // User's monthly income
    monthlyExpenses: Double = 0.0, // User's monthly expenses
    monthlySavings: Double = 0.0, // User's monthly savings (income - expenses)
    onAddGoal: (FinancialGoal) -> Unit,
    onUpdateGoal: (FinancialGoal) -> Unit,
    onDeleteGoal: (FinancialGoal) -> Unit,
    onAddContribution: (String, Double) -> Unit,
    onStartSaving: (FinancialGoal) -> Unit = {}, // Start saving for future goals
    onAddGoalLoan: (Loan) -> Unit = {}, // Add goal-linked loan
    onNavigateToLoans: ((loanAmount: Double, interestRate: Double, termMonths: Int, interestType: String) -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showContributionDialog by remember { mutableStateOf<FinancialGoal?>(null) }
    var selectedGoalForDetails by remember { mutableStateOf<FinancialGoal?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val tabs = listOf("All", "Short", "Medium", "Long", "Done")

    val filteredGoals = when (selectedTab) {
        1 -> goals.filter { it.goalType == GoalType.SHORT_TERM && !it.isCompleted }
        2 -> goals.filter { it.goalType == GoalType.MEDIUM_TERM && !it.isCompleted }
        3 -> goals.filter { it.goalType == GoalType.LONG_TERM && !it.isCompleted }
        4 -> goals.filter { it.isCompleted }
        else -> goals.filter { !it.isCompleted }
    }

    Scaffold(
        containerColor = WealthNavy,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Budgie Goals",
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        Text(
                            "${goals.count { !it.isCompleted }} active goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WealthSoftWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WealthNavy
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Export FAB
                if (goals.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showExportDialog = true },
                        containerColor = WealthBlue,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.FileUpload, "Export", modifier = Modifier.size(22.dp))
                    }
                }
                // Add Goal FAB
                ExtendedFloatingActionButton(
                    onClick = { showAddGoalDialog = true },
                    containerColor = WealthEmerald,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Goal")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Cards
            GoalsSummarySection(goals = goals.filter { !it.isCompleted })

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = WealthSoftWhite,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) WealthEmerald else WealthSoftWhite.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goals List
            if (filteredGoals.isEmpty()) {
                EmptyGoalsState(selectedTab)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredGoals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onClick = { selectedGoalForDetails = goal },
                            onContribute = { showContributionDialog = goal },
                            onStartSaving = { onStartSaving(goal) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        GoalsExportDialog(
            goals = goals,
            context = context,
            onDismiss = { showExportDialog = false }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        AddGoalDialog(
            currentSavings = currentSavings,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            monthlySavings = monthlySavings,
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { goal ->
                onAddGoal(goal)
                showAddGoalDialog = false
            },
            onAddGoalLoan = onAddGoalLoan,
            onNavigateToLoans = onNavigateToLoans
        )
    }

    // Contribution Dialog
    showContributionDialog?.let { goal ->
        AddContributionDialog(
            goal = goal,
            onDismiss = { showContributionDialog = null },
            onConfirm = { amount ->
                onAddContribution(goal.id, amount)
                showContributionDialog = null
            }
        )
    }

    // Goal Details Dialog
    selectedGoalForDetails?.let { goal ->
        GoalDetailsDialog(
            goal = goal,
            onDismiss = { selectedGoalForDetails = null },
            onContribute = {
                selectedGoalForDetails = null
                showContributionDialog = goal
            },
            onDelete = {
                onDeleteGoal(goal)
                selectedGoalForDetails = null
            }
        )
    }
}

@Composable
private fun GoalsSummarySection(goals: List<FinancialGoal>) {
    val totalTarget = goals.sumOf { it.targetAmount }
    val totalCurrent = goals.sumOf { it.currentAmount }
    val overallProgress = if (totalTarget > 0) (totalCurrent / totalTarget * 100) else 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .glassmorphicAccentCard(WealthEmerald, 24)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(WealthEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Goals Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = WealthSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${String.format("%.1f", overallProgress)}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthEmerald
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "$${String.format("%,.0f", totalTarget)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar with glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(WealthSoftWhite.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((overallProgress / 100).toFloat().coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(WealthEmerald, WealthTeal)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = WealthEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Saved: $${String.format("%,.0f", totalCurrent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "Remaining: $${String.format("%,.0f", totalTarget - totalCurrent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthSoftWhite.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: FinancialGoal,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onStartSaving: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Glassmorphic Goal Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize()
            .glassmorphicCard(18)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon with glow
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = when (goal.goalType) {
                                GoalType.SHORT_TERM -> WealthEmerald
                                GoalType.MEDIUM_TERM -> WealthBlue
                                GoalType.LONG_TERM -> WealthPurple
                            }.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    when (goal.goalType) {
                                        GoalType.SHORT_TERM -> WealthEmerald
                                        GoalType.MEDIUM_TERM -> WealthBlue
                                        GoalType.LONG_TERM -> WealthPurple
                                    }.copy(alpha = 0.3f),
                                    when (goal.goalType) {
                                        GoalType.SHORT_TERM -> WealthEmerald
                                        GoalType.MEDIUM_TERM -> WealthBlue
                                        GoalType.LONG_TERM -> WealthPurple
                                    }.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        goal.category.emoji,
                        fontSize = 26.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoalTypeChip(goal.goalType)
                        if (goal.fundingMethod == FundingMethod.LOAN) {
                            Surface(
                                color = WealthAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Loan",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthAmber
                                )
                            }
                        }
                    }
                }

                // Progress Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { (goal.progressPercentage / 100).toFloat() },
                        modifier = Modifier.fillMaxSize(),
                        color = when {
                            goal.progressPercentage >= 100 -> WealthEmerald
                            goal.progressPercentage >= 50 -> WealthCyan
                            else -> WealthAmber
                        },
                        trackColor = WealthSoftWhite.copy(alpha = 0.1f),
                        strokeWidth = 4.dp
                    )
                    Text(
                        "${goal.progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "$${String.format("%,.0f", goal.currentAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthEmerald
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "$${String.format("%,.0f", goal.targetAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Glassmorphic Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(WealthSoftWhite.copy(alpha = 0.1f))
            ) {
                val progressColor = when {
                    goal.progressPercentage >= 100 -> WealthEmerald
                    goal.progressPercentage >= 50 -> WealthCyan
                    else -> WealthAmber
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((goal.progressPercentage / 100).toFloat().coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    progressColor,
                                    progressColor.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer with glassmorphic button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "Due: ${dateFormat.format(Date(goal.targetDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthSoftWhite.copy(alpha = 0.6f)
                    )
                }

                // Show "Start Now" button for future goals, or "Add" button for active goals
                if (!goal.isCompleted) {
                    if (!goal.isSavingStarted && goal.savingStartDate > System.currentTimeMillis()) {
                        // Future goal - Show "Start Now" button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            WealthCyan.copy(alpha = 0.3f),
                                            WealthCyan.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = WealthCyan.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = onStartSaving)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = WealthCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Start Now",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WealthCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (goal.fundingMethod == FundingMethod.SAVING || goal.fundingMethod == FundingMethod.MIXED) {
                        // Active goal - Show "Add" button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            WealthEmerald.copy(alpha = 0.3f),
                                            WealthEmerald.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = WealthEmerald.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = onContribute)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = WealthEmerald
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Add",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WealthEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalTypeChip(goalType: GoalType) {
    val (color, label) = when (goalType) {
        GoalType.SHORT_TERM -> Pair(WealthEmerald, "Short")
        GoalType.MEDIUM_TERM -> Pair(WealthBlue, "Medium")
        GoalType.LONG_TERM -> Pair(WealthPurple, "Long")
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun EmptyGoalsState(selectedTab: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Flag,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = WealthSoftWhite.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                when (selectedTab) {
                    4 -> "No completed goals yet"
                    else -> "No goals in this category"
                },
                style = MaterialTheme.typography.titleMedium,
                color = WealthSoftWhite.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Start by adding a new financial goal",
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    currentSavings: Double = 0.0,
    monthlyIncome: Double = 0.0,
    monthlyExpenses: Double = 0.0,
    monthlySavings: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (FinancialGoal) -> Unit,
    onAddGoalLoan: (Loan) -> Unit = {},
    onNavigateToLoans: ((loanAmount: Double, interestRate: Double, termMonths: Int, interestType: String) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var selectedGoalType by remember { mutableStateOf(GoalType.SHORT_TERM) }
    var selectedCategory by remember { mutableStateOf(GoalCategory.OTHER) }
    var selectedFundingMethod by remember { mutableStateOf(FundingMethod.SAVING) }
    var selectedSavingFrequency by remember { mutableStateOf(SavingFrequency.MONTHLY) }
    var isVariableSaving by remember { mutableStateOf(false) }
    var savingAmount by remember { mutableStateOf("") }
    var targetMonths by remember { mutableStateOf("6") }

    // Loan Calculator States
    var loanInterestRate by remember { mutableStateOf("12") }
    var loanTermMonths by remember { mutableStateOf("12") }
    var loanCharges by remember { mutableStateOf("0") }
    var selectedInterestType by remember { mutableStateOf("REDUCING_BALANCE") }
    var showLoanCalculator by remember { mutableStateOf(false) }

    // New: Start Date Calendar State
    var savingStartDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // New: Lender Name for Goal Loans
    var showLenderDialog by remember { mutableStateOf(false) }
    var lenderName by remember { mutableStateOf("") }

    // Calculate derived values
    val target = targetAmount.toDoubleOrNull() ?: 0.0
    val loanNeeded = (target - currentSavings).coerceAtLeast(0.0)
    val rate = loanInterestRate.toDoubleOrNull() ?: 0.0
    val termMonths = loanTermMonths.toIntOrNull() ?: 12
    val charges = loanCharges.toDoubleOrNull() ?: 0.0
    val months = targetMonths.toIntOrNull() ?: 6

    // Auto-calculate saving amount based on period
    val calculatedSavingAmount = remember(target, months, selectedSavingFrequency, selectedFundingMethod, loanNeeded, rate, termMonths, charges, selectedInterestType) {
        calculateSavingAmountPerPeriod(
            totalAmount = if (selectedFundingMethod == FundingMethod.LOAN) 0.0
                         else if (selectedFundingMethod == FundingMethod.MIXED) currentSavings.coerceAtMost(target)
                         else target,
            months = months,
            frequency = selectedSavingFrequency,
            fundingMethod = selectedFundingMethod,
            loanMonthlyPayment = if (selectedFundingMethod == FundingMethod.LOAN || selectedFundingMethod == FundingMethod.MIXED) {
                calculateLoan(if (selectedFundingMethod == FundingMethod.MIXED) loanNeeded else target, rate, termMonths, charges, selectedInterestType).monthlyPayment
            } else 0.0
        )
    }

    // AI Probability Calculation
    val goalProbability = remember(target, months, monthlySavings, selectedFundingMethod, calculatedSavingAmount) {
        calculateGoalProbability(
            targetAmount = target,
            months = months,
            monthlySavings = monthlySavings,
            savingPerPeriod = calculatedSavingAmount,
            frequency = selectedSavingFrequency,
            fundingMethod = selectedFundingMethod
        )
    }

    // Loan calculations based on interest type
    val loanCalculation = remember(loanNeeded, rate, termMonths, charges, selectedInterestType, target, selectedFundingMethod) {
        val loanPrincipal = if (selectedFundingMethod == FundingMethod.MIXED) loanNeeded else target
        calculateLoan(loanPrincipal, rate, termMonths, charges, selectedInterestType)
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        // Glassmorphic Dialog Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .glassmorphicCard(28, 1.5f)
        ) {
            // Inner gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WealthNavy.copy(alpha = 0.95f),
                                WealthNavyLight.copy(alpha = 0.9f),
                                WealthNavy.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with glassmorphic styling
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = WealthEmerald.copy(alpha = 0.3f))
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(WealthEmerald, WealthTeal)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Add New Goal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthSoftWhite
                                )
                                Text(
                                    "Set your financial target",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthSoftWhite.copy(alpha = 0.5f)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = WealthSoftWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Goal Title - Glassmorphic Input Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicAccentCard(WealthEmerald, 16)
                            .padding(4.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Goal Title") },
                            placeholder = { Text("e.g., Dream House Fund", color = WealthSoftWhite.copy(alpha = 0.3f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = WealthSoftWhite,
                                unfocusedTextColor = WealthSoftWhite,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = WealthEmerald,
                                unfocusedLabelColor = WealthSoftWhite.copy(alpha = 0.5f),
                                cursorColor = WealthEmerald
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = WealthEmerald.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                // Target Amount & Duration Row - Glassmorphic Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphicCard(14)
                                .padding(4.dp)
                        ) {
                            OutlinedTextField(
                                value = targetAmount,
                                onValueChange = { targetAmount = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Target ($)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WealthSoftWhite,
                                    unfocusedTextColor = WealthSoftWhite,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = WealthEmerald
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Text("$", color = WealthEmerald, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .glassmorphicCard(14)
                                .padding(4.dp)
                        ) {
                            OutlinedTextField(
                                value = targetMonths,
                                onValueChange = { targetMonths = it.filter { c -> c.isDigit() } },
                                label = { Text("Months") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WealthSoftWhite,
                                    unfocusedTextColor = WealthSoftWhite,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = WealthEmerald
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                // Goal Type - Glassmorphic Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(16)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = WealthEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Goal Type", style = MaterialTheme.typography.labelLarge, color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoalType.entries.forEach { type ->
                                    val isSelected = selectedGoalType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) WealthEmerald.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) WealthEmerald else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedGoalType = type }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            type.displayName.split(" ")[0],
                                            fontSize = 12.sp,
                                            color = if (isSelected) WealthEmerald else WealthSoftWhite.copy(alpha = 0.6f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Category - Glassmorphic Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(16)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = WealthGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Category", style = MaterialTheme.typography.labelLarge, color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(GoalCategory.entries.take(8).toList()) { category ->
                                    val isSelected = selectedCategory == category
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) WealthGold.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) WealthGold else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedCategory = category }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            "${category.emoji} ${category.displayName.take(8)}",
                                            fontSize = 11.sp,
                                            color = if (isSelected) WealthGold else WealthSoftWhite.copy(alpha = 0.6f),
                                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Funding Method - Glassmorphic Cards
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(16)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = WealthCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("How will you fund this goal?", style = MaterialTheme.typography.labelLarge, color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FundingMethod.entries.forEach { method ->
                                    val isSelected = selectedFundingMethod == method
                                    val methodColor = when (method) {
                                        FundingMethod.SAVING -> WealthEmerald
                                        FundingMethod.LOAN -> WealthBlue
                                        FundingMethod.MIXED -> WealthCyan
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .shadow(
                                                if (isSelected) 6.dp else 0.dp,
                                                RoundedCornerShape(14.dp),
                                                ambientColor = methodColor.copy(alpha = 0.3f)
                                            )
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected)
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            methodColor.copy(alpha = 0.25f),
                                                            methodColor.copy(alpha = 0.1f)
                                                        )
                                                    )
                                                else
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0.08f),
                                                            Color.White.copy(alpha = 0.03f)
                                                        )
                                                    )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) methodColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                selectedFundingMethod = method
                                                if (method == FundingMethod.MIXED || method == FundingMethod.LOAN) {
                                                    showLoanCalculator = true
                                                }
                                            }
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                when (method) {
                                                    FundingMethod.SAVING -> Icons.Default.Savings
                                                    FundingMethod.LOAN -> Icons.Default.AccountBalance
                                                    FundingMethod.MIXED -> Icons.Default.SwapHoriz
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) methodColor else WealthSoftWhite.copy(alpha = 0.5f),
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                when (method) {
                                                    FundingMethod.SAVING -> "Save"
                                                    FundingMethod.LOAN -> "Loan"
                                                    FundingMethod.MIXED -> "Both"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) methodColor else WealthSoftWhite.copy(alpha = 0.6f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mixed Funding Breakdown - Glassmorphic
                if (selectedFundingMethod == FundingMethod.MIXED && target > 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicAccentCard(WealthCyan, 16)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PieChart, contentDescription = null, tint = WealthCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Funding Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WealthCyan)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Your Savings", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                                        Text("$${String.format("%,.0f", currentSavings)}", style = MaterialTheme.typography.titleMedium, color = WealthEmerald, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Loan Needed", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                                        Text("$${String.format("%,.0f", loanNeeded)}", style = MaterialTheme.typography.titleMedium, color = WealthAmber, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Visual breakdown bar
                                val savingsPercent = if (target > 0) (currentSavings / target).coerceIn(0.0, 1.0) else 0.0
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(WealthAmber.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(savingsPercent.toFloat())
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(WealthEmerald, WealthTeal)
                                                )
                                            )
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${(savingsPercent * 100).toInt()}% from savings", style = MaterialTheme.typography.labelSmall, color = WealthEmerald)
                                    Text("${((1 - savingsPercent) * 100).toInt()}% from loan", style = MaterialTheme.typography.labelSmall, color = WealthAmber)
                                }
                            }
                        }
                    }
                }

                // Loan Calculator - Glassmorphic (for LOAN and MIXED)
                if ((selectedFundingMethod == FundingMethod.LOAN || selectedFundingMethod == FundingMethod.MIXED) && showLoanCalculator) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicAccentCard(WealthBlue, 16)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WealthBlue.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Calculate, contentDescription = null, tint = WealthBlue, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Loan Calculator", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WealthBlue)
                                }

                                // Interest Type Selection - Glassmorphic chips
                                Text("Interest Type", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("REDUCING_BALANCE" to "Reducing", "FLAT_RATE" to "Flat", "NO_INTEREST" to "None").forEach { (type, label) ->
                                        val isSelected = selectedInterestType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) WealthBlue.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, if (isSelected) WealthBlue else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .clickable { selectedInterestType = type }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 11.sp, color = if (isSelected) WealthBlue else WealthSoftWhite.copy(alpha = 0.6f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }

                                // Loan inputs - Glassmorphic mini cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).glassmorphicCard(12).padding(4.dp)) {
                                        OutlinedTextField(
                                            value = loanInterestRate,
                                            onValueChange = { loanInterestRate = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text("Rate %", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = WealthSoftWhite,
                                                unfocusedTextColor = WealthSoftWhite,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            ),
                                            singleLine = true,
                                            enabled = selectedInterestType != "NO_INTEREST"
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f).glassmorphicCard(12).padding(4.dp)) {
                                        OutlinedTextField(
                                            value = loanTermMonths,
                                            onValueChange = { loanTermMonths = it.filter { c -> c.isDigit() } },
                                            label = { Text("Term", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = WealthSoftWhite,
                                                unfocusedTextColor = WealthSoftWhite,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            ),
                                            singleLine = true
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f).glassmorphicCard(12).padding(4.dp)) {
                                        OutlinedTextField(
                                            value = loanCharges,
                                            onValueChange = { loanCharges = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text("Fees", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = WealthSoftWhite,
                                                unfocusedTextColor = WealthSoftWhite,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }

                                HorizontalDivider(color = WealthSoftWhite.copy(alpha = 0.1f))

                                // Loan Calculation Results
                                val loanPrincipal = if (selectedFundingMethod == FundingMethod.MIXED) loanNeeded else target
                                val calcResult = calculateLoan(loanPrincipal, rate, termMonths, charges, selectedInterestType)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Loan Principal:", style = MaterialTheme.typography.bodySmall, color = WealthSoftWhite.copy(alpha = 0.7f))
                                        Text("$${String.format("%,.0f", loanPrincipal)}", style = MaterialTheme.typography.bodySmall, color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Interest:", style = MaterialTheme.typography.bodySmall, color = WealthSoftWhite.copy(alpha = 0.7f))
                                        Text("$${String.format("%,.0f", calcResult.totalInterest)}", style = MaterialTheme.typography.bodySmall, color = WealthAmber, fontWeight = FontWeight.Medium)
                                    }
                                    HorizontalDivider(color = WealthSoftWhite.copy(alpha = 0.1f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Repayment:", style = MaterialTheme.typography.bodyMedium, color = WealthSoftWhite, fontWeight = FontWeight.Bold)
                                        Text("$${String.format("%,.0f", calcResult.totalAmount)}", style = MaterialTheme.typography.bodyMedium, color = WealthMutedRed, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Monthly Payment:", style = MaterialTheme.typography.titleSmall, color = WealthBlue, fontWeight = FontWeight.Bold)
                                        Text("$${String.format("%,.0f", calcResult.monthlyPayment)}", style = MaterialTheme.typography.titleMedium, color = WealthBlue, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Add Loan Button - Glassmorphic
                                if (onNavigateToLoans != null && loanPrincipal > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(WealthBlue, WealthCyan)
                                                )
                                            )
                                            .clickable {
                                                onNavigateToLoans(loanPrincipal, rate, termMonths, selectedInterestType)
                                                onDismiss()
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add This Loan", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Saving Options - Glassmorphic (for SAVING and MIXED)
                if (selectedFundingMethod == FundingMethod.SAVING || selectedFundingMethod == FundingMethod.MIXED) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicAccentCard(WealthEmerald, 16)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Savings, contentDescription = null, tint = WealthEmerald, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving Plan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WealthEmerald)
                                }

                                // Frequency chips - Glassmorphic
                                Text("Saving Frequency", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(SavingFrequency.entries.take(4).toList()) { freq ->
                                        val isSelected = selectedSavingFrequency == freq
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) WealthEmerald.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, if (isSelected) WealthEmerald else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .clickable { selectedSavingFrequency = freq }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(freq.displayName.take(7), fontSize = 11.sp, color = if (isSelected) WealthEmerald else WealthSoftWhite.copy(alpha = 0.6f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }

                                // Auto-calculated saving amount display
                                if (calculatedSavingAmount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(WealthEmerald.copy(alpha = 0.15f))
                                            .border(1.dp, WealthEmerald.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    "Amount per ${selectedSavingFrequency.displayName.lowercase()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = WealthSoftWhite.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    "$${String.format("%,.2f", calculatedSavingAmount)}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WealthEmerald
                                                )
                                            }
                                            Icon(
                                                Icons.Default.Calculate,
                                                contentDescription = null,
                                                tint = WealthEmerald.copy(alpha = 0.7f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Variable amount?", color = WealthSoftWhite, style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = isVariableSaving,
                                        onCheckedChange = { isVariableSaving = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = WealthEmerald, checkedTrackColor = WealthEmerald.copy(alpha = 0.5f))
                                    )
                                }

                                if (!isVariableSaving && savingAmount.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .glassmorphicCard(12)
                                            .padding(4.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = savingAmount,
                                            onValueChange = { savingAmount = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text("Custom amount (optional)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = WealthSoftWhite,
                                                unfocusedTextColor = WealthSoftWhite,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                cursorColor = WealthEmerald
                                            ),
                                            singleLine = true,
                                            leadingIcon = {
                                                Text("$", color = WealthEmerald, fontWeight = FontWeight.Bold)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Start Date Picker Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(16)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = WealthCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("When to start saving?", style = MaterialTheme.typography.labelLarge, color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WealthCyan.copy(alpha = 0.15f))
                                    .border(1.dp, WealthCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            if (savingStartDate <= System.currentTimeMillis() + 86400000) "Start Today" else "Start Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WealthSoftWhite.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            dateFormat.format(Date(savingStartDate)),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = WealthCyan
                                        )
                                    }
                                    Icon(
                                        Icons.Default.EditCalendar,
                                        contentDescription = "Pick date",
                                        tint = WealthCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            if (savingStartDate > System.currentTimeMillis() + 86400000) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "💡 This goal will be marked as 'Future' until you start saving",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthSoftWhite.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // AI Probability Insight Card
                if (target > 0 && selectedFundingMethod != FundingMethod.LOAN) {
                    item {
                        val insightColor = when (goalProbability.insightLevel) {
                            "green" -> WealthEmerald
                            "amber" -> WealthAmber
                            else -> WealthMutedRed
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicAccentCard(insightColor, 16)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(insightColor.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = insightColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "AI Goal Analysis",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = insightColor
                                    )
                                }

                                // Probability indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Success Probability",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WealthSoftWhite.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            "${(goalProbability.probability * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = insightColor
                                        )
                                    }

                                    // Circular progress indicator
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { goalProbability.probability.toFloat() },
                                            modifier = Modifier.size(50.dp),
                                            color = insightColor,
                                            trackColor = WealthSoftWhite.copy(alpha = 0.1f),
                                            strokeWidth = 5.dp
                                        )
                                    }
                                }

                                HorizontalDivider(color = WealthSoftWhite.copy(alpha = 0.1f))

                                // Savings percentage insight
                                Text(
                                    goalProbability.insight,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthSoftWhite.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )

                                if (goalProbability.percentageOfSavings > 0 && monthlySavings > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = insightColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "You'll spend ${String.format("%.0f", goalProbability.percentageOfSavings)}% of your monthly savings on this goal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WealthSoftWhite.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons - Glassmorphic
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = WealthSoftWhite.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        }

                        // Add Goal Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = WealthEmerald.copy(alpha = 0.3f))
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(WealthEmerald, WealthTeal)
                                    )
                                )
                                .clickable(enabled = title.isNotBlank() && targetAmount.isNotBlank()) {
                                    // If funding method involves loan, show lender dialog first
                                    if ((selectedFundingMethod == FundingMethod.LOAN || selectedFundingMethod == FundingMethod.MIXED) && lenderName.isBlank()) {
                                        showLenderDialog = true
                                    } else {
                                        // Create and save goal
                                        val amount = targetAmount.toDoubleOrNull() ?: 0.0
                                        val goalMonths = targetMonths.toIntOrNull() ?: 6
                                        val calendar = Calendar.getInstance()
                                        calendar.add(Calendar.MONTH, goalMonths)

                                        val isFutureSaving = savingStartDate > System.currentTimeMillis() + 86400000

                                        val goal = FinancialGoal(
                                            title = title,
                                            description = description,
                                            targetAmount = amount,
                                            goalType = selectedGoalType,
                                            fundingMethod = selectedFundingMethod,
                                            savingFrequency = if (selectedFundingMethod != FundingMethod.LOAN) selectedSavingFrequency else null,
                                            savingAmount = if (isVariableSaving) null else (savingAmount.toDoubleOrNull() ?: calculatedSavingAmount),
                                            calculatedSavingAmount = calculatedSavingAmount,
                                            isVariableSaving = isVariableSaving,
                                            savingStartDate = savingStartDate,
                                            isSavingStarted = !isFutureSaving,
                                            targetDate = calendar.timeInMillis,
                                            category = selectedCategory
                                        )

                                        // If loan is involved, create the loan first
                                        if (selectedFundingMethod == FundingMethod.LOAN || selectedFundingMethod == FundingMethod.MIXED) {
                                            val loanPrincipal = if (selectedFundingMethod == FundingMethod.MIXED) loanNeeded else amount
                                            val calcResult = calculateLoan(loanPrincipal, rate, termMonths, charges, selectedInterestType)

                                            val loanCalendar = Calendar.getInstance()
                                            loanCalendar.add(Calendar.MONTH, termMonths)

                                            val loan = Loan(
                                                title = title,
                                                lenderName = lenderName,
                                                loanType = LoanType.PERSONAL,
                                                principalAmount = loanPrincipal,
                                                interestRate = rate,
                                                interestType = when (selectedInterestType) {
                                                    "FLAT_RATE" -> InterestType.FLAT_RATE
                                                    "NO_INTEREST" -> InterestType.NO_INTEREST
                                                    else -> InterestType.REDUCING_BALANCE
                                                },
                                                totalAmount = calcResult.totalAmount,
                                                monthlyPayment = calcResult.monthlyPayment,
                                                startDate = savingStartDate,
                                                endDate = loanCalendar.timeInMillis,
                                                nextPaymentDate = savingStartDate + (30L * 24 * 60 * 60 * 1000),
                                                isGoalLoan = true,
                                                isLoanActive = !isFutureSaving,
                                                fees = charges,
                                                notes = "Loan for goal: $title"
                                            )
                                            onAddGoalLoan(loan)
                                        }

                                        onConfirm(goal)
                                    }
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Goal", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = savingStartDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Only allow today or future dates
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    return utcTimeMillis >= today
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        savingStartDate = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = WealthEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = WealthSoftWhite)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = WealthNavy
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = WealthNavy,
                    titleContentColor = WealthSoftWhite,
                    headlineContentColor = WealthSoftWhite,
                    weekdayContentColor = WealthSoftWhite.copy(alpha = 0.6f),
                    subheadContentColor = WealthSoftWhite,
                    yearContentColor = WealthSoftWhite,
                    currentYearContentColor = WealthEmerald,
                    selectedYearContainerColor = WealthEmerald,
                    selectedYearContentColor = Color.White,
                    dayContentColor = WealthSoftWhite,
                    selectedDayContainerColor = WealthEmerald,
                    selectedDayContentColor = Color.White,
                    todayContentColor = WealthEmerald,
                    todayDateBorderColor = WealthEmerald
                )
            )
        }
    }

    // Lender Name Dialog
    if (showLenderDialog) {
        AlertDialog(
            onDismissRequest = { showLenderDialog = false },
            containerColor = WealthNavy,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = WealthBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Loan Provider",
                        color = WealthSoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Enter the name of the bank or financial institution providing the loan",
                        color = WealthSoftWhite.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = lenderName,
                        onValueChange = { lenderName = it },
                        label = { Text("Bank/Lender Name") },
                        placeholder = { Text("e.g., ABC Bank", color = WealthSoftWhite.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WealthSoftWhite,
                            unfocusedTextColor = WealthSoftWhite,
                            focusedBorderColor = WealthBlue,
                            unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f),
                            focusedLabelColor = WealthBlue,
                            unfocusedLabelColor = WealthSoftWhite.copy(alpha = 0.5f),
                            cursorColor = WealthBlue
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null, tint = WealthBlue.copy(alpha = 0.7f))
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLenderDialog = false
                        // Now trigger the goal creation
                        val amount = targetAmount.toDoubleOrNull() ?: 0.0
                        val goalMonths = targetMonths.toIntOrNull() ?: 6
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.MONTH, goalMonths)

                        val isFutureSaving = savingStartDate > System.currentTimeMillis() + 86400000

                        val goal = FinancialGoal(
                            title = title,
                            description = description,
                            targetAmount = amount,
                            goalType = selectedGoalType,
                            fundingMethod = selectedFundingMethod,
                            savingFrequency = if (selectedFundingMethod != FundingMethod.LOAN) selectedSavingFrequency else null,
                            savingAmount = if (isVariableSaving) null else (savingAmount.toDoubleOrNull() ?: calculatedSavingAmount),
                            calculatedSavingAmount = calculatedSavingAmount,
                            isVariableSaving = isVariableSaving,
                            savingStartDate = savingStartDate,
                            isSavingStarted = !isFutureSaving,
                            targetDate = calendar.timeInMillis,
                            category = selectedCategory
                        )

                        // Create the loan
                        val loanPrincipal = if (selectedFundingMethod == FundingMethod.MIXED) loanNeeded else amount
                        val calcResult = calculateLoan(loanPrincipal, rate, termMonths, charges, selectedInterestType)

                        val loanCalendar = Calendar.getInstance()
                        loanCalendar.add(Calendar.MONTH, termMonths)

                        val loan = Loan(
                            title = title,
                            lenderName = lenderName,
                            loanType = LoanType.PERSONAL,
                            principalAmount = loanPrincipal,
                            interestRate = rate,
                            interestType = when (selectedInterestType) {
                                "FLAT_RATE" -> InterestType.FLAT_RATE
                                "NO_INTEREST" -> InterestType.NO_INTEREST
                                else -> InterestType.REDUCING_BALANCE
                            },
                            totalAmount = calcResult.totalAmount,
                            monthlyPayment = calcResult.monthlyPayment,
                            startDate = savingStartDate,
                            endDate = loanCalendar.timeInMillis,
                            nextPaymentDate = savingStartDate + (30L * 24 * 60 * 60 * 1000),
                            isGoalLoan = true,
                            isLoanActive = !isFutureSaving,
                            fees = charges,
                            notes = "Loan for goal: $title"
                        )
                        onAddGoalLoan(loan)
                        onConfirm(goal)
                    },
                    enabled = lenderName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = WealthBlue)
                ) {
                    Text("Add Goal & Loan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLenderDialog = false }) {
                    Text("Cancel", color = WealthSoftWhite)
                }
            }
        )
    }
}

// Loan Calculation Data Class
private data class LoanCalculation(
    val principal: Double,
    val totalInterest: Double,
    val totalAmount: Double,
    val monthlyPayment: Double
)

// Goal Probability Data Class
data class GoalProbabilityResult(
    val probability: Double, // 0.0 to 1.0
    val percentageOfSavings: Double, // How much % of monthly savings this goal takes
    val insightLevel: String, // "green", "amber", "red"
    val insight: String // AI-generated insight message
)

// Calculate saving amount per period based on frequency
private fun calculateSavingAmountPerPeriod(
    totalAmount: Double,
    months: Int,
    frequency: SavingFrequency,
    fundingMethod: FundingMethod,
    loanMonthlyPayment: Double
): Double {
    if (totalAmount <= 0 || months <= 0) return 0.0

    // For loan-only funding, calculate payment per period from loan monthly payment
    if (fundingMethod == FundingMethod.LOAN) {
        return when (frequency) {
            SavingFrequency.DAILY -> loanMonthlyPayment / 30
            SavingFrequency.WEEKLY -> loanMonthlyPayment / 4
            SavingFrequency.BIWEEKLY -> loanMonthlyPayment / 2
            SavingFrequency.MONTHLY -> loanMonthlyPayment
            SavingFrequency.QUARTERLY -> loanMonthlyPayment * 3
            SavingFrequency.ANNUALLY -> loanMonthlyPayment * 12
        }
    }

    // For mixed funding, only calculate saving portion (loan portion uses loan payment)
    if (fundingMethod == FundingMethod.MIXED) {
        // The saving amount is just for the savings part, loan has its own calculation
        val savingMonthlyAmount = totalAmount / months
        return when (frequency) {
            SavingFrequency.DAILY -> savingMonthlyAmount / 30
            SavingFrequency.WEEKLY -> savingMonthlyAmount / 4
            SavingFrequency.BIWEEKLY -> savingMonthlyAmount / 2
            SavingFrequency.MONTHLY -> savingMonthlyAmount
            SavingFrequency.QUARTERLY -> savingMonthlyAmount * 3
            SavingFrequency.ANNUALLY -> savingMonthlyAmount * 12
        }
    }

    // For saving-only funding
    val totalDays = months * 30
    val dailyAmount = totalAmount / totalDays

    return when (frequency) {
        SavingFrequency.DAILY -> dailyAmount
        SavingFrequency.WEEKLY -> dailyAmount * 7
        SavingFrequency.BIWEEKLY -> dailyAmount * 14
        SavingFrequency.MONTHLY -> totalAmount / months
        SavingFrequency.QUARTERLY -> (totalAmount / months) * 3
        SavingFrequency.ANNUALLY -> totalAmount / (months / 12.0).coerceAtLeast(1.0)
    }
}

// Calculate goal achievement probability using AI-like analysis
private fun calculateGoalProbability(
    targetAmount: Double,
    months: Int,
    monthlySavings: Double,
    savingPerPeriod: Double,
    frequency: SavingFrequency,
    fundingMethod: FundingMethod
): GoalProbabilityResult {
    if (targetAmount <= 0 || months <= 0) {
        return GoalProbabilityResult(
            probability = 0.0,
            percentageOfSavings = 0.0,
            insightLevel = "red",
            insight = "Please enter a valid target amount and duration."
        )
    }

    // Convert saving per period to monthly equivalent
    val monthlySavingRequired = when (frequency) {
        SavingFrequency.DAILY -> savingPerPeriod * 30
        SavingFrequency.WEEKLY -> savingPerPeriod * 4
        SavingFrequency.BIWEEKLY -> savingPerPeriod * 2
        SavingFrequency.MONTHLY -> savingPerPeriod
        SavingFrequency.QUARTERLY -> savingPerPeriod / 3
        SavingFrequency.ANNUALLY -> savingPerPeriod / 12
    }

    // Calculate what percentage of monthly savings this goal requires
    val percentageOfSavings = if (monthlySavings > 0) {
        (monthlySavingRequired / monthlySavings * 100)
    } else {
        100.0
    }

    // Calculate probability based on affordability
    val probability = when {
        monthlySavings <= 0 -> 0.1 // Very low if no savings capacity
        percentageOfSavings <= 30 -> 0.95 // Very achievable
        percentageOfSavings <= 50 -> 0.85 // Achievable
        percentageOfSavings <= 70 -> 0.70 // Moderate
        percentageOfSavings <= 90 -> 0.50 // Challenging
        percentageOfSavings <= 100 -> 0.30 // Difficult
        else -> 0.15 // Very difficult - requires more than current savings
    }

    // Determine insight level
    val insightLevel = when {
        percentageOfSavings < 60 -> "green"
        percentageOfSavings in 60.0..90.0 -> "amber"
        else -> "red"
    }

    // Generate AI insight
    val insight = when {
        fundingMethod == FundingMethod.LOAN -> {
            "This goal will be funded via loan. Ensure you can afford the monthly payments."
        }
        percentageOfSavings < 30 -> {
            "✨ Excellent! This goal uses only ${String.format("%.0f", percentageOfSavings)}% of your monthly savings. Highly achievable!"
        }
        percentageOfSavings < 60 -> {
            "👍 Good! You'll spend about ${String.format("%.0f", percentageOfSavings)}% of your monthly savings on this goal. Very manageable!"
        }
        percentageOfSavings < 90 -> {
            "⚠️ Moderate challenge. This goal requires ${String.format("%.0f", percentageOfSavings)}% of your monthly savings. Consider extending the timeline."
        }
        percentageOfSavings <= 100 -> {
            "🔴 Challenging! This goal needs ${String.format("%.0f", percentageOfSavings)}% of your savings. You may need to reduce other expenses."
        }
        else -> {
            "❌ This goal exceeds your current saving capacity (${String.format("%.0f", percentageOfSavings)}%). Consider a longer timeline or mixed funding with loan."
        }
    }

    return GoalProbabilityResult(
        probability = probability,
        percentageOfSavings = percentageOfSavings,
        insightLevel = insightLevel,
        insight = insight
    )
}

// Loan Calculator Function
private fun calculateLoan(
    principal: Double,
    annualRate: Double,
    termMonths: Int,
    charges: Double,
    interestType: String
): LoanCalculation {
    if (principal <= 0 || termMonths <= 0) {
        return LoanCalculation(principal, 0.0, principal + charges, 0.0)
    }

    val totalInterest: Double
    val monthlyPayment: Double

    when (interestType) {
        "FLAT_RATE" -> {
            totalInterest = principal * (annualRate / 100) * (termMonths / 12.0)
            val totalAmount = principal + totalInterest + charges
            monthlyPayment = totalAmount / termMonths
        }
        "REDUCING_BALANCE" -> {
            val monthlyRate = annualRate / 100 / 12
            if (monthlyRate > 0) {
                monthlyPayment = principal * monthlyRate * Math.pow(1 + monthlyRate, termMonths.toDouble()) /
                        (Math.pow(1 + monthlyRate, termMonths.toDouble()) - 1)
                totalInterest = (monthlyPayment * termMonths) - principal
            } else {
                monthlyPayment = principal / termMonths
                totalInterest = 0.0
            }
        }
        "NO_INTEREST" -> {
            totalInterest = 0.0
            monthlyPayment = (principal + charges) / termMonths
        }
        else -> {
            totalInterest = 0.0
            monthlyPayment = principal / termMonths
        }
    }

    val totalAmount = principal + totalInterest + charges

    return LoanCalculation(
        principal = principal,
        totalInterest = totalInterest.coerceAtLeast(0.0),
        totalAmount = totalAmount,
        monthlyPayment = monthlyPayment.coerceAtLeast(0.0)
    )
}

@Composable
private fun AddContributionDialog(
    goal: FinancialGoal,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WealthNavy,
        title = {
            Text(
                "Add to ${goal.title}",
                color = WealthSoftWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Current: $${String.format("%,.0f", goal.currentAmount)} / $${String.format("%,.0f", goal.targetAmount)}",
                    color = WealthSoftWhite.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Remaining: $${String.format("%,.0f", goal.remainingAmount)}",
                    color = WealthEmerald,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount to add") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WealthSoftWhite,
                        unfocusedTextColor = WealthSoftWhite,
                        focusedBorderColor = WealthEmerald,
                        unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    leadingIcon = { Text("$", color = WealthEmerald, fontWeight = FontWeight.Bold) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { amount.toDoubleOrNull()?.let { onConfirm(it) } },
                enabled = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = WealthEmerald)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WealthSoftWhite)
            }
        }
    )
}

@Composable
private fun GoalDetailsDialog(
    goal: FinancialGoal,
    onDismiss: () -> Unit,
    onContribute: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WealthNavy)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            goal.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        if (goal.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                goal.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WealthSoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WealthSoftWhite.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", style = MaterialTheme.typography.labelMedium, color = WealthSoftWhite.copy(alpha = 0.5f))
                    Text("${goal.progressPercentage.toInt()}%", style = MaterialTheme.typography.labelMedium, color = WealthEmerald, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (goal.progressPercentage / 100).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = WealthEmerald,
                    trackColor = WealthSoftWhite.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amounts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Saved", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.5f))
                        Text("$${String.format("%,.0f", goal.currentAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WealthEmerald)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.5f))
                        Text("$${String.format("%,.0f", goal.targetAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WealthSoftWhite)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Remaining: $${String.format("%,.0f", goal.remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = WealthSoftWhite.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(16.dp))

                // Meta Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Funding", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.5f))
                        Text(goal.fundingMethod.displayName, color = WealthSoftWhite)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target Date", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.5f))
                        Text(dateFormat.format(Date(goal.targetDate)), color = WealthSoftWhite)
                    }
                }

                if (goal.savingFrequency != null && goal.savingAmount != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Saving $${String.format("%,.0f", goal.savingAmount)} ${goal.savingFrequency.displayName.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WealthEmerald
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WealthMutedRed
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(
                            brush = Brush.linearGradient(listOf(WealthMutedRed, WealthMutedRed))
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }

                    if (!goal.isCompleted && goal.fundingMethod != FundingMethod.LOAN) {
                        Button(
                            onClick = onContribute,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WealthEmerald)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Contribute")
                        }
                    }
                }
            }
        }
    }
}

// ========== EXPORT DIALOG ==========
@Composable
private fun GoalsExportDialog(
    goals: List<FinancialGoal>,
    context: Context,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("PDF") }
    var isExporting by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Success Dialog
    if (showSuccessDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false; onDismiss() },
            containerColor = WealthNavy,
            icon = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(WealthEmerald.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = WealthEmerald, modifier = Modifier.size(32.dp))
                    }
                }
            },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Export Successful!", color = WealthSoftWhite, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your goals report has been exported.", color = WealthSoftWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportedFile?.name ?: "", color = WealthEmerald, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                exportedFile?.let { shareGoalsFile(context, it, if (selectedFormat == "PDF") "application/pdf" else "text/csv") }
                                showSuccessDialog = false; onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthEmerald),
                            border = BorderStroke(1.dp, WealthEmerald)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                        Button(
                            onClick = {
                                exportedFile?.let { openGoalsFile(context, it) }
                                showSuccessDialog = false; onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WealthEmerald)
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open")
                        }
                    }
                    TextButton(onClick = { showSuccessDialog = false; onDismiss() }) {
                        Text("Done", color = WealthSoftWhite.copy(alpha = 0.7f))
                    }
                }
            },
            dismissButton = null
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WealthNavy,
        title = { Text("Export Goals", color = WealthSoftWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Export ${goals.size} goal(s)", color = WealthSoftWhite.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" },
                        label = { Text("PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WealthBlue),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedFormat == "Excel",
                        onClick = { selectedFormat = "Excel" },
                        label = { Text("Excel") },
                        leadingIcon = { Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WealthEmerald),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WealthEmerald, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...", color = WealthSoftWhite.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isExporting = true
                    val file = exportGoalsData(context, goals, selectedFormat)
                    isExporting = false
                    if (file != null) { exportedFile = file; showSuccessDialog = true }
                    else Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedFormat == "PDF") WealthBlue else WealthEmerald)
            ) {
                Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = WealthSoftWhite) } }
    )
}

private fun exportGoalsData(context: Context, goals: List<FinancialGoal>, format: String): File? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    return try {
        when (format) {
            "Excel" -> {
                val file = File(context.getExternalFilesDir(null), "Goals_Report_$timestamp.csv")
                FileWriter(file).use { writer ->
                    writer.append("Goals Report\n")
                    writer.append("Generated,${dateFormat.format(Date())}\n\n")
                    writer.append("Title,Type,Target,Saved,Progress,Status,Funding,Start Date,Target Date\n")
                    goals.forEach { goal ->
                        writer.append("${goal.title},${goal.goalType.displayName},")
                        writer.append("$${String.format("%.2f", goal.targetAmount)},")
                        writer.append("$${String.format("%.2f", goal.currentAmount)},")
                        writer.append("${String.format("%.1f", goal.progressPercentage)}%,")
                        writer.append("${if (goal.isCompleted) "Completed" else "Active"},")
                        writer.append("${goal.fundingMethod.displayName},")
                        writer.append("${dateFormat.format(Date(goal.startDate))},")
                        writer.append("${dateFormat.format(Date(goal.targetDate))}\n")
                    }
                }
                file
            }
            "PDF" -> {
                val file = File(context.getExternalFilesDir(null), "Goals_Report_$timestamp.pdf")
                val navyPrimary = DeviceRgb(11, 31, 42)
                val navySecondary = DeviceRgb(15, 40, 55)
                val emeraldColor = DeviceRgb(15, 174, 150)
                val softWhite = DeviceRgb(230, 241, 240)
                val whiteColor = DeviceRgb(255, 255, 255)
                val lightGray = DeviceRgb(245, 247, 250)

                val pdfWriter = PdfWriter(file)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                document.setMargins(36f, 36f, 36f, 36f)

                // Header
                val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
                val headerCell = Cell().setBackgroundColor(navyPrimary).setPadding(20f).setBorder(null)
                headerCell.add(Paragraph("BUDGIE").setFontSize(28f).setBold().setFontColor(emeraldColor).setTextAlignment(PdfTextAlignment.CENTER))
                headerCell.add(Paragraph("GOALS REPORT").setFontSize(16f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.CENTER).setMarginTop(-5f))
                headerCell.add(Paragraph("Your Personal Finance Companion").setFontSize(10f).setFontColor(DeviceRgb(150, 180, 175)).setTextAlignment(PdfTextAlignment.CENTER).setItalic().setMarginTop(8f))
                headerTable.addCell(headerCell)
                document.add(headerTable)

                // Info bar
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                val totalGoals = Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null)
                totalGoals.add(Paragraph("Total Goals: ${goals.size}").setFontSize(11f).setFontColor(softWhite))
                val dateCell = Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null)
                dateCell.add(Paragraph("Generated: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}").setFontSize(11f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.RIGHT))
                infoTable.addCell(totalGoals)
                infoTable.addCell(dateCell)
                document.add(infoTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Summary
                val activeGoals = goals.count { !it.isCompleted }
                val completedGoals = goals.count { it.isCompleted }
                val totalTarget = goals.sumOf { it.targetAmount }
                val totalSaved = goals.sumOf { it.currentAmount }

                document.add(Paragraph("SUMMARY").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f))).useAllAvailableWidth()
                listOf("Active", "Completed", "Total Target", "Total Saved").forEach {
                    summaryTable.addCell(Cell().add(Paragraph(it).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                listOf(activeGoals.toString(), completedGoals.toString(), "$${String.format("%,.2f", totalTarget)}", "$${String.format("%,.2f", totalSaved)}").forEach {
                    summaryTable.addCell(Cell().add(Paragraph(it).setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                document.add(summaryTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Goals table
                document.add(Paragraph("GOALS DETAILS").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val goalsTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1.2f, 1.2f, 0.8f, 1f))).useAllAvailableWidth()
                listOf("Goal", "Type", "Target", "Saved", "Progress", "Status").forEach {
                    goalsTable.addCell(Cell().add(Paragraph(it).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                goals.forEachIndexed { index, goal ->
                    val bgColor = if (index % 2 == 0) whiteColor else lightGray
                    goalsTable.addCell(Cell().add(Paragraph(goal.title).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f))
                    goalsTable.addCell(Cell().add(Paragraph(goal.goalType.displayName).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                    goalsTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", goal.targetAmount)}").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.RIGHT))
                    goalsTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", goal.currentAmount)}").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.RIGHT).setFontColor(emeraldColor))
                    goalsTable.addCell(Cell().add(Paragraph("${String.format("%.0f", goal.progressPercentage)}%").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                    goalsTable.addCell(Cell().add(Paragraph(if (goal.isCompleted) "Done" else "Active").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                document.add(goalsTable)

                // Footer
                document.add(Paragraph(" ").setMarginTop(30f))
                val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
                val footerCell = Cell().setBackgroundColor(navySecondary).setPadding(15f).setBorder(null)
                footerCell.add(Paragraph("Generated by Budgie - Your Personal Finance Companion").setFontSize(9f).setFontColor(DeviceRgb(150, 180, 175)).setTextAlignment(PdfTextAlignment.CENTER))
                footerCell.add(Paragraph("© ${Calendar.getInstance().get(Calendar.YEAR)} Budgie Financial App").setFontSize(8f).setFontColor(emeraldColor).setTextAlignment(PdfTextAlignment.CENTER).setMarginTop(5f))
                footerTable.addCell(footerCell)
                document.add(footerTable)

                document.close()
                file
            }
            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareGoalsFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Goals Report"))
}

private fun openGoalsFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = if (file.extension == "pdf") "application/pdf" else "text/csv"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    }
}

