package com.sorbonne.d2d.tools

import org.json.JSONObject

class ConnectedDevices {

    private var connectedDevices = mutableMapOf<String,JSONObject>()

    fun addNewDevice(endPointId: String, endPointName: String){
        val  parameters = JSONObject()
        parameters.put("deviceName", endPointName)
        connectedDevices[endPointId] = parameters
    }

    fun removeDevice(endPointId: String){
        connectedDevices.remove(endPointId)
    }

    fun isEmpty(): Boolean{
        return connectedDevices.isEmpty()
    }

    fun clear(){
        connectedDevices.clear()
    }

    fun getDevices(): MutableCollection<JSONObject> {
        return connectedDevices.values
    }

    fun getEndPointIds(): MutableSet<String> {
        return connectedDevices.keys
    }

    fun getDeviceParameters(endPointId: String): String {
        return connectedDevices[endPointId].toString()
    }
}