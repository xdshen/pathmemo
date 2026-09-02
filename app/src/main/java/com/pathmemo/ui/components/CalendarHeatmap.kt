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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

    val scrollState = rememberScrollState()
    var hasScrolledToEnd by remember { mutableStateOf(false) }
    LaunchedEffect(scrollState.maxValue) {
        if (!hasScrolledToEnd && scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
            hasScrolledToEnd = true
        }
    }

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

        val seenMonths = mutableSetOf<YearMonth>()
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            weeks.forEach { weekDays ->
                // Month label before the first week that contains a day of a new month
                val newMonthDay = weekDays.firstOrNull { YearMonth.from(it) !in seenMonths }
                if (newMonthDay != null) {
                    seenMonths.add(YearMonth.from(newMonthDay))
                    Box(modifier = Modifier.padding(end = 2.dp)) {
                        Text(
                            text = newMonthDay.format(DateTimeFormatter.ofPattern("M月")),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
