package com.sorbonne.atom_d.entities.connections_attempts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "connection_attempts",
        indices =[
            Index(
                value = ["experiment_name"],
                unique = true
            )
        ])
data class ConnectionAttempts(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "experiment_name")
    val expName: String,

    @ColumnInfo(name = "number_of_repetitions")
    val repetitions: Int
)