package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val source: IncomeSource,
    val date: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = true,
    val recurringType: RecurringType = RecurringType.MONTHLY
)

enum class IncomeSource(val displayName: String, val icon: String) {
    SALARY("Salary", "💼"),
    FREELANCE("Freelance", "💻"),
    BUSINESS("Business", "🏪"),
    INVESTMENT("Investment Returns", "📈"),
    RENTAL("Rental Income", "🏠"),
    GIFT("Gift", "🎁"),
    OTHER("Other", "💵")
}

