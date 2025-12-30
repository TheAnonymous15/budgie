package com.example.budgie.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.content.ContextWrapper

private const val TAG = "BiometricDiagnostics"

// Theme colors
private val NavyDark = Color(0xFF050F14)
private val NavyMid = Color(0xFF0A1A24)
private val Navy = Color(0xFF0B1F2A)
private val Emerald = Color(0xFF0FAE96)
private val Teal = Color(0xFF0B8F7A)
private val SoftWhite = Color(0xFFE6F1F0)
private val Gold = Color(0xFFBFA25A)
private val ErrorRed = Color(0xFFE57373)
private val WarningAmber = Color(0xFFFFB74D)

data class BiometricDiagnosticResult(
    val deviceInfo: DeviceInfo,
    val biometricCapabilities: BiometricCapabilities,
    val hardwareFeatures: HardwareFeatures,
    val recommendations: List<String>,
    val overallStatus: OverallStatus
)

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val device: String,
    val brand: String,
    val sdkVersion: Int,
    val androidVersion: String,
    val securityPatchLevel: String
)

data class BiometricCapabilities(
    val strongBiometric: AuthenticatorStatus,
    val weakBiometric: AuthenticatorStatus,
    val deviceCredential: AuthenticatorStatus,
    val strongOrDeviceCredential: AuthenticatorStatus,
    val weakOrDeviceCredential: AuthenticatorStatus
)

data class AuthenticatorStatus(
    val available: Boolean,
    val statusCode: Int,
    val statusMessage: String
)

data class HardwareFeatures(
    val hasFingerprint: Boolean,
    val hasFaceDetection: Boolean,
    val hasIris: Boolean,
    val hasBiometricHardware: Boolean
)

