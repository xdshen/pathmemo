package com.pathmemo.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathmemo.data.model.LocationInterval
import com.pathmemo.data.model.MapType
import com.pathmemo.data.model.MinAccuracy
import com.pathmemo.data.model.MinDistance
import com.pathmemo.data.repository.TrackRepository
import com.pathmemo.data.store.SettingsDataStore
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: TrackRepository,
    private val settingsDataStore: SettingsDataStore,
    application: Application
) : AndroidViewModel(application) {

    val settings = settingsDataStore.settings

    fun setLocationInterval(interval: LocationInterval) {
        viewModelScope.launch { settingsDataStore.setLocationInterval(interval) }
    }

    fun setMinDistance(distance: MinDistance) {
        viewModelScope.launch { settingsDataStore.setMinDistance(distance) }
    }

    fun setMinAccuracy(accuracy: MinAccuracy) {
        viewModelScope.launch { settingsDataStore.setMinAccuracy(accuracy) }
    }

    fun setMapType(mapType: MapType) {
        viewModelScope.launch { settingsDataStore.setMapType(mapType) }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoStartOnBoot(enabled) }
    }

    fun setAutoRecordEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoRecordEnabled(enabled) }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllTracks()
            onComplete()
        }
    }

    fun openBatteryOptimizationSettings() {
        val context = getApplication<Application>()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(getApplication<Application>().packageName)
        } else {
            true
        }
    }
}
