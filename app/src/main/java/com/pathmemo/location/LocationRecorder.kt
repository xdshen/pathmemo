package com.pathmemo.location

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationRecorder(context: Context) {

    private val appContext = context.applicationContext
    private var locationClient: AMapLocationClient? = null


    /**
     * Start location updates as a [Flow] of [AMapLocation].
     * Locations are filtered by accuracy and distance threshold.
     */
    fun start(
        intervalMillis: Long = 3000L,
        minDistanceMeters: Float = 10f,
        minAccuracyMeters: Float = 50f
    ): Flow<AMapLocation> = callbackFlow {
        destroyClient()

        AMapLocationClient.updatePrivacyAgree(appContext, true)
        val client = AMapLocationClient(appContext).also { locationClient = it }
        val option = AMapLocationClientOption().apply {
            locationPurpose = AMapLocationClientOption.AMapLocationPurpose.Sport
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isMockEnable = false
            interval = intervalMillis
            isNeedAddress = false
            isLocationCacheEnable = false
        }
        client.setLocationOption(option)

        var lastLocation: AMapLocation? = null

        val listener = AMapLocationListener { location ->
            if (location == null) return@AMapLocationListener
            if (location.errorCode != AMapLocation.LOCATION_SUCCESS) return@AMapLocationListener
            if (location.accuracy <= 0 || location.accuracy > minAccuracyMeters) return@AMapLocationListener

            val last = lastLocation
            if (last != null) {
                val distance = computeDistance(
                    last.latitude, last.longitude,
                    location.latitude, location.longitude
                )
                if (distance < minDistanceMeters) {
                    return@AMapLocationListener
                }
            }

            lastLocation = location
            trySend(location)
        }

        client.setLocationListener(listener)
        client.startLocation()

        awaitClose {
            client.stopLocation()
            client.onDestroy()
            locationClient = null
        }
    }

    fun stop() {
        destroyClient()
    }

    private fun destroyClient() {
        locationClient?.apply {
            stopLocation()
            onDestroy()
        }
        locationClient = null
    }

    companion object {
        /**
         * Haversine distance in meters.
         */
        fun computeDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        fun computeTotalDistance(points: List<com.pathmemo.data.model.LocationPoint>): Double {
            var total = 0.0
            for (i in 1 until points.size) {
                total += computeDistance(
                    points[i - 1].latitude, points[i - 1].longitude,
                    points[i].latitude, points[i].longitude
                )
            }
            return total
        }
    }
}
