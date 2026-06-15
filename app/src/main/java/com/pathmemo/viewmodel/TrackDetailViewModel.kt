package com.pathmemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathmemo.data.model.AppSettings
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track
import com.pathmemo.data.repository.TrackRepository
import com.pathmemo.data.store.SettingsDataStore
import com.pathmemo.location.LocationRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackDetailViewModel(
    private val trackId: Long,
    private val repository: TrackRepository,
    settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _points = MutableStateFlow<List<LocationPoint>>(emptyList())
    val points: StateFlow<List<LocationPoint>> = _points.asStateFlow()

    val track: StateFlow<Track?> = repository.getTrackByIdFlow(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _playbackIndex = MutableStateFlow(0)
    val playbackIndex: StateFlow<Int> = _playbackIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _adjacentTracks = MutableStateFlow<Pair<Track?, Track?>>(Pair(null, null))
    val adjacentTracks: StateFlow<Pair<Track?, Track?>> = _adjacentTracks.asStateFlow()

    private val _selectedPoint = MutableStateFlow<LocationPoint?>(null)
    val selectedPoint: StateFlow<LocationPoint?> = _selectedPoint.asStateFlow()

    val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings()).also { stateFlow ->
        viewModelScope.launch {
            settingsDataStore.settings.collect { stateFlow.value = it }
        }
    }

    private var playbackJob: Job? = null

    init {
        loadPoints()
        loadAdjacentTracks()
    }

    private fun loadPoints() {
        viewModelScope.launch {
            _points.value = repository.getPointsForTrackOnce(trackId)
        }
    }

    private fun loadAdjacentTracks() {
        viewModelScope.launch {
            val current = repository.getTrackById(trackId) ?: return@launch
            val prev = repository.getPreviousTrack(current.startTime)
            val next = repository.getNextTrack(current.startTime)
            _adjacentTracks.value = Pair(prev, next)
        }
    }

    fun seekTo(index: Int) {
        _playbackIndex.value = index.coerceIn(0, (_points.value.size - 1).coerceAtLeast(0))
        if (_isPlaying.value) {
            startPlayback()
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        if (_points.value.size < 2) return
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_isPlaying.value && _playbackIndex.value < _points.value.lastIndex) {
                val current = _points.value[_playbackIndex.value]
                val next = _points.value[_playbackIndex.value + 1]
                val interval = (next.timestamp - current.timestamp).coerceAtLeast(100L)
                val adjustedInterval = (interval / _playbackSpeed.value).toLong()
                delay(adjustedInterval.coerceIn(50L, 2000L))
                if (_isPlaying.value) {
                    _playbackIndex.value += 1
                }
            }
            if (_playbackIndex.value >= _points.value.lastIndex) {
                _isPlaying.value = false
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun resetPlayback() {
        pausePlayback()
        _playbackIndex.value = 0
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun selectPoint(point: LocationPoint?) {
        _selectedPoint.value = point
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }
}
