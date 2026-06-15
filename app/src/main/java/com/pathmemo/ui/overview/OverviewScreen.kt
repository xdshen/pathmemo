package com.pathmemo.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pathmemo.data.model.Track
import com.pathmemo.ui.components.CalendarHeatmap
import com.pathmemo.viewmodel.OverviewViewModel
import com.pathmemo.viewmodel.groupByDate
import com.pathmemo.viewmodel.totalDurationMillis
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTrackClick: (Long) -> Unit,
    onDateClick: (Long) -> Unit,
    onNavigateToRangePreview: (Long, Long) -> Unit,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val grouped = remember(tracks) { tracks.groupByDate() }
    val dailyDurations = remember(tracks) {
        grouped.mapValues { it.value.totalDurationMillis() }
    }
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayTracks by viewModel.dayTracks.collectAsStateWithLifecycle()

    var trackToRename by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var showRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("轨迹回顾") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                CalendarHeatmap(
                    dailyDurations = dailyDurations,
                    onDateClick = { date ->
                        val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onDateClick(millis)
                    }
                )
                OutlinedButton(
                    onClick = { showRangePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("自定义日期范围")
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无轨迹记录", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                grouped.forEach { (date, dateTracks) ->
                    item(key = date) {
                        DayCard(
                            date = date,
                            tracks = dateTracks,
                            isExpanded = selectedDate == date,
                            onExpandClick = {
                                if (selectedDate == date) {
                                    viewModel.selectDate(null)
                                } else {
                                    viewModel.selectDate(date)
                                }
                            },
                            onDateClick = onDateClick,
                            onTrackClick = onTrackClick,
                            onRename = { trackToRename = it },
                            onDelete = { trackToDelete = it }
                        )
                    }
                }
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

    if (showRangePicker) {
        DateRangePickerDialog(
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                showRangePicker = false
                val startMillis = start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = end.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                onNavigateToRangePreview(startMillis, endMillis)
            }
        )
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    tracks: List<Track>,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onDateClick: (Long) -> Unit,
    onTrackClick: (Long) -> Unit,
    onRename: (Track) -> Unit,
    onDelete: (Track) -> Unit
) {
    val totalDuration = tracks.totalDurationMillis()
    val totalDistance = tracks.sumOf { it.distanceMeters }
    val trackCount = tracks.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onExpandClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MM月dd日 EEEE", Locale.CHINA)),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${trackCount} 条轨迹 · ${formatDuration(totalDuration)} · ${formatDistance(totalDistance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isExpanded) "收起" else "展开",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (!isExpanded) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onDateClick(millis)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("查看当日地图预览")
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tracks.forEach { track ->
                        TrackRow(
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
private fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${formatTime(track.startTime)} · ${formatDuration(track.durationMillis)} · ${formatDistance(track.distanceMeters)} · ${track.pointCount} 点",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            IconButton(onClick = onRename) {
                Icon(Icons.Filled.Edit, contentDescription = "重命名")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var picking by remember { mutableStateOf<PickTarget?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期范围") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { picking = PickTarget.START },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("开始日期: ${startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}")
                }
                OutlinedButton(
                    onClick = { picking = PickTarget.END },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("结束日期: ${endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val (actualStart, actualEnd) = if (startDate.isAfter(endDate)) {
                        endDate to startDate
                    } else {
                        startDate to endDate
                    }
                    onConfirm(actualStart, actualEnd)
                }
            ) { Text("查看地图") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    picking?.let { target ->
        SingleDatePickerDialog(
            initialDate = if (target == PickTarget.START) startDate else endDate,
            onDismiss = { picking = null },
            onConfirm = { date ->
                if (target == PickTarget.START) startDate = date else endDate = date
                picking = null
            }
        )
    }
}

private enum class PickTarget { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val zone = java.time.ZoneId.systemDefault()
    val initialMillis = initialDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(zone)
                            .toLocalDate()
                        onConfirm(date)
                    } ?: onDismiss()
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
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
