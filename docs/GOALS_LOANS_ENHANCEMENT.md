# Budgie Goals & Loans Enhancement

## Overview

This document describes the comprehensive enhancement to the Goals and Loans system in Budgie, including:
- Auto-calculation of saving amounts per period
- AI-powered goal probability analysis
- Calendar picker for start dates
- Goal-linked loans with automatic creation
- Goal Loans tab in Loans screen

**Files Modified:**
- `app/src/main/java/com/example/budgie/ui/screens/GoalsScreen.kt`
- `app/src/main/java/com/example/budgie/ui/screens/LoansScreen.kt`
- `app/src/main/java/com/example/budgie/data/model/GoalModels.kt`
- `app/src/main/java/com/example/budgie/data/model/LoanModels.kt`

**Created:** December 26, 2025

---

## New Features

### 1. Auto-Calculated Saving Amounts

**Purpose:** Automatically calculate how much the user needs to save per period based on their goal.

**Calculation Logic:**

```kotlin
// For daily savings:
dailyAmount = targetAmount / (months * 30)

// For weekly savings:
weeklyAmount = dailyAmount * 7

// For bi-weekly savings:
biweeklyAmount = dailyAmount * 14

// For monthly savings:
monthlyAmount = targetAmount / months
```

**For Mixed Funding (Savings + Loan):**
- Only the savings portion is calculated
- Loan portion uses the monthly payment from loan calculator

**UI Display:**
- Shows calculated amount in a highlighted card
- Format: "$X.XX per [frequency]"
- Updates in real-time as user changes inputs

---

### 2. AI Goal Probability Analysis

**Purpose:** Provide users with intelligent feedback on goal achievability.

**Algorithm:**

```kotlin
// Calculate monthly savings required
monthlySavingRequired = savingPerPeriod converted to monthly equivalent

// Calculate percentage of monthly savings this goal uses
percentageOfSavings = (monthlySavingRequired / userMonthlySavings) * 100

// Determine probability
probability = when {
    percentageOfSavings <= 30 -> 0.95  // Very achievable
    percentageOfSavings <= 50 -> 0.85  // Achievable
    percentageOfSavings <= 70 -> 0.70  // Moderate
    percentageOfSavings <= 90 -> 0.50  // Challenging
    percentageOfSavings <= 100 -> 0.30 // Difficult
    else -> 0.15                        // Very difficult
}
```

**Insight Levels (Color-coded):**

| Savings % | Level | Color | Description |
|-----------|-------|-------|-------------|
| < 60% | Green | `#0FAE96` | Highly achievable |
| 60-90% | Amber | `#FFB74D` | Moderate challenge |
| > 90% | Red | `#E57373` | Difficult/risky |

**UI Components:**
- Circular progress indicator showing probability
- Color-coded insight card
- Percentage breakdown
- AI-generated recommendation text

---

### 3. Start Date Calendar Picker

**Purpose:** Allow users to specify when they want to start saving for a goal.

**Features:**
- Only future dates are selectable
- Today is always available
- Shows formatted date (MMM dd, yyyy)
- Visual indicator for "Start Today" vs custom date

**Validation:**
- Date must be >= today
- Past dates are disabled in picker

**Impact on Goal:**
- Goals with future start dates are marked as "not started"
- UI shows "Start Now" button for future goals

---

### 4. Goal-Linked Loans

**Purpose:** Automatically create loans when user selects loan-based funding.

**Workflow:**

1. User selects "Loan" or "Mixed" funding method
2. User configures loan parameters (rate, term, fees)
3. On goal creation, popup asks for lender name
4. Loan is automatically created with:
   - Title = Goal name
   - All calculated values pre-filled
   - `isGoalLoan = true` flag
   - `linkedGoalId` reference (if applicable)

**New Loan Fields:**

```kotlin
data class Loan(
    // ... existing fields ...
    val isGoalLoan: Boolean = false,      // Is this from a goal?
    val isLoanActive: Boolean = true,     // Is the loan started?
    val fees: Double = 0.0                // Processing fees
)
```

---

### 5. Goal Loans Tab

**Location:** Loans Screen

**Purpose:** Separate view for loans created from goals.

**Tab Structure:**
```
[All Loans] [Active] [Paid Off] [Overdue] [🎯 Goal Loans]
```

**Filtering Logic:**
- All Loans: Non-goal loans only
- Active: Active non-goal loans
- Paid Off: All paid off loans
- Overdue: All overdue loans
- Goal Loans: `loan.isGoalLoan == true`

