package com.pathmemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pathmemo.ui.detail.TrackDetailScreen
import com.pathmemo.ui.home.HomeScreen
import com.pathmemo.ui.overview.OverviewScreen
import com.pathmemo.ui.preview.DayPreviewScreen
import com.pathmemo.ui.range.RangePreviewScreen
import com.pathmemo.ui.settings.SettingsScreen

@Composable
fun PathMemoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToOverview = { navController.navigate(Screen.Overview.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Overview.route) {
            OverviewScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onDateClick = { dateMillis ->
                    navController.navigate(Screen.DayPreview.createRoute(dateMillis))
                },
                onNavigateToRangePreview = { startMillis, endMillis ->
                    navController.navigate(Screen.RangePreview.createRoute(startMillis, endMillis))
                }
            )
        }
        composable(
            route = Screen.DayPreview.route,
            arguments = listOf(navArgument("dateMillis") { type = NavType.LongType })
        ) { backStackEntry ->
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis") ?: System.currentTimeMillis()
            DayPreviewScreen(
                initialDate = dateMillis,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.RangePreview.route,
            arguments = listOf(
                navArgument("startMillis") { type = NavType.LongType },
                navArgument("endMillis") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val startMillis = backStackEntry.arguments?.getLong("startMillis") ?: System.currentTimeMillis()
            val endMillis = backStackEntry.arguments?.getLong("endMillis") ?: System.currentTimeMillis()
            RangePreviewScreen(
                initialStartMillis = startMillis,
                initialEndMillis = endMillis,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TrackDetail.route,
            arguments = listOf(navArgument("trackId") { type = NavType.LongType })
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getLong("trackId") ?: return@composable
            TrackDetailScreen(
                trackId = trackId,
                onBack = { navController.popBackStack() },
                onNavigateToTrack = { newTrackId ->
                    navController.navigate(Screen.TrackDetail.createRoute(newTrackId))
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
