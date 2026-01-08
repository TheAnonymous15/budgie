package com.example.budgie.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.budgie.data.api.ExchangeRateService
import com.example.budgie.data.preferences.Currency
import com.example.budgie.data.preferences.CurrencyManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Theme colors
private val ConverterNavy = Color(0xFF0A1628)
private val ConverterNavyLight = Color(0xFF1A2A44)
private val ConverterEmerald = Color(0xFF0FAE96)
private val ConverterSoftWhite = Color(0xFFF5F5F5)
private val ConverterGold = Color(0xFFFFD700)
private val ConverterBlue = Color(0xFF5C9CE5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currencyManager = remember { CurrencyManager.getInstance(context) }
    val exchangeRateService = remember { ExchangeRateService.getInstance(context) }
    val allCurrencies = CurrencyManager.currencies
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf("1") }
    var fromCurrency by remember { mutableStateOf(allCurrencies.find { it.code == "USD" }!!) }
    var toCurrency by remember { mutableStateOf(currencyManager.getSelectedCurrency()) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Live exchange rates
    var liveRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch live rates on launch
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            liveRates = exchangeRateService.getRates()
            lastUpdateTime = exchangeRateService.getLastUpdateTime()
            if (liveRates.isEmpty()) {
                errorMessage = "Using offline rates"
            }
        } catch (e: Exception) {
            errorMessage = "Using offline rates"
        }
        isLoading = false
    }

    // Calculate conversion using live rates or fallback to static rates
    val convertedAmount = remember(amount, fromCurrency, toCurrency, liveRates) {
        val inputAmount = amount.toDoubleOrNull() ?: 0.0

        if (liveRates.isNotEmpty()) {
            // Use live rates
            val fromRate = liveRates[fromCurrency.code] ?: fromCurrency.rateToUSD
            val toRate = liveRates[toCurrency.code] ?: toCurrency.rateToUSD
            inputAmount * (toRate / fromRate)
        } else {
            // Fallback to static rates
            val amountInUSD = inputAmount / fromCurrency.rateToUSD
            amountInUSD * toCurrency.rateToUSD
        }
    }

    val exchangeRate = remember(fromCurrency, toCurrency, liveRates) {
        if (liveRates.isNotEmpty()) {
            val fromRate = liveRates[fromCurrency.code] ?: fromCurrency.rateToUSD
            val toRate = liveRates[toCurrency.code] ?: toCurrency.rateToUSD
            toRate / fromRate
        } else {
            toCurrency.rateToUSD / fromCurrency.rateToUSD
        }
    }

    // Format last update time
    val lastUpdateText = remember(lastUpdateTime) {
        if (lastUpdateTime > 0) {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            "Updated: ${sdf.format(Date(lastUpdateTime))}"
        } else {
            "Using offline rates"
        }
    }

    FixedSizeDialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true
    )) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 650.dp),
            colors = CardDefaults.cardColors(containerColor = ConverterNavy),
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
                                    ConverterEmerald.copy(alpha = 0.4f),
                                    ConverterBlue.copy(alpha = 0.2f),
                                    ConverterGold.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(20.dp)
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
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(ConverterEmerald, ConverterBlue)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Currency Converter",
                                    color = ConverterSoftWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.dp,
                                            color = ConverterEmerald
                                        )
                                        Text(
                                            "Fetching live rates...",
                                            color = ConverterSoftWhite.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        Icon(
                                            if (errorMessage == null) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (errorMessage == null) ConverterEmerald else ConverterGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            if (errorMessage == null) "Live rates" else errorMessage!!,
                                            color = if (errorMessage == null) ConverterEmerald.copy(alpha = 0.8f) else ConverterGold.copy(alpha = 0.8f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Refresh Button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            liveRates = exchangeRateService.refreshRates()
                                            lastUpdateTime = System.currentTimeMillis()
                                            if (liveRates.isEmpty()) {
                                                errorMessage = "Using offline rates"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Refresh failed"
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ConverterSoftWhite.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh rates",
                                    tint = if (isLoading) ConverterSoftWhite.copy(alpha = 0.3f) else ConverterSoftWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Close Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ConverterSoftWhite.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = ConverterSoftWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Amount Input
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                amount = newValue
                            }
                        },
                        label = { Text("Amount") },
                        leadingIcon = {
                            Text(
                                fromCurrency.symbol,
                                color = ConverterEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConverterEmerald,
                            unfocusedBorderColor = ConverterSoftWhite.copy(alpha = 0.3f),
                            focusedTextColor = ConverterSoftWhite,
                            unfocusedTextColor = ConverterSoftWhite,
                            cursorColor = ConverterEmerald,
                            focusedLabelColor = ConverterEmerald,
                            unfocusedLabelColor = ConverterSoftWhite.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Currency Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // From Currency
                        CurrencySelector(
                            currency = fromCurrency,
                            label = "From",
                            onClick = { showFromPicker = true },
                            modifier = Modifier.weight(1f)
                        )

                        // Swap Button
                        IconButton(
                            onClick = {
                                val temp = fromCurrency
                                fromCurrency = toCurrency
                                toCurrency = temp
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ConverterEmerald.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Swap currencies",
                                tint = ConverterEmerald
                            )
                        }

                        // To Currency
                        CurrencySelector(
                            currency = toCurrency,
                            label = "To",
                            onClick = { showToPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Result Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ConverterNavyLight
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Converted Amount",
                                color = ConverterSoftWhite.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    toCurrency.flag,
                                    fontSize = 28.sp
                                )
                                Text(
                                    "${toCurrency.symbol}${"%.2f".format(convertedAmount)}",
                                    color = ConverterEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                )
                            }

                            Text(
                                toCurrency.name,
                                color = ConverterSoftWhite.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = ConverterSoftWhite.copy(alpha = 0.1f)
                            )

                            // Exchange Rate Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Exchange Rate",
                                    color = ConverterSoftWhite.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    "1 ${fromCurrency.code} = ${"%.4f".format(exchangeRate)} ${toCurrency.code}",
                                    color = ConverterSoftWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Last Update Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Source",
                                    color = ConverterSoftWhite.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (liveRates.isNotEmpty()) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (liveRates.isNotEmpty()) ConverterEmerald else ConverterGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        lastUpdateText,
                                        color = ConverterSoftWhite.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Quick Amount Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("100", "500", "1000", "5000").forEach { quickAmount ->
                            FilterChip(
                                selected = amount == quickAmount,
                                onClick = { amount = quickAmount },
                                label = { Text(quickAmount) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ConverterNavyLight,
                                    selectedContainerColor = ConverterEmerald.copy(alpha = 0.3f),
                                    labelColor = ConverterSoftWhite.copy(alpha = 0.7f),
                                    selectedLabelColor = ConverterEmerald
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Currency Picker Dialogs
    if (showFromPicker) {
        CurrencyPickerDialog(
            currencies = allCurrencies,
            selectedCurrency = fromCurrency,
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onCurrencySelect = {
                fromCurrency = it
                showFromPicker = false
                searchQuery = ""
            },
            onDismiss = {
                showFromPicker = false
                searchQuery = ""
            }
        )
    }

    if (showToPicker) {
        CurrencyPickerDialog(
            currencies = allCurrencies,
            selectedCurrency = toCurrency,
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onCurrencySelect = {
                toCurrency = it
                showToPicker = false
                searchQuery = ""
            },
            onDismiss = {
                showToPicker = false
                searchQuery = ""
            }
        )
    }
}

@Composable
private fun CurrencySelector(
    currency: Currency,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = ConverterNavyLight
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                color = ConverterSoftWhite.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                currency.flag,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                currency.code,
                color = ConverterSoftWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                currency.name,
                color = ConverterSoftWhite.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerDialog(
    currencies: List<Currency>,
    selectedCurrency: Currency,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCurrencySelect: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredCurrencies = remember(searchQuery, currencies) {
        if (searchQuery.isBlank()) {
            currencies
        } else {
            currencies.filter {
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    FixedSizeDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = ConverterNavy),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ConverterNavyLight)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Currency",
                            color = ConverterSoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ConverterSoftWhite
                            )
                        }
                    }
                }

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search currency...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = ConverterSoftWhite.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ConverterEmerald,
                        unfocusedBorderColor = ConverterSoftWhite.copy(alpha = 0.2f),
                        focusedTextColor = ConverterSoftWhite,
                        unfocusedTextColor = ConverterSoftWhite,
                        cursorColor = ConverterEmerald,
                        focusedPlaceholderColor = ConverterSoftWhite.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = ConverterSoftWhite.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Currency List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredCurrencies) { currency ->
                        CurrencyListItem(
                            currency = currency,
                            isSelected = currency.code == selectedCurrency.code,
                            onClick = { onCurrencySelect(currency) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyListItem(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                ConverterEmerald.copy(alpha = 0.2f)
            else
                ConverterNavyLight.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                currency.flag,
                fontSize = 28.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    currency.code,
                    color = if (isSelected) ConverterEmerald else ConverterSoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    currency.name,
                    color = ConverterSoftWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Text(
                currency.symbol,
                color = ConverterSoftWhite.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ConverterEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}



