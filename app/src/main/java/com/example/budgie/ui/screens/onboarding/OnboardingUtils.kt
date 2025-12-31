package com.example.budgie.ui.screens.onboarding

import androidx.compose.ui.graphics.Color

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
fun analyzePinStrength(pin: String): PinStrengthResult {
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
    @Suppress("KotlinConstantConditions")
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
fun getPinStrengthColor(strength: PinStrength): Color {
    return when (strength) {
        PinStrength.VERY_WEAK -> Color(0xFFEF4444) // Red
        PinStrength.WEAK -> Color(0xFFF97316) // Orange
        PinStrength.MODERATE -> Color(0xFFEAB308) // Yellow/Amber
        PinStrength.STRONG -> Color(0xFF10B981) // Green
    }
}

/**
 * Get maximum days in a month, accounting for leap years
 */
fun getMaxDaysInMonth(month: Int, year: Int): Int {
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
fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

/**
 * Month data for birthday picker
 */
val MONTHS = listOf(
    "01" to "Jan", "02" to "Feb", "03" to "Mar",
    "04" to "Apr", "05" to "May", "06" to "Jun",
    "07" to "Jul", "08" to "Aug", "09" to "Sep",
    "10" to "Oct", "11" to "Nov", "12" to "Dec"
)

