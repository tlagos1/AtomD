package com.sorbonne.atom_d.ui.experiment

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import kotlinx.coroutines.launch


class ExperimentViewModel(private val repository: DatabaseRepository) : ViewModel() {

    fun insertConnectionAttemptExperiment(connectionAttempts: ConnectionAttempts) = viewModelScope.launch{
        repository.insertConnectionAttempts(connectionAttempts)
    }

    fun getAllConnectionAttempts(): LiveData<List<ConnectionAttempts>> {
        return repository.getAllConnectionAttempts()
    }

    fun getAllExperimentsName(): LiveData<List<CustomQueriesDao.AllExperimentsName>> {
        return repository.getAllExperimentsName()
    }

    fun deleteConnectionAttempts(experimentName: String) = viewModelScope.launch{
        repository.deleteConnectionAttempts(experimentName)
    }
}
class ExperimentViewModelFactory(private val repository: DatabaseRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ExperimentViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return ExperimentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}