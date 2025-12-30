package com.example.budgie.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ai.ConversationalAI
import com.example.budgie.ai.ChatResponse
import kotlinx.coroutines.launch
import java.util.*

/**
 * AI Chat Screen - Uses Learner Engine Data
 *
 * Architecture (from ai.txt):
 * - Chatbot does NOT generate its own data
 * - Uses structured data from FinancialLearnerEngine
 * - Shows suggested question chips
 * - Friendly, max 3 bullets, emojis sparingly
 * - 100% private - all on device
 */

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
    val isStreaming: Boolean = false
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

    // Initialize ConversationalAI (thin interface to FinancialLearner)
    val conversationalAI = remember { ConversationalAI.getInstance(context) }
    var engineStatus by remember { mutableStateOf("Initializing...") }
    var isEngineReady by remember { mutableStateOf(false) }

    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isTyping by remember { mutableStateOf(false) }
    var currentStreamingContent by remember { mutableStateOf("") }
    var suggestedQuestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Initialize AI
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                conversationalAI.initialize()
                isEngineReady = true
                engineStatus = "Ready (On-Device)"
            } catch (_: Exception) {
                isEngineReady = true
                engineStatus = "Ready (Fallback)"
            }
        }
    }

    // Initialize with welcome message
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            val welcomeMessage = buildString {
                append("**Hi! I'm Budgie, your financial companion.**\n\n")
                append("I use what I've learned from your financial data to help you:\n\n")
                append("• Understand your spending patterns\n")
                append("• Track bills and savings\n")
                append("• Get personalized insights\n\n")
                append("🔒 100% private - everything stays on your device.\n\n")
                append("What would you like to know?")
            }
            messages = listOf(
                ChatMessage(
                    content = welcomeMessage,
                    isUser = false,
                    suggestions = listOf(
                        "How am I doing?",
                        "My spending this month",
                        "Upcoming bills"
                    )
                )
            )
            suggestedQuestions = listOf(
                "Can I afford this?",
                "Help me save more",
                "My top categories"
            )
        }
    }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Small delay to ensure the item is rendered
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-scroll when message content updates (after response generation)
    LaunchedEffect(messages.lastOrNull()?.content, currentStreamingContent) {
        if (messages.isNotEmpty()) {
            kotlinx.coroutines.delay(50)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-scroll when typing indicator appears/disappears
    LaunchedEffect(isTyping) {
        if (messages.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Send message function using ConversationalAI
    fun sendMessage(text: String = userInput) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(content = text.trim(), isUser = true)
        messages = messages + userMessage

        // Scroll after user message
        scope.launch {
            kotlinx.coroutines.delay(50)
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        val query = text
        userInput = ""
        focusManager.clearFocus()
        suggestedQuestions = emptyList()
        currentStreamingContent = ""

        scope.launch {
            isTyping = true

            // Add placeholder for response
            val responseMessageId = UUID.randomUUID().toString()
            messages = messages + ChatMessage(
                id = responseMessageId,
                content = "Thinking...",
                isUser = false,
                isStreaming = true
            )

            // Scroll to show thinking indicator
            kotlinx.coroutines.delay(50)
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }

            try {
                // Use ConversationalAI - it handles everything:
                // Language detection → Translation → Intent understanding →
                // Query Learner → Format response → Translate back
                val response = conversationalAI.processMessage(query)

                // Update with actual response
                messages = messages.map { msg ->
                    if (msg.id == responseMessageId) {
                        msg.copy(
                            content = response.content,
                            isStreaming = false,
                            suggestions = response.suggestions
                        )
                    } else msg
                }

                suggestedQuestions = response.suggestions

                // Scroll to show the complete response
                kotlinx.coroutines.delay(100)
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }

            } catch (_: Exception) {
                messages = messages.map { msg ->
                    if (msg.id == responseMessageId) {
                        msg.copy(
                            content = "I apologize, something went wrong. Please try again.",
                            isStreaming = false
                        )
                    } else msg
                }

                // Scroll after error message too
                kotlinx.coroutines.delay(100)
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            isTyping = false
            currentStreamingContent = ""

            // Final scroll to ensure we're at the bottom
            kotlinx.coroutines.delay(50)
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding() // Prevent overlap with system navigation buttons
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
                                    .background(if (isEngineReady) WealthEmerald else WealthGold)
                            )
                            Text(
                                engineStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = WealthSoftWhite.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Reset conversation button
                    IconButton(onClick = {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp), // Add bottom padding to prevent overlapping with nav buttons
                color = WealthNavy,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 8.dp) // Extra padding at bottom
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

        // Show streaming indicator
        if (!isUser && message.isStreaming) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(start = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = WealthEmerald
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Thinking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = WealthSoftWhite.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
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

// Generate contextual suggestions based on query - matching ai.txt style
private fun generateSuggestions(lastQuery: String): List<String> {
    val query = lastQuery.lowercase()
    return when {
        query.contains("spend") || query.contains("expense") ->
            listOf("My top categories", "Am I overspending?", "Tips to reduce")
        query.contains("save") || query.contains("saving") ->
            listOf("How to save more?", "Am I on track?", "50/30/20 rule")
        query.contains("budget") ->
            listOf("Budget status", "Adjust my budget", "Budget tips")
        query.contains("bill") || query.contains("due") ->
            listOf("All upcoming bills", "Payment reminders", "Bill strategies")
        query.contains("income") || query.contains("earn") ->
            listOf("My income breakdown", "Savings rate", "Can I afford more?")
        query.contains("risk") || query.contains("safe") ->
            listOf("How to improve?", "Build emergency fund", "Reduce expenses")
        query.contains("predict") || query.contains("forecast") ->
            listOf("End of month balance", "Next week outlook", "Budget alert")
        query.contains("loan") || query.contains("debt") ->
            listOf("Pay off faster", "Loan vs savings", "Interest tips")
        query.contains("goal") ->
            listOf("Create new goal", "Track progress", "Goal strategies")
        query.contains("yes") || query.contains("sure") || query.contains("okay") ->
            listOf("Show me details", "More tips", "What else?")
        query.contains("hello") || query.contains("hi") || query.contains("hey") ->
            listOf("How am I doing?", "Predict my bills", "Help me save")
        query.contains("track") || query.contains("status") ->
            listOf("Detailed breakdown", "Compare to last month", "Set targets")
        else ->
            listOf("How am I doing?", "Predict my bills", "Help me save")
    }
}
