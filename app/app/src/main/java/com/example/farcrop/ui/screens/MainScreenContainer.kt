package com.example.farcrop.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.farcrop.ui.viewmodel.CropCycleViewModel

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : TabScreen("home_tab", "Home", Icons.Filled.Home)
    object CropCycles : TabScreen("crop_cycles_tab", "Crop Cycles", Icons.Filled.Landscape)
    object Settings : TabScreen("settings_tab", "Settings", Icons.Filled.Settings)
}

@Composable
fun MainScreenContainer(
    viewModel: CropCycleViewModel,
    onNavigateToCreateCycle: () -> Unit,
    onNavigateToCycleDetails: (String) -> Unit
) {
    val items = listOf(TabScreen.Home, TabScreen.CropCycles, TabScreen.Settings)
    var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            is TabScreen.Home -> HomeScreen(
                viewModel = viewModel,
                onNavigateToCreateCycle = onNavigateToCreateCycle,
                modifier = Modifier.padding(innerPadding)
            )
            is TabScreen.CropCycles -> CropCyclesScreen(
                viewModel = viewModel,
                onNavigateToCreateCycle = onNavigateToCreateCycle,
                onNavigateToCycleDetails = onNavigateToCycleDetails,
                modifier = Modifier.padding(innerPadding)
            )
            is TabScreen.Settings -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
