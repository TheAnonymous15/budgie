package com.example.budgie.data.local

import androidx.room.TypeConverter
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.IncomeSource
import com.example.budgie.data.model.NotificationPriority
import com.example.budgie.data.model.NotificationType
import com.example.budgie.data.model.RecurringType
import com.example.budgie.data.model.ShoppingCategory
import com.example.budgie.data.model.ShoppingRecommendation
import com.example.budgie.data.model.AppNotificationPriority
import com.example.budgie.data.model.NotificationCategory
import com.example.budgie.data.model.NotificationStatus
import com.example.budgie.data.model.BillNotificationType
import com.example.budgie.data.model.BudgetNotificationType
import com.example.budgie.data.model.GoalNotificationType
import com.example.budgie.data.model.LoanNotificationType
import com.example.budgie.data.model.ExpenseNotificationType
import com.example.budgie.data.model.IncomeNotificationType
import com.example.budgie.data.model.SecurityNotificationType
import com.example.budgie.data.model.SystemNotificationType
import com.example.budgie.data.model.ShoppingNotificationType
import com.example.budgie.data.model.InvestmentNotificationType
import com.example.budgie.data.model.InsightNotificationType

class Converters {
    @TypeConverter
    fun fromExpenseCategory(category: ExpenseCategory): String = category.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)

    @TypeConverter
    fun fromIncomeSource(source: IncomeSource): String = source.name

    @TypeConverter
    fun toIncomeSource(value: String): IncomeSource = IncomeSource.valueOf(value)

    @TypeConverter
    fun fromRecurringType(type: RecurringType?): String? = type?.name

    @TypeConverter
    fun toRecurringType(value: String?): RecurringType? = value?.let { RecurringType.valueOf(it) }

    @TypeConverter
    fun fromShoppingCategory(category: ShoppingCategory): String = category.name

    @TypeConverter
    fun toShoppingCategory(value: String): ShoppingCategory = ShoppingCategory.valueOf(value)

    @TypeConverter
    fun fromShoppingRecommendation(recommendation: ShoppingRecommendation): String = recommendation.name

    @TypeConverter
    fun toShoppingRecommendation(value: String): ShoppingRecommendation = ShoppingRecommendation.valueOf(value)

    @TypeConverter
    fun fromNotificationType(type: NotificationType): String = type.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = NotificationType.valueOf(value)

    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority): String = priority.name

    @TypeConverter
    fun toNotificationPriority(value: String): NotificationPriority = NotificationPriority.valueOf(value)

    // ═══════════════════════════════════════════════════════════════════════════════
    // NEW NOTIFICATION CONVERTERS
    // ═══════════════════════════════════════════════════════════════════════════════

    // NotificationCategory
    @TypeConverter
    fun fromNotificationCategory(category: NotificationCategory): String = category.name

    @TypeConverter
    fun toNotificationCategory(value: String): NotificationCategory = NotificationCategory.valueOf(value)

    // NotificationStatus
    @TypeConverter
    fun fromNotificationStatus(status: NotificationStatus): String = status.name

    @TypeConverter
    fun toNotificationStatus(value: String): NotificationStatus = NotificationStatus.valueOf(value)

    // BillNotificationType
    @TypeConverter
    fun fromBillNotificationType(type: BillNotificationType): String = type.name

    @TypeConverter
    fun toBillNotificationType(value: String): BillNotificationType = BillNotificationType.valueOf(value)

    // BudgetNotificationType
    @TypeConverter
    fun fromBudgetNotificationType(type: BudgetNotificationType): String = type.name

    @TypeConverter
    fun toBudgetNotificationType(value: String): BudgetNotificationType = BudgetNotificationType.valueOf(value)

    // GoalNotificationType
    @TypeConverter
    fun fromGoalNotificationType(type: GoalNotificationType): String = type.name

    @TypeConverter
    fun toGoalNotificationType(value: String): GoalNotificationType = GoalNotificationType.valueOf(value)

    // LoanNotificationType
    @TypeConverter
    fun fromLoanNotificationType(type: LoanNotificationType): String = type.name

    @TypeConverter
    fun toLoanNotificationType(value: String): LoanNotificationType = LoanNotificationType.valueOf(value)

    // ExpenseNotificationType
    @TypeConverter
    fun fromExpenseNotificationType(type: ExpenseNotificationType): String = type.name

    @TypeConverter
    fun toExpenseNotificationType(value: String): ExpenseNotificationType = ExpenseNotificationType.valueOf(value)

    // IncomeNotificationType
    @TypeConverter
    fun fromIncomeNotificationType(type: IncomeNotificationType): String = type.name

    @TypeConverter
    fun toIncomeNotificationType(value: String): IncomeNotificationType = IncomeNotificationType.valueOf(value)

    // SecurityNotificationType
    @TypeConverter
    fun fromSecurityNotificationType(type: SecurityNotificationType): String = type.name

    @TypeConverter
    fun toSecurityNotificationType(value: String): SecurityNotificationType = SecurityNotificationType.valueOf(value)

    // SystemNotificationType
    @TypeConverter
    fun fromSystemNotificationType(type: SystemNotificationType): String = type.name

    @TypeConverter
    fun toSystemNotificationType(value: String): SystemNotificationType = SystemNotificationType.valueOf(value)

    // ShoppingNotificationType
    @TypeConverter
    fun fromShoppingNotificationType(type: ShoppingNotificationType): String = type.name

    @TypeConverter
    fun toShoppingNotificationType(value: String): ShoppingNotificationType = ShoppingNotificationType.valueOf(value)

    // InvestmentNotificationType
    @TypeConverter
    fun fromInvestmentNotificationType(type: InvestmentNotificationType): String = type.name

    @TypeConverter
    fun toInvestmentNotificationType(value: String): InvestmentNotificationType = InvestmentNotificationType.valueOf(value)

    // InsightNotificationType
    @TypeConverter
    fun fromInsightNotificationType(type: InsightNotificationType): String = type.name

    @TypeConverter
    fun toInsightNotificationType(value: String): InsightNotificationType = InsightNotificationType.valueOf(value)

    // AppNotificationPriority (from NotificationModels)
    @TypeConverter
    fun fromAppNotificationPriority(priority: AppNotificationPriority): String = priority.name

    @TypeConverter
    fun toAppNotificationPriority(value: String): AppNotificationPriority = AppNotificationPriority.valueOf(value)
}
