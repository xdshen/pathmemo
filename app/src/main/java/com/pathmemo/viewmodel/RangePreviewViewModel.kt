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

class RangePreviewViewModel(
    private val repository: TrackRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _startDate = MutableStateFlow(LocalDate.now().minusDays(6))
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(LocalDate.now())
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _rangePoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val rangePoints: StateFlow<List<LocationPoint>> = _rangePoints.asStateFlow()

    val settings = settingsDataStore.settings

    fun setRange(start: LocalDate, end: LocalDate) {
        val (actualStart, actualEnd) = if (start.isAfter(end)) end to start else start to end
        _startDate.value = actualStart
        _endDate.value = actualEnd
        loadRange(actualStart, actualEnd)
    }

    private fun loadRange(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            _rangePoints.value = repository.getPointsBetween(startMillis, endMillis)
        }
    }

    fun refresh() {
        loadRange(_startDate.value, _endDate.value)
    }
}
