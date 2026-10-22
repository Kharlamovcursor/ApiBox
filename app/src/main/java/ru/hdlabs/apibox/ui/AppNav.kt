package ru.hdlabs.apibox.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.hdlabs.apibox.ui.background.SpaceBackground
import ru.hdlabs.apibox.ui.dashboard.DashboardScreen
import ru.hdlabs.apibox.ui.settings.AccountEditorScreen
import ru.hdlabs.apibox.ui.settings.AccountsScreen
import ru.hdlabs.apibox.ui.settings.CustomEditorScreen
import ru.hdlabs.apibox.ui.settings.CustomListScreen
import ru.hdlabs.apibox.ui.settings.MoexScreen
import ru.hdlabs.apibox.ui.settings.SettingsHomeScreen

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    Box(Modifier.fillMaxSize()) {
        SpaceBackground()
        NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.fillMaxSize()) {
            composable("dashboard") {
                DashboardScreen(onSettings = { nav.navigate("settings") })
            }
            composable("settings") {
                SettingsHomeScreen(
                    onBack = { nav.popBackStack() },
                    onMoex = { nav.navigate("moex") },
                    onAccounts = { nav.navigate("accounts") },
                    onCustom = { nav.navigate("custom") },
                )
            }
            composable("moex") { MoexScreen(onBack = { nav.popBackStack() }) }
            composable("accounts") {
                AccountsScreen(
                    onBack = { nav.popBackStack() },
                    onCreate = { platform -> nav.navigate("account/new/$platform") },
                    onOpen = { id -> nav.navigate("account/edit/$id") },
                )
            }
            composable(
                route = "account/new/{platform}",
                arguments = listOf(navArgument("platform") { type = NavType.StringType }),
            ) { entry ->
                AccountEditorScreen(
                    existingId = null,
                    newPlatform = entry.arguments?.getString("platform").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = "account/edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                AccountEditorScreen(
                    existingId = entry.arguments?.getString("id"),
                    newPlatform = "",
                    onBack = { nav.popBackStack() },
                )
            }
            composable("custom") {
                CustomListScreen(
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate("custom/new") },
                    onOpen = { id -> nav.navigate("custom/edit/$id") },
                )
            }
            composable("custom/new") {
                CustomEditorScreen(existingId = null, onBack = { nav.popBackStack() })
            }
            composable(
                route = "custom/edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                CustomEditorScreen(
                    existingId = entry.arguments?.getString("id"),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
