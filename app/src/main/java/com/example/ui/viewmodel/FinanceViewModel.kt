package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiBudgetAdvisor
import com.example.data.ai.MonthlyAnalyticsSummary
import com.example.data.local.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.RewardProfileEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import com.example.data.parser.ParsedTransaction
import com.example.data.parser.SmsTransactionParser
import com.example.data.repository.CurrencyInfo
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SupportedCurrencies
import com.example.data.rewards.GamificationTier
import com.example.data.rewards.RewardEngine
import com.example.data.rewards.RewardQuest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val rewardProfile: RewardProfileEntity = RewardProfileEntity(),
    val userSettings: UserSettingsEntity = UserSettingsEntity(),
    val currentCurrency: CurrencyInfo = SupportedCurrencies.ALL.first(),
    val analyticsSummary: MonthlyAnalyticsSummary? = null,
    val currentTier: GamificationTier = RewardEngine.TIERS.first(),
    val nextTierInfo: Pair<GamificationTier, Float> = Pair(RewardEngine.TIERS[1], 0f),
    val dailyQuests: List<RewardQuest> = emptyList(),
    val isSyncing: Boolean = false,
    val isRewardedAdLoading: Boolean = false,
    val activeSnackbarMessage: String? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }
    }

    private val _parsedPreview = MutableStateFlow<ParsedTransaction?>(null)
    val parsedPreview: StateFlow<ParsedTransaction?> = _parsedPreview

    private val _isSyncing = MutableStateFlow(false)
    private val _isRewardedAdLoading = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    private data class RepositoryData(
        val transactions: List<TransactionEntity> = emptyList(),
        val budgets: List<BudgetEntity> = emptyList(),
        val rewardProfile: RewardProfileEntity? = null,
        val userSettings: UserSettingsEntity? = null
    )

    private val _repoDataFlow = combine(
        repository.allTransactions,
        repository.allBudgets,
        repository.rewardProfile,
        repository.userSettings
    ) { txs, budgets, rewardProfile, settings ->
        RepositoryData(txs, budgets, rewardProfile, settings)
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        _repoDataFlow,
        _isSyncing,
        _isRewardedAdLoading,
        _snackbarMessage
    ) { repoData, isSyncing, isRewardedAdLoading, snackbarMsg ->
        val safeProfile = repoData.rewardProfile ?: RewardProfileEntity()
        val safeSettings = repoData.userSettings ?: UserSettingsEntity()
        val currency = SupportedCurrencies.get(safeSettings.currencyCode)

        val analytics = AiBudgetAdvisor.analyze(
            transactions = repoData.transactions,
            budgets = repoData.budgets,
            currencySymbol = currency.symbol
        )

        val tier = RewardEngine.determineTier(safeProfile.totalCoins)
        val nextTierInfo = RewardEngine.calculateNextTierProgress(safeProfile.totalCoins)

        val todayTxCount = repoData.transactions.count { it.isAutoParsed }
        val isUnderBudget = analytics.overallBudgetPercentage <= 1.0f
        val quests = RewardEngine.getDailyQuests(
            todayParsedCount = todayTxCount,
            isUnderDailyBudget = isUnderBudget,
            hasReviewedInsights = true
        )

        FinanceUiState(
            transactions = repoData.transactions,
            budgets = repoData.budgets,
            rewardProfile = safeProfile,
            userSettings = safeSettings,
            currentCurrency = currency,
            analyticsSummary = analytics,
            currentTier = tier,
            nextTierInfo = nextTierInfo,
            dailyQuests = quests,
            isSyncing = isSyncing,
            isRewardedAdLoading = isRewardedAdLoading,
            activeSnackbarMessage = snackbarMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    fun updateUserSettings(settings: UserSettingsEntity) {
        viewModelScope.launch {
            repository.updateUserSettings(settings)
        }
    }

    fun parseSmsInput(smsText: String) {
        val currency = uiState.value.currentCurrency.code
        val result = SmsTransactionParser.parse(smsText, currency)
        _parsedPreview.value = result
        if (result == null) {
            _snackbarMessage.value = "Could not detect transaction amount or details in the provided text."
        }
    }

    fun clearParsedPreview() {
        _parsedPreview.value = null
    }

    fun saveParsedTransaction() {
        val parsed = _parsedPreview.value ?: return
        viewModelScope.launch {
            repository.insertTransaction(parsed.toEntity())
            _parsedPreview.value = null
            _snackbarMessage.value = "Transaction saved! +15 Reward Coins earned! 🪙"
        }
    }

    fun addManualTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        note: String? = null
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.ifBlank { if (type == TransactionType.INCOME) "Income" else "Expense" },
                amount = amount,
                type = type,
                category = category,
                rawSmsOrNote = note,
                currencyCode = uiState.value.currentCurrency.code,
                isAutoParsed = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertTransaction(entity)
            _snackbarMessage.value = "Logged ${type.name.lowercase()} of ${uiState.value.currentCurrency.symbol}$amount. +5 Coins earned!"
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _snackbarMessage.value = "Transaction deleted."
        }
    }

    fun updateBudget(category: ExpenseCategory, limit: Double) {
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(
                    category = category,
                    monthlyLimit = limit,
                    monthYear = "2026-09"
                )
            )
            _snackbarMessage.value = "Updated ${category.displayName} budget to ${uiState.value.currentCurrency.symbol}$limit."
        }
    }

    fun claimDailyBonus() {
        viewModelScope.launch {
            val current = uiState.value.rewardProfile
            val updated = current.copy(
                totalCoins = current.totalCoins + 25,
                currentStreakDays = current.currentStreakDays + 1
            )
            repository.updateRewardProfile(updated)
            _snackbarMessage.value = "Daily Streak Bonus claimed! +25 Coins added! 🔥"
        }
    }

    fun watchRewardedAdForCoins(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isRewardedAdLoading.value = true
            // Simulate AdMob video ad presentation delay
            delay(2200)
            repository.addRewardCoins(50)
            _isRewardedAdLoading.value = false
            _snackbarMessage.value = "Rewarded Ad Completed! +50 Coins credited to your vault! 🏆"
            onComplete()
        }
    }

    fun toggleSubscription(isPremium: Boolean) {
        viewModelScope.launch {
            repository.setPremiumStatus(isPremium)
            val msg = if (isPremium) "Welcome to Smart Budget Pro! Ads removed & Pro features unlocked." else "Switched to standard Freemium tier."
            _snackbarMessage.value = msg
        }
    }

    fun selectCurrency(currencyCode: String) {
        viewModelScope.launch {
            repository.setCurrency(currencyCode)
            _snackbarMessage.value = "Display currency set to $currencyCode."
        }
    }

    fun syncCloudData() {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(1500) // Simulated end-to-end encrypted cloud sync
            repository.toggleCloudSync(true)
            _isSyncing.value = false
            _snackbarMessage.value = "Cloud Sync Successful: 100% of encrypted entries backed up."
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
