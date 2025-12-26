package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isRecurring: Boolean = false,
    val recurringType: RecurringType? = null
)

enum class ExpenseCategory(val displayName: String, val icon: String) {
    FOOD("Food & Dining", "🍔"),
    TRANSPORT("Transport", "🚗"),
    UTILITIES("Utilities", "💡"),
    ENTERTAINMENT("Entertainment", "🎬"),
    SHOPPING("Shopping", "🛒"),
    HEALTH("Health", "🏥"),
    EDUCATION("Education", "📚"),
    RENT("Rent/Housing", "🏠"),
    INSURANCE("Insurance", "🛡️"),
    SAVINGS("Savings", "💰"),
    INVESTMENT("Investment", "📈"),
    OTHER("Other", "📦")
}

enum class RecurringType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}
