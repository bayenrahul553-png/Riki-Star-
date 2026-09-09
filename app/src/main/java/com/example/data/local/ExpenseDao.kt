package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for persisting and querying Expense / transaction data locally in Room.
 */
@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE type = :type ORDER BY timestamp DESC")
    fun getExpensesByType(type: TransactionType): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY timestamp DESC")
    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getExpensesSince(startTime: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: ExpenseCategory): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getTransactionsSince(startTime: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'EXPENSE' AND timestamp >= :startTime")
    fun getTotalExpenseSince(startTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'INCOME' AND timestamp >= :startTime")
    fun getTotalIncomeSince(startTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'EXPENSE'")
    fun getTotalExpenses(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Update
    suspend fun updateTransaction(transaction: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Delete
    suspend fun deleteTransaction(transaction: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

/**
 * Backward compatibility alias for transaction operations.
 */
typealias TransactionDao = ExpenseDao
