package com.example.budgie.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Glassmorphic chip component for feature badges
 */
@Composable
fun GlassmorphicChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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

/**
 * Responsive glassmorphic chip
 */
@Composable
fun ResponsiveGlassmorphicChip(
    text: String,
    dimens: ResponsiveDimens,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = dimens.chipPadding, vertical = (dimens.chipPadding.value * 0.6f).dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFE6F1F0),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            fontSize = dimens.badgeTextSize.sp
        )
    }
}

/**
 * Glassmorphic card container
 */
@Composable
fun GlassmorphicCard(
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

/**
 * Responsive glassmorphic card container
 */
@Composable
fun ResponsiveGlassmorphicCard(
    modifier: Modifier = Modifier,
    dimens: ResponsiveDimens,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dimens.cornerRadius + 8.dp),
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

/**
 * Glassmorphic text field
 */
@Composable
fun GlassmorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier) {
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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            isError = isError,
            shape = RoundedCornerShape(14.dp),
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
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Responsive glassmorphic text field
 */
@Composable
fun ResponsiveGlassmorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    dimens: ResponsiveDimens,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = dimens.labelFontSize.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            placeholder = {
                Text(
                    placeholder,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = dimens.bodyFontSize.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(dimens.iconSize)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.inputHeight),
            isError = isError,
            shape = RoundedCornerShape(dimens.cornerRadius),
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
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.bodyFontSize.sp)
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFEF4444),
                fontSize = dimens.labelFontSize.sp,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Terms and Privacy agreement section
 */
@Composable
fun TermsAndPrivacyAgreement(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "By continuing you agree to ",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF666666)
        )
        Text(
            text = "Terms of Use",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onTermsClick)
        )
        Text(
            text = " and ",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF666666)
        )
        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onPrivacyClick)
        )
    }
}

/**
 * Responsive terms and privacy agreement
 */
@Composable
fun ResponsiveTermsAndPrivacyAgreement(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    dimens: ResponsiveDimens,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier
) {
    if (screenSize == ScreenSize.COMPACT) {
        // Stacked layout for small screens
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "By continuing you agree to",
                fontSize = 10.sp,
                color = Color(0xFF666666)
            )
            Row {
                Text(
                    text = "Terms",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onTermsClick)
                )
                Text(
                    text = " and ",
                    fontSize = 10.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "Privacy Policy",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onPrivacyClick)
                )
            }
        }
    } else {
        // Original inline layout for larger screens
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "By continuing you agree to ",
                fontSize = dimens.labelFontSize.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "Terms of Use",
                fontSize = dimens.labelFontSize.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onTermsClick)
            )
            Text(
                text = " and ",
                fontSize = dimens.labelFontSize.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "Privacy Policy",
                fontSize = dimens.labelFontSize.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onPrivacyClick)
            )
        }
    }
}

/**
 * Feature badge component
 */
@Composable
fun FeatureBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Security option card for PIN/Biometric selection
 */
@Composable
fun SecurityOptionCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isRecommended: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    secondaryIcon: ImageVector? = null
) {
    val cardColor = if (isSelected) {
        Color(0xFF0B1F2A).copy(alpha = 0.8f)
    } else {
        Color(0xFF0B1F2A).copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                Brush.linearGradient(colors = listOf(Color(0xFF0FAE96), Color(0xFF0B8F7A)))
            )
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6F1F0).copy(alpha = 0.2f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0)
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "★",
                            fontSize = 9.sp,
                            color = Color(0xFF0B1F2A),
                            fontWeight = FontWeight.Bold,
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

/**
 * Responsive security option card
 */
@Composable
fun ResponsiveSecurityOptionCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isRecommended: Boolean,
    accentColor: Color,
    dimens: ResponsiveDimens,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier,
    secondaryIcon: ImageVector? = null
) {
    val cardColor = if (isSelected) {
        Color(0xFF0B1F2A).copy(alpha = 0.8f)
    } else {
        Color(0xFF0B1F2A).copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(dimens.cornerRadius),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                Brush.linearGradient(colors = listOf(Color(0xFF0FAE96), Color(0xFF0B8F7A)))
            )
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6F1F0).copy(alpha = 0.2f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (screenSize == ScreenSize.COMPACT) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(if (screenSize == ScreenSize.COMPACT) 40.dp else 48.dp)
                    .background(
                        color = if (isSelected) Color(0xFF0FAE96).copy(alpha = 0.2f) else Color(0xFFE6F1F0).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(if (screenSize == ScreenSize.COMPACT) 10.dp else 12.dp)
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
                            modifier = Modifier.size(if (screenSize == ScreenSize.COMPACT) 14.dp else 18.dp)
                        )
                        Icon(
                            imageVector = secondaryIcon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0).copy(alpha = 0.7f),
                            modifier = Modifier.size(if (screenSize == ScreenSize.COMPACT) 14.dp else 18.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0).copy(alpha = 0.7f),
                        modifier = Modifier.size(dimens.iconSize)
                    )
                }
            }

            Spacer(modifier = Modifier.width(if (screenSize == ScreenSize.COMPACT) 10.dp else 14.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = if (screenSize == ScreenSize.COMPACT) 13.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF0FAE96) else Color(0xFFE6F1F0)
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "★",
                            fontSize = if (screenSize == ScreenSize.COMPACT) 8.sp else 9.sp,
                            color = Color(0xFF0B1F2A),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFC9A14A), Color(0xFFFFD166))
                                    ),
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = if (screenSize == ScreenSize.COMPACT) 10.sp else 12.sp,
                    color = Color(0xFFE6F1F0).copy(alpha = 0.6f)
                )
            }

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(if (screenSize == ScreenSize.COMPACT) 20.dp else 24.dp)
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
                        modifier = Modifier.size(if (screenSize == ScreenSize.COMPACT) 12.dp else 16.dp)
                    )
                }
            }
        }
    }
}

