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
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.sorbonne.atom_d.MainActivity
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.RelaySelectionColumnAdapter
import com.sorbonne.atom_d.guard
import com.sorbonne.atom_d.services.socket.SocketListener
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.tools.JsonServerMessage
import com.sorbonne.atom_d.tools.MyAlertDialogFragment
import com.sorbonne.d2d.D2DListener
import org.json.JSONObject
import java.net.InetSocketAddress

class RelaySelectionFragment : Fragment(), SocketListener, D2DListener {
    companion object {
        fun newInstance() = RelaySelectionFragment()
    }

    private val TAG = RelaySelectionFragment::class.simpleName

    private lateinit var viewModel: RelaySelectionViewModel

    private var relaySelectionPlayersConnected = mutableListOf<JSONObject>()
    private var adapter = RelaySelectionColumnAdapter(relaySelectionPlayersConnected)

    private var isDiscoverer = false

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
        ).recyclerView

        val deviceId : TextView = view.findViewById(R.id.relay_selection_device_id)
        val startRelaySelection : Button = view.findViewById(R.id.relay_selection_start)
        val stopRelaySelection : Button = view.findViewById(R.id.relay_selection_stop)
        val selectedRole : MaterialButtonToggleGroup = view.findViewById(R.id.relay_selection_role)
        val bottomNavigationView : BottomNavigationView = requireActivity().findViewById(R.id.bottom_nav)
        val performSelection : Button = view.findViewById(R.id.relay_selection_perform_selection)

        deviceId.text = viewModel.deviceId

        startRelaySelection.setOnClickListener { start ->
            viewModel.socketService = (context as? MainActivity).guard {}.socketService

            viewModel.socketService?.setListener(viewLifecycleOwner,this)
            if (selectedRole.checkedButtonId == R.id.relay_selection_role_disc || selectedRole.checkedButtonId == R.id.relay_selection_role_adv) {
                if(start.isEnabled){
                    start.isEnabled = false
                    stopRelaySelection.isEnabled = true
                }

                bottomNavigationView.menu.apply {
                    findItem(R.id.navigation_dashboard).isEnabled = false
                    findItem(R.id.navigation_experiment).isEnabled = false
                }

                if(selectedRole.checkedButtonId == R.id.relay_selection_role_disc){
                    isDiscoverer = true

                    viewModel.deviceId?.let { deviceId ->
                        viewModel.socketService?.initSocketConnection(InetSocketAddress("192.168.1.100",33235),
                            deviceId
                        )
                    }

                    viewModel.instance?.startDiscovery(
                        Strategy.P2P_POINT_TO_POINT,
                        false,
                    )
                } else if(selectedRole.checkedButtonId == R.id.relay_selection_role_adv) {
                    isDiscoverer = false
                    viewModel.instance?.startAdvertising(
                        viewModel.deviceId!!,
                        Strategy.P2P_POINT_TO_POINT,
                        false,
                        ConnectionType.NON_DISRUPTIVE
                    )
                }

                for (index in 0 until selectedRole.childCount){
                    val singleRole = selectedRole.getChildAt(index) as MaterialButton
                    singleRole.isEnabled = false
                }
            }
        }

        stopRelaySelection.setOnClickListener { stop ->
            if(stop.isEnabled){
                stop.isEnabled = false
                startRelaySelection.isEnabled = true
            }

            bottomNavigationView.menu.apply {
                findItem(R.id.navigation_dashboard).isEnabled = true
                findItem(R.id.navigation_experiment).isEnabled = true
            }

            for (index in 0 until selectedRole.childCount){
                val singleRole = selectedRole.getChildAt(index) as MaterialButton
                singleRole.isEnabled = true
            }

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
                        MyAlertDialogFragment.showDialog(
                            parentFragmentManager,
                            TAG,
                            "Alert",
                            displayMessageParameters.getString("message"),
                            R.drawable.ic_alert_dialog_info_24,
                            false,
                            MyAlertDialogFragment.MESSAGE_TYPE.ALERT_INFO,
                            null,
                            null
                        )
                    } else {
                        //TODO
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
            val parameters = JSONObject()
            parameters.put("endPointId", endPointInfo.get("endPointId"))
            parameters.put("deviceId", endPointInfo.get("endPointName"))
            parameters.put("connectionDelay", 0)
            parameters.put("batteryLife", 0)
            parameters.put("throughput", 0)
            parameters.put("rank", 0)

            relaySelectionPlayersConnected.add(parameters)
            adapter.notifyItemInserted(relaySelectionPlayersConnected.lastIndex)

            if(isDiscoverer) {
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
                    return
                }
            }
        }
    }
}