package com.sorbonne.atom_d.entities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttemptsDao
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [
    ConnectionAttempts::class
], version = 1)

abstract class RoomDatabase: androidx.room.RoomDatabase() {
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