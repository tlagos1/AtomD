package com.sorbonne.atom_d.ui.relay_selection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.gms.nearby.connection.Payload
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
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.tools.JsonServerMessage
import com.sorbonne.atom_d.tools.MessageBytes
import com.sorbonne.atom_d.tools.MyAlertDialog
import com.sorbonne.atom_d.view_holders.SingleColumnType
import com.sorbonne.d2d.D2DListener
import org.json.JSONObject
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class RelaySelectionFragment : Fragment(), SocketListener, D2DListener {
    companion object {
        fun newInstance() = RelaySelectionFragment()
    }

    private val TAG = RelaySelectionFragment::class.simpleName

    private lateinit var viewModel: RelaySelectionViewModel

    private var relaySelectionPlayersConnected = mutableListOf<JSONObject>()
    private var adapter = SimpleSingleColumnAdapter(SingleColumnType.RelaySelection, relaySelectionPlayersConnected)

    private var isDiscoverer = false

    var startRelaySelection: Button ?=null
    var stopRelaySelection: Button ?=null
    var selectedRole: MaterialButtonToggleGroup ?= null
    var bottomNavigationView: BottomNavigationView ?= null

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
                if(start.isEnabled){
                    start.isEnabled = false
                    stopRelaySelection?.isEnabled = true
                }

                bottomNavigationView?.menu?.apply {
                    findItem(R.id.navigation_dashboard).isEnabled = false
                    findItem(R.id.navigation_experiment).isEnabled = false
                }

                if(selectedRole?.checkedButtonId == R.id.relay_selection_role_disc){
                    isDiscoverer = true



                    viewModel.instance?.startDiscovery(
                        Strategy.P2P_POINT_TO_POINT,
                        false,
                    )
                } else if(selectedRole?.checkedButtonId == R.id.relay_selection_role_adv) {
                    isDiscoverer = false

                    viewModel.deviceId?.let { deviceId ->
                        viewModel.socketService?.initSocketConnection(InetSocketAddress("192.168.1.100",33235),
                            deviceId
                        )
                    }

                    viewModel.instance?.startAdvertising(
                        viewModel.deviceId!!,
                        Strategy.P2P_POINT_TO_POINT,
                        false,
                        ConnectionType.NON_DISRUPTIVE
                    )
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
                            TAG,
                            "Alert",
                            displayMessageParameters.getString("message"),
                            R.drawable.ic_alert_dialog_info_24,
                            false,
                            MyAlertDialog.MESSAGE_TYPE.ALERT_INFO,
                            null,
                            null
                        )
                    } else {
                        val d2dMessage = MessageBytes()
                        val serverMessage = message.toString()

                        d2dMessage.buildRegularPacket(
                            MessageBytes.INFO_PACKET,
                            serverMessage.toByteArray(StandardCharsets.UTF_8)
                        )

                        viewModel.instance?.sendPayloadByDeviceId(
                            displayMessageParameters.getString("target"),
                            Payload.fromBytes(d2dMessage.buffer)
                        )
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    override fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject) {
        super.onDeviceConnected(isActive, endPointInfo)
        if(isActive){

            viewModel.instance?.stopDiscoveringOrAdvertising()

            val parameters = JSONObject()
            parameters.put("endPointId", endPointInfo.get("endPointId"))
            parameters.put("deviceId", endPointInfo.get("endPointName"))
            parameters.put("connectionDelay", 0)
            parameters.put("batteryLife", 0)
            parameters.put("throughput", 0)
            parameters.put("rank", 0)

            relaySelectionPlayersConnected.add(parameters)
            adapter.notifyItemInserted(relaySelectionPlayersConnected.lastIndex)

            if(!isDiscoverer) {
                val notifySocket = JsonServerMessage.newConnection(
                    JsonServerMessage.NewConnectionOptions.D2D_DEVICE_CONNECTION,
                    endPointInfo.get("endPointName").toString()
                )
                viewModel.socketService?.sendMessage(notifySocket.toString())
            }


        }else{
            relaySelectionPlayersConnected.forEachIndexed{ index, parameters ->
                if(parameters.get("endPointId") == endPointInfo.get("endPointId")) {
                    adapter.notifyItemRemoved(index)
                    relaySelectionPlayersConnected.removeAt(index)
                    if(relaySelectionPlayersConnected.isEmpty()){
                        startRelaySelection?.isEnabled = true
                        stopRelaySelection?.isEnabled = false

                        bottomNavigationView?.menu?.apply {
                            findItem(R.id.navigation_dashboard).isEnabled = true
                            findItem(R.id.navigation_experiment).isEnabled = true
                        }

                        for (index2 in 0 until selectedRole!!.childCount){
                            val singleRole = selectedRole!!.getChildAt(index2) as MaterialButton
                            singleRole.isEnabled = true
                        }
                        viewModel.instance?.stopAll()
                        if(!isDiscoverer){
                            viewModel.socketService?.disconnectSocket()
                        }
                    }
                    return
                }
            }
        }
    }

    override fun onReceivedChunk(payload: Payload) {
        super.onReceivedChunk(payload)

        val receivedPayload = MessageBytes(payload.asBytes())

        if(receivedPayload.type == MessageBytes.INFO_PACKET){
            val jsonMessage = JSONObject(String(receivedPayload.payload,StandardCharsets.UTF_8))
            Log.e(TAG, jsonMessage.toString())
            if(jsonMessage.getInt("command") == Socket.JsonCommands.SERVER_MESSAGE.ordinal){
                if(jsonMessage.getString("value") == "display_message"){
                    val messageParameters = JSONObject(jsonMessage.getString("parameters"))
                    MyAlertDialog.showDialog(
                        parentFragmentManager,
                        TAG,
                        "Alert",
                        messageParameters.getString("message"),
                        R.drawable.ic_alert_dialog_info_24,
                        false,
                        MyAlertDialog.MESSAGE_TYPE.ALERT_INFO,
                        null,
                        null
                    )
                }
            }
        }
    }
}