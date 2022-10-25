package com.sorbonne.atom_d.ui.dashboard

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.sorbonne.atom_d.MainActivity
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.EntityType
import com.sorbonne.atom_d.adapters.double_column.EntityAdapterDoubleColumn
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.entities.data_connection_attempts.DataConnectionAttempts
import com.sorbonne.atom_d.guard
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.tools.MessageTag
import com.sorbonne.atom_d.ui.experiment.ExperimentViewModel
import com.sorbonne.atom_d.ui.experiment.ExperimentViewModelFactory
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.D2DListener
import org.json.JSONObject
import java.util.*

class DashboardFragment : Fragment(), D2DListener {

    private val TAG = DashboardFragment::class.simpleName

    private lateinit var viewModel: DashboardViewModel

    private val experimentViewModel: ExperimentViewModel by viewModels {
        ExperimentViewModelFactory(DatabaseRepository(requireActivity().application))
    }

    private val strategy = Strategy.P2P_STAR

    private var discoveryState: Chip?= null
    private var connectionState: Chip?= null

    private lateinit var initD2D: Button
    private lateinit var startExperiment: Button
    private lateinit var stopExperiment: Button

    private lateinit var guiLatitude: TextView
    private lateinit var guiLongitude: TextView

    private var latitude = 0.0
    private var longitude = 0.0

    private lateinit var experimentProgressBar: ProgressBar
    private lateinit var taskProgressBar: ProgressBar

    private var viewInstanceState: Bundle ?= null

    private var targetEndDeviceId:String ?= null
    private var targetDeviceId:String ?= null
    private var experimentId: Long = 0

