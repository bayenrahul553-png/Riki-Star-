package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: ExpenseCategory,
    val monthlyLimit: Double,
    val monthYear: String // e.g. "2026-09"
)

@Entity(tableName = "reward_profile")
data class RewardProfileEntity(
    @PrimaryKey val id: Int = 1,
    val totalCoins: Int = 150,
    val currentStreakDays: Int = 3,
    val bestStreakDays: Int = 7,
    val tierName: String = "Silver Optimizer",
    val lastBudgetCheckEpochDay: Long = 0,
    val totalSavedAmount: Double = 320.0
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val currencyCode: String = "USD",
    val isPremium: Boolean = false,
    val cloudSyncEnabled: Boolean = false,
    val autoParseSmsEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0
)
