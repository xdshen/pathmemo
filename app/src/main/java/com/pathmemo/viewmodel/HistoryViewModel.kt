package com.pathmemo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathmemo.data.model.Track
import com.pathmemo.data.repository.TrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: TrackRepository
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = repository.getAllTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repository.deleteTrack(track)
        }
    }

    fun renameTrack(track: Track, newName: String) {
        viewModelScope.launch {
            repository.renameTrack(track.id, newName)
        }
    }
}
