package com.example.budgie.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

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
fun LoansScreen(
    loans: List<Loan>,
    loanPayments: Map<Long, List<LoanPayment>>,
    loanSummary: LoanSummary,
    financialSummary: FinancialSummary = FinancialSummary(),
    onAddLoan: (Loan) -> Unit,
    onUpdateLoan: (Loan) -> Unit,
    onDeleteLoan: (Loan) -> Unit,
    onAddPayment: (Long, Double, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf<Loan?>(null) }
    var selectedLoanForDetails by remember { mutableStateOf<Loan?>(null) }
    var showExportDialog by remember { mutableStateOf<Loan?>(null) }

    val context = LocalContext.current

    val tabs = listOf("All Loans", "Active", "Paid Off", "Overdue", "Goal Loans")

    val filteredLoans = when (selectedTab) {
        1 -> loans.filter { it.status == LoanStatus.ACTIVE && !it.isOverdue && !it.isGoalLoan }
        2 -> loans.filter { it.status == LoanStatus.PAID_OFF }
        3 -> loans.filter { it.status == LoanStatus.ACTIVE && it.isOverdue }
        4 -> loans.filter { it.isGoalLoan }
        else -> loans.filter { !it.isGoalLoan } // Default shows non-goal loans
    }

    Scaffold(
        containerColor = WealthNavy,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Loans",
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        Text(
                            "${loanSummary.activeLoans} active loan${if (loanSummary.activeLoans != 1) "s" else ""}",
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
            ExtendedFloatingActionButton(
                onClick = { showAddLoanDialog = true },
                containerColor = WealthBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Loan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Cards
            LoansSummarySection(summary = loanSummary)

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = WealthSoftWhite,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = when (index) {
                        1 -> loans.count { it.status == LoanStatus.ACTIVE && !it.isOverdue && !it.isGoalLoan }
                        2 -> loans.count { it.status == LoanStatus.PAID_OFF }
                        3 -> loans.count { it.status == LoanStatus.ACTIVE && it.isOverdue }
                        4 -> loans.count { it.isGoalLoan }
                        else -> loans.count { !it.isGoalLoan }
                    }
                    val isGoalLoansTab = index == 4
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isGoalLoansTab) {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (selectedTab == index) WealthGold else WealthSoftWhite.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    title,
                                    color = if (selectedTab == index) {
                                        if (isGoalLoansTab) WealthGold else WealthBlue
                                    } else WealthSoftWhite.copy(alpha = 0.6f)
                                )
                                if (count > 0 && (index == 3 || isGoalLoansTab)) {
                                    Badge(
                                        containerColor = if (index == 3) WealthMutedRed else WealthGold
                                    ) {
                                        Text("$count")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loans List
            if (filteredLoans.isEmpty()) {
                EmptyLoansState(selectedTab)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLoans, key = { it.id }) { loan ->
                        LoanCard(
                            loan = loan,
                            onClick = { selectedLoanForDetails = loan },
                            onMakePayment = { showPaymentDialog = loan },
                            onExport = { showExportDialog = loan }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add Loan Dialog
    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { loan ->
                onAddLoan(loan)
                showAddLoanDialog = false
            },
            monthlyIncome = financialSummary.totalIncome,
            monthlyExpenses = financialSummary.totalExpenses,
            savingsRate = financialSummary.savingsRate
        )
    }

    // Payment Dialog
    showPaymentDialog?.let { loan ->
        AddPaymentDialog(
            loan = loan,
            onDismiss = { showPaymentDialog = null },
            onConfirm = { amount, reference ->
                onAddPayment(loan.id, amount, reference)
                showPaymentDialog = null
            }
        )
    }

    // Loan Details Dialog
    selectedLoanForDetails?.let { loan ->
        LoanDetailsDialog(
            loan = loan,
            payments = loanPayments[loan.id] ?: emptyList(),
            onDismiss = { selectedLoanForDetails = null },
            onMakePayment = {
                selectedLoanForDetails = null
                showPaymentDialog = loan
            },
            onDelete = {
                onDeleteLoan(loan)
                selectedLoanForDetails = null
            },
            onExport = {
                selectedLoanForDetails = null
                showExportDialog = loan
            }
        )
    }

    // Export Dialog
    showExportDialog?.let { loan ->
        ExportLoanDialog(
            loan = loan,
            payments = loanPayments[loan.id] ?: emptyList(),
            context = context,
            onDismiss = { showExportDialog = null }
        )
    }
}

@Composable
private fun LoansSummarySection(summary: LoanSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .glassmorphicAccentCard(WealthBlue, 24)
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
                                .background(WealthBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Total Borrowed",
                            style = MaterialTheme.typography.labelMedium,
                            color = WealthSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$${String.format("%,.0f", summary.totalBorrowed)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthBlue
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "$${String.format("%,.0f", summary.totalRemaining)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthMutedRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glassmorphic Progress Bar
            val progress = if (summary.totalBorrowed > 0)
                (summary.totalRepaid / summary.totalBorrowed).toFloat() else 0f

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
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
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
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WealthEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Repaid: $${String.format("%,.0f", summary.totalRepaid)} (${String.format("%.0f", progress * 100)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (summary.overdueLoans > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WealthMutedRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${summary.overdueLoans} overdue",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthMutedRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    loan: Loan,
    onClick: () -> Unit,
    onMakePayment: () -> Unit,
    onExport: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val accentColor = when (loan.status) {
        LoanStatus.ACTIVE -> if (loan.isOverdue) WealthMutedRed else WealthBlue
        LoanStatus.PAID_OFF -> WealthEmerald
        else -> WealthAmber
    }

    // Glassmorphic Loan Card
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
                // Loan Type Icon with glow
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = accentColor.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.3f),
                                    accentColor.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (loan.loanType) {
                            LoanType.MORTGAGE -> Icons.Default.Home
                            LoanType.AUTO -> Icons.Default.DirectionsCar
                            LoanType.EDUCATION -> Icons.Default.School
                            LoanType.BUSINESS -> Icons.Default.Business
                            LoanType.CREDIT_CARD -> Icons.Default.CreditCard
                            else -> Icons.Default.AccountBalance
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        loan.title,
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
                        Text(
                            loan.lenderName,
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.6f)
                        )
                        LoanStatusChip(loan.status, loan.isOverdue)
                    }
                }

                // Progress Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { (loan.progressPercentage / 100).toFloat() },
                        modifier = Modifier.fillMaxSize(),
                        color = when {
                            loan.status == LoanStatus.PAID_OFF -> WealthEmerald
                            loan.isOverdue -> WealthMutedRed
                            else -> WealthBlue
                        },
                        trackColor = WealthSoftWhite.copy(alpha = 0.1f),
                        strokeWidth = 4.dp
                    )
                    Text(
                        "${loan.progressPercentage.toInt()}%",
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
                        "Repaid",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "$${String.format("%,.0f", loan.amountPaid)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthEmerald
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Interest",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "${String.format("%.1f", loan.interestRate)}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthAmber
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        "$${String.format("%,.0f", loan.remainingAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthMutedRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Glassmorphic Progress Bar
            val progressColor = when {
                loan.status == LoanStatus.PAID_OFF -> WealthEmerald
                loan.isOverdue -> WealthMutedRed
                else -> WealthBlue
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(WealthSoftWhite.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((loan.progressPercentage / 100).toFloat().coerceIn(0f, 1f))
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

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loan.status == LoanStatus.ACTIVE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (loan.isOverdue) WealthMutedRed else WealthSoftWhite.copy(alpha = 0.5f)
                        )
                        Text(
                            "Next: ${dateFormat.format(Date(loan.nextPaymentDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (loan.isOverdue) WealthMutedRed else WealthSoftWhite.copy(alpha = 0.6f)
                        )
                        Text(
                            "($${String.format("%,.0f", loan.monthlyPayment)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.4f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onExport,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = "Export",
                                tint = WealthSoftWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // Glassmorphic Pay Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            WealthBlue.copy(alpha = 0.3f),
                                            WealthBlue.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = WealthBlue.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = onMakePayment)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Pay",
                                style = MaterialTheme.typography.labelMedium,
                                color = WealthBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WealthEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Completed: ${dateFormat.format(Date(loan.endDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthEmerald
                        )
                    }
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.FileUpload,
                            contentDescription = "Export",
                            tint = WealthSoftWhite.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanStatusChip(status: LoanStatus, isOverdue: Boolean) {
    val (color, label) = when {
        isOverdue -> Pair(WealthMutedRed, "Overdue")
        status == LoanStatus.PAID_OFF -> Pair(WealthEmerald, "Paid Off")
        status == LoanStatus.ACTIVE -> Pair(WealthBlue, "Active")
        else -> Pair(WealthAmber, status.displayName)
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
private fun EmptyLoansState(selectedTab: Int) {
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
                Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = WealthSoftWhite.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                when (selectedTab) {
                    2 -> "No paid off loans yet"
                    3 -> "No overdue loans - great!"
                    else -> "No loans tracked"
                },
                style = MaterialTheme.typography.titleMedium,
                color = WealthSoftWhite.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add a loan to start tracking",
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Loan Calculator Helper Functions
private fun calculateFlatRateInterest(principal: Double, annualRate: Double, months: Int): Double {
    return principal * (annualRate / 100) * (months / 12.0)
}

private fun calculateReducingBalanceInterest(principal: Double, annualRate: Double, months: Int): Double {
    if (annualRate == 0.0 || months == 0) return 0.0
    val monthlyRate = annualRate / 100 / 12
    val emi = (principal * monthlyRate * (1 + monthlyRate).pow(months.toDouble())) /
            ((1 + monthlyRate).pow(months.toDouble()) - 1)
    return (emi * months) - principal
}

private fun calculateEMI(principal: Double, annualRate: Double, months: Int, interestType: InterestType): Double {
    if (months == 0) return 0.0
    return when (interestType) {
        InterestType.FLAT_RATE -> {
            val totalInterest = calculateFlatRateInterest(principal, annualRate, months)
            (principal + totalInterest) / months
        }
        InterestType.REDUCING_BALANCE -> {
            if (annualRate == 0.0) return principal / months
            val monthlyRate = annualRate / 100 / 12
            (principal * monthlyRate * (1 + monthlyRate).pow(months.toDouble())) /
                    ((1 + monthlyRate).pow(months.toDouble()) - 1)
        }
        InterestType.NO_INTEREST -> principal / months
    }
}

private fun calculateLoanCompletionProbability(
    monthlyInstallment: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    savingsRate: Double,
    loanTermMonths: Int
): Float {
    if (monthlyIncome <= 0) return 0.5f // No income data, neutral probability

    val disposableIncome = monthlyIncome - monthlyExpenses
    val installmentToIncomeRatio = monthlyInstallment / monthlyIncome
    val installmentToDisposableRatio = if (disposableIncome > 0) monthlyInstallment / disposableIncome else 2.0

    // Factors affecting probability
    var probability = 0.5f

    // Factor 1: Installment to Income Ratio (30% weight)
    // Below 30% is excellent, 30-50% is okay, above 50% is risky
    probability += when {
        installmentToIncomeRatio < 0.15 -> 0.20f
        installmentToIncomeRatio < 0.25 -> 0.15f
        installmentToIncomeRatio < 0.35 -> 0.10f
        installmentToIncomeRatio < 0.50 -> 0.0f
        installmentToIncomeRatio < 0.70 -> -0.15f
        else -> -0.25f
    }

    // Factor 2: Disposable Income Coverage (25% weight)
    probability += when {
        installmentToDisposableRatio < 0.3 -> 0.15f
        installmentToDisposableRatio < 0.5 -> 0.10f
        installmentToDisposableRatio < 0.7 -> 0.05f
        installmentToDisposableRatio < 1.0 -> -0.05f
        else -> -0.20f
    }

    // Factor 3: Savings Rate (20% weight)
    probability += when {
        savingsRate > 30 -> 0.15f
        savingsRate > 20 -> 0.10f
        savingsRate > 10 -> 0.05f
        savingsRate > 0 -> 0.0f
        else -> -0.10f
    }

    // Factor 4: Loan Term (15% weight) - Longer terms have more uncertainty
    probability += when {
        loanTermMonths <= 12 -> 0.10f
        loanTermMonths <= 24 -> 0.05f
        loanTermMonths <= 36 -> 0.0f
        loanTermMonths <= 60 -> -0.05f
        else -> -0.10f
    }

    // Factor 5: Buffer after installment (10% weight)
    val remainingAfterInstallment = disposableIncome - monthlyInstallment
    val bufferRatio = remainingAfterInstallment / monthlyIncome
    probability += when {
        bufferRatio > 0.2 -> 0.10f
        bufferRatio > 0.1 -> 0.05f
        bufferRatio > 0 -> 0.0f
        else -> -0.10f
    }

    return probability.coerceIn(0.05f, 0.98f)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (Loan) -> Unit,
    monthlyIncome: Double = 0.0,
    monthlyExpenses: Double = 0.0,
    savingsRate: Double = 0.0
) {
    var title by remember { mutableStateOf("") }
    var lenderName by remember { mutableStateOf("") }
    var principalAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var selectedLoanType by remember { mutableStateOf(LoanType.PERSONAL) }
    var selectedInterestType by remember { mutableStateOf(InterestType.REDUCING_BALANCE) }
    var loanTermMonths by remember { mutableStateOf("12") }
    var fees by remember { mutableStateOf("") }

    // Auto-calculated values
    val principal = principalAmount.toDoubleOrNull() ?: 0.0
    val rate = interestRate.toDoubleOrNull() ?: 0.0
    val months = loanTermMonths.toIntOrNull() ?: 12
    val loanFees = fees.toDoubleOrNull() ?: 0.0

    val totalInterest = when (selectedInterestType) {
        InterestType.FLAT_RATE -> calculateFlatRateInterest(principal, rate, months)
        InterestType.REDUCING_BALANCE -> calculateReducingBalanceInterest(principal, rate, months)
        InterestType.NO_INTEREST -> 0.0
    }

    val monthlyInstallment = calculateEMI(principal, rate, months, selectedInterestType)
    val totalPayable = principal + totalInterest + loanFees

    // Probability calculation
    val completionProbability = calculateLoanCompletionProbability(
        monthlyInstallment = monthlyInstallment,
        monthlyIncome = monthlyIncome,
        monthlyExpenses = monthlyExpenses,
        savingsRate = savingsRate,
        loanTermMonths = months
    )

    Dialog(onDismissRequest = onDismiss) {
        // Glassmorphic Dialog Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
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
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(WealthBlue, WealthCyan)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Add New Loan",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthSoftWhite
                                )
                                Text(
                                    "Track your borrowings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthSoftWhite.copy(alpha = 0.5f)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = WealthSoftWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Loan Name - Glassmorphic Input Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicAccentCard(WealthBlue, 14)
                            .padding(2.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Loan Name") },
                            placeholder = { Text("e.g., Car Loan", color = WealthSoftWhite.copy(alpha = 0.3f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = WealthSoftWhite,
                                unfocusedTextColor = WealthSoftWhite,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = WealthBlue,
                                unfocusedLabelColor = WealthSoftWhite.copy(alpha = 0.5f),
                                cursorColor = WealthBlue
                            ),
                            singleLine = true
                        )
                    }
                }

                // Lender Name - Glassmorphic Input Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(14)
                            .padding(2.dp)
                    ) {
                        OutlinedTextField(
                            value = lenderName,
                            onValueChange = { lenderName = it },
                            label = { Text("Lender Name") },
                            placeholder = { Text("e.g., ABC Bank", color = WealthSoftWhite.copy(alpha = 0.3f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = WealthSoftWhite,
                                unfocusedTextColor = WealthSoftWhite,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = WealthBlue
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null, tint = WealthBlue.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                // Loan Type - Glassmorphic Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(14)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Loan Type", style = MaterialTheme.typography.labelMedium, color = WealthSoftWhite.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LoanType.entries.take(6).forEach { type ->
                                    FilterChip(
                                        selected = selectedLoanType == type,
                                        onClick = { selectedLoanType = type },
                                        label = { Text(type.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WealthBlue,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount and Rate Row - Glassmorphic Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphicCard(12)
                                .padding(2.dp)
                        ) {
                            OutlinedTextField(
                                value = principalAmount,
                                onValueChange = { principalAmount = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Principal") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WealthSoftWhite,
                                    unfocusedTextColor = WealthSoftWhite,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = WealthBlue
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Text("$", color = WealthBlue, fontWeight = FontWeight.Bold)
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphicCard(12)
                                .padding(2.dp)
                        ) {
                            OutlinedTextField(
                                value = interestRate,
                                onValueChange = { interestRate = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Interest %") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WealthSoftWhite,
                                    unfocusedTextColor = WealthSoftWhite,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = WealthAmber
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    Text("%", color = WealthAmber, fontWeight = FontWeight.Bold)
                                }
                            )
                        }
                    }
                }

                // Interest Type - Glassmorphic Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(14)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Interest Type", style = MaterialTheme.typography.labelMedium, color = WealthSoftWhite.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InterestType.entries.forEach { type ->
                                    FilterChip(
                                        selected = selectedInterestType == type,
                                        onClick = { selectedInterestType = type },
                                        label = { Text(type.displayName, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WealthBlue,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Loan Term & Fees - Glassmorphic Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphicCard(12)
                                .padding(2.dp)
                        ) {
                            OutlinedTextField(
                                value = loanTermMonths,
                                onValueChange = { loanTermMonths = it.filter { c -> c.isDigit() } },
                                label = { Text("Duration (Months)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WealthSoftWhite,
                                    unfocusedTextColor = WealthSoftWhite,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = WealthBlue
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = WealthBlue.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassmorphicCard(12)
                                .padding(2.dp)
                        ) {
                            OutlinedTextField(
                                value = fees,
                                onValueChange = { fees = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Fees & Charges") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WealthSoftWhite,
                                    unfocusedTextColor = WealthSoftWhite,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = WealthAmber
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Text("$", color = WealthAmber, fontWeight = FontWeight.Bold)
                                }
                            )
                        }
                    }
                }

                // Auto-Calculated Loan Summary Card
                item {
                    if (principal > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicAccentCard(WealthCyan, 16)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = WealthCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Loan Calculation Summary",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = WealthSoftWhite
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Calculation Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Principal", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                                        Text("$${String.format("%,.2f", principal)}", color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Interest (${selectedInterestType.displayName})", style = MaterialTheme.typography.labelSmall, color = WealthAmber.copy(alpha = 0.8f))
                                        Text("$${String.format("%,.2f", totalInterest)}", color = WealthAmber, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Fees & Charges", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                                        Text("$${String.format("%,.2f", loanFees)}", color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                                        Text("$months months", color = WealthSoftWhite, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = WealthSoftWhite.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Key Results
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Monthly Installment", style = MaterialTheme.typography.labelSmall, color = WealthEmerald)
                                        Text(
                                            "$${String.format("%,.2f", monthlyInstallment)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WealthEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Payable", style = MaterialTheme.typography.labelSmall, color = WealthMutedRed)
                                        Text(
                                            "$${String.format("%,.2f", totalPayable)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WealthMutedRed,
                                            fontWeight = FontWeight.Bold
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = WealthSoftWhite.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        }

                        // Add Loan Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = WealthBlue.copy(alpha = 0.3f))
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(WealthBlue, WealthCyan)
                                    )
                                )
                                .clickable(enabled = title.isNotBlank() && principalAmount.isNotBlank()) {
                                    val calendar = Calendar.getInstance()
                                    val startDate = calendar.timeInMillis
                                    calendar.add(Calendar.MONTH, 1)
                                    val nextPayment = calendar.timeInMillis
                                    calendar.add(Calendar.MONTH, months - 1)
                                    val endDate = calendar.timeInMillis

                                    val loan = Loan(
                                        title = title,
                                        lenderName = lenderName,
                                        loanType = selectedLoanType,
                                        principalAmount = principal,
                                        interestRate = rate,
                                        interestType = selectedInterestType,
                                        totalAmount = totalPayable,
                                        monthlyPayment = monthlyInstallment,
                                        startDate = startDate,
                                        endDate = endDate,
                                        nextPaymentDate = nextPayment
                                    )
                                    onConfirm(loan)
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Loan", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Loan Completion Probability Card
                item {
                    if (principal > 0 && monthlyIncome > 0) {
                        val probabilityPercent = (completionProbability * 100).toInt()
                        val probabilityColor = when {
                            completionProbability >= 0.75f -> WealthEmerald
                            completionProbability >= 0.50f -> WealthAmber
                            else -> WealthMutedRed
                        }
                        val probabilityLabel = when {
                            completionProbability >= 0.85f -> "Excellent"
                            completionProbability >= 0.70f -> "Good"
                            completionProbability >= 0.55f -> "Moderate"
                            completionProbability >= 0.40f -> "Challenging"
                            else -> "High Risk"
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicAccentCard(probabilityColor, 16)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = probabilityColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "AI Repayment Analysis",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = WealthSoftWhite
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "Your likelihood of successfully completing this loan based on your current financial behavior:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthSoftWhite.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Probability Display
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "$probabilityPercent%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = probabilityColor
                                        )
                                        Text(
                                            probabilityLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = probabilityColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Progress Ring
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { completionProbability },
                                            modifier = Modifier.size(56.dp),
                                            color = probabilityColor,
                                            trackColor = probabilityColor.copy(alpha = 0.2f),
                                            strokeWidth = 6.dp
                                        )
                                        Icon(
                                            when {
                                                completionProbability >= 0.70f -> Icons.Default.CheckCircle
                                                completionProbability >= 0.50f -> Icons.Default.Info
                                                else -> Icons.Default.Warning
                                            },
                                            contentDescription = null,
                                            tint = probabilityColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Factors Analysis
                                val installmentRatio = if (monthlyIncome > 0) (monthlyInstallment / monthlyIncome * 100).toInt() else 0

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Installment to Income:", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.7f))
                                            Text(
                                                "$installmentRatio%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (installmentRatio <= 30) WealthEmerald else if (installmentRatio <= 50) WealthAmber else WealthMutedRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Your Savings Rate:", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.7f))
                                            Text(
                                                "${savingsRate.toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (savingsRate >= 20) WealthEmerald else if (savingsRate >= 10) WealthAmber else WealthMutedRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Loan Duration:", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.7f))
                                            Text(
                                                "$months months",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (months <= 24) WealthEmerald else if (months <= 48) WealthAmber else WealthMutedRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (completionProbability < 0.60f) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = WealthGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Consider extending the loan term or reducing the principal to improve affordability.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WealthGold
                                        )
                                    }
                                }
                            }
                        }
                    } else if (principal > 0) {
                        // No income data - show info card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphicCard(14)
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = WealthBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Add income data to see your loan completion probability analysis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthSoftWhite.copy(alpha = 0.7f)
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
private fun AddPaymentDialog(
    loan: Loan,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf(loan.monthlyPayment.toString()) }
    var reference by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WealthNavy,
        title = {
            Text(
                "Make Payment",
                color = WealthSoftWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    loan.title,
                    color = WealthSoftWhite,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Remaining: $${String.format("%,.0f", loan.remainingAmount)}",
                    color = WealthMutedRed,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Payment Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WealthSoftWhite,
                        unfocusedTextColor = WealthSoftWhite,
                        focusedBorderColor = WealthBlue,
                        unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WealthSoftWhite,
                        unfocusedTextColor = WealthSoftWhite,
                        focusedBorderColor = WealthBlue,
                        unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { amount.toDoubleOrNull()?.let { onConfirm(it, reference) } },
                enabled = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = WealthBlue)
            ) {
                Text("Pay")
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
private fun LoanDetailsDialog(
    loan: Loan,
    payments: List<LoanPayment>,
    onDismiss: () -> Unit,
    onMakePayment: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WealthNavy)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                loan.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = WealthSoftWhite
                            )
                            Text(
                                loan.lenderName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WealthSoftWhite.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = WealthSoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Progress
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { (loan.progressPercentage / 100).toFloat() },
                                modifier = Modifier.fillMaxSize(),
                                color = if (loan.status == LoanStatus.PAID_OFF) WealthEmerald else WealthBlue,
                                trackColor = WealthSoftWhite.copy(alpha = 0.1f),
                                strokeWidth = 8.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${loan.progressPercentage.toInt()}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthSoftWhite
                                )
                                Text(
                                    "Repaid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthSoftWhite.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Summary
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$${String.format("%,.0f", loan.amountPaid)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WealthEmerald)
                            Text("Paid", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.5f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$${String.format("%,.0f", loan.remainingAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WealthMutedRed)
                            Text("Remaining", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.5f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$${String.format("%,.0f", loan.totalAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WealthSoftWhite)
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.5f))
                        }
                    }
                }

                item { HorizontalDivider(color = WealthSoftWhite.copy(alpha = 0.1f)) }

                // Details
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("Principal", "$${String.format("%,.0f", loan.principalAmount)}")
                        DetailRow("Interest Rate", "${loan.interestRate}% (${loan.interestType.displayName})")
                        DetailRow("Total Interest", "$${String.format("%,.0f", loan.totalInterest)}")
                        DetailRow("Monthly Payment", "$${String.format("%,.0f", loan.monthlyPayment)}")
                        DetailRow("Start Date", dateFormat.format(Date(loan.startDate)))
                        DetailRow("End Date", dateFormat.format(Date(loan.endDate)))
                        if (loan.status == LoanStatus.ACTIVE) {
                            DetailRow("Next Payment", dateFormat.format(Date(loan.nextPaymentDate)))
                        }
                    }
                }

                // Payment History
                if (payments.isNotEmpty()) {
                    item {
                        Text(
                            "Payment History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                    }

                    items(payments.take(5)) { payment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                dateFormat.format(Date(payment.paymentDate)),
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthSoftWhite.copy(alpha = 0.6f)
                            )
                            Text(
                                "$${String.format("%,.0f", payment.amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = WealthEmerald
                            )
                        }
                    }

                    if (payments.size > 5) {
                        item {
                            Text(
                                "+${payments.size - 5} more payments",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthSoftWhite.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthMutedRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }

                        OutlinedButton(
                            onClick = onExport,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthSoftWhite),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export")
                        }
                    }

                    if (loan.status == LoanStatus.ACTIVE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onMakePayment,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = WealthBlue)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Make Payment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = WealthSoftWhite.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = WealthSoftWhite)
    }
}

@Composable
private fun ExportLoanDialog(
    loan: Loan,
    payments: List<LoanPayment>,
    context: Context,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("PDF") }
    var isExporting by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Success Dialog with Share/Open options
    if (showSuccessDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onDismiss()
            },
            containerColor = WealthNavy,
            icon = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(WealthEmerald.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WealthEmerald,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Export Successful!",
                        color = WealthSoftWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Your loan statement has been exported successfully.",
                        color = WealthSoftWhite.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        exportedFile?.name ?: "",
                        color = WealthEmerald,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        // Share Button
                        OutlinedButton(
                            onClick = {
                                exportedFile?.let { file ->
                                    shareFile(context, file, if (selectedFormat == "PDF") "application/pdf" else "text/csv")
                                }
                                showSuccessDialog = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WealthEmerald
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WealthEmerald)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }

                        // Open Button
                        Button(
                            onClick = {
                                exportedFile?.let { file ->
                                    openFile(context, file)
                                }
                                showSuccessDialog = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WealthEmerald
                            )
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open")
                        }
                    }

                    // Done button centered below
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            onDismiss()
                        }
                    ) {
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
        title = {
            Text("Export Loan Data", color = WealthSoftWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Export ${loan.title} statement",
                    color = WealthSoftWhite.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" },
                        label = { Text("PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WealthBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedFormat == "Excel",
                        onClick = { selectedFormat = "Excel" },
                        label = { Text("Excel") },
                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WealthEmerald
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = WealthEmerald,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Exporting...",
                            color = WealthSoftWhite.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isExporting = true
                    val file = exportLoanDataWithResult(context, loan, payments, selectedFormat)
                    isExporting = false
                    if (file != null) {
                        exportedFile = file
                        showSuccessDialog = true
                    } else {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedFormat == "PDF") WealthBlue else WealthEmerald
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = WealthSoftWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isExporting) "Exporting..." else "Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExporting) {
                Text("Cancel", color = WealthSoftWhite)
            }
        }
    )
}

/**
 * Export loan data and return the file
 */
private fun exportLoanDataWithResult(context: Context, loan: Loan, payments: List<LoanPayment>, format: String): File? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    return try {
        when (format) {
            "Excel" -> {
                val fileName = "Loan_${loan.title.replace(" ", "_")}_$timestamp.csv"
                val file = File(context.getExternalFilesDir(null), fileName)

                FileWriter(file).use { writer ->
                    writer.append("Loan Statement - ${loan.title}\n")
                    writer.append("Lender,${loan.lenderName}\n")
                    writer.append("Principal,$${String.format("%.2f", loan.principalAmount)}\n")
                    writer.append("Interest Rate,${loan.interestRate}%\n")
                    writer.append("Total Amount,$${String.format("%.2f", loan.totalAmount)}\n")
                    writer.append("Amount Paid,$${String.format("%.2f", loan.amountPaid)}\n")
                    writer.append("Remaining,$${String.format("%.2f", loan.remainingAmount)}\n")
                    writer.append("\nPayment History\n")
                    writer.append("Date,Amount,Reference,Status\n")

                    payments.forEach { payment ->
                        writer.append("${dateFormat.format(Date(payment.paymentDate))},")
                        writer.append("$${String.format("%.2f", payment.amount)},")
                        writer.append("${payment.referenceNumber},")
                        writer.append("${payment.status.displayName}\n")
                    }
                }
                file
            }

            "PDF" -> {
                val fileName = "Loan_${loan.title.replace(" ", "_")}_$timestamp.pdf"
                val file = File(context.getExternalFilesDir(null), fileName)

                // PDF Colors - matching main export
                val navyPrimary = DeviceRgb(11, 31, 42)
                val navySecondary = DeviceRgb(15, 40, 55)
                val emeraldColor = DeviceRgb(15, 174, 150)
                val goldColor = DeviceRgb(201, 161, 74)
                val softWhite = DeviceRgb(230, 241, 240)
                val whiteColor = DeviceRgb(255, 255, 255)
                val lightGrayColor = DeviceRgb(245, 247, 250)
                val mutedRed = DeviceRgb(229, 115, 115)

                val pdfWriter = PdfWriter(file)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                document.setMargins(36f, 36f, 36f, 36f)

                // ========== PROFESSIONAL HEADER ==========
                val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()

                val headerCell = Cell()
                    .setBackgroundColor(navyPrimary)
                    .setPadding(20f)
                    .setBorder(null)

                headerCell.add(
                    Paragraph("BUDGIE")
                        .setFontSize(28f)
                        .setBold()
                        .setFontColor(emeraldColor)
                        .setTextAlignment(TextAlignment.CENTER)
                )

                headerCell.add(
                    Paragraph("LOAN STATEMENT")
                        .setFontSize(16f)
                        .setFontColor(softWhite)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(-5f)
                )

                headerCell.add(
                    Paragraph("Your Personal Finance Companion")
                        .setFontSize(10f)
                        .setFontColor(DeviceRgb(150, 180, 175))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setItalic()
                        .setMarginTop(8f)
                )

                headerTable.addCell(headerCell)
                document.add(headerTable)

                // ========== LOAN INFO BAR ==========
                val infoBarTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()

                val loanNameCell = Cell()
                    .setBackgroundColor(navySecondary)
                    .setPadding(12f)
                    .setBorder(null)
                loanNameCell.add(
                    Paragraph("Loan: ${loan.title}")
                        .setFontSize(11f)
                        .setFontColor(softWhite)
                        .setBold()
                )

                val dateCell = Cell()
                    .setBackgroundColor(navySecondary)
                    .setPadding(12f)
                    .setBorder(null)
                dateCell.add(
                    Paragraph("Generated: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}")
                        .setFontSize(11f)
                        .setFontColor(softWhite)
                        .setTextAlignment(TextAlignment.RIGHT)
                )

                infoBarTable.addCell(loanNameCell)
                infoBarTable.addCell(dateCell)
                document.add(infoBarTable)

                // Spacer
                document.add(Paragraph(" ").setMarginBottom(15f))

                // ========== LOAN DETAILS SECTION ==========
                document.add(
                    Paragraph("LOAN DETAILS")
                        .setFontSize(14f)
                        .setBold()
                        .setFontColor(navyPrimary)
                        .setMarginBottom(10f)
                )

                val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(20f)

                // Helper function to add detail row
                fun addDetailRow(label: String, value: String, isHighlight: Boolean = false, isWarning: Boolean = false) {
                    val labelBgColor = when {
                        isHighlight -> emeraldColor
                        isWarning -> mutedRed
                        else -> lightGrayColor
                    }
                    val valueBgColor = when {
                        isHighlight -> DeviceRgb(20, 180, 160)
                        isWarning -> DeviceRgb(240, 130, 130)
                        else -> whiteColor
                    }
                    val textColor = if (isHighlight || isWarning) whiteColor else navyPrimary

                    detailsTable.addCell(
                        Cell().add(Paragraph(label).setBold().setFontSize(10f))
                            .setBackgroundColor(labelBgColor)
                            .setFontColor(textColor)
                            .setPadding(10f)
                            .setBorder(null)
                    )
                    detailsTable.addCell(
                        Cell().add(Paragraph(value).setFontSize(10f))
                            .setBackgroundColor(valueBgColor)
                            .setFontColor(textColor)
                            .setPadding(10f)
                            .setBorder(null)
                    )
                }

                addDetailRow("Lender / Institution", loan.lenderName)
                addDetailRow("Loan Type", loan.loanType.displayName)
                addDetailRow("Principal Amount", "$${String.format("%,.2f", loan.principalAmount)}")
                addDetailRow("Interest Rate", "${loan.interestRate}% (${loan.interestType.displayName})")
                addDetailRow("Total Fees", "$${String.format("%,.2f", loan.fees)}")
                addDetailRow("Total Loan Amount", "$${String.format("%,.2f", loan.totalAmount)}", isHighlight = true)
                addDetailRow("Monthly Payment", "$${String.format("%,.2f", loan.monthlyPayment)}")
                addDetailRow("Amount Paid", "$${String.format("%,.2f", loan.amountPaid)}")
                addDetailRow("Remaining Balance", "$${String.format("%,.2f", loan.remainingAmount)}", isWarning = loan.remainingAmount > 0)
                addDetailRow("Repayment Progress", "${String.format("%.1f", loan.progressPercentage)}%")
                addDetailRow("Status", loan.status.displayName)
                addDetailRow("Start Date", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(loan.startDate)))
                addDetailRow("End Date", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(loan.endDate)))

                document.add(detailsTable)

                // ========== PAYMENT HISTORY SECTION ==========
                if (payments.isNotEmpty()) {
                    document.add(
                        Paragraph("PAYMENT HISTORY")
                            .setFontSize(14f)
                            .setBold()
                            .setFontColor(navyPrimary)
                            .setMarginTop(10f)
                            .setMarginBottom(10f)
                    )

                    val paymentTable = Table(UnitValue.createPercentArray(floatArrayOf(1.2f, 1f, 1.5f, 1f)))
                        .useAllAvailableWidth()

                    // Header row
                    listOf("Date", "Amount", "Reference", "Status").forEach { header ->
                        paymentTable.addCell(
                            Cell().add(Paragraph(header).setBold().setFontSize(9f))
                                .setBackgroundColor(navyPrimary)
                                .setFontColor(whiteColor)
                                .setPadding(10f)
                                .setTextAlignment(TextAlignment.CENTER)
                        )
                    }

                    // Payment rows with alternating colors
                    payments.forEachIndexed { index, payment ->
                        val bgColor = if (index % 2 == 0) whiteColor else lightGrayColor

                        paymentTable.addCell(
                            Cell().add(Paragraph(dateFormat.format(Date(payment.paymentDate))).setFontSize(9f))
                                .setBackgroundColor(bgColor)
                                .setFontColor(navyPrimary)
                                .setPadding(8f)
                        )
                        paymentTable.addCell(
                            Cell().add(Paragraph("$${String.format("%,.2f", payment.amount)}").setFontSize(9f))
                                .setBackgroundColor(bgColor)
                                .setFontColor(emeraldColor)
                                .setPadding(8f)
                                .setTextAlignment(TextAlignment.RIGHT)
                        )
                        paymentTable.addCell(
                            Cell().add(Paragraph(payment.referenceNumber.ifEmpty { "-" }).setFontSize(9f))
                                .setBackgroundColor(bgColor)
                                .setFontColor(navyPrimary)
                                .setPadding(8f)
                        )
                        paymentTable.addCell(
                            Cell().add(Paragraph(payment.status.displayName).setFontSize(9f))
                                .setBackgroundColor(bgColor)
                                .setFontColor(navyPrimary)
                                .setPadding(8f)
                                .setTextAlignment(TextAlignment.CENTER)
                        )
                    }

                    // Total row
                    val totalPaid = payments.sumOf { it.amount }
                    paymentTable.addCell(
                        Cell(1, 1).add(Paragraph("TOTAL").setBold().setFontSize(9f))
                            .setBackgroundColor(emeraldColor)
                            .setFontColor(whiteColor)
                            .setPadding(10f)
                    )
                    paymentTable.addCell(
                        Cell(1, 1).add(Paragraph("$${String.format("%,.2f", totalPaid)}").setBold().setFontSize(9f))
                            .setBackgroundColor(emeraldColor)
                            .setFontColor(whiteColor)
                            .setPadding(10f)
                            .setTextAlignment(TextAlignment.RIGHT)
                    )
                    paymentTable.addCell(
                        Cell(1, 2).add(Paragraph("${payments.size} payment(s)").setFontSize(9f))
                            .setBackgroundColor(emeraldColor)
                            .setFontColor(whiteColor)
                            .setPadding(10f)
                            .setTextAlignment(TextAlignment.CENTER)
                    )

                    document.add(paymentTable)
                } else {
                    document.add(
                        Paragraph("No payment history available yet.")
                            .setFontSize(10f)
                            .setFontColor(DeviceRgb(128, 128, 128))
                            .setItalic()
                            .setMarginTop(10f)
                    )
                }

                // ========== PROFESSIONAL FOOTER ==========
                document.add(Paragraph(" ").setMarginTop(30f))

                val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
                val footerCell = Cell()
                    .setBackgroundColor(navySecondary)
                    .setPadding(15f)
                    .setBorder(null)

                footerCell.add(
                    Paragraph("Generated by Budgie - Your Personal Finance Companion")
                        .setFontSize(9f)
                        .setFontColor(DeviceRgb(150, 180, 175))
                        .setTextAlignment(TextAlignment.CENTER)
                )
                footerCell.add(
                    Paragraph("This document is for personal reference only. Keep your financial records secure.")
                        .setFontSize(8f)
                        .setFontColor(DeviceRgb(120, 140, 135))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(5f)
                )
                footerCell.add(
                    Paragraph("© ${Calendar.getInstance().get(Calendar.YEAR)} Budgie Financial App")
                        .setFontSize(8f)
                        .setFontColor(emeraldColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(8f)
                )

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

private fun exportLoanData(context: Context, loan: Loan, payments: List<LoanPayment>, format: String) {
    val file = exportLoanDataWithResult(context, loan, payments, format)
    if (file != null) {
        val mimeType = if (format == "Excel") "text/csv" else "application/pdf"
        shareFile(context, file, mimeType)
        Toast.makeText(context, "Export successful!", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Open file with default app
 */
private fun openFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            "txt" -> "text/plain"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "*/*"
        }
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

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Loan Statement"))
}

