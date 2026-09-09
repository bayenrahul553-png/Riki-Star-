package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.RewardProfileEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val displayName: String,
    val conversionRateToUsd: Double
)

object SupportedCurrencies {
    val ALL = listOf(
        CurrencyInfo("USD", "$", "US Dollar (USD)", 1.0),
        CurrencyInfo("EUR", "€", "Euro (EUR)", 0.92),
        CurrencyInfo("GBP", "£", "British Pound (GBP)", 0.79),
        CurrencyInfo("INR", "₹", "Indian Rupee (INR)", 83.5),
        CurrencyInfo("JPY", "¥", "Japanese Yen (JPY)", 155.0),
        CurrencyInfo("CAD", "C$", "Canadian Dollar (CAD)", 1.36),
        CurrencyInfo("AUD", "A$", "Australian Dollar (AUD)", 1.52)
    )

    fun get(code: String): CurrencyInfo {
        return ALL.find { it.code.equals(code, ignoreCase = true) } ?: ALL.first()
    }
}

class FinanceRepository(private val database: AppDatabase) {

    private val expenseDao = database.expenseDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val rewardDao = database.rewardProfileDao()
    private val settingsDao = database.userSettingsDao()

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val rewardProfile: Flow<RewardProfileEntity?> = rewardDao.getRewardProfile()
    val userSettings: Flow<UserSettingsEntity?> = settingsDao.getUserSettings()

    suspend fun insertExpense(expense: Expense): Long = insertTransaction(expense)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        // Award gamification coins for logging/parsing!
        val rewardAmount = if (transaction.isAutoParsed) 15 else 5
        rewardDao.addCoins(rewardAmount)
        return id
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun updateRewardProfile(profile: RewardProfileEntity) {
        rewardDao.saveRewardProfile(profile)
    }

    suspend fun addRewardCoins(amount: Int) {
        rewardDao.addCoins(amount)
    }

    suspend fun updateUserSettings(settings: UserSettingsEntity) {
        settingsDao.saveUserSettings(settings)
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        val current = settingsDao.getUserSettingsSync() ?: UserSettingsEntity()
        settingsDao.saveUserSettings(current.copy(isPremium = isPremium))
    }

    suspend fun setCurrency(currencyCode: String) {
        val current = settingsDao.getUserSettingsSync() ?: UserSettingsEntity()
        settingsDao.saveUserSettings(current.copy(currencyCode = currencyCode))
    }

    suspend fun toggleCloudSync(enabled: Boolean) {
        val current = settingsDao.getUserSettingsSync() ?: UserSettingsEntity()
        settingsDao.saveUserSettings(current.copy(cloudSyncEnabled = enabled, lastSyncTimestamp = System.currentTimeMillis()))
    }

    suspend fun initializeDefaultDataIfEmpty() {
        val existingTx = allTransactions.firstOrNull()
        if (existingTx.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val day = 86400000L

            val seedTransactions = listOf(
                TransactionEntity(
                    title = "Tech Corp Payroll",
                    amount = 3850.00,
                    type = TransactionType.INCOME,
                    category = ExpenseCategory.SALARY_INCOME,
                    timestamp = now - (day * 3),
                    currencyCode = "USD",
                    accountRef = "••4102",
                    isAutoParsed = true,
                    rawSmsOrNote = "Chase: Direct deposit of $3,850.00 from TECH CORP received."
                ),
                TransactionEntity(
                    title = "Whole Foods Market",
                    amount = 84.50,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.GROCERIES,
                    timestamp = now - (day * 1),
                    currencyCode = "USD",
                    accountRef = "••4102",
                    isAutoParsed = true,
                    rawSmsOrNote = "Chase: You spent $84.50 at WHOLE FOODS with card 4102."
                ),
                TransactionEntity(
                    title = "Starbucks Reserve",
                    amount = 12.75,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.FOOD_DINING,
                    timestamp = now - (day * 1) + 3600000L,
                    currencyCode = "USD",
                    accountRef = "••8821",
                    isAutoParsed = true,
                    rawSmsOrNote = "Debit of $12.75 at Starbucks Reserve."
                ),
                TransactionEntity(
                    title = "Uber Airport Ride",
                    amount = 42.80,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.TRAVEL_TRANSIT,
                    timestamp = now - (day * 2),
                    currencyCode = "USD",
                    accountRef = "••4102",
                    isAutoParsed = false,
                    rawSmsOrNote = "Rideshare trip to SFO airport"
                ),
                TransactionEntity(
                    title = "Electric & Power Utility",
                    amount = 95.00,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.BILLS_UTILITIES,
                    timestamp = now - (day * 4),
                    currencyCode = "USD",
                    accountRef = "••9012",
                    isAutoParsed = true,
                    rawSmsOrNote = "Automatic payment of $95.00 scheduled for Pacific Electric."
                ),
                TransactionEntity(
                    title = "Netflix Premium",
                    amount = 15.49,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.ENTERTAINMENT,
                    timestamp = now - (day * 5),
                    currencyCode = "USD",
                    accountRef = "••8821",
                    isAutoParsed = true,
                    rawSmsOrNote = "Netflix.com charged $15.49 on card ending 8821."
                )
            )
            transactionDao.insertAll(seedTransactions)

            // Seed initial budgets
            val seedBudgets = listOf(
                BudgetEntity(ExpenseCategory.GROCERIES, 450.0, "2026-09"),
                BudgetEntity(ExpenseCategory.FOOD_DINING, 250.0, "2026-09"),
                BudgetEntity(ExpenseCategory.BILLS_UTILITIES, 400.0, "2026-09"),
                BudgetEntity(ExpenseCategory.TRAVEL_TRANSIT, 180.0, "2026-09"),
                BudgetEntity(ExpenseCategory.ENTERTAINMENT, 100.0, "2026-09"),
                BudgetEntity(ExpenseCategory.SHOPPING, 200.0, "2026-09"),
                BudgetEntity(ExpenseCategory.HEALTH, 80.0, "2026-09"),
                BudgetEntity(ExpenseCategory.INVESTMENTS, 300.0, "2026-09")
            )
            budgetDao.insertBudgets(seedBudgets)

            // Seed user settings and rewards
            rewardDao.saveRewardProfile(
                RewardProfileEntity(
                    id = 1,
                    totalCoins = 240,
                    currentStreakDays = 4,
                    bestStreakDays = 7,
                    tierName = "Bronze Saver",
                    totalSavedAmount = 450.0
                )
            )

            settingsDao.saveUserSettings(
                UserSettingsEntity(
                    id = 1,
                    currencyCode = "USD",
                    isPremium = false,
                    cloudSyncEnabled = true,
                    autoParseSmsEnabled = true,
                    biometricLockEnabled = false,
                    lastSyncTimestamp = System.currentTimeMillis() - 3600000L
                )
            )
        }
    }
}
