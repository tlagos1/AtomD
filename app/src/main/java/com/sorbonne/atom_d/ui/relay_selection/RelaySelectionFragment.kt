package com.sorbonne.atom_d.ui.relay_selection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.sorbonne.atom_d.MainActivity
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.single_column.SimpleSingleColumnAdapter
import com.sorbonne.atom_d.guard
import com.sorbonne.atom_d.services.Socket
import com.sorbonne.atom_d.services.socket.SocketListener
import com.sorbonne.atom_d.tools.*
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.D2DListener
import com.sorbonne.d2d.tools.MessageBytes
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetSocketAddress

enum class RelaySelectionInfoMessage{
    SOCKET, PEER, METRICS
}

class RelaySelectionFragment : Fragment(), SocketListener, D2DListener {
    companion object {
        fun newInstance() = RelaySelectionFragment()
    }

    private val TAG = RelaySelectionFragment::class.simpleName

    private lateinit var viewModel: RelaySelectionViewModel
    private val globalStrategy = Strategy.P2P_CLUSTER

    private var relaySelectionPlayersConnected = mutableListOf<JSONObject>()
    private var adapter = SimpleSingleColumnAdapter(SingleColumnViewHolder.SingleColumnType.RelaySelection, relaySelectionPlayersConnected)

    private val scoreComputingScope = CoroutineScope(Dispatchers.IO + CoroutineName("ScoreComputingScope"))
    private val mutex = Mutex()
    private var throughputComputingJob:Job? = null
    private var isThroughputComputingRunning = false
    private var batteryManagerComputingJob:Job? = null

    private val scoreComputation = ScoreComputation()

    private var isDiscoverer = false
    private var targetRelay:JSONObject? = null

