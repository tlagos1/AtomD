package com.sorbonne.d2d

import android.app.Activity
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.nearby.connection.Payload
import com.sorbonne.d2d.internal.D2DSDK
import com.google.android.gms.nearby.connection.Strategy
import org.json.JSONObject
import java.util.Objects

object D2D {
    private var sdk: D2DSDK ?= null
    private val instance = this

    enum class ParameterTag{
        DISCOVERY
    }

    class Builder(private val owner: LifecycleOwner, val deviceName: String, val context: Context){
        private var listener: D2DListener?= null

        fun setListener(listener: D2DListener): Builder{
            this.listener = listener
            return this
        }

        fun build(): D2D{
            instance.sdk = D2DSDK()
            instance.sdk?.launchSDK(owner, listener, context)
            instance.sdk?.setDeviceName(deviceName)
            return instance
        }
    }

    fun startDiscovery(strategy: Strategy, lowPower: Boolean){
        instance.sdk?.startDiscovery(strategy, lowPower)
    }

    fun startAdvertising(deviceName: String, strategy: Strategy, lowPower: Boolean, connectionType: Int){
        instance.sdk?.startAdvertising(deviceName, strategy, lowPower, connectionType)
    }

    fun notifyToConnectedDevice(endPointId: String, tag: Byte, notificationParameters: JSONObject, afterCompleteTask:()->Unit? ){
        instance.sdk?.notifyToConnectedDevice(endPointId, tag, notificationParameters, afterCompleteTask)
    }

    fun isConnected(): Boolean {
        instance.sdk?.let {
            return it.isConnected()
        }
        return false
    }

    fun isDiscovering(): Boolean{
        instance.sdk?.let {
            return it.isDiscovering()
        }
        return false
    }

    fun stopDiscoveringOrAdvertising(){
        instance.sdk?.stopDiscoveringOrAdvertising()
    }

    fun disconnectFromDevice(endPointId: String){
        instance.sdk?.disconnectFromDevice(endPointId)
    }

    fun stopAll(){
        instance.sdk?.stopAll()
    }


    fun sendSetOfChunks(){

    }

    fun sendSetOfBinaryFile(){

    }

    fun performDiscoverAttempts(targetDevice: String, repetitions: Int, isLowPower: Boolean){
        instance.sdk?.performDiscoverAttempts(targetDevice, repetitions, isLowPower)
    }

    fun getRequiredPermissions(): List<String>? {
        return instance.sdk?.permissions
    }

    fun enableLocationUpdate(activity : Activity){
        instance.sdk?.enableLocationUpdate(activity)
    }

    fun disableLocationUpdate(){
         instance.sdk?.disableLocationUpdate()
    }
}