**Visual Indicators:**
- Gold color for Goal Loans tab
- Badge showing count
- Flag icon on tab

---

### 6. Start Saving Now Button

**Purpose:** Allow users to activate future goals early.

**Location:** Goal Card footer

**Behavior:**
- Shows for goals where `savingStartDate > now && !isSavingStarted`
- Clicking updates goal: `isSavingStarted = true`
- Changes goal status from "Future" to "Active"

**UI:**
- Cyan colored button
- Play icon + "Start Now" text

---

## Data Model Changes

### FinancialGoal Updates

```kotlin
data class FinancialGoal(
    // ... existing fields ...
    
    // New fields
    val calculatedSavingAmount: Double? = null,  // Auto-calculated
    val savingStartDate: Long = System.currentTimeMillis(),
    val isSavingStarted: Boolean = false
)
```

### Loan Updates

```kotlin
data class Loan(
    // ... existing fields ...
    
    // New fields
    val isGoalLoan: Boolean = false,
    val isLoanActive: Boolean = true,
    val fees: Double = 0.0
)
```

---

## GoalsScreen Function Signature

```kotlin
@Composable
fun GoalsScreen(
    goals: List<FinancialGoal>,
    currentSavings: Double = 0.0,
    monthlyIncome: Double = 0.0,
    monthlyExpenses: Double = 0.0,
    monthlySavings: Double = 0.0,
    onAddGoal: (FinancialGoal) -> Unit,
    onUpdateGoal: (FinancialGoal) -> Unit,
    onDeleteGoal: (FinancialGoal) -> Unit,
    onAddContribution: (Long, Double) -> Unit,
    onStartSaving: (FinancialGoal) -> Unit = {},
    onAddGoalLoan: (Loan) -> Unit = {},
    onNavigateToLoans: ((Double, Double, Int, String) -> Unit)? = null,
    onNavigateBack: () -> Unit
)
```

---

## Helper Functions

### calculateSavingAmountPerPeriod

```kotlin
private fun calculateSavingAmountPerPeriod(
    totalAmount: Double,
    months: Int,
    frequency: SavingFrequency,
    fundingMethod: FundingMethod,
    loanMonthlyPayment: Double
): Double
```

### calculateGoalProbability

```kotlin
private fun calculateGoalProbability(
    targetAmount: Double,
    months: Int,
    monthlySavings: Double,
    savingPerPeriod: Double,
    frequency: SavingFrequency,
    fundingMethod: FundingMethod
): GoalProbabilityResult
```

---

## UI/UX Guidelines

### Color Scheme

| Element | Color | Hex |
|---------|-------|-----|
| Savings/Green insights | Emerald | `#0FAE96` |
| Loans | Blue | `#5C9CE5` |
| Mixed funding | Cyan | `#26A69A` |
| Warnings/Amber insights | Amber | `#FFB74D` |
| Errors/Red insights | Red | `#E57373` |
| Goal Loans tab | Gold | `#C9A14A` |

### Glassmorphic Cards

All major UI sections use glassmorphic styling:
- Semi-transparent backgrounds
- Subtle borders
- Shadow effects
- Gradient overlays

---

## Testing Scenarios

1. **Auto-calculation test:**
   - Enter $1200 target, 12 months, monthly frequency
   - Expected: $100.00/month displayed

2. **AI Probability test:**
   - Monthly savings: $500
   - Goal: $300/month required
   - Expected: Amber insight (~60% of savings)

3. **Date picker test:**
   - Try selecting past date → Should be disabled
   - Select future date → Shows "Start Now" button

4. **Loan creation test:**
   - Select "Loan" funding
   - Complete goal
   - Verify loan appears in Goal Loans tab

5. **Start saving test:**
   - Create goal with future start date
   - Click "Start Now"
   - Verify goal becomes active

---

## Future Enhancements

1. **Notifications:**
   - Remind users of saving dates
   - Alert when loan payment due

2. **Progress tracking:**
   - Visual charts of goal progress
   - Comparison of planned vs actual

3. **Smart recommendations:**
   - AI-suggested goal adjustments
   - Optimal funding mix suggestions

4. **Recurring contributions:**
   - Automatic savings scheduling
   - Integration with bank accounts

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | Dec 26, 2025 | Initial implementation |

---

## Author

Budgie Development Team

