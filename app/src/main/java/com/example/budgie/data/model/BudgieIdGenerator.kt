package com.example.budgie.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Budgie Unique ID Generator
 *
 * Generates super unique IDs in format: BG-{PRODUCT}-{yyyyMMdd-HHmmss}{5-digit-milliseconds}
 *
 * Product Codes:
 * - EXP: Expense
 * - INC: Income
 * - BIL: Bill
 * - BUD: Budget
 * - LON: Loan
 * - LPM: Loan Payment
 * - GOL: Goal
 * - GCT: Goal Contribution
 * - SHL: Shopping List
 * - SHI: Shopping Item
 * - UTR: Utility Reading
 * - NOT: Notification (main)
 * - NBL: Bill Notification Detail
 * - NBG: Budget Notification Detail
 * - NGL: Goal Notification Detail
 * - NLN: Loan Notification Detail
 * - NEX: Expense Notification Detail
 * - NIN: Income Notification Detail
 * - NSC: Security Notification Detail
 * - NSY: System Notification Detail
 * - NSH: Shopping Notification Detail
 * - NIV: Investment Notification Detail
 * - NIS: Insight Notification Detail
 *
 * This format ensures uniqueness for millions of records:
 * - Timestamp precision to milliseconds
 * - 5-digit milliseconds allows for 99999 records per second
 * - Product code segregation for clarity
 */
object BudgieIdGenerator {

    // Date format for timestamp portion
    private val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    // Counter for same-millisecond conflicts (thread-safe)
    private val counter = AtomicInteger(0)
    private var lastTimestamp = 0L

    /**
     * Product codes for different entity types
     */
    enum class ProductCode(val code: String) {
        EXPENSE("EXP"),
        INCOME("INC"),
        BILL("BIL"),
        BUDGET("BUD"),
        LOAN("LON"),
        LOAN_PAYMENT("LPM"),
        GOAL("GOL"),
        GOAL_CONTRIBUTION("GCT"),
        SHOPPING_LIST("SHL"),
        SHOPPING_ITEM("SHI"),
        UTILITY_READING("UTR"),
        NOTIFICATION("NOT"),
        NOTIFICATION_BILL("NBL"),
        NOTIFICATION_BUDGET("NBG"),
        NOTIFICATION_GOAL("NGL"),
        NOTIFICATION_LOAN("NLN"),
        NOTIFICATION_EXPENSE("NEX"),
        NOTIFICATION_INCOME("NIN"),
        NOTIFICATION_SECURITY("NSC"),
        NOTIFICATION_SYSTEM("NSY"),
        NOTIFICATION_SHOPPING("NSH"),
        NOTIFICATION_INVESTMENT("NIV"),
        NOTIFICATION_INSIGHT("NIS")
    }

    /**
     * Generates a unique Budgie ID
     * Format: BG-{PRODUCT}-{yyyyMMdd-HHmmss}{5-digit-counter}
     *
     * @param productCode The product type code
     * @return Unique ID string
     */
    @Synchronized
    fun generate(productCode: ProductCode): String {
        val currentTime = System.currentTimeMillis()

        // Reset counter if new millisecond
        if (currentTime != lastTimestamp) {
            counter.set(0)
            lastTimestamp = currentTime
        }

        val timestamp = dateFormat.format(Date(currentTime))
        val millisPart = (currentTime % 100000).toString().padStart(5, '0')
        val counterPart = counter.incrementAndGet().toString().padStart(2, '0')
        val hexSuffix = String.format("%06X", Random.nextInt(0xFFFFFF + 1))

        // Format: BG-EXP-20251230-153045-12345-01-A1B2C3
        return "BG-${productCode.code}-$timestamp$millisPart$counterPart-$hexSuffix"
    }

    /**
     * Convenience methods for each entity type
     */
    fun generateExpenseId(): String = generate(ProductCode.EXPENSE)
    fun generateIncomeId(): String = generate(ProductCode.INCOME)
    fun generateBillId(): String = generate(ProductCode.BILL)
    fun generateBudgetId(): String = generate(ProductCode.BUDGET)
    fun generateLoanId(): String = generate(ProductCode.LOAN)
    fun generateLoanPaymentId(): String = generate(ProductCode.LOAN_PAYMENT)
    fun generateGoalId(): String = generate(ProductCode.GOAL)
    fun generateGoalContributionId(): String = generate(ProductCode.GOAL_CONTRIBUTION)
    fun generateShoppingListId(): String = generate(ProductCode.SHOPPING_LIST)
    fun generateShoppingItemId(): String = generate(ProductCode.SHOPPING_ITEM)
    fun generateUtilityReadingId(): String = generate(ProductCode.UTILITY_READING)
    fun generateNotificationId(): String = generate(ProductCode.NOTIFICATION)
    fun generateBillNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_BILL)
    fun generateBudgetNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_BUDGET)
    fun generateGoalNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_GOAL)
    fun generateLoanNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_LOAN)
    fun generateExpenseNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_EXPENSE)
    fun generateIncomeNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_INCOME)
    fun generateSecurityNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_SECURITY)
    fun generateSystemNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_SYSTEM)
    fun generateShoppingNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_SHOPPING)
    fun generateInvestmentNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_INVESTMENT)
    fun generateInsightNotificationDetailId(): String = generate(ProductCode.NOTIFICATION_INSIGHT)

    /**
     * Validates if an ID matches the Budgie ID format
     */
    fun isValidBudgieId(id: String): Boolean {
        // Pattern: BG-XXX-YYYYMMDD-HHMMSS-NNNNNNN-HHHHHH (7 digits + 6 hex)
        val pattern = """^BG-[A-Z]{3}-\d{8}-\d{6}\d{7}-[A-F0-9]{6}$""".toRegex()
        return pattern.matches(id)
    }

    /**
     * Extracts the product code from a Budgie ID
     */
    fun extractProductCode(id: String): String? {
        if (!isValidBudgieId(id)) return null
        return id.substring(3, 6)
    }

    /**
     * Extracts the timestamp from a Budgie ID
     */
    fun extractTimestamp(id: String): Long? {
        if (!isValidBudgieId(id)) return null
        try {
            val dateStr = id.substring(7, 22) // YYYYMMDD-HHMMSS
            return dateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            return null
        }
    }
}

