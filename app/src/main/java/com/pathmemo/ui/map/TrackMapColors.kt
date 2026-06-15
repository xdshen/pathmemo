package com.pathmemo.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Fixed time-of-day buckets used to color track points.
 */
private val TIME_BUCKETS = listOf(
    TimeBucket(0, 6, 0xFF9C27B0.toInt(), "凌晨 00:00-06:00"),
    TimeBucket(6, 12, 0xFF4CAF50.toInt(), "上午 06:00-12:00"),
    TimeBucket(12, 18, 0xFFFF9800.toInt(), "下午 12:00-18:00"),
    TimeBucket(18, 24, 0xFFF44336.toInt(), "晚上 18:00-24:00")
)

private data class TimeBucket(
    val startHour: Int,
    val endHour: Int,
    val color: Int,
    val label: String
)

fun getPointColor(timestamp: Long): Int {
    val hour = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .hour
    return TIME_BUCKETS.firstOrNull { hour >= it.startHour && hour < it.endHour }?.color
        ?: TIME_BUCKETS.last().color
}

@Composable
fun TimeColorLegend(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "时间段图例",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TIME_BUCKETS.forEach { bucket ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(bucket.color))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
