package com.sorbonne.atom_d.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import com.sorbonne.atom_d.services.Socket
import com.sorbonne.d2d.D2D

abstract class BaseViewModel(context: Context?): ViewModel() {
    var instance: D2D? = null
    var deviceId: String? = null

    @SuppressLint("StaticFieldLeak")
    var socketService: Socket? = null
}