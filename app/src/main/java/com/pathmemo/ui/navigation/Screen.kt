package com.pathmemo.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Overview : Screen("overview")
    data object DayPreview : Screen("day_preview/{dateMillis}") {
        fun createRoute(dateMillis: Long) = "day_preview/$dateMillis"
    }
    data object RangePreview : Screen("range_preview/{startMillis}/{endMillis}") {
        fun createRoute(startMillis: Long, endMillis: Long) = "range_preview/$startMillis/$endMillis"
    }
    data object Settings : Screen("settings")
    data object TrackDetail : Screen("track_detail/{trackId}") {
        fun createRoute(trackId: Long) = "track_detail/$trackId"
    }
}
