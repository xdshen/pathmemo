package com.pathmemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val pointCount: Int = 0
) {
    val durationMillis: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime
}
