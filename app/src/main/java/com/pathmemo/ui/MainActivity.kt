package com.pathmemo.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pathmemo.data.store.SettingsDataStore
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.pathmemo.service.LocationRecordService
import com.pathmemo.ui.navigation.PathMemoNavHost
import com.pathmemo.ui.theme.PathMemoTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsDataStore = SettingsDataStore(this)
        lifecycleScope.launch {
            settingsDataStore.settings.collect { settings ->
                if (settings.autoRecordEnabled) {
                    val serviceIntent = Intent(this@MainActivity, LocationRecordService::class.java).apply {
                        action = LocationRecordService.ACTION_START
                    }
                    ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                }
                return@collect
            }
        }

        setContent {
            PathMemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate {
                        val navController = rememberNavController()
                        PathMemoNavHost(navController = navController)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun PermissionGate(content: @Composable () -> Unit) {
        val locationPermissions = rememberMultiplePermissionsState(
            permissions = mutableListOf<String>().apply {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )

        val backgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else null

        when {
            locationPermissions.allPermissionsGranted && (backgroundPermission?.status?.isGranted != false) -> {
                content()
            }
            else -> {
                com.pathmemo.ui.home.PermissionRequestScreen(
                    onRequestPermissions = {
                        locationPermissions.launchMultiplePermissionRequest()
                    },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    },
                    isBackgroundGranted = backgroundPermission?.status?.isGranted ?: true
                )
            }
        }
    }
}
