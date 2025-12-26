package com.example.budgie.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.text.KeyboardOptions
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

// Glassmorphism Colors
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassHighlight = Color.White.copy(alpha = 0.15f)

// Glassmorphism Card Modifier
private fun Modifier.glassmorphicCard(cornerRadius: Int = 20) = this
    .shadow(8.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(GlassHighlight, GlassWhite, Color.White.copy(alpha = 0.05f))
        )
    )
    .border(1.dp, Brush.verticalGradient(listOf(GlassBorder, Color.White.copy(alpha = 0.05f))), RoundedCornerShape(cornerRadius.dp))

// Accent Glassmorphism Card Modifier
private fun Modifier.glassmorphicAccentCard(accentColor: Color, cornerRadius: Int = 16) = this
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
fun BudgetScreen(
    viewModel: MainViewModel,
    onAddBudget: () -> Unit,
    onBack: () -> Unit
) {
    val budgets by viewModel.currentBudgets.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Export Dialog
    if (showExportDialog && budgets.isNotEmpty()) {
        BudgetExportDialog(
            budgets = budgets,
            monthName = monthNames.getOrElse(selectedMonth) { "Unknown" },
            year = selectedYear,
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
                        "Budget",
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
                if (budgets.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showExportDialog = true },
                        containerColor = WealthTheme.Blue,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.FileUpload, "Export", modifier = Modifier.size(22.dp))
                    }
                }
                // Add Budget FAB
                FloatingActionButton(
                    onClick = onAddBudget,
                    containerColor = WealthTheme.Emerald,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Add Budget")
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
                Text(
                    text = "${monthNames[selectedMonth - 1]} $selectedYear",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite
                )
            }

            item {
                val totalBudget = budgets.sumOf { it.limit }
                val totalSpent = summary.totalExpenses
                val remaining = totalBudget - totalSpent
                val hasData = totalBudget > 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphicAccentCard(
                            if (remaining >= 0 && hasData) WealthTheme.Emerald else WealthTheme.MutedRed,
                            18
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header with icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(WealthTheme.Emerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = WealthTheme.Emerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (hasData) formatCurrency(totalBudget) else "Not set",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthTheme.SoftWhite
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (hasData) formatCurrency(remaining) else "—",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining >= 0) WealthTheme.Emerald else WealthTheme.MutedRed
                                )
                            }
                        }

                        if (hasData) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Progress bar
                            val progress = if (totalBudget > 0) (totalSpent / totalBudget).coerceIn(0.0, 1.0) else 0.0

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(WealthTheme.SoftWhite.copy(alpha = 0.1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.toFloat())
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = if (remaining < 0)
                                                    listOf(WealthTheme.MutedRed, WealthTheme.Amber)
                                                else
                                                    listOf(WealthTheme.Emerald, WealthTheme.Teal)
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent: ${formatCurrency(totalSpent)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}% used",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (remaining >= 0) WealthTheme.Emerald else WealthTheme.MutedRed
                                )
                            }
                        }
                    }
                }
            }

            if (budgets.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Budgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WealthTheme.SoftWhite
                    )
                }

                items(budgets) { budget ->
                    val spent = summary.expensesByCategory[budget.category] ?: 0.0
                    PremiumCategoryBudgetProgress(
                        category = budget.category,
                        spent = spent,
                        budget = budget.limit
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicCard(18)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(WealthTheme.Emerald.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = WealthTheme.Emerald.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No budgets set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Create a budget to track your spending\nand reach your financial goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onAddBudget,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WealthTheme.Emerald
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Budget", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PremiumCategoryBudgetProgress(
    category: ExpenseCategory,
    spent: Double,
    budget: Double
) {
    val progress = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0) else 0.0
    val isOverBudget = spent > budget
    val accentColor = if (isOverBudget) WealthTheme.MutedRed else WealthTheme.Emerald

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(accentColor, 14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            category.icon,
                            fontSize = 18.sp
                        )
                    }
                    Column {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.SoftWhite
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% used",
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(spent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "of ${formatCurrency(budget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(WealthTheme.SoftWhite.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isOverBudget)
                                    listOf(WealthTheme.MutedRed, WealthTheme.MutedRed.copy(alpha = 0.7f))
                                else
                                    listOf(WealthTheme.Emerald, WealthTheme.Teal)
                            )
                        )
                )
            }

            // Warning message if over budget
            if (isOverBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = WealthTheme.MutedRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Over budget by ${formatCurrency(spent - budget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthTheme.MutedRed
                    )
                }
            }
        }
    }
}

