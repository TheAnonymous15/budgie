package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val id: String = BudgieIdGenerator.generateBudgetId(),
    val category: ExpenseCategory,
    val limit: Double,
    val month: Int, // 1-12
    val year: Int
)

