package com.sorbonne.d2d.internal

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.nearby.connection.Payload
import org.json.JSONObject

class D2DViewModel: ViewModel() {

    val isDiscoveryActive: MutableLiveData<Boolean> = MutableLiveData()
    val isConnected: MutableLiveData<Boolean> = MutableLiveData()

    val foundDevice: MutableLiveData<JSONObject> = MutableLiveData()
    val lostDevice: MutableLiveData<JSONObject> = MutableLiveData()

    val connectedDevices: MutableLiveData<JSONObject> = MutableLiveData()
    val disconnectedDevices: MutableLiveData<JSONObject> = MutableLiveData()

    val experimentProgress: MutableLiveData<Int> = MutableLiveData()
    val taskProgress: MutableLiveData<Int> = MutableLiveData()

    val fileTransferTaskValue: MutableLiveData<JSONObject> = MutableLiveData()
    val discoveryTaskValue: MutableLiveData<JSONObject> = MutableLiveData()
    val infoPacket: MutableLiveData<MutableMap<Byte, List<String>>> = MutableLiveData()

    val lastLocation: MutableLiveData<Location> = MutableLiveData()
}