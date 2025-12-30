package com.example.budgie.ai

import android.content.Context
import android.util.Log
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.*
import com.example.budgie.data.preferences.UserPreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * SuperBrain - The Central Intelligence System
 *
 * This is the MASTER knowledge hub that has REAL-TIME access to ALL data
 * across the entire Budgie app. It provides instant answers with full details.
 *
 * Architecture:
 * - Real-time data sync from all database tables
 * - Comprehensive state tracking
 * - Intent detection and intelligent routing
 * - Detailed response generation with actual data
 * - Prediction and forecasting capabilities
 *
 * The SuperBrain knows EVERYTHING about the user's finances.
 */

private const val TAG = "SuperBrain"

/**
 * Complete system knowledge state - ALL data in one place
 */
data class SystemKnowledge(
    // User Info
    val userName: String = "",
    val userBirthday: String = "",
    val userAge: Int = 0,

    // Income Data
    val incomes: List<IncomeRecord> = emptyList(),
    val totalIncome: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val incomeBySource: Map<String, Double> = emptyMap(),

    // Expense Data
    val expenses: List<ExpenseRecord> = emptyList(),
    val totalExpenses: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val dailyAverageExpense: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val expensesByDay: Map<String, Double> = emptyMap(),

    // Bills Data
    val bills: List<BillRecord> = emptyList(),
    val totalBillsAmount: Double = 0.0,
    val paidBills: List<BillRecord> = emptyList(),
    val unpaidBills: List<BillRecord> = emptyList(),
    val upcomingBills: List<BillRecord> = emptyList(),
    val overdueBills: List<BillRecord> = emptyList(),

    // Loans Data
    val loans: List<LoanRecord> = emptyList(),
    val totalLoanAmount: Double = 0.0,
    val totalLoanPaid: Double = 0.0,
    val totalLoanRemaining: Double = 0.0,
    val activeLoans: List<LoanRecord> = emptyList(),
    val completedLoans: List<LoanRecord> = emptyList(),

    // Goals Data
    val goals: List<GoalRecord> = emptyList(),
    val activeGoals: List<GoalRecord> = emptyList(),
    val completedGoals: List<GoalRecord> = emptyList(),
    val totalGoalTarget: Double = 0.0,
    val totalGoalSaved: Double = 0.0,

    // Budget Data
    val budgets: List<BudgetRecord> = emptyList(),
    val totalBudget: Double = 0.0,
    val budgetUtilization: Double = 0.0,
    val overBudgetCategories: List<String> = emptyList(),

    // Shopping Lists
    val shoppingLists: List<ShoppingListRecord> = emptyList(),
    val totalShoppingBudget: Double = 0.0,

    // Calculated Metrics
    val netWorth: Double = 0.0,
    val savingsRate: Double = 0.0,
    val debtToIncomeRatio: Double = 0.0,
    val financialHealthScore: Int = 0,
    val riskLevel: String = "Unknown",

    // Trends & Predictions
    val spendingTrend: String = "Stable",
    val predictedNextMonthSpending: Double = 0.0,
    val predictedSavings: Double = 0.0,

    // Metadata
    val lastUpdated: Long = System.currentTimeMillis()
)

// Data records for comprehensive storage
data class IncomeRecord(
    val id: Long,
    val amount: Double,
    val source: String,
    val description: String,
    val date: Long,
    val isRecurring: Boolean
)

data class ExpenseRecord(
    val id: Long,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long,
    val merchant: String = ""
)

data class BillRecord(
    val id: Long,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean,
    val isRecurring: Boolean,
    val category: String = "",
    val daysUntilDue: Int = 0
)

data class LoanRecord(
    val id: Long,
    val title: String,
    val principalAmount: Double,
    val totalAmount: Double,
    val amountPaid: Double,
    val amountRemaining: Double,
    val interestRate: Double,
    val monthlyPayment: Double,
    val lender: String,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean,
    val paymentsLeft: Int
)

data class GoalRecord(
    val id: Long,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val progress: Double,
    val deadline: Long,
    val category: String,
    val isCompleted: Boolean,
    val daysRemaining: Int
)

data class BudgetRecord(
    val id: Long,
    val category: String,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val utilizationPercent: Double,
    val isOverBudget: Boolean
)

data class ShoppingListRecord(
    val id: Long,
    val title: String,
    val totalBudget: Double,
    val itemCount: Int,
    val items: List<String>,
    val date: Long
)

/**
 * SuperBrain - The Central Intelligence System
 */
