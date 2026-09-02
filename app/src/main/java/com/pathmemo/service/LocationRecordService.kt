package com.pathmemo.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pathmemo.PathMemoApp
import com.pathmemo.R
import com.pathmemo.data.model.AppSettings
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track
import com.pathmemo.data.repository.TrackRepository
import com.pathmemo.data.store.SettingsDataStore
import com.pathmemo.location.CellInfoProvider
import com.pathmemo.location.CellInfoSnapshot
import com.pathmemo.location.LocationRecorder
import com.pathmemo.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.time.LocalDate
import java.time.ZoneId

class LocationRecordService : Service() {

    private val repository: TrackRepository by inject()
    private val locationRecorder: LocationRecorder by inject()
    private val cellInfoProvider: CellInfoProvider by inject()
    private val settingsDataStore: SettingsDataStore by inject()
    private val binderManager: BinderManager by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var recordingJob: Job? = null
    private var currentSettings: AppSettings = AppSettings()

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        binderManager.attachService(this)
        serviceScope.launch {
            settingsDataStore.settings.collect { currentSettings = it }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        recordingJob?.cancel()
        locationRecorder.stop()
        binderManager.detachService()
    }

    fun startRecording() {
        if (_state.value.isRecording) return

        startForegroundService()

        serviceScope.launch {
            val activeTrack = repository.getActiveTrack()
            if (activeTrack != null) {
                beginRecording(activeTrack.id, activeTrack)
            } else {
                val today = LocalDate.now(ZoneId.systemDefault())
                val trackId = repository.getOrCreateTrackForDate(today)
                val track = repository.getTrackById(trackId)
                beginRecording(trackId, track)
            }
        }
    }

    private suspend fun beginRecording(trackId: Long, track: Track?) {
        val now = System.currentTimeMillis()
        _state.update {
            it.copy(
                isRecording = true,
                isPaused = false,
                currentTrackId = trackId,
                startTime = track?.startTime ?: now,
                distanceMeters = track?.distanceMeters ?: 0.0,
                pointCount = track?.pointCount ?: 0
            )
        }

        recordingJob?.cancel()
        recordingJob = serviceScope.launch { collectLocations() }
    }

    private suspend fun collectLocations() {
        locationRecorder.start(
            intervalMillis = currentSettings.locationInterval.millis,
            minDistanceMeters = currentSettings.minDistance.meters,
            minAccuracyMeters = currentSettings.minAccuracy.meters
        ).collect { location ->
            val state = _state.value
            if (state.isPaused || state.currentTrackId == null) return@collect

            val lastPoint = repository.getLastPoint(state.currentTrackId)
            val addedDistance = if (lastPoint != null) {
                LocationRecorder.computeDistance(
                    lastPoint.latitude, lastPoint.longitude,
                    location.latitude, location.longitude
                )
            } else 0.0

            val cell = cellInfoProvider.snapshot()
            val point = LocationPoint(
                trackId = state.currentTrackId,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy,
                speed = location.speed,
                timestamp = location.time,
                cellNetworkType = cell?.networkType,
                cellOperator = cell?.operatorName,
                cellMcc = cell?.mcc,
                cellMnc = cell?.mnc,
                cellTac = cell?.tac,
                cellPci = cell?.pci,
                cellCi = cell?.ci,
                cellArfcn = cell?.arfcn,
                cellBand = cell?.band,
                cellRsrp = cell?.rsrp,
                cellRsrq = cell?.rsrq,
                cellSinr = cell?.sinr
            )
            repository.insertPoint(point)

            _state.update { current ->
                current.copy(
                    distanceMeters = current.distanceMeters + addedDistance,
                    pointCount = current.pointCount + 1,
                    lastLocation = location,
                    lastCellInfo = cell ?: current.lastCellInfo
                )
            }

            updateNotification()
        }
    }

    fun pauseRecording() {
        _state.update { it.copy(isPaused = true) }
        updateNotification()
    }

    fun resumeRecording() {
        _state.update { it.copy(isPaused = false) }
        updateNotification()
    }

    fun stopRecording() {
        val current = _state.value
        val trackId = current.currentTrackId

        recordingJob?.cancel()
        recordingJob = null
        locationRecorder.stop()

        if (trackId != null) {
            serviceScope.launch {
                repository.finishTrack(trackId, current.distanceMeters, current.pointCount)
            }
        }

        _state.update {
            RecordingState()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Restore an active (unfinished) track from the database.
     * Used when the service restarts unexpectedly or on boot.
     */
    fun restoreActiveTrack() {
        if (_state.value.isRecording) return
        serviceScope.launch {
            val track = repository.getActiveTrack() ?: return@launch
            startForegroundService()
            beginRecording(track.id, track)
        }
    }

    private fun startForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stateText = when {
            _state.value.isPaused -> "已暂停"
            _state.value.isRecording -> "记录中 · ${formatDistance(_state.value.distanceMeters)}"
            else -> "准备中"
        }

        return NotificationCompat.Builder(this, PathMemoApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title_recording))
            .setContentText(stateText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.2f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationRecordService = this@LocationRecordService
    }

    /**
     * Holds a reference to the running service so ViewModels can bind to it.
     */
    class BinderManager {
        private var service: LocationRecordService? = null

        fun attachService(service: LocationRecordService) {
            this.service = service
        }

        fun detachService() {
            this.service = null
        }

        fun getService(): LocationRecordService? = service
    }

    data class RecordingState(
        val isRecording: Boolean = false,
        val isPaused: Boolean = false,
        val currentTrackId: Long? = null,
        val startTime: Long = 0L,
        val distanceMeters: Double = 0.0,
        val pointCount: Int = 0,
        val lastLocation: com.amap.api.location.AMapLocation? = null,
        val lastCellInfo: CellInfoSnapshot? = null
    ) {
        val elapsedMillis: Long
            get() = if (isRecording) System.currentTimeMillis() - startTime else 0L
    }

    companion object {
        const val ACTION_START = "com.pathmemo.action.START_RECORDING"
        const val ACTION_STOP = "com.pathmemo.action.STOP_RECORDING"
        const val ACTION_PAUSE = "com.pathmemo.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.pathmemo.action.RESUME_RECORDING"

        private const val NOTIFICATION_ID = 1001
    }
}