// Budget Entry data class for multiple budget creation
data class BudgetEntry(
    val id: Int,
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val amount: String = "",
    val categoryExpanded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val existingBudgets by viewModel.currentBudgets.collectAsState()

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Budget entries - start with 5 empty rows
    var budgetEntries by remember {
        mutableStateOf(
            (1..5).map { BudgetEntry(id = it) }
        )
    }

    // Get categories that already have budgets
    val categoriesWithBudgets = existingBudgets.map { it.category }.toSet()

    // Auto-add new row when last row is filled
    LaunchedEffect(budgetEntries) {
        val lastEntry = budgetEntries.lastOrNull()
        if (lastEntry != null && lastEntry.amount.isNotBlank()) {
            val newId = (budgetEntries.maxOfOrNull { it.id } ?: 0) + 1
            budgetEntries = budgetEntries + BudgetEntry(id = newId)
        }
    }

    // Count valid entries
    val validEntries = budgetEntries.filter {
        it.amount.isNotBlank() && (it.amount.toDoubleOrNull() ?: 0.0) > 0
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
                        "Set Budgets",
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
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicAccentCard(WealthTheme.Emerald, 20)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(WealthTheme.Emerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = WealthTheme.Emerald,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "Create Multiple Budgets",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )

                            Text(
                                "${monthNames[selectedMonth - 1]} $selectedYear",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Add multiple category budgets at once. New rows appear automatically!",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Budget Entry Rows Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = WealthTheme.Emerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Budget Entries",
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

                // Budget Entry Rows
                items(budgetEntries.size) { index ->
                    val entry = budgetEntries[index]
                    val isUsed = entry.amount.isNotBlank()
                    val hasExistingBudget = categoriesWithBudgets.contains(entry.category)
                    val isValid = entry.amount.isNotBlank() && (entry.amount.toDoubleOrNull() ?: 0.0) > 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicAccentCard(
                                if (isValid) WealthTheme.Emerald
                                else if (hasExistingBudget) WealthTheme.Amber
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
                                    if (hasExistingBudget) {
                                        Text(
                                            "Already set",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WealthTheme.Amber
                                        )
                                    }
                                }

                                if (budgetEntries.size > 1 && !isUsed) {
                                    IconButton(
                                        onClick = {
                                            budgetEntries = budgetEntries.toMutableList().also {
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

                            // Category and Amount in a row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Category Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = entry.categoryExpanded,
                                    onExpandedChange = { expanded ->
                                        budgetEntries = budgetEntries.toMutableList().also {
                                            it[index] = entry.copy(categoryExpanded = expanded)
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    OutlinedTextField(
                                        value = "${entry.category.icon} ${entry.category.displayName}",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = entry.categoryExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WealthTheme.Emerald,
                                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                            focusedTextColor = WealthTheme.SoftWhite,
                                            unfocusedTextColor = WealthTheme.SoftWhite,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    ExposedDropdownMenu(
                                        expanded = entry.categoryExpanded,
                                        onDismissRequest = {
                                            budgetEntries = budgetEntries.toMutableList().also {
                                                it[index] = entry.copy(categoryExpanded = false)
                                            }
                                        },
                                        modifier = Modifier.background(WealthTheme.Navy)
                                    ) {
                                        ExpenseCategory.entries.forEach { cat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "${cat.icon} ${cat.displayName}",
                                                        color = WealthTheme.SoftWhite,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                onClick = {
                                                    budgetEntries = budgetEntries.toMutableList().also {
                                                        it[index] = entry.copy(category = cat, categoryExpanded = false)
                                                    }
                                                },
                                                modifier = Modifier.background(Color.Transparent)
                                            )
                                        }
                                    }
                                }

                                // Amount Field
                                OutlinedTextField(
                                    value = entry.amount,
                                    onValueChange = { newAmount ->
                                        budgetEntries = budgetEntries.toMutableList().also {
                                            it[index] = entry.copy(amount = newAmount.filter { c -> c.isDigit() || c == '.' })
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            "Amount",
                                            color = WealthTheme.SoftWhite.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    leadingIcon = {
                                        Text(
                                            "$",
                                            color = WealthTheme.Emerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WealthTheme.Emerald,
                                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                                        focusedTextColor = WealthTheme.SoftWhite,
                                        unfocusedTextColor = WealthTheme.SoftWhite,
                                        cursorColor = WealthTheme.Emerald,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WealthTheme.SoftWhite
                                    )
                                )
                            }
                        }
                    }
                }

                // Budget Tips Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphicAccentCard(WealthTheme.Gold, 16)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(WealthTheme.Gold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = WealthTheme.Gold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Smart Budget Tips",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthTheme.SoftWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            BudgetTipItem("Food", "10-15%", Icons.Default.Restaurant)
                            BudgetTipItem("Housing", "25-30%", Icons.Default.Home)
                            BudgetTipItem("Transport", "10-15%", Icons.Default.DirectionsCar)
                            BudgetTipItem("Entertainment", "5-10%", Icons.Default.Celebration)
                            BudgetTipItem("Savings", "20%+", Icons.Default.Savings)
                        }
                    }
                }

                // Save Button
                item {
                    Button(
                        onClick = {
                            validEntries.forEach { entry ->
                                val budgetAmount = entry.amount.toDoubleOrNull() ?: 0.0
                                if (budgetAmount > 0) {
                                    viewModel.addBudget(
                                        Budget(
                                            category = entry.category,
                                            limit = budgetAmount,
                                            month = selectedMonth,
                                            year = selectedYear
                                        )
                                    )
                                }
                            }
                            onBack()
                        },
                        enabled = validEntries.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WealthTheme.Emerald,
                            disabledContainerColor = WealthTheme.SoftWhite.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (validEntries.size == 1) "Save Budget"
                            else "Save ${validEntries.size} Budgets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEntryRow(
    entry: Any, // Using Any to avoid data class reference issues
    index: Int,
    hasExistingBudget: Boolean,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onAmountChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    // Extract entry properties using reflection-like approach
    val category = (entry as? Map<*, *>)?.get("category") as? ExpenseCategory
        ?: ExpenseCategory.FOOD
    val amount = (entry as? Map<*, *>)?.get("amount") as? String ?: ""
    val categoryExpanded = (entry as? Map<*, *>)?.get("categoryExpanded") as? Boolean ?: false

    // Actually, let's handle this properly since entry is a data class
    // We need to use the actual data class properties
    val actualCategory = try {
        entry.javaClass.getDeclaredField("category").apply { isAccessible = true }.get(entry) as ExpenseCategory
    } catch (e: Exception) {
        ExpenseCategory.FOOD
    }
    val actualAmount = try {
        entry.javaClass.getDeclaredField("amount").apply { isAccessible = true }.get(entry) as String
    } catch (e: Exception) {
        ""
    }
    val actualExpanded = try {
        entry.javaClass.getDeclaredField("categoryExpanded").apply { isAccessible = true }.get(entry) as Boolean
    } catch (e: Exception) {
        false
    }

    val isValid = actualAmount.isNotBlank() && (actualAmount.toDoubleOrNull() ?: 0.0) > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(
                if (isValid) WealthTheme.Emerald
                else if (hasExistingBudget) WealthTheme.Amber
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
                            "$index",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isValid) Color.White else WealthTheme.SoftWhite
                        )
                    }
                    if (hasExistingBudget) {
                        Text(
                            "Already set",
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthTheme.Amber
                        )
                    }
                }

                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
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

            // Category and Amount in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = actualExpanded,
                    onExpandedChange = onExpandedChange,
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = "${actualCategory.icon} ${actualCategory.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = actualExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WealthTheme.Emerald,
                            unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = WealthTheme.SoftWhite,
                            unfocusedTextColor = WealthTheme.SoftWhite,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = actualExpanded,
                        onDismissRequest = { onExpandedChange(false) },
                        modifier = Modifier.background(WealthTheme.Navy)
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${cat.icon} ${cat.displayName}",
                                        color = WealthTheme.SoftWhite,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = { onCategoryChange(cat) },
                                modifier = Modifier.background(Color.Transparent)
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = actualAmount,
                    onValueChange = { onAmountChange(it.filter { c -> c.isDigit() || c == '.' }) },
                    placeholder = {
                        Text(
                            "Amount",
                            color = WealthTheme.SoftWhite.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Text(
                            "$",
                            color = WealthTheme.Emerald,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WealthTheme.Emerald,
                        unfocusedBorderColor = WealthTheme.SoftWhite.copy(alpha = 0.3f),
                        focusedTextColor = WealthTheme.SoftWhite,
                        unfocusedTextColor = WealthTheme.SoftWhite,
                        cursorColor = WealthTheme.Emerald,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WealthTheme.SoftWhite
                    )
                )
            }
        }
    }
}

@Composable
private fun BudgetTipItem(
    category: String,
    percentage: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = WealthTheme.SoftWhite.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                category,
                style = MaterialTheme.typography.bodyMedium,
                color = WealthTheme.SoftWhite.copy(alpha = 0.8f)
            )
        }
        Text(
            percentage,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = WealthTheme.Gold
        )
    }
}

