package com.sorbonne.d2d.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbEndpoint
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sorbonne.d2d.D2DListener
import com.sorbonne.d2d.tools.ConnectedDevices
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.sorbonne.d2d.D2D
import org.json.JSONObject

class D2DSDK {
    private val TAG = D2DSDK::class.simpleName

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
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
    }

    private fun checkPermissions(context: Context): Boolean = !permissions.map {
        ContextCompat.checkSelfPermission(context, it)
    }.contains(PackageManager.PERMISSION_DENIED)

    private val payloadCallback = object: PayloadCallback(){

        private var payloadById = mutableMapOf<Long, Payload>()

        override fun onPayloadReceived(endPointId: String, payload: Payload) {
            payloadById[payload.id] = payload
        }

        override fun onPayloadTransferUpdate(endPointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
            when(payloadTransferUpdate.status){
                PayloadTransferUpdate.Status.SUCCESS -> {
                    val receivedPayload = payloadById.remove(payloadTransferUpdate.payloadId)
                    when(receivedPayload?.type){
                        Payload.Type.BYTES -> {
                            viewModel.receivedChunk.value = receivedPayload
                        }
                    }
                }
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback(){
        override fun onEndpointFound(endPointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.i(TAG, "endPoint $endPointId - ${discoveredEndpointInfo.endpointName} discovered")
            connectionClient?.requestConnection(
                deviceName,
                endPointId,
                connectionLifecycleCallback)
            viewModel.foundDevice.value = JSONObject()
                .put("endPointId", endPointId)
                .put("endPointName", discoveredEndpointInfo.endpointName)
        }

        override fun onEndpointLost(endPointId: String) {
            Log.i(TAG, "endPoint $endPointId lost")
            viewModel.lostDevice.value = JSONObject()
                .put("endPointId", endPointId)
        }

    }

    private val connectionLifecycleCallback = object :ConnectionLifecycleCallback(){
        private var endDeviceName = ""

        override fun onConnectionInitiated(endPointId: String, connectionInfo: ConnectionInfo) {
            Log.i(TAG, "onConnectionInitiated from $endPointId")
            connectionClient?.acceptConnection(endPointId, payloadCallback)
            endDeviceName = connectionInfo.endpointName
        }

        override fun onConnectionResult(endPointId: String, connectionResolution: ConnectionResolution) {
            when(connectionResolution.status.statusCode){
                CommonStatusCodes.SUCCESS -> {
                    Log.i(TAG, "connected with  $endPointId - $endDeviceName")
                    viewModel.isConnected.value = true
                    viewModel.connectedDevices.value =
                        JSONObject("{\"endPointId\": \"$endPointId\", \"endPointName\": \"$endDeviceName\"}")
                    connectedDevices.addNewDevice(endPointId, endDeviceName)

                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                }
            }
        }

        override fun onDisconnected(endPointId: String) {
            viewModel.disconnectedDevices.value =
                JSONObject("{\"endPointId\": \"$endPointId\", \"endPointName\": ${connectedDevices.getDeviceParameters(endPointId)}}")
            connectedDevices.removeDevice(endPointId)

            Log.i(TAG, "disconnected from  $endPointId")

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
        viewModel.connectedDevices.observe(owner){ endPointInfo ->
            listener?.onDeviceConnected(true, endPointInfo)
        }
        viewModel.disconnectedDevices.observe(owner){ endPointInfo ->
            listener?.onDeviceConnected(false, endPointInfo)
        }
        viewModel.receivedChunk.observe(owner){ payload ->
            listener?.onReceivedChunk(payload)
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
                    Log.i(TAG, "Device Discovering")
                    viewModel.isDiscoveryActive.value = true
                    isDiscoveringAdvertising = true
                    Log.i(TAG, "Discoverer successfully started")
                }.addOnFailureListener {
                    it.printStackTrace()
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
                    Log.i(TAG, "Device Advertising")
                    viewModel.isDiscoveryActive.value = true
                    isDiscoveringAdvertising = true
                    Log.i(TAG, "Advertiser successfully started")
                }.addOnFailureListener{
                    it.printStackTrace()
                    stopDiscoveringOrAdvertising()
                }
            }
        }
    }

    fun sendPayloadByDeviceId(endPoint: String, payload: Payload){
        connectedDevices.getDeviceIdByDeviceName(endPoint)?.let{ endPointId ->
            connectionClient?.sendPayload(endPointId, payload)
        }
    }

    fun isConnected(): Boolean{
        return !connectedDevices.isEmpty()
    }

    fun isDiscovering(): Boolean{
        return isDiscoveringAdvertising
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
            viewModel.disconnectedDevices.value =
                JSONObject("{\"endPointId\": \"$endPointId\", \"endPointName\": \"${connectedDevices.getDeviceParameters(endPointId)}\"}")
            connectedDevices.removeDevice(endPointId)

            Log.i(TAG, "disconnected from  $endPointId")

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
            Log.i(TAG, "Discovery process stopped")
        }
        isDiscoveringAdvertising = false
    }

    fun stopAll(){
        isDiscoveringAdvertising = false

        connectionClient?.let {
            it.stopAllEndpoints()
            Log.i(TAG, "stopAllEndpoints")
            viewModel.isDiscoveryActive.value = false
            viewModel.isConnected.value = false

            connectedDevices.getEndPointIds().forEach{ endPointId ->
                viewModel.disconnectedDevices.value =  JSONObject("{\"endPointId\": \"$endPointId\", \"endPointParameters\": ${connectedDevices.getDeviceParameters(endPointId)}}")
                Log.i(TAG, "disconnected from  $endPointId")
            }
            connectedDevices.clear()
        }
    }

    fun setDeviceName(deviceName: String){
        this.deviceName = deviceName
    }
}