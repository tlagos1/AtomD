package com.sorbonne.d2d.internal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class D2DViewModel: ViewModel() {

    val isDiscoveryActive: MutableLiveData<Boolean> = MutableLiveData(false)
    val isConnected: MutableLiveData<Boolean> = MutableLiveData(false)

    val foundDevice: MutableLiveData<JSONObject> = MutableLiveData(JSONObject())
    val lostDevice: MutableLiveData<JSONObject> = MutableLiveData(JSONObject())


}