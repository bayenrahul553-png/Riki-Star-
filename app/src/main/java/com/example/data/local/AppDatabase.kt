package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.BudgetEntity
import com.example.data.model.Expense
import com.example.data.model.RewardProfileEntity
import com.example.data.model.UserSettingsEntity

@Database(
    entities = [
        Expense::class,
        BudgetEntity::class,
        RewardProfileEntity::class,
        UserSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    fun transactionDao(): ExpenseDao = expenseDao()
    abstract fun budgetDao(): BudgetDao
    abstract fun rewardProfileDao(): RewardProfileDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_budget_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
