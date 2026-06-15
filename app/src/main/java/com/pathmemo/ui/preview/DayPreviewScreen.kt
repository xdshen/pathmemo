package com.pathmemo.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
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
import com.pathmemo.ui.components.MonthCalendar
import com.pathmemo.ui.map.TimeColorLegend
import com.pathmemo.ui.map.TrackMapView
import com.pathmemo.ui.theme.TrackBlue
import com.pathmemo.viewmodel.DayPreviewViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPreviewScreen(
    initialDate: Long? = null,
    onBack: () -> Unit,
    onTrackClick: (Long) -> Unit,
    viewModel: DayPreviewViewModel = koinViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayTracks by viewModel.dayTracks.collectAsStateWithLifecycle()
    val dayPoints by viewModel.dayPoints.collectAsStateWithLifecycle()
    val dailyDurations by viewModel.allDailyDurations.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = com.pathmemo.data.model.AppSettings())

    var trackToRename by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var selectedPoint by remember { mutableStateOf<LocationPoint?>(null) }

    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(initialDate) {
        initialDate?.let {
            val instant = java.time.Instant.ofEpochMilli(it)
            val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            viewModel.selectDate(date)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 280.dp,
        topBar = {
            TopAppBar(
                title = { Text("轨迹预览") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        sheetContent = {
            DayPreviewSheetContent(
                selectedDate = selectedDate,
                dayTracks = dayTracks,
                dayPoints = dayPoints,
                dailyDurations = dailyDurations,
                onDateSelected = { viewModel.selectDate(it) },
                onTrackClick = onTrackClick,
                onRename = { trackToRename = it },
                onDelete = { trackToDelete = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TrackMapView(
                tracks = dayPoints,
                selectedPoint = selectedPoint,
                onPointClick = { selectedPoint = it },
                modifier = Modifier.fillMaxSize(),
                mapType = settings.mapType
            )
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
private fun DayPreviewSheetContent(
    selectedDate: LocalDate,
    dayTracks: List<Track>,
    dayPoints: Map<Long, List<LocationPoint>>,
    dailyDurations: Map<LocalDate, Long>,
    onDateSelected: (LocalDate) -> Unit,
    onTrackClick: (Long) -> Unit,
    onRename: (Track) -> Unit,
    onDelete: (Track) -> Unit
) {
    val totalDistance = dayTracks.sumOf { it.distanceMeters }
    val totalDuration = dayTracks.sumOf { it.durationMillis }
    val totalPoints = dayPoints.values.sumOf { it.size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape
                    )
            )
        }

        TimeColorLegend()

        MonthCalendar(
            selectedDate = selectedDate,
            dailyDurations = dailyDurations,
            onDateSelected = onDateSelected
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Day stats
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA)),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("轨迹", "${dayTracks.size} 条")
                    StatItem("点位", "$totalPoints")
                    StatItem("距离", formatDistance(totalDistance))
                    StatItem("时长", formatDuration(totalDuration))
                }
            }
        }

        Text(
            text = "当日时间线",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (dayTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("当天没有轨迹记录", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayTracks.forEach { track ->
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
