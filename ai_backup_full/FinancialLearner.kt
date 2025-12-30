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
 * =====================================================
 * FINANCIAL LEARNER - THE BRAIN OF BUDGIE
 * =====================================================
 *
 * This is the CENTRAL INTELLIGENCE that does ALL the analysis.
 * The chatbot is just a thin interface - THIS is where the magic happens.
 *
 * RESPONSIBILITIES:
 * 1. Learn and track ALL financial behaviors
 * 2. Answer ANY financial question with real data
 * 3. Make predictions based on patterns
 * 4. Provide age-appropriate advice
 * 5. Calculate affordability and probabilities
 * 6. Analyze spending patterns
 * 7. Track goals, loans, bills, budgets
 *
 * The chatbot NEVER generates its own answers -
 * it ALWAYS asks this Learner.
 */

private const val TAG = "FinancialLearner"

/**
 * Query types that the learner can handle
 */
enum class QueryCategory {
    LOAN_INFO,           // What are my loans?
    LOAN_AFFORDABILITY,  // Can I afford a new loan?
    GOAL_INFO,           // What are my goals?
    GOAL_FEASIBILITY,    // Can I achieve this goal?
    BILL_INFO,           // What bills do I have?
    BILL_PREDICTION,     // When are bills due?
    EXPENSE_INFO,        // What have I spent?
    EXPENSE_ANALYSIS,    // Where is my money going?
    INCOME_INFO,         // What's my income?
    INCOME_ANALYSIS,     // How stable is my income?
    BUDGET_INFO,         // What's my budget?
    BUDGET_STATUS,       // How am I doing with budgets?
    SAVINGS_INFO,        // How much am I saving?
    SAVINGS_POTENTIAL,   // Can I afford to save X?
    AFFORDABILITY,       // Can I afford X?
    PREDICTION,          // What will happen next month?
    HEALTH_CHECK,        // How am I doing financially?
    ADVICE,              // What should I do?
    COMPARISON,          // Compare periods
    SHOPPING_INFO,       // Shopping lists
    GENERAL              // General financial question
}

/**
 * Structured query from the chatbot
 */
data class LearnerQuery(
    val category: QueryCategory,
    val rawQuestion: String,
    val extractedAmount: Double? = null,
    val extractedTimeframe: Int? = null, // in months
    val extractedCategory: String? = null,
    val extractedItemName: String? = null,
    val userAge: Int = 0,
    val additionalContext: Map<String, Any> = emptyMap()
)

/**
 * Response from the learner
 */
data class LearnerResponse(
    val answer: String,
    val confidence: Double = 1.0,
    val dataUsed: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isAgeAdjusted: Boolean = false,
    val rawData: Map<String, Any> = emptyMap()
)

/**
 * Financial Learner - The intelligent core
 */
