package com.example.budgie.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.budgie.data.model.*
import com.example.budgie.ui.components.*
import com.example.budgie.ui.theme.WealthTheme
import com.example.budgie.ui.viewmodel.MainViewModel
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment as PdfTextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: MainViewModel,
    onAddBill: () -> Unit,
    onBack: () -> Unit
) {
    val unpaidBills by viewModel.unpaidBills.collectAsState()
    val paidBills by viewModel.paidBills.collectAsState()
    val overdueBills by viewModel.overdueBills.collectAsState()
    val totalUnpaid by viewModel.totalUnpaidBills.collectAsState()

    // Combine all bills for export
    val allBills = unpaidBills + paidBills

    var showExportDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedBillForPayment by remember { mutableStateOf<Bill?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var billToDelete by remember { mutableStateOf<Bill?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Limit unpaid bills to last 10
    val displayedUnpaidBills = unpaidBills.take(10)

    // Export Dialog
    if (showExportDialog && allBills.isNotEmpty()) {
        BillsExportDialog(
            bills = allBills,
            context = context,
            onDismiss = { showExportDialog = false }
        )
    }

    // Payment Dialog for selected bill
    if (showPaymentDialog && selectedBillForPayment != null) {
        BillPaymentDialog(
            bill = selectedBillForPayment!!,
            context = context,
            onDismiss = {
                showPaymentDialog = false
                selectedBillForPayment = null
            },
            onPaymentComplete = {
                // Mark as paid after successful payment
                viewModel.markBillAsPaid(selectedBillForPayment!!.id, true)
                showPaymentDialog = false
                selectedBillForPayment = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && billToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                billToDelete = null
            },
            containerColor = WealthTheme.Navy,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(WealthTheme.MutedRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        null,
                        tint = WealthTheme.MutedRed,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete Bill",
                    color = WealthTheme.SoftWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${billToDelete?.title}\"? This action cannot be undone.",
                    color = WealthTheme.SoftWhite.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        billToDelete?.let { viewModel.deleteBill(it) }
                        showDeleteConfirmation = false
                        billToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WealthTheme.MutedRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteConfirmation = false
                        billToDelete = null
                    },
                    border = BorderStroke(1.dp, WealthTheme.SoftWhite.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = WealthTheme.SoftWhite)
                }
            }
        )
    }

    Scaffold(
        containerColor = WealthTheme.Navy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bills & Payments",
                        color = WealthTheme.SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WealthTheme.SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WealthTheme.Navy
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Export FAB
                if (allBills.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showExportDialog = true },
                        containerColor = WealthTheme.Blue,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.FileUpload, "Export", modifier = Modifier.size(22.dp))
                    }
                }
                // Add Bill FAB
                FloatingActionButton(
                    onClick = onAddBill,
                    containerColor = WealthTheme.Emerald,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Add Bill")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Due Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (overdueBills.isNotEmpty()) WealthTheme.MutedRed.copy(alpha = 0.5f)
                                else WealthTheme.Emerald.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Total Due",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (totalUnpaid > 0) formatCurrency(totalUnpaid) else "—",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (overdueBills.isNotEmpty()) WealthTheme.MutedRed else WealthTheme.SoftWhite
                            )
                        }
                    }
                    // Overdue Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (overdueBills.isNotEmpty()) WealthTheme.MutedRed.copy(alpha = 0.5f)
                                else WealthTheme.SoftWhite.copy(alpha = 0.1f),
                                RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Overdue",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (overdueBills.isNotEmpty()) "${overdueBills.size} bills" else "None",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (overdueBills.isNotEmpty()) WealthTheme.MutedRed else WealthTheme.Emerald
                            )
                        }
                    }
                }
            }

            // ═══════════════ PAID BILLS SECTION ═══════════════
            if (paidBills.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(WealthTheme.Emerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = WealthTheme.Emerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Paid",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.Emerald
                            )
                        }
                        Surface(
                            color = WealthTheme.Emerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${paidBills.size}",
                                color = WealthTheme.Emerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(paidBills.take(5)) { bill ->
                    BillItemCard(
                        bill = bill,
                        isOverdue = false,
                        isPaid = true,
                        onPayClick = { /* Already paid */ },
                        onMarkPaidClick = { /* Already paid */ },
                        onMarkUnpaidClick = {
                            viewModel.markBillAsPaid(bill.id, false)
                        },
                        onDeleteClick = {
                            billToDelete = bill
                            showDeleteConfirmation = true
                        }
                    )
                }
            }

            // ═══════════════ OVERDUE BILLS SECTION ═══════════════
            if (overdueBills.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(WealthTheme.MutedRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WealthTheme.MutedRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Overdue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.MutedRed
                            )
                        }
                        Surface(
                            color = WealthTheme.MutedRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${overdueBills.size}",
                                color = WealthTheme.MutedRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(overdueBills) { bill ->
                    BillItemCard(
                        bill = bill,
                        isOverdue = true,
                        isPaid = false,
                        onPayClick = {
                            selectedBillForPayment = bill
                            showPaymentDialog = true
                        },
                        onMarkPaidClick = {
                            viewModel.markBillAsPaid(bill.id, true)
                        },
                        onMarkUnpaidClick = { /* Not applicable for unpaid bills */ },
                        onDeleteClick = {
                            billToDelete = bill
                            showDeleteConfirmation = true
                        }
                    )
                }
            }

            // ═══════════════ UPCOMING BILLS SECTION ═══════════════
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(WealthTheme.Gold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = WealthTheme.Gold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Upcoming",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.SoftWhite
                        )
                    }
                    if (displayedUnpaidBills.filter { it !in overdueBills }.isNotEmpty()) {
                        Surface(
                            color = WealthTheme.Gold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${displayedUnpaidBills.filter { it !in overdueBills }.size}",
                                color = WealthTheme.Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            val upcomingBills = displayedUnpaidBills.filter { it !in overdueBills }
            if (upcomingBills.isNotEmpty()) {
                items(upcomingBills) { bill ->
                    BillItemCard(
                        bill = bill,
                        isOverdue = false,
                        isPaid = false,
                        onPayClick = {
                            selectedBillForPayment = bill
                            showPaymentDialog = true
                        },
                        onMarkPaidClick = {
                            viewModel.markBillAsPaid(bill.id, true)
                        },
                        onMarkUnpaidClick = { /* Not applicable for unpaid bills */ },
                        onDeleteClick = {
                            billToDelete = bill
                            showDeleteConfirmation = true
                        }
                    )
                }
            } else if (unpaidBills.isEmpty() && paidBills.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = WealthTheme.Emerald.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = WealthTheme.Emerald
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No bills yet!",
                                style = MaterialTheme.typography.titleMedium,
                                color = WealthTheme.SoftWhite,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Add your first bill to start tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else if (upcomingBills.isEmpty() && unpaidBills.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = WealthTheme.Emerald.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Celebration,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = WealthTheme.Emerald
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "All bills paid!",
                                style = MaterialTheme.typography.titleMedium,
                                color = WealthTheme.SoftWhite,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "You're on top of your finances",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// NEW BILL ITEM CARD WITH 3 ACTION BUTTONS
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun BillItemCard(
    bill: Bill,
    isOverdue: Boolean,
    isPaid: Boolean,
    onPayClick: () -> Unit,
    onMarkPaidClick: () -> Unit,
    onMarkUnpaidClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val accentColor = when {
        isPaid -> WealthTheme.Emerald
        isOverdue -> WealthTheme.MutedRed
        else -> WealthTheme.Gold
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                accentColor.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Bill Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = bill.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = WealthTheme.SoftWhite
                        )
                        if (isPaid) {
                            Surface(
                                color = WealthTheme.Emerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = WealthTheme.Emerald,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        "PAID",
                                        color = WealthTheme.Emerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Due: ${formatDate(bill.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) WealthTheme.MutedRed else WealthTheme.SoftWhite.copy(alpha = 0.5f)
                    )
                    if (bill.category != null) {
                        Text(
                            text = bill.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.4f)
                        )
                    }
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(bill.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) WealthTheme.Emerald
                               else if (isOverdue) WealthTheme.MutedRed
                               else WealthTheme.SoftWhite
                    )
                    if (isOverdue && !isPaid) {
                        Text(
                            "OVERDUE",
                            color = WealthTheme.MutedRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action Buttons Row
            if (!isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pay Button
                    Button(
                        onClick = onPayClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A651)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Payment,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Pay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Mark as Paid Button
                    OutlinedButton(
                        onClick = onMarkPaidClick,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, WealthTheme.Emerald),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = WealthTheme.Emerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Paid",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WealthTheme.Emerald
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WealthTheme.MutedRed.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = WealthTheme.MutedRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // For paid bills, show Mark Unpaid and Delete buttons
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mark as Unpaid Button
                    OutlinedButton(
                        onClick = onMarkUnpaidClick,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, WealthTheme.Amber),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            null,
                            tint = WealthTheme.Amber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Mark Unpaid",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WealthTheme.Amber
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WealthTheme.MutedRed.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = WealthTheme.MutedRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Remove old PremiumBillItem - replaced by BillItemCard above

// Data class for bill entry
private data class BillEntry(
    val id: Int,
    val title: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.UTILITIES,
    val dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
    val isRecurring: Boolean = false,
    val categoryExpanded: Boolean = false,
    val showDatePicker: Boolean = false
)

// Glassmorphism for bill screen
private fun Modifier.billGlassmorphicCard(accentColor: Color, cornerRadius: Int = 14) = this
    .shadow(6.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = accentColor.copy(alpha = 0.2f))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f))
        )
    )
    .border(1.dp, Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.3f), accentColor.copy(alpha = 0.1f))), RoundedCornerShape(cornerRadius.dp))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    // Bill entries - start with 1 empty row, auto-adds more
    var billEntries by remember {
        mutableStateOf(listOf(BillEntry(id = 1)))
    }

    // Date picker state for individual bills
    var showDatePickerFor by remember { mutableStateOf<Int?>(null) }
    val datePickerState = rememberDatePickerState()

    // Auto-add new row when last row is filled
    LaunchedEffect(billEntries) {
        val lastEntry = billEntries.lastOrNull()
        if (lastEntry != null && lastEntry.title.isNotBlank() && lastEntry.amount.isNotBlank()) {
            val newId = (billEntries.maxOfOrNull { it.id } ?: 0) + 1
            billEntries = billEntries + BillEntry(id = newId)
        }
    }

    // Count valid entries
    val validEntries = billEntries.filter {
        it.title.isNotBlank() && it.amount.isNotBlank() && (it.amount.toDoubleOrNull() ?: 0.0) > 0
    }

    // Date picker dialog
    showDatePickerFor?.let { entryIndex ->
        val entry = billEntries.getOrNull(entryIndex)
        if (entry != null) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerFor = null },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            billEntries = billEntries.toMutableList().also {
                                it[entryIndex] = entry.copy(dueDate = selectedDate)
                            }
                        }
                        showDatePickerFor = null
                    }) {
                        Text("OK", color = WealthTheme.Emerald)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerFor = null }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(WealthTheme.Navy, Color(0xFF0D2E3D), WealthTheme.Navy)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(WealthTheme.SoftWhite.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = WealthTheme.SoftWhite)
                    }

                    Text(
                        "Add Bills",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthTheme.SoftWhite
                    )

                    // Badge showing count
                    if (validEntries.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WealthTheme.Emerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${validEntries.size}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .billGlassmorphicCard(WealthTheme.Amber, 20)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(WealthTheme.Amber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = WealthTheme.Amber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "Manage Bills",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                "New rows appear automatically!",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Bill Entry Rows Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = WealthTheme.Amber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Bill Entries",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.SoftWhite
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${validEntries.size} valid",
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthTheme.Emerald
                        )
                    }
                }

                // Bill Entry Rows
                items(billEntries.size) { index ->
                    val entry = billEntries[index]
                    val isUsed = entry.title.isNotBlank() || entry.amount.isNotBlank()
                    val isValid = entry.title.isNotBlank() && entry.amount.isNotBlank() && (entry.amount.toDoubleOrNull() ?: 0.0) > 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isValid) WealthTheme.Emerald.copy(alpha = 0.5f)
                                       else WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isValid) WealthTheme.Emerald.copy(alpha = 0.08f)
                                            else WealthTheme.SoftWhite.copy(alpha = 0.03f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header row with number badge and status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Number badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isValid) WealthTheme.Emerald
                                                else WealthTheme.SoftWhite.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isValid) Color.White else WealthTheme.SoftWhite.copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        if (isValid) "Ready" else "Fill details",
                                        fontSize = 12.sp,
                                        color = if (isValid) WealthTheme.Emerald else WealthTheme.SoftWhite.copy(alpha = 0.5f)
                                    )
                                }

                                if (billEntries.size > 1) {
                                    IconButton(
                                        onClick = {
                                            billEntries = billEntries.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(WealthTheme.MutedRed.copy(alpha = 0.1f))
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = WealthTheme.MutedRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bill Title
                            OutlinedTextField(
                                value = entry.title,
                                onValueChange = { newTitle ->
                                    billEntries = billEntries.toMutableList().also {
                                        it[index] = entry.copy(title = newTitle)
                                    }
                                },
                                label = { Text("Bill Title", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                placeholder = { Text("e.g., Electricity, Rent, Internet", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Receipt,
                                        null,
                                        tint = WealthTheme.Amber.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WealthTheme.Emerald,
                                    unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.2f),
                                    focusedTextColor = WealthTheme.SoftWhite,
                                    unfocusedTextColor = WealthTheme.SoftWhite,
                                    cursorColor = WealthTheme.Emerald
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Category dropdown
                            ExposedDropdownMenuBox(
                                expanded = entry.categoryExpanded,
                                onExpandedChange = { expanded ->
                                    billEntries = billEntries.toMutableList().also {
                                        it[index] = entry.copy(categoryExpanded = expanded)
                                    }
                                }
                            ) {
                                OutlinedTextField(
                                    value = "${entry.category.icon} ${entry.category.displayName}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Category,
                                            null,
                                            tint = WealthTheme.Gold.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entry.categoryExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WealthTheme.Emerald,
                                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.2f),
                                        focusedTextColor = WealthTheme.SoftWhite,
                                        unfocusedTextColor = WealthTheme.SoftWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = entry.categoryExpanded,
                                    onDismissRequest = {
                                        billEntries = billEntries.toMutableList().also {
                                            it[index] = entry.copy(categoryExpanded = false)
                                        }
                                    }
                                ) {
                                    ExpenseCategory.entries.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text("${category.icon} ${category.displayName}") },
                                            onClick = {
                                                billEntries = billEntries.toMutableList().also {
                                                    it[index] = entry.copy(category = category, categoryExpanded = false)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Due Date picker
                            OutlinedTextField(
                                value = formatDate(entry.dueDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Due Date", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        null,
                                        tint = WealthTheme.MutedRed.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showDatePickerFor = index }
                                    ) {
                                        Icon(
                                            Icons.Default.EditCalendar,
                                            "Select date",
                                            tint = WealthTheme.Emerald
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePickerFor = index },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WealthTheme.Emerald,
                                    unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.2f),
                                    focusedTextColor = WealthTheme.SoftWhite,
                                    unfocusedTextColor = WealthTheme.SoftWhite
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Recurring toggle card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (entry.isRecurring) WealthTheme.Amber.copy(alpha = 0.1f)
                                                    else WealthTheme.SoftWhite.copy(alpha = 0.03f)
                                ),
                                border = if (entry.isRecurring) BorderStroke(1.dp, WealthTheme.Amber.copy(alpha = 0.3f)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            billEntries = billEntries.toMutableList().also {
                                                it[index] = entry.copy(isRecurring = !entry.isRecurring)
                                            }
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Repeat,
                                            null,
                                            tint = if (entry.isRecurring) WealthTheme.Amber else WealthTheme.SoftWhite.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                "Recurring Bill",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (entry.isRecurring) WealthTheme.Amber else WealthTheme.SoftWhite.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                "Repeats monthly",
                                                fontSize = 11.sp,
                                                color = WealthTheme.SoftWhite.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = entry.isRecurring,
                                        onCheckedChange = { checked ->
                                            billEntries = billEntries.toMutableList().also {
                                                it[index] = entry.copy(isRecurring = checked)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = WealthTheme.Amber,
                                            checkedTrackColor = WealthTheme.Amber.copy(alpha = 0.3f),
                                            uncheckedThumbColor = WealthTheme.SoftWhite.copy(alpha = 0.5f),
                                            uncheckedTrackColor = WealthTheme.SoftWhite.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Amount field
                            OutlinedTextField(
                                value = entry.amount,
                                onValueChange = { newAmount ->
                                    billEntries = billEntries.toMutableList().also {
                                        it[index] = entry.copy(amount = newAmount.filter { c -> c.isDigit() || c == '.' })
                                    }
                                },
                                label = { Text("Amount", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                placeholder = { Text("0.00", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(WealthTheme.Emerald.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$",
                                            color = WealthTheme.Emerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WealthTheme.Emerald,
                                    unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.2f),
                                    focusedTextColor = WealthTheme.SoftWhite,
                                    unfocusedTextColor = WealthTheme.SoftWhite,
                                    cursorColor = WealthTheme.Emerald
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Total summary
                if (validEntries.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .billGlassmorphicCard(WealthTheme.Gold, 14)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total (${validEntries.size} bills)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthTheme.SoftWhite
                                )
                                Text(
                                    formatCurrency(validEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthTheme.Gold
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }

            // Save Button - moved up to avoid phone navigation buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                WealthTheme.Navy.copy(alpha = 0.95f),
                                WealthTheme.Navy
                            )
                        )
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                Button(
                    onClick = {
                        // Save all valid bills
                        validEntries.forEach { entry ->
                            viewModel.addBill(
                                Bill(
                                    title = entry.title,
                                    amount = entry.amount.toDoubleOrNull() ?: 0.0,
                                    dueDate = entry.dueDate,
                                    category = entry.category,
                                    isRecurring = entry.isRecurring
                                )
                            )
                        }
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            if (validEntries.isNotEmpty()) 12.dp else 0.dp,
                            RoundedCornerShape(16.dp),
                            ambientColor = WealthTheme.Emerald.copy(alpha = 0.4f)
                        ),
                    enabled = validEntries.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WealthTheme.Emerald,
                        disabledContainerColor = WealthTheme.SoftWhite.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (validEntries.isEmpty()) "Add at least one bill"
                        else "Save ${validEntries.size} Bill${if (validEntries.size > 1) "s" else ""}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ========== BILLS EXPORT DIALOG ==========
@Composable
private fun BillsExportDialog(
    bills: List<Bill>,
    context: Context,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("PDF") }
    var isExporting by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    if (showSuccessDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false; onDismiss() },
            containerColor = WealthTheme.Navy,
            icon = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.size(56.dp).background(WealthTheme.Emerald.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.CheckCircle, null, tint = WealthTheme.Emerald, modifier = Modifier.size(32.dp)) }
                }
            },
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Export Successful!", color = WealthTheme.SoftWhite, fontWeight = FontWeight.Bold) } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your bills report has been exported.", color = WealthTheme.SoftWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportedFile?.name ?: "", color = WealthTheme.Emerald, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { exportedFile?.let { shareBillsFile(context, it, if (selectedFormat == "PDF") "application/pdf" else "text/csv") }; showSuccessDialog = false; onDismiss() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthTheme.Emerald), border = BorderStroke(1.dp, WealthTheme.Emerald)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Share")
                        }
                        Button(onClick = { exportedFile?.let { openBillsFile(context, it) }; showSuccessDialog = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = WealthTheme.Emerald)) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Open")
                        }
                    }
                    TextButton(onClick = { showSuccessDialog = false; onDismiss() }) { Text("Done", color = WealthTheme.SoftWhite.copy(alpha = 0.7f)) }
                }
            },
            dismissButton = null
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WealthTheme.Navy,
        title = { Text("Export Bills", color = WealthTheme.SoftWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Export ${bills.size} bill(s)", color = WealthTheme.SoftWhite.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = selectedFormat == "PDF", onClick = { selectedFormat = "PDF" }, label = { Text("PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF5C9CE5)), modifier = Modifier.weight(1f))
                    FilterChip(selected = selectedFormat == "Excel", onClick = { selectedFormat = "Excel" }, label = { Text("Excel") },
                        leadingIcon = { Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WealthTheme.Emerald), modifier = Modifier.weight(1f))
                }
                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WealthTheme.Emerald, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp)); Text("Exporting...", color = WealthTheme.SoftWhite.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                isExporting = true
                val file = exportBillsData(context, bills, selectedFormat)
                isExporting = false
                if (file != null) { exportedFile = file; showSuccessDialog = true }
                else Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }, enabled = !isExporting, colors = ButtonDefaults.buttonColors(containerColor = if (selectedFormat == "PDF") Color(0xFF5C9CE5) else WealthTheme.Emerald)) {
                Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Export")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = WealthTheme.SoftWhite) } }
    )
}

private fun exportBillsData(context: Context, bills: List<Bill>, format: String): File? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    return try {
        when (format) {
            "Excel" -> {
                val file = File(context.getExternalFilesDir(null), "Bills_Report_$timestamp.csv")
                FileWriter(file).use { writer ->
                    writer.append("Bills Report\nGenerated,${dateFormat.format(Date())}\n\n")
                    writer.append("Title,Amount,Due Date,Category,Recurring,Paid\n")
                    bills.forEach { bill ->
                        writer.append("${bill.title},$${String.format("%.2f", bill.amount)},${dateFormat.format(Date(bill.dueDate))},${bill.category.displayName},${bill.isRecurring},${bill.isPaid}\n")
                    }
                }
                file
            }
            "PDF" -> {
                val file = File(context.getExternalFilesDir(null), "Bills_Report_$timestamp.pdf")
                val navyPrimary = DeviceRgb(11, 31, 42)
                val navySecondary = DeviceRgb(15, 40, 55)
                val emeraldColor = DeviceRgb(15, 174, 150)
                val softWhite = DeviceRgb(230, 241, 240)
                val whiteColor = DeviceRgb(255, 255, 255)
                val lightGray = DeviceRgb(245, 247, 250)
                val redColor = DeviceRgb(229, 115, 115)

                val pdfWriter = PdfWriter(file)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                document.setMargins(36f, 36f, 36f, 36f)

                // Header
                val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
                val headerCell = Cell().setBackgroundColor(navyPrimary).setPadding(20f).setBorder(null)
                headerCell.add(Paragraph("BUDGIE").setFontSize(28f).setBold().setFontColor(emeraldColor).setTextAlignment(PdfTextAlignment.CENTER))
                headerCell.add(Paragraph("BILLS REPORT").setFontSize(16f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.CENTER).setMarginTop(-5f))
                headerCell.add(Paragraph("Your Personal Finance Companion").setFontSize(10f).setFontColor(DeviceRgb(150, 180, 175)).setTextAlignment(PdfTextAlignment.CENTER).setItalic().setMarginTop(8f))
                headerTable.addCell(headerCell)
                document.add(headerTable)

                // Info bar
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Total Bills: ${bills.size}").setFontSize(11f).setFontColor(softWhite)))
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Generated: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}").setFontSize(11f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.RIGHT)))
                document.add(infoTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Summary
                val unpaidBillsList = bills.filter { !it.isPaid }
                val paidBillsList = bills.filter { it.isPaid }
                val totalUnpaid = unpaidBillsList.sumOf { it.amount }
                val totalPaid = paidBillsList.sumOf { it.amount }

                document.add(Paragraph("SUMMARY").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f, 1f))).useAllAvailableWidth()
                listOf("Unpaid", "Paid", "Total Unpaid", "Total Paid").forEach { header ->
                    summaryTable.addCell(Cell().add(Paragraph(header).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                summaryTable.addCell(Cell().add(Paragraph(unpaidBillsList.size.toString()).setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(redColor))
                summaryTable.addCell(Cell().add(Paragraph(paidBillsList.size.toString()).setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(emeraldColor))
                summaryTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", totalUnpaid)}").setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(redColor))
                summaryTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", totalPaid)}").setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(emeraldColor))
                document.add(summaryTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Bills table
                document.add(Paragraph("BILLS DETAILS").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val billsTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1.2f, 1.2f, 0.8f))).useAllAvailableWidth()
                listOf("Title", "Amount", "Due Date", "Category", "Status").forEach { header ->
                    billsTable.addCell(Cell().add(Paragraph(header).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                bills.forEachIndexed { index, bill ->
                    val bgColor = if (index % 2 == 0) whiteColor else lightGray
                    billsTable.addCell(Cell().add(Paragraph(bill.title).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f))
                    billsTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", bill.amount)}").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.RIGHT))
                    billsTable.addCell(Cell().add(Paragraph(dateFormat.format(Date(bill.dueDate))).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                    billsTable.addCell(Cell().add(Paragraph(bill.category.displayName).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                    billsTable.addCell(Cell().add(Paragraph(if (bill.isPaid) "Paid" else "Unpaid").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(if (bill.isPaid) emeraldColor else redColor))
                }
                document.add(billsTable)

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
    } catch (e: Exception) { e.printStackTrace(); null }
}

private fun shareBillsFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Bills Report"))
}

private fun openBillsFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (file.extension == "pdf") "application/pdf" else "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) { Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show() }
}

// ═══════════════════════════════════════════════════════════════════
// BILL PAYMENT DIALOG - For paying a specific bill with auto-filled amount
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun BillPaymentDialog(
    bill: Bill,
    context: Context,
    onDismiss: () -> Unit,
    onPaymentComplete: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(kotlin.math.ceil(bill.amount).toInt().toString()) }
    var accountNumber by remember { mutableStateOf(bill.title) }
    var paybillNumber by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(0) } // 0=STK Push, 1=M-Pesa, 2=STK
    var isProcessing by remember { mutableStateOf(false) }
    var showPaymentResult by remember { mutableStateOf(false) }
    var paymentResultSuccess by remember { mutableStateOf(false) }
    var paymentResultMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val mpesaRepository = remember { com.example.budgie.mpesa.MpesaRepository.getInstance() }

    // Round amount whenever it changes
    val roundedAmount = remember(amount) {
        amount.toDoubleOrNull()?.let { kotlin.math.ceil(it).toInt() } ?: 0
    }

    // Do NOT auto-fill paybill or account - user must enter manually

    // Premium Futuristic STK Push Result Modal
    if (showPaymentResult) {
        Dialog(
            onDismissRequest = {
                showPaymentResult = false
                if (paymentResultSuccess) {
                    onPaymentComplete()
                }
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
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
                                    Color(0xFF0D2137),
                                    Color(0xFF0A1A28),
                                    Color(0xFF061218)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = if (paymentResultSuccess) listOf(
                                    Color(0xFF00D26A).copy(alpha = 0.7f),
                                    Color(0xFF0FAE96).copy(alpha = 0.3f),
                                    Color(0xFF00D26A).copy(alpha = 0.1f)
                                ) else listOf(
                                    WealthTheme.Amber.copy(alpha = 0.7f),
                                    WealthTheme.Amber.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Premium Animated Success/Warning Icon with Glow Effect
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer glow ring
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = if (paymentResultSuccess) listOf(
                                                Color(0xFF00D26A).copy(alpha = 0.35f),
                                                Color(0xFF00D26A).copy(alpha = 0.15f),
                                                Color.Transparent
                                            ) else listOf(
                                                WealthTheme.Amber.copy(alpha = 0.35f),
                                                WealthTheme.Amber.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            // Middle ring with border
                            Box(
                                modifier = Modifier
                                    .size(85.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (paymentResultSuccess) Color(0xFF00D26A).copy(alpha = 0.12f)
                                        else WealthTheme.Amber.copy(alpha = 0.12f)
                                    )
                                    .border(
                                        2.dp,
                                        if (paymentResultSuccess) Color(0xFF00D26A).copy(alpha = 0.5f)
                                        else WealthTheme.Amber.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            )
                            // Inner gradient icon circle
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = if (paymentResultSuccess) listOf(
                                                Color(0xFF00D26A),
                                                Color(0xFF0FAE96)
                                            ) else listOf(
                                                WealthTheme.Amber,
                                                Color(0xFFFF9800)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (paymentResultSuccess) Icons.Default.Check else Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Title
                        Text(
                            text = if (paymentResultSuccess) "Payment Initiated" else "Action Required",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.SoftWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                            text = if (paymentResultSuccess) "STK Push sent to your phone" else "Unable to process payment",
                            fontSize = 14.sp,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (paymentResultSuccess) {
                            // Premium instruction card with steps
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF00D26A).copy(alpha = 0.06f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    Color(0xFF00D26A).copy(alpha = 0.25f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Phone indicator with pulse effect
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF00D26A).copy(alpha = 0.3f),
                                                            Color(0xFF0FAE96).copy(alpha = 0.2f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PhoneAndroid,
                                                null,
                                                tint = Color(0xFF00D26A),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Check Your Phone",
                                                color = Color(0xFF00D26A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                "M-Pesa PIN prompt awaiting",
                                                color = WealthTheme.SoftWhite.copy(alpha = 0.5f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Step indicators - Futuristic flow
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StepIndicator(
                                            number = "1",
                                            label = "Prompt",
                                            isActive = true,
                                            accentColor = Color(0xFF00D26A)
                                        )
                                        // Connecting line
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.dp)
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF00D26A).copy(alpha = 0.5f),
                                                            Color(0xFF00D26A).copy(alpha = 0.2f)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(1.dp)
                                                )
                                        )
                                        StepIndicator(
                                            number = "2",
                                            label = "PIN",
                                            isActive = false,
                                            accentColor = Color(0xFF00D26A)
                                        )
                                        // Connecting line
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.dp)
                                                .background(
                                                    WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(1.dp)
                                                )
                                        )
                                        StepIndicator(
                                            number = "3",
                                            label = "Done",
                                            isActive = false,
                                            accentColor = Color(0xFF00D26A)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Security badge - Premium look
                            Surface(
                                color = WealthTheme.SoftWhite.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, WealthTheme.SoftWhite.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        null,
                                        tint = WealthTheme.Emerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Secured by Safaricom M-Pesa",
                                        color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            // Error message card - Premium design
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = WealthTheme.Amber.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, WealthTheme.Amber.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        null,
                                        tint = WealthTheme.Amber,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = paymentResultMessage,
                                        color = WealthTheme.SoftWhite.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Action buttons - Premium styling
                        if (paymentResultSuccess) {
                            Button(
                                onClick = {
                                    showPaymentResult = false
                                    onPaymentComplete()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00D26A)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 8.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Got it!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showPaymentResult = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    border = BorderStroke(1.dp, WealthTheme.SoftWhite.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Close", color = WealthTheme.SoftWhite, fontWeight = FontWeight.Medium)
                                }

                                Button(
                                    onClick = {
                                        openMpesaApp(context)
                                        showPaymentResult = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00A651)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open M-Pesa", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WealthTheme.Navy,
                                WealthTheme.NavyMid,
                                WealthTheme.Navy
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                WealthTheme.Emerald.copy(alpha = 0.4f),
                                WealthTheme.Emerald.copy(alpha = 0.1f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF00A651).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Payment,
                                    null,
                                    tint = Color(0xFF00A651),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Pay Bill",
                                    color = WealthTheme.SoftWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    bill.title,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = WealthTheme.SoftWhite.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bill Amount Display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF00A651).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Amount to Pay",
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    "KES ${String.format("%,d", roundedAmount)}",
                                    color = Color(0xFF00D26A),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                Icons.Default.Receipt,
                                null,
                                tint = Color(0xFF00A651),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.filter { c -> c.isDigit() }.take(12) },
                        label = { Text("M-Pesa Phone Number", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                        placeholder = { Text("Number making the payment", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, null, tint = Color(0xFF00A651))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A651),
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite,
                            cursorColor = Color(0xFF00A651)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Paybill Number
                    OutlinedTextField(
                        value = paybillNumber,
                        onValueChange = { paybillNumber = it.filter { c -> c.isDigit() }.take(10) },
                        label = { Text("Paybill / Till Number", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                        placeholder = { Text("e.g., 888880", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Business, null, tint = WealthTheme.Gold)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WealthTheme.Emerald,
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite,
                            cursorColor = WealthTheme.Emerald
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Account Number
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, null, tint = WealthTheme.Blue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WealthTheme.Emerald,
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite,
                            cursorColor = WealthTheme.Emerald
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Payment Methods
                    Text(
                        "PAYMENT METHOD",
                        color = WealthTheme.SoftWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // STK Push Button
                    Button(
                        onClick = {
                            if (phoneNumber.length >= 9 && roundedAmount > 0) {
                                isProcessing = true
                                scope.launch {
                                    try {
                                        val result = mpesaRepository.initiateSTKPush(
                                            phoneNumber = phoneNumber,
                                            amount = roundedAmount.toDouble(),
                                            accountReference = accountNumber.ifBlank { bill.title },
                                            transactionDesc = "Bill payment: ${bill.title} via Budgie"
                                        )
                                        isProcessing = false
                                        paymentResultSuccess = result.success
                                        paymentResultMessage = result.message
                                        showPaymentResult = true
                                    } catch (e: Exception) {
                                        isProcessing = false
                                        paymentResultSuccess = false
                                        paymentResultMessage = "Error: ${e.message ?: "Unknown error"}"
                                        showPaymentResult = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isProcessing && phoneNumber.length >= 9,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A651),
                            disabledContainerColor = WealthTheme.SoftWhite.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.SendToMobile, null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (isProcessing) "Sending..." else "STK Push • Pay KES ${String.format("%,d", roundedAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Alternative payment buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openMpesaApp(context)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color(0xFF00A651)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, null, tint = Color(0xFF00A651), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("M-Pesa", color = Color(0xFF00A651), fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                openStkApp(context)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, WealthTheme.Amber),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.SimCard, null, tint = WealthTheme.Amber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STK", color = WealthTheme.Amber, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = WealthTheme.SoftWhite.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// ========== STK PAYMENT DIALOG ==========
/**
 * Universal Bill Payment Dialog
 *
 * Allows users to pay ANY Paybill or Till number directly.
 * Uses multiple payment methods:
 * 1. Opens M-Pesa App directly (most reliable)
 * 2. Falls back to SIM Toolkit
 * 3. STK Push for Budgie's own paybill
 *
 * Common Kenyan Paybills:
 * - Kenya Power: 888880
 * - Nairobi Water: 444444
 * - DSTV: 444900
 * - Zuku: 320320
 */
@Composable
private fun StkPaymentDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPaymentType by remember { mutableStateOf("M-Pesa") }
    var accountNumber by remember { mutableStateOf("") }
    var paybillNumber by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Paybill") }
    var showQuickPaybills by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var paymentResult by remember { mutableStateOf<PaymentResultState?>(null) }

    val scope = rememberCoroutineScope()
    val mpesaRepository = remember { com.example.budgie.mpesa.MpesaRepository.getInstance() }

    val paymentModes = listOf("Paybill", "Buy Goods")

    // Common Kenyan Paybills
    val quickPaybills = listOf(
        QuickPaybill("Kenya Power", "888880", "🔌"),
        QuickPaybill("Nairobi Water", "444444", "💧"),
        QuickPaybill("DSTV", "444900", "📺"),
        QuickPaybill("GOtv", "444900", "📺"),
        QuickPaybill("Zuku", "320320", "📡"),
        QuickPaybill("Showmax", "222000", "🎬"),
        QuickPaybill("Safaricom Postpay", "100200", "📱"),
        QuickPaybill("NHIF", "200222", "🏥"),
        QuickPaybill("KRA", "572572", "📋"),
        QuickPaybill("Naivas", "898989", "🛒"),
    )

    // Premium Futuristic Payment Result Dialog
    paymentResult?.let { result ->
        Dialog(
            onDismissRequest = {
                paymentResult = null
                if (result.success) onDismiss()
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
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
                                    Color(0xFF0D2137),
                                    Color(0xFF0A1A28),
                                    Color(0xFF061218)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = if (result.success) listOf(
                                    Color(0xFF00D26A).copy(alpha = 0.7f),
                                    Color(0xFF0FAE96).copy(alpha = 0.3f),
                                    Color(0xFF00D26A).copy(alpha = 0.1f)
                                ) else listOf(
                                    WealthTheme.Amber.copy(alpha = 0.7f),
                                    WealthTheme.Amber.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Premium Icon with Glow
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer glow
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = if (result.success) listOf(
                                                Color(0xFF00D26A).copy(alpha = 0.35f),
                                                Color(0xFF00D26A).copy(alpha = 0.15f),
                                                Color.Transparent
                                            ) else listOf(
                                                WealthTheme.Amber.copy(alpha = 0.35f),
                                                WealthTheme.Amber.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            // Middle ring
                            Box(
                                modifier = Modifier
                                    .size(85.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (result.success) Color(0xFF00D26A).copy(alpha = 0.12f)
                                        else WealthTheme.Amber.copy(alpha = 0.12f)
                                    )
                                    .border(
                                        2.dp,
                                        if (result.success) Color(0xFF00D26A).copy(alpha = 0.5f)
                                        else WealthTheme.Amber.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            )
                            // Inner icon
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = if (result.success) listOf(
                                                Color(0xFF00D26A),
                                                Color(0xFF0FAE96)
                                            ) else listOf(
                                                WealthTheme.Amber,
                                                Color(0xFFFF9800)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (result.success) Icons.Default.Check else Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Title
                        Text(
                            text = if (result.success) "Payment Initiated" else "Action Required",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.SoftWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (result.success) "STK Push sent to your phone" else "Unable to process",
                            fontSize = 14.sp,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (result.success) {
                            // Premium instruction card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF00D26A).copy(alpha = 0.06f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00D26A).copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF00D26A).copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PhoneAndroid,
                                                null,
                                                tint = Color(0xFF00D26A),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Check Your Phone",
                                                color = Color(0xFF00D26A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                "M-Pesa PIN prompt awaiting",
                                                color = WealthTheme.SoftWhite.copy(alpha = 0.5f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Steps
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StepIndicator("1", "Prompt", true, Color(0xFF00D26A))
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.dp)
                                                .background(
                                                    Color(0xFF00D26A).copy(alpha = 0.3f),
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                        StepIndicator("2", "PIN", false, Color(0xFF00D26A))
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.dp)
                                                .background(
                                                    WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                        StepIndicator("3", "Done", false, Color(0xFF00D26A))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Security badge
                            Surface(
                                color = WealthTheme.SoftWhite.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, WealthTheme.SoftWhite.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        null,
                                        tint = WealthTheme.Emerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Secured by Safaricom M-Pesa",
                                        color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            // Error card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = WealthTheme.Amber.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, WealthTheme.Amber.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        null,
                                        tint = WealthTheme.Amber,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = result.message,
                                        color = WealthTheme.SoftWhite.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Action button
                        Button(
                            onClick = {
                                paymentResult = null
                                if (result.success) onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (result.success) Color(0xFF00D26A) else WealthTheme.Amber
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (result.success) "Got it!" else "OK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = WealthTheme.Navy,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00A651), Color(0xFF4CAF50))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        "M-Pesa Payment",
                        color = WealthTheme.SoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "STK Push - Instant prompt to your phone",
                        color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Payment Mode (Paybill vs Buy Goods)
                Text(
                    "Payment Type",
                    color = WealthTheme.SoftWhite.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentModes.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00A651),
                                selectedLabelColor = Color.White,
                                containerColor = WealthTheme.SoftWhite.copy(alpha = 0.1f),
                                labelColor = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        )
                    }
                }

                // Quick Paybill Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quick Select Popular Bills",
                        color = WealthTheme.SoftWhite.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    TextButton(
                        onClick = { showQuickPaybills = !showQuickPaybills },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        enabled = !isProcessing
                    ) {
                        Text(
                            if (showQuickPaybills) "Hide" else "Show",
                            color = WealthTheme.Emerald,
                            fontSize = 12.sp
                        )
                        Icon(
                            if (showQuickPaybills) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint = WealthTheme.Emerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (showQuickPaybills) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                WealthTheme.SoftWhite.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        items(quickPaybills) { paybill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isProcessing) {
                                        paybillNumber = paybill.number
                                        showQuickPaybills = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(paybill.icon, fontSize = 18.sp)
                                    Text(
                                        paybill.name,
                                        color = WealthTheme.SoftWhite,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    paybill.number,
                                    color = WealthTheme.Emerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(color = WealthTheme.SoftWhite.copy(alpha = 0.1f))
                        }
                    }
                }

                // Phone Number (for STK Push)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { c -> c.isDigit() }.take(12) },
                    label = { Text("Your M-Pesa Phone Number", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                    placeholder = { Text("e.g., 0712345678", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, null, tint = WealthTheme.Emerald)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isProcessing,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WealthTheme.Emerald,
                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                        focusedTextColor = WealthTheme.SoftWhite,
                        unfocusedTextColor = WealthTheme.SoftWhite,
                        cursorColor = WealthTheme.Emerald
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // Paybill/Till Number
                OutlinedTextField(
                    value = paybillNumber,
                    onValueChange = { paybillNumber = it.filter { c -> c.isDigit() }.take(10) },
                    label = {
                        Text(
                            if (paymentMode == "Paybill") "Paybill Number" else "Till Number",
                            color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                        )
                    },
                    placeholder = {
                        Text(
                            if (paymentMode == "Paybill") "e.g., 888880" else "e.g., 123456",
                            color = WealthTheme.SoftWhite.copy(alpha = 0.3f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Business, null, tint = WealthTheme.Emerald)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isProcessing,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WealthTheme.Emerald,
                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                        focusedTextColor = WealthTheme.SoftWhite,
                        unfocusedTextColor = WealthTheme.SoftWhite,
                        cursorColor = WealthTheme.Emerald
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Account Number (for Paybill only)
                if (paymentMode == "Paybill") {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.take(20) },
                        label = { Text("Account Number", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                        placeholder = { Text("e.g., Meter No. / Account No.", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Tag, null, tint = WealthTheme.Emerald)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WealthTheme.Emerald,
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite,
                            cursorColor = WealthTheme.Emerald
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                    prefix = { Text("KES ", color = WealthTheme.Emerald, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, null, tint = WealthTheme.Emerald)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isProcessing,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WealthTheme.Emerald,
                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                        focusedTextColor = WealthTheme.SoftWhite,
                        unfocusedTextColor = WealthTheme.SoftWhite,
                        cursorColor = WealthTheme.Emerald
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Info text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            WealthTheme.Emerald.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = WealthTheme.Emerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "An STK push will be sent to your phone. Just enter your M-Pesa PIN to pay.",
                        color = WealthTheme.SoftWhite.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }

                // Processing indicator
                if (isProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = WealthTheme.Emerald,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Sending STK Push...",
                            color = WealthTheme.Emerald,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessing = true
                    scope.launch {
                        try {
                            val amountDouble = amount.toDoubleOrNull() ?: 0.0
                            if (amountDouble < 1) {
                                paymentResult = PaymentResultState(
                                    success = false,
                                    message = "Minimum amount is KES 1"
                                )
                                isProcessing = false
                                return@launch
                            }

                            if (phoneNumber.length < 9) {
                                paymentResult = PaymentResultState(
                                    success = false,
                                    message = "Please enter a valid phone number"
                                )
                                isProcessing = false
                                return@launch
                            }

                            // Use STK Push API
                            val result = mpesaRepository.initiateSTKPush(
                                phoneNumber = phoneNumber,
                                amount = amountDouble,
                                accountReference = if (paymentMode == "Paybill") accountNumber.ifBlank { paybillNumber } else paybillNumber,
                                transactionDesc = "Payment to $paybillNumber via Budgie"
                            )

                            paymentResult = PaymentResultState(
                                success = result.success,
                                message = result.message
                            )
                        } catch (e: Exception) {
                            paymentResult = PaymentResultState(
                                success = false,
                                message = "Error: ${e.message ?: "Unknown error occurred"}"
                            )
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                enabled = !isProcessing &&
                        phoneNumber.length >= 9 &&
                        paybillNumber.length >= 5 &&
                        amount.isNotBlank() &&
                        (amount.toIntOrNull() ?: 0) >= 1 &&
                        (paymentMode == "Buy Goods" || accountNumber.isNotBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A651),
                    disabledContainerColor = WealthTheme.SoftWhite.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isProcessing) "Sending..." else "Pay Now",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
           Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row with Open M-Pesa and Open STK buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Open M-Pesa App button
                    OutlinedButton(
                        onClick = {
                            openMpesaApp(context)
                            onDismiss()
                        },
                        enabled = !isProcessing,
                        border = BorderStroke(1.dp, Color(0xFF00A651)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            null,
                            tint = Color(0xFF00A651),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("M-Pesa", color = Color(0xFF00A651), fontSize = 11.sp)
                    }

                    // Open STK (SIM Toolkit) button
                    OutlinedButton(
                        onClick = {
                            openStkApp(context)
                            onDismiss()
                        },
                        enabled = !isProcessing,
                        border = BorderStroke(1.dp, WealthTheme.Amber),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.SimCard,
                            null,
                            tint = WealthTheme.Amber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STK", color = WealthTheme.Amber, fontSize = 11.sp)
                    }
                }

                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    enabled = !isProcessing
                ) {
                    Text("Cancel", color = WealthTheme.SoftWhite.copy(alpha = 0.7f))
                }
            }
        }
    )
}

// Quick paybill data class
private data class QuickPaybill(
    val name: String,
    val number: String,
    val icon: String
)

// Payment result state
private data class PaymentResultState(
    val success: Boolean,
    val message: String
)

/**
 * Futuristic Step Indicator for Payment Flow
 */
@Composable
private fun StepIndicator(
    number: String,
    label: String,
    isActive: Boolean,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) accentColor.copy(alpha = 0.2f)
                    else WealthTheme.SoftWhite.copy(alpha = 0.08f)
                )
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) accentColor else WealthTheme.SoftWhite.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = if (isActive) accentColor else WealthTheme.SoftWhite.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            color = if (isActive) accentColor else WealthTheme.SoftWhite.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Opens the M-Pesa app directly for manual payment
 * Searches by package name: com.safaricom.mpesa.lifestyle
 */
private fun openMpesaApp(context: Context) {
    val mpesaPackage = "com.safaricom.mpesa.lifestyle"

    try {
        // Method 1: Try getLaunchIntentForPackage
        val launchIntent = context.packageManager.getLaunchIntentForPackage(mpesaPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Toast.makeText(context, "Opening M-Pesa...", Toast.LENGTH_SHORT).show()
            return
        }
    } catch (e: Exception) {
        android.util.Log.e("BillsScreen", "Method 1 failed: ${e.message}")
    }

    try {
        // Method 2: Try explicit intent with package and action
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(mpesaPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Toast.makeText(context, "Opening M-Pesa...", Toast.LENGTH_SHORT).show()
        return
    } catch (e: Exception) {
        android.util.Log.e("BillsScreen", "Method 2 failed: ${e.message}")
    }

    try {
        // Method 3: Try with component name
        val intent = Intent().apply {
            setClassName(mpesaPackage, "$mpesaPackage.ui.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Toast.makeText(context, "Opening M-Pesa...", Toast.LENGTH_SHORT).show()
        return
    } catch (e: Exception) {
        android.util.Log.e("BillsScreen", "Method 3 failed: ${e.message}")
    }

    // If all methods fail, show error
    Toast.makeText(
        context,
        "Could not open M-Pesa app. Please open it manually.",
        Toast.LENGTH_LONG
    ).show()
}

/**
 * Opens the SIM Toolkit (STK) app for M-Pesa USSD menu access
 * This provides direct access to the carrier's STK menu
 */
private fun openStkApp(context: Context) {
    // Known STK app package names across different Android versions and manufacturers
    val stkPackages = listOf(
        "com.android.stk",              // Standard Android STK
        "com.android.stk2",             // Dual SIM STK (SIM 2)
        "com.mediatek.stk",             // MediaTek devices
        "com.qualcomm.qti.simkit",      // Qualcomm devices
        "com.samsung.android.stk",      // Samsung devices
        "com.huawei.stk",               // Huawei devices
        "com.oppo.stk",                 // Oppo devices
        "com.vivo.stk",                 // Vivo devices
        "com.xiaomi.stk",               // Xiaomi devices
        "com.android.simkit"            // Alternative package
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
            android.util.Log.d("BillsScreen", "STK package $packageName not found")
            continue
        }
    }

    // Try alternative method: open STK via action
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
        android.util.Log.e("BillsScreen", "STK action method failed: ${e.message}")
    }

    // If all methods fail
    Toast.makeText(
        context,
        "SIM Toolkit not found. Please open it from your phone settings.",
        Toast.LENGTH_LONG
    ).show()
}

