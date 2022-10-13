package com.sorbonne.atom_d.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sorbonne.atom_d.ui.BaseViewModel

class DashboardViewModel(context: Context?) : BaseViewModel(context)  {
    class Factory(private val context: Context?): ViewModelProvider.NewInstanceFactory() {
        override fun <T: ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(context) as T
    }
}