package com.rakor.sanzijing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rakor.sanzijing.SanzijingApp
import com.rakor.sanzijing.ui.home.HomeScreen
import com.rakor.sanzijing.ui.reader.ReaderScreen
import com.rakor.sanzijing.ui.report.ReportScreen
import com.rakor.sanzijing.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(app: SanzijingApp) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                app = app,
                onContinue = { unitId -> nav.navigate("reader/$unitId") { launchSingleTop = true } },
                onOpenReport = { nav.navigate("report") { launchSingleTop = true } },
                onOpenSettings = { nav.navigate("settings") { launchSingleTop = true } },
            )
        }
        composable("reader/{unitId}") { entry ->
            val unitId = entry.arguments?.getString("unitId") ?: "u001"
            ReaderScreen(app = app, startUnitId = unitId, onBack = { nav.popBackStack() })
        }
        composable("report") { ReportScreen(onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(app = app, onBack = { nav.popBackStack() }) }
    }
}
