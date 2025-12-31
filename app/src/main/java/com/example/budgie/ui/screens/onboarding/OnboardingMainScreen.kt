package com.example.budgie.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Main Onboarding Screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (name: String, birthday: String, securityType: String, pin: String) -> Unit
) {
    // Get responsive dimensions
    val dimens = getResponsiveDimens()
    val screenSize = getScreenSize()

    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var birthdayError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showUnderageDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showWhyDialog by remember { mutableStateOf(false) }

    // Security states
    var securityType by remember { mutableStateOf("none") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Premium gradient background
    val premiumGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B1F2A),
            Color(0xFF0D2832),
            Color(0xFF0B1F2A)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = premiumGradient)
    ) {
        // Animated Background Elements
        if (screenSize != ScreenSize.COMPACT) {
            PremiumAnimatedBackground()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(dimens.topSpacing))

            // Logo Section
            LogoSection(dimens = dimens)

            Spacer(modifier = Modifier.height(dimens.sectionSpacing))

            // Title Section
            TitleSection(dimens = dimens, screenSize = screenSize)

            Spacer(modifier = Modifier.height(dimens.sectionSpacing))

            // Main Input Card
            MainInputCard(
                name = name,
                onNameChange = { name = it; nameError = null },
                nameError = nameError,
                birthday = birthday,
                onBirthdayChange = { birthday = it; birthdayError = null },
                birthdayError = birthdayError,
                onOpenCalendar = { showDatePicker = true },
                onWhyClick = { showWhyDialog = true },
                dimens = dimens,
                screenSize = screenSize
            )

            Spacer(modifier = Modifier.height(dimens.sectionSpacing))

            // Security Card
            SecurityCard(
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
                onPinErrorChange = { pinError = it },
                dimens = dimens,
                screenSize = screenSize
            )

            Spacer(modifier = Modifier.height(dimens.sectionSpacing))

            // Terms & Privacy
            ResponsiveTermsAndPrivacyAgreement(
                onTermsClick = { showTermsDialog = true },
                onPrivacyClick = { showPrivacyDialog = true },
                dimens = dimens,
                screenSize = screenSize
            )

            Spacer(modifier = Modifier.height(dimens.sectionSpacing))

            // Continue Button
            ContinueButton(
                onClick = {
                    val hasError = validateInputs(
                        name = name,
                        birthday = birthday,
                        securityType = securityType,
                        pin = pin,
                        confirmPin = confirmPin,
                        onNameError = { nameError = it },
                        onBirthdayError = { birthdayError = it },
                        onPinError = { pinError = it },
                        onShowUnderageDialog = { showUnderageDialog = true }
                    )
                    if (!hasError) {
                        onComplete(name.trim(), birthday, securityType, pin)
                    }
                },
                dimens = dimens
            )

            Spacer(modifier = Modifier.height(dimens.sectionSpacing))

            // Privacy Footer
            PrivacyFooter(dimens = dimens, screenSize = screenSize)

            Spacer(modifier = Modifier.height(dimens.topSpacing))
        }

        // Dialogs
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Date(millis)
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
                colors = DatePickerDefaults.colors(containerColor = Color.White)
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = true,
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = Color(0xFF10B981),
                        todayDateBorderColor = Color(0xFF10B981)
                    )
                )
            }
        }

        if (showUnderageDialog) {
            UnderageDialog(
                onDismiss = {
                    showUnderageDialog = false
                    birthday = ""
                    birthdayError = null
                }
            )
        }

        if (showTermsDialog) {
            TermsOfUseDialog(onDismiss = { showTermsDialog = false })
        }

        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }

        if (showWhyDialog) {
            WhyBirthdayDialog(onDismiss = { showWhyDialog = false })
        }
    }
}

@Composable
private fun LogoSection(dimens: ResponsiveDimens) {
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
            .size(dimens.logoContainerSize)
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
        Image(
            painter = painterResource(R.drawable.icon),
            contentDescription = "Budgie Logo",
            modifier = Modifier
                .size(dimens.logoSize)
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
}

@Composable
private fun TitleSection(dimens: ResponsiveDimens, screenSize: ScreenSize) {
    Text(
        text = "Budgie",
        fontSize = dimens.titleFontSize.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFE6F1F0),
        textAlign = TextAlign.Center,
        letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "AI-Powered Personal Finance Advisor",
        fontSize = dimens.subtitleFontSize.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF0FAE96),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(dimens.verticalSpacing))

    Row(
        horizontalArrangement = Arrangement.spacedBy(if (screenSize == ScreenSize.COMPACT) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResponsiveGlassmorphicChip("💰 Track", dimens)
        ResponsiveGlassmorphicChip("🧠 Insights", dimens)
        ResponsiveGlassmorphicChip("📈 Grow", dimens)
    }
}

