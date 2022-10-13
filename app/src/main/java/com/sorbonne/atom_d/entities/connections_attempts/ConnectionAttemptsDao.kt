package com.sorbonne.atom_d.entities.connections_attempts

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConnectionAttemptsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(repetitions: ConnectionAttempts)

    @Query("DELETE FROM connection_attempts")
    suspend fun deleteAll();

    @Query("DELETE FROM connection_attempts WHERE experiment_name LIKE :name")
    suspend fun delete(name: String)

    @Query("SELECT * FROM connection_attempts ORDER BY id ASC")
    fun getRepetitions(): LiveData<List<ConnectionAttempts>>

}