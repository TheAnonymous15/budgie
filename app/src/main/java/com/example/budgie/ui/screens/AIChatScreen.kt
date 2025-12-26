package com.example.budgie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ai.conversational.*
import com.example.budgie.data.model.ExpenseCategory
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*

// Theme colors
private val WealthNavy = Color(0xFF0B1F2A)
private val WealthNavyLight = Color(0xFF0D2E3D)
private val WealthEmerald = Color(0xFF0FAE96)
private val WealthTeal = Color(0xFF0B8F7A)
private val WealthGold = Color(0xFFC9A14A)
private val WealthSoftWhite = Color(0xFFE6F1F0)
private val WealthMutedRed = Color(0xFFE57373)

// Chat message data class
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestions: List<String> = emptyList(),
    val intent: UserIntent? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsState()
    val incomes by viewModel.incomes.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val currentExpenses by viewModel.currentMonthExpenses.collectAsState()

    // Initialize Conversational AI
    val conversationalAI = remember { ConversationalAI(context) }

    // Create Financial Data Provider
    val financialDataProvider = remember(expenses, incomes, summary) {
        object : FinancialDataProvider {
            override fun getTotalExpenses(): Double = summary.totalExpenses
            override fun getTotalIncome(): Double = summary.totalIncome
            override fun getNetSavings(): Double = summary.netSavings
            override fun getSavingsRate(): Double = summary.savingsRate

            override fun getExpensesByCategory(): Map<String, Double> {
                return expenses.groupBy { it.category.displayName }
                    .mapValues { it.value.sumOf { exp -> exp.amount } }
            }

            override fun getTopSpendingCategory(): Pair<String, Double>? {
                return getExpensesByCategory().maxByOrNull { it.value }?.toPair()
            }

            override fun getExpenseCount(): Int = expenses.size

            override fun getRecentExpenses(limit: Int): List<Any> {
                return expenses.sortedByDescending { it.date }.take(limit)
            }

            override fun getMonthlyTrend(): Map<String, Double> {
                val calendar = Calendar.getInstance()
                return expenses.groupBy {
                    calendar.timeInMillis = it.date
                    "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}"
                }.mapValues { it.value.sumOf { exp -> exp.amount } }
            }

            override fun getBudgetStatus(): Map<String, Any> {
                return mapOf(
                    "totalBudget" to (summary.totalIncome * 0.8),
                    "spent" to summary.totalExpenses,
                    "remaining" to (summary.totalIncome * 0.8 - summary.totalExpenses)
                )
            }

            override fun getGoalsProgress(): Map<String, Any> {
                return mapOf("message" to "No active goals yet")
            }

            override fun getLoansStatus(): Map<String, Any> {
                return mapOf("message" to "No active loans")
            }
        }
    }

    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isTyping by remember { mutableStateOf(false) }
    var suggestedQuestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Initialize with welcome message
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            val welcomeMessage = buildString {
                append("👋 **Hi! I'm Budgie AI**\n\n")
                append("Your intelligent financial advisor powered by:\n")
                append("• 🧠 Intent Classification\n")
                append("• 📊 Entity Extraction\n")
                append("• 💡 Financial ML Pipeline\n")
                append("• 🔒 Rule-based Reasoning\n\n")
                append("**Ask me about:**\n")
                append("• \"How much did I spend this month?\"\n")
                append("• \"What's my biggest expense category?\"\n")
                append("• \"Am I overspending anywhere?\"\n")
                append("• \"Show my spending trends\"\n")
                append("• \"Give me financial advice\"")
            }
            messages = listOf(
                ChatMessage(
                    content = welcomeMessage,
                    isUser = false,
                    suggestions = listOf(
                        "Show my spending",
                        "What are my trends?",
                        "Am I overspending?"
                    )
                )
            )
            suggestedQuestions = listOf(
                "How much did I spend?",
                "Show my income",
                "Financial insights"
            )
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Send message function
    fun sendMessage(text: String = userInput) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(content = text.trim(), isUser = true)
        messages = messages + userMessage
        val query = text
        userInput = ""
        focusManager.clearFocus()
        suggestedQuestions = emptyList()

        scope.launch {
            isTyping = true

            conversationalAI.processInput(query, financialDataProvider)
                .onEach { response ->
                    val aiMessage = ChatMessage(
                        content = response.text,
                        isUser = false,
                        suggestions = response.suggestions,
                        intent = response.intent
                    )
                    messages = messages + aiMessage
                    suggestedQuestions = response.suggestions
                }
                .catch { e ->
                    val errorMessage = ChatMessage(
                        content = "I encountered an issue processing that. Could you try rephrasing?",
                        isUser = false
                    )
                    messages = messages + errorMessage
                }
                .collect()

            isTyping = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(WealthNavy, WealthNavyLight, WealthNavy)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WealthNavy,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WealthSoftWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // AI Avatar with pulse animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        WealthEmerald.copy(alpha = pulseAlpha),
                                        WealthTeal
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            "Budgie AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WealthSoftWhite
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(WealthEmerald)
                            )
                            Text(
                                "Multimodal Financial Advisor",
                                style = MaterialTheme.typography.labelSmall,
                                color = WealthSoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Reset conversation button
                    IconButton(onClick = {
                        conversationalAI.resetConversation()
                        messages = emptyList()
                        suggestedQuestions = emptyList()
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = WealthSoftWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        onSuggestionClick = { sendMessage(it) }
                    )
                }

                // Typing indicator
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Quick suggestion chips
            if (suggestedQuestions.isNotEmpty() && !isTyping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedQuestions.take(3).forEach { suggestion ->
                        SuggestionChip(
                            onClick = { sendMessage(suggestion) },
                            label = {
                                Text(
                                    suggestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = WealthNavyLight,
                                labelColor = WealthEmerald
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = WealthEmerald.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Input Field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WealthNavy,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(WealthNavyLight)
                        .border(
                            1.dp,
                            WealthEmerald.copy(alpha = 0.3f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        placeholder = {
                            Text(
                                "Ask about your finances...",
                                color = WealthSoftWhite.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = WealthEmerald,
                            focusedTextColor = WealthSoftWhite,
                            unfocusedTextColor = WealthSoftWhite
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                        singleLine = true
                    )

                    // Send button with animation
                    val sendButtonScale by animateFloatAsState(
                        targetValue = if (userInput.isNotBlank()) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f),
                        label = "send_scale"
                    )

                    IconButton(
                        onClick = { sendMessage() },
                        enabled = userInput.isNotBlank(),
                        modifier = Modifier.scale(sendButtonScale)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (userInput.isNotBlank()) WealthEmerald else WealthSoftWhite.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onSuggestionClick: (String) -> Unit = {}
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // AI Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(WealthEmerald, WealthTeal)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) WealthEmerald
                        else WealthNavyLight
                    )
                    .border(
                        1.dp,
                        if (isUser) WealthEmerald else WealthSoftWhite.copy(alpha = 0.1f),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isUser) Color.White else WealthSoftWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(WealthGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = WealthGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Show inline suggestions for AI messages
        if (!isUser && message.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(start = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.suggestions.take(2).forEach { suggestion ->
                    AssistChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            Text(
                                suggestion,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = WealthNavy,
                            labelColor = WealthEmerald.copy(alpha = 0.9f)
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = WealthEmerald.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Show intent badge for debugging (optional)
        if (!isUser && message.intent != null) {
            Text(
                text = "Intent: ${message.intent.name}",
                style = MaterialTheme.typography.labelSmall,
                color = WealthSoftWhite.copy(alpha = 0.3f),
                modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(WealthEmerald, WealthTeal)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(WealthNavyLight)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 200)
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WealthEmerald.copy(alpha = alpha))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            "Analyzing...",
            style = MaterialTheme.typography.labelSmall,
            color = WealthSoftWhite.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

