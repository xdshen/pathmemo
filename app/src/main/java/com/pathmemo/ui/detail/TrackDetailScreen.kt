package com.pathmemo.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track
import com.pathmemo.location.LocationRecorder
import com.pathmemo.ui.map.AMapView
import com.pathmemo.ui.theme.TrackBlue
import com.pathmemo.viewmodel.TrackDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    trackId: Long,
    onBack: () -> Unit,
    onNavigateToTrack: ((Long) -> Unit)? = null,
    viewModel: TrackDetailViewModel = koinViewModel(parameters = { parametersOf(trackId) })
) {
    val track by viewModel.track.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = com.pathmemo.data.model.AppSettings())
    val playbackIndex by viewModel.playbackIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val adjacentTracks by viewModel.adjacentTracks.collectAsStateWithLifecycle()
    val selectedPoint by viewModel.selectedPoint.collectAsStateWithLifecycle()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var playbackMarker by remember { mutableStateOf<Marker?>(null) }
    var currentZoom by remember { mutableFloatStateOf(15f) }

    val trackPolyline = remember { mutableStateOf<Polyline?>(null) }
    val densityMarkers = remember { mutableStateListOf<Marker>() }

    // Draw or update track when points or map changes.
    LaunchedEffect(mapView, points) {
        val mv = mapView ?: return@LaunchedEffect
        if (points.size >= 2) {
            val bounds = buildBounds(points)
            mv.map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
            renderTrack(mv, points, currentZoom, trackPolyline, densityMarkers)
        }
    }

    // Re-render when zoom changes significantly.
    LaunchedEffect(currentZoom, points) {
        val mv = mapView ?: return@LaunchedEffect
        if (points.size >= 2) {
            renderTrack(mv, points, currentZoom, trackPolyline, densityMarkers)
        }
    }

    // Update playback marker position.
    LaunchedEffect(mapView, points, playbackIndex) {
        val mv = mapView ?: return@LaunchedEffect
        if (points.isEmpty()) return@LaunchedEffect
        val point = points[playbackIndex.coerceIn(points.indices)]
        val position = LatLng(point.latitude, point.longitude)
        if (playbackMarker == null) {
            playbackMarker = mv.map.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .draggable(false)
            )
        } else {
            playbackMarker?.position = position
        }
        mv.map.animateCamera(CameraUpdateFactory.newLatLng(position))
    }

    // Long press listener to find nearest point.
    DisposableEffect(mapView, points) {
        val longClickListener = object : com.amap.api.maps.AMap.OnMapLongClickListener {
            override fun onMapLongClick(latLng: LatLng?) {
                latLng ?: return
                val nearest = points.minByOrNull {
                    LocationRecorder.computeDistance(
                        it.latitude, it.longitude,
                        latLng.latitude, latLng.longitude
                    )
                }
                nearest?.let { viewModel.selectPoint(it) }
            }
        }
        val cameraListener = object : com.amap.api.maps.AMap.OnCameraChangeListener {
            override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition?) {
                position?.let { currentZoom = it.zoom }
            }
            override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition?) {
                position?.let { currentZoom = it.zoom }
            }
        }
        mapView?.map?.setOnMapLongClickListener(longClickListener)
        mapView?.map?.setOnCameraChangeListener(cameraListener)
        onDispose {
            mapView?.map?.setOnMapLongClickListener(null)
            mapView?.map?.setOnCameraChangeListener(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(track?.name ?: "轨迹详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val (prev, next) = adjacentTracks
                    IconButton(
                        onClick = { prev?.let { onNavigateToTrack?.invoke(it.id) } },
                        enabled = prev != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前一天")
                    }
                    IconButton(
                        onClick = { next?.let { onNavigateToTrack?.invoke(it.id) } },
                        enabled = next != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "后一天")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AMapView(
                    modifier = Modifier.fillMaxSize(),
                    mapType = settings.mapType,
                    onMapReady = { mv ->
                        mapView = mv
                        mv.map.clear()
                        if (points.size >= 2) {
                            renderTrack(mv, points, currentZoom, trackPolyline, densityMarkers)
                        }
                    }
                )

                // Zoom hint
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (currentZoom < 14) "当前：点密度视图" else "当前：轨迹线视图",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomPadding)
            ) {
                Text(
                    text = "时间: ${formatTime(points.getOrNull(playbackIndex)?.timestamp ?: track?.startTime ?: 0L)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = playbackIndex.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toInt()) },
                    valueRange = 0f..(points.size - 1).coerceAtLeast(0).toFloat(),
                    steps = (points.size - 2).coerceAtLeast(0)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { viewModel.resetPlayback() },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = "重置")
                    }
                    FilledIconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SpeedSelector(
                    currentSpeed = speed,
                    onSpeedSelected = { viewModel.setSpeed(it) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    selectedPoint?.let { point ->
        PointDetailDialog(
            point = point,
            track = track,
            onDismiss = { viewModel.selectPoint(null) }
        )
    }
}

@Composable
private fun PointDetailDialog(
    point: LocationPoint,
    track: Track?,
    onDismiss: () -> Unit
) {
    val stayDuration = track?.let { t ->
        val idx = t.pointCount
        if (idx > 0) t.durationMillis / idx else 0L
    } ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("位置详情") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("时间: ${formatTime(point.timestamp)}")
                Text("坐标: ${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}")
                Text("海拔: ${"%.1f".format(point.altitude)} m")
                Text("精度: ${"%.1f".format(point.accuracy)} m")
                Text("速度: ${"%.1f".format(point.speed)} m/s")
                Text("参考停留: ${formatDuration(stayDuration)}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun SpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val speeds = listOf(0.5f, 1f, 2f, 4f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("倍速", style = MaterialTheme.typography.bodyMedium)
        speeds.forEach { s ->
            Button(
                onClick = { onSpeedSelected(s) },
                enabled = currentSpeed != s,
                shape = CircleShape,
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("${s}x")
            }
        }
    }
}

private fun renderTrack(
    mapView: MapView,
    points: List<LocationPoint>,
    zoom: Float,
    polylineRef: androidx.compose.runtime.MutableState<Polyline?>,
    markersRef: MutableList<Marker>
) {
    // Clear previous dynamic layers but keep start/end markers? Simpler to clear all and redraw.
    mapView.map.clear()

    val latLngs = points.map { LatLng(it.latitude, it.longitude) }

    if (zoom < 14) {
        // Zoomed out: show density points (sampled)
        val sampleStep = when {
            zoom < 10 -> 20
            zoom < 12 -> 10
            else -> 5
        }
        points.filterIndexed { index, _ -> index % sampleStep == 0 }.forEach { p ->
            mapView.map.addMarker(
                MarkerOptions()
                    .position(LatLng(p.latitude, p.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .draggable(false)
            )
        }
    } else {
        // Zoomed in: show polyline
        polylineRef.value = mapView.map.addPolyline(
            PolylineOptions()
                .addAll(latLngs)
                .width(12f)
                .color(TrackBlue.value.toInt())
        )
    }

    mapView.map.addMarker(
        MarkerOptions().position(latLngs.first()).title("起点")
    )
    mapView.map.addMarker(
        MarkerOptions().position(latLngs.last()).title("终点")
    )
}

private fun buildBounds(points: List<LocationPoint>): LatLngBounds {
    val builder = LatLngBounds.builder()
    points.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
    return builder.build()
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}
