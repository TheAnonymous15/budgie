package com.example.budgie.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.IncomeSource
import com.example.budgie.ui.viewmodel.MainViewModel
import java.util.*

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE ADD INCOME DIALOG
   Premium glassmorphic income entry with responsive sizing
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveAddIncomeDialog(
    dimens: DashboardDimens,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val sources = listOf("Salary", "Freelance", "Investment", "Rental", "Gift", "Other")
    var expandedDropdown by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(dimens.dialogWidth)
                .clip(RoundedCornerShape(dimens.cardCornerRadius))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DashboardNavyLight.copy(alpha = 0.98f),
                            DashboardNavy.copy(alpha = 0.99f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(dimens.dialogPadding)
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
                                .size(dimens.sectionIconContainerSize + 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DashboardEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = DashboardEmerald,
                                modifier = Modifier.size(dimens.sectionIconSize)
                            )
                        }
                        Spacer(modifier = Modifier.width(dimens.actionButtonSpacing))
                        Column {
                            Text(
                                "Add Income",
                                fontSize = dimens.dialogTitleSize.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashboardSoftWhite
                            )
                            Text(
                                "Record your earnings",
                                fontSize = (dimens.dialogMessageSize).sp,
                                color = DashboardSoftWhite.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DashboardSoftWhite.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimens.sectionSpacing))

                // Amount Input
                Text(
                    "Amount",
                    fontSize = dimens.dialogMessageSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = DashboardSoftWhite.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() || char == '.' }
                        showError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.incomeDialogInputHeight),
                    placeholder = {
                        Text(
                            "0.00",
                            color = DashboardSoftWhite.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Text(
                            "$",
                            color = DashboardEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = (dimens.dialogMessageSize + 2).sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = showError && amount.isEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DashboardEmerald,
                        unfocusedBorderColor = DashboardSoftWhite.copy(alpha = 0.2f),
                        focusedTextColor = DashboardSoftWhite,
                        unfocusedTextColor = DashboardSoftWhite,
                        cursorColor = DashboardEmerald,
                        errorBorderColor = DashboardMutedRed
                    ),
                    shape = RoundedCornerShape(dimens.actionButtonCornerRadius)
                )

                Spacer(modifier = Modifier.height(dimens.verticalSpacing))

                // Source Dropdown
                Text(
                    "Source",
                    fontSize = dimens.dialogMessageSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = DashboardSoftWhite.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = it }
                ) {
                    OutlinedTextField(
                        value = source.ifEmpty { "Select source" },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .height(dimens.incomeDialogInputHeight),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                        },
                        isError = showError && source.isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DashboardEmerald,
                            unfocusedBorderColor = DashboardSoftWhite.copy(alpha = 0.2f),
                            focusedTextColor = if (source.isEmpty())
                                DashboardSoftWhite.copy(alpha = 0.4f)
                            else DashboardSoftWhite,
                            unfocusedTextColor = if (source.isEmpty())
                                DashboardSoftWhite.copy(alpha = 0.4f)
                            else DashboardSoftWhite,
                            errorBorderColor = DashboardMutedRed
                        ),
                        shape = RoundedCornerShape(dimens.actionButtonCornerRadius)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(DashboardNavyLight)
                    ) {
                        sources.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Text(s, color = DashboardSoftWhite)
                                },
                                onClick = {
                                    source = s
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimens.verticalSpacing))

                // Recurring Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = DashboardSoftWhite.copy(alpha = 0.7f),
                            modifier = Modifier.size(dimens.actionButtonIconSize)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Recurring income",
                            fontSize = dimens.dialogMessageSize.sp,
                            color = DashboardSoftWhite.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DashboardEmerald,
                            checkedTrackColor = DashboardEmerald.copy(alpha = 0.3f),
                            uncheckedThumbColor = DashboardSoftWhite.copy(alpha = 0.5f),
                            uncheckedTrackColor = DashboardSoftWhite.copy(alpha = 0.2f)
                        )
                    )
                }

                // Error message
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please fill in all required fields",
                        fontSize = (dimens.dialogMessageSize - 1).sp,
                        color = DashboardMutedRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(dimens.sectionSpacing))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonSpacing)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.dialogButtonHeight),
                        shape = RoundedCornerShape(dimens.actionButtonCornerRadius),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            DashboardSoftWhite.copy(alpha = 0.25f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DashboardSoftWhite.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.Medium,
                            fontSize = dimens.actionButtonFontSize.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (amount.isNotEmpty() && source.isNotEmpty()) {
                                val amountValue = amount.toDoubleOrNull() ?: 0.0
                                if (amountValue > 0) {
                                    // Map string source to IncomeSource enum
                                    val incomeSource = when (source) {
                                        "Salary" -> IncomeSource.SALARY
                                        "Freelance" -> IncomeSource.FREELANCE
                                        "Investment" -> IncomeSource.INVESTMENT
                                        "Rental" -> IncomeSource.RENTAL
                                        "Gift" -> IncomeSource.GIFT
                                        else -> IncomeSource.OTHER
                                    }

                                    val income = Income(
                                        title = source,
                                        amount = amountValue,
                                        source = incomeSource,
                                        date = System.currentTimeMillis(),
                                        isRecurring = isRecurring
                                    )
                                    viewModel.addIncome(income)
                                    onDismiss()
                                } else {
                                    showError = true
                                }
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.dialogButtonHeight),
                        shape = RoundedCornerShape(dimens.actionButtonCornerRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = DashboardEmerald)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(dimens.actionButtonIconSize)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Add Income",
                            fontWeight = FontWeight.Bold,
                            fontSize = dimens.actionButtonFontSize.sp
                        )
                    }
                }
            }
        }
    }
}

