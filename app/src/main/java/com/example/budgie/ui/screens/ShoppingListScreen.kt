package com.example.budgie.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
private val WealthAmber = Color(0xFFFF9800)
private val WealthBlue = Color(0xFF5C9CE5)
private val WealthCyan = Color(0xFF26A69A)
private val WealthPurple = Color(0xFF9575CD)

// Glassmorphism Colors
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassHighlight = Color.White.copy(alpha = 0.15f)

// Glassmorphism Modifier
private fun Modifier.glassmorphicCard(cornerRadius: Int = 20) = this
    .shadow(8.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(GlassHighlight, GlassWhite, Color.White.copy(alpha = 0.05f))
        )
    )
    .border(1.dp, Brush.verticalGradient(listOf(GlassBorder, Color.White.copy(alpha = 0.05f))), RoundedCornerShape(cornerRadius.dp))

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
fun ShoppingListScreen(
    shoppingLists: List<ShoppingListWithItems>,
    monthlyIncome: Double = 0.0,
    monthlyExpenses: Double = 0.0,
    onCreateList: (ShoppingList) -> Unit,
    onUpdateList: (ShoppingList) -> Unit,
    onDeleteList: (ShoppingList) -> Unit,
    onAddItem: (ShoppingItem) -> Unit,
    onUpdateItem: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    onAnalyzeList: (String) -> Unit,
    onAddToBudget: (String, Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val selectedList = shoppingLists.find { it.shoppingList.id == selectedListId }

    // Export Dialog
    if (showExportDialog && shoppingLists.isNotEmpty()) {
        ShoppingListExportDialog(
            shoppingLists = shoppingLists,
            context = context,
            onDismiss = { showExportDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(WealthNavy, WealthNavyLight, WealthNavy)
                )
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Shopping Lists",
                                fontWeight = FontWeight.Bold,
                                color = WealthSoftWhite
                            )
                            Text(
                                "${shoppingLists.size} list${if (shoppingLists.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthSoftWhite.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WealthSoftWhite)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(WealthEmerald.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Add, "Create List", tint = WealthEmerald)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                if (selectedList != null) {
                    // Show selected list details
                    ShoppingListDetail(
                        listWithItems = selectedList,
                        monthlyIncome = monthlyIncome,
                        monthlyExpenses = monthlyExpenses,
                        onBack = { selectedListId = null },
                        onAddItem = { showAddItemDialog = selectedList.shoppingList.id },
                        onUpdateItem = onUpdateItem,
                        onDeleteItem = onDeleteItem,
                        onAnalyze = { onAnalyzeList(selectedList.shoppingList.id) },
                        onAddToBudget = { onAddToBudget(selectedList.shoppingList.id, selectedList.totalEstimated) }
                    )
                } else {
                    // Show all lists
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (shoppingLists.isEmpty()) {
                            item {
                                EmptyShoppingListCard(onCreateNew = { showCreateDialog = true })
                            }
                        } else {
                            items(shoppingLists) { listWithItems ->
                                ShoppingListCard(
                                    listWithItems = listWithItems,
                                    onClick = { selectedListId = listWithItems.shoppingList.id },
                                    onDelete = { onDeleteList(listWithItems.shoppingList) }
                                )
                            }
                        }

                        // Add space for FAB
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // Floating Export Button
            if (shoppingLists.isNotEmpty() && selectedList == null) {
                FloatingActionButton(
                    onClick = { showExportDialog = true },
                    containerColor = WealthEmerald,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp)
                ) {
                    Icon(Icons.Filled.FileUpload, "Export", modifier = Modifier.size(22.dp))
                }
            }
        }
    }

    // Create List Dialog
    if (showCreateDialog) {
        CreateShoppingListDialog(
            existingListCount = shoppingLists.size,
            onDismiss = { showCreateDialog = false },
            onConfirm = { list ->
                onCreateList(list)
                showCreateDialog = false
            }
        )
    }

    // Add Item Dialog
    showAddItemDialog?.let { listId ->
        AddShoppingItemDialog(
            listId = listId,
            onDismiss = { showAddItemDialog = null },
            onConfirm = { item ->
                onAddItem(item)
                showAddItemDialog = null
            }
        )
    }
}

