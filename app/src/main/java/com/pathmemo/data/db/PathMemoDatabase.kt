package com.pathmemo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.Track

@Database(
    entities = [Track::class, LocationPoint::class],
    version = 3,
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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellNetworkType TEXT")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellOperator TEXT")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellMcc TEXT")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellMnc TEXT")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellTac INTEGER")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellPci INTEGER")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellCi INTEGER")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellArfcn INTEGER")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellBand TEXT")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellRsrp INTEGER")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellRsrq INTEGER")
        db.execSQL("ALTER TABLE location_points ADD COLUMN cellSinr INTEGER")
    }
}
