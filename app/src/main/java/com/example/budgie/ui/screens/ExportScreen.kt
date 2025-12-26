package com.example.budgie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.data.model.Bill
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.FinancialGoal
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.Loan
import com.example.budgie.data.model.ShoppingListWithItems
import com.example.budgie.ui.theme.WealthTheme
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.util.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
fun ExportScreen(
    viewModel: MainViewModel,
    userName: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val expenses by viewModel.expenses.collectAsState()
    val incomes by viewModel.incomes.collectAsState()
    val unpaidBills by viewModel.unpaidBills.collectAsState()
    val budgets by viewModel.currentBudgets.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val shoppingLists by viewModel.shoppingListsWithItems.collectAsState()

    var isExporting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<java.io.File?>(null) }
    var exportFormat by remember { mutableStateOf("") }

    // Data selection states
    var exportAll by remember { mutableStateOf(true) }
    var exportIncome by remember { mutableStateOf(true) }
    var exportExpenses by remember { mutableStateOf(true) }
    var exportBills by remember { mutableStateOf(true) }
    var exportBudgets by remember { mutableStateOf(true) }
    var exportLoans by remember { mutableStateOf(true) }
    var exportGoals by remember { mutableStateOf(true) }
    var exportShoppingLists by remember { mutableStateOf(true) }

    // Update individual selections when "All" is toggled
    LaunchedEffect(exportAll) {
        if (exportAll) {
            exportIncome = true
            exportExpenses = true
            exportBills = true
            exportBudgets = true
            exportLoans = true
            exportGoals = true
            exportShoppingLists = true
        }
    }

    // Update "All" checkbox based on individual selections
    LaunchedEffect(exportIncome, exportExpenses, exportBills, exportBudgets, exportLoans, exportGoals, exportShoppingLists) {
        exportAll = exportIncome && exportExpenses && exportBills && exportBudgets && exportLoans && exportGoals && exportShoppingLists
    }

    val hasSelectedData = exportIncome || exportExpenses || exportBills || exportBudgets || exportLoans || exportGoals || exportShoppingLists

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WealthTheme.SoftWhite)
                    }

                    Text(
                        "Export Data",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthTheme.SoftWhite
                    )

                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
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
                                Icons.Filled.Output,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = WealthTheme.Emerald
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Export Your Financial Data",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WealthTheme.SoftWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose what to export and download in PDF or Excel format",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Data Selection Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphicCard(16)
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
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = null,
                                tint = WealthTheme.Emerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Select Data to Export",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "You're in control. Choose exactly what data you want to export.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthTheme.SoftWhite.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Export All Option
                        DataSelectionItem(
                            icon = Icons.Default.SelectAll,
                            label = "Export All Data",
                            count = expenses.size + incomes.size + unpaidBills.size + budgets.size + loans.size + goals.size + shoppingLists.size,
                            color = WealthTheme.Gold,
                            isSelected = exportAll,
                            onSelectionChange = { exportAll = it },
                            isAllOption = true
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = WealthTheme.SoftWhite.copy(alpha = 0.1f)
                        )

                        // Individual Options
                        DataSelectionItem(
                            icon = Icons.Default.TrendingUp,
                            label = "Income",
                            count = incomes.size,
                            color = WealthTheme.Emerald,
                            isSelected = exportIncome,
                            onSelectionChange = { exportIncome = it }
                        )

                        DataSelectionItem(
                            icon = Icons.Default.Receipt,
                            label = "Expenses",
                            count = expenses.size,
                            color = WealthTheme.MutedRed,
                            isSelected = exportExpenses,
                            onSelectionChange = { exportExpenses = it }
                        )

                        DataSelectionItem(
                            icon = Icons.Default.CreditCard,
                            label = "Bills",
                            count = unpaidBills.size,
                            color = WealthTheme.Blue,
                            isSelected = exportBills,
                            onSelectionChange = { exportBills = it }
                        )

                        DataSelectionItem(
                            icon = Icons.Default.MonetizationOn,
                            label = "Loans",
                            count = loans.size,
                            color = WealthTheme.Purple,
                            isSelected = exportLoans,
                            onSelectionChange = { exportLoans = it }
                        )

                        DataSelectionItem(
                            icon = Icons.Default.AccountBalance,
                            label = "Budgets",
                            count = budgets.size,
                            color = WealthTheme.Cyan,
                            isSelected = exportBudgets,
                            onSelectionChange = { exportBudgets = it }
                        )

                        DataSelectionItem(
                            icon = Icons.Default.Flag,
                            label = "Goals",
                            count = goals.size,
                            color = WealthTheme.Gold,
                            isSelected = exportGoals,
                            onSelectionChange = { exportGoals = it }
                        )

                        DataSelectionItem(
                            icon = Icons.Default.ShoppingCart,
                            label = "Shopping Lists",
                            count = shoppingLists.size,
                            color = WealthTheme.Teal,
                            isSelected = exportShoppingLists,
                            onSelectionChange = { exportShoppingLists = it }
                        )

                        if (!hasSelectedData) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WealthTheme.Amber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Please select at least one data type to export",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WealthTheme.Amber
                                )
                            }
                        }
                    }
                }

                // Export Format Section
                Text(
                    text = "Choose Export Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite
                )

                // Export Buttons in a Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // PDF Export Option
                    ExportOptionCardCompact(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PictureAsPdf,
                        title = "PDF",
                        description = "Pro report",
                        color = Color(0xFFE53935),
                        isLoading = isExporting && exportFormat == "PDF",
                        enabled = hasSelectedData,
                        onClick = {
                            isExporting = true
                            exportFormat = "PDF"
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    ExportManager.exportToPdf(
                                        context = context,
                                        userName = userName ?: "User",
                                        expenses = if (exportExpenses) expenses else emptyList(),
                                        incomes = if (exportIncome) incomes else emptyList(),
                                        bills = if (exportBills) unpaidBills else emptyList(),
                                        loans = if (exportLoans) loans else emptyList(),
                                        goals = if (exportGoals) goals else emptyList(),
                                        shoppingLists = if (exportShoppingLists) shoppingLists else emptyList()
                                    )
                                }
                                isExporting = false
                                if (file != null) {
                                    exportedFile = file
                                    showSuccessDialog = true
                                } else {
                                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    // Excel Export Option
                    ExportOptionCardCompact(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TableChart,
                        title = "Excel",
                        description = "Spreadsheet",
                        color = Color(0xFF4CAF50),
                        isLoading = isExporting && exportFormat == "Excel",
                        enabled = hasSelectedData,
                        onClick = {
                            isExporting = true
                            exportFormat = "Excel"
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    ExportManager.exportToExcel(
                                        context = context,
                                        userName = userName ?: "User",
                                        expenses = if (exportExpenses) expenses else emptyList(),
                                        incomes = if (exportIncome) incomes else emptyList(),
                                        bills = if (exportBills) unpaidBills else emptyList(),
                                        loans = if (exportLoans) loans else emptyList(),
                                        goals = if (exportGoals) goals else emptyList(),
                                        shoppingLists = if (exportShoppingLists) shoppingLists else emptyList()
                                    )
                                }
                                isExporting = false
                                if (file != null) {
                                    exportedFile = file
                                    showSuccessDialog = true
                                } else {
                                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                // Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphicAccentCard(WealthTheme.Blue, 12)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(WealthTheme.Blue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = WealthTheme.Blue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your Data, Your Control",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WealthTheme.SoftWhite
                            )
                            Text(
                                text = "Exports include your name and timestamp. Income appears first when exporting all data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = WealthTheme.Navy,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(WealthTheme.Emerald.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WealthTheme.Emerald,
                        modifier = Modifier.size(40.dp)
                    )
                }
            },
            title = {
                Text(
                    "Export Successful! 🎉",
                    textAlign = TextAlign.Center,
                    color = WealthTheme.SoftWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your data has been exported successfully.",
                        textAlign = TextAlign.Center,
                        color = WealthTheme.SoftWhite.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportedFile?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = WealthTheme.SoftWhite.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Would you like to open or share the file?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = WealthTheme.SoftWhite,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportedFile?.let { file ->
                                ExportManager.openFile(context, file)
                            }
                            showSuccessDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WealthTheme.Emerald
                        )
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }

                    OutlinedButton(
                        onClick = {
                            exportedFile?.let { file ->
                                val mimeType = if (file.extension == "pdf") "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                ExportManager.shareFile(context, file, mimeType)
                            }
                            showSuccessDialog = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WealthTheme.SoftWhite
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = WealthTheme.SoftWhite.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DataSelectionItem(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    isAllOption: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectionChange(!isSelected) }
            .background(
                if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange,
            colors = CheckboxDefaults.colors(
                checkedColor = color,
                uncheckedColor = WealthTheme.SoftWhite.copy(alpha = 0.4f),
                checkmarkColor = Color.White
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isAllOption) FontWeight.Bold else FontWeight.Medium,
                color = WealthTheme.SoftWhite
            )
            if (!isAllOption) {
                Text(
                    text = "$count records",
                    style = MaterialTheme.typography.labelSmall,
                    color = WealthTheme.SoftWhite.copy(alpha = 0.5f)
                )
            }
        }

        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    fileExtension: String,
    color: Color,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cardAlpha = if (enabled) 1f else 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(if (enabled) color else WealthTheme.SoftWhite.copy(alpha = 0.3f), 14)
            .clickable(enabled = enabled && !isLoading) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.2f * cardAlpha)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = color,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color.copy(alpha = cardAlpha),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WealthTheme.SoftWhite.copy(alpha = cardAlpha)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = WealthTheme.SoftWhite.copy(alpha = 0.6f * cardAlpha)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f * cardAlpha))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = fileExtension,
                    style = MaterialTheme.typography.labelMedium,
                    color = color.copy(alpha = cardAlpha),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExportOptionCardCompact(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cardAlpha = if (enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .glassmorphicAccentCard(if (enabled) color else WealthTheme.SoftWhite.copy(alpha = 0.3f), 14)
            .clickable(enabled = enabled && !isLoading) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f * cardAlpha)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = color,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color.copy(alpha = cardAlpha),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WealthTheme.SoftWhite.copy(alpha = cardAlpha)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = WealthTheme.SoftWhite.copy(alpha = 0.6f * cardAlpha)
            )
        }
    }
}