@Composable
private fun EmptyShoppingListCard(onCreateNew: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .glassmorphicCard(20)
            .clickable(onClick = onCreateNew),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = WealthEmerald.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No shopping lists yet",
                style = MaterialTheme.typography.titleMedium,
                color = WealthSoftWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap to create your first list",
                style = MaterialTheme.typography.bodySmall,
                color = WealthSoftWhite.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ShoppingListCard(
    listWithItems: ShoppingListWithItems,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val list = listWithItems.shoppingList
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(WealthBlue, 16)
            .clickable(onClick = onClick)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WealthBlue.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛒", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            list.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        Text(
                            "List #${list.listNumber} • ${dateFormat.format(Date(list.createdAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WealthSoftWhite.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(WealthMutedRed.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = WealthMutedRed, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Items", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                    Text(
                        "${listWithItems.purchasedCount}/${listWithItems.itemCount}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }
                Column {
                    Text("Budget", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                    Text(
                        "$${String.format("%,.2f", list.totalBudget)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthEmerald
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Est. Total", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                    Text(
                        "$${String.format("%,.2f", listWithItems.totalEstimated)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (listWithItems.totalEstimated > list.totalBudget) WealthMutedRed else WealthSoftWhite
                    )
                }
            }

            // Progress bar
            if (listWithItems.itemCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { listWithItems.purchasedCount.toFloat() / listWithItems.itemCount },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = WealthEmerald,
                    trackColor = WealthSoftWhite.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun ShoppingListDetail(
    listWithItems: ShoppingListWithItems,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    onBack: () -> Unit,
    onAddItem: () -> Unit,
    onUpdateItem: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    onAnalyze: () -> Unit,
    onAddToBudget: () -> Unit
) {
    val list = listWithItems.shoppingList
    val items = listWithItems.items

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .glassmorphicAccentCard(WealthTeal, 16)
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WealthSoftWhite)
                    }
                    Text(
                        list.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                    IconButton(onClick = onAddItem) {
                        Icon(Icons.Default.Add, "Add Item", tint = WealthEmerald)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Budget", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                        Text(
                            "$${String.format("%,.2f", list.totalBudget)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthEmerald
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Items", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                        Text(
                            "${items.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Total", style = MaterialTheme.typography.labelSmall, color = WealthSoftWhite.copy(alpha = 0.6f))
                        Text(
                            "$${String.format("%,.2f", listWithItems.totalEstimated)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (listWithItems.totalEstimated > list.totalBudget) WealthMutedRed else WealthSoftWhite
                        )
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAnalyze,
                colors = ButtonDefaults.buttonColors(containerColor = WealthPurple),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Analyze", fontSize = 12.sp)
            }

            Button(
                onClick = onAddToBudget,
                colors = ButtonDefaults.buttonColors(containerColor = WealthEmerald),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Budget", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .glassmorphicCard(14),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = WealthSoftWhite.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No items yet", color = WealthSoftWhite.copy(alpha = 0.6f))
                        }
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    ShoppingItemCard(
                        item = item,
                        onTogglePurchased = {
                            onUpdateItem(item.copy(isPurchased = !item.isPurchased))
                        },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItem,
    onTogglePurchased: () -> Unit,
    onDelete: () -> Unit
) {
    val recommendationColor = when (item.aiRecommendation) {
        ShoppingRecommendation.OK -> WealthEmerald
        ShoppingRecommendation.REDUCE -> WealthAmber
        ShoppingRecommendation.REMOVE -> WealthMutedRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicAccentCard(recommendationColor, 12)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
                colors = CheckboxDefaults.colors(
                    checkedColor = WealthEmerald,
                    uncheckedColor = WealthSoftWhite.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.category.icon,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = WealthSoftWhite,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Priority indicator
                    repeat(5) { index ->
                        Icon(
                            if (index < item.needPriority) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (index < item.needPriority) WealthGold else WealthSoftWhite.copy(alpha = 0.3f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                // AI Reason if exists
                item.aiReason?.let { reason ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = recommendationColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = recommendationColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", item.estimatedPrice * item.quantity)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WealthSoftWhite
                )

                // AI suggested quantity
                item.aiSuggestedQuantity?.let { suggested ->
                    if (suggested != item.quantity) {
                        Text(
                            "Suggest: $suggested",
                            style = MaterialTheme.typography.labelSmall,
                            color = recommendationColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Close, "Delete", tint = WealthMutedRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateShoppingListDialog(
    existingListCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (ShoppingList) -> Unit
) {
    var title by remember { mutableStateOf("Shopping List ${existingListCount + 1}") }
    var budget by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicCard(24)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WealthNavy.copy(alpha = 0.95f))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = WealthEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Create Shopping List",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("List Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WealthSoftWhite,
                            unfocusedTextColor = WealthSoftWhite,
                            focusedBorderColor = WealthEmerald,
                            unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Budget (Optional)") },
                        placeholder = { Text("Enter budget amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WealthSoftWhite,
                            unfocusedTextColor = WealthSoftWhite,
                            focusedBorderColor = WealthEmerald,
                            unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                        ),
                        leadingIcon = { Text("$", color = WealthEmerald, fontWeight = FontWeight.Bold) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthSoftWhite)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onConfirm(
                                    ShoppingList(
                                        listNumber = existingListCount + 1,
                                        title = title,
                                        totalBudget = budget.toDoubleOrNull() ?: 0.0
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WealthEmerald),
                            enabled = title.isNotBlank()
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}

// Data class for shopping item entry
private data class ShoppingItemEntry(
    val id: Int,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "pcs",
    val price: String = "",
    val priority: Int = 3,
    val category: ShoppingCategory = ShoppingCategory.GROCERIES,
    val unitExpanded: Boolean = false,
    val categoryExpanded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddShoppingItemDialog(
    listId: String,
    onDismiss: () -> Unit,
    onConfirm: (ShoppingItem) -> Unit
) {
    val units = listOf("pcs", "kg", "g", "liters", "ml", "pack", "box", "bottle")

    // Item entries - start with 5 empty rows
    var itemEntries by remember {
        mutableStateOf(
            (1..5).map { ShoppingItemEntry(id = it) }
        )
    }

    // Auto-add new row when last row is filled
    LaunchedEffect(itemEntries) {
        val lastEntry = itemEntries.lastOrNull()
        if (lastEntry != null && lastEntry.name.isNotBlank()) {
            val newId = (itemEntries.maxOfOrNull { it.id } ?: 0) + 1
            itemEntries = itemEntries + ShoppingItemEntry(id = newId)
        }
    }

    // Count valid entries
    val validEntries = itemEntries.filter { it.name.isNotBlank() }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .glassmorphicCard(24)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WealthNavy.copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = WealthEmerald)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Add Items",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = WealthSoftWhite
                                )
                                Text(
                                    "New rows appear automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WealthSoftWhite.copy(alpha = 0.5f)
                                )
                            }
                        }
                        if (validEntries.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(WealthEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${validEntries.size}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Item entries
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(itemEntries.size) { index ->
                            val entry = itemEntries[index]
                            val isUsed = entry.name.isNotBlank()
                            val isValid = entry.name.isNotBlank()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphicAccentCard(
                                        if (isValid) WealthEmerald else WealthSoftWhite.copy(alpha = 0.3f),
                                        12
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    // Row number and remove
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(if (isValid) WealthEmerald else WealthSoftWhite.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isValid) Color.White else WealthSoftWhite,
                                                fontSize = 10.sp
                                            )
                                        }

                                        if (itemEntries.size > 1 && !isUsed) {
                                            IconButton(
                                                onClick = {
                                                    itemEntries = itemEntries.toMutableList().also { it.removeAt(index) }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = WealthSoftWhite.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Name and Quantity row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = entry.name,
                                            onValueChange = { newName ->
                                                itemEntries = itemEntries.toMutableList().also {
                                                    it[index] = entry.copy(name = newName)
                                                }
                                            },
                                            label = { Text("Name", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = WealthSoftWhite,
                                                unfocusedTextColor = WealthSoftWhite,
                                                focusedBorderColor = WealthEmerald,
                                                unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                                            ),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )

                                        OutlinedTextField(
                                            value = entry.quantity,
                                            onValueChange = { newQty ->
                                                itemEntries = itemEntries.toMutableList().also {
                                                    it[index] = entry.copy(quantity = newQty.filter { c -> c.isDigit() })
                                                }
                                            },
                                            label = { Text("Qty", fontSize = 11.sp) },
                                            modifier = Modifier.width(60.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = WealthSoftWhite,
                                                unfocusedTextColor = WealthSoftWhite,
                                                focusedBorderColor = WealthEmerald,
                                                unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                                            ),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )

                                        OutlinedTextField(
                                            value = entry.price,
                                            onValueChange = { newPrice ->
                                                itemEntries = itemEntries.toMutableList().also {
                                                    it[index] = entry.copy(price = newPrice.filter { c -> c.isDigit() || c == '.' })
                                                }
                                            },
                                            label = { Text("$", fontSize = 11.sp) },
                                            modifier = Modifier.width(70.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = WealthSoftWhite,
                                                unfocusedTextColor = WealthSoftWhite,
                                                focusedBorderColor = WealthEmerald,
                                                unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                                            ),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Unit, Category and Priority row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Unit dropdown
                                        ExposedDropdownMenuBox(
                                            expanded = entry.unitExpanded,
                                            onExpandedChange = { expanded ->
                                                itemEntries = itemEntries.toMutableList().also {
                                                    it[index] = entry.copy(unitExpanded = expanded)
                                                }
                                            },
                                            modifier = Modifier.width(70.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = entry.unit,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Unit", fontSize = 9.sp) },
                                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = WealthSoftWhite,
                                                    unfocusedTextColor = WealthSoftWhite,
                                                    focusedBorderColor = WealthEmerald,
                                                    unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                                                ),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                            ExposedDropdownMenu(
                                                expanded = entry.unitExpanded,
                                                onDismissRequest = {
                                                    itemEntries = itemEntries.toMutableList().also {
                                                        it[index] = entry.copy(unitExpanded = false)
                                                    }
                                                }
                                            ) {
                                                units.forEach { u ->
                                                    DropdownMenuItem(
                                                        text = { Text(u) },
                                                        onClick = {
                                                            itemEntries = itemEntries.toMutableList().also {
                                                                it[index] = entry.copy(unit = u, unitExpanded = false)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Category dropdown
                                        ExposedDropdownMenuBox(
                                            expanded = entry.categoryExpanded,
                                            onExpandedChange = { expanded ->
                                                itemEntries = itemEntries.toMutableList().also {
                                                    it[index] = entry.copy(categoryExpanded = expanded)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                value = "${entry.category.icon}",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Cat", fontSize = 9.sp) },
                                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = WealthSoftWhite,
                                                    unfocusedTextColor = WealthSoftWhite,
                                                    focusedBorderColor = WealthEmerald,
                                                    unfocusedBorderColor = WealthSoftWhite.copy(alpha = 0.3f)
                                                ),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                            ExposedDropdownMenu(
                                                expanded = entry.categoryExpanded,
                                                onDismissRequest = {
                                                    itemEntries = itemEntries.toMutableList().also {
                                                        it[index] = entry.copy(categoryExpanded = false)
                                                    }
                                                }
                                            ) {
                                                ShoppingCategory.entries.forEach { category ->
                                                    DropdownMenuItem(
                                                        text = { Text("${category.icon} ${category.displayName}") },
                                                        onClick = {
                                                            itemEntries = itemEntries.toMutableList().also {
                                                                it[index] = entry.copy(category = category, categoryExpanded = false)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Priority
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            (1..5).forEach { p ->
                                                val color = when (p) {
                                                    1 -> WealthMutedRed
                                                    2 -> WealthAmber
                                                    3 -> WealthEmerald
                                                    4 -> WealthBlue
                                                    5 -> WealthPurple
                                                    else -> WealthSoftWhite
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (entry.priority == p) color
                                                            else WealthSoftWhite.copy(alpha = 0.1f)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (entry.priority == p) color else WealthSoftWhite.copy(alpha = 0.3f),
                                                            CircleShape
                                                        )
                                                        .clickable {
                                                            itemEntries = itemEntries.toMutableList().also {
                                                                it[index] = entry.copy(priority = p)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "$p",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (entry.priority == p) Color.White else WealthSoftWhite
                                                    )
                                                }
                                            }
                                        }
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
                                        .glassmorphicAccentCard(WealthGold, 12)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Total (${validEntries.size} items)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = WealthSoftWhite
                                        )
                                        val total = validEntries.sumOf {
                                            (it.price.toDoubleOrNull() ?: 0.0) * (it.quantity.toIntOrNull() ?: 1)
                                        }
                                        Text(
                                            "$${String.format("%.2f", total)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = WealthGold
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthSoftWhite)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                // Add all valid items
                                validEntries.forEach { entry ->
                                    onConfirm(
                                        ShoppingItem(
                                            listId = listId,
                                            name = entry.name,
                                            quantity = entry.quantity.toIntOrNull() ?: 1,
                                            unit = entry.unit,
                                            estimatedPrice = entry.price.toDoubleOrNull() ?: 0.0,
                                            needPriority = entry.priority,
                                            category = entry.category
                                        )
                                    )
                                }
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = WealthEmerald),
                            enabled = validEntries.isNotEmpty()
                        ) {
                            Text(
                                if (validEntries.isEmpty()) "Add at least one"
                                else "Add ${validEntries.size} Item${if (validEntries.size > 1) "s" else ""}"
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== SHOPPING LIST EXPORT DIALOG ==========
@Composable
private fun ShoppingListExportDialog(
    shoppingLists: List<ShoppingListWithItems>,
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
            containerColor = WealthNavy,
            icon = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(56.dp).background(WealthEmerald.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = WealthEmerald, modifier = Modifier.size(32.dp))
                    }
                }
            },
            title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Export Successful!", color = WealthSoftWhite, fontWeight = FontWeight.Bold) } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your shopping lists have been exported.", color = WealthSoftWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportedFile?.name ?: "", color = WealthEmerald, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { exportedFile?.let { shareShoppingFile(context, it, if (selectedFormat == "PDF") "application/pdf" else "text/csv") }; showSuccessDialog = false; onDismiss() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WealthEmerald), border = BorderStroke(1.dp, WealthEmerald)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Share")
                        }
                        Button(onClick = { exportedFile?.let { openShoppingFile(context, it) }; showSuccessDialog = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = WealthEmerald)) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Open")
                        }
                    }
                    TextButton(onClick = { showSuccessDialog = false; onDismiss() }) { Text("Done", color = WealthSoftWhite.copy(alpha = 0.7f)) }
                }
            },
            dismissButton = null
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WealthNavy,
        title = { Text("Export Shopping Lists", color = WealthSoftWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                val totalItems = shoppingLists.sumOf { it.items.size }
                Text("Export ${shoppingLists.size} list(s) with $totalItems items", color = WealthSoftWhite.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = selectedFormat == "PDF", onClick = { selectedFormat = "PDF" }, label = { Text("PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WealthBlue), modifier = Modifier.weight(1f))
                    FilterChip(selected = selectedFormat == "Excel", onClick = { selectedFormat = "Excel" }, label = { Text("Excel") },
                        leadingIcon = { Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WealthEmerald), modifier = Modifier.weight(1f))
                }
                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WealthEmerald, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp)); Text("Exporting...", color = WealthSoftWhite.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                isExporting = true
                val file = exportShoppingData(context, shoppingLists, selectedFormat)
                isExporting = false
                if (file != null) { exportedFile = file; showSuccessDialog = true }
                else Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }, enabled = !isExporting, colors = ButtonDefaults.buttonColors(containerColor = if (selectedFormat == "PDF") WealthBlue else WealthEmerald)) {
                Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Export")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = WealthSoftWhite) } }
    )
}

private fun exportShoppingData(context: Context, shoppingLists: List<ShoppingListWithItems>, format: String): File? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    return try {
        when (format) {
            "Excel" -> {
                val file = File(context.getExternalFilesDir(null), "Shopping_Lists_$timestamp.csv")
                FileWriter(file).use { writer ->
                    writer.append("Shopping Lists Report\nGenerated,${dateFormat.format(Date())}\n\n")
                    shoppingLists.forEach { listWithItems ->
                        writer.append("List: ${listWithItems.shoppingList.title}\n")
                        writer.append("Budget: $${String.format("%.2f", listWithItems.shoppingList.totalBudget)}\n")
                        writer.append("Item,Quantity,Price,Priority,Status\n")
                        listWithItems.items.forEach { item ->
                            writer.append("${item.name},${item.quantity},$${String.format("%.2f", item.estimatedPrice)},${item.needPriority},${if (item.isPurchased) "Purchased" else "Pending"}\n")
                        }
                        writer.append("\n")
                    }
                }
                file
            }
            "PDF" -> {
                val file = File(context.getExternalFilesDir(null), "Shopping_Lists_$timestamp.pdf")
                val navyPrimary = DeviceRgb(11, 31, 42)
                val navySecondary = DeviceRgb(15, 40, 55)
                val emeraldColor = DeviceRgb(15, 174, 150)
                val softWhite = DeviceRgb(230, 241, 240)
                val whiteColor = DeviceRgb(255, 255, 255)
                val lightGray = DeviceRgb(245, 247, 250)
                val amberColor = DeviceRgb(255, 152, 0)
                val redColor = DeviceRgb(229, 115, 115)

                val pdfWriter = PdfWriter(file)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                document.setMargins(36f, 36f, 36f, 36f)

                // Header
                val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
                val headerCell = Cell().setBackgroundColor(navyPrimary).setPadding(20f).setBorder(null)
                headerCell.add(Paragraph("BUDGIE").setFontSize(28f).setBold().setFontColor(emeraldColor).setTextAlignment(PdfTextAlignment.CENTER))
                headerCell.add(Paragraph("SHOPPING LISTS").setFontSize(16f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.CENTER).setMarginTop(-5f))
                headerCell.add(Paragraph("Your Personal Finance Companion").setFontSize(10f).setFontColor(DeviceRgb(150, 180, 175)).setTextAlignment(PdfTextAlignment.CENTER).setItalic().setMarginTop(8f))
                headerTable.addCell(headerCell)
                document.add(headerTable)

                // Info bar
                val totalItems = shoppingLists.sumOf { it.items.size }
                val totalBudget = shoppingLists.sumOf { it.shoppingList.totalBudget }
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Lists: ${shoppingLists.size} | Items: $totalItems").setFontSize(11f).setFontColor(softWhite)))
                infoTable.addCell(Cell().setBackgroundColor(navySecondary).setPadding(12f).setBorder(null).add(Paragraph("Generated: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}").setFontSize(11f).setFontColor(softWhite).setTextAlignment(PdfTextAlignment.RIGHT)))
                document.add(infoTable)

                document.add(Paragraph(" ").setMarginBottom(15f))

                // Summary
                document.add(Paragraph("SUMMARY").setFontSize(14f).setBold().setFontColor(navyPrimary).setMarginBottom(10f))
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f))).useAllAvailableWidth()
                listOf("Total Lists", "Total Items", "Total Budget").forEach {
                    summaryTable.addCell(Cell().add(Paragraph(it).setBold().setFontSize(9f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                listOf(shoppingLists.size.toString(), totalItems.toString(), "$${String.format("%,.2f", totalBudget)}").forEach {
                    summaryTable.addCell(Cell().add(Paragraph(it).setFontSize(10f)).setBackgroundColor(lightGray).setPadding(8f).setTextAlignment(PdfTextAlignment.CENTER))
                }
                document.add(summaryTable)

                // Each list
                shoppingLists.forEach { listWithItems ->
                    document.add(Paragraph(" ").setMarginBottom(10f))
                    document.add(Paragraph(listWithItems.shoppingList.title.uppercase()).setFontSize(12f).setBold().setFontColor(navyPrimary).setMarginBottom(5f))
                    document.add(Paragraph("Budget: $${String.format("%,.2f", listWithItems.shoppingList.totalBudget)}").setFontSize(10f).setFontColor(emeraldColor).setMarginBottom(8f))

                    if (listWithItems.items.isNotEmpty()) {
                        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 0.6f, 1f, 0.6f, 0.8f))).useAllAvailableWidth()
                        listOf("Item", "Qty", "Price", "Priority", "Status").forEach { header ->
                            itemsTable.addCell(Cell().add(Paragraph(header).setBold().setFontSize(8f)).setBackgroundColor(navyPrimary).setFontColor(whiteColor).setPadding(6f).setTextAlignment(PdfTextAlignment.CENTER))
                        }
                        listWithItems.items.forEachIndexed { index, item ->
                            val bgColor = if (index % 2 == 0) whiteColor else lightGray
                            itemsTable.addCell(Cell().add(Paragraph(item.name).setFontSize(8f)).setBackgroundColor(bgColor).setPadding(5f))
                            itemsTable.addCell(Cell().add(Paragraph(item.quantity.toString()).setFontSize(8f)).setBackgroundColor(bgColor).setPadding(5f).setTextAlignment(PdfTextAlignment.CENTER))
                            itemsTable.addCell(Cell().add(Paragraph("$${String.format("%.2f", item.estimatedPrice)}").setFontSize(8f)).setBackgroundColor(bgColor).setPadding(5f).setTextAlignment(PdfTextAlignment.RIGHT))
                            val priorityColor = when (item.needPriority) { 5 -> redColor; 4 -> amberColor; else -> emeraldColor }
                            itemsTable.addCell(Cell().add(Paragraph(item.needPriority.toString()).setFontSize(8f)).setBackgroundColor(bgColor).setPadding(5f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(priorityColor))
                            itemsTable.addCell(Cell().add(Paragraph(if (item.isPurchased) "✓" else "○").setFontSize(8f)).setBackgroundColor(bgColor).setPadding(5f).setTextAlignment(PdfTextAlignment.CENTER).setFontColor(if (item.isPurchased) emeraldColor else navyPrimary))
                        }
                        document.add(itemsTable)
                    }
                }

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

private fun shareShoppingFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Shopping Lists"))
}

private fun openShoppingFile(context: Context, file: File) {
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
