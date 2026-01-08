package com.example.budgie.ui.navigation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.budgie.data.model.UserProfile
import com.example.budgie.data.preferences.UserPreferencesManager
import com.example.budgie.security.InactivityTimer
import com.example.budgie.security.SecurityManager
import com.example.budgie.ui.components.AutoLockWarningDialog
import com.example.budgie.ui.screens.*
import com.example.budgie.ui.screens.dashboard.FuturisticDashboard
import com.example.budgie.ui.viewmodel.MainViewModel
import com.example.budgie.ui.viewmodel.ModelDownloadViewModel
import java.util.Calendar

@Composable
fun BudgieNavigation(
    navController: NavHostController,
    viewModel: MainViewModel,
    securityManager: SecurityManager
) {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager.getInstance(context) }
    val userProfile by preferencesManager.userProfile.collectAsStateWithLifecycle(initialValue = null)

    // Shared ModelDownloadViewModel for Dashboard and AIChat screens
    val modelDownloadViewModel: ModelDownloadViewModel = viewModel(
        factory = ModelDownloadViewModel.Factory(context)
    )

    // Initialize isLocked based on security settings - locked if security is enabled and not authenticated
    val securityType = securityManager.getSecurityType()
    val initialLockState = securityType != SecurityManager.SECURITY_NONE && !securityManager.isAuthenticated()
    var isLocked by remember { mutableStateOf(initialLockState) }
    var showBirthdayCelebration by remember { mutableStateOf(false) }
    var birthdayAge by remember { mutableStateOf(0) }
    var birthdayChecked by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    // ===== INACTIVITY TIMER =====
    var showAutoLockWarning by remember { mutableStateOf(false) }

    val inactivityTimer = remember {
        InactivityTimer(
            onInactivityWarning = {
                android.util.Log.d("InactivityTimer", "⚠️ Showing auto-lock warning")
                showAutoLockWarning = true
            },
            onAutoLock = {
                android.util.Log.d("InactivityTimer", "🔒 Auto-locking app")
                showAutoLockWarning = false
                securityManager.setAuthenticated(false)
                isLocked = true
            }
        )
    }

    val countdownSeconds by inactivityTimer.countdownSeconds.collectAsStateWithLifecycle()

    // Start/stop timer based on lock state AND auto-lock preference
    LaunchedEffect(isLocked) {
        val autoLockEnabled = preferencesManager.getAutoLockEnabled()
        val hasSecurityEnabled = securityType != SecurityManager.SECURITY_NONE

        if (!isLocked && hasSecurityEnabled && autoLockEnabled) {
            android.util.Log.d("InactivityTimer", "▶️ Starting inactivity timer (user authenticated, auto-lock enabled)")
            inactivityTimer.start()
        } else {
            if (!autoLockEnabled) {
                android.util.Log.d("InactivityTimer", "🛑 Auto-lock disabled by user - timer stopped")
            } else {
                android.util.Log.d("InactivityTimer", "🛑 Stopping inactivity timer (user locked/no security)")
            }
            inactivityTimer.stop()
            showAutoLockWarning = false
        }
    }

    // Cleanup timer on dispose
    DisposableEffect(Unit) {
        onDispose {
            inactivityTimer.cleanup()
        }
    }
    // ===== END INACTIVITY TIMER =====

    // Log initial state
    android.util.Log.d("BudgieNav", "showSplash=$showSplash, userProfile=${userProfile?.name}, birthdayChecked=$birthdayChecked")

    // Show splash screen first
    if (showSplash) {
        SplashScreen(
            onSplashComplete = {
                android.util.Log.d("BudgieNav", "Splash complete!")
                showSplash = false
            }
        )
        return
    }

    // Check birthday AFTER splash completes - use showSplash as key to trigger when it changes
    LaunchedEffect(showSplash, userProfile) {
        android.util.Log.d("BirthdayCheck", "LaunchedEffect triggered - showSplash=$showSplash, profile=${userProfile?.name}")

        if (!birthdayChecked && userProfile != null && !showSplash) {
            birthdayChecked = true
            val profile = userProfile!!
            val today = Calendar.getInstance()

            android.util.Log.d("BirthdayCheck", "=== Birthday Check ===")
            android.util.Log.d("BirthdayCheck", "User: ${profile.name}, Birthday: '${profile.birthday}'")
            android.util.Log.d("BirthdayCheck", "Today: ${today.get(Calendar.MONTH)+1}/${today.get(Calendar.DAY_OF_MONTH)}")

            // Check if birthday celebration was already shown this year
            val alreadyShownThisYear = preferencesManager.wasBirthdayCelebrationShownThisYear()
            android.util.Log.d("BirthdayCheck", "Already shown this year: $alreadyShownThisYear")

            if (alreadyShownThisYear) {
                android.util.Log.d("BirthdayCheck", "Birthday celebration already shown this year - skipping")
                return@LaunchedEffect
            }

            // Check if today is user's birthday from stored profile
            val birthday = parseBirthday(profile.birthday)
            if (birthday != null) {
                val isBirthday = today.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) &&
                                 today.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH)

                android.util.Log.d("BirthdayCheck", "Parsed birthday: ${birthday.get(Calendar.MONTH)+1}/${birthday.get(Calendar.DAY_OF_MONTH)}")
                android.util.Log.d("BirthdayCheck", "Is birthday today: $isBirthday")

                if (isBirthday) {
                    birthdayAge = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR)
                    android.util.Log.d("BirthdayCheck", "🎂 Happy Birthday! Age: $birthdayAge")
                    showBirthdayCelebration = true
                }
            } else {
                android.util.Log.d("BirthdayCheck", "Could not parse birthday: '${profile.birthday}'")
            }
        }
    }

    // Check if app needs to be locked - use the already declared securityType
    val shouldShowLock = securityType != SecurityManager.SECURITY_NONE && userProfile != null && isLocked

    // Show birthday celebration first if it's user's birthday
    if (showBirthdayCelebration && userProfile != null) {
        BirthdayCelebrationScreen(
            userName = userProfile!!.name,
            age = birthdayAge,
            onDismiss = {
                // Mark birthday celebration as shown for this year
                preferencesManager.markBirthdayCelebrationShown()
                android.util.Log.d("BirthdayCheck", "Birthday celebration dismissed - marked as shown for this year")
                showBirthdayCelebration = false
            }
        )
    } else if (shouldShowLock) {
        LockScreen(
            securityManager = securityManager,
            onUnlocked = { isLocked = false }
        )
    } else {
        // Determine start destination based on onboarding status
        val startDestination = if (userProfile == null) {
            Screen.Onboarding.route
        } else {
            Screen.Dashboard.route
        }

        // Detect user interaction to reset inactivity timer
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures {
                    // Reset timer on any tap/interaction
                    inactivityTimer.resetTimer()
                }
            }
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = { name, birthday, secType, userPin ->
                        val profile = UserProfile(
                            name = name,
                            birthday = birthday
                        )
                        preferencesManager.saveUserProfile(profile)

                        // Save security settings
                        securityManager.setSecurityType(secType)
                        if ((secType == SecurityManager.SECURITY_PIN || secType == SecurityManager.SECURITY_PIN_BIOMETRIC) && userPin.length == 5) {
                            securityManager.savePin(userPin)
                        }
                        securityManager.setAuthenticated(true)
                        isLocked = false

                        // Navigate to dashboard and clear back stack
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                FuturisticDashboard(
                    viewModel = viewModel,
                    userName = userProfile?.name,
                    onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                    onNavigateToIncome = { navController.navigate(Screen.Income.route) },
                    onNavigateToBills = { navController.navigate(Screen.Bills.route) },
                    onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                    onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                    onNavigateToLoans = { navController.navigate(Screen.Loans.route) },
                    onNavigateToShoppingList = { navController.navigate(Screen.ShoppingList.route) },
                    onNavigateToAIChat = { navController.navigate(Screen.AIChat.route) },
                    onNavigateToExport = { navController.navigate(Screen.Export.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onAddIncome = { navController.navigate(Screen.AddIncome.route) },
                    // Menu navigation
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                    // Logout - go back to lock screen
                    onLogout = {
                        securityManager.setAuthenticated(false)
                        isLocked = true
                    },
                    // Exit - close the app
                    onExit = {
                        (context as? android.app.Activity)?.finishAffinity()
                    },
                    // Model download
                    modelDownloadViewModel = modelDownloadViewModel
                )
            }

        composable(Screen.Expenses.route) {
            ExpensesScreen(
                viewModel = viewModel,
                onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddExpense.route) {
            AddExpenseScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Income.route) {
            IncomeScreen(
                viewModel = viewModel,
                onAddIncome = { navController.navigate(Screen.AddIncome.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddIncome.route) {
            AddIncomeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Bills.route) {
            BillsScreen(
                viewModel = viewModel,
                onAddBill = { navController.navigate(Screen.AddBill.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddBill.route) {
            AddBillScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                viewModel = viewModel,
                onAddBudget = { navController.navigate(Screen.AddBudget.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddBudget.route) {
            AddBudgetScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Insights.route) {
            InsightsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Investments.route) {
            InvestmentsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WealthProjection.route) {
            WealthProjectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Export.route) {
            ExportScreen(
                viewModel = viewModel,
                userName = userProfile?.name,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Goals.route) {
            val goals by viewModel.goals.collectAsStateWithLifecycle(initialValue = emptyList())
            val summary by viewModel.financialSummary.collectAsStateWithLifecycle()
            GoalsScreen(
                goals = goals,
                currentSavings = summary.netSavings.coerceAtLeast(0.0),
                onAddGoal = { viewModel.addGoal(it) },
                onUpdateGoal = { viewModel.updateGoal(it) },
                onDeleteGoal = { viewModel.deleteGoal(it) },
                onAddContribution = { goalId, amount -> viewModel.addGoalContribution(goalId, amount) },
                onNavigateToLoans = { loanAmount, rate, termMonths, interestType ->
                    // Navigate to loans with pre-filled data (via route params or state)
                    navController.navigate(Screen.Loans.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Loans.route) {
            val loans by viewModel.loans.collectAsStateWithLifecycle(initialValue = emptyList())
            val loanPayments by viewModel.loanPayments.collectAsStateWithLifecycle(initialValue = emptyMap())
            val loanSummary by viewModel.loanSummary.collectAsStateWithLifecycle()
            val financialSummary by viewModel.financialSummary.collectAsStateWithLifecycle()
            LoansScreen(
                loans = loans,
                loanPayments = loanPayments,
                loanSummary = loanSummary,
                financialSummary = financialSummary,
                onAddLoan = { viewModel.addLoan(it) },
                onUpdateLoan = { viewModel.updateLoan(it) },
                onDeleteLoan = { viewModel.deleteLoan(it) },
                onAddPayment = { loanId, amount, ref -> viewModel.addLoanPayment(loanId, amount, ref) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ShoppingList.route) {
            val shoppingLists by viewModel.shoppingListsWithItems.collectAsStateWithLifecycle(initialValue = emptyList())
            val financialSummary by viewModel.financialSummary.collectAsStateWithLifecycle()
            ShoppingListScreen(
                shoppingLists = shoppingLists,
                monthlyIncome = financialSummary.totalIncome,
                monthlyExpenses = financialSummary.totalExpenses,
                onCreateList = { viewModel.createShoppingList(it) },
                onUpdateList = { viewModel.updateShoppingList(it) },
                onDeleteList = { viewModel.deleteShoppingList(it) },
                onAddItem = { viewModel.addShoppingItem(it) },
                onUpdateItem = { viewModel.updateShoppingItem(it) },
                onDeleteItem = { viewModel.deleteShoppingItem(it) },
                onAnalyzeList = { listId -> viewModel.analyzeShoppingList(listId) },
                onAddToBudget = { listId, amount -> viewModel.addShoppingListToBudget(listId, amount) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AIChat.route) {
            AIChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToRoute = { route ->
                    // Navigate to the appropriate screen based on route
                    when (route) {
                        "dashboard" -> navController.navigate(Screen.Dashboard.route)
                        "expenses" -> navController.navigate(Screen.Expenses.route)
                        "income" -> navController.navigate(Screen.Income.route)
                        "bills" -> navController.navigate(Screen.Bills.route)
                        "budget" -> navController.navigate(Screen.Budget.route)
                        "goals" -> navController.navigate(Screen.Goals.route)
                        "loans" -> navController.navigate(Screen.Loans.route)
                        "add_expense" -> navController.navigate(Screen.AddExpense.route)
                        "insights" -> navController.navigate(Screen.Insights.route)
                        else -> { /* Unknown route */ }
                    }
                }
            )
        }

        // ==================== Menu Screens ====================
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) }
            )
        }

        composable(Screen.Security.route) {
            SecuritySettingsScreen(
                securityManager = securityManager,
                onBack = { navController.popBackStack() },
                onNavigateToBiometricDiagnostics = { navController.navigate(Screen.BiometricDiagnostics.route) }
            )
        }

        composable(Screen.BiometricDiagnostics.route) {
            BiometricDiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Help.route) {
            HelpSupportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Terms.route) {
            TermsOfServiceScreen(
                onBack = { navController.popBackStack() }
            )
        }
        }

        // Show auto-lock warning dialog when inactivity detected
        if (showAutoLockWarning) {
            AutoLockWarningDialog(
                countdownSeconds = countdownSeconds,
                onStayActive = {
                    inactivityTimer.cancelAutoLock()
                    showAutoLockWarning = false
                },
                onLockNow = {
                    inactivityTimer.lockNow()
                    showAutoLockWarning = false
                }
            )
        }
    }
}

private fun parseBirthday(birthday: String): Calendar? {
    return try {
        android.util.Log.d("BirthdayParse", "Parsing: '$birthday'")
        val parts = birthday.split("/", "-")
        android.util.Log.d("BirthdayParse", "Parts: $parts (size: ${parts.size})")

        if (parts.size == 3) {
            val year = parts[0].trim().toIntOrNull()
            val month = parts[1].trim().toIntOrNull()
            val day = parts[2].trim().toIntOrNull()

            android.util.Log.d("BirthdayParse", "Parsed values: year=$year, month=$month, day=$day")

            if (year != null && month != null && day != null) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1) // Calendar months are 0-indexed
                    set(Calendar.DAY_OF_MONTH, day)
                }
            } else null
        } else null
    } catch (e: Exception) {
        android.util.Log.e("BirthdayParse", "Error parsing birthday: ${e.message}")
        null
    }
}

