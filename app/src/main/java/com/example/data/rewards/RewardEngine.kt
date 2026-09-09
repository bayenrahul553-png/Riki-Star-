package com.example.data.rewards

import com.example.data.model.RewardProfileEntity

data class GamificationTier(
    val name: String,
    val minCoins: Int,
    val maxCoins: Int,
    val perkDescription: String,
    val badgeIconName: String
)

data class RewardQuest(
    val id: String,
    val title: String,
    val description: String,
    val coinReward: Int,
    val isCompleted: Boolean,
    val progress: Float // 0f to 1f
)

object RewardEngine {

    val TIERS = listOf(
        GamificationTier("Bronze Saver", 0, 250, "Standard analytics & basic coin earnings", "shield"),
        GamificationTier("Silver Optimizer", 251, 600, "5% bonus coin multiplier & category breakdowns", "military_tech"),
        GamificationTier("Gold Wealth Builder", 601, 1200, "10% bonus coin multiplier & Pro budget alerts", "workspace_premium"),
        GamificationTier("Platinum Investor", 1201, 2500, "20% bonus coins & 1 Free Ad-Pass per month", "stars"),
        GamificationTier("Diamond Prodigy", 2501, 999999, "VIP golden theme & double daily bonus streak", "diamond")
    )

    fun determineTier(coins: Int): GamificationTier {
        return TIERS.find { coins in it.minCoins..it.maxCoins } ?: TIERS.last()
    }

    fun calculateNextTierProgress(coins: Int): Pair<GamificationTier, Float> {
        val currentTier = determineTier(coins)
        val currentIndex = TIERS.indexOf(currentTier)
        if (currentIndex == TIERS.size - 1) {
            return Pair(currentTier, 1.0f)
        }
        val nextTier = TIERS[currentIndex + 1]
        val range = (nextTier.minCoins - currentTier.minCoins).toFloat()
        val currentWithinRange = (coins - currentTier.minCoins).toFloat()
        val progress = (currentWithinRange / range).coerceIn(0f, 1f)
        return Pair(nextTier, progress)
    }

    fun getDailyQuests(
        todayParsedCount: Int,
        isUnderDailyBudget: Boolean,
        hasReviewedInsights: Boolean
    ): List<RewardQuest> {
        return listOf(
            RewardQuest(
                id = "quest_sms_parse",
                title = "Smart Auto-Parser",
                description = "Parse at least 1 SMS or receipt transaction today",
                coinReward = 15,
                isCompleted = todayParsedCount >= 1,
                progress = (todayParsedCount / 1f).coerceIn(0f, 1f)
            ),
            RewardQuest(
                id = "quest_under_budget",
                title = "Discipline Master",
                description = "Keep total daily expenditure below the daily limit",
                coinReward = 25,
                isCompleted = isUnderDailyBudget,
                progress = if (isUnderDailyBudget) 1f else 0.4f
            ),
            RewardQuest(
                id = "quest_review_ai",
                title = "Financial Sage",
                description = "Review AI budget recommendations & analytics",
                coinReward = 10,
                isCompleted = hasReviewedInsights,
                progress = if (hasReviewedInsights) 1f else 0f
            ),
            RewardQuest(
                id = "quest_streak_maintain",
                title = "Consistency Champion",
                description = "Maintain your daily streak of under-budget days",
                coinReward = 30,
                isCompleted = true,
                progress = 1f
            )
        )
    }
}
