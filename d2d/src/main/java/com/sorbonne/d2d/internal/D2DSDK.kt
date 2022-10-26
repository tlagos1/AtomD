package com.sorbonne.d2d.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sorbonne.d2d.D2DListener
import com.sorbonne.d2d.tools.ConnectedDevices
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.location.*
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.tools.MessageBytes
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class D2DSDK {
    private val TAG = D2DSDK::class.simpleName

    private val viewModel = D2DViewModel()
    private var connectionClient: ConnectionsClient?=null

    private var serviceId: String?=null
    private var deviceName = "UnNamed"

    private val connectedDevices = ConnectedDevices()

    private var isDiscoveringAdvertising = false

    private var fusedLocationProviderClient: FusedLocationProviderClient?= null

    private var targetDevice:String ?= null
    private var totalNumberOfTask: Int = 0
    // Discovery_experiment
    private var isDiscoveryExperiment = false
    private var discoveryRepetitions = 0
    private var islowPower = false




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
            Log.i(TAG,"onPayloadReceived")
            payloadById[payload.id] = payload
        }

        override fun onPayloadTransferUpdate(endPointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
            when(payloadTransferUpdate.status){
                PayloadTransferUpdate.Status.SUCCESS ->{
                    val receivedPayload = payloadById.remove(payloadTransferUpdate.payloadId)
                    when(receivedPayload?.type){
                        Payload.Type.BYTES -> {
                            val messageBytes = MessageBytes(receivedPayload.asBytes())
                            if(messageBytes.type == MessageBytes.INFO_PACKET){
                                val infoPacket = mutableMapOf<Byte, String>()
                                infoPacket[messageBytes.tag] = String(messageBytes.payload, StandardCharsets.UTF_8)
                                viewModel.infoPacket.value = infoPacket
                            }
                        }
                        Payload.Type.FILE -> {

                        }
                    }
                }
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback(){
        override fun onEndpointFound(endPointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.i(TAG, "endPoint $endPointId - ${discoveredEndpointInfo.endpointName} discovered")
            if(targetDevice != null){
                if(isDiscoveryExperiment){
                    val stateTiming = System.currentTimeMillis()
                    viewModel.discoveryTaskValue.value = discoveryTaskValue(
                        "running",
                        JSONObject()
                            .put("totalNumberOfAttempts", totalNumberOfTask)
                            .put("isLowPower", islowPower)
                            .put("attempt", discoveryRepetitions)
                            .put("state", "onEndpointFound")
                            .put("stateTiming", stateTiming)
                    )
                }
                if(discoveredEndpointInfo.endpointName == targetDevice){
                    connectionClient?.requestConnection(
                        deviceName,
                        endPointId,
                        connectionLifecycleCallback
                    )?.let {
                        it.addOnSuccessListener {
                            if(isDiscoveryExperiment){
                                val stateTiming = System.currentTimeMillis()
                                viewModel.discoveryTaskValue.value = discoveryTaskValue(
                                    "running",
                                    JSONObject()
                                        .put("totalNumberOfAttempts", totalNumberOfTask)
                                        .put("isLowPower", islowPower)
                                        .put("attempt", discoveryRepetitions)
                                        .put("state", "requestConnection")
                                        .put("stateTiming", stateTiming)
                                )
                            }
                        }
                    }
                }
            } else {
                connectionClient?.requestConnection(
                    deviceName,
                    endPointId,
                    connectionLifecycleCallback
                )
                viewModel.foundDevice.value = JSONObject()
                    .put("endPointId", endPointId)
                    .put("endPointName", discoveredEndpointInfo.endpointName)
            }
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
            if(isDiscoveryExperiment){
                val stateTiming = System.currentTimeMillis()
                viewModel.discoveryTaskValue.value = discoveryTaskValue(
                    "running",
                    JSONObject()
                        .put("totalNumberOfAttempts", totalNumberOfTask)
                        .put("isLowPower", islowPower)
                        .put("attempt", discoveryRepetitions)
                        .put("state", "onConnectionInitiated")
                        .put("stateTiming", stateTiming)
                )
            }
            connectionClient?.acceptConnection(endPointId, payloadCallback)?.let {
                it.addOnSuccessListener {
                    if(isDiscoveryExperiment){
                        val stateTiming = System.currentTimeMillis()
                        viewModel.discoveryTaskValue.value = discoveryTaskValue(
                            "running",
                            JSONObject()
                                .put("totalNumberOfAttempts", totalNumberOfTask)
                                .put("isLowPower", islowPower)
                                .put("attempt", discoveryRepetitions)
                                .put("state", "acceptConnection")
                                .put("stateTiming", stateTiming)
                        )
                    }
                }
            }
            endDeviceName = connectionInfo.endpointName
        }

        override fun onConnectionResult(endPointId: String, connectionResolution: ConnectionResolution) {
            when(connectionResolution.status.statusCode){
                CommonStatusCodes.SUCCESS -> {
                    viewModel.isConnected.value = true
                    if(discoveryRepetitions > 0){
                        if(isDiscoveryExperiment){
                            val stateTiming = System.currentTimeMillis()
                            viewModel.discoveryTaskValue.value = discoveryTaskValue(
                                "running",
                                JSONObject()
                                    .put("totalNumberOfAttempts", totalNumberOfTask)
                                    .put("isLowPower", islowPower)
                                    .put("attempt", discoveryRepetitions)
                                    .put("state", "onConnectionResult")
                                    .put("stateTiming", stateTiming)
                            )
                        }
                        discoveryRepetitions -= 1
                        viewModel.experimentProgress.value = (((totalNumberOfTask-discoveryRepetitions).toDouble()/totalNumberOfTask)*100).toInt()
                        if(discoveryRepetitions == 0){
                            targetDevice = null
                            viewModel.connectedDevices.value =
                                JSONObject("{\"endPointId\": \"$endPointId\", \"endPointName\": \"$endDeviceName\"}")
                            viewModel.discoveryTaskValue.value = discoveryTaskValue(
                                "finished",
                                notificationParametersForCompletedExperiment("discovery")
                            )
                        }
                        else{
                            stopDiscoveringOrAdvertising()
                            disconnectFromDevice(endPointId)
                            Log.e(TAG, "discoveryRepetitions: $discoveryRepetitions")
                            startDiscovery(Strategy.P2P_POINT_TO_POINT, false)
                        }
                    }
                    if(discoveryRepetitions == 0){
                        Log.i(TAG, "connected with  $endPointId - $endDeviceName")
                        viewModel.connectedDevices.value =
                            JSONObject("{\"endPointId\": \"$endPointId\", \"endPointName\": \"$endDeviceName\"}")
                        connectedDevices.addNewDevice(endPointId, endDeviceName)
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                }
            }
        }

        override fun onDisconnected(endPointId: String) {
            viewModel.disconnectedDevices.value =
                JSONObject("{\"endPointId\": \"$endPointId\", \"endPointParameters\": ${connectedDevices.getDeviceParameters(endPointId)}}")
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
        viewModel.experimentProgress.observe(owner){ experimentProgress ->
            listener?.onExperimentProgress(true, experimentProgress)
        }
        viewModel.taskProgress.observe(owner){ taskProgress ->
            listener?.onExperimentProgress(false, taskProgress)
        }
        viewModel.discoveryTaskValue.observe(owner){ taskValue ->
            listener?.onReceivedTaskResul(D2D.ParameterTag.DISCOVERY, taskValue)
        }
        viewModel.infoPacket.observe(owner){ infoPacket ->
            infoPacket.entries.forEach{ (entryKey, entryValue) ->
                listener?.onInfoPacketReceived(entryKey, entryValue)
            }
        }
        viewModel.lastLocation.observe(owner){ lastLocation ->
            listener?.onLastLocation(lastLocation)
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
                    if(isDiscoveryExperiment){
                        val stateTiming = System.currentTimeMillis()
                        islowPower = lowPower

                        viewModel.discoveryTaskValue.value = discoveryTaskValue(
                            "running",
                            JSONObject()
                                .put("totalNumberOfAttempts", totalNumberOfTask)
                                .put("isLowPower", islowPower)
                                .put("attempt", discoveryRepetitions)
                                .put("state", "startDiscovery")
                                .put("stateTiming", stateTiming)
                        )
                    }
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

    fun notifyToConnectedDevice(endPointId: String, tag: Byte, notificationParameters: JSONObject, afterCompleteTask:()->Unit?){
        val messageBytes = MessageBytes()

        messageBytes.buildRegularPacket(
            MessageBytes.INFO_PACKET,
            tag,
            notificationParameters.toString().toByteArray(StandardCharsets.UTF_8)
        )

        connectionClient?.sendPayload(endPointId, Payload.fromBytes(messageBytes.buffer))?.let { it ->
            it.addOnSuccessListener {
                Log.i(TAG, "message successfully sent to $endPointId")
            }.addOnFailureListener { e ->
                e.printStackTrace()
            }.addOnCompleteListener {
                afterCompleteTask()
            }
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

    fun performDiscoverAttempts(targetDevice: String,repetitions: Int, lowPower: Boolean){
        this.isDiscoveryExperiment = true
        this.targetDevice = targetDevice
        this.totalNumberOfTask = repetitions
        this.discoveryRepetitions = repetitions
        connectedDevices.getEndPointIds().forEach{ endPointId ->
            connectionClient?.disconnectFromEndpoint(endPointId)
        }
        connectedDevices.clear()
        startDiscovery(Strategy.P2P_POINT_TO_POINT, lowPower)
    }

    fun disconnectFromDevice(endPointId: String){
        connectionClient?.let {
            it.disconnectFromEndpoint(endPointId)
            viewModel.disconnectedDevices.value =
                JSONObject("{\"endPointId\": \"$endPointId\", \"endPointParameters\": ${connectedDevices.getDeviceParameters(endPointId)}}")

            if(!connectedDevices.isEmpty()){
                connectedDevices.removeDevice(endPointId)
            }

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
            isDiscoveryExperiment = false
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

    private val locationCallback: LocationCallback = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            viewModel.lastLocation.value = locationResult.lastLocation
            Log.i(TAG, "lastLocation: ${locationResult.lastLocation}")
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            super.onLocationAvailability(locationAvailability)
            if(locationAvailability.isLocationAvailable){
                Log.i(TAG, "location is available")
            } else {
                Log.i(TAG, "location is not available")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun enableLocationUpdate(activity :Activity){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        fusedLocationProviderClient?.lastLocation?.addOnSuccessListener { location ->
                location?.let {
                    viewModel.lastLocation.value = location
                    Log.i(TAG, "lastLocation: $location")
                }
            }
        fusedLocationProviderClient?.requestLocationUpdates(
            createLocationRequest(Priority.PRIORITY_HIGH_ACCURACY, 60),
            locationCallback,
            Looper.myLooper()
        )
    }

    fun disableLocationUpdate(){
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        fusedLocationProviderClient = null
    }

    private fun createLocationRequest(priority: Int, interval: Int): LocationRequest {
        val locationRequest = LocationRequest.Builder(
            priority,
            interval.toDuration(DurationUnit.SECONDS).inWholeMilliseconds
        )
        .build()

        return locationRequest
    }

    private fun notificationParametersForCompletedExperiment(experiment: String): JSONObject{
        val notificationParameters = JSONObject()
        notificationParameters.put("experimentType", experiment)
        notificationParameters.put("experimentMessageType", "finished")
        notificationParameters.put("target", "local")
        return notificationParameters
    }

    private fun discoveryTaskValue(experimentState: String, experimentValueParameters: JSONObject): JSONObject {
        return JSONObject()
            .put("experimentState", experimentState)
            .put("experimentValueParameters", experimentValueParameters)
    }
}