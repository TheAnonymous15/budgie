package com.example.budgie.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budgie.data.model.Bill
import com.example.budgie.data.model.Budget
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.FinancialGoal
import com.example.budgie.data.model.GoalContribution
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.Loan
import com.example.budgie.data.model.LoanPayment
import com.example.budgie.data.model.UtilityReading
import com.example.budgie.data.model.ShoppingList
import com.example.budgie.data.model.ShoppingItem

@Database(
    entities = [
        Expense::class,
        Income::class,
        Bill::class,
        Budget::class,
        UtilityReading::class,
        FinancialGoal::class,
        GoalContribution::class,
        Loan::class,
        LoanPayment::class,
        ShoppingList::class,
        ShoppingItem::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BudgieDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun billDao(): BillDao
    abstract fun budgetDao(): BudgetDao
    abstract fun utilityReadingDao(): UtilityReadingDao
    abstract fun goalDao(): GoalDao
    abstract fun loanDao(): LoanDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: BudgieDatabase? = null

        fun getDatabase(context: Context): BudgieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgieDatabase::class.java,
                    "budgie_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

