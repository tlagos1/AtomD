package com.sorbonne.d2d.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.location.*
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.D2DListener
import com.sorbonne.d2d.tools.ConnectedDevices
import com.sorbonne.d2d.tools.MessageBytes
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.*
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class D2DSDK {
    private val TAG = D2DSDK::class.simpleName

    private val viewModel = D2DViewModel()
    private var connectionClient: ConnectionsClient?=null

    private var serviceId: String?=null
    private var deviceName = "UnNamed"

    private var activityRef: WeakReference<Activity>? = null

    private val connectedDevices = ConnectedDevices()

    private var isDiscoveringAdvertising = false
    private var isAdvertiser = false
    private var requireConnectionRequest = false

    private var fusedLocationProviderClient: FusedLocationProviderClient?= null

    private var targetDevice:String ?= null
    private var totalNumberOfTask: Int = 0
    private var experimentId: Long = 0
    private var experimentName = ""
    private var endDevicesBandwidthInfo = mutableMapOf<String, Int>()
    //file_experiment
    private var isUploader = false
    private var totalNumberOfTargetDevices: Int = 0
    private var isFileExperiment: Boolean = false
    private var numberOfFileRepetitions = mutableMapOf<String, Int>()
    // Discovery_experiment
    private var isDiscoveryExperiment = false
    private var discoveryRepetitions = 0
    private var islowPower = false

    //File
    private val listOfFilesIds = mutableListOf<Long>()


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
                            val messageBytes = MessageBytes(receivedPayload.asBytes())
                            val translatedPayload = String(messageBytes.payload, StandardCharsets.UTF_8)
                            if(messageBytes.type == MessageBytes.INFO_PACKET){
                                val infoPacket = mutableMapOf<Byte, List<String>>()
                                infoPacket[messageBytes.tag] =
                                    mutableListOf(endPointId, translatedPayload)
                                viewModel.infoPacket.value = infoPacket
                            }
                            if(messageBytes.type == MessageBytes.INFO_FILE){
                                isUploader = false
                                val fileInfo = JSONObject(translatedPayload)
                                experimentId = fileInfo.getLong("experimentId")
                                experimentName = fileInfo.getString("experimentName")
                                isFileExperiment = fileInfo.getBoolean("isFileExperiment")
                                listOfFilesIds.add(fileInfo.getLong("payloadId"))
                                totalNumberOfTask = fileInfo.getInt("totalNumberOfTask")


                            }
                        }
                        Payload.Type.FILE -> {
                            Log.i(TAG, "File received")
                            CoroutineScope(Dispatchers.IO + CoroutineName("fileScope")).launch {
                                receivedPayload.asFile()?.asUri()?.let {
                                    val receivedFile = storeFile(it, "tmp_${payloadTransferUpdate.payloadId}")
                                    receivedFile?.delete()
                                }
                            }
                        }
                        Payload.Type.STREAM -> {}
                    }
                }
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
//                    if(isUploader) {
//                        Log.i(
//                            TAG,
//                            "Uploading to $deviceName: ${payloadTransferUpdate.bytesTransferred} - ${payloadTransferUpdate.totalBytes}"
//                        )
//                    } else {
//                        Log.i(
//                            TAG,
//                            "Downloading: ${payloadTransferUpdate.bytesTransferred} - ${payloadTransferUpdate.totalBytes}"
//                        )
//                    }
                    if(listOfFilesIds.contains(payloadTransferUpdate.payloadId)) {
                        val uploadingDownloadingTiming = System.nanoTime()
                        if(!numberOfFileRepetitions.containsKey(endPointId)){
                            numberOfFileRepetitions[endPointId] = 0
                        }
                        if(isFileExperiment) {
                            viewModel.fileTransferTaskValue.value = taskValue(
                                "running",
                                JSONObject()
                                    .put("experimentId", experimentId)
                                    .put("experimentName", experimentName)
                                    .put("endPointId", endPointId)
                                    .put("targetId", connectedDevices.getDeviceName(endPointId))
                                    .put("repetition", numberOfFileRepetitions[endPointId])
                                    .put("bytesTransferred", payloadTransferUpdate.bytesTransferred)
                                    .put("bytesToTransfer", payloadTransferUpdate.totalBytes)
                                    .put("timing", uploadingDownloadingTiming)
                                    .put("uploading", isUploader)
                            )
                            viewModel.taskProgress.value = ((payloadTransferUpdate.bytesTransferred.toDouble()/payloadTransferUpdate.totalBytes)*100).toInt()
                        }
                        if(payloadTransferUpdate.bytesTransferred == payloadTransferUpdate.totalBytes){
                            if(isFileExperiment) {
                                numberOfFileRepetitions[endPointId]?.let {
                                    numberOfFileRepetitions[endPointId] = it + 1
                                }
//                                Log.e(TAG, "numberOfFileRepetitions.size: ${numberOfFileRepetitions.size} - totalNumberOfTargetDevices: ${totalNumberOfTargetDevices}")
//                                if(numberOfFileRepetitions.size == totalNumberOfTargetDevices){
//                                    Log.e(TAG, "isFileExperiment B")
                                    var fileTaskCompleted = 0
                                    numberOfFileRepetitions.values.forEachIndexed { index, counter ->
                                        if(index == 0){
                                            fileTaskCompleted = counter
                                        } else {
                                            if(counter < fileTaskCompleted){
                                                fileTaskCompleted = counter
                                            }
                                        }
                                    }
                                    val experimentProgression = ((fileTaskCompleted.toDouble()/totalNumberOfTask)* 100).toInt()

                                    if(experimentProgression == 100){
                                        numberOfFileRepetitions.clear()
                                        listOfFilesIds.clear()
                                        Log.i(TAG, "File experiment finished")
                                        viewModel.fileTransferTaskValue.value = taskValue(
                                            "finished",
                                            notificationParametersForCompletedExperiment("file")
                                        )
                                        isFileExperiment = false
                                    }
                                    viewModel.experimentProgress.value = experimentProgression
//                                }
//                                return
                            }
                            listOfFilesIds.remove(payloadTransferUpdate.payloadId)
                        }
                    }
                }
                PayloadTransferUpdate.Status.CANCELED -> {
                    Log.w(TAG, "payload ${payloadTransferUpdate.payloadId} transfer cancelled")
                }
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback(){
        private val discoveredDeviceName = mutableMapOf<String,String>()
        override fun onEndpointFound(endPointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.i(TAG, "endPoint $endPointId - ${discoveredEndpointInfo.endpointName} discovered")
            if(targetDevice != null){
                if(isDiscoveryExperiment){
                    val stateTiming = System.nanoTime()
                    viewModel.discoveryTaskValue.value = taskValue(
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
                                val stateTiming = System.nanoTime()
                                viewModel.discoveryTaskValue.value = taskValue(
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
                discoveredDeviceName[endPointId] = discoveredEndpointInfo.endpointName
                viewModel.foundDevice.value = JSONObject()
                    .put("endPointId", endPointId)
                    .put("endPointName", discoveredDeviceName[endPointId])

                if (!requireConnectionRequest) {
                    connectionClient?.requestConnection(
                        deviceName,
                        endPointId,
                        connectionLifecycleCallback
                    )
                }
            }
        }

        override fun onEndpointLost(endPointId: String) {
            Log.i(TAG, "endPoint $endPointId lost")
            discoveredDeviceName.remove(endPointId)?.let {
                viewModel.lostDevice.value = JSONObject()
                    .put("endPointId", endPointId)
                    .put("endPointName", it)
            }
        }

    }

    private val connectionLifecycleCallback = object :ConnectionLifecycleCallback(){
        private var endDeviceName = mutableMapOf<String, String>()

        override fun onBandwidthChanged(endPointId: String, bandwidthInfo: BandwidthInfo) {
            super.onBandwidthChanged(endPointId, bandwidthInfo)
            endDevicesBandwidthInfo[endPointId] = bandwidthInfo.quality
            viewModel.bandwidthInfo.value = JSONObject()
                .put("endPointId", endPointId)
                .put("bandwidthInfo", bandwidthInfo.quality)

            Log.e(TAG, "bandwidth: ${bandwidthInfo.quality}")
        }

        override fun onConnectionInitiated(endPointId: String, connectionInfo: ConnectionInfo) {
            isAdvertiser = connectionInfo.isIncomingConnection
            endDeviceName[endPointId] = connectionInfo.endpointName
            if(isDiscoveryExperiment){
                val stateTiming = System.nanoTime()
                viewModel.discoveryTaskValue.value = taskValue(
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
                        val stateTiming = System.nanoTime()
                        viewModel.discoveryTaskValue.value = taskValue(
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
                it.addOnFailureListener {
                    connectionClient?.disconnectFromEndpoint(endPointId)
                }
            }
        }

        override fun onConnectionResult(endPointId: String, connectionResolution: ConnectionResolution) {
            when(connectionResolution.status.statusCode){
                CommonStatusCodes.SUCCESS -> {
                    if(connectedDevices.isEmpty()) {
                        viewModel.isConnected.value = true
                    }
                    if(discoveryRepetitions > 0){
                        if(isDiscoveryExperiment){
                            val stateTiming = System.nanoTime()
                            viewModel.discoveryTaskValue.value = taskValue(
                                "running",
                                JSONObject()
                                    .put("targetId", connectedDevices.getDeviceName(endPointId))
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
                            endDeviceName.remove(endPointId)?.let{ mDeviceName ->
                                targetDevice = null
                                viewModel.connectedDevices.value =
                                    JSONObject()
                                        .put("endPointId", endPointId)
                                        .put("endPointName", mDeviceName)
                                        .put("bandwidthQuality", BandwidthInfo.Quality.UNKNOWN)
                                viewModel.discoveryTaskValue.value = taskValue(
                                    "finished",
                                    notificationParametersForCompletedExperiment("discovery")
                                )
                            }
                        }
                        else{
                            stopDiscoveringOrAdvertising()
                            disconnectFromDevice(endPointId)
                            Log.e(TAG, "discoveryRepetitions: $discoveryRepetitions")
                            startDiscovery(Strategy.P2P_POINT_TO_POINT, false)
                        }
                    }
                    if(discoveryRepetitions == 0){
                        endDeviceName.remove(endPointId)?.let { mDeviceName ->
                            Log.i(TAG, "connected with  $endPointId - $mDeviceName")
                            viewModel.connectedDevices.value =
                                JSONObject()
                                    .put("endPointId", endPointId)
                                    .put("endPointName", mDeviceName)
                                    .put("bandwidthQuality", BandwidthInfo.Quality.UNKNOWN)
                            connectedDevices.addNewDevice(endPointId, mDeviceName)
                        }
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    endDeviceName.remove(endPointId)
                    endDevicesBandwidthInfo.remove(endPointId)
                }
            }
        }

        override fun onDisconnected(endPointId: String) {
            numberOfFileRepetitions.clear()
            listOfFilesIds.clear()

            viewModel.disconnectedDevices.value =
                JSONObject("{\"endPointId\": \"$endPointId\", \"endPointParameters\": ${connectedDevices.getDeviceParameters(endPointId)}}")
            connectedDevices.removeDevice(endPointId)

            Log.i(TAG, "disconnected from  $endPointId")

            if(connectedDevices.isEmpty()){
                viewModel.isConnected.value = false
            }
            endDevicesBandwidthInfo.remove(endPointId)
        }
    }

    fun launchSDK(owner: LifecycleOwner, listener: D2DListener?, activity: Activity){

        connectionClient = Nearby.getConnectionsClient(activity)

        serviceId = activity.packageName

        activityRef = WeakReference(activity)

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
        viewModel.bandwidthInfo.observe(owner){ endPointInfo ->
            listener?.onBandwidthQuality(endPointInfo)
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
        viewModel.fileTransferTaskValue.observe(owner){ taskValue ->
            listener?.onReceivedTaskResul(D2D.ParameterTag.FILE, taskValue)
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

    fun startDiscovery(strategy: Strategy, lowPower: Boolean, automaticRequest: Boolean = true){
        if(isDiscoveringAdvertising){
            stopDiscoveringOrAdvertising()
            return
        }

        requireConnectionRequest = !automaticRequest

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
                        val stateTiming = System.nanoTime()
                        islowPower = lowPower

                        viewModel.discoveryTaskValue.value = taskValue(
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

    fun requestConnectionToEndPoint(endPointId: String){
        connectionClient?.requestConnection(
            deviceName,
            endPointId,
            connectionLifecycleCallback
        )
    }

    fun notifyToConnectedDevice(endPointId: String, tag: Byte, notificationParameters: JSONObject, afterCompleteTask:(()->Any?)?){
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
                if (afterCompleteTask != null) {
                    afterCompleteTask()
                }
            }
        }
    }

    fun notifyToSetOfConnectedDevices(setOfDevices: List<String>, tag: Byte, messageType: Byte, notificationParameters: JSONObject, afterCompleteTask:(()->Any?)?){
        val messageBytes = MessageBytes()
        messageBytes.buildRegularPacket(
            messageType,
            tag,
            notificationParameters.toString().toByteArray(StandardCharsets.UTF_8)
        )
        connectionClient?.sendPayload(setOfDevices, Payload.fromBytes(messageBytes.buffer))?.let { it ->
            it.addOnSuccessListener {
                Log.i(TAG, "message successfully sent to $setOfDevices")
            }.addOnFailureListener { e ->
                e.printStackTrace()
            }.addOnCompleteListener {
                if (afterCompleteTask != null) {
                    afterCompleteTask()
                }
            }
        }
    }

    fun notifyToAllConnectedDevices(tag: Byte, messageType: Byte, notificationParameters: JSONObject, afterCompleteTask:(()->Any?)?){
        val messageBytes = MessageBytes()
        messageBytes.buildRegularPacket(
            messageType,
            tag,
            notificationParameters.toString().toByteArray(StandardCharsets.UTF_8)
        )
        if(!connectedDevices.isEmpty()){
            connectionClient?.let {
                it.sendPayload(connectedDevices.getEndPointIds().toList(), Payload.fromBytes(messageBytes.buffer))
                    .addOnSuccessListener {
                        Log.i(TAG, "message successfully sent to ${connectedDevices.getEndPointIds()}")
                    }.addOnFailureListener { e ->
                        e.printStackTrace()
                    }.addOnCompleteListener {
                        if (afterCompleteTask != null) {
                            afterCompleteTask()
                        }
                    }
            }
        }
    }

    fun sendFileToConnectedDevices(tag: Byte, file: File, afterCompleteTask:(()->Any?)?){
        isUploader = true
        if(!connectedDevices.isEmpty()) {
            val payloadFile = Payload.fromFile(file)

            listOfFilesIds.add(payloadFile.id)
            notifyToAllConnectedDevices(
                tag,
                MessageBytes.INFO_FILE,
                JSONObject().put("payloadId", payloadFile.id)
            ){
                connectionClient?.let {
                    it.sendPayload(connectedDevices.getEndPointIds().toList(), payloadFile)
                        .addOnSuccessListener {
                            Log.i(TAG, "file successfully sent to ${connectedDevices.getEndPointIds()}")
                        }.addOnFailureListener { e ->
                            Log.e(TAG,"sendPayload Fail")
                            e.printStackTrace()
                        }.addOnCompleteListener {
                            if (afterCompleteTask != null) {
                                afterCompleteTask()
                            }
                        }
                }
            }
        }
    }

    private fun sendFileToSetOfDevices(setOfDevices: List<String>, tag: Byte, file: File, fileInfo: JSONObject, afterCompleteTask:(()->Any?)?){

        val payload = Payload.fromFile(file)
        listOfFilesIds.add(payload.id)

        totalNumberOfTargetDevices = setOfDevices.size
        notifyToSetOfConnectedDevices(setOfDevices, tag, MessageBytes.INFO_FILE,fileInfo.put("payloadId", payload.id)) {
            connectionClient?.let {
                it.sendPayload(setOfDevices, payload)
                    .addOnSuccessListener {
                        Log.i(TAG, "file successfully sent to ${connectedDevices.getEndPointIds()}")
                    }.addOnFailureListener { e ->
                        Log.e(TAG,"sendPayload Fail")
                        e.printStackTrace()
                    }.addOnCompleteListener {
                        if (afterCompleteTask != null) {
                            afterCompleteTask()
                        }
                    }
            }
        }

    }

    fun cancelFileTransferIfAny(afterCompleteTask: (()->Any?)? = null){
        connectionClient?.let {
            listOfFilesIds.forEach { payloadId ->
                it.cancelPayload(payloadId)
                    .addOnCompleteListener {
                        if (afterCompleteTask != null) {
                            afterCompleteTask()
                        }
                    }
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

    fun performFileExperiment(targetDevices: List<String>, tag: Byte, experimentName: String, repetitions: Int, file: File){
        this.experimentId = System.currentTimeMillis()
        this.experimentName = experimentName
        this.totalNumberOfTask = repetitions
        this.isFileExperiment = true

        this.isUploader = true
        this.listOfFilesIds.clear()
        this.numberOfFileRepetitions.clear()

        for (i in  0 until repetitions){

            sendFileToSetOfDevices(targetDevices, tag, file,
                    JSONObject()
                    .put("experimentId", experimentId)
                    .put("experimentName", experimentName)
                    .put("isFileExperiment", true)
                    .put("totalNumberOfTask", repetitions)
            ){
                Log.i(TAG, "performFileExperiment repetition: $i")
            }
        }
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
            endDevicesBandwidthInfo.remove(endPointId)
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
            isFileExperiment = false
            connectedDevices.getEndPointIds().forEach{ endPointId ->
                viewModel.disconnectedDevices.value =  JSONObject("{\"endPointId\": \"$endPointId\", \"endPointParameters\": ${connectedDevices.getDeviceParameters(endPointId)}}")
                Log.i(TAG, "disconnected from  $endPointId")
                endDevicesBandwidthInfo.remove(endPointId)
            }
            connectedDevices.clear()

            numberOfFileRepetitions.clear()
            listOfFilesIds.clear()
        }
    }

    fun setDeviceName(deviceName: String){
        this.deviceName = deviceName
    }

    private fun storeFile(uri:Uri, fileName: String): File?{
        activityRef?.get()?.let { activity ->
            try {
                val inputStream = activity.contentResolver.openInputStream(uri)
                copyStream(inputStream!!, FileOutputStream(File(activity.cacheDir, fileName)))
                inputStream.close()
            } catch (e: IOException){
                e.printStackTrace()
            } finally {
                activity.contentResolver.delete(uri, null, null)
            }
            return File(activity.cacheDir, fileName)
        }
        return null
    }

    private fun copyStream(mIn: InputStream, mOut: OutputStream){
        try {
            mIn.copyTo(mOut)
        } finally {
            mIn.close()
            mOut.close()
        }
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


    private fun taskValue(experimentState: String, experimentValueParameters: JSONObject): JSONObject {
        return JSONObject()
            .put("experimentState", experimentState)
            .put("experimentValueParameters", experimentValueParameters)
    }
}