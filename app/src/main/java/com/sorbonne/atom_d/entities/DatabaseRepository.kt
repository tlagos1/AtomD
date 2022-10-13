package com.sorbonne.atom_d.entities

import android.app.Application
import androidx.lifecycle.LiveData
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttemptsDao
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao

class DatabaseRepository(application: Application){

    private var connectionAttemptsDao: ConnectionAttemptsDao
    private var customQueriesDao: CustomQueriesDao

    init {
        val db = RoomDatabase.getDatabase(application)
        connectionAttemptsDao = db.connectionAttemptsDao()
        customQueriesDao = db.customQueriesDao()
    }

    /*
     * =========================================================================
     * connectionAttempts
     * =========================================================================
     */

    fun getAllConnectionAttempts(): LiveData<List<ConnectionAttempts>> {
        return connectionAttemptsDao.getRepetitions()
    }

    suspend fun insertConnectionAttempts(connectionAttempts: ConnectionAttempts){
        connectionAttemptsDao.insert(connectionAttempts)
    }

    suspend fun deleteConnectionAttempts(name: String){
        connectionAttemptsDao.delete(name)
    }

    /*
     * =========================================================================
     * customQuery
     * =========================================================================
     */

    fun getAllExperimentsName(): LiveData<List<CustomQueriesDao.AllExperimentsName>> {
        return customQueriesDao.getAllExperimentsName()
    }

}