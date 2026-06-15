package com.pathmemo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track

@Database(
    entities = [Track::class, LocationPoint::class],
    version = 1,
    exportSchema = false
)
abstract class PathMemoDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun locationPointDao(): LocationPointDao
}
