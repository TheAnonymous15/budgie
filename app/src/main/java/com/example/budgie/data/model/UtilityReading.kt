package com.example.budgie.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "utility_readings")
data class UtilityReading(
    @PrimaryKey
    val id: String = BudgieIdGenerator.generateUtilityReadingId(),
    val utilityName: String,  // e.g., "Electricity", "Water", "Gas"
    val reading: Double,
    val costPerUnit: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

