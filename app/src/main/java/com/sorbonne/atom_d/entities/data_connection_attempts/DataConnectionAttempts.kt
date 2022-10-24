package com.sorbonne.atom_d.entities.data_connection_attempts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "data_connection_attempts")
data class DataConnectionAttempts (

    @PrimaryKey(autoGenerate = true)
    val Id: Int,

    @ColumnInfo(name = "experiment_id")
    val experimentId: Long,

    @ColumnInfo(name = "source_id")
    val sourceId: String,

    @ColumnInfo(name = "target_id")
    val targetId: String,

    @ColumnInfo(name = "total_attempts")
    val totalAttempts: Int,

    @ColumnInfo(name = "low_power")
    val lowPower: Boolean,

    @ColumnInfo(name = "attempt")
    val attempt: Int,

    @ColumnInfo(name = "state")
    val state: String,

    @ColumnInfo(name = "state_timing")
    val stateTiming: Long,

    @ColumnInfo(name = "latitude")
     val gpsLatitude: Double,

    @ColumnInfo(name = "longitude")
     val gpsLongitude: Double
)