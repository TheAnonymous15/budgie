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
import androidx.compose.ui.window.Dialog
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: MainViewModel,
    onAddExpense: () -> Unit,
    onBack: () -> Unit
) {
    val expenses by viewModel.currentMonthExpenses.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Export Dialog
    if (showExportDialog && expenses.isNotEmpty()) {
        ExpensesExportDialog(
            expenses = expenses,
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
                        "Expenses",
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
                if (expenses.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showExportDialog = true },
                        containerColor = WealthTheme.Emerald,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.FileUpload, "Export", modifier = Modifier.size(22.dp))
                    }
                }
                // Add Expense FAB
                FloatingActionButton(
                    onClick = onAddExpense,
                    containerColor = WealthTheme.MutedRed,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Add Expense")
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            WealthTheme.MutedRed.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Total Expenses This Month",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (summary.totalExpenses > 0) formatCurrency(summary.totalExpenses) else "No expenses yet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.MutedRed
                        )
                    }
                }
            }

            // Category breakdown
            if (summary.expensesByCategory.isNotEmpty()) {
                item {
                    Text(
                        text = "By Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthTheme.SoftWhite
                    )
                }

                items(summary.expensesByCategory.entries.sortedByDescending { it.value }.toList()) { (category, amount) ->
                    PremiumCategoryExpenseRow(category = category, amount = amount, total = summary.totalExpenses)
                }
            }

            item {
                HorizontalDivider(color = WealthTheme.SoftWhite.copy(alpha = 0.1f))
                Text(
                    text = "All Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(expenses) { expense ->
                PremiumExpenseItem(
                    expense = expense,
                    onDelete = { viewModel.deleteExpense(expense) }
                )
            }

            if (expenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = WealthTheme.SoftWhite.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No expenses this month",
                                style = MaterialTheme.typography.titleMedium,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Track your spending to get insights",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
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
fun PremiumCategoryExpenseRow(
    category: ExpenseCategory,
    amount: Double,
    total: Double
) {
    val percentage = if (total > 0) (amount / total) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.03f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = category.icon, modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = category.displayName,
                    color = WealthTheme.SoftWhite
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(amount),
                    fontWeight = FontWeight.Medium,
                    color = WealthTheme.SoftWhite
                )
                Text(
                    text = "${String.format("%.1f", percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun PremiumExpenseItem(
    expense: Expense,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(expense.category.icon)
                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = WealthTheme.SoftWhite
                    )
                    Text(
                        text = expense.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.MutedRed
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = WealthTheme.SoftWhite.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryExpenseRow(
    category: ExpenseCategory,
    amount: Double,
    total: Double
) {
    PremiumCategoryExpenseRow(category, amount, total)
}

// Data class for expense entry
private data class ExpenseEntry(
    val id: Int,
    val title: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val notes: String = "",
    val categoryExpanded: Boolean = false
)

// Glassmorphism for expense screen
private fun Modifier.expenseGlassmorphicCard(accentColor: Color, cornerRadius: Int = 14) = this
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
fun AddExpenseScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    // Expense entries - start with 1 empty row, auto-adds more
    var expenseEntries by remember {
        mutableStateOf(listOf(ExpenseEntry(id = 1)))
    }


    // Auto-add new row when last row is filled
    LaunchedEffect(expenseEntries) {
        val lastEntry = expenseEntries.lastOrNull()
        if (lastEntry != null && lastEntry.title.isNotBlank() && lastEntry.amount.isNotBlank()) {
            val newId = (expenseEntries.maxOfOrNull { it.id } ?: 0) + 1
            expenseEntries = expenseEntries + ExpenseEntry(id = newId)
        }
    }

    // Count valid entries
    val validEntries = expenseEntries.filter {
        it.title.isNotBlank() && it.amount.isNotBlank() && (it.amount.toDoubleOrNull() ?: 0.0) > 0
    }

    // Calculate total
    val totalAmount = validEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF0D2137),
                        Color(0xFF0A1628)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Premium Top Bar with glassmorphism
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WealthTheme.Emerald.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button with glow
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(8.dp, CircleShape, ambientColor = WealthTheme.Emerald.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                        WealthTheme.SoftWhite.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(1.dp, WealthTheme.SoftWhite.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.Close,
                                "Close",
                                tint = WealthTheme.SoftWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Title with subtle animation effect
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Add Expense",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.SoftWhite,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "Track your spending",
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                        )
                    }

                    // Count badge with glow effect
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(
                                if (validEntries.isNotEmpty()) 12.dp else 0.dp,
                                CircleShape,
                                ambientColor = WealthTheme.Emerald.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (validEntries.isNotEmpty())
                                    Brush.radialGradient(
                                        colors = listOf(WealthTheme.Emerald, WealthTheme.Emerald.copy(alpha = 0.7f))
                                    )
                                else
                                    Brush.radialGradient(
                                        colors = listOf(
                                            WealthTheme.SoftWhite.copy(alpha = 0.1f),
                                            WealthTheme.SoftWhite.copy(alpha = 0.05f)
                                        )
                                    )
                            )
                            .border(
                                1.dp,
                                if (validEntries.isNotEmpty()) WealthTheme.Emerald.copy(alpha = 0.5f)
                                else WealthTheme.SoftWhite.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${validEntries.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Card - Premium glassmorphism
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = WealthTheme.Emerald.copy(alpha = 0.2f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        WealthTheme.Emerald.copy(alpha = 0.2f),
                                        WealthTheme.Emerald.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.03f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Icon with glow
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(12.dp, CircleShape, ambientColor = WealthTheme.Emerald.copy(alpha = 0.4f))
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                WealthTheme.Emerald.copy(alpha = 0.3f),
                                                WealthTheme.Emerald.copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                                    .border(1.dp, WealthTheme.Emerald.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = WealthTheme.Emerald,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Total Expense",
                                style = MaterialTheme.typography.labelLarge,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Amount with premium styling
                            Text(
                                formatCurrency(totalAmount),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (totalAmount > 0) WealthTheme.Emerald else WealthTheme.SoftWhite.copy(alpha = 0.4f),
                                letterSpacing = 1.sp
                            )

                            if (validEntries.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Stats row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${validEntries.size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = WealthTheme.SoftWhite
                                        )
                                        Text(
                                            "Items",
                                            fontSize = 11.sp,
                                            color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(32.dp)
                                            .background(WealthTheme.SoftWhite.copy(alpha = 0.1f))
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${validEntries.map { it.category }.distinct().size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = WealthTheme.SoftWhite
                                        )
                                        Text(
                                            "Categories",
                                            fontSize = 11.sp,
                                            color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WealthTheme.Emerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = WealthTheme.Emerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                "Expense Entries",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )
                        }

                        // Valid count chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WealthTheme.Emerald.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${validEntries.size} ready",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = WealthTheme.Emerald
                            )
                        }
                    }
                }

                // Expense Entry Rows
                items(expenseEntries.size) { index ->
                    val entry = expenseEntries[index]
                    val isUsed = entry.title.isNotBlank() || entry.amount.isNotBlank()
                    val isValid = entry.title.isNotBlank() && entry.amount.isNotBlank() && (entry.amount.toDoubleOrNull() ?: 0.0) > 0

                    // Premium Glassmorphic Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                if (isValid) 12.dp else 4.dp,
                                RoundedCornerShape(20.dp),
                                ambientColor = if (isValid) WealthTheme.Emerald.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (isValid) listOf(
                                        WealthTheme.Emerald.copy(alpha = 0.15f),
                                        WealthTheme.Emerald.copy(alpha = 0.05f),
                                        Color.White.copy(alpha = 0.02f)
                                    ) else listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.04f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        if (isValid) WealthTheme.Emerald.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                                        if (isValid) WealthTheme.Emerald.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Header row with number badge, status and remove button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Premium number badge with glow
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .shadow(
                                                if (isValid) 8.dp else 0.dp,
                                                CircleShape,
                                                ambientColor = WealthTheme.Emerald.copy(alpha = 0.4f)
                                            )
                                            .clip(CircleShape)
                                            .background(
                                                if (isValid)
                                                    Brush.radialGradient(
                                                        colors = listOf(WealthTheme.Emerald, WealthTheme.Emerald.copy(alpha = 0.8f))
                                                    )
                                                else
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                                            WealthTheme.SoftWhite.copy(alpha = 0.08f)
                                                        )
                                                    )
                                            )
                                            .border(
                                                1.dp,
                                                if (isValid) WealthTheme.Emerald.copy(alpha = 0.5f)
                                                else WealthTheme.SoftWhite.copy(alpha = 0.1f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isValid) Color.White else WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Status indicator
                                    Column {
                                        Text(
                                            if (isValid) "Ready to save" else "Enter details",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isValid) WealthTheme.Emerald else WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                        )
                                        if (isValid) {
                                            Text(
                                                formatCurrency(entry.amount.toDoubleOrNull() ?: 0.0),
                                                fontSize = 11.sp,
                                                color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                                // Delete button
                                if (expenseEntries.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(WealthTheme.MutedRed.copy(alpha = 0.1f))
                                            .border(1.dp, WealthTheme.MutedRed.copy(alpha = 0.2f), CircleShape)
                                            .clickable {
                                                expenseEntries = expenseEntries.toMutableList().also {
                                                    it.removeAt(index)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = WealthTheme.MutedRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Expense Title - Premium input
                            OutlinedTextField(
                                value = entry.title,
                                onValueChange = { newTitle ->
                                    expenseEntries = expenseEntries.toMutableList().also {
                                        it[index] = entry.copy(title = newTitle)
                                    }
                                },
                                label = { Text("Expense Title", color = WealthTheme.SoftWhite.copy(alpha = 0.5f)) },
                                placeholder = { Text("e.g., Groceries, Electricity", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WealthTheme.Emerald.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Receipt,
                                            null,
                                            tint = WealthTheme.Emerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WealthTheme.Emerald,
                                    unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                    focusedTextColor = WealthTheme.SoftWhite,
                                    unfocusedTextColor = WealthTheme.SoftWhite,
                                    cursorColor = WealthTheme.Emerald,
                                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Category dropdown - Premium style
                            ExposedDropdownMenuBox(
                                expanded = entry.categoryExpanded,
                                onExpandedChange = { expanded ->
                                    expenseEntries = expenseEntries.toMutableList().also {
                                        it[index] = entry.copy(categoryExpanded = expanded)
                                    }
                                }
                            ) {
                                OutlinedTextField(
                                    value = "${entry.category.icon} ${entry.category.displayName}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category", color = WealthTheme.SoftWhite.copy(alpha = 0.5f)) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(WealthTheme.Gold.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Category,
                                                null,
                                                tint = WealthTheme.Gold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entry.categoryExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WealthTheme.Emerald,
                                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                        focusedTextColor = WealthTheme.SoftWhite,
                                        unfocusedTextColor = WealthTheme.SoftWhite,
                                        focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = entry.categoryExpanded,
                                    onDismissRequest = {
                                        expenseEntries = expenseEntries.toMutableList().also {
                                            it[index] = entry.copy(categoryExpanded = false)
                                        }
                                    }
                                ) {
                                    ExpenseCategory.entries.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text("${category.icon} ${category.displayName}") },
                                            onClick = {
                                                expenseEntries = expenseEntries.toMutableList().also {
                                                    it[index] = entry.copy(category = category, categoryExpanded = false)
                                                }
                                            }
                                        )
                                    }
                                }
                            }


                            Spacer(modifier = Modifier.height(14.dp))

                            // Amount field - Premium Hero Style
                            OutlinedTextField(
                                value = entry.amount,
                                onValueChange = { newAmount ->
                                    expenseEntries = expenseEntries.toMutableList().also {
                                        it[index] = entry.copy(amount = newAmount.filter { c -> c.isDigit() || c == '.' })
                                    }
                                },
                                label = { Text("Amount", color = WealthTheme.SoftWhite.copy(alpha = 0.5f)) },
                                placeholder = { Text("0.00", color = WealthTheme.SoftWhite.copy(alpha = 0.3f)) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .shadow(4.dp, CircleShape, ambientColor = WealthTheme.Emerald.copy(alpha = 0.3f))
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        WealthTheme.Emerald.copy(alpha = 0.2f),
                                                        WealthTheme.Emerald.copy(alpha = 0.1f)
                                                    )
                                                )
                                            )
                                            .border(1.dp, WealthTheme.Emerald.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$",
                                            color = WealthTheme.Emerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WealthTheme.Emerald,
                                    unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.15f),
                                    focusedTextColor = WealthTheme.SoftWhite,
                                    unfocusedTextColor = WealthTheme.SoftWhite,
                                    cursorColor = WealthTheme.Emerald,
                                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }

                // Premium Total Summary Card
                if (validEntries.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = WealthTheme.Gold.copy(alpha = 0.3f))
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            WealthTheme.Gold.copy(alpha = 0.2f),
                                            WealthTheme.Amber.copy(alpha = 0.15f),
                                            WealthTheme.Gold.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    Brush.horizontalGradient(
                                        listOf(WealthTheme.Gold.copy(alpha = 0.5f), WealthTheme.Amber.copy(alpha = 0.3f))
                                    ),
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(WealthTheme.Gold.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Savings,
                                            null,
                                            tint = WealthTheme.Gold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Total",
                                            fontSize = 12.sp,
                                            color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            "${validEntries.size} item${if (validEntries.size > 1) "s" else ""}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = WealthTheme.SoftWhite
                                        )
                                    }
                                }
                                Text(
                                    formatCurrency(totalAmount),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthTheme.Gold
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }

            // Premium Save Button - moved up to avoid phone navigation buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0A1628).copy(alpha = 0.95f),
                                Color(0xFF0A1628)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 32.dp) // Extra bottom padding
            ) {
                Button(
                    onClick = {
                        // Save all valid expenses
                        validEntries.forEach { entry ->
                            viewModel.addExpense(
                                Expense(
                                    title = entry.title,
                                    amount = entry.amount.toDoubleOrNull() ?: 0.0,
                                    category = entry.category,
                                    notes = entry.notes
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
                        if (validEntries.isEmpty()) "Add at least one expense"
                        else "Save ${validEntries.size} Expense${if (validEntries.size > 1) "s" else ""} • ${formatCurrency(totalAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityCalculatorDialog(
    utilityName: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onCalculate: (calculatedAmount: Double, currentReading: Double, costPerUnit: Double) -> Unit
) {
    var currentReading by remember { mutableStateOf("") }
    var previousReading by remember { mutableStateOf("") }
    var costPerUnit by remember { mutableStateOf("") }
    var isLoadingPrevious by remember { mutableStateOf(true) }
    var lastReadingDate by remember { mutableStateOf<String?>(null) }

    // Load previous reading from database
    LaunchedEffect(utilityName) {
        val lastReading = viewModel.getLastUtilityReading(utilityName)
        if (lastReading != null) {
            previousReading = String.format("%.2f", lastReading.reading)
            costPerUnit = String.format("%.2f", lastReading.costPerUnit)
            // Format the date
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            lastReadingDate = dateFormat.format(java.util.Date(lastReading.timestamp))
        }
        isLoadingPrevious = false
    }

    val currentValue = currentReading.toDoubleOrNull() ?: 0.0
    val previousValue = previousReading.toDoubleOrNull() ?: 0.0
    val unitCost = costPerUnit.toDoubleOrNull() ?: 0.0

    val unitsUsed = if (currentValue >= previousValue) currentValue - previousValue else 0.0
    val totalBill = unitsUsed * unitCost

    val isValid = currentReading.isNotBlank() &&
                  previousReading.isNotBlank() &&
                  costPerUnit.isNotBlank() &&
                  currentValue >= previousValue &&
                  unitCost > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$utilityName Calculator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter meter readings and cost per unit to calculate your bill",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = currentReading,
                    onValueChange = { currentReading = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Current Reading") },
                    placeholder = { Text("This month's reading") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null)
                    }
                )

                OutlinedTextField(
                    value = previousReading,
                    onValueChange = { previousReading = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Previous Reading") },
                    placeholder = { Text("Last month's reading") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.History, contentDescription = null)
                    },
                    isError = currentReading.isNotBlank() && previousReading.isNotBlank() && currentValue < previousValue,
                    supportingText = {
                        when {
                            isLoadingPrevious -> Text("Loading previous reading...")
                            lastReadingDate != null && previousReading.isNotBlank() -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Auto-filled from $lastReadingDate",
                                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    )
                                }
                            }
                            currentReading.isNotBlank() && previousReading.isNotBlank() && currentValue < previousValue -> {
                                Text("Current reading must be greater than previous")
                            }
                            else -> null
                        }
                    }
                )

                OutlinedTextField(
                    value = costPerUnit,
                    onValueChange = { costPerUnit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Cost per Unit") },
                    placeholder = { Text("Price per unit") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    supportingText = if (lastReadingDate != null && costPerUnit.isNotBlank()) {
                        {
                            Text(
                                "Using last saved rate",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else null
                )

                // Calculation Result Card
                if (currentReading.isNotBlank() && previousReading.isNotBlank() && costPerUnit.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isValid)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Units Used:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format("%.2f", unitsUsed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Calculation:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${String.format("%.2f", unitsUsed)} × $${String.format("%.2f", unitCost)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Bill:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$${String.format("%.2f", totalBill)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValid)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Info about saving
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current reading will be saved for next month's calculation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCalculate(totalBill, currentValue, unitCost) },
                enabled = isValid
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use This Amount")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    viewModel: MainViewModel,
    onAddIncome: () -> Unit,
    onBack: () -> Unit
) {
    val incomes by viewModel.incomes.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()

    // Dialog state for adding income
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WealthTheme.Navy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Income",
                        color = WealthTheme.SoftWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WealthTheme.SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WealthTheme.Navy)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddIncomeDialog = true },
                containerColor = WealthTheme.Emerald
            ) {
                Icon(Icons.Default.Add, "Add Income", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = WealthTheme.Emerald.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Total Income This Month",
                            style = MaterialTheme.typography.titleMedium,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatCurrency(summary.totalIncome),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.Emerald
                        )
                    }
                }
            }

            item {
                Text(
                    text = "All Income",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite
                )
            }

            items(incomes) { income ->
                IncomeItem(
                    income = income,
                    onClick = { },
                    onDelete = { viewModel.deleteIncome(income) }
                )
            }

            if (incomes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = WealthTheme.SoftWhite.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No income recorded",
                                style = MaterialTheme.typography.bodyLarge,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showAddIncomeDialog = true }) {
                                Text("Add your first income", color = WealthTheme.Emerald)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Income Dialog
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            viewModel = viewModel,
            onDismiss = { showAddIncomeDialog = false }
        )
    }
}

// Glassmorphic Add Income Dialog for IncomeScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(IncomeSource.SALARY) }
    var isRecurring by remember { mutableStateOf(true) }
    var sourceExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
                    ),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WealthTheme.Navy.copy(alpha = 0.98f),
                                Color(0xFF0D2E3D).copy(alpha = 0.95f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(WealthTheme.Emerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = WealthTheme.Emerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Add Income",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthTheme.SoftWhite
                                )
                                Text(
                                    "Record your earnings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(WealthTheme.SoftWhite.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = WealthTheme.SoftWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Income Title", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                        placeholder = { Text("e.g., Monthly Salary", color = WealthTheme.SoftWhite.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WealthTheme.Emerald,
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite,
                            cursorColor = WealthTheme.Emerald
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                        placeholder = { Text("0.00", color = WealthTheme.SoftWhite.copy(alpha = 0.4f)) },
                        prefix = {
                            Text(
                                "$",
                                color = WealthTheme.Emerald,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WealthTheme.Emerald,
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite,
                            cursorColor = WealthTheme.Emerald
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Source Dropdown
                    ExposedDropdownMenuBox(
                        expanded = sourceExpanded,
                        onExpandedChange = { sourceExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${selectedSource.icon} ${selectedSource.displayName}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Income Source", color = WealthTheme.SoftWhite.copy(alpha = 0.6f)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WealthTheme.Emerald,
                                unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                focusedTextColor = WealthTheme.SoftWhite,
                                unfocusedTextColor = WealthTheme.SoftWhite
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false },
                            modifier = Modifier.background(Color(0xFF0D2E3D))
                        ) {
                            IncomeSource.entries.forEach { source ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${source.icon} ${source.displayName}",
                                            color = WealthTheme.SoftWhite
                                        )
                                    },
                                    onClick = {
                                        selectedSource = source
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Recurring Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WealthTheme.SoftWhite.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                WealthTheme.SoftWhite.copy(alpha = 0.1f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (isRecurring) WealthTheme.Emerald else WealthTheme.SoftWhite.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Recurring Income",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = WealthTheme.SoftWhite
                                    )
                                    Text(
                                        "Income repeats monthly",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = WealthTheme.Emerald,
                                    uncheckedThumbColor = WealthTheme.SoftWhite,
                                    uncheckedTrackColor = WealthTheme.SoftWhite.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WealthTheme.SoftWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                WealthTheme.SoftWhite.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val incomeAmount = amount.toDoubleOrNull() ?: 0.0
                                if (title.isNotBlank() && incomeAmount > 0) {
                                    viewModel.addIncome(
                                        Income(
                                            title = title,
                                            amount = incomeAmount,
                                            source = selectedSource,
                                            isRecurring = isRecurring
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WealthTheme.Emerald,
                                disabledContainerColor = WealthTheme.SoftWhite.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Income", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(IncomeSource.SALARY) }
    var isRecurring by remember { mutableStateOf(true) }
    var sourceExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Income") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val incomeAmount = amount.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && incomeAmount > 0) {
                                viewModel.addIncome(
                                    Income(
                                        title = title,
                                        amount = incomeAmount,
                                        source = selectedSource,
                                        isRecurring = isRecurring
                                    )
                                )
                                onBack()
                            }
                        },
                        enabled = title.isNotBlank() && amount.toDoubleOrNull() != null
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = sourceExpanded,
                onExpandedChange = { sourceExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${selectedSource.icon} ${selectedSource.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Source") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = sourceExpanded,
                    onDismissRequest = { sourceExpanded = false }
                ) {
                    IncomeSource.entries.forEach { source ->
                        DropdownMenuItem(
                            text = { Text("${source.icon} ${source.displayName}") },
                            onClick = {
                                selectedSource = source
                                sourceExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring Income")
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
            }
        }
    }
}

// Expenses Export Dialog
@Composable
private fun ExpensesExportDialog(
    expenses: List<Expense>,
    context: Context,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("PDF") }
    var isExporting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    if (showSuccessDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false; onDismiss() },
            containerColor = WealthTheme.NavyMid,
            icon = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(56.dp).background(WealthTheme.Emerald.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = WealthTheme.Emerald, modifier = Modifier.size(32.dp))
                    }
                }
            },
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Export Successful!", color = WealthTheme.SoftWhite, fontWeight = FontWeight.Bold) } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your expenses report has been exported.", color = WealthTheme.SoftWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportedFile?.name ?: "", color = WealthTheme.Emerald, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { exportedFile?.let { shareExpensesFile(context, it, if (selectedFormat == "PDF") "application/pdf" else "text/csv") }; showSuccessDialog = false; onDismiss() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthTheme.Emerald), border = androidx.compose.foundation.BorderStroke(1.dp, WealthTheme.Emerald)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Share")
                        }
                        Button(onClick = { exportedFile?.let { openExpensesFile(context, it) }; showSuccessDialog = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = WealthTheme.Emerald)) {
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
        containerColor = WealthTheme.NavyMid,
        title = { Text("Export Expenses", color = WealthTheme.SoftWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose export format:", color = WealthTheme.SoftWhite.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("PDF" to Color(0xFF5C9CE5), "Excel" to WealthTheme.Emerald).forEach { (format, color) ->
                        FilterChip(selected = selectedFormat == format, onClick = { selectedFormat = format },
                            label = { Text(format) }, colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f), selectedLabelColor = color,
                                containerColor = Color.Transparent, labelColor = WealthTheme.SoftWhite))
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = WealthTheme.Navy.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = WealthTheme.Emerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Info", color = WealthTheme.SoftWhite, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• ${expenses.size} expense records", color = WealthTheme.SoftWhite.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                        Text("• Total: $${String.format("%,.2f", expenses.sumOf { it.amount })}", color = WealthTheme.SoftWhite.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                isExporting = true
                val file = exportExpensesData(context, expenses, selectedFormat)
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

private fun exportExpensesData(context: Context, expenses: List<Expense>, format: String): File? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    return try {
        when (format) {
            "Excel" -> {
                val file = File(context.getExternalFilesDir(null), "Expenses_Report_$timestamp.csv")
                FileWriter(file).use { writer ->
                    writer.append("Expenses Report\nGenerated,${dateFormat.format(Date())}\n\n")
                    writer.append("Title,Amount,Date,Category\n")
                    expenses.forEach { expense ->
                        writer.append("${expense.title},$${String.format("%.2f", expense.amount)},${dateFormat.format(Date(expense.date))},${expense.category.displayName}\n")
                    }
                    writer.append("\nTOTAL,$${String.format("%.2f", expenses.sumOf { it.amount })}\n")
                }
                file
            }
            "PDF" -> {
                val file = File(context.getExternalFilesDir(null), "Expenses_Report_$timestamp.pdf")
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
                headerCell.add(Paragraph("EXPENSES REPORT").setFontSize(16f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.CENTER).setMarginTop(-5f))
                headerCell.add(Paragraph("Your Personal Finance Companion").setFontSize(10f).setFontColor(DeviceRgb(150, 180, 175)).setTextAlignment(PdfTextAlignment.CENTER).setItalic().setMarginTop(8f))
                headerTable.addCell(headerCell)
                document.add(headerTable)

                // Info bar
                val totalExpenses = expenses.sumOf { it.amount }
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Total Expenses: ${expenses.size}").setFontSize(11f).setFontColor(softWhite)))
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Generated: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}").setFontSize(11f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.RIGHT)))
                document.add(infoTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Summary
                document.add(Paragraph("SUMMARY").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
                listOf("Total Records", "Total Amount").forEach { header ->
                    summaryTable.addCell(Cell().add(Paragraph(header).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                summaryTable.addCell(Cell().add(Paragraph(expenses.size.toString()).setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                summaryTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", totalExpenses)}").setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(redColor))
                document.add(summaryTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Expenses table
                document.add(Paragraph("EXPENSE DETAILS").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val expensesTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1.2f, 1.5f))).useAllAvailableWidth()
                listOf("Title", "Amount", "Date", "Category").forEach { header ->
                    expensesTable.addCell(Cell().add(Paragraph(header).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                expenses.forEachIndexed { index, expense ->
                    val bgColor = if (index % 2 == 0) whiteColor else lightGray
                    expensesTable.addCell(Cell().add(Paragraph(expense.title).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f))
                    expensesTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", expense.amount)}").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.RIGHT).setFontColor(redColor))
                    expensesTable.addCell(Cell().add(Paragraph(dateFormat.format(Date(expense.date))).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                    expensesTable.addCell(Cell().add(Paragraph(expense.category.displayName).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                document.add(expensesTable)

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

private fun shareExpensesFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Expenses Report"))
}

private fun openExpensesFile(context: Context, file: File) {
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

