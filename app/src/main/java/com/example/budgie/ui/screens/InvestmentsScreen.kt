package com.example.budgie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.budgie.data.model.*
import com.example.budgie.ui.viewmodel.MainViewModel

// Premium Colors
private val WealthNavy = Color(0xFF0B1F2A)
private val WealthEmerald = Color(0xFF0FAE96)
private val WealthGold = Color(0xFFC9A14A)
private val WealthSoftWhite = Color(0xFFE6F1F0)
private val WealthMutedRed = Color(0xFFE57373)
private val WealthAmber = Color(0xFFFFB74D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val investmentSuggestions by viewModel.investmentSuggestions.collectAsState()

    Scaffold(
        containerColor = WealthNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Investment Ideas",
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WealthSoftWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = WealthEmerald.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WealthEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = WealthEmerald,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Smart Investments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WealthSoftWhite
                            )
                            Text(
                                text = "Personalized suggestions based on your profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = WealthSoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (investmentSuggestions.isEmpty()) {
                item {
                    EmptyInvestmentsState()
                }
            } else {
                items(investmentSuggestions) { suggestion ->
                    InvestmentCard(suggestion = suggestion)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun InvestmentCard(suggestion: InvestmentSuggestion) {
    val riskColor = when (suggestion.riskLevel) {
        RiskLevel.LOW -> WealthEmerald
        RiskLevel.MEDIUM -> WealthAmber
        RiskLevel.HIGH -> WealthMutedRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = suggestion.type.icon,
                        fontSize = 32.sp
                    )
                    Column {
                        Text(
                            text = suggestion.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        Text(
                            text = suggestion.type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = WealthSoftWhite.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = suggestion.riskLevel.displayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = WealthSoftWhite.copy(alpha = 0.8f)
            )

            Divider(color = WealthSoftWhite.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Expected Return",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        text = suggestion.expectedReturn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthEmerald
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Minimum",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "$${String.format("%.0f", suggestion.minimumAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthSoftWhite
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Suitability",
                        style = MaterialTheme.typography.labelSmall,
                        color = WealthSoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${suggestion.suitabilityScore}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WealthGold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInvestmentsState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = WealthEmerald.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Start Your Investment Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WealthSoftWhite
            )
            Text(
                text = "Add your income and build your emergency fund first. We'll suggest investment options tailored to your financial situation.",
                style = MaterialTheme.typography.bodyMedium,
                color = WealthSoftWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

