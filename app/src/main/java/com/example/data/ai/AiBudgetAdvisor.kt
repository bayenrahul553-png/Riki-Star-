package com.example.data.ai

import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Calendar

data class CategorySpendProgress(
    val category: ExpenseCategory,
    val spent: Double,
    val limit: Double,
    val percentage: Float, // 0.0 to 1.0+
    val remaining: Double,
    val isOverBudget: Boolean
)

data class AiFinancialInsight(
    val title: String,
    val description: String,
    val actionSuggestion: String,
    val severity: InsightSeverity,
    val category: ExpenseCategory?
)

enum class InsightSeverity {
    POSITIVE,
    NEUTRAL,
    WARNING,
    ALERT
}

data class MonthlyAnalyticsSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRatePercentage: Float,
    val totalBudgetLimit: Double,
    val overallBudgetPercentage: Float,
    val categoryBreakdowns: List<CategorySpendProgress>,
    val aiInsights: List<AiFinancialInsight>,
    val projectedMonthEndSpend: Double,
    val monthProgressPercentage: Float
)

object AiBudgetAdvisor {

    fun analyze(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        currencySymbol: String = "$"
    ): MonthlyAnalyticsSummary {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthProgress = currentDay.toFloat() / maxDaysInMonth.toFloat()

        var totalIncome = 0.0
        var totalExpense = 0.0

        val categoryExpenseMap = mutableMapOf<ExpenseCategory, Double>()
        for (cat in ExpenseCategory.entries) {
            categoryExpenseMap[cat] = 0.0
        }

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
                categoryExpenseMap[tx.category] = (categoryExpenseMap[tx.category] ?: 0.0) + tx.amount
            }
        }

        val budgetMap = budgets.associateBy { it.category }
        var totalBudgetLimit = 0.0

        val categoryProgressList = ExpenseCategory.entries
            .filter { it != ExpenseCategory.SALARY_INCOME }
            .map { cat ->
                val spent = categoryExpenseMap[cat] ?: 0.0
                val limit = budgetMap[cat]?.monthlyLimit ?: defaultBudgetFor(cat)
                totalBudgetLimit += limit
                val pct = if (limit > 0) (spent / limit).toFloat() else 0f
                CategorySpendProgress(
                    category = cat,
                    spent = spent,
                    limit = limit,
                    percentage = pct,
                    remaining = (limit - spent).coerceAtLeast(0.0),
                    isOverBudget = spent > limit
                )
            }
            .sortedByDescending { it.spent }

        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) (((netSavings / totalIncome) * 100.0).coerceIn(-100.0, 100.0)).toFloat() else 0f
        val overallBudgetPct = if (totalBudgetLimit > 0) (totalExpense / totalBudgetLimit).toFloat() else 0f

        val dailyBurnRate = if (currentDay > 0) totalExpense / currentDay else totalExpense
        val projectedMonthEndSpend = dailyBurnRate * maxDaysInMonth

        val insights = generateAiInsights(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            categoryProgressList = categoryProgressList,
            monthProgress = monthProgress,
            projectedSpend = projectedMonthEndSpend,
            totalBudgetLimit = totalBudgetLimit,
            currencySymbol = currencySymbol
        )

        return MonthlyAnalyticsSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRatePercentage = savingsRate,
            totalBudgetLimit = totalBudgetLimit,
            overallBudgetPercentage = overallBudgetPct,
            categoryBreakdowns = categoryProgressList,
            aiInsights = insights,
            projectedMonthEndSpend = projectedMonthEndSpend,
            monthProgressPercentage = monthProgress
        )
    }

    private fun generateAiInsights(
        totalIncome: Double,
        totalExpense: Double,
        categoryProgressList: List<CategorySpendProgress>,
        monthProgress: Float,
        projectedSpend: Double,
        totalBudgetLimit: Double,
        currencySymbol: String
    ): List<AiFinancialInsight> {
        val list = mutableListOf<AiFinancialInsight>()

        // 1. Overall Velocity Check
        if (projectedSpend > totalBudgetLimit && totalBudgetLimit > 0) {
            val overage = projectedSpend - totalBudgetLimit
            list.add(
                AiFinancialInsight(
                    title = "Burn Rate Alert: Budget Overrun Forecast",
                    description = "At your current pace of ${currencySymbol}${String.format("%.0f", totalExpense / (monthProgress * 30).coerceAtLeast(1f))}/day, you are projected to exceed your monthly ceiling by ${currencySymbol}${String.format("%.0f", overage)}.",
                    actionSuggestion = "Reduce discretionary dining and entertainment by ~15% for the remaining days.",
                    severity = InsightSeverity.WARNING,
                    category = null
                )
            )
        } else if (totalExpense < (totalBudgetLimit * monthProgress * 0.8) && totalBudgetLimit > 0) {
            val potentialSavings = totalBudgetLimit - projectedSpend
            list.add(
                AiFinancialInsight(
                    title = "Excellent Budget Discipline!",
                    description = "You're spending 20% below your anticipated month-to-date pace. You're on track to bank an extra ${currencySymbol}${String.format("%.0f", potentialSavings)} in savings.",
                    actionSuggestion = "Allocate excess surplus to your emergency fund or investment portfolio to compound reward coins.",
                    severity = InsightSeverity.POSITIVE,
                    category = null
                )
            )
        }

        // 2. High Category Spenders
        val topSpender = categoryProgressList.firstOrNull { it.spent > 0 }
        if (topSpender != null && topSpender.percentage > 0.75f && monthProgress < 0.6f) {
            list.add(
                AiFinancialInsight(
                    title = "${topSpender.category.displayName} Exhaustion Warning",
                    description = "You have consumed ${(topSpender.percentage * 100).toInt()}% of your ${topSpender.category.displayName} limit while only ${(monthProgress * 100).toInt()}% of the month has elapsed.",
                    actionSuggestion = "Cap non-essential orders in this category to ${currencySymbol}${String.format("%.0f", topSpender.remaining / 10)}/day.",
                    severity = InsightSeverity.ALERT,
                    category = topSpender.category
                )
            )
        }

        // 3. Subscriptions / Bills Insight
        val billsProgress = categoryProgressList.find { it.category == ExpenseCategory.BILLS_UTILITIES }
        if (billsProgress != null && billsProgress.spent > 0) {
            list.add(
                AiFinancialInsight(
                    title = "Recurring Bills & Subscription Audit",
                    description = "Fixed bills currently stand at ${currencySymbol}${String.format("%.0f", billsProgress.spent)} this month.",
                    actionSuggestion = "Audit unused streaming subs and negotiate broadband fees for instant $20-40 monthly recurring yield.",
                    severity = InsightSeverity.NEUTRAL,
                    category = ExpenseCategory.BILLS_UTILITIES
                )
            )
        }

        // 4. Default Positive encouragement if few insights
        if (list.size < 2) {
            list.add(
                AiFinancialInsight(
                    title = "Smart Habit Compounder",
                    description = "Tracking every transaction automatically with SMS parsing increases month-end net savings by an average of 14%.",
                    actionSuggestion = "Keep logging daily expenses to maintain your coin streak and unlock premium badges.",
                    severity = InsightSeverity.POSITIVE,
                    category = null
                )
            )
        }

        return list
    }

    fun defaultBudgetFor(category: ExpenseCategory): Double {
        return when (category) {
            ExpenseCategory.GROCERIES -> 450.0
            ExpenseCategory.FOOD_DINING -> 300.0
            ExpenseCategory.BILLS_UTILITIES -> 400.0
            ExpenseCategory.SHOPPING -> 200.0
            ExpenseCategory.TRAVEL_TRANSIT -> 180.0
            ExpenseCategory.ENTERTAINMENT -> 120.0
            ExpenseCategory.HEALTH -> 100.0
            ExpenseCategory.INVESTMENTS -> 250.0
            ExpenseCategory.OTHER -> 150.0
            ExpenseCategory.SALARY_INCOME -> 0.0
        }
    }
}
