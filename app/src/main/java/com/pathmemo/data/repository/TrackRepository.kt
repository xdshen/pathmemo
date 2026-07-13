package com.pathmemo.data.repository

import com.pathmemo.data.db.LocationPointDao
import com.pathmemo.data.db.TrackDao
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track
import com.pathmemo.location.LocationRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TrackRepository(
    private val trackDao: TrackDao,
    private val locationPointDao: LocationPointDao
) {
    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()

    fun getTrackByIdFlow(trackId: Long): Flow<Track?> = trackDao.getTrackByIdFlow(trackId)

    suspend fun getTrackById(trackId: Long): Track? = trackDao.getTrackById(trackId)

    suspend fun getActiveTrack(): Track? = trackDao.getActiveTrack()

    suspend fun createTrack(name: String = generateDefaultTrackName()): Long {
        val track = Track(name = name, startTime = System.currentTimeMillis())
        return trackDao.insert(track)
    }

    suspend fun getOrCreateTrackForDate(date: java.time.LocalDate): Long {
        val zone = java.time.ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val existing = trackDao.getTracksBetween(start, end).firstOrNull()
        return existing?.id ?: createTrackForDate(date)
    }

    suspend fun createTrackForDate(date: java.time.LocalDate): Long {
        val track = Track(
            name = generateTrackNameForDate(date),
            startTime = date.atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        return trackDao.insert(track)
    }

    suspend fun updateTrack(track: Track) = trackDao.update(track)

    suspend fun finishTrack(trackId: Long, distanceMeters: Double, pointCount: Int) {
        val track = trackDao.getTrackById(trackId) ?: return
        trackDao.update(
            track.copy(
                endTime = System.currentTimeMillis(),
                distanceMeters = distanceMeters,
                pointCount = pointCount
            )
        )
    }

    suspend fun deleteTrack(track: Track) {
        locationPointDao.deletePointsForTrack(track.id)
        trackDao.delete(track)
    }

    suspend fun deleteAllTracks() {
        trackDao.deleteAll()
    }

    suspend fun insertPoint(point: LocationPoint): Long {
        return locationPointDao.insert(point)
    }

    suspend fun insertPoints(points: List<LocationPoint>): List<Long> {
        return locationPointDao.insertAll(points)
    }

    fun getPointsForTrack(trackId: Long): Flow<List<LocationPoint>> {
        return locationPointDao.getPointsForTrack(trackId)
    }

    suspend fun getPointsForTrackOnce(trackId: Long): List<LocationPoint> {
        return locationPointDao.getPointsForTrackOnce(trackId)
    }

    suspend fun getLastPoint(trackId: Long): LocationPoint? {
        return locationPointDao.getLastPoint(trackId)
    }

    suspend fun getPointsBetween(start: Long, end: Long): List<LocationPoint> {
        return locationPointDao.getPointsBetween(start, end)
    }

    suspend fun getPointTimeRange(): Pair<Long?, Long?> {
        return locationPointDao.getMinTimestamp() to locationPointDao.getMaxTimestamp()
    }

    suspend fun recalculateTrackStats(trackId: Long) {
        val points = locationPointDao.getPointsForTrackOnce(trackId)
        if (points.isEmpty()) return
        val track = trackDao.getTrackById(trackId) ?: return
        trackDao.update(
            track.copy(
                startTime = points.first().timestamp,
                endTime = points.last().timestamp,
                distanceMeters = LocationRecorder.computeTotalDistance(points),
                pointCount = points.size
            )
        )
    }

    suspend fun renameTrack(trackId: Long, newName: String) {
        val track = trackDao.getTrackById(trackId) ?: return
        trackDao.update(track.copy(name = newName))
    }

    suspend fun getTracksBetween(start: Long, end: Long): List<Track> {
        return trackDao.getTracksBetween(start, end)
    }

    suspend fun getPreviousTrack(time: Long): Track? {
        return trackDao.getPreviousTrack(time)
    }

    suspend fun getNextTrack(time: Long): Track? {
        return trackDao.getNextTrack(time)
    }

    private fun generateDefaultTrackName(): String {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        return "轨迹 $now"
    }

    private fun generateTrackNameForDate(date: java.time.LocalDate): String {
        return "${date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)} 的轨迹"
    }
}
