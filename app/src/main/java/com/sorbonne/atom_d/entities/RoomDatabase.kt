package com.sorbonne.atom_d.entities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperiments
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperimentsDao
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttemptsDao
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [
    ChunkExperiments::class,
    ConnectionAttempts::class
], version = 1)

abstract class RoomDatabase: androidx.room.RoomDatabase() {

    abstract fun chunkExperimentsDao(): ChunkExperimentsDao
    abstract fun connectionAttemptsDao(): ConnectionAttemptsDao
    abstract fun customQueriesDao(): CustomQueriesDao

    companion object {
        @Volatile
        private var INSTANCE: RoomDatabase? = null
        fun getDatabase(context: Context): RoomDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    RoomDatabase::class.java, "AtomD_DB"
                ).build()
            }
            return INSTANCE as RoomDatabase
        }
    }
}