package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class ExpenseCategory(val displayName: String, val iconName: String) {
    FOOD_DINING("Dining & Food", "restaurant"),
    GROCERIES("Groceries", "shopping_cart"),
    SHOPPING("Shopping", "shopping_bag"),
    BILLS_UTILITIES("Bills & Utilities", "receipt"),
    SALARY_INCOME("Salary & Income", "payments"),
    TRAVEL_TRANSIT("Travel & Transit", "directions_car"),
    ENTERTAINMENT("Entertainment", "movie"),
    HEALTH("Health & Fitness", "fitness_center"),
    INVESTMENTS("Investments", "trending_up"),
    OTHER("Other", "category");

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}

/**
 * Backward compatibility typealias for Expense
 */
typealias TransactionEntity = Expense
