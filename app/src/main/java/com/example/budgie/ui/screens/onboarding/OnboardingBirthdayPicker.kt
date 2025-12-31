package com.example.budgie.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import java.util.Calendar

/**
 * Responsive Smart Birthday Picker component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveSmartBirthdayPicker(
    birthday: String,
    onBirthdayChange: (String) -> Unit,
    birthdayError: String?,
    onOpenCalendar: () -> Unit,
    dimens: ResponsiveDimens,
    screenSize: ScreenSize
) {
    val initialParts = remember { birthday.split("/") }
    var year by remember { mutableStateOf(initialParts.getOrNull(0) ?: "") }
    var month by remember { mutableStateOf(initialParts.getOrNull(1) ?: "") }
    var day by remember { mutableStateOf(initialParts.getOrNull(2) ?: "") }

    var showMonthDropdown by remember { mutableStateOf(false) }

    // Revalidate day when month or year changes
    LaunchedEffect(month, year) {
        if (day.isNotEmpty() && month.isNotEmpty()) {
            val dayNum = day.toIntOrNull() ?: 0
            val yearNum = year.toIntOrNull() ?: 2023
            val maxDays = getMaxDaysInMonth(month.toIntOrNull() ?: 1, yearNum)
            if (dayNum > maxDays) {
                day = maxDays.toString().padStart(2, '0')
            }
        }
    }

    // Update combined birthday when parts change
    LaunchedEffect(year, month, day) {
        val formatted = buildString {
            if (year.isNotEmpty()) {
                append(year)
            }
            if (year.length == 4 && month.isNotEmpty()) {
                append("/$month")
            }
            if (year.length == 4 && month.length == 2 && day.isNotEmpty()) {
                val paddedDay = day.padStart(2, '0')
                append("/$paddedDay")
            }
        }
        if (formatted != birthday) {
            onBirthdayChange(formatted)
        }
    }

    // Responsive sizing
    val inputHeight = if (screenSize == ScreenSize.COMPACT) 44.dp else 50.dp
    val headerIconSize = if (screenSize == ScreenSize.COMPACT) 32.dp else 40.dp
    val innerIconSize = if (screenSize == ScreenSize.COMPACT) 18.dp else 22.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = 0.15f),
                        Color(0xFF10B981).copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF10B981).copy(alpha = 0.3f),
                        Color(0xFF10B981).copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(dimens.cornerRadius)
            )
            .padding(dimens.cardPadding - 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(headerIconSize)
                        .clip(RoundedCornerShape(if (screenSize == ScreenSize.COMPACT) 8.dp else 12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cake,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(innerIconSize)
                    )
                }

                Spacer(modifier = Modifier.width(if (screenSize == ScreenSize.COMPACT) 8.dp else 12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date of Birth",
                        fontSize = if (screenSize == ScreenSize.COMPACT) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Must be 18+",
                        fontSize = if (screenSize == ScreenSize.COMPACT) 10.sp else 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Calendar picker button
                IconButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier
                        .size(headerIconSize)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Open calendar",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(innerIconSize)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (screenSize == ScreenSize.COMPACT) 10.dp else 16.dp))

            // Date input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (screenSize == ScreenSize.COMPACT) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Input
                DateInputField(
                    value = year,
                    onValueChange = { newYear ->
                        if (newYear.isEmpty()) {
                            year = ""
                            return@DateInputField
                        }
                        if (newYear.all { it.isDigit() } && newYear.length <= 4) {
                            if (newYear.length == 4) {
                                val yearNum = newYear.toIntOrNull() ?: 0
                                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                if (yearNum in 1900..currentYear) {
                                    year = newYear
                                }
                            } else {
                                year = newYear
                            }
                        }
                    },
                    placeholder = "YYYY",
                    label = "Year",
                    isComplete = year.length == 4,
                    inputHeight = inputHeight,
                    screenSize = screenSize,
                    modifier = Modifier.weight(1.2f)
                )

                // Divider
                Text(
                    "/",
                    fontSize = if (screenSize == ScreenSize.COMPACT) 18.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                // Month Selector
                MonthSelector(
                    month = month,
                    onMonthChange = { month = it },
                    showDropdown = showMonthDropdown,
                    onDropdownToggle = { showMonthDropdown = it },
                    inputHeight = inputHeight,
                    screenSize = screenSize,
                    modifier = Modifier.weight(1f)
                )

                // Divider
                Text(
                    "/",
                    fontSize = if (screenSize == ScreenSize.COMPACT) 18.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                // Day Input
                DateInputField(
                    value = day,
                    onValueChange = { newDay ->
                        if (newDay.isEmpty()) {
                            day = ""
                            return@DateInputField
                        }
                        if (newDay.all { it.isDigit() } && newDay.length <= 2) {
                            val maxDays = if (year.length == 4 && month.isNotEmpty()) {
                                getMaxDaysInMonth(month.toIntOrNull() ?: 1, year.toIntOrNull() ?: 2024)
                            } else {
                                31
                            }
                            val dayNum = newDay.toIntOrNull() ?: 0
                            when (newDay.length) {
                                1 -> day = newDay
                                2 -> if (dayNum in 1..maxDays) day = newDay
                            }
                        }
                    },
                    placeholder = "DD",
                    label = "Day",
                    isComplete = day.isNotEmpty(),
                    inputHeight = inputHeight,
                    screenSize = screenSize,
                    modifier = Modifier.weight(1f)
                )
            }

            // Status message
            BirthdayStatusMessage(
                birthdayError = birthdayError,
                year = year,
                month = month,
                day = day,
                screenSize = screenSize
            )
        }
    }
}

@Composable
private fun DateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    isComplete: Boolean,
    inputHeight: androidx.compose.ui.unit.Dp,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(inputHeight)
                .clip(RoundedCornerShape(if (screenSize == ScreenSize.COMPACT) 8.dp else 12.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    1.dp,
                    if (isComplete) Color(0xFF10B981) else Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(if (screenSize == ScreenSize.COMPACT) 8.dp else 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = if (screenSize == ScreenSize.COMPACT) 14.sp else 16.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = if (screenSize == ScreenSize.COMPACT) 14.sp else 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        Text(
            label,
            fontSize = if (screenSize == ScreenSize.COMPACT) 9.sp else 10.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun MonthSelector(
    month: String,
    onMonthChange: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: (Boolean) -> Unit,
    inputHeight: androidx.compose.ui.unit.Dp,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(inputHeight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(if (screenSize == ScreenSize.COMPACT) 8.dp else 12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        if (month.isNotEmpty()) Color(0xFF10B981) else Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(if (screenSize == ScreenSize.COMPACT) 8.dp else 12.dp)
                    )
                    .clickable { onDropdownToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (month.isNotEmpty()) {
                            MONTHS.find { it.first == month }?.second ?: month
                        } else "MM",
                        fontSize = if (screenSize == ScreenSize.COMPACT) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (month.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(if (screenSize == ScreenSize.COMPACT) 16.dp else 20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { onDropdownToggle(false) },
                modifier = Modifier
                    .background(Color(0xFF0B1F2A))
                    .heightIn(max = 250.dp)
            ) {
                MONTHS.forEach { (num, name) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "$name ($num)",
                                fontSize = if (screenSize == ScreenSize.COMPACT) 12.sp else 14.sp,
                                fontWeight = if (month == num) FontWeight.Bold else FontWeight.Normal,
                                color = if (month == num) Color(0xFF10B981) else Color.White
                            )
                        },
                        onClick = {
                            onMonthChange(num)
                            onDropdownToggle(false)
                        },
                        leadingIcon = if (month == num) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.background(
                            if (month == num) Color(0xFF10B981).copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                    )
                }
            }
        }
        Text(
            "Month",
            fontSize = if (screenSize == ScreenSize.COMPACT) 9.sp else 10.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun BirthdayStatusMessage(
    birthdayError: String?,
    year: String,
    month: String,
    day: String,
    screenSize: ScreenSize
) {
    if (birthdayError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE53935).copy(alpha = 0.1f))
                .padding(6.dp)
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = birthdayError,
                color = Color(0xFFE53935),
                fontSize = if (screenSize == ScreenSize.COMPACT) 10.sp else 11.sp
            )
        }
    } else if (year.length == 4 && month.isNotEmpty() && day.length == 2) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.1f))
                .padding(6.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Birthday: ${MONTHS.find { it.first == month }?.second} $day, $year",
                color = Color(0xFF10B981),
                fontSize = if (screenSize == ScreenSize.COMPACT) 10.sp else 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

