package com.pathmemo.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.viewmodel.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onDateClick: (Long) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val dates by viewModel.dates.collectAsStateWithLifecycle()
    val datePoints by viewModel.datePoints.collectAsStateWithLifecycle()
    var dateToDelete by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史轨迹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (dates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无轨迹记录", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dates, key = { it.toString() }) { date ->
                    val points = datePoints[date] ?: emptyList()
                    DateItem(
                        date = date,
                        points = points,
                        onClick = {
                            val millis = date.atStartOfDay(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            onDateClick(millis)
                        },
                        onDelete = { dateToDelete = date }
                    )
                }
            }
        }
    }

    dateToDelete?.let { date ->
        DeleteConfirmDialog(
            date = date,
            onDismiss = { dateToDelete = null },
            onConfirm = {
                viewModel.deleteDate(date)
                dateToDelete = null
            }
        )
    }
}

@Composable
private fun DateItem(
    date: LocalDate,
    points: List<LocationPoint>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val totalDistance = computeTotalDistance(points)
    val duration = if (points.isEmpty()) 0L else points.last().timestamp - points.first().timestamp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA)),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "点数: ${points.size}", style = MaterialTheme.typography.bodySmall)
                Text(text = "距离: ${formatDistance(totalDistance)}", style = MaterialTheme.typography.bodySmall)
                Text(text = "时长: ${formatDuration(duration)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除记录") },
        text = { Text("确定要删除 ${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} 的全部轨迹记录吗？此操作不可恢复。") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
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

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) String.format("%.2f km", meters / 1000)
    else String.format("%.0f m", meters)
}
