package com.example.budgie.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budgie.data.model.Bill
import com.example.budgie.data.model.Budget
import com.example.budgie.data.model.BudgieNotification
import com.example.budgie.data.model.Expense
import com.example.budgie.data.model.FinancialGoal
import com.example.budgie.data.model.GoalContribution
import com.example.budgie.data.model.Income
import com.example.budgie.data.model.Loan
import com.example.budgie.data.model.LoanPayment
import com.example.budgie.data.model.UtilityReading
import com.example.budgie.data.model.ShoppingList
import com.example.budgie.data.model.ShoppingItem
// New notification entities
import com.example.budgie.data.model.AppNotification
import com.example.budgie.data.model.BillNotificationDetail
import com.example.budgie.data.model.BudgetNotificationDetail
import com.example.budgie.data.model.GoalNotificationDetail
import com.example.budgie.data.model.LoanNotificationDetail
import com.example.budgie.data.model.ExpenseNotificationDetail
import com.example.budgie.data.model.IncomeNotificationDetail
import com.example.budgie.data.model.SecurityNotificationDetail
import com.example.budgie.data.model.SystemNotificationDetail
import com.example.budgie.data.model.ShoppingNotificationDetail
import com.example.budgie.data.model.InvestmentNotificationDetail
import com.example.budgie.data.model.InsightNotificationDetail

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
        ShoppingItem::class,
        BudgieNotification::class,
        // Notification entities
        AppNotification::class,
        BillNotificationDetail::class,
        BudgetNotificationDetail::class,
        GoalNotificationDetail::class,
        LoanNotificationDetail::class,
        ExpenseNotificationDetail::class,
        IncomeNotificationDetail::class,
        SecurityNotificationDetail::class,
        SystemNotificationDetail::class,
        ShoppingNotificationDetail::class,
        InvestmentNotificationDetail::class,
        InsightNotificationDetail::class
    ],
    version = 6,
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
    abstract fun notificationDao(): NotificationDao
    abstract fun appNotificationDao(): AppNotificationDao

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

