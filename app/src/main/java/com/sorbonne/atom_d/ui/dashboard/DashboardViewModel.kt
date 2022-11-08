package com.sorbonne.atom_d.ui.dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.entities.data_connection_attempts.DataConnectionAttempts
import com.sorbonne.atom_d.entities.data_file_experiments.DataFileExperiments
import com.sorbonne.atom_d.ui.BaseViewModel
import kotlinx.coroutines.launch

class DashboardViewModel(context: Context?, private val repository: DatabaseRepository) : BaseViewModel(context)  {
    class Factory(private val context: Context?, private val repository: DatabaseRepository): ViewModelProvider.NewInstanceFactory() {
        override fun <T: ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(context, repository) as T
    }

    fun insertFileExperiments(dataFileExperiments: DataFileExperiments) = viewModelScope.launch {
        repository.insertDataFileExperiments(dataFileExperiments)
    }

    fun insertDataConnectionAttempts(dataConnectionAttempts: DataConnectionAttempts) = viewModelScope.launch {
        repository.insertDataConnectionAttempts(dataConnectionAttempts)
    }

    val connectedDevices: MutableLiveData<List<List<String>>> by lazy {
        MutableLiveData<List<List<String>>>()
    }
}