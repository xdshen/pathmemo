package com.pathmemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathmemo.data.model.Track
import com.pathmemo.data.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class OverviewViewModel(
    private val repository: TrackRepository
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = repository.getAllTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _dayTracks = MutableStateFlow<List<Track>>(emptyList())
    val dayTracks: StateFlow<List<Track>> = _dayTracks.asStateFlow()

    fun selectDate(date: LocalDate?) {
        if (date == null) {
            _selectedDate.value = null
            _dayTracks.value = emptyList()
            return
        }
        _selectedDate.value = date
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            _dayTracks.value = repository.getTracksBetween(start, end)
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repository.deleteTrack(track)
            _selectedDate.value?.let { selectDate(it) }
        }
    }

    fun renameTrack(track: Track, newName: String) {
        viewModelScope.launch {
            repository.renameTrack(track.id, newName)
            _selectedDate.value?.let { selectDate(it) }
        }
    }
}

fun Track.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun List<Track>.groupByDate(): Map<LocalDate, List<Track>> {
    return groupBy { it.toLocalDate() }.toSortedMap(reverseOrder())
}

fun List<Track>.totalDurationMillis(): Long {
    return sumOf { it.durationMillis }
}
