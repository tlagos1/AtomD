package com.sorbonne.atom_d.entities.data_file_experiments

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_file_experiments")
data class DataFileExperiments(

    @PrimaryKey(autoGenerate = true)
    val Id:Int,

    @ColumnInfo(name = "experiment_id")
    val experimentId: Long,

    @ColumnInfo(name = "experiment_name")
    val experimentName: String,

    @ColumnInfo(name = "source_id")
    val sourceId: String,

    @ColumnInfo(name = "target_id")
    val targetId: String,

    @ColumnInfo(name = "repetition")
    val repetition: Int,

    @ColumnInfo(name = "bytes_transferred")
    val bytesTransferred: Long,

    @ColumnInfo(name = "bytes_to_transfer")
    val bytes_to_transfer: Long,

    @ColumnInfo(name = "timing")
    val timing: Long,

    @ColumnInfo(name = "uploading")
    val uploading: Boolean,

    @ColumnInfo(name = "strategy")
    val strategy: Int
)