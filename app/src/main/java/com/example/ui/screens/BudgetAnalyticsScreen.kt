package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiFinancialInsight
import com.example.data.ai.CategorySpendProgress
import com.example.data.ai.InsightSeverity
import com.example.data.model.ExpenseCategory
import com.example.ui.theme.ChartColors
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceUiState
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun BudgetAnalyticsScreen(
    uiState: FinanceUiState,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val currency = uiState.currentCurrency
    val analytics = uiState.analyticsSummary

    var editingBudgetCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var newLimitInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Screen Title
        Column {
            Text(
                text = "Smart Budget & AI Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Predictive spending curves & automated optimization.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Category Spend Distribution Donut Chart
        if (analytics != null && analytics.totalExpense > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spend_chart_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Expense Distribution by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Donut Canvas
                    Box(
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val activeCategories = analytics.categoryBreakdowns.filter { it.spent > 0 }
                        val totalExp = analytics.totalExpense

                        Canvas(modifier = Modifier.size(180.dp)) {
                            var startAngle = -90f
                            activeCategories.forEachIndexed { index, cat ->
                                val sweep = ((cat.spent / totalExp) * 360f).toFloat()
                                val color = ChartColors[index % ChartColors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep - 2f, // Subtle gap between segments
                                    useCenter = false,
                                    style = Stroke(width = 30f, cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }

                        // Center stats
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Spent",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${currency.symbol}${String.format(Locale.US, "%,.0f", analytics.totalExpense)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chart Legend
                    val activeCategories = analytics.categoryBreakdowns.filter { it.spent > 0 }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCategories.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEachIndexed { itemIdx, cat ->
                                    val globalIndex = activeCategories.indexOf(cat)
                                    val color = ChartColors[globalIndex % ChartColors.size]
                                    val pct = ((cat.spent / analytics.totalExpense) * 100).toInt()

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${cat.category.displayName} ($pct%)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI-Driven Insights Section
        if (analytics != null && analytics.aiInsights.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Insights",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Smart Budget Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                analytics.aiInsights.forEach { insight ->
                    AiInsightCard(insight = insight)
                }
            }
        }

        // Category Budgets Breakdown & Targets
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budget Targets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap pencil to edit limits",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            analytics?.categoryBreakdowns?.forEach { catProgress ->
                CategoryBudgetRow(
                    progress = catProgress,
                    currencySymbol = currency.symbol,
                    onEditLimit = {
                        editingBudgetCategory = catProgress.category
                        newLimitInput = catProgress.limit.toInt().toString()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Edit Budget Limit Dialog
    if (editingBudgetCategory != null) {
        val cat = editingBudgetCategory!!
        AlertDialog(
            onDismissRequest = { editingBudgetCategory = null },
            title = { Text("Set ${cat.displayName} Limit") },
            text = {
                OutlinedTextField(
                    value = newLimitInput,
                    onValueChange = { newLimitInput = it },
                    label = { Text("Monthly Target (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = newLimitInput.toDoubleOrNull() ?: 0.0
                        if (limit > 0) {
                            viewModel.updateBudget(cat, limit)
                            editingBudgetCategory = null
                        }
                    }
                ) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBudgetCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AiInsightCard(insight: AiFinancialInsight) {
    val (bgColor, iconColor, icon) = when (insight.severity) {
        InsightSeverity.ALERT -> Triple(ExpenseRed.copy(alpha = 0.1f), ExpenseRed, Icons.Default.ErrorOutline)
        InsightSeverity.WARNING -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Warning)
        InsightSeverity.POSITIVE -> Triple(IncomeGreen.copy(alpha = 0.1f), IncomeGreen, Icons.Default.Lightbulb)
        InsightSeverity.NEUTRAL -> Triple(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.primary, Icons.Default.Info)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_insight_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = insight.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = iconColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 Action: ${insight.actionSuggestion}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CategoryBudgetRow(
    progress: CategorySpendProgress,
    currencySymbol: String,
    onEditLimit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_row_${progress.category.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = progress.category.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${currencySymbol}${String.format(Locale.US, "%.0f", progress.spent)} of ${currencySymbol}${String.format(Locale.US, "%.0f", progress.limit)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(progress.percentage * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (progress.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEditLimit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (progress.isOverBudget) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ Over by ${currencySymbol}${String.format(Locale.US, "%.0f", progress.spent - progress.limit)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }
        }
    }
}
