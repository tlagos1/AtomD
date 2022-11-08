package com.sorbonne.atom_d.entities.data_file_experiments

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface DataFileExperimentsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(data: DataFileExperiments)
}