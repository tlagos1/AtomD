package com.sorbonne.atom_d.entities.chunk_experiments

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chunk_experiments",
    indices =[
        Index(
            value = ["experiment_name"],
            unique = true
        )
    ])
data class ChunkExperiments(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "experiment_name")
    val expName: String?,

    @ColumnInfo(name = "message_size")
    val chunkSize: Int,

    @ColumnInfo(name = "message_attempts")
    val chunkAttempts:Int,

    @ColumnInfo(name = "message_payload")
    val chunkPayload: ByteArray?
)