package com.example.budgie.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.R

/* ═══════════════════════════════════════════════════════════════════
   RESPONSIVE DASHBOARD TOP BAR
   Premium navigation with responsive sizing
═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveDashboardTopBar(
    dimens: DashboardDimens,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onShowCurrencyConverter: () -> Unit,
    onShowCalculator: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    unreadNotificationCount: Int,
    onLogout: () -> Unit,
    onExit: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Logout Dialog
    if (showLogoutDialog) {
        ResponsiveConfirmationDialog(
            dimens = dimens,
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            icon = Icons.Default.Logout,
            iconColor = DashboardAmber,
            title = "Logout Session",
            subtitle = "Secure Sign Out",
            message = "Your session will be ended and you'll need to authenticate again.",
            confirmText = "Logout",
            confirmColor = DashboardAmber,
            cancelText = "Stay"
        )
    }

    // Exit Dialog
    if (showExitDialog) {
        ResponsiveConfirmationDialog(
            dimens = dimens,
            onDismiss = { showExitDialog = false },
            onConfirm = {
                showExitDialog = false
                onExit()
            },
            icon = Icons.Default.PowerSettingsNew,
            iconColor = DashboardMutedRed,
            title = "Close Budgie",
            subtitle = "Exit Application",
            message = "Your data is automatically saved. You can return anytime.",
            confirmText = "Exit",
            confirmColor = DashboardMutedRed,
            cancelText = "Keep Open"
        )
    }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Budgie Logo",
                    modifier = Modifier
                        .size(dimens.topBarLogoSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    "Budgie",
                    fontWeight = FontWeight.Bold,
                    color = DashboardSoftWhite,
                    fontSize = dimens.topBarTitleSize.sp
                )
            }
        },
        actions = {

            Box {
                IconButton(onClick = { onShowMenuChange(true) }) {
                    Icon(Icons.Default.MoreVert, "Menu", tint = DashboardSoftWhite)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { onShowMenuChange(false) },
                    modifier = Modifier
                        .background(DashboardNavyLight)
                        .width(dimens.menuWidth)
                ) {
                    // Profile
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.Person,
                        iconTint = DashboardEmerald,
                        text = "My Profile",
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToProfile()
                        }
                    )


                    // Security
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.Security,
                        iconTint = DashboardSoftWhite.copy(alpha = 0.7f),
                        text = "Security",
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToSecurity()
                        }
                    )

                    // Currency Converter
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.CurrencyExchange,
                        iconTint = DashboardEmerald,
                        text = "Currency Converter",
                        onClick = {
                            onShowMenuChange(false)
                            onShowCurrencyConverter()
                        }
                    )

                    // Calculator
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.Calculate,
                        iconTint = DashboardAmber,
                        text = "Calculator",
                        onClick = {
                            onShowMenuChange(false)
                            onShowCalculator()
                        }
                    )

                    // Help
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.Help,
                        iconTint = DashboardSoftWhite.copy(alpha = 0.7f),
                        text = "Help & Support",
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToHelp()
                        }
                    )

                    // About
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.Info,
                        iconTint = DashboardSoftWhite.copy(alpha = 0.7f),
                        text = "About Budgie",
                        onClick = {
                            onShowMenuChange(false)
                            onNavigateToAbout()
                        }
                    )

                    HorizontalDivider(
                        color = DashboardSoftWhite.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Logout
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.Logout,
                        iconTint = DashboardAmber,
                        text = "Logout",
                        textColor = DashboardAmber,
                        onClick = {
                            onShowMenuChange(false)
                            showLogoutDialog = true
                        }
                    )

                    // Exit
                    ResponsiveMenuItem(
                        dimens = dimens,
                        icon = Icons.Default.ExitToApp,
                        iconTint = DashboardMutedRed,
                        text = "Exit App",
                        textColor = DashboardMutedRed,
                        onClick = {
                            onShowMenuChange(false)
                            showExitDialog = true
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DashboardNavy)
    )
}

@Composable
private fun ResponsiveMenuItem(
    dimens: DashboardDimens,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String,
    textColor: Color = DashboardSoftWhite,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(dimens.menuItemIconSize)
                )
                Text(
                    text,
                    color = textColor,
                    fontSize = dimens.menuItemFontSize.sp
                )
            }
        },
        onClick = onClick
    )
}

@Composable
fun ResponsiveConfirmationDialog(
    dimens: DashboardDimens,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    cancelText: String
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(dimens.dialogWidth)
                .ultraGlassCard(iconColor, dimens.cardCornerRadius)
        ) {
            Column(
                modifier = Modifier.padding(dimens.dialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with glow
                Box(
                    modifier = Modifier
                        .size(dimens.dialogIconSize)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    iconColor.copy(alpha = 0.25f),
                                    iconColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(dimens.dialogIconSize * 0.45f)
                    )
                }

                Spacer(modifier = Modifier.height(dimens.verticalSpacing))

                // Subtitle badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = subtitle.uppercase(),
                        fontSize = (dimens.dialogMessageSize - 2).sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(dimens.verticalSpacing / 2))

                // Title
                Text(
                    text = title,
                    fontSize = dimens.dialogTitleSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardSoftWhite
                )

                Spacer(modifier = Modifier.height(dimens.verticalSpacing / 2))

                // Message
                Text(
                    text = message,
                    fontSize = dimens.dialogMessageSize.sp,
                    color = DashboardSoftWhite.copy(alpha = 0.65f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(dimens.verticalSpacing * 1.5f))

                // Buttons
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
                        Text(cancelText, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.dialogButtonHeight),
                        shape = RoundedCornerShape(dimens.actionButtonCornerRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(dimens.actionButtonIconSize)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(confirmText, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(dimens.verticalSpacing))

                // Security note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DashboardSoftWhite.copy(alpha = 0.04f))
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = DashboardEmerald.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "Your data is encrypted & secure",
                        fontSize = (dimens.dialogMessageSize - 2).sp,
                        color = DashboardSoftWhite.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

