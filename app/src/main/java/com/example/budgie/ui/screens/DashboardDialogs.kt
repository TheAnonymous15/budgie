package com.example.budgie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.IncomeSource
import com.example.budgie.ui.viewmodel.MainViewModel

// Glassmorphism Colors
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassHighlight = Color.White.copy(alpha = 0.15f)

// Glassmorphism Card Modifier
private fun Modifier.glassmorphicCard(
    cornerRadius: Int = 16,
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

// Glassmorphic Add Income Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAddIncomeDialog(
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
                .glassmorphicCard(24, 1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DashboardNavy.copy(alpha = 0.98f),
                                DashboardNavyLight.copy(alpha = 0.95f)
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
                                    .background(DashboardEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = DashboardEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Add Income",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DashboardSoftWhite
                                )
                                Text(
                                    "Record your earnings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DashboardSoftWhite.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DashboardSoftWhite.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = DashboardSoftWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Income Title", color = DashboardSoftWhite.copy(alpha = 0.6f)) },
                        placeholder = { Text("e.g., Monthly Salary", color = DashboardSoftWhite.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DashboardEmerald,
                            unfocusedBorderColor = DashboardSoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = DashboardSoftWhite,
                            unfocusedTextColor = DashboardSoftWhite,
                            cursorColor = DashboardEmerald
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount", color = DashboardSoftWhite.copy(alpha = 0.6f)) },
                        placeholder = { Text("0.00", color = DashboardSoftWhite.copy(alpha = 0.4f)) },
                        prefix = {
                            Text(
                                "$",
                                color = DashboardEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DashboardEmerald,
                            unfocusedBorderColor = DashboardSoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = DashboardSoftWhite,
                            unfocusedTextColor = DashboardSoftWhite,
                            cursorColor = DashboardEmerald
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
                            label = { Text("Income Source", color = DashboardSoftWhite.copy(alpha = 0.6f)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DashboardEmerald,
                                unfocusedBorderColor = DashboardSoftWhite.copy(alpha = 0.3f),
                                focusedTextColor = DashboardSoftWhite,
                                unfocusedTextColor = DashboardSoftWhite
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false },
                            modifier = Modifier.background(DashboardNavyLight)
                        ) {
                            IncomeSource.entries.forEach { source ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${source.icon} ${source.displayName}",
                                            color = DashboardSoftWhite
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
                            .background(DashboardSoftWhite.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                DashboardSoftWhite.copy(alpha = 0.1f),
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
                                    tint = if (isRecurring) DashboardEmerald else DashboardSoftWhite.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Recurring Income",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = DashboardSoftWhite
                                    )
                                    Text(
                                        "Income repeats monthly",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DashboardSoftWhite.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DashboardEmerald,
                                    uncheckedThumbColor = DashboardSoftWhite,
                                    uncheckedTrackColor = DashboardSoftWhite.copy(alpha = 0.3f)
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
                                contentColor = DashboardSoftWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                DashboardSoftWhite.copy(alpha = 0.3f)
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
                                containerColor = DashboardEmerald,
                                disabledContainerColor = DashboardSoftWhite.copy(alpha = 0.2f)
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

