package com.example.budgie.data.local

import androidx.room.TypeConverter
import com.example.budgie.data.model.ExpenseCategory
import com.example.budgie.data.model.IncomeSource
import com.example.budgie.data.model.RecurringType
import com.example.budgie.data.model.ShoppingCategory
import com.example.budgie.data.model.ShoppingRecommendation

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
}
