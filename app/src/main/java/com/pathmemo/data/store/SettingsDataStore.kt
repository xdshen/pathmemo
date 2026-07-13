package com.pathmemo.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pathmemo.data.model.AppSettings
import com.pathmemo.data.model.LocationInterval
import com.pathmemo.data.model.MapType
import com.pathmemo.data.model.MinAccuracy
import com.pathmemo.data.model.MinDistance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pathmemo_settings")

class SettingsDataStore(private val context: Context) {

    private val locationIntervalKey = longPreferencesKey("location_interval_ms")
    private val minDistanceKey = floatPreferencesKey("min_distance_m")
    private val minAccuracyKey = floatPreferencesKey("min_accuracy_m")
    private val mapTypeKey = intPreferencesKey("map_type")
    private val autoStartOnBootKey = booleanPreferencesKey("auto_start_on_boot")
    private val autoRecordEnabledKey = booleanPreferencesKey("auto_record_enabled")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            locationInterval = LocationInterval.fromMillis(
                prefs[locationIntervalKey] ?: LocationInterval.THREE_SECONDS.millis
            ),
            minDistance = MinDistance.fromMeters(
                prefs[minDistanceKey] ?: MinDistance.EIGHT.meters
            ),
            minAccuracy = MinAccuracy.fromMeters(
                prefs[minAccuracyKey] ?: MinAccuracy.FIFTY.meters
            ),
            mapType = MapType.fromValue(
                prefs[mapTypeKey] ?: MapType.NORMAL.value
            ),
            autoStartOnBoot = prefs[autoStartOnBootKey] ?: false,
            autoRecordEnabled = prefs[autoRecordEnabledKey] ?: false
        )
    }

    suspend fun setLocationInterval(interval: LocationInterval) {
        context.dataStore.edit { prefs ->
            prefs[locationIntervalKey] = interval.millis
        }
    }

    suspend fun setMinDistance(distance: MinDistance) {
        context.dataStore.edit { prefs ->
            prefs[minDistanceKey] = distance.meters
        }
    }

    suspend fun setMinAccuracy(accuracy: MinAccuracy) {
        context.dataStore.edit { prefs ->
            prefs[minAccuracyKey] = accuracy.meters
        }
    }

    suspend fun setMapType(mapType: MapType) {
        context.dataStore.edit { prefs ->
            prefs[mapTypeKey] = mapType.value
        }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[autoStartOnBootKey] = enabled
        }
    }

    suspend fun setAutoRecordEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[autoRecordEnabledKey] = enabled
        }
    }
}
