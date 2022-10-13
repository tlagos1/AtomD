package com.sorbonne.d2d

import org.json.JSONObject

interface D2DListener {

    fun onDiscoveryChange(active: Boolean){}
    fun onConnectivityChange(active: Boolean){}
    fun onEndPointsDiscovered(isActive: Boolean, endPointInfo: JSONObject){}
    fun onReceivedParameter(parameter: D2D.ParameterTag, value: JSONObject){}
}