class FinancialLearner private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val database = BudgieDatabase.getDatabase(context)
    private val preferencesManager = UserPreferencesManager.getInstance(context)

    // The SuperBrain that holds all knowledge
    private val superBrain = SuperBrain.getInstance(context)

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    companion object {
        @Volatile
        private var INSTANCE: FinancialLearner? = null

        fun getInstance(context: Context): FinancialLearner {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinancialLearner(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Initialize the learner
     */
    suspend fun initialize() {
        Log.d(TAG, "FinancialLearner initializing...")
        superBrain.initialize()
        Log.d(TAG, "FinancialLearner initialized")
    }

    /**
     * Refresh all data
     */
    suspend fun refreshData() {
        superBrain.refreshKnowledge()
    }

    /**
     * =====================================================
     * MAIN ENTRY POINT - Answer ANY financial question
     * =====================================================
     *
     * This is called by the chatbot with a structured query.
     * The learner analyzes ALL relevant data and returns a complete answer.
     */
    suspend fun answerQuestion(query: LearnerQuery): LearnerResponse = withContext(Dispatchers.Default) {
        // Refresh data for latest info
        refreshData()

        val knowledge = superBrain.knowledge.value
        val userAge = query.userAge.takeIf { it > 0 } ?: knowledge.userAge

        Log.d(TAG, "Processing query: ${query.category} for user age $userAge")

        val response = when (query.category) {
            // ===== LOAN QUERIES =====
            QueryCategory.LOAN_INFO -> answerLoanInfo(knowledge, userAge)
            QueryCategory.LOAN_AFFORDABILITY -> answerLoanAffordability(query, knowledge, userAge)

            // ===== GOAL QUERIES =====
            QueryCategory.GOAL_INFO -> answerGoalInfo(knowledge, userAge)
            QueryCategory.GOAL_FEASIBILITY -> answerGoalFeasibility(query, knowledge, userAge)

            // ===== BILL QUERIES =====
            QueryCategory.BILL_INFO -> answerBillInfo(knowledge, userAge)
            QueryCategory.BILL_PREDICTION -> answerBillPrediction(knowledge, userAge)

            // ===== EXPENSE QUERIES =====
            QueryCategory.EXPENSE_INFO -> answerExpenseInfo(query, knowledge, userAge)
            QueryCategory.EXPENSE_ANALYSIS -> answerExpenseAnalysis(knowledge, userAge)

            // ===== INCOME QUERIES =====
            QueryCategory.INCOME_INFO -> answerIncomeInfo(knowledge, userAge)
            QueryCategory.INCOME_ANALYSIS -> answerIncomeAnalysis(knowledge, userAge)

            // ===== BUDGET QUERIES =====
            QueryCategory.BUDGET_INFO -> answerBudgetInfo(knowledge, userAge)
            QueryCategory.BUDGET_STATUS -> answerBudgetStatus(knowledge, userAge)

            // ===== SAVINGS QUERIES =====
            QueryCategory.SAVINGS_INFO -> answerSavingsInfo(knowledge, userAge)
            QueryCategory.SAVINGS_POTENTIAL -> answerSavingsPotential(query, knowledge, userAge)

            // ===== AFFORDABILITY (THE MAGIC!) =====
            QueryCategory.AFFORDABILITY -> answerAffordability(query, knowledge, userAge)

            // ===== PREDICTIONS =====
            QueryCategory.PREDICTION -> answerPrediction(knowledge, userAge)

            // ===== HEALTH CHECK =====
            QueryCategory.HEALTH_CHECK -> answerHealthCheck(knowledge, userAge)

            // ===== ADVICE =====
            QueryCategory.ADVICE -> answerAdvice(knowledge, userAge)

            // ===== COMPARISONS =====
            QueryCategory.COMPARISON -> answerComparison(query, knowledge, userAge)

            // ===== SHOPPING =====
            QueryCategory.SHOPPING_INFO -> answerShoppingInfo(knowledge, userAge)

            // ===== GENERAL =====
            QueryCategory.GENERAL -> answerGeneral(query, knowledge, userAge)
        }

        response
    }

    // =====================================================
    // LOAN ANALYSIS
    // =====================================================

    private fun answerLoanInfo(k: SystemKnowledge, age: Int): LearnerResponse {
        if (k.loans.isEmpty()) {
            return LearnerResponse(
                answer = buildString {
                    appendLine("NO_LOANS")
                    appendLine("You have no loans recorded.")
                    if (age < 30) {
                        appendLine("ADVICE: Starting debt-free is a great foundation! If you need to borrow, compare rates carefully.")
                    } else if (age >= 50) {
                        appendLine("ADVICE: Being debt-free at this stage is excellent for retirement planning.")
                    }
                },
                suggestions = listOf("Add a loan", "Check affordability", "View goals")
            )
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("LOAN_DATA")
                appendLine("TOTAL_LOANS:${k.loans.size}")
                appendLine("ACTIVE_LOANS:${k.activeLoans.size}")
                appendLine("TOTAL_BORROWED:${k.totalLoanAmount}")
                appendLine("TOTAL_PAID:${k.totalLoanPaid}")
                appendLine("TOTAL_REMAINING:${k.totalLoanRemaining}")
                appendLine("DEBT_TO_INCOME:${k.debtToIncomeRatio}")
                appendLine("---DETAILS---")

                k.activeLoans.forEach { loan ->
                    appendLine("LOAN:${loan.title}")
                    appendLine("  LENDER:${loan.lender}")
                    appendLine("  PRINCIPAL:${loan.principalAmount}")
                    appendLine("  TOTAL_PAYABLE:${loan.totalAmount}")
                    appendLine("  PAID:${loan.amountPaid}")
                    appendLine("  REMAINING:${loan.amountRemaining}")
                    appendLine("  MONTHLY_PAYMENT:${loan.monthlyPayment}")
                    appendLine("  INTEREST_RATE:${loan.interestRate}%")
                    appendLine("  PAYMENTS_LEFT:${loan.paymentsLeft}")
                }

                // Age-specific advice
                appendLine("---ADVICE---")
                when {
                    age < 30 && k.debtToIncomeRatio > 30 -> {
                        appendLine("At your age, focus on paying down high-interest debt first to build wealth early.")
                    }
                    age in 30..50 && k.debtToIncomeRatio > 40 -> {
                        appendLine("Consider accelerating debt repayment to free up cash flow for investments and savings.")
                    }
                    age > 50 && k.debtToIncomeRatio > 20 -> {
                        appendLine("Prioritize becoming debt-free before retirement. Consider refinancing for lower rates.")
                    }
                    else -> {
                        appendLine("Your debt levels are manageable. Keep making regular payments.")
                    }
                }
            },
            dataUsed = listOf("loans", "income", "debt_ratio"),
            suggestions = listOf("Pay off faster", "Calculate savings from early payoff", "Compare with income"),
            isAgeAdjusted = true,
            rawData = mapOf(
                "totalLoans" to k.loans.size,
                "totalRemaining" to k.totalLoanRemaining,
                "debtToIncome" to k.debtToIncomeRatio
            )
        )
    }

    private fun answerLoanAffordability(query: LearnerQuery, k: SystemKnowledge, age: Int): LearnerResponse {
        val loanAmount = query.extractedAmount ?: 0.0
        val termMonths = query.extractedTimeframe ?: 12

        val monthlyIncome = k.monthlyIncome
        val monthlyExpenses = k.monthlyExpenses
        val currentDebtPayments = k.activeLoans.sumOf { it.monthlyPayment }
        val availableForDebt = monthlyIncome - monthlyExpenses - currentDebtPayments

        // Estimate monthly payment (simple interest for rough calculation)
        val estimatedMonthlyPayment = loanAmount / termMonths * 1.1 // +10% for interest

        val canAfford = estimatedMonthlyPayment <= availableForDebt * 0.7 // 70% of available
        val affordabilityScore = if (availableForDebt > 0)
            ((availableForDebt * 0.7) / estimatedMonthlyPayment * 100).coerceIn(0.0, 100.0) else 0.0

        val newDebtToIncome = if (monthlyIncome > 0)
            ((k.totalLoanRemaining + loanAmount) / (monthlyIncome * 12) * 100) else 100.0

        return LearnerResponse(
            answer = buildString {
                appendLine("AFFORDABILITY_ANALYSIS")
                appendLine("LOAN_AMOUNT:$loanAmount")
                appendLine("TERM_MONTHS:$termMonths")
                appendLine("ESTIMATED_MONTHLY:$estimatedMonthlyPayment")
                appendLine("AVAILABLE_INCOME:$availableForDebt")
                appendLine("CAN_AFFORD:$canAfford")
                appendLine("AFFORDABILITY_SCORE:${affordabilityScore.toInt()}")
                appendLine("NEW_DEBT_TO_INCOME:${newDebtToIncome.toInt()}")
                appendLine("---ANALYSIS---")

                when {
                    canAfford && affordabilityScore >= 80 -> {
                        appendLine("VERDICT:COMFORTABLE")
                        appendLine("You can comfortably afford this loan. Payment would be ${(estimatedMonthlyPayment/availableForDebt*100).toInt()}% of your available income.")
                    }
                    canAfford && affordabilityScore >= 50 -> {
                        appendLine("VERDICT:TIGHT")
                        appendLine("You can afford this, but it will be tight. Consider a longer term to reduce monthly payments.")
                    }
                    else -> {
                        appendLine("VERDICT:NOT_RECOMMENDED")
                        appendLine("This loan would strain your finances.")
                        val maxAffordable = availableForDebt * 0.7 * termMonths / 1.1
                        appendLine("MAX_AFFORDABLE:$maxAffordable")
                    }
                }

                // Age-specific advice
                appendLine("---AGE_ADVICE---")
                when {
                    age < 25 -> appendLine("At your age, be cautious with debt. Build an emergency fund first.")
                    age in 25..35 -> appendLine("Consider how this debt affects your other goals like home ownership or starting a family.")
                    age in 36..50 -> appendLine("Factor in retirement savings when taking on new debt.")
                    age > 50 -> appendLine("Taking on new debt approaching retirement requires careful consideration.")
                }
            },
            confidence = if (k.monthlyIncome > 0 && k.expenses.isNotEmpty()) 0.9 else 0.6,
            dataUsed = listOf("income", "expenses", "current_loans", "available_income"),
            suggestions = when {
                !canAfford -> listOf("Try longer term", "Reduce amount", "Increase income first")
                else -> listOf("View loan options", "Calculate total interest", "Compare rates")
            },
            isAgeAdjusted = true,
            rawData = mapOf(
                "canAfford" to canAfford,
                "affordabilityScore" to affordabilityScore,
                "estimatedPayment" to estimatedMonthlyPayment,
                "availableIncome" to availableForDebt
            )
        )
    }

    // =====================================================
    // GOAL ANALYSIS
    // =====================================================

    private fun answerGoalInfo(k: SystemKnowledge, age: Int): LearnerResponse {
        if (k.goals.isEmpty()) {
            return LearnerResponse(
                answer = buildString {
                    appendLine("NO_GOALS")
                    appendLine("You haven't set any financial goals yet.")
                    appendLine("---SUGGESTION---")
                    when {
                        age < 30 -> appendLine("Consider starting with an emergency fund (3-6 months expenses) and retirement savings.")
                        age in 30..50 -> appendLine("Focus on major life goals: home ownership, education funds, and retirement.")
                        age > 50 -> appendLine("Prioritize retirement security and potentially helping family.")
                    }
                },
                suggestions = listOf("Create a goal", "Emergency fund guide", "Goal strategies")
            )
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("GOAL_DATA")
                appendLine("TOTAL_GOALS:${k.goals.size}")
                appendLine("ACTIVE_GOALS:${k.activeGoals.size}")
                appendLine("COMPLETED_GOALS:${k.completedGoals.size}")
                appendLine("TOTAL_TARGET:${k.totalGoalTarget}")
                appendLine("TOTAL_SAVED:${k.totalGoalSaved}")
                appendLine("OVERALL_PROGRESS:${if (k.totalGoalTarget > 0) (k.totalGoalSaved/k.totalGoalTarget*100).toInt() else 0}%")
                appendLine("---DETAILS---")

                k.activeGoals.forEach { goal ->
                    appendLine("GOAL:${goal.title}")
                    appendLine("  TARGET:${goal.targetAmount}")
                    appendLine("  SAVED:${goal.currentAmount}")
                    appendLine("  PROGRESS:${goal.progress.toInt()}%")
                    appendLine("  DAYS_LEFT:${goal.daysRemaining}")
                    appendLine("  CATEGORY:${goal.category}")

                    // Calculate if on track
                    val dailyNeeded = if (goal.daysRemaining > 0)
                        (goal.targetAmount - goal.currentAmount) / goal.daysRemaining else 0.0
                    val monthlyNeeded = dailyNeeded * 30
                    val canAchieve = monthlyNeeded <= k.monthlyIncome - k.monthlyExpenses
                    appendLine("  MONTHLY_NEEDED:$monthlyNeeded")
                    appendLine("  ON_TRACK:$canAchieve")
                }

                appendLine("---ADVICE---")
                if (k.savingsRate >= 20) {
                    appendLine("Your savings rate supports goal achievement. Stay consistent!")
                } else {
                    appendLine("To reach goals faster, try increasing your savings rate from ${k.savingsRate.toInt()}% to 20%.")
                }
            },
            dataUsed = listOf("goals", "savings_rate", "income", "expenses"),
            suggestions = listOf("Create new goal", "Adjust goal timeline", "Boost savings"),
            isAgeAdjusted = true
        )
    }

    private fun answerGoalFeasibility(query: LearnerQuery, k: SystemKnowledge, age: Int): LearnerResponse {
        val targetAmount = query.extractedAmount ?: 0.0
        val timeframeMonths = query.extractedTimeframe ?: 12
        val itemName = query.extractedItemName ?: "this goal"

        val monthlySavings = k.monthlyIncome - k.monthlyExpenses
        val monthlyNeeded = targetAmount / timeframeMonths
        val currentSavingsRate = k.savingsRate

        // Calculate probability based on savings patterns
        val probability = when {
            monthlySavings <= 0 -> 0.0
            monthlyNeeded <= monthlySavings * 0.3 -> 95.0 // Easy
            monthlyNeeded <= monthlySavings * 0.5 -> 80.0 // Moderate
            monthlyNeeded <= monthlySavings * 0.7 -> 60.0 // Challenging
            monthlyNeeded <= monthlySavings -> 40.0 // Difficult
            else -> max(5.0, (monthlySavings / monthlyNeeded * 100))
        }

        // Calculate required savings rate
        val requiredSavingsRate = if (k.monthlyIncome > 0)
            (monthlyNeeded / k.monthlyIncome * 100) else 100.0

        val savingsGap = monthlyNeeded - monthlySavings

        // Suggest adjustments
        val suggestedTimeframe = if (monthlySavings > 0)
            ceil(targetAmount / (monthlySavings * 0.7)).toInt() else 0

        return LearnerResponse(
            answer = buildString {
                appendLine("GOAL_FEASIBILITY_ANALYSIS")
                appendLine("ITEM:$itemName")
                appendLine("TARGET_AMOUNT:$targetAmount")
                appendLine("TIMEFRAME_MONTHS:$timeframeMonths")
                appendLine("MONTHLY_NEEDED:$monthlyNeeded")
                appendLine("CURRENT_MONTHLY_SAVINGS:$monthlySavings")
                appendLine("PROBABILITY:${probability.toInt()}%")
                appendLine("REQUIRED_SAVINGS_RATE:${requiredSavingsRate.toInt()}%")
                appendLine("CURRENT_SAVINGS_RATE:${currentSavingsRate.toInt()}%")
                appendLine("---ANALYSIS---")

                when {
                    probability >= 80 -> {
                        appendLine("VERDICT:ACHIEVABLE")
                        appendLine("You can achieve this goal! You'll be using ${(monthlyNeeded/monthlySavings*100).toInt()}% of your monthly savings.")
                    }
                    probability >= 50 -> {
                        appendLine("VERDICT:CHALLENGING")
                        appendLine("This is possible but will require discipline. Consider reducing other expenses.")
                    }
                    probability >= 20 -> {
                        appendLine("VERDICT:DIFFICULT")
                        appendLine("This goal is ambitious given your current finances.")
                        appendLine("SUGGESTED_TIMEFRAME:$suggestedTimeframe months")
                        appendLine("SUGGESTED_AMOUNT:${monthlySavings * 0.7 * timeframeMonths}")
                    }
                    else -> {
                        appendLine("VERDICT:NOT_FEASIBLE_CURRENTLY")
                        appendLine("Your current finances don't support this goal in this timeframe.")
                        if (savingsGap > 0) {
                            appendLine("SAVINGS_GAP:$savingsGap per month")
                        }
                        if (suggestedTimeframe > 0) {
                            appendLine("REALISTIC_TIMEFRAME:$suggestedTimeframe months")
                        }
                    }
                }

                appendLine("---TIPS---")
                when {
                    probability < 50 -> {
                        appendLine("TIP:Consider extending the timeline to $suggestedTimeframe months")
                        appendLine("TIP:Look for a more affordable alternative")
                        appendLine("TIP:Find ways to increase income or cut expenses")
                    }
                    probability < 80 -> {
                        appendLine("TIP:Automate savings to stay on track")
                        appendLine("TIP:Review monthly expenses for potential cuts")
                    }
                    else -> {
                        appendLine("TIP:Set up automatic transfers to a dedicated savings account")
                    }
                }

                appendLine("---AGE_CONTEXT---")
                when {
                    age < 30 -> appendLine("At your age, building good savings habits now will compound significantly over time.")
                    age in 30..50 -> appendLine("Balance this goal with retirement savings and emergency funds.")
                    age > 50 -> appendLine("Ensure this goal doesn't impact your retirement security.")
                }
            },
            confidence = when {
                k.monthlyIncome > 0 && k.expenses.isNotEmpty() && k.incomes.size > 3 -> 0.9
                k.monthlyIncome > 0 -> 0.7
                else -> 0.5
            },
            dataUsed = listOf("income", "expenses", "savings_rate", "spending_patterns"),
            suggestions = when {
                probability >= 80 -> listOf("Set up goal", "Automate savings", "Track progress")
                probability >= 50 -> listOf("Extend timeline", "Cut expenses", "Create budget")
                else -> listOf("Adjust amount", "Extend to $suggestedTimeframe months", "Explore alternatives")
            },
            isAgeAdjusted = true,
            rawData = mapOf(
                "probability" to probability,
                "monthlyNeeded" to monthlyNeeded,
                "monthlySavings" to monthlySavings,
                "suggestedTimeframe" to suggestedTimeframe
            )
        )
    }

    // =====================================================
    // BILL ANALYSIS
    // =====================================================

    private fun answerBillInfo(k: SystemKnowledge, age: Int): LearnerResponse {
        if (k.bills.isEmpty()) {
            return LearnerResponse(
                answer = "NO_BILLS\nYou don't have any bills recorded. Track your bills to stay on top of payments!",
                suggestions = listOf("Add a bill", "Set reminders")
            )
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("BILL_DATA")
                appendLine("TOTAL_BILLS:${k.bills.size}")
                appendLine("TOTAL_AMOUNT:${k.totalBillsAmount}")
                appendLine("PAID:${k.paidBills.size}")
                appendLine("UNPAID:${k.unpaidBills.size}")
                appendLine("OVERDUE:${k.overdueBills.size}")

                if (k.overdueBills.isNotEmpty()) {
                    appendLine("---OVERDUE---")
                    k.overdueBills.forEach {
                        appendLine("OVERDUE_BILL:${it.title}|${it.amount}|${abs(it.daysUntilDue)} days")
                    }
                }

                appendLine("---UPCOMING---")
                k.upcomingBills.take(5).forEach {
                    val urgency = when {
                        it.daysUntilDue <= 3 -> "URGENT"
                        it.daysUntilDue <= 7 -> "SOON"
                        else -> "OK"
                    }
                    appendLine("UPCOMING_BILL:${it.title}|${it.amount}|${it.daysUntilDue}|$urgency")
                }

                appendLine("---SUMMARY---")
                appendLine("TOTAL_DUE:${k.unpaidBills.sumOf { it.amount }}")
            },
            dataUsed = listOf("bills"),
            suggestions = listOf("Pay bills", "Set reminders", "View calendar")
        )
    }

    private fun answerBillPrediction(k: SystemKnowledge, age: Int): LearnerResponse {
        val nextBill = k.upcomingBills.firstOrNull()
        val totalUpcoming30Days = k.upcomingBills.filter { it.daysUntilDue <= 30 }.sumOf { it.amount }

        return LearnerResponse(
            answer = buildString {
                appendLine("BILL_PREDICTION")
                if (nextBill != null) {
                    appendLine("NEXT_BILL:${nextBill.title}")
                    appendLine("NEXT_AMOUNT:${nextBill.amount}")
                    appendLine("NEXT_DUE_IN:${nextBill.daysUntilDue} days")
                }
                appendLine("TOTAL_30_DAYS:$totalUpcoming30Days")
                appendLine("CAN_COVER:${totalUpcoming30Days <= k.monthlyIncome - k.monthlyExpenses}")
            },
            suggestions = listOf("Set reminders", "Budget for bills")
        )
    }

    // =====================================================
    // EXPENSE ANALYSIS
    // =====================================================

    private fun answerExpenseInfo(query: LearnerQuery, k: SystemKnowledge, age: Int): LearnerResponse {
        if (k.expenses.isEmpty()) {
            return LearnerResponse(
                answer = "NO_EXPENSES\nNo expenses recorded yet. Start tracking to understand your spending!",
                suggestions = listOf("Add expense", "Import transactions")
            )
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("EXPENSE_DATA")
                appendLine("MONTHLY_TOTAL:${k.monthlyExpenses}")
                appendLine("DAILY_AVERAGE:${k.dailyAverageExpense}")
                appendLine("TOTAL_ALL_TIME:${k.totalExpenses}")
                appendLine("TREND:${k.spendingTrend}")

                appendLine("---BY_CATEGORY---")
                k.expensesByCategory.entries.sortedByDescending { it.value }.forEach { (cat, amount) ->
                    val percent = if (k.monthlyExpenses > 0) (amount / k.monthlyExpenses * 100).toInt() else 0
                    appendLine("CATEGORY:$cat|$amount|$percent%")
                }

                appendLine("---RECENT---")
                k.expenses.sortedByDescending { it.date }.take(10).forEach {
                    appendLine("EXPENSE:${it.category}|${it.amount}|${it.description}")
                }
            },
            dataUsed = listOf("expenses", "categories"),
            suggestions = listOf("Analyze patterns", "Set budget", "Compare months")
        )
    }

    private fun answerExpenseAnalysis(k: SystemKnowledge, age: Int): LearnerResponse {
        val topCategory = k.expensesByCategory.maxByOrNull { it.value }
        val spendingRate = if (k.monthlyIncome > 0) (k.monthlyExpenses / k.monthlyIncome * 100) else 100.0

        return LearnerResponse(
            answer = buildString {
                appendLine("EXPENSE_ANALYSIS")
                appendLine("SPENDING_RATE:${spendingRate.toInt()}%")
                appendLine("TREND:${k.spendingTrend}")
                appendLine("TOP_CATEGORY:${topCategory?.key ?: "None"}|${topCategory?.value ?: 0.0}")

                appendLine("---INSIGHTS---")
                when {
                    spendingRate > 90 -> appendLine("INSIGHT:You're spending over 90% of your income. This is concerning.")
                    spendingRate > 70 -> appendLine("INSIGHT:You're spending ${spendingRate.toInt()}% of income. Aim for 70% or less.")
                    else -> appendLine("INSIGHT:Good spending control at ${spendingRate.toInt()}% of income.")
                }

                // 50/30/20 analysis
                val needs = k.expensesByCategory.filterKeys { it.lowercase() in listOf("rent", "utilities", "groceries", "transport", "health") }
                    .values.sum()
                val needsPercent = if (k.monthlyIncome > 0) (needs / k.monthlyIncome * 100) else 0.0
                appendLine("NEEDS_PERCENT:${needsPercent.toInt()}%")
                appendLine("NEEDS_TARGET:50%")

                appendLine("---AGE_ADVICE---")
                when {
                    age < 25 -> appendLine("Building good spending habits now sets the foundation for wealth.")
                    age in 25..35 -> appendLine("Balance enjoyment with saving for major milestones.")
                    age in 36..50 -> appendLine("Optimize spending to maximize retirement contributions.")
                    age > 50 -> appendLine("Focus on sustainable spending patterns for retirement.")
                }
            },
            dataUsed = listOf("expenses", "income", "categories", "trends"),
            suggestions = listOf("Cut top category", "Set budget", "Track daily"),
            isAgeAdjusted = true
        )
    }

    // =====================================================
    // INCOME ANALYSIS
    // =====================================================

    private fun answerIncomeInfo(k: SystemKnowledge, age: Int): LearnerResponse {
        if (k.incomes.isEmpty()) {
            return LearnerResponse(
                answer = "NO_INCOME\nNo income recorded. Add your income sources for better insights!",
                suggestions = listOf("Add income", "Set up recurring")
            )
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("INCOME_DATA")
                appendLine("MONTHLY_INCOME:${k.monthlyIncome}")
                appendLine("TOTAL_RECORDED:${k.totalIncome}")
                appendLine("SOURCES:${k.incomeBySource.size}")

                appendLine("---BY_SOURCE---")
                k.incomeBySource.forEach { (source, amount) ->
                    appendLine("SOURCE:$source|$amount")
                }
            },
            dataUsed = listOf("income"),
            suggestions = listOf("Add income", "Track sources")
        )
    }

    private fun answerIncomeAnalysis(k: SystemKnowledge, age: Int): LearnerResponse {
        val diversification = k.incomeBySource.size
        val mainSourcePercent = if (k.monthlyIncome > 0)
            (k.incomeBySource.maxOfOrNull { it.value } ?: 0.0) / k.monthlyIncome * 100 else 100.0

        return LearnerResponse(
            answer = buildString {
                appendLine("INCOME_ANALYSIS")
                appendLine("MONTHLY:${k.monthlyIncome}")
                appendLine("SOURCES:$diversification")
                appendLine("MAIN_SOURCE_DEPENDENCY:${mainSourcePercent.toInt()}%")

                appendLine("---DIVERSIFICATION---")
                when {
                    diversification == 1 -> appendLine("RISK:HIGH - Single income source")
                    diversification <= 3 -> appendLine("RISK:MODERATE - Limited diversification")
                    else -> appendLine("RISK:LOW - Well diversified income")
                }

                appendLine("---AGE_ADVICE---")
                when {
                    age < 30 -> appendLine("Consider building side income streams early.")
                    age in 30..50 -> appendLine("Focus on maximizing primary income while building passive streams.")
                    age > 50 -> appendLine("Ensure income sources are sustainable into retirement.")
                }
            },
            suggestions = listOf("Diversify income", "Side hustles", "Passive income"),
            isAgeAdjusted = true
        )
    }

    // =====================================================
    // BUDGET ANALYSIS
    // =====================================================

    private fun answerBudgetInfo(k: SystemKnowledge, age: Int): LearnerResponse {
        if (k.budgets.isEmpty()) {
            return LearnerResponse(
                answer = "NO_BUDGETS\nNo budgets set up. Budgets help control spending!",
                suggestions = listOf("Create budget", "50/30/20 guide")
            )
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("BUDGET_DATA")
                appendLine("TOTAL_BUDGET:${k.totalBudget}")
                appendLine("AVERAGE_UTILIZATION:${k.budgetUtilization.toInt()}%")
                appendLine("OVER_BUDGET:${k.overBudgetCategories.size}")

                appendLine("---CATEGORIES---")
                k.budgets.forEach {
                    val status = when {
                        it.isOverBudget -> "OVER"
                        it.utilizationPercent >= 80 -> "WARNING"
                        else -> "OK"
                    }
                    appendLine("BUDGET:${it.category}|${it.limit}|${it.spent}|${it.utilizationPercent.toInt()}%|$status")
                }

                if (k.overBudgetCategories.isNotEmpty()) {
                    appendLine("---OVER_BUDGET---")
                    appendLine("CATEGORIES:${k.overBudgetCategories.joinToString(",")}")
                }
            },
            dataUsed = listOf("budgets", "expenses"),
            suggestions = listOf("Adjust budgets", "Review spending", "Set alerts")
        )
    }

    private fun answerBudgetStatus(k: SystemKnowledge, age: Int): LearnerResponse {
        val onTrack = k.budgets.filter { !it.isOverBudget && it.utilizationPercent < 80 }.size
        val warning = k.budgets.filter { !it.isOverBudget && it.utilizationPercent >= 80 }.size
        val overBudget = k.overBudgetCategories.size

        return LearnerResponse(
            answer = buildString {
                appendLine("BUDGET_STATUS")
                appendLine("ON_TRACK:$onTrack")
                appendLine("WARNING:$warning")
                appendLine("OVER_BUDGET:$overBudget")
                appendLine("OVERALL:${if (overBudget == 0) "GOOD" else "NEEDS_ATTENTION"}")
            },
            suggestions = listOf("View details", "Adjust limits")
        )
    }

    // =====================================================
    // SAVINGS ANALYSIS
    // =====================================================

    private fun answerSavingsInfo(k: SystemKnowledge, age: Int): LearnerResponse {
        val monthlySavings = k.monthlyIncome - k.monthlyExpenses

        return LearnerResponse(
            answer = buildString {
                appendLine("SAVINGS_DATA")
                appendLine("SAVINGS_RATE:${k.savingsRate.toInt()}%")
                appendLine("MONTHLY_SAVINGS:$monthlySavings")
                appendLine("MONTHLY_INCOME:${k.monthlyIncome}")
                appendLine("MONTHLY_EXPENSES:${k.monthlyExpenses}")

                appendLine("---STATUS---")
                when {
                    k.savingsRate >= 30 -> appendLine("STATUS:EXCELLENT")
                    k.savingsRate >= 20 -> appendLine("STATUS:GOOD")
                    k.savingsRate >= 10 -> appendLine("STATUS:FAIR")
                    k.savingsRate > 0 -> appendLine("STATUS:LOW")
                    else -> appendLine("STATUS:NEGATIVE")
                }

                appendLine("---PROJECTIONS---")
                if (monthlySavings > 0) {
                    appendLine("6_MONTHS:${monthlySavings * 6}")
                    appendLine("1_YEAR:${monthlySavings * 12}")
                    appendLine("5_YEARS:${monthlySavings * 60}")
                }

                appendLine("---TARGETS---")
                when {
                    age < 30 -> {
                        appendLine("TARGET_RATE:20-25%")
                        appendLine("FOCUS:Emergency fund and retirement start")
                    }
                    age in 30..40 -> {
                        appendLine("TARGET_RATE:25-30%")
                        appendLine("FOCUS:Maximize retirement, home equity")
                    }
                    age in 41..50 -> {
                        appendLine("TARGET_RATE:30-35%")
                        appendLine("FOCUS:Accelerate retirement savings")
                    }
                    age > 50 -> {
                        appendLine("TARGET_RATE:35%+")
                        appendLine("FOCUS:Final retirement push")
                    }
                }
            },
            dataUsed = listOf("income", "expenses", "savings_rate"),
            suggestions = listOf("Increase savings", "Cut expenses", "Automate savings"),
            isAgeAdjusted = true
        )
    }

    private fun answerSavingsPotential(query: LearnerQuery, k: SystemKnowledge, age: Int): LearnerResponse {
        val targetSavings = query.extractedAmount ?: 0.0
        val currentMonthlySavings = k.monthlyIncome - k.monthlyExpenses
        val canSave = targetSavings <= currentMonthlySavings
        val gapOrSurplus = currentMonthlySavings - targetSavings

        return LearnerResponse(
            answer = buildString {
                appendLine("SAVINGS_POTENTIAL")
                appendLine("TARGET:$targetSavings")
                appendLine("CURRENT_CAPACITY:$currentMonthlySavings")
                appendLine("CAN_SAVE:$canSave")
                appendLine("DIFFERENCE:$gapOrSurplus")

                if (!canSave) {
                    appendLine("---TO_ACHIEVE---")
                    appendLine("EXPENSE_CUTS_NEEDED:${abs(gapOrSurplus)}")
                    appendLine("OR_INCOME_INCREASE_NEEDED:${abs(gapOrSurplus)}")
                }
            },
            suggestions = if (canSave) listOf("Set up savings", "Automate") else listOf("Cut expenses", "Increase income")
        )
    }

    // =====================================================
    // THE MAGIC - AFFORDABILITY ANALYSIS
    // =====================================================

    private fun answerAffordability(query: LearnerQuery, k: SystemKnowledge, age: Int): LearnerResponse {
        val itemAmount = query.extractedAmount ?: 0.0
        val timeframeMonths = query.extractedTimeframe ?: 12 // Default to 12 months if not specified
        val itemName = query.extractedItemName ?: "this item"

        // Check if we have enough data
        if (itemAmount <= 0) {
            return LearnerResponse(
                answer = buildString {
                    appendLine("NEED_MORE_INFO")
                    appendLine("I'd love to help you figure this out! Could you tell me:")
                    appendLine("• How much does $itemName cost?")
                    appendLine("• When do you want to buy it?")
                },
                suggestions = listOf("Tell me the price", "Set a timeline"),
                confidence = 0.3
            )
        }

        val monthlySavings = k.monthlyIncome - k.monthlyExpenses
        val monthlyNeeded = itemAmount / timeframeMonths

        // Calculate probability
        val probability = when {
            monthlySavings <= 0 -> 0.0
            monthlyNeeded <= monthlySavings * 0.3 -> 95.0
            monthlyNeeded <= monthlySavings * 0.5 -> 80.0
            monthlyNeeded <= monthlySavings * 0.7 -> 60.0
            monthlyNeeded <= monthlySavings -> 40.0
            else -> max(5.0, (monthlySavings / monthlyNeeded * 100))
        }

        // Calculate impact on other goals
        val goalImpact = if (k.activeGoals.isNotEmpty()) {
            val currentGoalNeeds = k.activeGoals.sumOf {
                if (it.daysRemaining > 0) (it.targetAmount - it.currentAmount) / (it.daysRemaining / 30.0) else 0.0
            }
            val remainingAfterThis = monthlySavings - monthlyNeeded
            remainingAfterThis >= currentGoalNeeds
        } else true

        // Calculate recommended timeframe
        val recommendedTimeframe = when {
            monthlySavings <= 0 -> 0
            else -> ceil(itemAmount / (monthlySavings * 0.5)).toInt()
        }

        // Calculate spending ratio impact
        val currentSpendingRatio = k.monthlyExpenses / max(k.monthlyIncome, 1.0)
        val newSpendingRatio = (k.monthlyExpenses + monthlyNeeded) / max(k.monthlyIncome, 1.0)

        return LearnerResponse(
            answer = buildString {
                appendLine("AFFORDABILITY_ANALYSIS")
                appendLine("ITEM:$itemName")
                appendLine("AMOUNT:$itemAmount")
                appendLine("TIMEFRAME:$timeframeMonths months")
                appendLine("MONTHLY_NEEDED:$monthlyNeeded")
                appendLine("MONTHLY_AVAILABLE:$monthlySavings")
                appendLine("PROBABILITY:${probability.toInt()}%")
                appendLine("AFFECTS_GOALS:${!goalImpact}")

                appendLine("---VERDICT---")
                when {
                    probability >= 80 && goalImpact -> {
                        appendLine("VERDICT:YES_COMFORTABLE")
                        appendLine("MESSAGE:You can comfortably afford $itemName. It will use ${(monthlyNeeded/monthlySavings*100).toInt()}% of your monthly savings for $timeframeMonths month(s).")
                    }
                    probability >= 60 && goalImpact -> {
                        appendLine("VERDICT:YES_MANAGEABLE")
                        appendLine("MESSAGE:You can afford $itemName, though it will require some discipline.")
                    }
                    probability >= 40 -> {
                        appendLine("VERDICT:POSSIBLE_TIGHT")
                        appendLine("MESSAGE:This is possible but tight. Consider extending to $recommendedTimeframe months.")
                        if (!goalImpact) {
                            appendLine("WARNING:This may impact your current goals.")
                        }
                    }
                    probability >= 20 -> {
                        appendLine("VERDICT:DIFFICULT")
                        appendLine("MESSAGE:This would be challenging with current finances.")
                        appendLine("SUGGESTION:Try $recommendedTimeframe months instead, or look for a more affordable option.")
                    }
                    else -> {
                        appendLine("VERDICT:NOT_RECOMMENDED")
                        appendLine("MESSAGE:Your current finances don't support this purchase in this timeframe.")
                        if (monthlySavings > 0) {
                            val maxAffordable = monthlySavings * 0.5 * timeframeMonths
                            appendLine("MAX_AFFORDABLE:$maxAffordable in $timeframeMonths months")
                            appendLine("OR_TIMEFRAME:$recommendedTimeframe months for ${itemAmount}")
                        } else {
                            appendLine("CRITICAL:You're currently spending more than you earn. Focus on fixing this first.")
                        }
                    }
                }

                appendLine("---TIPS---")
                when {
                    probability >= 80 -> {
                        appendLine("TIP:Set up a dedicated savings goal for this")
                        appendLine("TIP:Automate transfers to stay on track")
                    }
                    probability >= 50 -> {
                        appendLine("TIP:Extend timeline to $recommendedTimeframe months to ease pressure")
                        appendLine("TIP:Look for ways to cut ${monthlyNeeded - monthlySavings * 0.5} in monthly expenses")
                    }
                    else -> {
                        appendLine("TIP:Save for ${recommendedTimeframe} months instead")
                        val cheaperOption = monthlySavings * 0.5 * timeframeMonths
                        appendLine("TIP:Or consider an option around $${cheaperOption.toLong()}")
                        appendLine("TIP:Review and cut non-essential expenses")
                    }
                }

                appendLine("---AGE_CONTEXT---")
                when {
                    age < 25 -> {
                        appendLine("AGE_ADVICE:At your age, avoid debt for wants. If you can't save for it, wait.")
                        appendLine("PRIORITY:Building emergency fund should come first.")
                    }
                    age in 25..35 -> {
                        appendLine("AGE_ADVICE:Balance purchases with major life goals (home, family, retirement).")
                        appendLine("QUESTION:Does this purchase support your long-term goals?")
                    }
                    age in 36..50 -> {
                        appendLine("AGE_ADVICE:Consider opportunity cost - could this money grow more in investments?")
                        appendLine("RETIREMENT_IMPACT:Every $$itemAmount saved now could be $${"%.0f".format(itemAmount * 3)} at retirement.")
                    }
                    age > 50 -> {
                        appendLine("AGE_ADVICE:Protect retirement security. Non-essential purchases should come from surplus only.")
                        appendLine("QUESTION:Will this impact your retirement timeline?")
                    }
                }
            },
            confidence = when {
                k.monthlyIncome > 0 && k.expenses.size > 10 -> 0.95
                k.monthlyIncome > 0 && k.expenses.size > 3 -> 0.85
                k.monthlyIncome > 0 -> 0.7
                else -> 0.5
            },
            dataUsed = listOf("income", "expenses", "savings", "goals", "spending_patterns"),
            suggestions = when {
                probability >= 80 -> listOf("Create savings goal", "Automate savings", "Start now")
                probability >= 50 -> listOf("Extend timeline", "Create budget", "Cut expenses")
                else -> listOf("Set $recommendedTimeframe month goal", "Find alternatives", "Increase income")
            },
            isAgeAdjusted = true,
            rawData = mapOf(
                "probability" to probability,
                "monthlyNeeded" to monthlyNeeded,
                "monthlySavings" to monthlySavings,
                "recommendedTimeframe" to recommendedTimeframe,
                "goalImpact" to goalImpact
            )
        )
    }

    // =====================================================
    // PREDICTIONS
    // =====================================================

    private fun answerPrediction(k: SystemKnowledge, age: Int): LearnerResponse {
        return LearnerResponse(
            answer = buildString {
                appendLine("FINANCIAL_PREDICTION")
                appendLine("PREDICTED_SPENDING:${k.predictedNextMonthSpending}")
                appendLine("PREDICTED_SAVINGS:${k.predictedSavings}")
                appendLine("SPENDING_TREND:${k.spendingTrend}")

                appendLine("---FORECAST---")
                appendLine("NEXT_MONTH_EXPENSES:${k.predictedNextMonthSpending}")
                appendLine("EXPECTED_INCOME:${k.monthlyIncome}")
                appendLine("EXPECTED_SAVINGS:${k.monthlyIncome - k.predictedNextMonthSpending}")

                appendLine("---TREND_ANALYSIS---")
                when (k.spendingTrend) {
                    "Increasing" -> appendLine("ALERT:Spending is trending up. Review recent purchases.")
                    "Decreasing" -> appendLine("POSITIVE:Great! Your spending is decreasing.")
                    "Volatile" -> appendLine("NOTE:Spending varies significantly. Work on consistency.")
                    else -> appendLine("STABLE:Your spending is relatively stable.")
                }
            },
            dataUsed = listOf("expenses", "income", "trends"),
            suggestions = listOf("Set budget", "Review trends", "Plan ahead")
        )
    }

    // =====================================================
    // HEALTH CHECK
    // =====================================================

    private fun answerHealthCheck(k: SystemKnowledge, age: Int): LearnerResponse {
        val score = k.financialHealthScore

        return LearnerResponse(
            answer = buildString {
                appendLine("HEALTH_CHECK")
                appendLine("SCORE:$score")
                appendLine("RISK_LEVEL:${k.riskLevel}")
                appendLine("SAVINGS_RATE:${k.savingsRate.toInt()}%")
                appendLine("DEBT_TO_INCOME:${k.debtToIncomeRatio.toInt()}%")
                appendLine("SPENDING_TREND:${k.spendingTrend}")

                appendLine("---BREAKDOWN---")
                appendLine("INCOME:${k.monthlyIncome}")
                appendLine("EXPENSES:${k.monthlyExpenses}")
                appendLine("NET_SAVINGS:${k.monthlyIncome - k.monthlyExpenses}")
                appendLine("ACTIVE_LOANS:${k.activeLoans.size}")
                appendLine("LOAN_BURDEN:${k.totalLoanRemaining}")
                appendLine("ACTIVE_GOALS:${k.activeGoals.size}")
                appendLine("OVERDUE_BILLS:${k.overdueBills.size}")
                appendLine("OVER_BUDGET:${k.overBudgetCategories.size}")

                appendLine("---RATING---")
                when {
                    score >= 80 -> appendLine("RATING:EXCELLENT - Financial health is strong!")
                    score >= 60 -> appendLine("RATING:GOOD - Solid foundation with room to improve")
                    score >= 40 -> appendLine("RATING:FAIR - Some areas need attention")
                    else -> appendLine("RATING:NEEDS_WORK - Focus on fundamentals")
                }

                appendLine("---AGE_BENCHMARKS---")
                when {
                    age < 30 -> {
                        appendLine("BENCHMARK:Building phase - focus on habits and emergency fund")
                        appendLine("TARGET_SAVINGS:3+ months expenses in emergency fund")
                    }
                    age in 30..40 -> {
                        appendLine("BENCHMARK:Accumulation phase - maximize savings and growth")
                        appendLine("TARGET_SAVINGS:6 months expenses + retirement on track")
                    }
                    age in 41..55 -> {
                        appendLine("BENCHMARK:Peak earning years - accelerate wealth building")
                        appendLine("TARGET_SAVINGS:Full emergency fund + retirement catch-up if needed")
                    }
                    age > 55 -> {
                        appendLine("BENCHMARK:Preservation phase - protect and grow steadily")
                        appendLine("TARGET_SAVINGS:12+ months expenses + clear retirement path")
                    }
                }
            },
            dataUsed = listOf("income", "expenses", "loans", "goals", "bills", "budgets"),
            suggestions = when {
                score >= 80 -> listOf("Optimize investments", "Advanced strategies", "Stay consistent")
                score >= 60 -> listOf("Boost savings", "Pay down debt", "Set goals")
                score >= 40 -> listOf("Build emergency fund", "Budget strictly", "Track spending")
                else -> listOf("Cut expenses", "Increase income", "Seek guidance")
            },
            isAgeAdjusted = true,
            rawData = mapOf("score" to score, "riskLevel" to k.riskLevel)
        )
    }

    // =====================================================
    // ADVICE
    // =====================================================

    private fun answerAdvice(k: SystemKnowledge, age: Int): LearnerResponse {
        val priorities = mutableListOf<String>()

        // Determine priorities based on data
        if (k.overdueBills.isNotEmpty()) {
            priorities.add("URGENT:Pay ${k.overdueBills.size} overdue bill(s) immediately")
        }
        if (k.savingsRate < 10) {
            priorities.add("HIGH:Increase savings rate from ${k.savingsRate.toInt()}% to at least 10%")
        }
        if (k.debtToIncomeRatio > 40) {
            priorities.add("HIGH:Reduce debt-to-income ratio from ${k.debtToIncomeRatio.toInt()}%")
        }
        if (k.overBudgetCategories.isNotEmpty()) {
            priorities.add("MEDIUM:Review spending in ${k.overBudgetCategories.joinToString(", ")}")
        }
        if (k.savingsRate < 20 && k.savingsRate >= 10) {
            priorities.add("MEDIUM:Work towards 20% savings rate")
        }
        if (k.activeGoals.isEmpty()) {
            priorities.add("LOW:Set financial goals to stay motivated")
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("FINANCIAL_ADVICE")
                appendLine("PRIORITIES:${priorities.size}")

                appendLine("---PRIORITIES---")
                priorities.forEachIndexed { index, p ->
                    appendLine("${index + 1}.$p")
                }

                if (priorities.isEmpty()) {
                    appendLine("STATUS:EXCELLENT - No urgent issues!")
                    appendLine("FOCUS:Optimization and growth")
                }

                appendLine("---AGE_SPECIFIC---")
                when {
                    age < 25 -> {
                        appendLine("ADVICE:Focus on building habits. Automate savings. Avoid lifestyle inflation.")
                        appendLine("ACTION:Set up automatic 15% savings before you see it")
                    }
                    age in 25..35 -> {
                        appendLine("ADVICE:Balance present enjoyment with future security. Max out retirement matches.")
                        appendLine("ACTION:Increase savings by 1% each year until you reach 25%")
                    }
                    age in 36..50 -> {
                        appendLine("ADVICE:This is your highest earning potential. Maximize it.")
                        appendLine("ACTION:Eliminate all high-interest debt. Max retirement contributions.")
                    }
                    age in 51..60 -> {
                        appendLine("ADVICE:Final sprint to retirement. Catch up on savings if needed.")
                        appendLine("ACTION:Use catch-up contributions. Model retirement scenarios.")
                    }
                    age > 60 -> {
                        appendLine("ADVICE:Protect wealth. Plan for longevity.")
                        appendLine("ACTION:Review withdrawal strategy. Consider healthcare costs.")
                    }
                }
            },
            dataUsed = listOf("all"),
            suggestions = if (priorities.isNotEmpty())
                priorities.take(3).map { it.substringAfter(":").take(30) }
            else
                listOf("Review investments", "Set new goals", "Celebrate progress"),
            isAgeAdjusted = true
        )
    }

    // =====================================================
    // COMPARISON
    // =====================================================

    private fun answerComparison(query: LearnerQuery, k: SystemKnowledge, age: Int): LearnerResponse {
        return LearnerResponse(
            answer = buildString {
                appendLine("COMPARISON_ANALYSIS")
                appendLine("CURRENT_SPENDING:${k.monthlyExpenses}")
                appendLine("TREND:${k.spendingTrend}")
                appendLine("PREDICTED_NEXT:${k.predictedNextMonthSpending}")

                val change = k.predictedNextMonthSpending - k.monthlyExpenses
                val changePercent = if (k.monthlyExpenses > 0) (change / k.monthlyExpenses * 100) else 0.0
                appendLine("PROJECTED_CHANGE:$change")
                appendLine("CHANGE_PERCENT:${changePercent.toInt()}%")
            },
            dataUsed = listOf("expenses", "trends"),
            suggestions = listOf("View breakdown", "Set limits")
        )
    }

    // =====================================================
    // SHOPPING
    // =====================================================

    private fun answerShoppingInfo(k: SystemKnowledge, age: Int): LearnerResponse {
        if (k.shoppingLists.isEmpty()) {
            return LearnerResponse(
                answer = "NO_SHOPPING_LISTS\nNo shopping lists found. Create one to budget your purchases!",
                suggestions = listOf("Create list", "Smart shopping tips")
            )
        }

        return LearnerResponse(
            answer = buildString {
                appendLine("SHOPPING_DATA")
                appendLine("TOTAL_LISTS:${k.shoppingLists.size}")
                appendLine("TOTAL_BUDGET:${k.totalShoppingBudget}")

                appendLine("---LISTS---")
                k.shoppingLists.forEach {
                    appendLine("LIST:${it.title}|${it.totalBudget}|${it.itemCount} items")
                }
            },
            dataUsed = listOf("shopping_lists"),
            suggestions = listOf("Create list", "Budget tips")
        )
    }

    // =====================================================
    // GENERAL
    // =====================================================

    private fun answerGeneral(query: LearnerQuery, k: SystemKnowledge, age: Int): LearnerResponse {
        return LearnerResponse(
            answer = buildString {
                appendLine("GENERAL_SNAPSHOT")
                appendLine("USER:${k.userName}")
                appendLine("AGE:$age")
                appendLine("HEALTH_SCORE:${k.financialHealthScore}")
                appendLine("RISK:${k.riskLevel}")
                appendLine("INCOME:${k.monthlyIncome}")
                appendLine("EXPENSES:${k.monthlyExpenses}")
                appendLine("SAVINGS_RATE:${k.savingsRate.toInt()}%")
                appendLine("LOANS:${k.activeLoans.size}")
                appendLine("GOALS:${k.activeGoals.size}")
                appendLine("BILLS_DUE:${k.unpaidBills.size}")

                appendLine("---QUICK_STATUS---")
                when {
                    k.financialHealthScore >= 80 -> appendLine("STATUS:Excellent financial health!")
                    k.financialHealthScore >= 60 -> appendLine("STATUS:Good standing, room to improve")
                    k.financialHealthScore >= 40 -> appendLine("STATUS:Some areas need attention")
                    else -> appendLine("STATUS:Focus on fundamentals")
                }
            },
            dataUsed = listOf("all"),
            suggestions = listOf("Check health", "View spending", "Get advice"),
            isAgeAdjusted = true
        )
    }

    /**
     * Direct query - for backward compatibility
     */
    fun directQuery(query: String): String = superBrain.answerQuery(query)

    /**
     * Get current knowledge state
     */
    fun getKnowledge(): SystemKnowledge = superBrain.knowledge.value

    // =====================================================
    // DELEGATE METHODS FOR IntelligentFinancialAssistant
    // =====================================================

    fun getUserName(): String = superBrain.knowledge.value.userName
    fun getUserAge(): Int = superBrain.knowledge.value.userAge
    fun getFinancialSummary(): FinancialSummaryData {
        val k = superBrain.knowledge.value
        return FinancialSummaryData(
            totalIncome = k.monthlyIncome,
            totalExpenses = k.monthlyExpenses,
            netSavings = k.monthlyIncome - k.monthlyExpenses,
            savingsRate = k.savingsRate,
            upcomingBillsTotal = k.upcomingBills.sumOf { it.amount },
            healthScore = k.financialHealthScore
        )
    }
    fun getRecentTransactions(days: Int): List<ExpenseRecord> {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return superBrain.knowledge.value.expenses.filter { it.date >= cutoff }
    }
    fun getUpcomingBills(days: Int): List<BillRecord> = superBrain.knowledge.value.upcomingBills.filter { it.daysUntilDue <= days }
    fun getActiveGoals(): List<GoalRecord> = superBrain.knowledge.value.activeGoals
    fun getActiveLoans(): List<LoanRecord> = superBrain.knowledge.value.activeLoans

    /**
     * Cleanup
     */
    fun cleanup() {
        scope.cancel()
        superBrain.cleanup()
    }
}

// Data class for financial summary
data class FinancialSummaryData(
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,
    val savingsRate: Double,
    val upcomingBillsTotal: Double,
    val healthScore: Int
)

