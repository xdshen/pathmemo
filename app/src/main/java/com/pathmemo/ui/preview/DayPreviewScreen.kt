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
    viewModel: DayPreviewViewModel = koinViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayPoints by viewModel.dayPoints.collectAsStateWithLifecycle()
    val dailyDurations by viewModel.allDailyDurations.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = com.pathmemo.data.model.AppSettings())

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
                dayPoints = dayPoints,
                dailyDurations = dailyDurations,
                onDateSelected = { viewModel.selectDate(it) },
                onDelete = { viewModel.deleteDay(selectedDate) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TrackMapView(
                points = dayPoints,
                selectedPoint = selectedPoint,
                onPointClick = { selectedPoint = it },
                modifier = Modifier.fillMaxSize(),
                mapType = settings.mapType
            )
        }
    }
}

@Composable
private fun DayPreviewSheetContent(
    selectedDate: LocalDate,
    dayPoints: List<LocationPoint>,
    dailyDurations: Map<LocalDate, Long>,
    onDateSelected: (LocalDate) -> Unit,
    onDelete: () -> Unit
) {
    val totalDistance = computeTotalDistance(dayPoints)
    val totalDuration = if (dayPoints.isEmpty()) 0L else dayPoints.last().timestamp - dayPoints.first().timestamp
    val totalPoints = dayPoints.size

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
                    StatItem("记录段", "${dayPoints.map { it.trackId }.toSet().size} 段")
                    StatItem("点位", "$totalPoints")
                    StatItem("距离", formatDistance(totalDistance))
                    StatItem("时长", formatDuration(totalDuration))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当日时间线",
                style = MaterialTheme.typography.titleMedium
            )
            if (dayPoints.isNotEmpty()) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(20.dp))
                }
            }
        }

        if (dayPoints.isEmpty()) {
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
                val startTime = dayPoints.first().timestamp
                val endTime = dayPoints.last().timestamp
                Text(
                    text = "${formatTime(startTime)} - ${formatTime(endTime)} · ${formatDistance(totalDistance)} · ${formatDuration(totalDuration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
