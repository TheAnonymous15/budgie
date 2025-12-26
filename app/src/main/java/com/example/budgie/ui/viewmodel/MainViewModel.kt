package com.example.budgie.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgie.ai.FinancialAdvisor
import com.example.budgie.ai.ShoppingListAnalyzer
import com.example.budgie.ai.ml.BehaviorLearningEngine
import com.example.budgie.ai.ml.*
import com.example.budgie.ai.pipeline.DashboardSummary
import com.example.budgie.ai.pipeline.FinancialAIPipeline
import com.example.budgie.ai.pipeline.PipelineAnalysis
import com.example.budgie.ai.pipeline.TransactionAnalysis
import com.example.budgie.data.local.BudgieDatabase
import com.example.budgie.data.model.*
import com.example.budgie.data.model.RiskLevel as ModelRiskLevel
import com.example.budgie.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BudgieDatabase.getDatabase(application)
    private val repository = FinanceRepository(
        database.expenseDao(),
        database.incomeDao(),
        database.billDao(),
        database.budgetDao()
    )
    private val utilityReadingDao = database.utilityReadingDao()
    private val goalDao = database.goalDao()
    private val loanDao = database.loanDao()
    private val shoppingDao = database.shoppingDao()
    private val advisor = FinancialAdvisor(application)
    private val shoppingAnalyzer = ShoppingListAnalyzer()

    // Behavior Learning Engine (Offline ML)
    private val behaviorEngine = BehaviorLearningEngine(application)

    // AI Pipeline
    private val aiPipeline = FinancialAIPipeline.getInstance(application)

    // AI Pipeline Analysis State
    private val _aiAnalysis = MutableStateFlow<PipelineAnalysis?>(null)
    val aiAnalysis: StateFlow<PipelineAnalysis?> = _aiAnalysis.asStateFlow()

    private val _dashboardSummary = MutableStateFlow<DashboardSummary?>(null)
    val dashboardSummary: StateFlow<DashboardSummary?> = _dashboardSummary.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════════════
    // Behavior Learning Engine State
    // ═══════════════════════════════════════════════════════════════════════════════
    private val _userBehaviorProfile = MutableStateFlow<UserBehaviorProfile?>(null)
    val userBehaviorProfile: StateFlow<UserBehaviorProfile?> = _userBehaviorProfile.asStateFlow()

    private val _spendingPrediction = MutableStateFlow<BehaviorSpendingPrediction?>(null)
    val spendingPrediction: StateFlow<BehaviorSpendingPrediction?> = _spendingPrediction.asStateFlow()

    private val _incomePrediction = MutableStateFlow<IncomePrediction?>(null)
    val incomePrediction: StateFlow<IncomePrediction?> = _incomePrediction.asStateFlow()

    private val _riskAssessment = MutableStateFlow<RiskAssessment?>(null)
    val riskAssessment: StateFlow<RiskAssessment?> = _riskAssessment.asStateFlow()

    private val _anomalies = MutableStateFlow<List<AnomalyReport>>(emptyList())
    val anomalies: StateFlow<List<AnomalyReport>> = _anomalies.asStateFlow()

    private val _isBehaviorModelTrained = MutableStateFlow(false)
    val isBehaviorModelTrained: StateFlow<Boolean> = _isBehaviorModelTrained.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════════════
    // Goals State
    // ═══════════════════════════════════════════════════════════════════════════════
    val goals: StateFlow<List<FinancialGoal>> = goalDao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeGoals: StateFlow<List<FinancialGoal>> = goalDao.getActiveGoals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeGoalsCount: StateFlow<Int> = goalDao.getActiveGoalsCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Goals summary for dashboard
    val goalsSummary: StateFlow<GoalsSummary> = combine(
        goalDao.getActiveGoals(),
        goalDao.getTotalTargetAmount(),
        goalDao.getTotalCurrentAmount()
    ) { goals, totalTarget, totalCurrent ->
        GoalsSummary(
            activeGoals = goals.size,
            totalTargetAmount = totalTarget ?: 0.0,
            totalCurrentAmount = totalCurrent ?: 0.0,
            overallProgress = if ((totalTarget ?: 0.0) > 0)
                ((totalCurrent ?: 0.0) / (totalTarget ?: 1.0) * 100) else 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, GoalsSummary())

    // ═══════════════════════════════════════════════════════════════════════════════
    // Loans State
    // ═══════════════════════════════════════════════════════════════════════════════
    val loans: StateFlow<List<Loan>> = loanDao.getAllLoans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeLoans: StateFlow<List<Loan>> = loanDao.getActiveLoans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Loan payments by loan ID
    private val _loanPayments = MutableStateFlow<Map<Long, List<LoanPayment>>>(emptyMap())
    val loanPayments: StateFlow<Map<Long, List<LoanPayment>>> = _loanPayments.asStateFlow()

    // Loans summary for dashboard
    val loanSummary: StateFlow<LoanSummary> = combine(
        loanDao.getAllLoans(),
        loanDao.getTotalBorrowed(),
        loanDao.getTotalRepaid(),
        loanDao.getTotalRemaining(),
        loanDao.getOverdueLoansCount()
    ) { loans, borrowed, repaid, remaining, overdue ->
        LoanSummary(
            totalLoans = loans.size,
            activeLoans = loans.count { it.status == LoanStatus.ACTIVE },
            totalBorrowed = borrowed ?: 0.0,
            totalRepaid = repaid ?: 0.0,
            totalRemaining = remaining ?: 0.0,
            overdueLoans = overdue,
            nextPaymentAmount = loans.filter { it.status == LoanStatus.ACTIVE }
                .minByOrNull { it.nextPaymentDate }?.monthlyPayment ?: 0.0,
            nextPaymentDate = loans.filter { it.status == LoanStatus.ACTIVE }
                .minByOrNull { it.nextPaymentDate }?.nextPaymentDate
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, LoanSummary())

    // ═══════════════════════════════════════════════════════════════════════════════
    // Shopping List State
    // ═══════════════════════════════════════════════════════════════════════════════
    val shoppingLists: StateFlow<List<ShoppingList>> = shoppingDao.getAllShoppingLists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val shoppingListsWithItems: StateFlow<List<ShoppingListWithItems>> = shoppingDao.getAllShoppingListsWithItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeShoppingLists: StateFlow<List<ShoppingListWithItems>> = shoppingDao.getActiveShoppingListsWithItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Initialize AI Pipeline
        viewModelScope.launch {
            aiPipeline.initialize()
            refreshAiAnalysis()
        }

        // Load loan payments
        viewModelScope.launch {
            loans.collect { loanList ->
                val paymentsMap = mutableMapOf<Long, List<LoanPayment>>()
                loanList.forEach { loan ->
                    loanDao.getPaymentsByLoan(loan.id).collect { payments ->
                        paymentsMap[loan.id] = payments
                        _loanPayments.value = paymentsMap.toMap()
                    }
                }
            }
        }
    }

    // Current month/year for filtering
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Date range based on selected month/year
    private val dateRange = combine(selectedMonth, selectedYear) { month, year ->
        FinanceRepository.getMonthStartEnd(month, year)
    }.stateIn(viewModelScope, SharingStarted.Lazily, FinanceRepository.getMonthStartEnd(
        Calendar.getInstance().get(Calendar.MONTH) + 1,
        Calendar.getInstance().get(Calendar.YEAR)
    ))

    // Expenses
    val expenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentMonthExpenses: StateFlow<List<Expense>> = dateRange.flatMapLatest { (start, end) ->
        repository.getExpensesByDateRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Last 7 days expenses
    val last7DaysExpenses: StateFlow<List<Expense>> = flow {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.timeInMillis
        repository.getExpensesByDateRange(startDate, endDate).collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Incomes
    val incomes: StateFlow<List<Income>> = repository.getAllIncomes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Bills
    val unpaidBills: StateFlow<List<Bill>> = repository.getUnpaidBills()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val overdueBills: StateFlow<List<Bill>> = repository.getOverdueBills()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalUnpaidBills: StateFlow<Double> = repository.getTotalUnpaidBills()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // Budgets
    val currentBudgets: StateFlow<List<Budget>> = combine(selectedMonth, selectedYear) { month, year ->
        Pair(month, year)
    }.flatMapLatest { (month, year) ->
        repository.getBudgetsByMonth(month, year)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Financial Summary
    val financialSummary: StateFlow<FinancialSummary> = dateRange.flatMapLatest { (start, end) ->
        repository.getFinancialSummary(start, end)
    }.stateIn(viewModelScope, SharingStarted.Lazily, FinancialSummary())

    // AI Insights - now includes expenses for expense-specific insights
    val spendingInsights: StateFlow<List<SpendingInsight>> = combine(
        financialSummary,
        currentBudgets,
        currentMonthExpenses
    ) { summary, budgets, expenses ->
        advisor.generateSpendingInsights(summary, null, budgets, expenses)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // User settings
    private val _userSavings = MutableStateFlow(0.0)
    val userSavings: StateFlow<Double> = _userSavings.asStateFlow()

    private val _riskTolerance = MutableStateFlow(ModelRiskLevel.MEDIUM)
    val riskTolerance: StateFlow<ModelRiskLevel> = _riskTolerance.asStateFlow()

    private val _userAge = MutableStateFlow(30)
    val userAge: StateFlow<Int> = _userAge.asStateFlow()

    // Investment suggestions
    val investmentSuggestions: StateFlow<List<InvestmentSuggestion>> = combine(
        financialSummary,
        userSavings,
        riskTolerance,
        userAge
    ) { summary: FinancialSummary, savings: Double, risk: ModelRiskLevel, age: Int ->
        advisor.generateInvestmentSuggestions(summary, savings, risk, age)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Wealth projection
    val wealthProjection: StateFlow<WealthProjection> = combine(
        userSavings,
        financialSummary
    ) { savings, summary ->
        advisor.calculateWealthProjection(
            currentNetWorth = savings,
            monthlyContribution = summary.netSavings.coerceAtLeast(0.0)
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, WealthProjection())

    // Actions
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.addExpense(expense)
            invalidateAiCache()
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            invalidateAiCache()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            invalidateAiCache()
        }
    }

    fun addIncome(income: Income) {
        viewModelScope.launch {
            repository.addIncome(income)
            invalidateAiCache()
        }
    }

    fun updateIncome(income: Income) {
        viewModelScope.launch {
            repository.updateIncome(income)
            invalidateAiCache()
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            repository.deleteIncome(income)
            invalidateAiCache()
        }
    }

    fun addBill(bill: Bill) {
        viewModelScope.launch {
            repository.addBill(bill)
        }
    }

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            repository.updateBill(bill)
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun markBillAsPaid(billId: Long, isPaid: Boolean) {
        viewModelScope.launch {
            repository.markBillAsPaid(billId, isPaid)
        }
    }

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.addBudget(budget)
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun setSelectedMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun updateUserSavings(amount: Double) {
        _userSavings.value = amount
    }

    fun updateRiskTolerance(risk: ModelRiskLevel) {
        _riskTolerance.value = risk
    }

    fun updateUserAge(age: Int) {
        _userAge.value = age
    }

    // Utility Reading operations
    suspend fun getLastUtilityReading(utilityName: String): UtilityReading? {
        return utilityReadingDao.getLastReading(utilityName)
    }

    fun saveUtilityReading(utilityName: String, reading: Double, costPerUnit: Double, notes: String = "") {
        viewModelScope.launch {
            utilityReadingDao.insertReading(
                UtilityReading(
                    utilityName = utilityName,
                    reading = reading,
                    costPerUnit = costPerUnit,
                    notes = notes
                )
            )
        }
    }

    fun getUtilityReadingHistory(utilityName: String): Flow<List<UtilityReading>> {
        return utilityReadingDao.getReadingsByUtility(utilityName)
    }

    fun getAllUtilityNames(): Flow<List<String>> {
        return utilityReadingDao.getUtilityNames()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Goals Methods
    // ═══════════════════════════════════════════════════════════════════════════════

    fun addGoal(goal: FinancialGoal) {
        viewModelScope.launch {
            goalDao.insertGoal(goal)
        }
    }

    fun updateGoal(goal: FinancialGoal) {
        viewModelScope.launch {
            goalDao.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: FinancialGoal) {
        viewModelScope.launch {
            goalDao.deleteGoal(goal)
        }
    }

    fun addGoalContribution(goalId: Long, amount: Double) {
        viewModelScope.launch {
            goalDao.addContribution(goalId, amount)
            goalDao.insertContribution(
                GoalContribution(
                    goalId = goalId,
                    amount = amount
                )
            )
            // Check if goal is completed
            goalDao.getGoalById(goalId)?.let { goal ->
                if (goal.currentAmount + amount >= goal.targetAmount) {
                    goalDao.markGoalCompleted(goalId)
                }
            }
        }
    }

    fun getGoalContributions(goalId: Long): Flow<List<GoalContribution>> {
        return goalDao.getContributionsByGoal(goalId)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Loans Methods
    // ═══════════════════════════════════════════════════════════════════════════════

    fun addLoan(loan: Loan) {
        viewModelScope.launch {
            loanDao.insertLoan(loan)
        }
    }

    fun updateLoan(loan: Loan) {
        viewModelScope.launch {
            loanDao.updateLoan(loan)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanDao.deleteLoan(loan)
        }
    }

    fun addLoanPayment(loanId: Long, amount: Double, reference: String = "") {
        viewModelScope.launch {
            // Get current loan
            loanDao.getLoanById(loanId)?.let { loan ->
                val newAmountPaid = loan.amountPaid + amount
                val balanceAfterPayment = loan.totalAmount - newAmountPaid

                // Insert payment record
                loanDao.insertPayment(
                    LoanPayment(
                        loanId = loanId,
                        amount = amount,
                        dueDate = loan.nextPaymentDate,
                        referenceNumber = reference,
                        balanceAfterPayment = balanceAfterPayment.coerceAtLeast(0.0)
                    )
                )

                // Update loan amount paid
                loanDao.addPayment(loanId, amount)

                // Calculate next payment date
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = loan.nextPaymentDate
                calendar.add(Calendar.MONTH, 1)
                loanDao.updateNextPaymentDate(loanId, calendar.timeInMillis)

                // Check if loan is paid off
                if (newAmountPaid >= loan.totalAmount) {
                    loanDao.updateLoanStatus(loanId, LoanStatus.PAID_OFF)
                }
            }
        }
    }

    fun getLoanPayments(loanId: Long): Flow<List<LoanPayment>> {
        return loanDao.getPaymentsByLoan(loanId)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Shopping List Methods
    // ═══════════════════════════════════════════════════════════════════════════════

    fun createShoppingList(list: ShoppingList) {
        viewModelScope.launch {
            shoppingDao.insertShoppingList(list)
        }
    }

    fun updateShoppingList(list: ShoppingList) {
        viewModelScope.launch {
            shoppingDao.updateShoppingList(list)
        }
    }

    fun deleteShoppingList(list: ShoppingList) {
        viewModelScope.launch {
            shoppingDao.deleteShoppingList(list)
        }
    }

    fun addShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.insertShoppingItem(item)
            // Update list total
            updateShoppingListTotal(item.listId)
        }
    }

    fun updateShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.updateShoppingItem(item)
            updateShoppingListTotal(item.listId)
        }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.deleteShoppingItem(item)
            updateShoppingListTotal(item.listId)
        }
    }

    private suspend fun updateShoppingListTotal(listId: Long) {
        val listWithItems = shoppingDao.getShoppingListWithItems(listId)
        listWithItems?.let {
            val total = it.items.sumOf { item -> item.estimatedPrice * item.quantity }
            shoppingDao.updateListTotal(listId, total)
        }
    }

    fun analyzeShoppingList(listId: Long) {
        viewModelScope.launch {
            val listWithItems = shoppingDao.getShoppingListWithItems(listId) ?: return@launch
            val summary = financialSummary.value

            val analysis = shoppingAnalyzer.analyzeShoppingList(
                items = listWithItems.items,
                budget = listWithItems.shoppingList.totalBudget,
                monthlyIncome = summary.totalIncome,
                monthlyExpenses = summary.totalExpenses,
                savingsRate = summary.savingsRate
            )

            // Update items with AI recommendations
            analysis.itemAnalysis.forEach { itemAnalysis ->
                shoppingDao.updateItemAIAnalysis(
                    itemId = itemAnalysis.itemId,
                    recommendation = itemAnalysis.recommendation,
                    suggestedQty = itemAnalysis.suggestedQuantity,
                    reason = itemAnalysis.reason
                )
            }
        }
    }

    fun addShoppingListToBudget(listId: Long, amount: Double) {
        viewModelScope.launch {
            val list = shoppingDao.getShoppingListById(listId) ?: return@launch
            val calendar = Calendar.getInstance()

            // Create a budget for shopping category
            val budget = Budget(
                category = ExpenseCategory.SHOPPING,
                limit = amount,
                month = calendar.get(Calendar.MONTH) + 1,
                year = calendar.get(Calendar.YEAR)
            )

            repository.addBudget(budget)

            // Mark list as completed
            shoppingDao.markListCompleted(listId)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // AI Pipeline Methods
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Refresh AI analysis
     */
    fun refreshAiAnalysis() {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val analysis = aiPipeline.runFullAnalysis(forceRefresh = true)
                _aiAnalysis.value = analysis
                _dashboardSummary.value = aiPipeline.getDashboardSummary()
            } catch (e: Exception) {
                // Handle error gracefully
                e.printStackTrace()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    /**
     * Get notification insight from AI
     */
    suspend fun getAiNotificationInsight(): String {
        return try {
            aiPipeline.getNotificationInsight()
        } catch (e: Exception) {
            "Keep tracking your finances!"
        }
    }

    /**
     * Analyze a single transaction
     */
    suspend fun analyzeTransaction(expense: Expense): TransactionAnalysis? {
        return try {
            aiPipeline.analyzeTransaction(expense)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Invalidate AI cache when data changes
     */
    private fun invalidateAiCache() {
        aiPipeline.invalidateCache()
        refreshAiAnalysis()
        // Also update behavior model predictions
        refreshBehaviorPredictions()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Behavior Learning Engine Methods
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Train the behavior learning model on historical data
     */
    fun trainBehaviorModel() {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val expenseList = expenses.value
                val incomeList = incomes.value

                val result = behaviorEngine.trainModel(expenseList, incomeList)

                if (result.success) {
                    _isBehaviorModelTrained.value = true
                    _userBehaviorProfile.value = behaviorEngine.getUserProfile()

                    // Generate initial predictions
                    refreshBehaviorPredictions()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    /**
     * Refresh behavior predictions
     */
    private fun refreshBehaviorPredictions() {
        viewModelScope.launch {
            try {
                val expenseList = expenses.value
                val incomeList = incomes.value

                if (expenseList.isNotEmpty()) {
                    _spendingPrediction.value = behaviorEngine.predictSpending(30, expenseList)
                    _anomalies.value = behaviorEngine.detectAnomalies(expenseList)
                }

                if (incomeList.isNotEmpty()) {
                    _incomePrediction.value = behaviorEngine.predictIncome(30, incomeList)
                }

                if (expenseList.isNotEmpty() || incomeList.isNotEmpty()) {
                    _riskAssessment.value = behaviorEngine.assessRisk(expenseList, incomeList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Predict goal completion probability
     */
    suspend fun predictGoalCompletion(
        targetAmount: Double,
        currentAmount: Double,
        monthsRemaining: Int
    ): GoalPrediction {
        return behaviorEngine.predictGoalCompletion(
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            monthsRemaining = monthsRemaining,
            expenses = expenses.value,
            incomes = incomes.value
        )
    }

    /**
     * Predict loan repayment probability
     */
    suspend fun predictLoanRepayment(
        loanAmount: Double,
        monthlyPayment: Double,
        interestRate: Double,
        months: Int
    ): LoanRepaymentPrediction {
        return behaviorEngine.predictLoanRepayment(
            loanAmount = loanAmount,
            monthlyPayment = monthlyPayment,
            interestRate = interestRate,
            months = months,
            expenses = expenses.value,
            incomes = incomes.value
        )
    }

    /**
     * Get spending patterns identified by the ML model
     */
    fun getSpendingPatterns(): List<SpendingPattern> {
        return behaviorEngine.getSpendingPatterns()
    }

    /**
     * Get user behavior profile
     */
    fun getBehaviorProfile(): UserBehaviorProfile? {
        return behaviorEngine.getUserProfile()
    }

    override fun onCleared() {
        super.onCleared()
        aiPipeline.cleanup()
    }
}

