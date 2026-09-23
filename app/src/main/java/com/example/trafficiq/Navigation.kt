package com.example.trafficiq

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.trafficiq.data.TrafficRepository
import com.example.trafficiq.ui.screens.SessionHistoryScreen
import com.example.trafficiq.ui.screens.TrafficMonitorScreen

@Composable
fun MainNavigation(repository: TrafficRepository) {
    val backStack = rememberNavBackStack(MonitorNavKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<MonitorNavKey> {
                TrafficMonitorScreen(
                    repository = repository,
                    onOpenHistory = { backStack.add(HistoryNavKey) }
                )
            }
            entry<HistoryNavKey> {
                SessionHistoryScreen(
                    repository = repository,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
