package com.sorbonne.atom_d.services.socket

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class SocketViewModel: ViewModel() {

    val receivedMessage: MutableLiveData<JSONObject> =  MutableLiveData()
}