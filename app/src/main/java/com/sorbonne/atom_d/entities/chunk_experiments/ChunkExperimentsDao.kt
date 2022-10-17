package com.sorbonne.atom_d.entities.chunk_experiments

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChunkExperimentsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(chunkExperiments: ChunkExperiments)

    @Query("DELETE FROM chunk_experiments WHERE experiment_name LIKE :name")
    suspend fun delete(name: String?)

    @Query("DELETE FROM chunk_experiments")
    suspend fun deleteAll()

    @Query("SELECT * FROM chunk_experiments ORDER BY id ASC")
    fun getMessages(): LiveData<List<ChunkExperiments>>

    @Query("SELECT EXISTS (SELECT * FROM chunk_experiments WHERE experiment_name LIKE :experimentName AND message_size LIKE :messageSize AND message_attempts LIKE :messageAttempts)")
    fun messageExists(experimentName: String, messageSize: Int, messageAttempts: Int): Boolean
}