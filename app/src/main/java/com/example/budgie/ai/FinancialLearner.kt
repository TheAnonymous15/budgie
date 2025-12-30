package com.example.budgie.ai

import android.content.Context

/**
 * FinancialLearner - Stub implementation
 * Will be implemented with real ML models later
 */
class FinancialLearner(private val context: Context) {

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

    suspend fun initialize() {
        // Will be implemented when ML models are ready
    }
}

