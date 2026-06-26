package com.example.ringtoneid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ringtoneid.ui.contacts.ContactListScreen
import com.example.ringtoneid.ui.detail.RingtoneDetailScreen
import com.example.ringtoneid.ui.favorites.FavoritesScreen
import com.example.ringtoneid.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object ContactList : Screen("contacts")
    object RingtoneDetail : Screen("detail/{contactId}") {
        fun createRoute(contactId: Long) = "detail/$contactId"
    }
    object Settings : Screen("settings")
    object Favorites : Screen("favorites")
}

@Composable
fun RingtoneIdNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.ContactList.route) {
        composable(Screen.ContactList.route) {
            ContactListScreen(
                onContactClick = { contactId ->
                    navController.navigate(Screen.RingtoneDetail.createRoute(contactId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onFavoritesClick = {
                    navController.navigate(Screen.Favorites.route)
                }
            )
        }
        composable(
            route = Screen.RingtoneDetail.route,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable
            RingtoneDetailScreen(
                contactId = contactId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(onBack = { navController.popBackStack() })
        }
    }
}
