package com.sorbonne.d2d.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbEndpoint
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sorbonne.d2d.D2DListener
import com.sorbonne.d2d.tools.ConnectedDevices
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.json.JSONObject

class D2DSDK {

    private val viewModel = D2DViewModel()
    private var connectionClient: ConnectionsClient?=null

    private var serviceId: String?=null
    private var deviceName = "UnNamed"

    private val connectedDevices = ConnectedDevices()

    private var isDiscoveringAdvertising = false

    val permissions: List<String> by lazy {
        buildList {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
    }

    private fun checkPermissions(context: Context): Boolean = !permissions.map {
        ContextCompat.checkSelfPermission(context, it)
    }.contains(PackageManager.PERMISSION_DENIED)

    private val payloadCallback = object: PayloadCallback(){
        override fun onPayloadReceived(endPointId: String, payload: Payload) {
            TODO("Not yet implemented")
        }

        override fun onPayloadTransferUpdate(endPointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
            TODO("Not yet implemented")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback(){
        override fun onEndpointFound(endPointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            connectionClient?.requestConnection(
                deviceName,
                endPointId,
                connectionLifecycleCallback)
            viewModel.foundDevice.value = JSONObject()
                .put("endPointId", endPointId)
                .put("endPointName", discoveredEndpointInfo.endpointName)
        }

        override fun onEndpointLost(endPointId: String) {
            viewModel.lostDevice.value = JSONObject()
                .put("endPointId", endPointId)
        }

    }

    private val connectionLifecycleCallback = object :ConnectionLifecycleCallback(){
        private var endDeviceName = ""

        override fun onConnectionInitiated(endPointId: String, connectionInfo: ConnectionInfo) {
            connectionClient?.acceptConnection(endPointId, payloadCallback)
            endDeviceName = connectionInfo.endpointName
        }

        override fun onConnectionResult(endPointId: String, connectionResolution: ConnectionResolution) {
            when(connectionResolution.status.statusCode){
                CommonStatusCodes.SUCCESS -> {
                    viewModel.isConnected.value = true
                    connectedDevices.addNewDevice(endPointId, endDeviceName)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                }
            }
        }

        override fun onDisconnected(endPointId: String) {
            connectedDevices.removeDevice(endPointId)
            if(connectedDevices.isEmpty()){
                viewModel.isConnected.value = false
            }
        }
    }

    fun launchSDK(owner: LifecycleOwner, listener: D2DListener?, context: Context){

        connectionClient = Nearby.getConnectionsClient(context)

        serviceId = context.packageName

        viewModel.isDiscoveryActive.observe(owner){ active ->
            listener?.onDiscoveryChange(active)
        }
        viewModel.isConnected.observe(owner){ active ->
            listener?.onConnectivityChange(active)
        }
        viewModel.foundDevice.observe(owner){ endPointInfo ->
            listener?.onEndPointsDiscovered(true, endPointInfo)
        }
        viewModel.lostDevice.observe(owner){ endPointInfo ->
            listener?.onEndPointsDiscovered(false, endPointInfo)
        }
    }

    fun startDiscovery(strategy: Strategy, lowPower: Boolean){
        if(isDiscoveringAdvertising){
            stopDiscoveringOrAdvertising()
            return
        }
        val discoveryOption = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .setLowPower(lowPower)
            .build()
        serviceId?.let { serviceId ->
            connectionClient?.let { connectionClient ->
                connectionClient.startDiscovery(
                    serviceId,
                    endpointDiscoveryCallback,
                    discoveryOption
                ).addOnSuccessListener {
                    viewModel.isDiscoveryActive.value = true
                    isDiscoveringAdvertising = true
                }.addOnFailureListener {
                    stopDiscoveringOrAdvertising()
                }
            }
        }
    }

    fun startAdvertising(deviceName: String, strategy: Strategy, lowPower: Boolean, connectionType: Int){
        if(isDiscoveringAdvertising){
            stopDiscoveringOrAdvertising()
            return
        }

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .setLowPower(lowPower)
            .setConnectionType(connectionType)
            .build()
        serviceId?.let { serviceId ->
            connectionClient?.let { connectionClient ->
                connectionClient.startAdvertising(
                    deviceName,
                    serviceId,
                    connectionLifecycleCallback,
                    advertisingOptions
                ).addOnSuccessListener {
                    viewModel.isDiscoveryActive.value = true
                    isDiscoveringAdvertising = true
                }.addOnFailureListener{
                    stopDiscoveringOrAdvertising()
                }
            }
        }
    }

    fun sendSetOfChunks(){

    }

    fun sendSetOfBinaryFile(){

    }

    fun performDiscoverAttempts(){

    }

    fun disconnectFromDevice(endPointId: String){
        connectionClient?.let {
            it.disconnectFromEndpoint(endPointId)
            connectedDevices.removeDevice(endPointId)

            if(connectedDevices.isEmpty()){
                viewModel.isConnected.value = false
            }
        }
    }

    fun stopDiscoveringOrAdvertising(){
        connectionClient?.let {
            it.stopDiscovery()
            it.stopAdvertising()
            viewModel.isDiscoveryActive.value = false
        }
        isDiscoveringAdvertising = false
    }

    fun stopAll(){
        connectionClient?.let {
            it.stopAllEndpoints()
            viewModel.isDiscoveryActive.value = false
            viewModel.isConnected.value = false
        }
    }

    fun setDeviceName(deviceName: String){
        this.deviceName = deviceName
    }
}