@Composable
private fun MainInputCard(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    birthday: String,
    onBirthdayChange: (String) -> Unit,
    birthdayError: String?,
    onOpenCalendar: () -> Unit,
    onWhyClick: () -> Unit,
    dimens: ResponsiveDimens,
    screenSize: ScreenSize
) {
    ResponsiveGlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        dimens = dimens
    ) {
        Column(
            modifier = Modifier.padding(dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(if (screenSize == ScreenSize.COMPACT) 32.dp else 36.dp)
                        .background(
                            Color(0xFF0FAE96).copy(alpha = 0.2f),
                            RoundedCornerShape(if (screenSize == ScreenSize.COMPACT) 8.dp else 10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFF0FAE96),
                        modifier = Modifier.size(dimens.iconSize)
                    )
                }
                Spacer(modifier = Modifier.width(if (screenSize == ScreenSize.COMPACT) 8.dp else 10.dp))
                Text(
                    text = "Step 1 of 2 - Personal setup",
                    fontSize = dimens.labelFontSize.sp,
                    color = Color(0xFF0FAE96)
                )
            }

            // Name Input
            ResponsiveGlassmorphicTextField(
                value = name,
                onValueChange = onNameChange,
                label = if (screenSize == ScreenSize.COMPACT) "Your name" else "By what name should I refer you?",
                placeholder = "Name or nickname is okay",
                leadingIcon = Icons.Filled.Person,
                isError = nameError != null,
                errorMessage = nameError,
                dimens = dimens
            )

            // Birthday Picker
            ResponsiveSmartBirthdayPicker(
                birthday = birthday,
                onBirthdayChange = onBirthdayChange,
                birthdayError = birthdayError,
                onOpenCalendar = onOpenCalendar,
                dimens = dimens,
                screenSize = screenSize
            )

            // Why info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onWhyClick)
                    .padding(vertical = if (screenSize == ScreenSize.COMPACT) 4.dp else 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(dimens.smallIconSize)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (screenSize == ScreenSize.COMPACT) "Why birthday?" else "Why do I need your accurate birthday?",
                    fontSize = dimens.bodyFontSize.sp,
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SecurityCard(
    securityType: String,
    onSecurityTypeChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    pinError: String?,
    onPinErrorChange: (String?) -> Unit,
    dimens: ResponsiveDimens,
    screenSize: ScreenSize
) {
    ResponsiveGlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        dimens = dimens
    ) {
        Column(
            modifier = Modifier.padding(dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
        ) {
            ResponsiveSecuritySelectionSection(
                securityType = securityType,
                onSecurityTypeChange = onSecurityTypeChange,
                pin = pin,
                onPinChange = onPinChange,
                confirmPin = confirmPin,
                onConfirmPinChange = onConfirmPinChange,
                pinError = pinError,
                onPinErrorChange = onPinErrorChange,
                dimens = dimens,
                screenSize = screenSize
            )
        }
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit,
    dimens: ResponsiveDimens
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight),
        shape = RoundedCornerShape(dimens.cornerRadius),
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
            fontSize = dimens.bodyFontSize.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0FAE96)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color(0xFF0FAE96),
            modifier = Modifier.size(dimens.smallIconSize)
        )
    }
}

@Composable
private fun PrivacyFooter(dimens: ResponsiveDimens, screenSize: ScreenSize) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF0FAE96).copy(alpha = 0.1f),
                RoundedCornerShape(dimens.cornerRadius)
            )
            .padding(
                horizontal = if (screenSize == ScreenSize.COMPACT) 10.dp else 16.dp,
                vertical = if (screenSize == ScreenSize.COMPACT) 8.dp else 12.dp
            )
    ) {
        Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = "Security",
            tint = Color(0xFF0FAE96),
            modifier = Modifier.size(dimens.smallIconSize)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (screenSize == ScreenSize.COMPACT) "100% Private · On-device only" else "100% Private · Stored on your device · Never shared",
            fontSize = if (screenSize == ScreenSize.COMPACT) 10.sp else dimens.bodyFontSize.sp,
            color = Color(0xFFE6F1F0),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Validates all inputs and returns true if there are errors
 */
private fun validateInputs(
    name: String,
    birthday: String,
    securityType: String,
    pin: String,
    confirmPin: String,
    onNameError: (String?) -> Unit,
    onBirthdayError: (String?) -> Unit,
    onPinError: (String?) -> Unit,
    onShowUnderageDialog: () -> Unit
): Boolean {
    var hasError = false

    if (name.isBlank()) {
        onNameError("Please enter your name")
        hasError = true
    }

    if (birthday.isBlank()) {
        onBirthdayError("Please enter your birthday")
        hasError = true
    } else if (birthday.length < 10) {
        onBirthdayError("Please enter complete date (YYYY/MM/DD)")
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
                    onBirthdayError("Birthday cannot be in the future")
                    hasError = true
                } else {
                    val age18 = Calendar.getInstance()
                    age18.add(Calendar.YEAR, -18)

                    if (birthCal.after(age18)) {
                        onShowUnderageDialog()
                        return true
                    }

                    val age120 = Calendar.getInstance()
                    age120.add(Calendar.YEAR, -120)
                    if (birthCal.before(age120)) {
                        onBirthdayError("Please enter a valid birthday")
                        hasError = true
                    }
                }
            } else {
                onBirthdayError("Invalid date format")
                hasError = true
            }
        } catch (_: Exception) {
            onBirthdayError("Invalid format. Use YYYY/MM/DD (e.g., 1995/06/15)")
            hasError = true
        }
    }

    if (securityType == "pin" || securityType == "pin_biometric") {
        if (pin.length != 5) {
            onPinError("Please enter a 5-digit PIN")
            hasError = true
        } else if (confirmPin.length != 5) {
            onPinError("Please confirm your PIN")
            hasError = true
        } else if (pin != confirmPin) {
            onPinError("PINs do not match")
            hasError = true
        }
    }

    return hasError
}

