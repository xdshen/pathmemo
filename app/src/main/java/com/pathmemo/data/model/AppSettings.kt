package com.pathmemo.data.model

import androidx.annotation.StringRes
import com.pathmemo.R

enum class LocationInterval(val millis: Long, @StringRes val labelRes: Int) {
    ONE_SECOND(1000L, R.string.interval_1s),
    THREE_SECONDS(3000L, R.string.interval_3s),
    FIVE_SECONDS(5000L, R.string.interval_5s),
    TEN_SECONDS(10000L, R.string.interval_10s);

    companion object {
        fun fromMillis(millis: Long): LocationInterval =
            entries.find { it.millis == millis } ?: THREE_SECONDS
    }
}

enum class MinDistance(val meters: Float, @StringRes val labelRes: Int) {
    FIVE(5f, R.string.distance_5m),
    EIGHT(8f, R.string.distance_8m),
    TEN(10f, R.string.distance_10m),
    TWENTY(20f, R.string.distance_20m);

    companion object {
        fun fromMeters(meters: Float): MinDistance =
            entries.find { it.meters == meters } ?: EIGHT
    }
}

enum class MinAccuracy(val meters: Float, @StringRes val labelRes: Int) {
    TWENTY(20f, R.string.accuracy_20m),
    FIFTY(50f, R.string.accuracy_50m),
    ONE_HUNDRED(100f, R.string.accuracy_100m);

    companion object {
        fun fromMeters(meters: Float): MinAccuracy =
            entries.find { it.meters == meters } ?: FIFTY
    }
}

enum class MapType(val value: Int, @StringRes val labelRes: Int) {
    NORMAL(1, R.string.map_type_normal),
    SATELLITE(2, R.string.map_type_satellite),
    NIGHT(3, R.string.map_type_night);

    companion object {
        fun fromValue(value: Int): MapType =
            entries.find { it.value == value } ?: NORMAL
    }
}

data class AppSettings(
    val locationInterval: LocationInterval = LocationInterval.THREE_SECONDS,
    val minDistance: MinDistance = MinDistance.EIGHT,
    val minAccuracy: MinAccuracy = MinAccuracy.FIFTY,
    val mapType: MapType = MapType.NORMAL,
    val autoStartOnBoot: Boolean = false
)