enum class OverallStatus {
    FULL_SUPPORT,           // All biometrics work including face
    FINGERPRINT_ONLY,       // Only fingerprint works
    DEVICE_CREDENTIAL_ONLY, // Only PIN/pattern/password
    NO_BIOMETRIC_SUPPORT,   // Nothing works
    UNKNOWN
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricDiagnosticsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var diagnosticResult by remember { mutableStateOf<BiometricDiagnosticResult?>(null) }
    var isRunningTest by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        diagnosticResult = runDiagnostics(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDark, NavyMid, Navy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text("Biometric Diagnostics", color = SoftWhite)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            if (diagnosticResult == null) {
                // Loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Emerald)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val result = diagnosticResult!!

                    // Overall Status Card
                    OverallStatusCard(result.overallStatus)

                    Spacer(Modifier.height(16.dp))

                    // Device Info Card
                    DiagnosticCard(
                        title = "Device Information",
                        icon = Icons.Default.PhoneAndroid
                    ) {
                        DeviceInfoContent(result.deviceInfo)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Biometric Capabilities Card
                    DiagnosticCard(
                        title = "BiometricPrompt API Status",
                        icon = Icons.Default.Fingerprint
                    ) {
                        BiometricCapabilitiesContent(result.biometricCapabilities)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Hardware Features Card
                    DiagnosticCard(
                        title = "Hardware Features",
                        icon = Icons.Default.Memory
                    ) {
                        HardwareFeaturesContent(result.hardwareFeatures)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Recommendations Card
                    if (result.recommendations.isNotEmpty()) {
                        DiagnosticCard(
                            title = "Analysis & Recommendations",
                            icon = Icons.Default.Lightbulb
                        ) {
                            RecommendationsContent(result.recommendations)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Test Biometrics Card
                    DiagnosticCard(
                        title = "Live Biometric Test",
                        icon = Icons.Default.Science
                    ) {
                        Column {
                            Text(
                                "Test each biometric type to see what your device actually supports:",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftWhite.copy(alpha = 0.7f)
                            )

                            Spacer(Modifier.height(12.dp))

                            // Test buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TestButton(
                                    text = "Strong",
                                    icon = Icons.Default.Fingerprint,
                                    enabled = result.biometricCapabilities.strongBiometric.available,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        testBiometric(context, BiometricManager.Authenticators.BIOMETRIC_STRONG) {
                                            testResult = it
                                            showTestDialog = true
                                        }
                                    }
                                )
                                TestButton(
                                    text = "Weak",
                                    icon = Icons.Default.Face,
                                    enabled = result.biometricCapabilities.weakBiometric.available,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        testBiometric(context, BiometricManager.Authenticators.BIOMETRIC_WEAK) {
                                            testResult = it
                                            showTestDialog = true
                                        }
                                    }
                                )
                                TestButton(
                                    text = "Any",
                                    icon = Icons.Default.Security,
                                    enabled = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        testBiometric(
                                            context,
                                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                        ) {
                                            testResult = it
                                            showTestDialog = true
                                        }
                                    }
                                )
                            }

                            testResult?.let { result ->
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (result.startsWith("✓")) Emerald.copy(alpha = 0.2f)
                                                        else ErrorRed.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        result,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SoftWhite
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Face Recognition Explanation
                    ExplanationCard()

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    // Test Result Dialog
    if (showTestDialog && testResult != null) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            containerColor = NavyDark,
            title = {
                Text("Test Result", color = SoftWhite)
            },
            text = {
                Text(testResult!!, color = SoftWhite.copy(alpha = 0.8f))
            },
            confirmButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("OK", color = Emerald)
                }
            }
        )
    }
}

@Composable
private fun OverallStatusCard(status: OverallStatus) {
    val (icon, title, description, color) = when (status) {
        OverallStatus.FULL_SUPPORT -> Quadruple(
            Icons.Default.CheckCircle,
            "Full Biometric Support",
            "Your device supports all biometric authentication methods including face recognition through BiometricPrompt API.",
            Emerald
        )
        OverallStatus.FINGERPRINT_ONLY -> Quadruple(
            Icons.Default.Fingerprint,
            "Fingerprint Only",
            "Your device supports fingerprint authentication. Face unlock may not be available through the standard API (common on Xiaomi, Oppo, Vivo devices).",
            Gold
        )
        OverallStatus.DEVICE_CREDENTIAL_ONLY -> Quadruple(
            Icons.Default.Pin,
            "PIN/Pattern Only",
            "Only device credentials (PIN, pattern, password) are available. No biometric hardware detected or enrolled.",
            WarningAmber
        )
        OverallStatus.NO_BIOMETRIC_SUPPORT -> Quadruple(
            Icons.Default.Error,
            "No Biometric Support",
            "Your device does not support biometric authentication through the standard Android API.",
            ErrorRed
        )
        OverallStatus.UNKNOWN -> Quadruple(
            Icons.Default.HelpOutline,
            "Unknown Status",
            "Could not determine biometric capabilities.",
            SoftWhite.copy(alpha = 0.5f)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SoftWhite.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Emerald, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftWhite
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DeviceInfoContent(info: DeviceInfo) {
    Column {
        InfoRow("Manufacturer", info.manufacturer)
        InfoRow("Model", info.model)
        InfoRow("Brand", info.brand)
        InfoRow("Device", info.device)
        InfoRow("Android Version", info.androidVersion)
        InfoRow("SDK Level", "API ${info.sdkVersion}")
        InfoRow("Security Patch", info.securityPatchLevel)
    }
}

@Composable
private fun BiometricCapabilitiesContent(caps: BiometricCapabilities) {
    Column {
        StatusRow("BIOMETRIC_STRONG", caps.strongBiometric)
        StatusRow("BIOMETRIC_WEAK", caps.weakBiometric)
        StatusRow("DEVICE_CREDENTIAL", caps.deviceCredential)

        HorizontalDivider(color = SoftWhite.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        Text(
            "Combined Authenticators:",
            style = MaterialTheme.typography.labelSmall,
            color = SoftWhite.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(4.dp))
        StatusRow("STRONG | DEVICE_CREDENTIAL", caps.strongOrDeviceCredential)
        StatusRow("WEAK | DEVICE_CREDENTIAL", caps.weakOrDeviceCredential)
    }
}

@Composable
private fun HardwareFeaturesContent(features: HardwareFeatures) {
    Column {
        FeatureRow("Fingerprint Sensor", features.hasFingerprint)
        FeatureRow("Face Detection", features.hasFaceDetection)
        FeatureRow("Iris Scanner", features.hasIris)
        FeatureRow("General Biometric HW", features.hasBiometricHardware)
    }
}

@Composable
private fun RecommendationsContent(recommendations: List<String>) {
    Column {
        recommendations.forEachIndexed { index, rec ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    rec,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftWhite.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = SoftWhite.copy(alpha = 0.5f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = SoftWhite,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusRow(name: String, status: AuthenticatorStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (status.available) Emerald else ErrorRed)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite
            )
            Text(
                status.statusMessage,
                style = MaterialTheme.typography.labelSmall,
                color = SoftWhite.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun FeatureRow(name: String, available: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = SoftWhite
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (available) Icons.Default.Check else Icons.Default.Close,
                null,
                tint = if (available) Emerald else ErrorRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (available) "Available" else "Not Found",
                style = MaterialTheme.typography.labelSmall,
                color = if (available) Emerald else ErrorRed.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TestButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Emerald.copy(alpha = 0.2f),
            contentColor = Emerald,
            disabledContainerColor = SoftWhite.copy(alpha = 0.05f),
            disabledContentColor = SoftWhite.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExplanationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Gold.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Gold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Why Face Unlock Might Not Work",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                """Most Android OEMs (Xiaomi, Oppo, Vivo, OnePlus, etc.) implement face unlock using proprietary APIs that are NOT exposed through the standard BiometricPrompt API.

What happens on your device:
• BIOMETRIC_WEAK reports "Available" ✓
• But tapping "Weak" opens FINGERPRINT dialog
• This is because Xiaomi maps BIOMETRIC_WEAK to fingerprint

Face unlock through BiometricPrompt typically only works on:
• Google Pixel 4+ (with Soli radar)
• Samsung Galaxy S21+ (with 3D ToF sensor)
• Devices with Class 3 (BIOMETRIC_STRONG) face hardware

The face unlock you use to unlock your phone is managed by Xiaomi's MIUI and is not available for third-party apps.

✓ Budgie uses fingerprint (BIOMETRIC_STRONG) for secure authentication.""",
                style = MaterialTheme.typography.bodySmall,
                color = SoftWhite.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
        }
    }
}

// Helper data class
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Diagnostic functions
private fun runDiagnostics(context: Context): BiometricDiagnosticResult {
    val deviceInfo = getDeviceInfo()
    val biometricCaps = getBiometricCapabilities(context)
    val hardwareFeatures = getHardwareFeatures(context)
    val recommendations = generateRecommendations(deviceInfo, biometricCaps, hardwareFeatures)
    val overallStatus = determineOverallStatus(biometricCaps, hardwareFeatures)

    Log.d(TAG, "Diagnostics complete: status=$overallStatus")

    return BiometricDiagnosticResult(
        deviceInfo = deviceInfo,
        biometricCapabilities = biometricCaps,
        hardwareFeatures = hardwareFeatures,
        recommendations = recommendations,
        overallStatus = overallStatus
    )
}

private fun getDeviceInfo(): DeviceInfo {
    return DeviceInfo(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        device = Build.DEVICE,
        brand = Build.BRAND,
        sdkVersion = Build.VERSION.SDK_INT,
        androidVersion = Build.VERSION.RELEASE,
        securityPatchLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Build.VERSION.SECURITY_PATCH else "N/A"
    )
}

private fun getBiometricCapabilities(context: Context): BiometricCapabilities {
    val manager = BiometricManager.from(context)

    fun checkAuth(authenticators: Int): AuthenticatorStatus {
        val result = manager.canAuthenticate(authenticators)
        return AuthenticatorStatus(
            available = result == BiometricManager.BIOMETRIC_SUCCESS,
            statusCode = result,
            statusMessage = biometricStatusToString(result)
        )
    }

    return BiometricCapabilities(
        strongBiometric = checkAuth(BiometricManager.Authenticators.BIOMETRIC_STRONG),
        weakBiometric = checkAuth(BiometricManager.Authenticators.BIOMETRIC_WEAK),
        deviceCredential = checkAuth(BiometricManager.Authenticators.DEVICE_CREDENTIAL),
        strongOrDeviceCredential = checkAuth(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ),
        weakOrDeviceCredential = checkAuth(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    )
}

private fun getHardwareFeatures(context: Context): HardwareFeatures {
    val pm = context.packageManager

    val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
        pm.hasSystemFeature("android.hardware.biometrics.face")
    } else {
        pm.hasSystemFeature("android.hardware.biometrics.face")
    }

    val hasIris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        pm.hasSystemFeature(PackageManager.FEATURE_IRIS) ||
        pm.hasSystemFeature("android.hardware.biometrics.iris")
    } else {
        pm.hasSystemFeature("android.hardware.biometrics.iris")
    }

    return HardwareFeatures(
        hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT),
        hasFaceDetection = hasFace,
        hasIris = hasIris,
        hasBiometricHardware = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) || hasFace || hasIris
    )
}

private fun generateRecommendations(
    device: DeviceInfo,
    caps: BiometricCapabilities,
    @Suppress("UNUSED_PARAMETER") hw: HardwareFeatures
): List<String> {
    val recs = mutableListOf<String>()

    // Check manufacturer-specific issues
    val manufacturer = device.manufacturer.lowercase()
    if (manufacturer in listOf("xiaomi", "redmi", "poco", "oppo", "vivo", "realme", "oneplus")) {
        recs.add("Your device (${device.manufacturer}) uses proprietary face unlock that's not available through standard Android APIs. This is a known limitation.")

        if (caps.weakBiometric.available) {
            recs.add("BIOMETRIC_WEAK shows as 'Available' but will use fingerprint, not face unlock. This is normal for ${device.manufacturer} devices.")
        }
    }

    // Check BIOMETRIC_WEAK specifically
    if (!caps.weakBiometric.available && hw.hasFaceDetection) {
        recs.add("Your device has face detection hardware, but it's not exposed through BIOMETRIC_WEAK. The manufacturer may use a custom implementation.")
    }

    // Recommendations based on what works
    if (caps.strongBiometric.available) {
        recs.add("Fingerprint authentication (BIOMETRIC_STRONG) is available and recommended for secure authentication.")
    }

    if (!caps.strongBiometric.available && !caps.weakBiometric.available) {
        if (caps.deviceCredential.available) {
            recs.add("Use PIN, pattern, or password for authentication as no biometrics are available through standard APIs.")
        }
    }

    // SDK version check
    if (device.sdkVersion < 30) {
        recs.add("Consider upgrading to Android 11+ for better biometric API support.")
    }

    if (recs.isEmpty()) {
        recs.add("Your device appears to have good biometric support through standard Android APIs.")
    }

    return recs
}

private fun determineOverallStatus(
    caps: BiometricCapabilities,
    hw: HardwareFeatures
): OverallStatus {
    return when {
        caps.strongBiometric.available && caps.weakBiometric.available -> OverallStatus.FULL_SUPPORT
        caps.strongBiometric.available -> OverallStatus.FINGERPRINT_ONLY
        caps.weakBiometric.available -> OverallStatus.FULL_SUPPORT // Has weak but not strong - likely face only
        caps.deviceCredential.available -> OverallStatus.DEVICE_CREDENTIAL_ONLY
        else -> OverallStatus.NO_BIOMETRIC_SUPPORT
    }
}

private fun biometricStatusToString(status: Int): String {
    return when (status) {
        BiometricManager.BIOMETRIC_SUCCESS -> "Available"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No hardware"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Hardware unavailable"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "None enrolled"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Update required"
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Unsupported"
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unknown"
        else -> "Code: $status"
    }
}

private fun testBiometric(context: Context, authenticators: Int, onResult: (String) -> Unit) {
    val activity = context.findFragmentActivity()
    if (activity == null) {
        onResult("✗ Could not get activity context")
        return
    }

    val manager = BiometricManager.from(context)
    val canAuth = manager.canAuthenticate(authenticators)

    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        onResult("✗ Cannot authenticate: ${biometricStatusToString(canAuth)}")
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val authType = when (result.authenticationType) {
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "Biometric"
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "Device Credential"
                else -> "Unknown"
            }
            onResult("✓ Authentication successful!\nType: $authType")
        }

        override fun onAuthenticationFailed() {
            onResult("✗ Authentication failed (biometric not recognized)")
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                onResult("Cancelled by user")
            } else {
                onResult("✗ Error ($errorCode): $errString")
            }
        }
    }

    val prompt = BiometricPrompt(activity, executor, callback)

    try {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Test")
            .setSubtitle("Testing authenticator type")
            .setAllowedAuthenticators(authenticators)

        // Only add negative button if not using DEVICE_CREDENTIAL
        if ((authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
            promptInfo.setNegativeButtonText("Cancel")
        }

        prompt.authenticate(promptInfo.build())
    } catch (e: Exception) {
        onResult("✗ Exception: ${e.message}")
    }
}

