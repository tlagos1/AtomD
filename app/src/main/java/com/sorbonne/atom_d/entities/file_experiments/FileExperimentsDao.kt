package com.sorbonne.atom_d.entities.file_experiments

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileExperimentsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(file: FileExperiments)

    @Query("DELETE FROM file_experiments")
    suspend fun deleteAll()

    @Query("DELETE FROM file_experiments WHERE experiment_name LIKE :name")
    suspend fun delete(name: String)

    @Query("SELECT * FROM file_experiments ORDER BY id ASC")
    fun getFiles(): LiveData<List<FileExperiments>>

    @Query("SELECT COUNT(*) FROM file_experiments WHERE file_size = :size")
    fun numberOfFilesBySize(size: Long): Int
}