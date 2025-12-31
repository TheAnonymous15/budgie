package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey
    val id: String = BudgieIdGenerator.generateBillId(),
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val category: ExpenseCategory,
    val isPaid: Boolean = false,
    val isRecurring: Boolean = false,
    val recurringType: RecurringType? = null,
    val reminderDaysBefore: Int = 3
)

