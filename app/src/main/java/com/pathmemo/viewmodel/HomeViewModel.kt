package com.pathmemo.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathmemo.data.model.AppSettings
import com.pathmemo.data.store.SettingsDataStore
import com.pathmemo.service.LocationRecordService
import com.pathmemo.service.LocationRecordService.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val binderManager: LocationRecordService.BinderManager,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings()).also { stateFlow ->
        viewModelScope.launch {
            settingsDataStore.settings.collect { stateFlow.value = it }
        }
    }

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? LocationRecordService.LocalBinder
            binder?.getService()?.let { recordService ->
                viewModelScope.launch {
                    recordService.state.collect { state ->
                        _recordingState.value = state
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _recordingState.value = RecordingState()
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent(getApplication(), LocationRecordService::class.java)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }

    fun startRecording() {
        sendServiceAction(LocationRecordService.ACTION_START)
    }

    fun pauseRecording() {
        sendServiceAction(LocationRecordService.ACTION_PAUSE)
    }

    fun resumeRecording() {
        sendServiceAction(LocationRecordService.ACTION_RESUME)
    }

    fun stopRecording() {
        sendServiceAction(LocationRecordService.ACTION_STOP)
    }

    fun setAutoRecordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val dataStore = settingsDataStore
            dataStore.setAutoRecordEnabled(enabled)
            val currentState = _recordingState.value
            if (enabled && !currentState.isRecording) {
                sendServiceAction(LocationRecordService.ACTION_START)
            } else if (!enabled && currentState.isRecording) {
                sendServiceAction(LocationRecordService.ACTION_STOP)
            }
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(getApplication(), LocationRecordService::class.java).apply {
            this.action = action
        }
        getApplication<Application>().startService(intent)
    }
}
