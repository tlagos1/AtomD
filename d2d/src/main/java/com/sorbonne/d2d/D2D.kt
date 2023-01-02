package com.sorbonne.d2d

import android.app.Activity
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.nearby.connection.Payload
import com.sorbonne.d2d.internal.D2DSDK
import com.google.android.gms.nearby.connection.Strategy
import org.json.JSONObject
import java.io.File
import java.util.Objects

object D2D {
    private var sdk: D2DSDK ?= null
    private val instance = this

    enum class ParameterTag{
        FILE,
        DISCOVERY
    }

    class Builder(private val owner: LifecycleOwner, val deviceName: String, val activity: Activity){
        private var listener: D2DListener?= null

        fun setListener(listener: D2DListener): Builder{
            this.listener = listener
            return this
        }

        fun build(): D2D{
            instance.sdk = D2DSDK()
            instance.sdk?.launchSDK(owner, listener, activity)
            instance.sdk?.setDeviceName(deviceName)
            return instance
        }
    }

    fun startDiscovery(strategy: Strategy, lowPower: Boolean, automaticRequest: Boolean = true){
        instance.sdk?.startDiscovery(strategy, lowPower, automaticRequest = automaticRequest)
    }

    fun startAdvertising(deviceName: String, strategy: Strategy, lowPower: Boolean, connectionType: Int){
        instance.sdk?.startAdvertising(deviceName, strategy, lowPower, connectionType)
    }

    fun requestConnectionToEndPoint(endPointId: String){
        instance.sdk?.requestConnectionToEndPoint(endPointId)
    }

    fun notifyToConnectedDevice(endPointId: String, tag: Byte, notificationParameters: JSONObject, afterCompleteTask:(()->Any?)? = null){
        instance.sdk?.notifyToConnectedDevice(endPointId, tag, notificationParameters, afterCompleteTask)
    }

    fun notifyToSetOfConnectedDevices(setOfDevices: List<String>, tag: Byte, messageType: Byte, notificationParameters: JSONObject, afterCompleteTask:(()->Any?)?){
        instance.sdk?.notifyToSetOfConnectedDevices(setOfDevices, tag, messageType, notificationParameters, afterCompleteTask)
    }

    fun notifyToAllConnectedDevices(tag: Byte, messageType: Byte, notificationParameters: JSONObject, afterCompleteTask:(()->Any?)? = null){
        instance.sdk?.notifyToAllConnectedDevices(tag, messageType, notificationParameters, afterCompleteTask)
    }

    fun sendFileToConnectedDevices(tag: Byte,file: File, afterCompleteTask: (()->Any?)? = null){
        instance.sdk?.sendFileToConnectedDevices(tag, file, afterCompleteTask)
    }

    fun cancelFileTransferIfAny(afterCompleteTask: (()->Any?)? = null){
        instance.sdk?.cancelFileTransferIfAny(afterCompleteTask)
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


    fun performFileExperiment(targetDevices: List<String>, tag: Byte, experimentName: String, repetitions: Int, file: File){
        instance.sdk?.performFileExperiment(targetDevices, tag, experimentName, repetitions, file)
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