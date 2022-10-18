package com.sorbonne.atom_d.ui.dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.ui.BaseViewModel

class DashboardViewModel(context: Context?, private val repository: DatabaseRepository) : BaseViewModel(context)  {
    class Factory(private val context: Context?, private val repository: DatabaseRepository): ViewModelProvider.NewInstanceFactory() {
        override fun <T: ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(context, repository) as T
    }

    fun getAllExperimentsName(): LiveData<List<CustomQueriesDao.AllExperimentsName>> {
        return repository.getAllExperimentsName()
    }
}