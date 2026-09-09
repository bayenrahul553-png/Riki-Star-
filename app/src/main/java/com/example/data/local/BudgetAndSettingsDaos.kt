package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.RewardProfileEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getBudgetForCategory(category: ExpenseCategory): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: ExpenseCategory)
}

@Dao
interface RewardProfileDao {
    @Query("SELECT * FROM reward_profile WHERE id = 1 LIMIT 1")
    fun getRewardProfile(): Flow<RewardProfileEntity?>

    @Query("SELECT * FROM reward_profile WHERE id = 1 LIMIT 1")
    suspend fun getRewardProfileSync(): RewardProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRewardProfile(profile: RewardProfileEntity)

    @Query("UPDATE reward_profile SET totalCoins = totalCoins + :amount WHERE id = 1")
    suspend fun addCoins(amount: Int)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getUserSettingsSync(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettingsEntity)
}
