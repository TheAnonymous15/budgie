package com.example.budgie.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Intelligent date formatter that:
 * - Auto-inserts separators after YYYY and MM
 * - Validates month (max 12)
 * - Validates day based on month (28/29/30/31)
 * - Handles leap years for February
 */
private fun formatDateInput(newValue: String, oldValue: String): String {
    // Remove all non-digit characters
    val digitsOnly = newValue.filter { it.isDigit() }

    // Build formatted string
    val formatted = StringBuilder()

    for (i in digitsOnly.indices) {
        when (i) {
            4, 6 -> formatted.append('/') // Add separator after year and month
        }
        formatted.append(digitsOnly[i])
    }

    val result = formatted.toString()

    // Validate as we type
    val parts = result.split('/')

    // Validate month (if entered)
    if (parts.size >= 2 && parts[1].length >= 2) {
        val month = parts[1].take(2).toIntOrNull() ?: 0
        if (month > 12 || month == 0) {
            return oldValue // Invalid month, keep old value
        }
    }

    // Validate day (if entered)
    if (parts.size == 3 && parts[0].length == 4 && parts[1].length == 2) {
        val year = parts[0].toIntOrNull() ?: 0
        val month = parts[1].toIntOrNull() ?: 0
        val dayInput = parts[2]

        if (dayInput.isNotEmpty()) {
            val day = dayInput.toIntOrNull() ?: 0
            val maxDays = getMaxDaysInMonth(month, year)

            // If day exceeds max for the month, don't allow it
            if (day > maxDays) {
                return oldValue
            }
        }
    }

    return result
}

/**
 * Get maximum days in a month, accounting for leap years
 */
private fun getMaxDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31 // Jan, Mar, May, Jul, Aug, Oct, Dec
        4, 6, 9, 11 -> 30 // Apr, Jun, Sep, Nov
        2 -> if (isLeapYear(year)) 29 else 28 // Feb
        else -> 31
    }
}

/**
 * Check if a year is a leap year
 */
private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

/**
 * PIN Strength Analysis Result
 */
data class PinStrengthResult(
    val strength: PinStrength,
    val score: Int, // 0-100
    val warnings: List<String>,
    val suggestion: String?
)

enum class PinStrength {
    VERY_WEAK,
    WEAK,
    MODERATE,
    STRONG
}

/**
 * Analyzes PIN strength and detects common weak patterns
 *
 * Checks for:
 * - Sequential numbers (12345, 54321)
 * - Repeated digits (11111, 00000)
 * - Common PINs (12345, 00000, 11111, etc.)
 * - Keyboard patterns
 * - Date patterns (birth years, etc.)
 * - Repeating pairs (12121, 12312)
 */
private fun analyzePinStrength(pin: String): PinStrengthResult {
    if (pin.length < 5) {
        return PinStrengthResult(
            strength = PinStrength.VERY_WEAK,
            score = 0,
            warnings = listOf("PIN must be 5 digits"),
            suggestion = null
        )
    }

    val warnings = mutableListOf<String>()
    var score = 100

    // Check for common weak PINs
    val commonWeakPins = setOf(
        "12345", "54321", "11111", "22222", "33333", "44444",
        "55555", "66666", "77777", "88888", "99999", "00000",
        "12321", "11211", "12121", "11112", "11122", "12222",
        "01234", "43210", "98765", "56789", "67890",
        "13579", "24680", "11223", "12123", "21212",
        "10101", "20202", "10001", "12312", "32123",
        "11234", "12344", "12340", "01230", "98760"
    )

    if (pin in commonWeakPins) {
        warnings.add("This is a commonly used PIN")
        score -= 50
    }

    // Check for all same digits (11111, 00000)
    if (pin.all { it == pin[0] }) {
        warnings.add("All digits are the same")
        score -= 40
    }

    // Check for sequential ascending (12345, 23456)
    val isAscending = pin.zipWithNext().all { (a, b) -> b.digitToInt() - a.digitToInt() == 1 }
    if (isAscending) {
        warnings.add("Sequential ascending pattern")
        score -= 35
    }

    // Check for sequential descending (54321, 98765)
    val isDescending = pin.zipWithNext().all { (a, b) -> a.digitToInt() - b.digitToInt() == 1 }
    if (isDescending) {
        warnings.add("Sequential descending pattern")
        score -= 35
    }

    // Check for repeating pairs (12121, 34343)
    val hasTwoDigitRepeat = pin.length >= 4 &&
        (pin.substring(0, 2) == pin.substring(2, 4) ||
         (pin.length >= 5 && pin.substring(0, 2) == pin.substring(3, 5)))
    if (hasTwoDigitRepeat) {
        warnings.add("Repeating pattern detected")
        score -= 25
    }

    // Check for only 2 unique digits
    val uniqueDigits = pin.toSet().size
    if (uniqueDigits <= 2) {
        warnings.add("Only $uniqueDigits unique digit${if (uniqueDigits == 1) "" else "s"}")
        score -= 20
    }

    // Check for potential birth year patterns (19XX, 20XX)
    if (pin.startsWith("19") || pin.startsWith("20")) {
        warnings.add("Looks like a year - avoid personal dates")
        score -= 15
    }

    // Check for keyboard row patterns
    val keyboardPatterns = setOf("13579", "24680", "02468", "97531")
    if (pin in keyboardPatterns) {
        warnings.add("Keyboard pattern detected")
        score -= 30
    }

    // Bonus for good variety
    if (uniqueDigits >= 4) {
        score += 10
    }
    if (uniqueDigits == 5) {
        score += 5
    }

    // Ensure score is within bounds
    score = score.coerceIn(0, 100)

    // Determine strength level
    val strength = when {
        score >= 80 -> PinStrength.STRONG
        score >= 60 -> PinStrength.MODERATE
        score >= 40 -> PinStrength.WEAK
        else -> PinStrength.VERY_WEAK
    }

    // Generate suggestion
    val suggestion = when (strength) {
        PinStrength.VERY_WEAK -> "Please choose a stronger PIN with varied digits"
        PinStrength.WEAK -> "Consider using more varied digits for better security"
        PinStrength.MODERATE -> "Good, but could be stronger with more variety"
        PinStrength.STRONG -> null
    }

    return PinStrengthResult(
        strength = strength,
        score = score,
        warnings = warnings,
        suggestion = suggestion
    )
}

