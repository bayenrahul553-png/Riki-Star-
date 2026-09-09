package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.BudgetAnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsParserScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

sealed class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : NavItem("Home", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "nav_home")
    object SmsParser : NavItem("SMS Parse", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_sms_parser")
    object Budget : NavItem("Budget AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_budget")
    object Rewards : NavItem("Rewards", Icons.Filled.MilitaryTech, Icons.Outlined.MilitaryTech, "nav_rewards")
    object Settings : NavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SmartBudgetApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SmartBudgetApp(viewModel: FinanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val navItems = listOf(
        NavItem.Home,
        NavItem.SmsParser,
        NavItem.Budget,
        NavItem.Rewards,
        NavItem.Settings
    )

    // Handle incoming snackbar messages
    LaunchedEffect(uiState.activeSnackbarMessage) {
        val msg = uiState.activeSnackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        label = { Text(item.title) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            label = "ScreenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { targetIndex ->
            when (targetIndex) {
                0 -> HomeScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToSmsParser = { selectedTab = 1 },
                    onNavigateToRewards = { selectedTab = 3 }
                )
                1 -> SmsParserScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                2 -> BudgetAnalyticsScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                3 -> RewardsScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                4 -> SettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
