package com.example.budgie.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.itextpdf.layout.properties.UnitValue
import com.example.budgie.mpesa.MpesaRepository
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var customLenderName by remember { mutableStateOf("") }
    var customPaybill by remember { mutableStateOf("") }
    var principalAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var selectedLoanType by remember { mutableStateOf(LoanType.PERSONAL) }
    var selectedInterestType by remember { mutableStateOf(InterestType.REDUCING_BALANCE) }
    var loanTermMonths by remember { mutableStateOf("12") }
    var fees by remember { mutableStateOf("") }
    var loanAccountNumber by remember { mutableStateOf("") }

    // Dropdown states
    var selectedLenderCategory by remember { mutableStateOf<LenderCategory?>(null) }
    var selectedLender by remember { mutableStateOf<KenyanLender?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showLenderDropdown by remember { mutableStateOf(false) }
    var showLoanTypeDropdown by remember { mutableStateOf(false) }
    var showInterestTypeDropdown by remember { mutableStateOf(false) }
    var isOtherLender by remember { mutableStateOf(false) }

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

    val completionProbability = calculateLoanCompletionProbability(
        monthlyInstallment = monthlyInstallment,
        monthlyIncome = monthlyIncome,
        monthlyExpenses = monthlyExpenses,
        savingsRate = savingsRate,
        loanTermMonths = months
    )

    // Get lenders for selected category + Other option
    val lendersInCategory = remember(selectedLenderCategory) {
        if (selectedLenderCategory != null &&
            selectedLenderCategory != LenderCategory.EMPLOYER &&
            selectedLenderCategory != LenderCategory.FAMILY_FRIENDS &&
            selectedLenderCategory != LenderCategory.OTHER) {
            KenyanLenders.getLendersByCategory(selectedLenderCategory!!) +
            listOf(KenyanLender("Other (Enter manually)", "Other", selectedLenderCategory!!, "", "", "📋"))
        } else emptyList()
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = WealthSoftWhite,
        unfocusedTextColor = WealthSoftWhite,
        focusedBorderColor = WealthBlue,
        unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f),
        focusedLabelColor = WealthBlue,
        unfocusedLabelColor = WealthSoftWhite.copy(alpha = 0.5f),
        cursorColor = WealthBlue,
        focusedPlaceholderColor = WealthSoftWhite.copy(alpha = 0.3f),
        unfocusedPlaceholderColor = WealthSoftWhite.copy(alpha = 0.3f)
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(WealthNavy)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, null, tint = WealthBlue, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Add New Loan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = WealthSoftWhite)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = WealthSoftWhite.copy(alpha = 0.6f))
                        }
                    }
                }

                // Loan Name
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Loan Name") },
                        placeholder = { Text("e.g., Car Loan") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        singleLine = true
                    )
                }

                // Lender Category Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown,
                        onExpandedChange = { showCategoryDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLenderCategory?.displayName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Lender Category") },
                            placeholder = { Text("Select category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            LenderCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text("${category.icon} ${category.displayName}") },
                                    onClick = {
                                        selectedLenderCategory = category
                                        selectedLender = null
                                        lenderName = ""
                                        isOtherLender = false
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Lender Selection Dropdown
                if (selectedLenderCategory != null &&
                    selectedLenderCategory != LenderCategory.EMPLOYER &&
                    selectedLenderCategory != LenderCategory.FAMILY_FRIENDS &&
                    selectedLenderCategory != LenderCategory.OTHER) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = showLenderDropdown,
                            onExpandedChange = { showLenderDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedLender?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select ${selectedLenderCategory?.displayName}") },
                                placeholder = { Text("Choose lender") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLenderDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = textFieldColors
                            )
                            ExposedDropdownMenu(
                                expanded = showLenderDropdown,
                                onDismissRequest = { showLenderDropdown = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                lendersInCategory.forEach { lender ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(lender.shortName)
                                                if (lender.paybillNumber.isNotEmpty()) {
                                                    Text(lender.paybillNumber, color = WealthEmerald, fontSize = 12.sp)
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (lender.shortName == "Other") {
                                                isOtherLender = true
                                                selectedLender = null
                                                lenderName = ""
                                            } else {
                                                selectedLender = lender
                                                lenderName = lender.name
                                                isOtherLender = false
                                            }
                                            showLenderDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom lender input
                if (selectedLenderCategory == LenderCategory.EMPLOYER ||
                    selectedLenderCategory == LenderCategory.FAMILY_FRIENDS ||
                    selectedLenderCategory == LenderCategory.OTHER || isOtherLender) {
                    item {
                        OutlinedTextField(
                            value = customLenderName,
                            onValueChange = { customLenderName = it; lenderName = it },
                            label = { Text(when (selectedLenderCategory) {
                                LenderCategory.EMPLOYER -> "Employer Name"
                                LenderCategory.FAMILY_FRIENDS -> "Person's Name"
                                else -> "Lender Name"
                            })},
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            singleLine = true
                        )
                    }
                    if (isOtherLender || selectedLenderCategory == LenderCategory.OTHER) {
                        item {
                            OutlinedTextField(
                                value = customPaybill,
                                onValueChange = { customPaybill = it.filter { c -> c.isDigit() } },
                                label = { Text("Paybill Number (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                // Paybill info
                if (selectedLender != null && selectedLender!!.paybillNumber.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(WealthEmerald.copy(alpha = 0.15f)).padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = WealthEmerald, modifier = Modifier.size(16.dp))
                                Text("Paybill: ${selectedLender!!.paybillNumber}", color = WealthEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("Auto-detected", color = WealthSoftWhite.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }
                }

                // Loan Account Number
                if (selectedLender != null || customPaybill.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = loanAccountNumber,
                            onValueChange = { loanAccountNumber = it },
                            label = { Text("Loan Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            singleLine = true
                        )
                    }
                }

                // Loan Type Dropdown
                item {
                    ExposedDropdownMenuBox(expanded = showLoanTypeDropdown, onExpandedChange = { showLoanTypeDropdown = it }) {
                        OutlinedTextField(
                            value = selectedLoanType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loan Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLoanTypeDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(expanded = showLoanTypeDropdown, onDismissRequest = { showLoanTypeDropdown = false }) {
                            LoanType.entries.forEach { type ->
                                DropdownMenuItem(text = { Text(type.displayName) }, onClick = { selectedLoanType = type; showLoanTypeDropdown = false })
                            }
                        }
                    }
                }

                // Principal & Interest Rate
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = principalAmount,
                            onValueChange = { principalAmount = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Principal (KES)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = textFieldColors,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = interestRate,
                            onValueChange = { interestRate = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Interest %") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = textFieldColors,
                            singleLine = true
                        )
                    }
                }

                // Interest Type Dropdown
                item {
                    ExposedDropdownMenuBox(expanded = showInterestTypeDropdown, onExpandedChange = { showInterestTypeDropdown = it }) {
                        OutlinedTextField(
                            value = selectedInterestType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Interest Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showInterestTypeDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(expanded = showInterestTypeDropdown, onDismissRequest = { showInterestTypeDropdown = false }) {
                            InterestType.entries.forEach { type ->
                                DropdownMenuItem(text = { Text(type.displayName) }, onClick = { selectedInterestType = type; showInterestTypeDropdown = false })
                            }
                        }
                    }
                }

                // Duration & Fees
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = loanTermMonths,
                            onValueChange = { loanTermMonths = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration (Months)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fees,
                            onValueChange = { fees = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Fees (KES)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = textFieldColors,
                            singleLine = true
                        )
                    }
                }

                // Loan Summary
                if (principal > 0) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WealthCyan.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Loan Summary", fontWeight = FontWeight.Bold, color = WealthCyan, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Monthly Payment:", color = WealthSoftWhite.copy(alpha = 0.7f), fontSize = 13.sp)
                                    Text("KES ${String.format("%,.2f", monthlyInstallment)}", color = WealthEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Interest:", color = WealthSoftWhite.copy(alpha = 0.7f), fontSize = 13.sp)
                                    Text("KES ${String.format("%,.2f", totalInterest)}", color = WealthAmber, fontSize = 13.sp)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Payable:", color = WealthSoftWhite.copy(alpha = 0.7f), fontSize = 13.sp)
                                    Text("KES ${String.format("%,.2f", totalPayable)}", color = WealthMutedRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (monthlyIncome > 0) {
                        item {
                            val probPercent = (completionProbability * 100).toInt()
                            val probColor = when { completionProbability >= 0.7f -> WealthEmerald; completionProbability >= 0.5f -> WealthAmber; else -> WealthMutedRed }
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = probColor.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Analytics, null, tint = probColor, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Repayment Probability", color = WealthSoftWhite.copy(alpha = 0.7f), fontSize = 12.sp)
                                        Text("$probPercent%", color = probColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, WealthSoftWhite.copy(alpha = 0.3f))) {
                            Text("Cancel", color = WealthSoftWhite.copy(alpha = 0.7f))
                        }
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                val startDate = calendar.timeInMillis
                                calendar.add(Calendar.MONTH, 1)
                                val nextPayment = calendar.timeInMillis
                                calendar.add(Calendar.MONTH, months - 1)
                                val endDate = calendar.timeInMillis
                                val finalLenderName = if (isOtherLender || selectedLenderCategory in listOf(LenderCategory.EMPLOYER, LenderCategory.FAMILY_FRIENDS, LenderCategory.OTHER)) customLenderName else lenderName
                                val loan = Loan(title = title, lenderName = finalLenderName, loanType = selectedLoanType, principalAmount = principal, interestRate = rate, interestType = selectedInterestType, totalAmount = totalPayable, monthlyPayment = monthlyInstallment, startDate = startDate, endDate = endDate, nextPaymentDate = nextPayment)
                                onConfirm(loan)
                            },
                            enabled = title.isNotBlank() && principalAmount.isNotBlank() && (lenderName.isNotBlank() || customLenderName.isNotBlank()),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WealthBlue)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Loan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentDialog(
    loan: Loan,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    // Round up to next whole number
    val roundedMonthlyPayment = kotlin.math.ceil(loan.monthlyPayment).toInt()
    var amount by remember { mutableStateOf(roundedMonthlyPayment.toString()) }
    var reference by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var isProcessingStkPush by remember { mutableStateOf(false) }
    var stkPushStatus by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf(0) } // 0=STK, 1=MPesa, 2=STK Menu
    var showSimSelector by remember { mutableStateOf(false) }
    var showPaymentResult by remember { mutableStateOf(false) }
    var paymentResultSuccess by remember { mutableStateOf(false) }
    var paymentResultMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mpesaRepository = remember { MpesaRepository.getInstance() }

    // Get phone numbers from SIM cards
    val simPhoneNumbers = remember { getSimPhoneNumbers(context) }

    // Auto-populate phone number from SIM (prefer Safaricom for M-Pesa)
    var phoneNumber by remember {
        mutableStateOf(
            simPhoneNumbers.find {
                it.carrierName?.contains("Safaricom", ignoreCase = true) == true
            }?.phoneNumber
            ?: simPhoneNumbers.firstOrNull()?.phoneNumber
            ?: ""
        )
    }

    // Flag to show if phone was auto-detected
    val isPhoneAutoDetected = remember { simPhoneNumbers.isNotEmpty() }

    // Try to find lender paybill
    val lenderInfo = remember(loan.lenderName) {
        KenyanLenders.getLenderByName(loan.lenderName)
    }
    val paybillNumber = lenderInfo?.paybillNumber ?: ""

    // Round amount whenever it changes
    val roundedAmount = remember(amount) {
        amount.toDoubleOrNull()?.let { kotlin.math.ceil(it).toInt() } ?: 0
    }

    // Payment Result Dialog
    if (showPaymentResult) {
        AlertDialog(
            onDismissRequest = {
                showPaymentResult = false
                if (paymentResultSuccess) onDismiss()
            },
            containerColor = WealthNavy,
            icon = {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (paymentResultSuccess) WealthEmerald.copy(alpha = 0.2f)
                            else WealthAmber.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (paymentResultSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (paymentResultSuccess) WealthEmerald else WealthAmber,
                        modifier = Modifier.size(48.dp)
                    )
                }
            },
            title = {
                Text(
                    if (paymentResultSuccess) "STK Push Sent!" else "Action Required",
                    color = WealthSoftWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        paymentResultMessage,
                        color = WealthSoftWhite.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    if (paymentResultSuccess) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    WealthEmerald.copy(alpha = 0.1f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                null,
                                tint = WealthEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Check your phone",
                                    color = WealthEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Enter your M-Pesa PIN to complete",
                                    color = WealthSoftWhite.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentResult = false
                        if (paymentResultSuccess) onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (paymentResultSuccess) WealthEmerald else WealthAmber
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (paymentResultSuccess) "Done" else "OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = if (!paymentResultSuccess) {
                {
                    OutlinedButton(
                        onClick = { openMpesaAppForPayment(context) },
                        border = BorderStroke(1.dp, Color(0xFF00A651)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open M-Pesa", color = Color(0xFF00A651))
                    }
                }
            } else null
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D1B2A),
                                Color(0xFF1B263B),
                                Color(0xFF0D1B2A)
                            )
                        )
                    )
            ) {
                // Futuristic glow effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00A651).copy(alpha = 0.15f),
                                    Color.Transparent
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
                    // ═══════════════ HEADER ═══════════════
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Animated payment icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF00A651),
                                                    Color(0xFF00D26A)
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFF00D26A).copy(alpha = 0.5f),
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.SendToMobile,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "INSTANT PAYMENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF00D26A),
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Pay Loan",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = WealthSoftWhite
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WealthSoftWhite.copy(alpha = 0.1f))
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

                    // ═══════════════ LOAN SUMMARY CARD ═══════════════
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF1E3A5F).copy(alpha = 0.8f),
                                            Color(0xFF2E4A6F).copy(alpha = 0.6f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    Color(0xFF4A90D9).copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "LOAN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WealthSoftWhite.copy(alpha = 0.5f),
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            loan.title,
                                            color = WealthSoftWhite,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            loan.lenderName,
                                            color = WealthSoftWhite.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "BALANCE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WealthMutedRed.copy(alpha = 0.7f),
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            "KES ${String.format("%,d", kotlin.math.ceil(loan.remainingAmount).toInt())}",
                                            color = WealthMutedRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }

                                // Progress bar
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${String.format("%.0f", loan.progressPercentage)}% paid",
                                            color = WealthEmerald,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "KES ${String.format("%,d", kotlin.math.ceil(loan.amountPaid).toInt())} / ${String.format("%,d", kotlin.math.ceil(loan.totalAmount).toInt())}",
                                            color = WealthSoftWhite.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(WealthSoftWhite.copy(alpha = 0.1f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((loan.progressPercentage / 100).toFloat().coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(WealthEmerald, Color(0xFF00D26A))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════ PAYBILL AUTO-DETECTED ═══════════════
                    if (paybillNumber.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF00A651).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFF00A651).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF00D26A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                "Paybill: $paybillNumber",
                                                color = Color(0xFF00D26A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                lenderInfo?.shortName ?: loan.lenderName,
                                                color = WealthSoftWhite.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFF00A651).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "AUTO",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            color = Color(0xFF00D26A),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                        }
                    }
                }

                    // ═══════════════ PAYMENT AMOUNT ═══════════════
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "AMOUNT TO PAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = WealthSoftWhite.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF00A651).copy(alpha = 0.08f),
                                                Color(0xFF00A651).copy(alpha = 0.03f)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color(0xFF00A651).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = { newValue ->
                                        // Only allow digits, auto round up
                                        val filtered = newValue.filter { c -> c.isDigit() }
                                        amount = filtered
                                    },
                                    label = { Text("Payment Amount", color = WealthSoftWhite.copy(alpha = 0.5f)) },
                                    prefix = {
                                        Text(
                                            "KES ",
                                            color = Color(0xFF00D26A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WealthSoftWhite,
                                        unfocusedTextColor = WealthSoftWhite,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = Color(0xFF00D26A)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    singleLine = true
                                )
                            }
                            // Show rounded amount
                            if (roundedAmount > 0) {
                                Text(
                                    "Amount: KES ${String.format("%,d", roundedAmount)}",
                                    color = Color(0xFF00D26A).copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // ═══════════════ ACCOUNT NUMBER ═══════════════
                    if (paybillNumber.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "LOAN ACCOUNT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthSoftWhite.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WealthSoftWhite.copy(alpha = 0.05f))
                                        .border(1.dp, WealthSoftWhite.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = accountNumber,
                                        onValueChange = { accountNumber = it },
                                        placeholder = {
                                            Text(
                                                lenderInfo?.accountFormat ?: "Enter your loan account number",
                                                color = WealthSoftWhite.copy(alpha = 0.3f)
                                            )
                                        },
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
                                            Icon(
                                                Icons.Default.Tag,
                                                null,
                                                tint = WealthBlue.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ═══════════════ PHONE NUMBER FOR STK ═══════════════
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "M-PESA PHONE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthSoftWhite.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                // Show auto-detected badge if phone was found from SIM
                                if (isPhoneAutoDetected && phoneNumber.isNotEmpty()) {
                                    Surface(
                                        color = Color(0xFF00A651).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.SimCard,
                                                null,
                                                tint = Color(0xFF00D26A),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                "FROM SIM",
                                                color = Color(0xFF00D26A),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isPhoneAutoDetected && phoneNumber.isNotEmpty())
                                            Color(0xFF00A651).copy(alpha = 0.08f)
                                        else
                                            WealthSoftWhite.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isPhoneAutoDetected && phoneNumber.isNotEmpty())
                                            Color(0xFF00A651).copy(alpha = 0.3f)
                                        else
                                            WealthSoftWhite.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it.filter { c -> c.isDigit() }.take(12) },
                                    placeholder = { Text("254712345678", color = WealthSoftWhite.copy(alpha = 0.3f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WealthSoftWhite,
                                        unfocusedTextColor = WealthSoftWhite,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = Color(0xFF00A651)
                                    ),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PhoneAndroid,
                                            null,
                                            tint = Color(0xFF00A651).copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        // Show SIM selector if multiple SIMs available
                                        if (simPhoneNumbers.size > 1) {
                                            IconButton(
                                                onClick = { showSimSelector = !showSimSelector },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    if (showSimSelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Select SIM",
                                                    tint = Color(0xFF00A651).copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            // SIM Selector dropdown (if multiple SIMs)
                            if (showSimSelector && simPhoneNumbers.size > 1) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(WealthNavyLight.copy(alpha = 0.9f))
                                        .border(1.dp, WealthSoftWhite.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    simPhoneNumbers.forEachIndexed { index, simInfo ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (phoneNumber == simInfo.phoneNumber)
                                                        Color(0xFF00A651).copy(alpha = 0.2f)
                                                    else
                                                        Color.Transparent
                                                )
                                                .clickable {
                                                    phoneNumber = simInfo.phoneNumber
                                                    showSimSelector = false
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (simInfo.carrierName?.contains("Safaricom", ignoreCase = true) == true)
                                                                Color(0xFF00A651)
                                                            else if (simInfo.carrierName?.contains("Airtel", ignoreCase = true) == true)
                                                                Color(0xFFE53935)
                                                            else
                                                                WealthBlue
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "${simInfo.slotIndex + 1}",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        simInfo.phoneNumber,
                                                        color = WealthSoftWhite,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        simInfo.carrierName ?: "SIM ${simInfo.slotIndex + 1}",
                                                        color = WealthSoftWhite.copy(alpha = 0.5f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            if (phoneNumber == simInfo.phoneNumber) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    null,
                                                    tint = Color(0xFF00D26A),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Helper text
                            if (!isPhoneAutoDetected) {
                                Text(
                                    "Enter the M-Pesa registered phone number",
                                    color = WealthSoftWhite.copy(alpha = 0.4f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // ═══════════════ PAYMENT OPTIONS ═══════════════
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "PAYMENT METHOD",
                                style = MaterialTheme.typography.labelSmall,
                                color = WealthSoftWhite.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )

                            // ═══ OPTION 1: STK PUSH (Main - Recommended) ═══
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF00A651).copy(alpha = 0.2f),
                                                Color(0xFF00D26A).copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                                    .border(
                                        2.dp,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF00A651).copy(alpha = 0.6f),
                                                Color(0xFF00D26A).copy(alpha = 0.4f)
                                            )
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable(enabled = !isProcessingStkPush) {
                                        val payAmount = roundedAmount.toDouble()
                                        val phone = phoneNumber.ifEmpty { "" }
                                        val account = accountNumber.ifEmpty { loan.title }

                                        when {
                                            payAmount <= 0 -> {
                                                Toast
                                                    .makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT)
                                                    .show()
                                            }

                                            phone.length < 9 -> {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Enter phone number (e.g., 0712345678)",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }

                                            else -> {
                                                isProcessingStkPush = true
                                                stkPushStatus = "Sending STK Push..."

                                                // Use the real MpesaRepository for STK Push
                                                scope.launch {
                                                    try {
                                                        val result = mpesaRepository.initiateSTKPush(
                                                            phoneNumber = phone,
                                                            amount = payAmount,
                                                            accountReference = account,
                                                            transactionDesc = "Loan payment to ${loan.lenderName} via Budgie"
                                                        )

                                                        isProcessingStkPush = false
                                                        paymentResultSuccess = result.success
                                                        paymentResultMessage = result.message
                                                        showPaymentResult = true
                                                        stkPushStatus = if (result.success) "STK Push sent!" else "Error: ${result.message}"
                                                    } catch (e: Exception) {
                                                        isProcessingStkPush = false
                                                        paymentResultSuccess = false
                                                        paymentResultMessage = "Error: ${e.message ?: "Unknown error occurred"}"
                                                        showPaymentResult = true
                                                        stkPushStatus = "Error: ${e.message}"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF00A651),
                                                            Color(0xFF00D26A)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isProcessingStkPush) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.SendToMobile,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    "STK Push",
                                                    color = WealthSoftWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Surface(
                                                    color = Color(0xFF00D26A),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "INSTANT",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                if (isProcessingStkPush) "Processing payment..." else "Enter M-Pesa PIN on your phone",
                                                color = WealthSoftWhite.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.Lock,
                                        null,
                                        tint = Color(0xFF00D26A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // STK Push Status Message
                            if (stkPushStatus != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (stkPushStatus!!.startsWith("Error"))
                                                WealthMutedRed.copy(alpha = 0.15f)
                                            else
                                                Color(0xFF00A651).copy(alpha = 0.15f)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (stkPushStatus!!.startsWith("Error"))
                                                Icons.Default.Error
                                            else
                                                Icons.Default.CheckCircle,
                                            null,
                                            tint = if (stkPushStatus!!.startsWith("Error"))
                                                WealthMutedRed
                                            else
                                                Color(0xFF00D26A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            stkPushStatus!!,
                                            color = WealthSoftWhite.copy(alpha = 0.9f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Divider with text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = WealthSoftWhite.copy(alpha = 0.1f)
                                )
                                Text(
                                    "OR PAY MANUALLY",
                                    color = WealthSoftWhite.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = WealthSoftWhite.copy(alpha = 0.1f)
                                )
                            }

                            // ═══ OPTION 2 & 3: Manual Options Row ═══
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Open M-Pesa App
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF00A651).copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            Color(0xFF00A651).copy(alpha = 0.25f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { openMpesaAppForPayment(context) }
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF00A651)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "M",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            )
                                        }
                                        Text(
                                            "M-Pesa App",
                                            color = WealthSoftWhite,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Open SIM Toolkit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WealthAmber.copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            WealthAmber.copy(alpha = 0.25f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { openStkAppForPayment(context) }
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(WealthAmber),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.SimCard,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            "SIM Toolkit",
                                            color = WealthSoftWhite,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════ PAYMENT SUMMARY ═══════════════
                    if (paybillNumber.isNotEmpty() && roundedAmount > 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WealthBlue.copy(alpha = 0.1f))
                                    .border(1.dp, WealthBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Receipt,
                                            null,
                                            tint = WealthBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "PAYMENT SUMMARY",
                                            color = WealthBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    HorizontalDivider(color = WealthBlue.copy(alpha = 0.2f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Paybill", color = WealthSoftWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                                        Text(paybillNumber, color = WealthSoftWhite, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Account", color = WealthSoftWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                                        Text(
                                            accountNumber.ifEmpty { "Your loan account" },
                                            color = WealthSoftWhite,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Amount", color = WealthSoftWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                                        Text(
                                            "KES ${String.format("%,d", roundedAmount)}",
                                            color = Color(0xFF00D26A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════ ACTION BUTTONS ═══════════════
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, WealthSoftWhite.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    "Cancel",
                                    color = WealthSoftWhite.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Record Payment
                            Button(
                                onClick = {
                                    if (roundedAmount > 0) {
                                        onConfirm(roundedAmount.toDouble(), reference.ifEmpty { "Payment recorded" })
                                    }
                                },
                                enabled = roundedAmount > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WealthEmerald,
                                    disabledContainerColor = WealthSoftWhite.copy(alpha = 0.15f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Record", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

/**
 * Data class to hold SIM card phone number info
 */
data class SimPhoneInfo(
    val phoneNumber: String,
    val carrierName: String?,
    val slotIndex: Int
)

/**
 * Gets phone number(s) from SIM card(s) installed on the device
 * Returns a list of phone numbers (may be empty if not available or permission denied)
 *
 * Note: Many carriers don't store phone numbers on SIM cards, so this may return empty.
 */
@Suppress("DEPRECATION")
private fun getSimPhoneNumbers(context: Context): List<SimPhoneInfo> {
    val phoneNumbers = mutableListOf<SimPhoneInfo>()

    // Check if we have permission
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
        != PackageManager.PERMISSION_GRANTED) {
        android.util.Log.d("LoansScreen", "READ_PHONE_STATE permission not granted")
        return emptyList()
    }

    try {
        // Try using SubscriptionManager for dual SIM support (API 22+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

            if (subscriptionManager != null) {
                try {
                    val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

                    if (subscriptionInfoList != null) {
                        for (subscriptionInfo in subscriptionInfoList) {
                            val number = subscriptionInfo.number
                            if (!number.isNullOrBlank()) {
                                val formattedNumber = formatPhoneNumberTo254(number)
                                if (formattedNumber != null) {
                                    phoneNumbers.add(
                                        SimPhoneInfo(
                                            phoneNumber = formattedNumber,
                                            carrierName = subscriptionInfo.carrierName?.toString(),
                                            slotIndex = subscriptionInfo.simSlotIndex
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("LoansScreen", "SecurityException getting subscription info: ${e.message}")
                }
            }
        }

        // Fallback: Try TelephonyManager for primary SIM
        if (phoneNumbers.isEmpty()) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            if (telephonyManager != null) {
                try {
                    val line1Number = telephonyManager.line1Number
                    if (!line1Number.isNullOrBlank()) {
                        val formattedNumber = formatPhoneNumberTo254(line1Number)
                        if (formattedNumber != null) {
                            phoneNumbers.add(
                                SimPhoneInfo(
                                    phoneNumber = formattedNumber,
                                    carrierName = telephonyManager.networkOperatorName,
                                    slotIndex = 0
                                )
                            )
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("LoansScreen", "SecurityException getting line1Number: ${e.message}")
                }
            }
        }

    } catch (e: Exception) {
        android.util.Log.e("LoansScreen", "Error getting SIM phone numbers: ${e.message}")
    }

    return phoneNumbers
}

/**
 * Format phone number to 254XXXXXXXXX format (Kenya)
 */
private fun formatPhoneNumberTo254(phone: String): String? {
    // Remove all non-digit characters
    val cleanPhone = phone.replace(Regex("[^0-9]"), "")

    return when {
        // Already in 254 format with 12 digits
        cleanPhone.startsWith("254") && cleanPhone.length == 12 -> cleanPhone
        // Starts with + and 254
        cleanPhone.startsWith("254") && cleanPhone.length == 12 -> cleanPhone
        // Starts with 0 (local format) - 10 digits
        cleanPhone.startsWith("0") && cleanPhone.length == 10 -> "254${cleanPhone.substring(1)}"
        // Starts with 7 (without leading 0) - 9 digits
        cleanPhone.startsWith("7") && cleanPhone.length == 9 -> "254$cleanPhone"
        // Just 9 digits starting with 7
        cleanPhone.length == 9 && cleanPhone.first() == '7' -> "254$cleanPhone"
        // Starts with 1 (some Airtel numbers) - 9 digits
        cleanPhone.startsWith("1") && cleanPhone.length == 9 -> "254$cleanPhone"
        // Already 9 digits
        cleanPhone.length == 9 -> "254$cleanPhone"
        else -> null
    }
}

/**
 * Opens M-Pesa app for payment
 */
private fun openMpesaAppForPayment(context: Context) {
    val mpesaPackage = "com.safaricom.mpesa.lifestyle"

    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(mpesaPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Toast.makeText(context, "Opening M-Pesa...", Toast.LENGTH_SHORT).show()
            return
        }
    } catch (e: Exception) {
        android.util.Log.e("LoansScreen", "M-Pesa launch failed: ${e.message}")
    }

    // Fallback: Search installed apps
    try {
        val installedApps = context.packageManager.getInstalledApplications(0)
        for (app in installedApps) {
            val packageName = app.packageName.lowercase()
            if (packageName.contains("mpesa") || packageName.contains("safaricom")) {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Toast.makeText(context, "Opening M-Pesa...", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("LoansScreen", "App search failed: ${e.message}")
    }

    Toast.makeText(context, "M-Pesa app not found. Please open manually.", Toast.LENGTH_LONG).show()
}

/**
 * Opens SIM Toolkit app for USSD payment
 */
private fun openStkAppForPayment(context: Context) {
    val stkPackages = listOf(
        "com.android.stk",
        "com.android.stk2",
        "com.mediatek.stk",
        "com.qualcomm.qti.simkit",
        "com.samsung.android.stk",
        "com.huawei.stk",
        "com.oppo.stk",
        "com.vivo.stk",
        "com.xiaomi.stk"
    )

    for (packageName in stkPackages) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "Opening SIM Toolkit...", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            continue
        }
    }

    // Fallback
    try {
        val intent = Intent().apply {
            action = "android.intent.action.MAIN"
            addCategory("android.intent.category.LAUNCHER")
            setClassName("com.android.stk", "com.android.stk.StkLauncherActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Toast.makeText(context, "Opening SIM Toolkit...", Toast.LENGTH_SHORT).show()
        return
    } catch (e: Exception) {
        android.util.Log.e("LoansScreen", "STK launch failed: ${e.message}")
    }

    Toast.makeText(context, "SIM Toolkit not found. Open from phone settings.", Toast.LENGTH_LONG).show()
}

/**
 * Initiates M-Pesa STK Push using Safaricom Daraja API
 * This sends a payment prompt directly to the user's phone
 */
private fun initiateStkPush(
    context: Context,
    phoneNumber: String,
    amount: Double,
    paybill: String,
    accountNumber: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    // Format phone number (ensure it starts with 254)
    val formattedPhone = formatPhoneNumber(phoneNumber)
    if (formattedPhone == null) {
        onError("Invalid phone number format. Use 254XXXXXXXXX")
        return
    }

    // In a production app, you would:
    // 1. Call your backend server (NOT directly to Safaricom from mobile)
    // 2. Your backend would authenticate with Safaricom and initiate STK Push
    // 3. Your backend returns status to the app

    // For now, we'll simulate the flow and show instructions
    // because Daraja API requires server-side implementation for security

    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        try {
            // Simulate API call delay
            kotlinx.coroutines.delay(2000)

            // In production, this would be a real API response
            // For demo, we'll show the user what they need to do manually
            val message = """
                STK Push initiated!
                
                📱 Check your phone for M-Pesa prompt
                
                Details:
                • Paybill: $paybill
                • Account: $accountNumber
                • Amount: KES ${String.format("%,.0f", amount)}
                • Phone: $formattedPhone
                
                Enter your M-Pesa PIN to complete payment.
            """.trimIndent()

            // Show detailed instructions
            onSuccess("STK Push sent! Check your phone for M-Pesa prompt.")

            // Also show a notification with payment details
            showPaymentNotification(context, paybill, accountNumber, amount)

        } catch (e: Exception) {
            onError("Failed to initiate STK Push: ${e.message}")
        }
    }
}

/**
 * Format phone number to 254XXXXXXXXX format
 */
private fun formatPhoneNumber(phone: String): String? {
    val cleanPhone = phone.replace(Regex("[^0-9]"), "")

    return when {
        cleanPhone.startsWith("254") && cleanPhone.length == 12 -> cleanPhone
        cleanPhone.startsWith("0") && cleanPhone.length == 10 -> "254${cleanPhone.substring(1)}"
        cleanPhone.startsWith("7") && cleanPhone.length == 9 -> "254$cleanPhone"
        cleanPhone.length == 9 -> "254$cleanPhone"
        else -> null
    }
}

/**
 * Show notification with payment details for reference
 */
private fun showPaymentNotification(
    context: Context,
    paybill: String,
    accountNumber: String,
    amount: Double
) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "payment_channel",
                "Payment Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for M-Pesa payments"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, "payment_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("M-Pesa Payment")
            .setContentText("Paybill: $paybill | Account: $accountNumber | KES ${String.format("%,.0f", amount)}")
            .setStyle(
                androidx.core.app.NotificationCompat.BigTextStyle()
                    .bigText("""
                        Paybill: $paybill
                        Account: $accountNumber
                        Amount: KES ${String.format("%,.0f", amount)}
                        
                        If STK Push doesn't arrive, dial *334# or open M-Pesa app.
                    """.trimIndent())
            )
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    } catch (e: Exception) {
        android.util.Log.e("LoansScreen", "Failed to show notification: ${e.message}")
    }
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A651))
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

