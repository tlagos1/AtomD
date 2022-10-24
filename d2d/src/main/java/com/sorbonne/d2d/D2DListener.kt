package com.sorbonne.d2d

import android.location.Location
import com.google.android.gms.nearby.connection.Payload
import org.json.JSONObject

interface D2DListener {

    fun onDiscoveryChange(active: Boolean){}
    fun onConnectivityChange(active: Boolean){}
    fun onEndPointsDiscovered(isActive: Boolean, endPointInfo: JSONObject){}
    fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject){}
    fun onReceivedChunk(payload: Payload){}
    fun onReceivedParameter(parameter: D2D.ParameterTag, value: JSONObject){}
    fun onExperimentProgress(isExperimentBar: Boolean, progression: Int){}
    fun onReceivedTaskResul(from: D2D.ParameterTag, value: JSONObject){}
    fun onInfoPacketReceived(payload: String){}
    fun onLastLocation(location: Location){}
}