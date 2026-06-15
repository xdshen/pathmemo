package com.pathmemo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pathmemo.data.model.LocationPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {

    @Insert
    suspend fun insert(point: LocationPoint): Long

    @Insert
    suspend fun insertAll(points: List<LocationPoint>): List<Long>

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsForTrack(trackId: Long): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsForTrackOnce(trackId: Long): List<LocationPoint>

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPoint(trackId: Long): LocationPoint?

    @Query("DELETE FROM location_points WHERE trackId = :trackId")
    suspend fun deletePointsForTrack(trackId: Long)
}
