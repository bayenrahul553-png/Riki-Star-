package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing an Expense or financial transaction stored locally.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val timestamp: Long = System.currentTimeMillis(),
    val rawSmsOrNote: String? = null,
    val currencyCode: String = "USD",
    val accountRef: String? = null,
    val isAutoParsed: Boolean = false
)
