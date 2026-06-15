package com.pathmemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Composable
fun CalendarHeatmap(
    dailyDurations: Map<LocalDate, Long>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val weeksToShow = 20
    val startDate = today.minusWeeks(weeksToShow.toLong()).with(DayOfWeek.MONDAY)
    val totalDays = ChronoUnit.DAYS.between(startDate, today).toInt() + 1

    val days = (0 until totalDays).map { startDate.plusDays(it.toLong()) }
    val weeks = days.chunked(7)

    val maxDuration = dailyDurations.values.maxOrNull()?.toFloat() ?: 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "近 ${weeksToShow} 周轨迹",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            weeks.forEachIndexed { weekIndex, weekDays ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    weekDays.forEach { date ->
                        val duration = dailyDurations[date] ?: 0L
                        val intensity = if (maxDuration > 0) duration / maxDuration else 0f
                        DayCell(
                            date = date,
                            intensity = intensity,
                            onClick = { onDateClick(date) }
                        )
                    }
                }
                // Month label on first week of month
                if (weekDays.any { it.dayOfMonth <= 7 }) {
                    val monthDate = weekDays.first { it.dayOfMonth <= 7 }
                    if (monthDate.dayOfWeek == DayOfWeek.MONDAY || weekDays.indexOf(monthDate) == 0) {
                        Box(modifier = Modifier.padding(start = 2.dp, top = 0.dp)) {
                            Text(
                                text = monthDate.format(DateTimeFormatter.ofPattern("M月")),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HeatmapLegend()
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    intensity: Float,
    onClick: () -> Unit
) {
    val color = when {
        intensity <= 0f -> MaterialTheme.colorScheme.surfaceVariant
        intensity < 0.25f -> Color(0xFF9BE9A8)
        intensity < 0.5f -> Color(0xFF40C463)
        intensity < 0.75f -> Color(0xFF30A14E)
        else -> Color(0xFF216E39)
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun HeatmapLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "少",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            Color(0xFF9BE9A8),
            Color(0xFF40C463),
            Color(0xFF30A14E),
            Color(0xFF216E39)
        ).forEach { color ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
        Text(
            text = "多",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