class SuperBrain private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val database = BudgieDatabase.getDatabase(context)
    private val preferencesManager = UserPreferencesManager.getInstance(context)

    // Live knowledge state
    private val _knowledge = MutableStateFlow(SystemKnowledge())
    val knowledge: StateFlow<SystemKnowledge> = _knowledge.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    companion object {
        @Volatile
        private var INSTANCE: SuperBrain? = null

        fun getInstance(context: Context): SuperBrain {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SuperBrain(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Initialize the SuperBrain - call on app start
     */
    suspend fun initialize() {
        Log.d(TAG, "SuperBrain initializing...")
        refreshKnowledge()
        startRealTimeSync()
        Log.d(TAG, "SuperBrain initialized with full system knowledge")
    }

    /**
     * Start real-time data synchronization
     */
    private fun startRealTimeSync() {
        // Refresh knowledge every 30 seconds and on data changes
        scope.launch {
            while (isActive) {
                delay(30_000) // 30 seconds
                refreshKnowledge()
            }
        }
    }

    /**
     * Refresh all knowledge from database
     */
    suspend fun refreshKnowledge() = withContext(Dispatchers.IO) {
        try {
            val userProfile = preferencesManager.userProfile.first()
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis

            // Get date ranges
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis

            // Load all data
            val incomes = loadIncomes()
            val expenses = loadExpenses()
            val bills = loadBills(today)
            val loans = loadLoans()
            val goals = loadGoals(today)
            val budgets = loadBudgets(expenses, monthStart, monthEnd)
            val shoppingLists = loadShoppingLists()

            // Calculate metrics
            val monthlyIncome = incomes.filter { it.date >= monthStart && it.date < monthEnd }.sumOf { it.amount }
            val monthlyExpenses = expenses.filter { it.date >= monthStart && it.date < monthEnd }.sumOf { it.amount }
            val totalLoanRemaining = loans.filter { it.isActive }.sumOf { it.amountRemaining }

            val savingsRate = if (monthlyIncome > 0) {
                ((monthlyIncome - monthlyExpenses) / monthlyIncome * 100).coerceIn(0.0, 100.0)
            } else 0.0

            val debtToIncomeRatio = if (monthlyIncome > 0) {
                (totalLoanRemaining / (monthlyIncome * 12) * 100)
            } else 0.0

            val healthScore = calculateHealthScore(monthlyIncome, monthlyExpenses, savingsRate, debtToIncomeRatio)
            val riskLevel = calculateRiskLevel(savingsRate, debtToIncomeRatio)

            // Update knowledge state
            _knowledge.value = SystemKnowledge(
                // User Info
                userName = userProfile?.name ?: "User",
                userBirthday = userProfile?.birthday ?: "",
                userAge = calculateAge(userProfile?.birthday),

                // Income
                incomes = incomes,
                totalIncome = incomes.sumOf { it.amount },
                monthlyIncome = monthlyIncome,
                incomeBySource = incomes.groupBy { it.source }.mapValues { it.value.sumOf { i -> i.amount } },

                // Expenses
                expenses = expenses,
                totalExpenses = expenses.sumOf { it.amount },
                monthlyExpenses = monthlyExpenses,
                dailyAverageExpense = if (expenses.isNotEmpty()) monthlyExpenses / 30 else 0.0,
                expensesByCategory = expenses.filter { it.date >= monthStart }
                    .groupBy { it.category }
                    .mapValues { it.value.sumOf { e -> e.amount } },
                expensesByDay = calculateDailyExpenses(expenses),

                // Bills
                bills = bills,
                totalBillsAmount = bills.sumOf { it.amount },
                paidBills = bills.filter { it.isPaid },
                unpaidBills = bills.filter { !it.isPaid },
                upcomingBills = bills.filter { !it.isPaid && it.daysUntilDue >= 0 }.sortedBy { it.daysUntilDue },
                overdueBills = bills.filter { !it.isPaid && it.daysUntilDue < 0 },

                // Loans
                loans = loans,
                totalLoanAmount = loans.sumOf { it.totalAmount },
                totalLoanPaid = loans.sumOf { it.amountPaid },
                totalLoanRemaining = totalLoanRemaining,
                activeLoans = loans.filter { it.isActive },
                completedLoans = loans.filter { !it.isActive },

                // Goals
                goals = goals,
                activeGoals = goals.filter { !it.isCompleted },
                completedGoals = goals.filter { it.isCompleted },
                totalGoalTarget = goals.sumOf { it.targetAmount },
                totalGoalSaved = goals.sumOf { it.currentAmount },

                // Budget
                budgets = budgets,
                totalBudget = budgets.sumOf { it.limit },
                budgetUtilization = if (budgets.isNotEmpty()) budgets.map { it.utilizationPercent }.average() else 0.0,
                overBudgetCategories = budgets.filter { it.isOverBudget }.map { it.category },

                // Shopping Lists
                shoppingLists = shoppingLists,
                totalShoppingBudget = shoppingLists.sumOf { it.totalBudget },

                // Metrics
                netWorth = monthlyIncome - monthlyExpenses - totalLoanRemaining,
                savingsRate = savingsRate,
                debtToIncomeRatio = debtToIncomeRatio,
                financialHealthScore = healthScore,
                riskLevel = riskLevel,

                // Predictions
                spendingTrend = detectSpendingTrend(expenses),
                predictedNextMonthSpending = predictNextMonthSpending(expenses),
                predictedSavings = monthlyIncome - predictNextMonthSpending(expenses),

                lastUpdated = System.currentTimeMillis()
            )

            Log.d(TAG, "Knowledge refreshed: ${incomes.size} incomes, ${expenses.size} expenses, ${loans.size} loans")

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing knowledge: ${e.message}", e)
        }
    }

    // Data loading functions
    private suspend fun loadIncomes(): List<IncomeRecord> {
        return try {
            database.incomeDao().getAllIncomesOnce().map { income ->
                IncomeRecord(
                    id = income.id,
                    amount = income.amount,
                    source = income.source.displayName,
                    description = income.title,
                    date = income.date,
                    isRecurring = income.isRecurring
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading incomes: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadExpenses(): List<ExpenseRecord> {
        return try {
            database.expenseDao().getAllExpensesOnce().map { expense ->
                ExpenseRecord(
                    id = expense.id,
                    amount = expense.amount,
                    category = expense.category.displayName,
                    description = expense.notes,
                    date = expense.date,
                    merchant = expense.title
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading expenses: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadBills(today: Long): List<BillRecord> {
        return try {
            database.billDao().getAllBillsOnce().map { bill ->
                val daysUntilDue = ((bill.dueDate - today) / (24 * 60 * 60 * 1000)).toInt()
                BillRecord(
                    id = bill.id,
                    title = bill.title,
                    amount = bill.amount,
                    dueDate = bill.dueDate,
                    isPaid = bill.isPaid,
                    isRecurring = bill.isRecurring,
                    category = bill.category?.displayName ?: "Other",
                    daysUntilDue = daysUntilDue
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bills: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadLoans(): List<LoanRecord> {
        return try {
            database.loanDao().getAllLoans().first().map { loan ->
                val amountRemaining = loan.remainingAmount
                val monthlyPayment = loan.monthlyPayment
                val paymentsLeft = loan.remainingPayments

                LoanRecord(
                    id = loan.id,
                    title = loan.title,
                    principalAmount = loan.principalAmount,
                    totalAmount = loan.totalAmount,
                    amountPaid = loan.amountPaid,
                    amountRemaining = amountRemaining,
                    interestRate = loan.interestRate,
                    monthlyPayment = monthlyPayment,
                    lender = loan.lenderName,
                    startDate = loan.startDate,
                    endDate = loan.endDate,
                    isActive = loan.status == LoanStatus.ACTIVE,
                    paymentsLeft = paymentsLeft
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading loans: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadGoals(today: Long): List<GoalRecord> {
        return try {
            database.goalDao().getAllGoals().first().map { goal ->
                val daysRemaining = ((goal.targetDate - today) / (24 * 60 * 60 * 1000)).toInt()

                GoalRecord(
                    id = goal.id,
                    title = goal.title,
                    targetAmount = goal.targetAmount,
                    currentAmount = goal.currentAmount,
                    progress = goal.progressPercentage,
                    deadline = goal.targetDate,
                    category = goal.category.displayName,
                    isCompleted = goal.isCompleted,
                    daysRemaining = daysRemaining
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading goals: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadBudgets(expenses: List<ExpenseRecord>, monthStart: Long, monthEnd: Long): List<BudgetRecord> {
        return try {
            val monthlyExpenses = expenses.filter { it.date >= monthStart && it.date < monthEnd }

            database.budgetDao().getAllBudgets().first().map { budget ->
                val spent = monthlyExpenses.filter { it.category == budget.category.displayName }.sumOf { it.amount }
                val remaining = budget.limit - spent
                val utilization = if (budget.limit > 0) (spent / budget.limit * 100) else 0.0

                BudgetRecord(
                    id = budget.id,
                    category = budget.category.displayName,
                    limit = budget.limit,
                    spent = spent,
                    remaining = remaining,
                    utilizationPercent = utilization,
                    isOverBudget = spent > budget.limit
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading budgets: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadShoppingLists(): List<ShoppingListRecord> {
        return try {
            database.shoppingDao().getAllShoppingListsWithItems().first().map { listWithItems ->
                ShoppingListRecord(
                    id = listWithItems.shoppingList.id,
                    title = listWithItems.shoppingList.title,
                    totalBudget = listWithItems.shoppingList.totalBudget,
                    itemCount = listWithItems.items.size,
                    items = listWithItems.items.map { "${it.name} - $${String.format("%.2f", it.estimatedPrice)}" },
                    date = listWithItems.shoppingList.createdAt
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading shopping lists: ${e.message}")
            emptyList()
        }
    }

    // Helper functions
    private fun calculateAge(birthday: String?): Int {
        if (birthday.isNullOrEmpty()) return 0
        return try {
            val parts = birthday.split("/", "-")
            if (parts.size >= 3) {
                val birthYear = parts[0].toIntOrNull() ?: return 0
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                currentYear - birthYear
            } else 0
        } catch (e: Exception) { 0 }
    }

    private fun calculateHealthScore(income: Double, expenses: Double, savingsRate: Double, debtRatio: Double): Int {
        var score = 50 // Base score

        // Savings rate impact (0-30 points)
        score += when {
            savingsRate >= 30 -> 30
            savingsRate >= 20 -> 25
            savingsRate >= 10 -> 15
            savingsRate >= 5 -> 10
            else -> 0
        }

        // Debt ratio impact (-20 to +20 points)
        score += when {
            debtRatio <= 10 -> 20
            debtRatio <= 20 -> 15
            debtRatio <= 30 -> 10
            debtRatio <= 50 -> 0
            else -> -20
        }

        return score.coerceIn(0, 100)
    }

    private fun calculateRiskLevel(savingsRate: Double, debtRatio: Double): String {
        val riskScore = (100 - savingsRate) * 0.5 + debtRatio * 0.5
        return when {
            riskScore < 20 -> "Low Risk"
            riskScore < 40 -> "Moderate Risk"
            riskScore < 60 -> "High Risk"
            else -> "Critical Risk"
        }
    }

    private fun detectSpendingTrend(expenses: List<ExpenseRecord>): String {
        if (expenses.size < 14) return "Stable"

        val twoWeeksAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
        val oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)

        val week1 = expenses.filter { it.date in twoWeeksAgo until oneWeekAgo }.sumOf { it.amount }
        val week2 = expenses.filter { it.date >= oneWeekAgo }.sumOf { it.amount }

        val change = if (week1 > 0) ((week2 - week1) / week1 * 100) else 0.0

        return when {
            change > 20 -> "Increasing"
            change < -20 -> "Decreasing"
            abs(change) > 30 -> "Volatile"
            else -> "Stable"
        }
    }

    private fun predictNextMonthSpending(expenses: List<ExpenseRecord>): Double {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentExpenses = expenses.filter { it.date >= thirtyDaysAgo }
        return recentExpenses.sumOf { it.amount } * 1.05 // 5% buffer
    }

    private fun calculateDailyExpenses(expenses: List<ExpenseRecord>): Map<String, Double> {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        return expenses.filter { it.date >= sevenDaysAgo }
            .groupBy { shortDateFormat.format(Date(it.date)) }
            .mapValues { it.value.sumOf { e -> e.amount } }
    }

    // ==================== QUERY FUNCTIONS ====================

    /**
     * Answer ANY question about the user's finances
     */
    fun answerQuery(query: String): String {
        val q = query.lowercase()
        val k = _knowledge.value

        return when {
            // Loan queries
            q.contains("loan") || q.contains("debt") -> answerLoanQuery(q, k)

            // Goal queries
            q.contains("goal") || q.contains("target") || q.contains("saving for") -> answerGoalQuery(q, k)

            // Bill queries
            q.contains("bill") || q.contains("due") || q.contains("payment") -> answerBillQuery(q, k)

            // Income queries
            q.contains("income") || q.contains("earn") || q.contains("salary") -> answerIncomeQuery(q, k)

            // Expense queries
            q.contains("spent") || q.contains("spending") || q.contains("expense") -> answerExpenseQuery(q, k)

            // Budget queries
            q.contains("budget") -> answerBudgetQuery(q, k)

            // Savings queries
            q.contains("saving") || q.contains("save") -> answerSavingsQuery(q, k)

            // Shopping list queries
            q.contains("shopping") || q.contains("list") -> answerShoppingQuery(q, k)

            // Health/Status queries
            q.contains("health") || q.contains("score") || q.contains("status") || q.contains("how am i") -> answerHealthQuery(k)

            // Prediction queries
            q.contains("predict") || q.contains("forecast") || q.contains("next month") -> answerPredictionQuery(k)

            // General overview
            q.contains("overview") || q.contains("summary") || q.contains("snapshot") -> answerOverviewQuery(k)

            else -> answerGeneralQuery(q, k)
        }
    }

    private fun answerLoanQuery(query: String, k: SystemKnowledge): String {
        if (k.loans.isEmpty()) {
            return "You don't have any loans recorded. Would you like to add one?"
        }

        val sb = StringBuilder()
        sb.appendLine("📋 **Your Loan Details:**\n")

        if (k.activeLoans.isNotEmpty()) {
            sb.appendLine("**Active Loans (${k.activeLoans.size}):**")
            k.activeLoans.forEach { loan ->
                sb.appendLine("• **${loan.title}** from ${loan.lender}")
                sb.appendLine("  Principal: $${String.format("%.2f", loan.principalAmount)}")
                sb.appendLine("  Total Payable: $${String.format("%.2f", loan.totalAmount)}")
                sb.appendLine("  Paid: $${String.format("%.2f", loan.amountPaid)}")
                sb.appendLine("  Remaining: $${String.format("%.2f", loan.amountRemaining)}")
                sb.appendLine("  Monthly Payment: $${String.format("%.2f", loan.monthlyPayment)}")
                sb.appendLine("  Interest Rate: ${loan.interestRate}%")
                sb.appendLine("  Payments Left: ${loan.paymentsLeft}")
                sb.appendLine()
            }
        }

        sb.appendLine("**Summary:**")
        sb.appendLine("• Total Loan Amount: $${String.format("%.2f", k.totalLoanAmount)}")
        sb.appendLine("• Total Paid: $${String.format("%.2f", k.totalLoanPaid)}")
        sb.appendLine("• Total Remaining: $${String.format("%.2f", k.totalLoanRemaining)}")

        if (k.completedLoans.isNotEmpty()) {
            sb.appendLine("\n✅ Completed Loans: ${k.completedLoans.size}")
        }

        return sb.toString()
    }

    private fun answerGoalQuery(query: String, k: SystemKnowledge): String {
        if (k.goals.isEmpty()) {
            return "You haven't set any financial goals yet. Would you like to create one?"
        }

        val sb = StringBuilder()
        sb.appendLine("🎯 **Your Financial Goals:**\n")

        if (k.activeGoals.isNotEmpty()) {
            sb.appendLine("**Active Goals (${k.activeGoals.size}):**")
            k.activeGoals.forEach { goal ->
                val progressBar = createProgressBar(goal.progress.toInt())
                sb.appendLine("• **${goal.title}**")
                sb.appendLine("  Target: $${String.format("%.2f", goal.targetAmount)}")
                sb.appendLine("  Saved: $${String.format("%.2f", goal.currentAmount)}")
                sb.appendLine("  Progress: $progressBar ${goal.progress.toInt()}%")
                sb.appendLine("  Days Remaining: ${goal.daysRemaining}")
                sb.appendLine()
            }
        }

        if (k.completedGoals.isNotEmpty()) {
            sb.appendLine("✅ **Completed Goals: ${k.completedGoals.size}**")
            k.completedGoals.forEach { goal ->
                sb.appendLine("• ${goal.title} - $${String.format("%.2f", goal.targetAmount)}")
            }
        }

        sb.appendLine("\n**Summary:**")
        sb.appendLine("• Total Target: $${String.format("%.2f", k.totalGoalTarget)}")
        sb.appendLine("• Total Saved: $${String.format("%.2f", k.totalGoalSaved)}")

        return sb.toString()
    }

    private fun answerBillQuery(query: String, k: SystemKnowledge): String {
        if (k.bills.isEmpty()) {
            return "You don't have any bills recorded. Would you like to add one?"
        }

        val sb = StringBuilder()
        sb.appendLine("📄 **Your Bills:**\n")

        if (k.overdueBills.isNotEmpty()) {
            sb.appendLine("🔴 **Overdue Bills (${k.overdueBills.size}):**")
            k.overdueBills.forEach { bill ->
                sb.appendLine("• ${bill.title}: $${String.format("%.2f", bill.amount)} - ${abs(bill.daysUntilDue)} days overdue!")
            }
            sb.appendLine()
        }

        if (k.upcomingBills.isNotEmpty()) {
            sb.appendLine("📅 **Upcoming Bills (${k.upcomingBills.size}):**")
            k.upcomingBills.take(5).forEach { bill ->
                val urgency = when {
                    bill.daysUntilDue <= 3 -> "🔴"
                    bill.daysUntilDue <= 7 -> "🟡"
                    else -> "🟢"
                }
                sb.appendLine("$urgency ${bill.title}: $${String.format("%.2f", bill.amount)} - due in ${bill.daysUntilDue} days")
            }
            sb.appendLine()
        }

        sb.appendLine("**Summary:**")
        sb.appendLine("• Total Bills: $${String.format("%.2f", k.totalBillsAmount)}")
        sb.appendLine("• Paid: ${k.paidBills.size} bills")
        sb.appendLine("• Unpaid: ${k.unpaidBills.size} bills")

        return sb.toString()
    }

    private fun answerIncomeQuery(query: String, k: SystemKnowledge): String {
        if (k.incomes.isEmpty()) {
            return "You haven't recorded any income yet. Would you like to add your income?"
        }

        val sb = StringBuilder()
        sb.appendLine("💰 **Your Income:**\n")
        sb.appendLine("**This Month:** $${String.format("%.2f", k.monthlyIncome)}")
        sb.appendLine("**Total Recorded:** $${String.format("%.2f", k.totalIncome)}")
        sb.appendLine()

        if (k.incomeBySource.isNotEmpty()) {
            sb.appendLine("**By Source:**")
            k.incomeBySource.entries.sortedByDescending { it.value }.forEach { (source, amount) ->
                sb.appendLine("• $source: $${String.format("%.2f", amount)}")
            }
        }

        sb.appendLine()
        sb.appendLine("**Recent Income:**")
        k.incomes.sortedByDescending { it.date }.take(5).forEach { income ->
            sb.appendLine("• ${income.source}: $${String.format("%.2f", income.amount)} - ${dateFormat.format(Date(income.date))}")
        }

        return sb.toString()
    }

    private fun answerExpenseQuery(query: String, k: SystemKnowledge): String {
        if (k.expenses.isEmpty()) {
            return "No expenses recorded yet. Start tracking your spending!"
        }

        val sb = StringBuilder()
        sb.appendLine("💸 **Your Spending:**\n")
        sb.appendLine("**This Month:** $${String.format("%.2f", k.monthlyExpenses)}")
        sb.appendLine("**Daily Average:** $${String.format("%.2f", k.dailyAverageExpense)}")
        sb.appendLine("**Total Recorded:** $${String.format("%.2f", k.totalExpenses)}")
        sb.appendLine()

        if (k.expensesByCategory.isNotEmpty()) {
            sb.appendLine("**By Category (This Month):**")
            k.expensesByCategory.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
                val percent = if (k.monthlyExpenses > 0) (amount / k.monthlyExpenses * 100).toInt() else 0
                sb.appendLine("• $category: $${String.format("%.2f", amount)} ($percent%)")
            }
        }

        sb.appendLine()
        sb.appendLine("**Recent Expenses:**")
        k.expenses.sortedByDescending { it.date }.take(5).forEach { expense ->
            sb.appendLine("• ${expense.category}: $${String.format("%.2f", expense.amount)} - ${expense.description}")
        }

        return sb.toString()
    }

    private fun answerBudgetQuery(query: String, k: SystemKnowledge): String {
        if (k.budgets.isEmpty()) {
            return "You haven't set any budgets yet. Would you like to create one?"
        }

        val sb = StringBuilder()
        sb.appendLine("📊 **Your Budgets:**\n")

        k.budgets.sortedByDescending { it.utilizationPercent }.forEach { budget ->
            val status = when {
                budget.isOverBudget -> "🔴"
                budget.utilizationPercent >= 80 -> "🟡"
                else -> "🟢"
            }
            val progressBar = createProgressBar(budget.utilizationPercent.toInt().coerceAtMost(100))
            sb.appendLine("$status **${budget.category}**")
            sb.appendLine("  Limit: $${String.format("%.2f", budget.limit)}")
            sb.appendLine("  Spent: $${String.format("%.2f", budget.spent)}")
            sb.appendLine("  $progressBar ${budget.utilizationPercent.toInt()}%")
            sb.appendLine()
        }

        sb.appendLine("**Summary:**")
        sb.appendLine("• Total Budget: $${String.format("%.2f", k.totalBudget)}")
        sb.appendLine("• Average Utilization: ${k.budgetUtilization.toInt()}%")

        if (k.overBudgetCategories.isNotEmpty()) {
            sb.appendLine("• ⚠️ Over Budget: ${k.overBudgetCategories.joinToString(", ")}")
        }

        return sb.toString()
    }

    private fun answerSavingsQuery(query: String, k: SystemKnowledge): String {
        val monthlySavings = k.monthlyIncome - k.monthlyExpenses

        val sb = StringBuilder()
        sb.appendLine("💎 **Your Savings Analysis:**\n")
        sb.appendLine("**Savings Rate:** ${k.savingsRate.toInt()}%")
        sb.appendLine("**Monthly Savings:** $${String.format("%.2f", monthlySavings)}")
        sb.appendLine()

        val savingsEmoji = when {
            k.savingsRate >= 30 -> "🌟 Excellent!"
            k.savingsRate >= 20 -> "✅ Good"
            k.savingsRate >= 10 -> "📊 Fair"
            else -> "⚠️ Needs Improvement"
        }
        sb.appendLine("**Status:** $savingsEmoji")
        sb.appendLine()

        sb.appendLine("**Recommended:** Aim for 20-30% savings rate")
        sb.appendLine("**Your Gap:** ${if (k.savingsRate < 20) "${(20 - k.savingsRate).toInt()}% more needed" else "You're on track!"}")

        if (monthlySavings > 0) {
            sb.appendLine()
            sb.appendLine("**Projections:**")
            sb.appendLine("• 6 months: $${String.format("%.2f", monthlySavings * 6)}")
            sb.appendLine("• 1 year: $${String.format("%.2f", monthlySavings * 12)}")
        }

        return sb.toString()
    }

    private fun answerShoppingQuery(query: String, k: SystemKnowledge): String {
        if (k.shoppingLists.isEmpty()) {
            return "No shopping lists found. Would you like to create one?"
        }

        val sb = StringBuilder()
        sb.appendLine("🛒 **Your Shopping Lists:**\n")

        k.shoppingLists.sortedByDescending { it.date }.forEach { list ->
            sb.appendLine("• **${list.title}**")
            sb.appendLine("  Budget: $${String.format("%.2f", list.totalBudget)}")
            sb.appendLine("  Items: ${list.itemCount}")
            sb.appendLine("  Date: ${dateFormat.format(Date(list.date))}")
            sb.appendLine()
        }

        sb.appendLine("**Total Shopping Budget:** $${String.format("%.2f", k.totalShoppingBudget)}")

        return sb.toString()
    }

    private fun answerHealthQuery(k: SystemKnowledge): String {
        val sb = StringBuilder()
        sb.appendLine("📈 **Financial Health Report:**\n")

        val healthEmoji = when {
            k.financialHealthScore >= 80 -> "🌟"
            k.financialHealthScore >= 60 -> "✅"
            k.financialHealthScore >= 40 -> "📊"
            else -> "⚠️"
        }

        sb.appendLine("$healthEmoji **Health Score: ${k.financialHealthScore}/100**")
        sb.appendLine("**Risk Level:** ${k.riskLevel}")
        sb.appendLine("**Spending Trend:** ${k.spendingTrend}")
        sb.appendLine()

        sb.appendLine("**Key Metrics:**")
        sb.appendLine("• Income: $${String.format("%.2f", k.monthlyIncome)}")
        sb.appendLine("• Spending: $${String.format("%.2f", k.monthlyExpenses)}")
        sb.appendLine("• Savings: $${String.format("%.2f", k.monthlyIncome - k.monthlyExpenses)}")
        sb.appendLine("• Savings Rate: ${k.savingsRate.toInt()}%")
        sb.appendLine("• Debt-to-Income: ${k.debtToIncomeRatio.toInt()}%")

        if (k.overBudgetCategories.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("⚠️ **Attention Areas:**")
            sb.appendLine("• Over budget in: ${k.overBudgetCategories.joinToString(", ")}")
        }

        if (k.overdueBills.isNotEmpty()) {
            sb.appendLine("• Overdue bills: ${k.overdueBills.size}")
        }

        return sb.toString()
    }

    private fun answerPredictionQuery(k: SystemKnowledge): String {
        val sb = StringBuilder()
        sb.appendLine("🔮 **Financial Predictions:**\n")

        sb.appendLine("**Next Month Forecast:**")
        sb.appendLine("• Predicted Spending: $${String.format("%.2f", k.predictedNextMonthSpending)}")
        sb.appendLine("• Predicted Savings: $${String.format("%.2f", k.predictedSavings)}")
        sb.appendLine()

        sb.appendLine("**Trend Analysis:**")
        sb.appendLine("• Current Trend: ${k.spendingTrend}")

        val trendAdvice = when (k.spendingTrend) {
            "Increasing" -> "⚠️ Your spending is increasing. Consider reviewing discretionary expenses."
            "Decreasing" -> "✅ Great job! Your spending is decreasing."
            "Volatile" -> "📊 Your spending varies significantly. Try to maintain consistency."
            else -> "Your spending is stable. Keep it up!"
        }
        sb.appendLine("• Advice: $trendAdvice")

        if (k.upcomingBills.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("**Upcoming Bills to Budget For:**")
            val upcomingTotal = k.upcomingBills.sumOf { it.amount }
            sb.appendLine("• Total: $${String.format("%.2f", upcomingTotal)} in the next 30 days")
        }

        return sb.toString()
    }

    private fun answerOverviewQuery(k: SystemKnowledge): String {
        val sb = StringBuilder()
        sb.appendLine("📊 **Financial Overview for ${k.userName}:**\n")

        sb.appendLine("**💰 Income & Spending:**")
        sb.appendLine("• Monthly Income: $${String.format("%.2f", k.monthlyIncome)}")
        sb.appendLine("• Monthly Expenses: $${String.format("%.2f", k.monthlyExpenses)}")
        sb.appendLine("• Net Savings: $${String.format("%.2f", k.monthlyIncome - k.monthlyExpenses)}")
        sb.appendLine()

        sb.appendLine("**📋 Bills & Loans:**")
        sb.appendLine("• Upcoming Bills: ${k.upcomingBills.size} ($${String.format("%.2f", k.upcomingBills.sumOf { it.amount })})")
        sb.appendLine("• Active Loans: ${k.activeLoans.size} ($${String.format("%.2f", k.totalLoanRemaining)} remaining)")
        sb.appendLine()

        sb.appendLine("**🎯 Goals:**")
        sb.appendLine("• Active Goals: ${k.activeGoals.size}")
        sb.appendLine("• Total Saved: $${String.format("%.2f", k.totalGoalSaved)} of $${String.format("%.2f", k.totalGoalTarget)}")
        sb.appendLine()

        sb.appendLine("**📈 Health Score: ${k.financialHealthScore}/100 (${k.riskLevel})**")

        return sb.toString()
    }

    private fun answerGeneralQuery(query: String, k: SystemKnowledge): String {
        // Default comprehensive response
        val sb = StringBuilder()
        sb.appendLine("Here's your financial snapshot, ${k.userName}:\n")
        sb.appendLine("• Income: $${String.format("%.2f", k.monthlyIncome)}, Spending: $${String.format("%.2f", k.monthlyExpenses)}")
        sb.appendLine("• Savings Rate: ${k.savingsRate.toInt()}%")
        sb.appendLine("• Risk: ${k.riskLevel}")
        sb.appendLine()
        sb.appendLine("What would you like to explore further?")

        return sb.toString()
    }

    private fun createProgressBar(percent: Int): String {
        val filled = percent / 10
        val empty = 10 - filled
        return "▓".repeat(filled) + "░".repeat(empty)
    }

    /**
     * Get specific data point
     */
    fun getTotalLoans(): Double = _knowledge.value.totalLoanRemaining
    fun getTotalGoals(): Double = _knowledge.value.totalGoalTarget
    fun getMonthlyIncome(): Double = _knowledge.value.monthlyIncome
    fun getMonthlyExpenses(): Double = _knowledge.value.monthlyExpenses
    fun getSavingsRate(): Double = _knowledge.value.savingsRate
    fun getHealthScore(): Int = _knowledge.value.financialHealthScore
    fun getUpcomingBills(): List<BillRecord> = _knowledge.value.upcomingBills
    fun getActiveLoans(): List<LoanRecord> = _knowledge.value.activeLoans
    fun getActiveGoals(): List<GoalRecord> = _knowledge.value.activeGoals

    /**
     * Cleanup
     */
    fun cleanup() {
        scope.cancel()
    }
}

