package com.sorbonne.atom_d.entities.file_experiments

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "file_experiments",
    indices =[
        Index(
            value = ["experiment_name"],
            unique = true
        )
    ])
data class FileExperiments(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "experiment_name")
    val expName: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "number_of_tries")
    val fileTries: Int
)