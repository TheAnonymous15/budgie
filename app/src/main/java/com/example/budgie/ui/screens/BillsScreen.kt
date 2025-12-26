package com.example.budgie.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val overdueBills by viewModel.overdueBills.collectAsState()
    val totalUnpaid by viewModel.totalUnpaidBills.collectAsState()

    // Combine all bills for export
    val allBills = unpaidBills

    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Export Dialog
    if (showExportDialog && allBills.isNotEmpty()) {
        BillsExportDialog(
            bills = allBills,
            context = context,
            onDismiss = { showExportDialog = false }
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

            if (overdueBills.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WealthTheme.MutedRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Overdue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.MutedRed
                        )
                    }
                }

                items(overdueBills) { bill ->
                    PremiumBillItem(
                        bill = bill,
                        onPaidToggle = { viewModel.markBillAsPaid(bill.id, it) },
                        isOverdue = true
                    )
                }
            }

            item {
                Text(
                    text = "Upcoming Bills",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite
                )
            }

            items(unpaidBills.filter { it !in overdueBills }) { bill ->
                PremiumBillItem(
                    bill = bill,
                    onPaidToggle = { viewModel.markBillAsPaid(bill.id, it) },
                    isOverdue = false
                )
            }

            if (unpaidBills.isEmpty()) {
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

@Composable
fun PremiumBillItem(
    bill: Bill,
    onPaidToggle: (Boolean) -> Unit,
    isOverdue: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isOverdue) WealthTheme.MutedRed.copy(alpha = 0.3f) else WealthTheme.SoftWhite.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = WealthTheme.SoftWhite
                )
                Text(
                    text = "Due: ${formatDate(bill.dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) WealthTheme.MutedRed else WealthTheme.SoftWhite.copy(alpha = 0.5f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatCurrency(bill.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverdue) WealthTheme.MutedRed else WealthTheme.SoftWhite
                )
                Checkbox(
                    checked = bill.isPaid,
                    onCheckedChange = onPaidToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = WealthTheme.Emerald,
                        uncheckedColor = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

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
    // Bill entries - start with 5 empty rows
    var billEntries by remember {
        mutableStateOf(
            (1..5).map { BillEntry(id = it) }
        )
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
                                "Add Multiple Bills",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Track all your bills at once. New rows appear automatically!",
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .billGlassmorphicCard(
                                if (isValid) WealthTheme.Emerald
                                else WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                14
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Row number and remove button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isValid) WealthTheme.Emerald
                                                else WealthTheme.SoftWhite.copy(alpha = 0.2f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isValid) Color.White else WealthTheme.SoftWhite
                                        )
                                    }
                                }

                                if (billEntries.size > 1 && !isUsed) {
                                    IconButton(
                                        onClick = {
                                            billEntries = billEntries.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = WealthTheme.SoftWhite.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Title and Amount row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = entry.title,
                                    onValueChange = { newTitle ->
                                        billEntries = billEntries.toMutableList().also {
                                            it[index] = entry.copy(title = newTitle)
                                        }
                                    },
                                    label = { Text("Bill Title", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WealthTheme.Emerald,
                                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                        focusedTextColor = WealthTheme.SoftWhite,
                                        unfocusedTextColor = WealthTheme.SoftWhite,
                                        cursorColor = WealthTheme.Emerald
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )

                                OutlinedTextField(
                                    value = entry.amount,
                                    onValueChange = { newAmount ->
                                        billEntries = billEntries.toMutableList().also {
                                            it[index] = entry.copy(amount = newAmount.filter { c -> c.isDigit() || c == '.' })
                                        }
                                    },
                                    label = { Text("Amount", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                    prefix = { Text("$", color = WealthTheme.Emerald) },
                                    modifier = Modifier.weight(0.6f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WealthTheme.Emerald,
                                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                        focusedTextColor = WealthTheme.SoftWhite,
                                        unfocusedTextColor = WealthTheme.SoftWhite,
                                        cursorColor = WealthTheme.Emerald
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Due date and category row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Due date
                                OutlinedTextField(
                                    value = formatDate(entry.dueDate),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Due", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showDatePickerFor = index },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarToday, "Date", tint = WealthTheme.Emerald, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    modifier = Modifier.weight(0.5f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WealthTheme.Emerald,
                                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                        focusedTextColor = WealthTheme.SoftWhite,
                                        unfocusedTextColor = WealthTheme.SoftWhite
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )

                                // Category dropdown
                                ExposedDropdownMenuBox(
                                    expanded = entry.categoryExpanded,
                                    onExpandedChange = { expanded ->
                                        billEntries = billEntries.toMutableList().also {
                                            it[index] = entry.copy(categoryExpanded = expanded)
                                        }
                                    },
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    OutlinedTextField(
                                        value = entry.category.icon,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Cat", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entry.categoryExpanded) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WealthTheme.Emerald,
                                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                            focusedTextColor = WealthTheme.SoftWhite,
                                            unfocusedTextColor = WealthTheme.SoftWhite
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = MaterialTheme.typography.bodySmall
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
                            }

                            // Recurring toggle
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = entry.isRecurring,
                                    onCheckedChange = { checked ->
                                        billEntries = billEntries.toMutableList().also {
                                            it[index] = entry.copy(isRecurring = checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = WealthTheme.Emerald,
                                        uncheckedColor = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Recurring",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                                )
                            }
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

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WealthTheme.Navy)
                    .padding(20.dp)
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
                        .height(56.dp),
                    enabled = validEntries.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WealthTheme.Emerald,
                        disabledContainerColor = WealthTheme.SoftWhite.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
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

