package com.pathmemo.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.pathmemo.data.store.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Starts location recording automatically after the device boots,
 * if the user has enabled "auto-start on boot" in settings.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val settingsDataStore: SettingsDataStore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed received")

        // Keep the broadcast alive asynchronously while we read the setting.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                settingsDataStore.settings.collect { settings ->
                    if (settings.autoRecordEnabled || settings.autoStartOnBoot) {
                        if (hasRequiredPermissions(context)) {
                            Log.d(TAG, "Auto-starting location recording")
                            startRecordingService(context)
                        } else {
                            Log.w(TAG, "Auto-start skipped: location permission not granted")
                        }
                    } else {
                        Log.d(TAG, "Auto-start on boot is disabled")
                    }
                    // Only need the first emission.
                    return@collect
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocation && backgroundLocation
    }

    private fun startRecordingService(context: Context) {
        val serviceIntent = Intent(context, LocationRecordService::class.java).apply {
            action = LocationRecordService.ACTION_START
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