    private var startRelaySelection: Button ?=null
    private var stopRelaySelection: Button ?=null
    private var selectedRole: MaterialButtonToggleGroup ?= null
    private var bottomNavigationView: BottomNavigationView ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, RelaySelectionViewModel.Factory(context))[RelaySelectionViewModel::class.java]
        viewModel.instance = (context as? MainActivity).guard { return }.d2d
        viewModel.deviceId = (context as? MainActivity).guard { return }.androidId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_relay_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.relay_selection_connected_devices),
            adapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()

        val deviceId : TextView = view.findViewById(R.id.relay_selection_device_id)
        startRelaySelection = view.findViewById(R.id.relay_selection_start)
        stopRelaySelection = view.findViewById(R.id.relay_selection_stop)
        selectedRole = view.findViewById(R.id.relay_selection_role)
        bottomNavigationView = requireActivity().findViewById(R.id.bottom_nav)
        val performSelection : Button = view.findViewById(R.id.relay_selection_perform_selection)

        deviceId.text = viewModel.deviceId

        startRelaySelection?.setOnClickListener { start ->
            viewModel.socketService = (context as? MainActivity).guard {}.socketService

            viewModel.socketService?.setListener(viewLifecycleOwner,this)
            if (selectedRole?.checkedButtonId == R.id.relay_selection_role_disc || selectedRole?.checkedButtonId == R.id.relay_selection_role_adv) {

                if(selectedRole?.checkedButtonId == R.id.relay_selection_role_disc){
                    isDiscoverer = true
                    viewModel.instance?.startDiscovery(
                        globalStrategy,
                        false,
                    )
                } else if(selectedRole?.checkedButtonId == R.id.relay_selection_role_adv) {
                    if(!checkForInternet(requireContext())){
                        Toast.makeText(requireContext(), "Internet access required", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    isDiscoverer = false
                    MyAlertDialog.showDialog(
                        parentFragmentManager,
                        TAG!!,
                        MyAlertDialog.MessageType.ALERT_INPUT_TEXT,
                        R.drawable.ic_alert_dialog_info_24,
                        "Server Ip",
                        null,
                        R.layout.dialog_text_input,
                        filter = "Ipv4",
                        option1 = fun (ipAddressEditText){
                            ipAddressEditText as EditText
                            val ipV4 = ipAddressEditText.text.toString()
                            viewModel.deviceId?.let { deviceId ->
                                viewModel.socketService?.initSocketConnection(
                                    InetSocketAddress(ipV4,33235),
                                    deviceId
                                )
                            }
                            viewModel.instance?.startAdvertising(
                                viewModel.deviceId!!,
                                globalStrategy,
                                false,
                                ConnectionType.NON_DISRUPTIVE
                            )
                        }
                    )
                }

                if(start.isEnabled){
                    start.isEnabled = false
                    stopRelaySelection?.isEnabled = true
                }

                bottomNavigationView?.menu?.apply {
                    findItem(R.id.navigation_dashboard).isEnabled = false
                    findItem(R.id.navigation_experiment).isEnabled = false
                }

                for (index in 0 until selectedRole!!.childCount){
                    val singleRole = selectedRole!!.getChildAt(index) as MaterialButton
                    singleRole.isEnabled = false
                }
            }
        }

        stopRelaySelection?.setOnClickListener { stop ->
            if(stop.isEnabled){
                stop.isEnabled = false
                startRelaySelection?.isEnabled = true
            }

            bottomNavigationView?.menu?.apply {
                findItem(R.id.navigation_dashboard).isEnabled = true
                findItem(R.id.navigation_experiment).isEnabled = true
            }

            for (index in 0 until selectedRole!!.childCount){
                val singleRole = selectedRole!!.getChildAt(index) as MaterialButton
                singleRole.isEnabled = true
            }
            viewModel.instance?.stopAll()
            viewModel.socketService?.disconnectSocket()
        }
    }

    override fun receivedMessage(message: JSONObject) {
        super.receivedMessage(message)

        try {
            if (message.getString("value") == "IS_ALIVE"){
                viewModel.socketService?.sendMessage(JsonServerMessage.isAliveMessage(message.getString("value")).toString())
            } else {
                Log.i(TAG, message.toString())
                if (message.getString("value") == "display_message"){
                    val displayMessageParameters = message.getJSONObject("parameters")
                    if (displayMessageParameters.getString("target") == viewModel.deviceId) {
                        MyAlertDialog.showDialog(
                            parentFragmentManager,
                            TAG!!,
                            MyAlertDialog.MessageType.ALERT_INFO,
                            R.drawable.ic_alert_dialog_info_24,
                            "Alert",
                            displayMessageParameters.getString("message")
                        )
                    } else {
                        relaySelectionPlayersConnected.forEach { connectedDevice ->
                            if (displayMessageParameters.getString("target") == connectedDevice.getString("endPointName")){
                                message.put("messageType", RelaySelectionInfoMessage.SOCKET.ordinal)
                                viewModel.instance?.notifyToConnectedDevice(
                                    connectedDevice.getString("endPointId"),
                                    MessageTag.RELAY_SELECTION,
                                    message
                                )
                                return
                            }
                        }
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onConnectivityChange(active: Boolean) {
        super.onConnectivityChange(active)
        if(!isDiscoverer){
            return
        }

        scoreComputingScope.launch {
            if (active && throughputComputingJob == null) {
                throughputComputingJob = launch {
                    while (isActive) {
                        delay(5*60000L)
                        viewModel.instance?.apply {

                            cancelFileTransferIfAny {
                                Thread.sleep(500L)
                            }

                            val testFile = File.createTempFile("testFile",null, requireContext().cacheDir)
                            val raf = RandomAccessFile(testFile, "rw")
                            raf.setLength((ConnectionsClient.MAX_BYTES_DATA_SIZE * 5).toLong())
                            raf.close()
                            mutex.withLock {
                                isThroughputComputingRunning = true
                                sendFileToConnectedDevices(MessageTag.RELAY_SELECTION, testFile) {
                                    isThroughputComputingRunning = false
                                    try {
                                        testFile.delete()
                                        Log.i(TAG, "${testFile.name} deleted")
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
                batteryManagerComputingJob = launch {
                    while (isActive){
                        mutex.withLock {
                            if (!isThroughputComputingRunning) {
                                D2D.notifyToAllConnectedDevices(
                                    MessageTag.RELAY_SELECTION,
                                    MessageBytes.INFO_PACKET,
                                    JSONObject()
                                        .put(
                                            "messageType",
                                            RelaySelectionInfoMessage.METRICS.ordinal
                                        )
                                        .put("messageCategory", "request")
                                )
                                delay(5000L)
                            }
                        }
                    }
                }
            } else {
                throughputComputingJob?.let {
                    it.cancel()
                    it.join()
                }
                batteryManagerComputingJob?.let {
                    it.cancel()
                    it.join()
                }
                throughputComputingJob = null
                batteryManagerComputingJob = null
            }
        }
    }

    override fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject) {
        super.onDeviceConnected(isActive, endPointInfo)
        if(isActive){
            if(isDiscoverer){
                if(targetRelay == null){
                    targetRelay = endPointInfo
                    targetRelay?.let{
                        viewModel.instance?.notifyToConnectedDevice(
                            it.getString("endPointId"),
                            MessageTag.RELAY_SELECTION,
                            JSONObject()
                                .put("messageType", RelaySelectionInfoMessage.PEER.ordinal)
                                .put("messageCategory", "targetRelay")
                                .put("isTargetRelay", true)
                        )
                    }
                }
            }

            val parameters = JSONObject()
            parameters.put("endPointId", endPointInfo.get("endPointId"))
            parameters.put("deviceId", endPointInfo.get("endPointName"))
            parameters.put("score", 0)

            relaySelectionPlayersConnected.add(parameters)
            adapter.notifyItemInserted(relaySelectionPlayersConnected.lastIndex)

        } else{
            relaySelectionPlayersConnected.forEachIndexed{ index, parameters ->
                if(parameters.get("endPointId") == endPointInfo.get("endPointId")) {
                    adapter.notifyItemRemoved(index)
                    relaySelectionPlayersConnected.removeAt(index)
                    return
                }
            }
        }

        if(relaySelectionPlayersConnected.size >= 2 && viewModel.instance?.isDiscovering() == true){
            Log.e(TAG, "relaySelectionPlayersConnected.size >= 2")
            viewModel.instance?.stopDiscoveringOrAdvertising()
        } else {
            if(viewModel.instance?.isDiscovering() == false) {
                if (isDiscoverer) {
                    viewModel.instance?.startDiscovery(globalStrategy, false)
                } else {
                    viewModel.instance?.startAdvertising(
                        viewModel.deviceId!!,
                        globalStrategy,
                        false,
                        ConnectionType.NON_DISRUPTIVE
                    )
                }
            }
        }
    }

    override fun onInfoPacketReceived(messageTag: Byte, payload: List<String>) {
        super.onInfoPacketReceived(messageTag, payload)

        val fromEndPointId = payload[0]
        val jsonMessage = JSONObject(payload[1])

        when(jsonMessage.getInt("messageType")){
            RelaySelectionInfoMessage.SOCKET.ordinal -> {
                if(jsonMessage.getInt("command") == Socket.JsonCommands.SERVER_MESSAGE.ordinal){
                    if(jsonMessage.getString("value") == "display_message"){
                        val messageParameters = JSONObject(jsonMessage.getString("parameters"))
                        MyAlertDialog.showDialog(
                            parentFragmentManager,
                            TAG!!,
                            MyAlertDialog.MessageType.ALERT_INFO,
                            R.drawable.ic_alert_dialog_info_24,
                            "Alert",
                            messageParameters.getString("message")
                        )
                    }
                }
            }
            RelaySelectionInfoMessage.PEER.ordinal -> {
                if(jsonMessage.getString("messageCategory") == "targetRelay"){
                    if(jsonMessage.getBoolean("isTargetRelay")){
                        relaySelectionPlayersConnected.forEach { connectedDevices ->
                            if(connectedDevices.getString("endPointId") == fromEndPointId){
                                targetRelay = connectedDevices
                                val notifySocket = JsonServerMessage.newConnection(
                                    JsonServerMessage.NewConnectionOptions.D2D_DEVICE_CONNECTION,
                                    connectedDevices.getString("deviceId")
                                )
                                viewModel.socketService?.sendMessage(notifySocket.toString())
                                return@forEach
                            }
                        }
                    } else{
                        targetRelay = null
                    }
                }
            }
            RelaySelectionInfoMessage.METRICS.ordinal -> {
                when(jsonMessage.getString("messageCategory")){
                    "request" -> {
                        val batteryManagerMetrics = BatteryManagerMetrics(requireContext())

                        jsonMessage.put("messageCategory", "reply")
                        jsonMessage.put("batteryManagerMetric", batteryManagerMetrics.getEstimatedBatteryLifetime())

                        viewModel.instance?.notifyToConnectedDevice(
                            fromEndPointId,
                            MessageTag.RELAY_SELECTION,
                            jsonMessage
                        )
                    }

                    "reply" -> {
                        try {
                            val batteryManagerMetric:Double = jsonMessage.getDouble("batteryManagerMetric")
                            batteryManagerMetric.let {
                                scoreComputation.insertBatteryManagerMetric(fromEndPointId, it)
                            }

                            Log.i(TAG, "batteryManagerMetric: $batteryManagerMetric from $fromEndPointId")
                            Log.i(TAG, relaySelectionPlayersConnected.toString())


                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun onReceivedTaskResul(from: D2D.ParameterTag, value: JSONObject) {
        super.onReceivedTaskResul(from, value)
        if(value.getString("type") == "payloadTransferUpdate"){
            when(value.getString("status")){
                "inProgress" -> {
                    scoreComputation.insertThroughputMetric(
                        value.getString("endPointId"),
                        value.getLong("timing"),
                        value.getLong("bytesTransferred")
                    )
                }
                "finished" -> {
                    relaySelectionPlayersConnected.forEachIndexed { index, parameters ->
                        if(parameters.get("endPointId") == value.getString("endPointId")) {
                            parameters.put("score", scoreComputation.getComputedScore(value.getString("endPointId")))
                            adapter.notifyItemChanged(index)
                            return@forEachIndexed
                        }
                    }
                }
            }
        }
    }

    private fun checkForInternet(context: Context): Boolean {

        // register activity with the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Returns a Network object corresponding to
        // the currently active default data network.
        val network = connectivityManager.activeNetwork ?: return false

        // Representation of the capabilities of an active network.
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            // Indicates this network uses a Wi-Fi transport,
            // or WiFi has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            // Indicates this network uses a Cellular transport. or
            // Cellular has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            // else return false
            else -> false
        }
    }
}