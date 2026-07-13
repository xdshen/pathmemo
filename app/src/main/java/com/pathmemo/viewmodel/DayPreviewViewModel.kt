package com.pathmemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track
import com.pathmemo.data.repository.TrackRepository
import com.pathmemo.data.store.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class DayPreviewViewModel(
    private val repository: TrackRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _dayPoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val dayPoints: StateFlow<List<LocationPoint>> = _dayPoints.asStateFlow()

    private val _allDailyDurations = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())
    val allDailyDurations: StateFlow<Map<LocalDate, Long>> = _allDailyDurations.asStateFlow()

    val settings = settingsDataStore.settings

    init {
        loadAllPointsSummary()
        loadDay(_selectedDate.value)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadDay(date)
    }

    private fun loadDay(date: LocalDate) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            _dayPoints.value = repository.getPointsBetween(start, end)
        }
    }

    private fun loadAllPointsSummary() {
        viewModelScope.launch {
            val (min, max) = repository.getPointTimeRange()
            if (min == null || max == null) {
                _allDailyDurations.value = emptyMap()
                return@launch
            }
            val points = repository.getPointsBetween(min, max + 1)
            val durations = points.groupBy { it.toLocalDate() }
                .mapValues { entry -> entry.value.totalDurationMillis() }
            _allDailyDurations.value = durations
        }
    }

    fun refresh() {
        loadAllPointsSummary()
        loadDay(_selectedDate.value)
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