    private var isExperimentRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, DashboardViewModel.Factory(context, DatabaseRepository(requireActivity().application)))[DashboardViewModel::class.java]
        viewModel.instance = (context as? MainActivity).guard { return }.d2d
        viewModel.deviceId = (context as? MainActivity).guard { return }.androidId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceId: TextView = view.findViewById(R.id.dashboard_device_id)
        initD2D = view.findViewById(R.id.dashboard_start_d2d)
        val selectedRole: MaterialButtonToggleGroup = view.findViewById(R.id.dashboard_role)
        startExperiment = view.findViewById(R.id.dashboard_start_experiment)
        stopExperiment = view.findViewById(R.id.dashboard_stop_experiment)
        val endDevicesDiscovered: Button = view.findViewById(R.id.dashboard_playerList)
        val gps: Switch = view.findViewById(R.id.dashboard_gps_button)
        guiLatitude = view.findViewById(R.id.dashboard_latitude)
        guiLongitude = view.findViewById(R.id.dashboard_longitude)
        taskProgressBar = view.findViewById(R.id.dashboard_payload_task_progressBar)
        experimentProgressBar = view.findViewById(R.id.dashboard_payload_experiment_progressBar)



        startExperiment.isEnabled = false
        stopExperiment.isEnabled = false

        val dashboardAdapter = EntityAdapterDoubleColumn(DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView, EntityType.CustomQueries)
        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.dashboard_recyclerView),
            dashboardAdapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()
        experimentViewModel.getAllExperimentsName().observe(requireActivity(), dashboardAdapter::submitList)


        discoveryState = view.findViewById(R.id.dashboard_status_discovering)
        connectionState = view.findViewById(R.id.dashboard_status_connected)

        deviceId.text = viewModel.deviceId.toString()

        initD2D.setOnClickListener{
            it.isEnabled = false
            stopExperiment.isEnabled = true
            if(selectedRole.checkedButtonId == R.id.dashboard_role_disc || selectedRole.checkedButtonId == R.id.dashboard_role_adv){
                if(selectedRole.checkedButtonId == R.id.dashboard_role_disc){
                    viewModel.instance?.startDiscovery(strategy, false)
                }else{
                    viewModel.deviceId?.let {
                            deviceName -> viewModel.instance?.startAdvertising(deviceName, strategy, false, ConnectionType.DISRUPTIVE)
                    }
                }
            }
        }

        gps.setOnClickListener { it as Switch
            if(it.isChecked){
                viewModel.instance?.enableLocationUpdate(requireActivity())
            }else{
                viewModel.instance?.disableLocationUpdate()
            }
        }

        startExperiment.setOnClickListener {
            isExperimentRunning = true
            it.isEnabled = false
            dashboardAdapter.currentList[dashboardAdapter.getLastCheckedPosition()].let { selectedExperiment ->
                selectedExperiment as CustomQueriesDao.AllExperimentsName
                val notificationParameters: JSONObject

                when(selectedExperiment.type){
                    "CHUNK" ->{
                        viewModel.instance?.sendSetOfChunks()
                    }
                    "FILE" ->{
                        viewModel.instance?.sendSetOfBinaryFile()
                    }
                    "DISCOVERY" ->{

                        notificationParameters = setNotificationParametersDiscovery(
                            "request",
                            selectedExperiment.experiment_name,
                            selectedExperiment.attempts,
                            false,
                            viewModel.deviceId!!
                        )

                        targetEndDeviceId?.let { endPointId ->
                            viewModel.instance?.notifyToConnectedDevice(endPointId, MessageTag.D2D_PERFORMANCE,notificationParameters){}
                        }
                    }
                }
            }
        }

        stopExperiment.setOnClickListener {
            stopExperiment()
            viewModel.instance?.stopAll()
        }

        endDevicesDiscovered.setOnClickListener {
            val action = DashboardFragmentDirections.actionDashboardFragmentToEndpointsDiscoveredFragment()
            Navigation.findNavController(view).navigate(action)
        }

        if(viewInstanceState != null){
            isExperimentRunning = viewInstanceState!!.getBoolean("isExperimentRunning")
            viewModel.instance?.let {
                setConnectedStatus(it.isConnected())
                setDiscoveringStatus(it.isDiscovering())
            }
            viewInstanceState = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewInstanceState = Bundle()
        viewInstanceState!!.putBoolean("isExperimentRunning", isExperimentRunning)
    }

    override fun onDiscoveryChange(active: Boolean) {
        super.onDiscoveryChange(active)
        setDiscoveringStatus(active)
    }

    override fun onConnectivityChange(active: Boolean) {
        super.onConnectivityChange(active)
        setConnectedStatus(active)
        if(!isExperimentRunning){
            viewModel.instance?.stopDiscoveringOrAdvertising()
        }
    }

    override fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject) {
        super.onDeviceConnected(isActive, endPointInfo)
        if(isActive){
            targetEndDeviceId = endPointInfo.getString("endPointId")
            targetDeviceId = endPointInfo.getString("endPointName")
        }else {
            targetEndDeviceId = null
        }
    }

    override fun onExperimentProgress(isExperimentBar: Boolean, progression: Int) {
        super.onExperimentProgress(isExperimentBar, progression)
        if(isExperimentBar){
            experimentProgressBar.setProgress(progression, true)
        } else {
            taskProgressBar.setProgress(progression, true)
        }
    }

    override fun onInfoPacketReceived(messageTag: Byte, payload: String) {
        super.onInfoPacketReceived(messageTag, payload)
        Log.i(TAG, "onInfoPacketReceived: $payload")

        val notificationParameters: JSONObject
        val payloadParameters = JSONObject(payload)
        when(payloadParameters.get("experimentType")){
            "discovery" -> {
                when(payloadParameters.get("experimentMessageType")){
                    "request" ->{
                        experimentViewModel.insertConnectionAttemptExperiment(
                            ConnectionAttempts(
                                0,
                                payloadParameters.getString("experimentName"),
                                payloadParameters.getInt("discoveryAttempts"),
                            )
                        )

                        isExperimentRunning = true
                        startExperiment.isEnabled = false


                        notificationParameters = setNotificationParametersDiscovery(
                            "replay",
                            payloadParameters.getString("experimentName"),
                            payloadParameters.getInt("discoveryAttempts"),
                            payloadParameters.getBoolean("asLowPower"),
                            viewModel.deviceId!!
                        )

                        targetEndDeviceId?.let {
                            viewModel.instance?.notifyToConnectedDevice(it, MessageTag.D2D_PERFORMANCE,notificationParameters) {
                                targetEndDeviceId?.let {  endDevice ->
                                    Thread.sleep(500L)
                                    viewModel.instance?.disconnectFromDevice(endDevice)
                                    viewModel.instance?.startAdvertising(
                                        viewModel.deviceId!!,
                                        Strategy.P2P_POINT_TO_POINT,
                                        payloadParameters.getBoolean("asLowPower"),
                                        ConnectionType.DISRUPTIVE
                                    )
                                }
                            }
                        }
                    }

                    "replay" -> {
                        experimentId = System.currentTimeMillis()
                        isExperimentRunning = true
                        viewModel.instance?.performDiscoverAttempts(
                            payloadParameters.getString("targetDevice"),
                            payloadParameters.getInt("discoveryAttempts"),
                            payloadParameters.getBoolean("asLowPower")
                        )
                    }

                    "finished" ->{
                        targetEndDeviceId?.let {
//                                if(payloadParameters.get("target") == "local"){
//                                    payloadParameters.put("target", "peer")
//                                    viewModel.instance?.notifyToConnectedDevice(it, MessageTag.D2D_PERFORMANCE, payloadParameters){}
//                                    resetToStandbyStatus()
//                                }
                            if(payloadParameters.get("target") == "peer"){
                                viewModel.instance?.stopDiscoveringOrAdvertising()
                                resetToStandbyStatus()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onReceivedTaskResul(from: D2D.ParameterTag, value: JSONObject) {
        super.onReceivedTaskResul(from, value)
        when(from){
            D2D.ParameterTag.DISCOVERY ->{
                Log.i(TAG, "onReceivedTaskResul: $value")
                if(value.getString("experimentState") == "running"){
                    val experimentValueParameters = value.getJSONObject("experimentValueParameters")
                    viewModel.insertDataConnectionAttempts(
                        DataConnectionAttempts(0,
                            experimentId,
                            viewModel.deviceId!!,
                            targetDeviceId!!,
                            experimentValueParameters.getInt("totalNumberOfAttempts"),
                            experimentValueParameters.getBoolean("isLowPower"),
                            experimentValueParameters.getInt("attempt"),
                            experimentValueParameters.getString("state"),
                            experimentValueParameters.getLong("stateTiming"),
                            latitude,
                            longitude
                        )
                    )
                }
                if(value.getString("experimentState") == "finished"){
                    viewModel.instance?.stopDiscoveringOrAdvertising()
                    val experimentValueParameters = value.getJSONObject("experimentValueParameters")
                    experimentValueParameters.put("target", "peer")
                    targetEndDeviceId?.let {
                        viewModel.instance?.notifyToConnectedDevice(
                            it,
                            MessageTag.D2D_PERFORMANCE,
                            experimentValueParameters
                        ) {}
                    }
                    resetToStandbyStatus()
                }
            }
        }
    }

    override fun onLastLocation(location: Location) {
        super.onLastLocation(location)
        latitude = location.latitude
        longitude = location.longitude

        guiLatitude.text = latitude.toString()
        guiLongitude.text = longitude.toString()
    }

    private fun stopExperiment(){
        isExperimentRunning = false
        initD2D.isEnabled = true
        startExperiment.isEnabled = false
        stopExperiment.isEnabled = false
    }

    private fun resetToStandbyStatus(){
        isExperimentRunning = false
        startExperiment.isEnabled = true
    }

    private fun setConnectedStatus(state: Boolean){
        if(state){
            connectionState?.setChipBackgroundColorResource(R.color.light_coral)
            if(!isExperimentRunning){
                startExperiment.isEnabled = true
            }
            stopExperiment.isEnabled = true
            initD2D.isEnabled = false
        } else {
            connectionState?.setChipBackgroundColorResource(R.color.light_grey)
            if(!isExperimentRunning){
                stopExperiment()
            }
        }
    }

    private fun setDiscoveringStatus(state: Boolean){
        if(state){
            discoveryState?.setChipBackgroundColorResource(R.color.light_coral)
            stopExperiment.isEnabled = true
            initD2D.isEnabled = false
        } else {
            discoveryState?.setChipBackgroundColorResource(R.color.light_grey)
        }
    }

    private fun setNotificationParametersDiscovery(experimentMessageType: String, experimentName: String, discoveryAttempts:Int, asLowPower: Boolean, targetDevice: String): JSONObject {
        val notificationParameters = JSONObject()
        notificationParameters.put("experimentType", "discovery")
        notificationParameters.put("experimentMessageType", experimentMessageType)
        notificationParameters.put("experimentName", experimentName)
        notificationParameters.put("discoveryAttempts", discoveryAttempts)
        notificationParameters.put("asLowPower",asLowPower)
        notificationParameters.put("targetDevice", targetDevice)
        return notificationParameters
    }
}