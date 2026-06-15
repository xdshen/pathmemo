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
import com.pathmemo.data.model.Track
import com.pathmemo.ui.map.TimeColorLegend
import com.pathmemo.ui.map.TrackMapView
import com.pathmemo.ui.theme.TrackBlue
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
    onTrackClick: (Long) -> Unit,
    viewModel: RangePreviewViewModel = koinViewModel()
) {
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val rangeTracks by viewModel.rangeTracks.collectAsStateWithLifecycle()
    val rangePoints by viewModel.rangePoints.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = com.pathmemo.data.model.AppSettings())

    var trackToRename by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
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
                    tracks = rangePoints,
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
                    rangeTracks = rangeTracks,
                    rangePoints = rangePoints,
                    onTrackClick = onTrackClick,
                    onRename = { trackToRename = it },
                    onDelete = { trackToDelete = it }
                )
            }
        }
    }

    trackToRename?.let { track ->
        RenameDialog(
            currentName = track.name,
            onDismiss = { trackToRename = null },
            onConfirm = { newName ->
                viewModel.renameTrack(track, newName)
                trackToRename = null
            }
        )
    }

    trackToDelete?.let { track ->
        DeleteConfirmDialog(
            trackName = track.name,
            onDismiss = { trackToDelete = null },
            onConfirm = {
                viewModel.deleteTrack(track)
                trackToDelete = null
            }
        )
    }
}

@Composable
private fun RangeTrackPanel(
    modifier: Modifier = Modifier,
    startDate: LocalDate,
    endDate: LocalDate,
    rangeTracks: List<Track>,
    rangePoints: Map<Long, List<LocationPoint>>,
    onTrackClick: (Long) -> Unit,
    onRename: (Track) -> Unit,
    onDelete: (Track) -> Unit
) {
    val grouped = remember(rangeTracks) {
        rangeTracks.groupBy { it.toLocalDate() }.toSortedMap()
    }
    val totalDistance = rangeTracks.sumOf { it.distanceMeters }
    val totalDuration = rangeTracks.sumOf { it.durationMillis }
    val totalPoints = rangePoints.values.sumOf { it.size }

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
                    StatItem("轨迹", "${rangeTracks.size} 条")
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
                grouped.forEach { (date, tracks) ->
                    item(key = date) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("MM月dd日 EEEE", Locale.CHINA)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(tracks, key = { it.id }) { track ->
                        TimelineTrackItem(
                            track = track,
                            onClick = { onTrackClick(track.id) },
                            onRename = { onRename(track) },
                            onDelete = { onDelete(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTrackItem(
    track: Track,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = TrackBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${formatTime(track.startTime)} - ${formatTime(track.endTime ?: track.startTime)} · ${formatDistance(track.distanceMeters)} · ${formatDuration(track.durationMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = "重命名", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(20.dp))
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

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名轨迹") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    trackName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除轨迹") },
        text = { Text("确定要删除 \"$trackName\" 吗？此操作不可恢复。") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
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
