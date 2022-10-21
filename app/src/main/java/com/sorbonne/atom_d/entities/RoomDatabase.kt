package com.sorbonne.atom_d.entities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperiments
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperimentsDao
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttemptsDao
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.entities.data_connection_attempts.DataConnectionAttempts
import com.sorbonne.atom_d.entities.data_connection_attempts.DataConnectionAttemptsDao
import com.sorbonne.atom_d.entities.file_experiments.FileExperiments
import com.sorbonne.atom_d.entities.file_experiments.FileExperimentsDao

@Database(entities = [
    ChunkExperiments::class,
    FileExperiments::class,
    ConnectionAttempts::class,
    DataConnectionAttempts::class
], version = 1)

abstract class RoomDatabase: androidx.room.RoomDatabase() {

    abstract fun chunkExperimentsDao(): ChunkExperimentsDao
    abstract fun FileExperimentsDao(): FileExperimentsDao
    abstract fun connectionAttemptsDao(): ConnectionAttemptsDao
    abstract fun customQueriesDao(): CustomQueriesDao

    abstract fun dataConnectionAttemptsDao(): DataConnectionAttemptsDao

    companion object {
        @Volatile
        private var INSTANCE: RoomDatabase? = null
        fun getDatabase(context: Context): RoomDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    RoomDatabase::class.java, "atomd_db"
                ).build()
            }
            return INSTANCE as RoomDatabase
        }
    }
}