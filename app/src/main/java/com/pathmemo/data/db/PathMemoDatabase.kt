package com.pathmemo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track

@Database(
    entities = [Track::class, LocationPoint::class],
    version = 2,
    exportSchema = false
)
abstract class PathMemoDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun locationPointDao(): LocationPointDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_location_points_latitude_longitude " +
            "ON location_points(latitude, longitude)"
        )
    }
}
