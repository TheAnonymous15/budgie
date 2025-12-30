package com.example.budgie.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.DecimalFormat
import kotlin.math.*

// Theme colors
private val CalcNavy = Color(0xFF0A1628)
private val CalcNavyLight = Color(0xFF1A2A44)
private val CalcEmerald = Color(0xFF0FAE96)
private val CalcSoftWhite = Color(0xFFF5F5F5)
private val CalcOrange = Color(0xFFFF9500)
private val CalcBlue = Color(0xFF5C9CE5)
private val CalcRed = Color(0xFFE57373)

@Composable
fun CalculatorModal(
    onDismiss: () -> Unit
) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var lastOperator by remember { mutableStateOf<String?>(null) }
    var lastNumber by remember { mutableStateOf<Double?>(null) }
    var newNumber by remember { mutableStateOf(true) }
    var memory by remember { mutableStateOf(0.0) }
    var showScientific by remember { mutableStateOf(false) }

    val decimalFormat = remember { DecimalFormat("#,##0.########") }

    fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble() && abs(value) < 1e15) {
            decimalFormat.format(value.toLong())
        } else {
            decimalFormat.format(value)
        }
    }

    fun calculate() {
        if (lastNumber != null && lastOperator != null) {
            val current = display.replace(",", "").toDoubleOrNull() ?: 0.0
            val result = when (lastOperator) {
                "+" -> lastNumber!! + current
                "-" -> lastNumber!! - current
                "×" -> lastNumber!! * current
                "÷" -> if (current != 0.0) lastNumber!! / current else Double.NaN
                "%" -> lastNumber!! * (current / 100)
                "^" -> lastNumber!!.pow(current)
                else -> current
            }
            display = if (result.isNaN() || result.isInfinite()) "Error" else formatNumber(result)
            expression = ""
            lastNumber = null
            lastOperator = null
            newNumber = true
        }
    }

    fun onNumberClick(number: String) {
        if (display == "Error") {
            display = number
            newNumber = false
            return
        }

        if (newNumber) {
            display = if (number == ".") "0." else number
            newNumber = false
        } else {
            if (number == "." && display.contains(".")) return
            if (display.replace(",", "").length >= 15) return
            display = if (display == "0" && number != ".") number else display + number
        }

        // Format with commas
        val value = display.replace(",", "").toDoubleOrNull()
        if (value != null && !display.contains(".")) {
            display = formatNumber(value)
        }
    }

    fun onOperatorClick(operator: String) {
        if (display == "Error") return

        calculate()

        lastNumber = display.replace(",", "").toDoubleOrNull() ?: 0.0
        lastOperator = operator
        expression = "${formatNumber(lastNumber!!)} $operator"
        newNumber = true
    }

    fun onClear() {
        display = "0"
        expression = ""
        lastOperator = null
        lastNumber = null
        newNumber = true
    }

    fun onClearEntry() {
        display = "0"
        newNumber = true
    }

    fun onBackspace() {
        if (display == "Error" || display.length <= 1 || (display.length == 2 && display.startsWith("-"))) {
            display = "0"
            newNumber = true
        } else {
            display = display.dropLast(1)
            val value = display.replace(",", "").toDoubleOrNull()
            if (value != null && !display.contains(".")) {
                display = formatNumber(value)
            }
        }
    }

    fun onNegate() {
        if (display == "Error" || display == "0") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        display = formatNumber(-value)
    }

    fun onPercent() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        display = formatNumber(value / 100)
        newNumber = true
    }

    fun onSquareRoot() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        if (value < 0) {
            display = "Error"
        } else {
            display = formatNumber(sqrt(value))
        }
        newNumber = true
    }

    fun onSquare() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        display = formatNumber(value * value)
        newNumber = true
    }

    fun onReciprocal() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        if (value == 0.0) {
            display = "Error"
        } else {
            display = formatNumber(1 / value)
        }
        newNumber = true
    }

    fun onSin() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        display = formatNumber(sin(Math.toRadians(value)))
        newNumber = true
    }

    fun onCos() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        display = formatNumber(cos(Math.toRadians(value)))
        newNumber = true
    }

    fun onTan() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        display = formatNumber(tan(Math.toRadians(value)))
        newNumber = true
    }

    fun onLog() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        if (value <= 0) {
            display = "Error"
        } else {
            display = formatNumber(log10(value))
        }
        newNumber = true
    }

    fun onLn() {
        if (display == "Error") return
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        if (value <= 0) {
            display = "Error"
        } else {
            display = formatNumber(ln(value))
        }
        newNumber = true
    }

    fun onPi() {
        display = formatNumber(PI)
        newNumber = true
    }

    fun onE() {
        display = formatNumber(E)
        newNumber = true
    }

    fun onMemoryAdd() {
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        memory += value
    }

    fun onMemorySubtract() {
        val value = display.replace(",", "").toDoubleOrNull() ?: return
        memory -= value
    }

    fun onMemoryRecall() {
        display = formatNumber(memory)
        newNumber = true
    }

    fun onMemoryClear() {
        memory = 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = CalcNavy),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    CalcOrange.copy(alpha = 0.3f),
                                    CalcBlue.copy(alpha = 0.2f),
                                    CalcEmerald.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(CalcOrange, CalcEmerald)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Calculator",
                                    color = CalcSoftWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    if (showScientific) "Scientific Mode" else "Standard Mode",
                                    color = CalcSoftWhite.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Toggle Scientific Mode
                            IconButton(
                                onClick = { showScientific = !showScientific },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (showScientific) CalcEmerald.copy(alpha = 0.3f)
                                        else CalcSoftWhite.copy(alpha = 0.1f)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Functions,
                                    contentDescription = "Scientific",
                                    tint = if (showScientific) CalcEmerald else CalcSoftWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CalcSoftWhite.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = CalcSoftWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CalcNavyLight)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Expression
                    if (expression.isNotEmpty()) {
                        Text(
                            text = expression,
                            color = CalcSoftWhite.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.End
                        )
                    }

                    // Current Value
                    Text(
                        text = display,
                        color = CalcSoftWhite,
                        fontSize = if (display.length > 12) 28.sp else 40.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )

                    // Memory indicator
                    if (memory != 0.0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = CalcEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "M",
                                color = CalcEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Scientific Functions (Expandable)
                AnimatedVisibility(
                    visible = showScientific,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Memory Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CalcSmallButton("MC", CalcBlue, Modifier.weight(1f)) { onMemoryClear() }
                            CalcSmallButton("MR", CalcBlue, Modifier.weight(1f)) { onMemoryRecall() }
                            CalcSmallButton("M+", CalcBlue, Modifier.weight(1f)) { onMemoryAdd() }
                            CalcSmallButton("M-", CalcBlue, Modifier.weight(1f)) { onMemorySubtract() }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Scientific Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CalcSmallButton("sin", CalcEmerald, Modifier.weight(1f)) { onSin() }
                            CalcSmallButton("cos", CalcEmerald, Modifier.weight(1f)) { onCos() }
                            CalcSmallButton("tan", CalcEmerald, Modifier.weight(1f)) { onTan() }
                            CalcSmallButton("π", CalcEmerald, Modifier.weight(1f)) { onPi() }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Scientific Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CalcSmallButton("log", CalcEmerald, Modifier.weight(1f)) { onLog() }
                            CalcSmallButton("ln", CalcEmerald, Modifier.weight(1f)) { onLn() }
                            CalcSmallButton("x²", CalcEmerald, Modifier.weight(1f)) { onSquare() }
                            CalcSmallButton("e", CalcEmerald, Modifier.weight(1f)) { onE() }
                        }
                    }
                }

                // Main Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Clear buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcButton("C", CalcRed, Modifier.weight(1f)) { onClear() }
                        CalcButton("CE", CalcRed.copy(alpha = 0.7f), Modifier.weight(1f)) { onClearEntry() }
                        CalcButton("⌫", CalcNavyLight, Modifier.weight(1f)) { onBackspace() }
                        CalcButton("÷", CalcOrange, Modifier.weight(1f)) { onOperatorClick("÷") }
                    }

                    // Row 2: 7-9
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcButton("7", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("7") }
                        CalcButton("8", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("8") }
                        CalcButton("9", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("9") }
                        CalcButton("×", CalcOrange, Modifier.weight(1f)) { onOperatorClick("×") }
                    }

                    // Row 3: 4-6
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcButton("4", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("4") }
                        CalcButton("5", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("5") }
                        CalcButton("6", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("6") }
                        CalcButton("-", CalcOrange, Modifier.weight(1f)) { onOperatorClick("-") }
                    }

                    // Row 4: 1-3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcButton("1", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("1") }
                        CalcButton("2", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("2") }
                        CalcButton("3", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("3") }
                        CalcButton("+", CalcOrange, Modifier.weight(1f)) { onOperatorClick("+") }
                    }

                    // Row 5: 0, decimal, equals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcButton("±", CalcNavyLight, Modifier.weight(1f)) { onNegate() }
                        CalcButton("0", CalcNavyLight, Modifier.weight(1f)) { onNumberClick("0") }
                        CalcButton(".", CalcNavyLight, Modifier.weight(1f)) { onNumberClick(".") }
                        CalcButton("=", CalcEmerald, Modifier.weight(1f)) { calculate() }
                    }

                    // Row 6: Extra functions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcButton("%", CalcNavyLight, Modifier.weight(1f)) { onPercent() }
                        CalcButton("√", CalcNavyLight, Modifier.weight(1f)) { onSquareRoot() }
                        CalcButton("1/x", CalcNavyLight, Modifier.weight(1f)) { onReciprocal() }
                        CalcButton("xʸ", CalcNavyLight, Modifier.weight(1f)) { onOperatorClick("^") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(
            text = text,
            color = CalcSoftWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CalcSmallButton(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CalcNavyLight.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

