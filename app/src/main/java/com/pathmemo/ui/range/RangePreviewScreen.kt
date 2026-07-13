package com.pathmemo.ui.range

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.ui.map.TimeColorLegend
import com.pathmemo.ui.map.TrackMapView
import com.pathmemo.viewmodel.RangePreviewViewModel
import com.pathmemo.viewmodel.toLocalDate
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangePreviewScreen(
    initialStartMillis: Long? = null,
    initialEndMillis: Long? = null,
    onBack: () -> Unit,
    viewModel: RangePreviewViewModel = koinViewModel()
) {
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val rangePoints by viewModel.rangePoints.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = com.pathmemo.data.model.AppSettings())

    var selectedPoint by remember { mutableStateOf<LocationPoint?>(null) }

    LaunchedEffect(initialStartMillis, initialEndMillis) {
        val start = initialStartMillis?.let { millisToLocalDate(it) } ?: LocalDate.now().minusDays(6)
        val end = initialEndMillis?.let { millisToLocalDate(it) } ?: LocalDate.now()
        viewModel.setRange(start, end)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("区间轨迹预览") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                TrackMapView(
                    points = rangePoints,
                    selectedPoint = selectedPoint,
                    onPointClick = { selectedPoint = it },
                    modifier = Modifier.fillMaxSize(),
                    mapType = settings.mapType
                )
            }

            TimeColorLegend()

            Box(modifier = Modifier.weight(0.55f)) {
                RangeTrackPanel(
                    modifier = Modifier.fillMaxSize(),
                    startDate = startDate,
                    endDate = endDate,
                    rangePoints = rangePoints
                )
            }
        }
    }
}

@Composable
private fun RangeTrackPanel(
    modifier: Modifier = Modifier,
    startDate: LocalDate,
    endDate: LocalDate,
    rangePoints: List<LocationPoint>
) {
    val grouped = remember(rangePoints) {
        rangePoints.groupBy { it.toLocalDate() }.toSortedMap()
    }
    val totalDistance = computeTotalDistance(rangePoints)
    val totalDuration = if (rangePoints.isEmpty()) 0L else rangePoints.last().timestamp - rangePoints.first().timestamp
    val totalPoints = rangePoints.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} 至 ${endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("记录段", "${rangePoints.map { it.trackId }.toSet().size} 段")
                    StatItem("点位", "$totalPoints")
                    StatItem("距离", formatDistance(totalDistance))
                    StatItem("时长", formatDuration(totalDuration))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("该区间内没有轨迹记录", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                grouped.forEach { (date, points) ->
                    item(key = date) {
                        val dayDistance = computeTotalDistance(points)
                        val dayDuration = if (points.isEmpty()) 0L else points.last().timestamp - points.first().timestamp
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("MM月dd日 EEEE", Locale.CHINA)),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${points.size} 个点 · ${formatDistance(dayDistance)} · ${formatDuration(dayDuration)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun computeTotalDistance(points: List<LocationPoint>): Double {
    if (points.size < 2) return 0.0
    var total = 0.0
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val r = 6371000.0
        val dLat = Math.toRadians(curr.latitude - prev.latitude)
        val dLon = Math.toRadians(curr.longitude - prev.longitude)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(prev.latitude)) * kotlin.math.cos(Math.toRadians(curr.latitude)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        total += r * c
    }
    return total
}

private fun millisToLocalDate(millis: Long): LocalDate {
    return java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        String.format("%d小时%02d分", hours, minutes)
    } else {
        String.format("%02d分%02d秒", minutes, seconds % 60)
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) String.format("%.2f km", meters / 1000)
    else String.format("%.0f m", meters)
}
