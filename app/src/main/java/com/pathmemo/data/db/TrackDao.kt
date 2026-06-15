package com.pathmemo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pathmemo.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert
    suspend fun insert(track: Track): Long

    @Update
    suspend fun update(track: Track)

    @Delete
    suspend fun delete(track: Track)

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: Long): Track?

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    fun getTrackByIdFlow(trackId: Long): Flow<Track?>

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tracks WHERE startTime >= :start AND startTime < :end ORDER BY startTime ASC")
    suspend fun getTracksBetween(start: Long, end: Long): List<Track>

    @Query("SELECT * FROM tracks WHERE startTime < :time ORDER BY startTime DESC LIMIT 1")
    suspend fun getPreviousTrack(time: Long): Track?

    @Query("SELECT * FROM tracks WHERE startTime > :time ORDER BY startTime ASC LIMIT 1")
    suspend fun getNextTrack(time: Long): Track?
}
