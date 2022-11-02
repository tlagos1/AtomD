package com.sorbonne.atom_d.services.socket

import androidx.lifecycle.MutableLiveData
import org.json.JSONObject

class SocketObservers {
    val receivedMessage: MutableLiveData<JSONObject> =  MutableLiveData()
}