package ru.ecobirch.fa.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ecobirch.fa.app.ui.screens.JournalScreen
import ru.ecobirch.fa.app.ui.screens.MapScreen
import ru.ecobirch.fa.app.ui.screens.MethodologyScreen
import ru.ecobirch.fa.app.ui.screens.ScanScreen
import ru.ecobirch.fa.app.ui.state.MainViewModel
import ru.ecobirch.fa.app.ui.theme.EcoColors

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val TABS = listOf(
    NavTab("scan", "Сканер", Icons.Default.CameraAlt),
    NavTab("map", "Карта", Icons.Default.Map),
    NavTab("journal", "Журнал", Icons.AutoMirrored.Filled.ListAlt),
    NavTab("methodology", "Методика", Icons.AutoMirrored.Filled.MenuBook)
)

@Composable
fun EcoBirchRoot(viewModel: MainViewModel) {
    val activeRoute by viewModel.activeTabRoute.collectAsStateWithLifecycle()
    val territories by viewModel.territories.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val focusedTerritoryId by viewModel.focusedJournalTerritoryId.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val lastCrashLog by viewModel.lastCrashLog.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = EcoColors.Slate50,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                TABS.forEach { tab ->
                    val selected = activeRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EcoColors.Emerald700,
                            selectedTextColor = EcoColors.Emerald700,
                            indicatorColor = EcoColors.Emerald100,
                            unselectedIconColor = EcoColors.Slate500,
                            unselectedTextColor = EcoColors.Slate500
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeRoute) {
                "scan" -> ScanScreen(
                    state = scanState,
                    territories = territories,
                    viewModel = viewModel,
                    onOpenJournal = { viewModel.navigateToTab("journal") },
                    onOpenMap = { viewModel.navigateToTab("map") }
                )
                "map" -> MapScreen(
                    state = mapState,
                    territories = territories,
                    viewModel = viewModel
                )
                "journal" -> JournalScreen(
                    territories = territories,
                    focusedTerritoryId = focusedTerritoryId,
                    viewModel = viewModel,
                    onNavigateToMap = { viewModel.navigateToTab("map") },
                    onNavigateToScan = { viewModel.navigateToTab("scan") }
                )
                "methodology" -> MethodologyScreen(
                    settings = settings,
                    lastCrashLog = lastCrashLog,
                    viewModel = viewModel
                )
            }
        }
    }

    // Friendly recovery dialog if previous session crashed — guarantees user data was preserved
    if (!lastCrashLog.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCrashLog,
            title = { Text("Восстановление после сбоя") },
            text = {
                Text(
                    "Предыдущая сессия завершилась с ошибкой. Ваш полевой журнал и фотографии листьев сохранены в локальной базе данных без потерь. Подробный стек-трейс доступен в Настройках."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCrashLog) {
                    Text("Понятно")
                }
            }
        )
    }
}