/**
 * Get color for PIN strength indicator
 */
private fun getPinStrengthColor(strength: PinStrength): Color {
    return when (strength) {
        PinStrength.VERY_WEAK -> Color(0xFFEF4444) // Red
        PinStrength.WEAK -> Color(0xFFF97316) // Orange
        PinStrength.MODERATE -> Color(0xFFEAB308) // Yellow/Amber
        PinStrength.STRONG -> Color(0xFF10B981) // Green
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartBirthdayPicker(
    birthday: String,
    onBirthdayChange: (String) -> Unit,
    birthdayError: String?,
    onOpenCalendar: () -> Unit
) {
    // Parse existing birthday only on first composition
    val initialParts = remember { birthday.split("/") }
    var year by remember { mutableStateOf(initialParts.getOrNull(0) ?: "") }
    var month by remember { mutableStateOf(initialParts.getOrNull(1) ?: "") }
    var day by remember { mutableStateOf(initialParts.getOrNull(2) ?: "") }

    var showMonthDropdown by remember { mutableStateOf(false) }

    // Month names
    val months = listOf(
        "01" to "Jan", "02" to "Feb", "03" to "Mar",
        "04" to "Apr", "05" to "May", "06" to "Jun",
        "07" to "Jul", "08" to "Aug", "09" to "Sep",
        "10" to "Oct", "11" to "Nov", "12" to "Dec"
    )

    // Revalidate day when month or year changes
    LaunchedEffect(month, year) {
        if (day.isNotEmpty() && month.isNotEmpty()) {
            val dayNum = day.toIntOrNull() ?: 0
            // Use current year or default to non-leap year for validation
            val yearNum = year.toIntOrNull() ?: 2023
            val maxDays = getMaxDaysInMonth(month.toIntOrNull() ?: 1, yearNum)
            if (dayNum > maxDays) {
                // Reset day if it's now invalid for the selected month
                day = maxDays.toString().padStart(2, '0')
            }
        }
    }

    // Update combined birthday when parts change
    LaunchedEffect(year, month, day) {
        val formatted = buildString {
            // Only include year if it has content
            if (year.isNotEmpty()) {
                append(year)
            }
            // Only include month if year is complete (4 digits) and month has content
            if (year.length == 4 && month.isNotEmpty()) {
                append("/$month")
            }
            // Only include day if year and month are complete
            if (year.length == 4 && month.length == 2 && day.isNotEmpty()) {
                val paddedDay = day.padStart(2, '0')
                append("/$paddedDay")
            }
        }
        if (formatted != birthday) {
            onBirthdayChange(formatted)
        }
    }

    // Premium glassmorphic card for birthday
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cake,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date of Birth",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Must be 18 years or older",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Calendar picker button
                IconButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Open calendar",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date input row - modern pill design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Input - Pill style
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                if (year.length == 4) Color(0xFF10B981) else Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = year,
                            onValueChange = { newYear ->
                                if (newYear.isEmpty()) {
                                    year = ""
                                    return@BasicTextField
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
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (year.isEmpty()) {
                                        Text(
                                            "YYYY",
                                            color = Color.White.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    Text(
                        "Year",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Divider
                Text(
                    "/",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                // Month selector - Dropdown style
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(
                                    1.dp,
                                    if (month.isNotEmpty()) Color(0xFF10B981) else Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { showMonthDropdown = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (month.isNotEmpty()) {
                                        months.find { it.first == month }?.second ?: month
                                    } else "MM",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (month.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Month Dropdown - with safe dismiss
                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false },
                            modifier = Modifier
                                .background(Color(0xFF0B1F2A))
                                .heightIn(max = 250.dp)
                        ) {
                            months.forEach { (num, name) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$name ($num)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (month == num) FontWeight.Bold else FontWeight.Normal,
                                            color = if (month == num) Color(0xFF10B981) else Color.White
                                        )
                                    },
                                    onClick = {
                                        month = num
                                        showMonthDropdown = false
                                    },
                                    leadingIcon = if (month == num) {
                                        {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(18.dp)
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
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Divider
                Text(
                    "/",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                // Day Input - Pill style
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                if (day.isNotEmpty()) Color(0xFF10B981) else Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = day,
                            onValueChange = { newDay ->
                                if (newDay.isEmpty()) {
                                    day = ""
                                    return@BasicTextField
                                }
                                // Only allow digits, max 2 characters
                                if (newDay.all { it.isDigit() } && newDay.length <= 2) {
                                    val maxDays = if (year.length == 4 && month.isNotEmpty()) {
                                        getMaxDaysInMonth(month.toIntOrNull() ?: 1, year.toIntOrNull() ?: 2024)
                                    } else {
                                        31
                                    }
                                    val dayNum = newDay.toIntOrNull() ?: 0

                                    when (newDay.length) {
                                        1 -> {
                                            // Single digit: allow 0-9
                                            // 0 = user is typing 01-09
                                            // 1-9 = valid single digit day (will be padded to 0X)
                                            day = newDay
                                        }
                                        2 -> {
                                            // Two digits: must be 01-maxDays
                                            if (dayNum in 1..maxDays) {
                                                day = newDay
                                            }
                                        }
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (day.isEmpty()) {
                                        Text(
                                            "DD",
                                            color = Color.White.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    Text(
                        "Day",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Error or help text
            if (birthdayError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = birthdayError,
                        color = Color(0xFFE53935),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else if (year.length == 4 && month.isNotEmpty() && day.length == 2) {
                // Show formatted date when complete
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Birthday: ${months.find { it.first == month }?.second} $day, $year",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecuritySelectionSection(
    securityType: String,
    onSecurityTypeChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    pinError: String?,
    onPinErrorChange: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))



            Text(
                text = "Step 2 of 2 - Protect your finances",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF0FAE96)
            )
        }

        // PIN + Biometrics Card (Recommended)
        SecurityOptionCard(
            isSelected = securityType == "pin_biometric",
            onClick = { onSecurityTypeChange("pin_biometric") },
            icon = Icons.Filled.Fingerprint,
            secondaryIcon = Icons.Filled.Pin,
            title = "Biometrics + PIN",
            subtitle = "Quick unlock with secure backup",
            isRecommended = true,
            accentColor = Color(0xFF10B981)
        )

        // PIN Only Card
        SecurityOptionCard(
            isSelected = securityType == "pin",
            onClick = { onSecurityTypeChange("pin") },
            icon = Icons.Filled.Lock,
            title = "PIN Only",
            subtitle = "Secure 5-digit code",
            isRecommended = false,
            accentColor = Color(0xFF3B82F6)
        )

        // PIN Entry Field (when applicable)
        if (securityType == "pin" || securityType == "pin_biometric") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0B1F2A).copy(alpha = 0.7f) // Midnight blue glassmorphic
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0FAE96).copy(alpha = 0.5f),
                            Color(0xFF0FAE96).copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create your PIN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6F1F0) // Soft white
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a 5-digit PIN you'll remember",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE6F1F0).copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Analyze PIN strength
                    val pinStrength = remember(pin) {
                        if (pin.length == 5) analyzePinStrength(pin) else null
                    }

                    // PIN Input Field
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { newPin ->
                            if (newPin.all { it.isDigit() } && newPin.length <= 5) {
                                onPinChange(newPin)
                                onPinErrorChange(null)
                            }
                        },
                        label = { Text("Enter PIN", color = Color(0xFFE6F1F0).copy(alpha = 0.8f)) },
                        placeholder = { Text("5 digits", color = Color(0xFFE6F1F0).copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (pinStrength != null)
                                getPinStrengthColor(pinStrength.strength) else Color(0xFF0FAE96),
                            unfocusedBorderColor = Color(0xFFE6F1F0).copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFF0FAE96),
                            cursorColor = Color(0xFF0FAE96),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (pin.length == 5) {
                                Icon(
                                    imageVector = if (pinStrength?.strength == PinStrength.STRONG)
                                        Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = getPinStrengthColor(pinStrength?.strength ?: PinStrength.MODERATE)
                                )
                            }
                        }
                    )

                    // PIN Strength Indicator
                    if (pin.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Strength bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val strengthLevel = when {
                                pin.length < 5 -> 0
                                pinStrength?.strength == PinStrength.VERY_WEAK -> 1
                                pinStrength?.strength == PinStrength.WEAK -> 2
                                pinStrength?.strength == PinStrength.MODERATE -> 3
                                pinStrength?.strength == PinStrength.STRONG -> 4
                                else -> 0
                            }

                            repeat(4) { index ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (index < strengthLevel) {
                                                when (strengthLevel) {
                                                    1 -> Color(0xFFEF4444) // Red
                                                    2 -> Color(0xFFF97316) // Orange
                                                    3 -> Color(0xFFEAB308) // Yellow
                                                    4 -> Color(0xFF10B981) // Green
                                                    else -> Color.Gray.copy(alpha = 0.3f)
                                                }
                                            } else {
                                                Color.White.copy(alpha = 0.1f)
                                            }
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Strength label and warnings
                        if (pin.length == 5 && pinStrength != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (pinStrength.strength) {
                                        PinStrength.VERY_WEAK -> "Very Weak"
                                        PinStrength.WEAK -> "Weak"
                                        PinStrength.MODERATE -> "Moderate"
                                        PinStrength.STRONG -> "Strong"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = getPinStrengthColor(pinStrength.strength)
                                )
                                Text(
                                    text = "${pinStrength.score}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE6F1F0).copy(alpha = 0.6f)
                                )
                            }

                            // Show warnings for weak PINs
                            if (pinStrength.warnings.isNotEmpty() && pinStrength.strength != PinStrength.STRONG) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = getPinStrengthColor(pinStrength.strength).copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = getPinStrengthColor(pinStrength.strength),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (pinStrength.strength == PinStrength.VERY_WEAK)
                                                    "Weak PIN detected!" else "PIN could be stronger",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = getPinStrengthColor(pinStrength.strength)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Show first 2 warnings max
                                        pinStrength.warnings.take(2).forEach { warning ->
                                            Text(
                                                text = "• $warning",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFE6F1F0).copy(alpha = 0.7f)
                                            )
                                        }

                                        if (pinStrength.suggestion != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = pinStrength.suggestion,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFE6F1F0).copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (pin.length < 5) {
                            Text(
                                text = "Enter ${5 - pin.length} more digit${if (5 - pin.length > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE6F1F0).copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm PIN Field
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { newConfirmPin ->
                            if (newConfirmPin.all { it.isDigit() } && newConfirmPin.length <= 5) {
                                onConfirmPinChange(newConfirmPin)
                                if (newConfirmPin.length == 5 && pin.length == 5) {
                                    if (newConfirmPin != pin) {
                                        onPinErrorChange("PINs do not match")
                                    } else {
                                        onPinErrorChange(null)
                                    }
                                }
                            }
                        },
                        label = { Text("Confirm PIN", color = Color(0xFFE6F1F0).copy(alpha = 0.8f)) },
                        placeholder = { Text("Re-enter PIN", color = Color(0xFFE6F1F0).copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = pin.length == 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (confirmPin.length == 5 && confirmPin == pin)
                                Color(0xFF0FAE96) else Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFFE6F1F0).copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFF0FAE96),
                            cursorColor = Color(0xFF0FAE96),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledBorderColor = Color(0xFFE6F1F0).copy(alpha = 0.2f),
                            disabledTextColor = Color(0xFFE6F1F0).copy(alpha = 0.5f),
                            disabledLabelColor = Color(0xFFE6F1F0).copy(alpha = 0.5f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (confirmPin.length == 5 && confirmPin == pin) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF0FAE96)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status indicator
                    val finalPinStrength = remember(pin) {
                        if (pin.length == 5) analyzePinStrength(pin) else null
                    }

                    when {
                        pin.length == 5 && confirmPin.length == 5 && pin == confirmPin -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF0FAE96),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "PIN set successfully!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF0FAE96),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Show warning if weak PIN is confirmed
                                if (finalPinStrength != null &&
                                    (finalPinStrength.strength == PinStrength.VERY_WEAK ||
                                     finalPinStrength.strength == PinStrength.WEAK)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFF97316),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Consider using a stronger PIN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF97316)
                                        )
                                    }
                                }
                            }
                        }
                        pinError != null -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = pinError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                        pin.length < 5 -> {
                            Text(
                                text = "Enter ${5 - pin.length} more digit${if (5 - pin.length > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE6F1F0).copy(alpha = 0.6f)
                            )
                        }
                        confirmPin.length < 5 -> {
                            Text(
                                text = "Now confirm your PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0FAE96)
                            )
                        }
                    }
                }
            }
        }

        // Skip Security (subtle warning)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSecurityTypeChange("none") }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (securityType == "none") {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFE6F1F0).copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = "Set up later",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE6F1F0).copy(alpha = 0.5f),
                fontWeight = if (securityType == "none") FontWeight.SemiBold else FontWeight.Normal
            )
            if (securityType != "none") {
                Text(
                    text = " (not recommended)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE6F1F0).copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun SecurityOptionCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    secondaryIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    title: String,
    subtitle: String,
    isRecommended: Boolean,
    accentColor: Color
) {
    val cardColor = if (isSelected) {
        Color(0xFF0B1F2A).copy(alpha = 0.8f) // Midnight blue
    } else {
        Color(0xFF0B1F2A).copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0FAE96), Color(0xFF0B8F7A))
                )
            )
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6F1F0).copy(alpha = 0.2f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSelected) Color(0xFF0FAE96).copy(alpha = 0.2f) else Color(0xFFE6F1F0).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (secondaryIcon != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            imageVector = secondaryIcon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0).copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0)
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "★ Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0B1F2A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFC9A14A), Color(0xFFFFD166))
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE6F1F0).copy(alpha = 0.6f)
                )
            }

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0).copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinDot(
    isFilled: Boolean,
    hasError: Boolean
) {
    val color = when {
        hasError -> Color(0xFFE53935)
        isFilled -> Color(0xFF10B981)
        else -> Color(0xFFE5E7EB)
    }

    Box(
        modifier = Modifier
            .size(16.dp)
            .background(color, CircleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (name: String, birthday: String, securityType: String, pin: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var birthdayError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showUnderageDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Security states
    var securityType by remember { mutableStateOf("none") } // none, pin, pin_biometric
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Premium gradient background - Midnight Blue to Deep Teal
    val premiumGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B1F2A), // Midnight blue
            Color(0xFF0D2832), // Dark teal
            Color(0xFF0B1F2A)  // Back to midnight
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = premiumGradient)
    ) {
        // Animated Background Elements
        PremiumAnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // Glowing Logo Container with pulse animation
            val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0FAE96).copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // App Logo with premium border
                Image(
                    painter = painterResource(R.drawable.icon),
                    contentDescription = "Budgie Logo",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0FAE96),
                                    Color(0xFF0B8F7A),
                                    Color(0xFF0FAE96)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Title - No emoji, clean authority
            Text(
                text = "Budgie",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6F1F0), // Soft white
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle - AI-powered positioning
            Text(
                text = "AI-Powered Personal Finance Advisor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0FAE96), // Deep emerald/teal
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Glassmorphic feature chips - outcome focused
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassmorphicChip("💰 Track")
                GlassmorphicChip("🧠 Insights")
                GlassmorphicChip("📈 Grow")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GLASSMORPHISM CARD - Main Input Card
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Color(0xFF0FAE96).copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = Color(0xFF0FAE96),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Step 1 of 2 - Personal setup",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF0FAE96)
                            )
//                            Text(
//                                text = "Personal Setup",
//                                style = MaterialTheme.typography.titleMedium,
//                                fontWeight = FontWeight.Bold,
//                                color = Color(0xFFE6F1F0)
//                            )


                        }
                    }

                    // Name Input - Glassmorphic style with better placeholder
                    GlassmorphicTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = "By what name should I refer you?",
                        placeholder = "Name or nickname is okay",
                        leadingIcon = Icons.Filled.Person,
                        isError = nameError != null,
                        errorMessage = nameError
                    )

                    // Smart Birthday Picker
                    SmartBirthdayPicker(
                        birthday = birthday,
                        onBirthdayChange = {
                            birthday = it
                            birthdayError = null
                        },
                        birthdayError = birthdayError,
                        onOpenCalendar = { showDatePicker = true }
                    )

                    // Why I need this info - tooltip style
                    var showWhyDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWhyDialog = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Why do I need your accurate birthday?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF60A5FA),
                            fontWeight = FontWeight.Medium
                        )
                    }






                    // Why Dialog
                    if (showWhyDialog) {
                        AlertDialog(
                            onDismissRequest = { showWhyDialog = false },
                            containerColor = Color(0xFF1B263B),
                            shape = RoundedCornerShape(24.dp),
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            Color(0xFF10B981).copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Personalizing Your Experience",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {

                                    Text(
                                        text = "A little information helps Budgie deliver smarter and more relevant financial guidance.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )

                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.08f),
                                        thickness = 1.dp
                                    )

                                    WhyBenefitItem(
                                        icon = Icons.Filled.Person,
                                        text = "Insights personalized to your financial profile"
                                    )

                                    WhyBenefitItem(
                                        icon = Icons.Filled.Cake,
                                        text = "Helpful birthday reminders and milestones"
                                    )

                                    WhyBenefitItem(
                                        icon = Icons.Filled.Psychology,
                                        text = "Advice aligned with your life stage"
                                    )

                                    WhyBenefitItem(
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        text = "Tailored wealth-building strategies"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF10B981).copy(alpha = 0.08f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Lightbulb,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Accurate details lead to better financial insights.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showWhyDialog = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Continue",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }









                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECURITY CARD - Glassmorphic
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    SecuritySelectionSection(
                        securityType = securityType,
                        onSecurityTypeChange = {
                            securityType = it
                            pin = ""
                            confirmPin = ""
                            pinError = null
                        },
                        pin = pin,
                        onPinChange = { pin = it },
                        confirmPin = confirmPin,
                        onConfirmPinChange = { confirmPin = it },
                        pinError = pinError,
                        onPinErrorChange = { pinError = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terms & Privacy Agreement
            TermsAndPrivacyAgreement(
                onTermsClick = { showTermsDialog = true },
                onPrivacyClick = { showPrivacyDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Button
            Button(
                onClick = {
                    var hasError = false

                    if (name.isBlank()) {
                        nameError = "Please enter your name"
                        hasError = true
                    }

                    if (birthday.isBlank()) {
                        birthdayError = "Please enter your birthday"
                        hasError = true
                    } else if (birthday.length < 10) {
                        birthdayError = "Please enter complete date (YYYY/MM/DD)"
                        hasError = true
                    } else {
                        try {
                            val normalizedDate = birthday.replace("/", "-")
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            dateFormat.isLenient = false
                            val date = dateFormat.parse(normalizedDate)
                            val today = Calendar.getInstance()
                            val birthCal = Calendar.getInstance()

                            if (date != null) {
                                birthCal.time = date

                                if (birthCal.after(today)) {
                                    birthdayError = "Birthday cannot be in the future"
                                    hasError = true
                                } else {
                                    val age18 = Calendar.getInstance()
                                    age18.add(Calendar.YEAR, -18)

                                    if (birthCal.after(age18)) {
                                        showUnderageDialog = true
                                        hasError = true
                                        return@Button
                                    }

                                    val age120 = Calendar.getInstance()
                                    age120.add(Calendar.YEAR, -120)
                                    if (birthCal.before(age120)) {
                                        birthdayError = "Please enter a valid birthday"
                                        hasError = true
                                    }
                                }
                            } else {
                                birthdayError = "Invalid date format"
                                hasError = true
                            }
                        } catch (_: Exception) {
                            birthdayError = "Invalid format. Use YYYY/MM/DD (e.g., 1995/06/15)"
                            hasError = true
                        }
                    }

                    if (securityType == "pin" || securityType == "pin_biometric") {
                        if (pin.length != 5) {
                            pinError = "Please enter a 5-digit PIN"
                            hasError = true
                        } else if (confirmPin.length != 5) {
                            pinError = "Please confirm your PIN"
                            hasError = true
                        } else if (pin != confirmPin) {
                            pinError = "PINs do not match"
                            hasError = true
                        }
                    }

                    if (!hasError) {
                        onComplete(name.trim(), birthday, securityType, pin)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B1F2A)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF0FAE96).copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0FAE96)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF0FAE96),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enhanced Privacy Footer - closer to CTA
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF0FAE96).copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = "Security",
                    tint = Color(0xFF0FAE96),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "100% Private · Stored on your device · Never shared",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE6F1F0),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }


        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Date(millis)
                                // Format as YYYY/MM/DD to match manual input format
                                birthday = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date)
                                birthdayError = null
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = true,
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = Color(0xFF10B981), // Emerald
                        todayDateBorderColor = Color(0xFF10B981)
                    )
                )
            }
        }

        // Underage Dialog (Below 18)
        if (showUnderageDialog) {
            AlertDialog(
                onDismissRequest = { showUnderageDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = "Age Requirement Not Met",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "You must be at least 18 years old to use Budgie.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Budgie is a financial management app designed for adults who can make independent financial decisions.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📚 In the meantime, consider learning about personal finance through age-appropriate resources!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUnderageDialog = false
                            birthday = ""
                            birthdayError = null
                        }
                    ) {
                        Text(
                            "I Understand",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Terms of Use Dialog
        if (showTermsDialog) {
            TermsOfUseDialog(onDismiss = { showTermsDialog = false })
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }
    }
}

// ============== GLASSMORPHIC COMPONENTS ==============








@Composable
private fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun GlassmorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = Color.White.copy(alpha = 0.8f)) },
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color(0xFF10B981),
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                cursorColor = Color(0xFF10B981),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                errorBorderColor = Color(0xFFEF4444),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
            ),
            singleLine = true
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun FeatureBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GlassmorphicChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE6F1F0),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun WhyBenefitItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun PremiumAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "premium_bg")

    // Floating orbs animations
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )

    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )

    val rotate1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate1"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Glowing orb - top left
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (100 + float1).dp)
                .size(200.dp)
                .scale(scale1)
                .alpha(0.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981),
                            Color(0xFF10B981).copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Glowing orb - top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (50 + float2).dp)
                .size(180.dp)
                .alpha(0.12f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFD700).copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Glowing orb - bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 30.dp, y = (-50 + float1 * -1).dp)
                .size(250.dp)
                .alpha(0.1f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF3B82F6).copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Floating financial icons
        Text(
            text = "💰",
            fontSize = 40.sp,
            modifier = Modifier
                .offset(x = 30.dp, y = (180 + float1).dp)
                .alpha(0.2f)
                .rotate(rotate1 * 0.1f)
        )

        Text(
            text = "📈",
            fontSize = 35.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = (220 + float2).dp)
                .alpha(0.18f)
        )

        Text(
            text = "💎",
            fontSize = 30.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 20.dp, y = (100 + float1 * -0.5f).dp)
                .alpha(0.15f)
                .scale(scale1 * 0.8f)
        )

        Text(
            text = "🏦",
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-25).dp, y = (-80 + float2).dp)
                .alpha(0.15f)
        )

        Text(
            text = "💵",
            fontSize = 38.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 50.dp, y = (-150 + float1).dp)
                .alpha(0.18f)
        )
    }
}

