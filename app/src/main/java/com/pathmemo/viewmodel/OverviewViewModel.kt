package com.pathmemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class OverviewViewModel(
    private val repository: TrackRepository
) : ViewModel() {

    private val _allPoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val allPoints: StateFlow<List<LocationPoint>> = _allPoints.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    init {
        loadAllPoints()
    }

    private fun loadAllPoints() {
        viewModelScope.launch {
            val (min, max) = repository.getPointTimeRange()
            if (min == null || max == null) {
                _allPoints.value = emptyList()
                return@launch
            }
            _allPoints.value = repository.getPointsBetween(min, max + 1)
        }
    }

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun refresh() {
        loadAllPoints()
        _selectedDate.value?.let { selectDate(it) }
    }

    fun deleteDay(date: LocalDate) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val points = repository.getPointsBetween(start, end)
            val trackIds = points.map { it.trackId }.toSet()
            trackIds.forEach { trackId ->
                val track = repository.getTrackById(trackId) ?: return@forEach
                repository.deleteTrack(track)
            }
            refresh()
        }
    }
}

fun LocationPoint.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun List<LocationPoint>.groupByDate(): Map<LocalDate, List<LocationPoint>> {
    return groupBy { it.toLocalDate() }.toSortedMap(reverseOrder())
}

fun List<LocationPoint>.totalDurationMillis(): Long {
    if (isEmpty()) return 0L
    val first = minOf { it.timestamp }
    val last = maxOf { it.timestamp }
    return last - first
}