// ========== BUDGET EXPORT DIALOG ==========
@Composable
private fun BudgetExportDialog(
    budgets: List<Budget>,
    monthName: String,
    year: Int,
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
                    Box(modifier = Modifier.size(56.dp).background(WealthTheme.Emerald.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = WealthTheme.Emerald, modifier = Modifier.size(32.dp))
                    }
                }
            },
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Export Successful!", color = WealthTheme.SoftWhite, fontWeight = FontWeight.Bold) } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your budget report has been exported.", color = WealthTheme.SoftWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportedFile?.name ?: "", color = WealthTheme.Emerald, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { exportedFile?.let { shareBudgetFile(context, it, if (selectedFormat == "PDF") "application/pdf" else "text/csv") }; showSuccessDialog = false; onDismiss() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthTheme.Emerald), border = BorderStroke(1.dp, WealthTheme.Emerald)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Share")
                        }
                        Button(onClick = { exportedFile?.let { openBudgetFile(context, it) }; showSuccessDialog = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = WealthTheme.Emerald)) {
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
        title = { Text("Export Budget", color = WealthTheme.SoftWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Export $monthName $year budget (${budgets.size} categories)", color = WealthTheme.SoftWhite.copy(alpha = 0.7f))
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
                val file = exportBudgetData(context, budgets, monthName, year, selectedFormat)
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

private fun exportBudgetData(context: Context, budgets: List<Budget>, monthName: String, year: Int, format: String): File? {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    return try {
        when (format) {
            "Excel" -> {
                val file = File(context.getExternalFilesDir(null), "Budget_${monthName}_${year}_$timestamp.csv")
                FileWriter(file).use { writer ->
                    writer.append("Budget Report - $monthName $year\n")
                    writer.append("Generated,${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}\n\n")
                    writer.append("Category,Budget Limit\n")
                    budgets.forEach { budget ->
                        writer.append("${budget.category.displayName},$${String.format("%.2f", budget.limit)}\n")
                    }
                    val totalBudget = budgets.sumOf { it.limit }
                    writer.append("\nTOTAL,$${String.format("%.2f", totalBudget)}\n")
                }
                file
            }
            "PDF" -> {
                val file = File(context.getExternalFilesDir(null), "Budget_${monthName}_${year}_$timestamp.pdf")
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
                headerCell.add(Paragraph("BUDGET REPORT").setFontSize(16f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.CENTER).setMarginTop(-5f))
                headerCell.add(Paragraph("$monthName $year").setFontSize(12f).setFontColor(DeviceRgb(150, 180, 175)).setTextAlignment(PdfTextAlignment.CENTER).setMarginTop(8f))
                headerTable.addCell(headerCell)
                document.add(headerTable)

                // Info bar
                val totalBudget = budgets.sumOf { it.limit }
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Categories: ${budgets.size}").setFontSize(11f).setFontColor(softWhite)))
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Generated: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}").setFontSize(11f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.RIGHT)))
                document.add(infoTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Summary
                document.add(Paragraph("SUMMARY").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
                listOf("Total Categories", "Total Budget").forEach { header ->
                    summaryTable.addCell(Cell().add(Paragraph(header).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                summaryTable.addCell(Cell().add(Paragraph(budgets.size.toString()).setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                summaryTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", totalBudget)}").setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(emeraldColor))
                document.add(summaryTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Budget details
                document.add(Paragraph("BUDGET DETAILS").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val budgetTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f))).useAllAvailableWidth()
                listOf("Category", "Budget Limit").forEach { header ->
                    budgetTable.addCell(Cell().add(Paragraph(header).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                budgets.forEachIndexed { index, budget ->
                    val bgColor = if (index % 2 == 0) whiteColor else lightGray
                    budgetTable.addCell(Cell().add(Paragraph(budget.category.displayName).setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f))
                    budgetTable.addCell(Cell().add(Paragraph("$${String.format("%,.2f", budget.limit)}").setFontSize(9f)).setBackgroundColor(bgColor).setPadding(6f).setTextAlignment(PdfTextAlignment.RIGHT).setFontColor(emeraldColor))
                }
                document.add(budgetTable)

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

private fun shareBudgetFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Budget Report"))
}

private fun openBudgetFile(context: Context, file: File) {
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

