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

class HistoryViewModel(
    private val repository: TrackRepository
) : ViewModel() {

    private val _dates = MutableStateFlow<List<LocalDate>>(emptyList())
    val dates: StateFlow<List<LocalDate>> = _dates.asStateFlow()

    private val _datePoints = MutableStateFlow<Map<LocalDate, List<LocationPoint>>>(emptyMap())
    val datePoints: StateFlow<Map<LocalDate, List<LocationPoint>>> = _datePoints.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val (min, max) = repository.getPointTimeRange()
            if (min == null || max == null) {
                _dates.value = emptyList()
                return@launch
            }

            val zone = ZoneId.systemDefault()
            val startDate = Instant.ofEpochMilli(min).atZone(zone).toLocalDate()
            val endDate = Instant.ofEpochMilli(max).atZone(zone).toLocalDate()

            val dateList = mutableListOf<LocalDate>()
            val pointsMap = mutableMapOf<LocalDate, List<LocationPoint>>()

            var current = startDate
            while (!current.isAfter(endDate)) {
                val dayStart = current.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = current.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val points = repository.getPointsBetween(dayStart, dayEnd)
                if (points.isNotEmpty()) {
                    dateList.add(current)
                    pointsMap[current] = points
                }
                current = current.plusDays(1)
            }

            _dates.value = dateList.sortedDescending()
            _datePoints.value = pointsMap
        }
    }

    fun refresh() {
        loadHistory()
    }

    fun deleteDate(date: LocalDate) {
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
            loadHistory()
        }
    }
}
