package com.sorbonne.d2d

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.sorbonne.d2d.internal.D2DSDK
import com.google.android.gms.nearby.connection.Strategy

object D2D {
    private var sdk: D2DSDK ?= null
    private val instance = this

    enum class ParameterTag{
        INIT_DISCOVERY,
        END_DISCOVERY,
        INIT_CONNECTIVITY,
        END_CONNECTIVITY
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

    fun sendSetOfChunks(){

    }

    fun sendSetOfBinaryFile(){

    }

    fun performDiscoverAttempts(){

    }

    fun getRequiredPermissions(): List<String>? {
        return instance.sdk?.permissions
    }
}