@Composable
private fun BenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
private fun TermsAndPrivacyAgreement(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "By continuing you agree to ",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
        Text(
            text = "Terms of Use",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onTermsClick)
        )
        Text(
            text = " and ",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onPrivacyClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsOfUseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terms of Use",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A472A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Last Updated: December 25, 2025",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE0F2FE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Please read these terms carefully before using Budgie. By clicking 'Start Building Wealth!' you acknowledge that you have read, understood, and agree to be bound by these Terms of Use.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0A1929)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "1. Acceptance of Terms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "By accessing or using Budgie (\"the App\"), you agree to be legally bound by these Terms of Use. These terms constitute a binding legal agreement between you and Budgie. If you do not agree to these terms in their entirety, you must not use the App.\n\nYour continued use of the App following any amendments to these Terms will constitute your acceptance of such amendments.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "2. Eligibility and Age Requirement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You must be at least 18 years of age to use Budgie. By using this App, you represent and warrant that:\n\n" +
                                    "• You are 18 years of age or older\n" +
                                    "• You have the legal capacity to enter into a binding agreement\n" +
                                    "• You are using the App for personal, non-commercial purposes\n" +
                                    "• All information you provide is accurate and truthful\n\n" +
                                    "We reserve the right to request proof of age and terminate accounts that do not meet this requirement.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "3. License to Use",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Subject to your compliance with these Terms, we grant you a limited, non-exclusive, non-transferable, revocable license to:\n\n" +
                                    "• Download and install the App on your personal device\n" +
                                    "• Access and use the App for personal financial management\n\n" +
                                    "This license does NOT permit you to:\n\n" +
                                    "• Modify, copy, or distribute the App\n" +
                                    "• Reverse engineer, decompile, or disassemble the App\n" +
                                    "• Create derivative works based on the App\n" +
                                    "• Use the App for commercial purposes\n" +
                                    "• Remove or alter any copyright notices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "4. Acceptable Use Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You agree to use Budgie only for lawful purposes and in accordance with these Terms. You agree NOT to:\n\n" +
                                    "• Violate any local, state, national, or international law\n" +
                                    "• Interfere with or disrupt the App's functionality\n" +
                                    "• Attempt to gain unauthorized access to the App\n" +
                                    "• Use automated systems to access the App\n" +
                                    "• Introduce viruses, malware, or harmful code\n" +
                                    "• Impersonate any person or entity\n" +
                                    "• Use the App to transmit false or misleading information",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "5. User Data and Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Data Collection: Budgie only collects your date of birth, which is stored locally on your device. No other personal data is collected or transmitted by Budgie.\n\n" +
                                    "Data Storage: All your financial data (expenses, income, bills, budgets) is stored exclusively on your device using local database storage.\n\n" +
                                    "No Cloud Storage: We do NOT sync your data to cloud servers. We do NOT have servers that receive or store your data.\n\n" +
                                    "Data Transmission - YOU ARE IN CONTROL:\n" +
                                    "• Budgie NEVER automatically transmits your data anywhere\n" +
                                    "• ONLY YOU can choose to export or share your data\n" +
                                    "• When you export reports (PDF, Excel), YOU control where they go\n" +
                                    "• You decide who receives your exported reports\n" +
                                    "• We have ZERO access to your exported files\n\n" +
                                    "Data Responsibility: You are solely responsible for:\n" +
                                    "• Maintaining the security of your device\n" +
                                    "• Backing up your data\n" +
                                    "• Protecting your data from unauthorized access\n" +
                                    "• Any data you choose to export and share with others\n\n" +
                                    "Please read our Privacy Policy for detailed information about data handling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "6. Financial Information Disclaimer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "IMPORTANT: Budgie provides general financial insights and suggestions based on your data. These are NOT professional financial advice.\n\n" +
                                    "• The App's insights are algorithmic and automated\n" +
                                    "• They do NOT constitute professional financial, investment, legal, or tax advice\n" +
                                    "• You should NOT make financial decisions based solely on the App's suggestions\n" +
                                    "• Always consult with qualified financial advisors before making significant financial decisions\n\n" +
                                    "We are NOT licensed financial advisors and do NOT provide personalized financial advice tailored to your specific circumstances.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "7. Accuracy of Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You acknowledge that:\n\n" +
                                    "• You are responsible for the accuracy of all data you enter\n" +
                                    "• The App's calculations are only as accurate as the data you provide\n" +
                                    "• We do NOT verify or validate your financial data\n" +
                                    "• Errors in data entry may lead to incorrect insights\n" +
                                    "• You should regularly review and verify your financial information",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "8. Intellectual Property Rights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All intellectual property rights in and to the App, including but not limited to:\n\n" +
                                    "• Software code and algorithms\n" +
                                    "• Design, graphics, and user interface\n" +
                                    "• Text, images, and content\n" +
                                    "• Trademarks and branding\n\n" +
                                    "are owned by or licensed to Budgie. You acknowledge that you have no rights to the App except for the limited license granted in these Terms.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "9. Limitation of Liability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "TO THE MAXIMUM EXTENT PERMITTED BY LAW:\n\n" +
                                    "• The App is provided \"AS IS\" and \"AS AVAILABLE\"\n" +
                                    "• We make NO warranties, express or implied\n" +
                                    "• We are NOT liable for any direct, indirect, incidental, special, consequential, or punitive damages\n" +
                                    "• This includes but is not limited to: financial losses, data loss, loss of profits, business interruption\n" +
                                    "• Our total liability shall not exceed KSh 1,000\n\n" +
                                    "Some jurisdictions do not allow limitations on liability, so these limitations may not apply to you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "10. Indemnification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You agree to indemnify, defend, and hold harmless Budgie, its developers, and affiliates from any claims, damages, losses, liabilities, and expenses (including legal fees) arising from:\n\n" +
                                    "• Your use or misuse of the App\n" +
                                    "• Your violation of these Terms\n" +
                                    "• Your violation of any rights of another party\n" +
                                    "• Any financial decisions you make based on App insights",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "11. Termination",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We reserve the right to:\n\n" +
                                    "• Suspend or terminate your access at any time\n" +
                                    "• Discontinue the App without notice\n" +
                                    "• Modify or remove features\n\n" +
                                    "You may terminate your use by uninstalling the App. Upon termination, all licenses granted to you will cease immediately.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "12. Changes to Terms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We reserve the right to modify these Terms at any time. When we make changes:\n\n" +
                                    "• We will update the \"Last Updated\" date\n" +
                                    "• Significant changes will be communicated via the App\n" +
                                    "• Your continued use constitutes acceptance of the new Terms\n\n" +
                                    "If you do not agree to the modified Terms, you must stop using the App.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "13. Governing Law",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "These Terms shall be governed by and construed in accordance with the laws of Kenya, without regard to its conflict of law provisions. Any disputes shall be resolved in the courts of Kenya.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "14. Severability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If any provision of these Terms is found to be unenforceable or invalid, that provision shall be limited or eliminated to the minimum extent necessary, and the remaining provisions shall remain in full force and effect.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "15. Contact Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "For questions, concerns, or notices regarding these Terms of Use:\n\n" +
                                    "Email: legal@budgieapp.com\n" +
                                    "Support: support@budgieapp.com\n\n" +
                                    "We aim to respond to all inquiries within 48 hours.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981) // Emerald
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A1929) // Navy
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Last Updated: December 25, 2025",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE0F2FE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Your Privacy, Our Promise",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0A1929)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Budgie ONLY collects your date of birth. All other data stays exclusively on your device. We don't collect, transmit, or sell your information.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "1. Information We Collect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Personal Data We Collect:\n\n" +
                                    "Budgie collects ONLY ONE piece of personal data:\n\n" +
                                    "• Your Date of Birth (birthday)\n\n" +
                                    "Purpose of Collection:\n" +
                                    "• Age verification (18+ requirement)\n" +
                                    "• Personalized birthday greetings\n" +
                                    "• Age-appropriate financial insights\n" +
                                    "• Life-stage specific recommendations\n\n" +
                                    "Data You Store Locally:\n\n" +
                                    "The following data is stored ONLY on your device and is NOT collected by us:\n\n" +
                                    "• Your name/nickname\n" +
                                    "• Financial transactions (expenses, income)\n" +
                                    "• Bills and budgets\n" +
                                    "• Any notes or categories you create\n\n" +
                                    "We have NO access to this information.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "2. How We Use Your Birthday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your date of birth is used exclusively for:\n\n" +
                                    "1. Age Verification\n" +
                                    "   • Ensuring you meet the 18+ requirement\n" +
                                    "   • Complying with legal age restrictions\n\n" +
                                    "2. Personalization\n" +
                                    "   • Displaying birthday wishes on your special day\n" +
                                    "   • Personalizing greeting messages\n\n" +
                                    "3. Financial Insights\n" +
                                    "   • Calculating your age for life-stage analysis\n" +
                                    "   • Providing age-appropriate financial recommendations\n" +
                                    "   • Retirement planning calculations\n" +
                                    "   • Long-term wealth projections\n\n" +
                                    "4. Analytics (Local Only)\n" +
                                    "   • Understanding user age demographics\n" +
                                    "   • Improving app features for different age groups\n\n" +
                                    "All processing happens locally on your device. Your birthday is NEVER transmitted to our servers or any third party.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "3. Data Storage & Security",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Storage Location:\n\n" +
                                    "• All data is stored in your device's local database using Android's secure storage (SharedPreferences and Room Database)\n" +
                                    "• Data is encrypted using your device's built-in security\n" +
                                    "• We do NOT use cloud storage\n" +
                                    "• We do NOT sync data across devices\n\n" +
                                    "Data Transmission - YOU ARE IN COMPLETE CONTROL:\n\n" +
                                    "🔒 Budgie NEVER transmits your data automatically\n" +
                                    "🔒 We do NOT have backend servers collecting user data\n" +
                                    "🔒 The app functions entirely offline after installation\n" +
                                    "🔒 No internet connection is required for core functionality\n\n" +
                                    "📤 ONLY YOU Control Data Exports:\n" +
                                    "• When you export reports (PDF, Excel), YOU initiate the action\n" +
                                    "• YOU choose where to save or send exported files\n" +
                                    "• YOU decide who receives your financial reports\n" +
                                    "• Budgie has NO access to your exported files once created\n" +
                                    "• We NEVER see, store, or process your exports\n" +
                                    "• Your exported data is YOUR property entirely\n\n" +
                                    "Security Measures:\n\n" +
                                    "• Your device's lock screen protection secures your data\n" +
                                    "• Android's app sandboxing prevents other apps from accessing Budgie's data\n" +
                                    "• Data is protected by your device's security features (encryption, biometrics)\n" +
                                    "• Optional in-app PIN/biometric lock for additional security\n\n" +
                                    "Your Responsibility:\n\n" +
                                    "• Keep your device secure with a strong password/PIN\n" +
                                    "• Enable device encryption if available\n" +
                                    "• Regularly back up your device\n" +
                                    "• Don't share your device with untrusted individuals\n" +
                                    "• Be careful who you share exported reports with",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "4. Data Sharing & Third Parties",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "WE DO NOT SHARE YOUR DATA. PERIOD.\n\n" +
                                    "• We do NOT sell your data to advertisers\n" +
                                    "• We do NOT share data with marketing companies\n" +
                                    "• We do NOT provide data to data brokers\n" +
                                    "• We do NOT use analytics services that collect personal data\n" +
                                    "• We do NOT have partnerships that involve data sharing\n\n" +
                                    "There are NO third parties involved in Budgie's operation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        Text(
                            text = "5. Cookies & Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Budgie does NOT use:\n\n" +
                                    "• Cookies\n" +
                                    "• Web beacons\n" +
                                    "• Tracking pixels\n" +
                                    "• Analytics SDKs\n" +
                                    "• Advertising identifiers\n" +
                                    "• Session tracking\n\n" +
                                    "We do NOT track your behavior, location, or device information.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "6. Your Rights & Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You have COMPLETE control over your data:\n\n" +
                                    "1. Right to Access\n" +
                                    "   • View your birthday anytime in the app\n" +
                                    "   • Access all financial data you've entered\n\n" +
                                    "2. Right to Modify\n" +
                                    "   • Change your personal information anytime\n" +
                                    "   • Edit or delete any financial records\n\n" +
                                    "3. Right to Delete\n" +
                                    "   • Uninstall the app to permanently delete all data\n" +
                                    "   • Clear app data from device settings\n" +
                                    "   • No residual data remains after deletion\n\n" +
                                    "4. Right to Export - YOUR DATA, YOUR CHOICE\n" +
                                    "   • Export your data to PDF or Excel anytime\n" +
                                    "   • YOU initiate all exports - we never do\n" +
                                    "   • Choose exactly what data to include\n" +
                                    "   • Send exports wherever YOU want\n" +
                                    "   • Share with anyone YOU choose\n" +
                                    "   • Budgie has NO visibility into your exports\n" +
                                    "   • Take your data with you - it's YOURS\n\n" +
                                    "5. Right to Opt-Out\n" +
                                    "   • Since we don't collect data, there's nothing to opt out of!\n" +
                                    "   • No marketing emails (we don't have your email)\n" +
                                    "   • No notifications unless you enable them\n\n" +
                                    "⭐ Bottom Line: Your financial data belongs to YOU. Only YOU can decide to share it. Budgie is just a tool - you're the owner.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "7. Children's Privacy (COPPA Compliance)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Budgie is NOT intended for individuals under 18 years of age.\n\n" +
                                    "• We do NOT knowingly collect data from minors\n" +
                                    "• Age verification is required during onboarding\n" +
                                    "• Users under 18 cannot proceed past the onboarding screen\n\n" +
                                    "If we discover that a minor has provided information, we will:\n" +
                                    "• Immediately prevent further use\n" +
                                    "• Delete any collected data\n\n" +
                                    "Parents: If you believe your child has used Budgie, please contact us immediately.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "8. Data Retention",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your data is retained:\n\n" +
                                    "• Only on your device\n" +
                                    "• For as long as you keep the app installed\n" +
                                    "• Until you choose to delete it\n\n" +
                                    "We do NOT retain any data because we don't collect it in the first place!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "9. International Data Transfers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Since all data is stored locally on your device:\n\n" +
                                    "• There are NO international data transfers\n" +
                                    "• Your data never leaves your device\n" +
                                    "• GDPR, CCPA, and other privacy regulations don't apply in the traditional sense\n\n" +
                                    "Your data stays in your country, on your device, in your control.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "10. Changes to This Privacy Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We may update this Privacy Policy from time to time. When we do:\n\n" +
                                    "• We will update the \"Last Updated\" date\n" +
                                    "• Material changes will be communicated via the app\n" +
                                    "• You will be notified of significant changes\n\n" +
                                    "We encourage you to review this policy periodically. Continued use after changes constitutes acceptance of the updated policy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "11. Legal Compliance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We comply with:\n\n" +
                                    "• Kenya Data Protection Act, 2019\n" +
                                    "• General Data Protection Regulation (GDPR) principles\n" +
                                    "• California Consumer Privacy Act (CCPA) where applicable\n\n" +
                                    "However, since we only collect your birthday and store everything locally, most regulations don't apply in the traditional sense.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }

                    item {
                        Text(
                            text = "12. Contact Us",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A1929)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If you have questions, concerns, or requests regarding this Privacy Policy or your data:\n\n" +
                                    "Privacy Officer: privacy@budgieapp.com\n" +
                                    "General Support: support@budgieapp.com\n" +
                                    "Legal Inquiries: legal@budgieapp.com\n\n" +
                                    "We aim to respond within 48 hours.\n\n" +
                                    "Mailing Address:\n" +
                                    "Budgie App\n" +
                                    "Nairobi, Kenya\n\n" +
                                    "We're committed to protecting your privacy and will address any concerns promptly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981) // Emerald
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

