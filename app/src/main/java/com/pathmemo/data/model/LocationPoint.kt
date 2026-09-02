package com.pathmemo.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackId", "timestamp"]),
        Index(value = ["latitude", "longitude"])
    ]
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long,
    // Serving cell (base station) snapshot taken at record time; null when unavailable.
    val cellNetworkType: String? = null,
    val cellOperator: String? = null,
    val cellMcc: String? = null,
    val cellMnc: String? = null,
    val cellTac: Int? = null,
    val cellPci: Int? = null,
    val cellCi: Long? = null,
    val cellArfcn: Int? = null,
    val cellBand: String? = null,
    val cellRsrp: Int? = null,
    val cellRsrq: Int? = null,
    val cellSinr: Int? = null
)
