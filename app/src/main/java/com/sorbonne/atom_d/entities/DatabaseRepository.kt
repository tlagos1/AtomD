package com.sorbonne.atom_d.entities

import android.app.Application
import androidx.lifecycle.LiveData
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperiments
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperimentsDao
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttemptsDao
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao

class DatabaseRepository(application: Application){

    private var chunkExperimentsDao: ChunkExperimentsDao
    private var connectionAttemptsDao: ConnectionAttemptsDao
    private var customQueriesDao: CustomQueriesDao

    init {
        val db = RoomDatabase.getDatabase(application)

        chunkExperimentsDao = db.chunkExperimentsDao()
        connectionAttemptsDao = db.connectionAttemptsDao()
        customQueriesDao = db.customQueriesDao()
    }

    /*
     * =========================================================================
     * Chunk Experiments
     * =========================================================================
     */

    // Room executes all queries on a separate thread.
    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    /*
     * =========================================================================
     * Message
     * =========================================================================
     */
    fun getAllChunks(): LiveData<List<ChunkExperiments>> {
        return chunkExperimentsDao.getMessages()
    }

    suspend fun insertChunkExperiment(chunkExperiments: ChunkExperiments) {
        chunkExperimentsDao.insert(chunkExperiments)
    }

    suspend fun deleteChunkExperiment(name: String) {
        chunkExperimentsDao.delete(name)
    }

    fun chunkExperimentExists(name: String, size: Int, attempts: Int): Boolean {
        return chunkExperimentsDao.messageExists(name, size, attempts)
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