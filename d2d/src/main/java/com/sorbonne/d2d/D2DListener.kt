package com.sorbonne.d2d

import com.google.android.gms.nearby.connection.Payload
import org.json.JSONObject

interface D2DListener {

    fun onDiscoveryChange(active: Boolean){}
    fun onConnectivityChange(active: Boolean){}
    fun onEndPointsDiscovered(isActive: Boolean, endPointInfo: JSONObject){}
    fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject){}
    fun onReceivedChunk(payload: Payload){}
    fun onReceivedParameter(parameter: D2D.ParameterTag, value: JSONObject){}

}