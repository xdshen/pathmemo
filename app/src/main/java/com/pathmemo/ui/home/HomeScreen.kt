package com.pathmemo.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.PolylineOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pathmemo.service.LocationRecordService
import com.pathmemo.ui.map.AMapView
import com.pathmemo.ui.theme.TrackBlue
import com.pathmemo.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToOverview: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.recordingState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = com.pathmemo.data.model.AppSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PathMemo") },
                actions = {
                    IconButton(onClick = onNavigateToOverview) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "轨迹回顾")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AMapView(
                modifier = Modifier.fillMaxSize(),
                mapType = settings.mapType,
                onMapReady = { mapView ->
                    state.lastLocation?.let { loc ->
                        mapView.map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17f)
                        )
                    }
                }
            )

            // Stats card
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when {
                            state.isPaused -> "已暂停"
                            state.isRecording -> "正在记录轨迹"
                            else -> "准备开始记录"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "距离", value = formatDistance(state.distanceMeters))
                        StatItem(label = "点数", value = state.pointCount.toString())
                        StatItem(label = "时长", value = formatDuration(state.elapsedMillis))
                    }
                    state.lastCellInfo?.let { cell ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatCellInfo(cell),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Control buttons
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp + bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Auto-record switch
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("自动记录", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.autoRecordEnabled,
                            onCheckedChange = { viewModel.setAutoRecordEnabled(it) }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!state.isRecording) {
                        FloatingActionButton(
                            onClick = { viewModel.startRecording() },
                            shape = CircleShape,
                            containerColor = TrackBlue
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "开始", modifier = Modifier.size(32.dp))
                        }
                    } else {
                        if (state.isPaused) {
                            FloatingActionButton(
                                onClick = { viewModel.resumeRecording() },
                                shape = CircleShape,
                                containerColor = TrackBlue
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "继续", modifier = Modifier.size(32.dp))
                            }
                        } else {
                            FloatingActionButton(
                                onClick = { viewModel.pauseRecording() },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.secondary
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = "暂停", modifier = Modifier.size(32.dp))
                            }
                        }

                        FloatingActionButton(
                            onClick = { viewModel.stopRecording() },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.error
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "结束", modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) String.format("%.2f km", meters / 1000)
    else String.format("%.0f m", meters)
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

private fun formatCellInfo(cell: com.pathmemo.location.CellInfoSnapshot): String {
    val parts = mutableListOf<String>()
    parts += cell.networkType
    cell.operatorName?.let { parts += it }
    cell.band?.let { parts += it }
    cell.pci?.let { parts += "PCI $it" }
    cell.ci?.let { parts += "CI $it" }
    cell.rsrp?.let { parts += "RSRP $it dBm" }
    cell.rsrq?.let { parts += "RSRQ $it dB" }
    cell.sinr?.let { parts += "SINR $it dB" }
    return parts.joinToString(" · ")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    isBackgroundGranted: Boolean
) {
    val context = LocalContext.current
    val backgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要位置权限",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "PathMemo 需要获取您的位置信息以记录运动轨迹。位置数据仅保存在本地，不会上传。",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermissions) {
            Text("授予位置权限")
        }

        if (!isBackgroundGranted && backgroundPermission != null && !backgroundPermission.status.isGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }) {
                Text("授予后台定位权限")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text("打开应用设置")
        }
    }
}

