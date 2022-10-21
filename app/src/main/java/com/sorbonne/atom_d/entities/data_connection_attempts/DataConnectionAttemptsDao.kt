package com.sorbonne.atom_d.entities.data_connection_attempts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DataConnectionAttemptsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(data: DataConnectionAttempts)
}