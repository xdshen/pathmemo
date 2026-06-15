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

    private val _dayTracks = MutableStateFlow<List<Track>>(emptyList())
    val dayTracks: StateFlow<List<Track>> = _dayTracks.asStateFlow()

    private val _dayPoints = MutableStateFlow<Map<Long, List<LocationPoint>>>(emptyMap())
    val dayPoints: StateFlow<Map<Long, List<LocationPoint>>> = _dayPoints.asStateFlow()

    private val _allDailyDurations = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())
    val allDailyDurations: StateFlow<Map<LocalDate, Long>> = _allDailyDurations.asStateFlow()

    val settings = settingsDataStore.settings

    init {
        loadAllTracksSummary()
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
            val tracks = repository.getTracksBetween(start, end)
            _dayTracks.value = tracks

            val pointsMap = mutableMapOf<Long, List<LocationPoint>>()
            tracks.forEach { track ->
                pointsMap[track.id] = repository.getPointsForTrackOnce(track.id)
            }
            _dayPoints.value = pointsMap
        }
    }

    private fun loadAllTracksSummary() {
        viewModelScope.launch {
            repository.getAllTracks().collect { tracks ->
                val durations = tracks.groupBy { it.toLocalDate() }
                    .mapValues { entry -> entry.value.sumOf { it.durationMillis } }
                _allDailyDurations.value = durations
            }
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repository.deleteTrack(track)
            loadDay(_selectedDate.value)
        }
    }

    fun renameTrack(track: Track, newName: String) {
        viewModelScope.launch {
            repository.renameTrack(track.id, newName)
            loadDay(_selectedDate.value)
        }
    }
}
