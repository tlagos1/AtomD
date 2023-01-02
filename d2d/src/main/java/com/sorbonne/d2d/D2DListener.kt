package com.sorbonne.d2d

import android.location.Location
import org.json.JSONObject

interface D2DListener {

    fun onDiscoveryChange(active: Boolean){}
    fun onConnectivityChange(active: Boolean){}
    fun onEndPointsDiscovered(isActive: Boolean, endPointInfo: JSONObject){}
    fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject){}
    fun onBandwidthQuality(endPointInfo: JSONObject){}
    fun onExperimentProgress(isExperimentBar: Boolean, progression: Int){}
    fun onReceivedTaskResul(from: D2D.ParameterTag, value: JSONObject){}
    fun onInfoPacketReceived(messageTag: Byte,payload: List<String>){}
    fun onLastLocation(location: Location){}
}