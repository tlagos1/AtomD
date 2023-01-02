package com.sorbonne.atom_d.ui.dashboard

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.connection.BandwidthInfo
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.sorbonne.atom_d.MainActivity
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.AdapterType
import com.sorbonne.atom_d.adapters.double_column.AdapterDoubleColumn
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.entities.data_connection_attempts.DataConnectionAttempts
import com.sorbonne.atom_d.entities.data_file_experiments.DataFileExperiments
import com.sorbonne.atom_d.entities.file_experiments.FileExperiments
import com.sorbonne.atom_d.guard
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.tools.MessageTag
import com.sorbonne.atom_d.tools.MyAlertDialog
import com.sorbonne.atom_d.ui.experiment.ExperimentViewModel
import com.sorbonne.atom_d.ui.experiment.ExperimentViewModelFactory
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.D2DListener
import com.sorbonne.d2d.tools.MessageBytes
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.*


class DashboardFragment : Fragment(), D2DListener, OnItemSelectedListener  {

    private val TAG = DashboardFragment::class.simpleName

    private lateinit var viewModel: DashboardViewModel

    private val experimentViewModel: ExperimentViewModel by viewModels {
        ExperimentViewModelFactory(DatabaseRepository(requireActivity().application))
    }

    private var strategy: Strategy ?= null
    private var readableStrategy: Int = -1

    private var discoveryState: Chip?= null
    private var connectionState: Chip?= null

    private lateinit var initD2D: Button
    private lateinit var startExperiment: Button
    private lateinit var stopExperiment: Button
    private lateinit var bandwidthStatus: ImageView

    private lateinit var guiLatitude: TextView
    private lateinit var guiLongitude: TextView

    private var latitude = 0.0
    private var longitude = 0.0

    private lateinit var experimentProgressBar: ProgressBar
    private lateinit var taskProgressBar: ProgressBar

    private var viewInstanceState: Bundle ?= null

    private var targetEndDeviceId:String ?= null
    private var experimentId: Long = 0

    private var isExperimentRunning = false
    private var isValidStrategy = false
    private var isValidRole = false

    private val adapterConnectedDevices = AdapterDoubleColumn(DoubleColumnViewHolder.DoubleColumnType.CheckBoxTextView, AdapterType.DynamicList)
    private val adapterDiscoveredDevices = AdapterDoubleColumn(DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView, AdapterType.DynamicList)
    private val connectedDevices = mutableMapOf<String, String>()
    private val bandwidthInfo = mutableMapOf<String, Int>()
    private val deviceBandwidthQuality = mutableMapOf<String, Int>()
    private val discoveredDevices = mutableMapOf<String,String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, DashboardViewModel.Factory(context, DatabaseRepository(requireActivity().application)))[DashboardViewModel::class.java]
        viewModel.instance = (context as? MainActivity).guard { return }.d2d
        viewModel.deviceId = (context as? MainActivity).guard { return }.androidId

        viewModel.connectedDevices.observe(requireActivity(), adapterConnectedDevices::submitList )
        viewModel.discoveredDevices.observe(requireActivity(), adapterDiscoveredDevices::submitList)
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
//        val endDevicesDiscovered: Button = view.findViewById(R.id.dashboard_playerList)
        val d2dStrategies: Spinner = view.findViewById(R.id.dashboard_strategies)
        val gps: Switch = view.findViewById(R.id.dashboard_gps_button)
        guiLatitude = view.findViewById(R.id.dashboard_latitude)
        guiLongitude = view.findViewById(R.id.dashboard_longitude)
        taskProgressBar = view.findViewById(R.id.dashboard_payload_task_progressBar)
        experimentProgressBar = view.findViewById(R.id.dashboard_payload_experiment_progressBar)
        bandwidthStatus = view.findViewById(R.id.bandwidth_quality)

        startExperiment.isEnabled = false
        stopExperiment.isEnabled = false

        val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayOf("Strategies","P2P_POINT_TO_POINT", "P2P_STAR", "P2P_CLUSTER")
        )

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        d2dStrategies.adapter = spinnerAdapter
        d2dStrategies.onItemSelectedListener = this


        val dashboardAdapter = AdapterDoubleColumn(DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView, AdapterType.CustomQueries)
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

        initD2D.setOnClickListener {
            if(isValidStrategy && strategy != Strategy.P2P_CLUSTER) {
                if (selectedRole.checkedButtonId == R.id.dashboard_role_disc || selectedRole.checkedButtonId == R.id.dashboard_role_adv) {
                    it.isEnabled = false
                    stopExperiment.isEnabled = true

                    if(selectedRole.checkedButtonId == R.id.dashboard_role_disc) {

                        if(strategy == Strategy.P2P_POINT_TO_POINT) {
                            viewModel.instance?.startDiscovery(strategy!!, false, automaticRequest = false)
                            MyAlertDialog.showDialog(
                                parentFragmentManager,
                                TAG!!,
                                MyAlertDialog.MessageType.ALERT_INPUT_RECYCLE_VIEW,
                                R.drawable.ic_alert_dialog_info_24,
                                "Discovered Devices",
                                null,
                                R.layout.dialog_recycleview,
                                adapterDoubleColumn = adapterDiscoveredDevices,
                                option1 = fun (recycleViewAdapter){
                                    recycleViewAdapter as AdapterDoubleColumn
                                    if(recycleViewAdapter.getLastCheckedPosition() >= 0){
                                        if(recycleViewAdapter.currentList.size > recycleViewAdapter.getLastCheckedPosition()){
                                            val mEndPointInfo = (recycleViewAdapter.currentList[recycleViewAdapter.getLastCheckedPosition()]) as List<*>
                                            viewModel.instance?.requestConnectionToEndPoint(mEndPointInfo[1].toString())
                                            return
                                        }
                                    }
                                    viewModel.instance?.stopAll()
                                }
                            )
                        } else if (strategy == Strategy.P2P_STAR){
                            viewModel.instance?.startDiscovery(strategy!!, false)
                        }
                    } else {
                        viewModel.deviceId?.let { deviceName ->
                            viewModel.instance?.startAdvertising(
                                deviceName,
                                strategy!!,
                                false,
                                ConnectionType.DISRUPTIVE
                            )
                        }
                    }
                }
            } else if(isValidStrategy && strategy == Strategy.P2P_CLUSTER){
                viewModel.instance?.startDiscovery(strategy!!, false)
                viewModel.deviceId?.let { deviceName ->
                    viewModel.instance?.startAdvertising(
                        deviceName,
                        strategy!!,
                        false,
                        ConnectionType.DISRUPTIVE
                    )
                }
            } else {
                Toast.makeText(requireContext(), "Strategy or Role not valid", Toast.LENGTH_LONG).show()
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

            if(dashboardAdapter.currentList.isEmpty() || dashboardAdapter.getLastCheckedPosition() < 0){
                Toast.makeText(requireContext(), "Experiment not valid", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

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
                        MyAlertDialog.showDialog(
                            parentFragmentManager,
                            TAG!!,
                            MyAlertDialog.MessageType.ALERT_INPUT_RECYCLE_VIEW,
                            R.drawable.ic_alert_dialog_info_24,
                            "Connected Devices",
                            null,
                            R.layout.dialog_recycleview,
                            adapterDoubleColumn = adapterConnectedDevices,
                            option1 = fun (recycleViewAdapter){
                                recycleViewAdapter as AdapterDoubleColumn
                                if(recycleViewAdapter.getCheckedBoxes().isEmpty()){
                                    it.isEnabled = true
                                    return
                                }

                                val testFile = File.createTempFile("testFile",null, requireContext().cacheDir)
                                val raf = RandomAccessFile(testFile, "rw")
                                raf.setLength(selectedExperiment.size)
                                raf.close()

                                viewModel.instance?.apply {
                                    notifyToSetOfConnectedDevices(
                                        recycleViewAdapter.getCheckedBoxes(),
                                        MessageTag.D2D_PERFORMANCE,
                                        MessageBytes.INFO_PACKET,
                                        JSONObject()
                                            .put("experimentType","file")
                                            .put("experimentName", selectedExperiment.experiment_name)
                                            .put("fileSize", selectedExperiment.size)
                                            .put("fileTries", selectedExperiment.attempts)
                                    ){
                                        performFileExperiment(
                                            recycleViewAdapter.getCheckedBoxes(),
                                            MessageTag.D2D_PERFORMANCE,
                                            selectedExperiment.experiment_name,
                                            selectedExperiment.attempts,
                                            testFile
                                        )
                                    }
                                }
                            }
                        )
//                        viewModel.instance?.performFileExperiment()
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

//        endDevicesDiscovered.setOnClickListener {
//            val action = DashboardFragmentDirections.actionDashboardFragmentToEndpointsDiscoveredFragment()
//            Navigation.findNavController(view).navigate(action)
//        }

        if(viewInstanceState != null){
            isExperimentRunning = viewInstanceState!!.getBoolean("isExperimentRunning")
            viewModel.instance?.let {
                setConnectedStatus(it.isConnected())
                setDiscoveringStatus(it.isDiscovering())
            }
            viewInstanceState = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.instance?.stopAll()
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, index: Int, Id: Long) {
         when(index){
            1 ->{
                strategy = Strategy.P2P_POINT_TO_POINT
            }
            2 -> {
                strategy = Strategy.P2P_STAR
            }
            3 -> {
                strategy = Strategy.P2P_CLUSTER
            }
            else -> {
                readableStrategy = -1
                isValidStrategy = false
                return
            }
        }
        readableStrategy = index
        isValidStrategy = true
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewInstanceState = Bundle()
        viewInstanceState!!.putBoolean("isExperimentRunning", isExperimentRunning)
    }

    override fun onDiscoveryChange(active: Boolean) {
        super.onDiscoveryChange(active)
        setDiscoveringStatus(active)
        if(!active){
            discoveredDevices.clear()
            val auxList = mutableListOf<List<String>>()
            viewModel.discoveredDevices.value = auxList
        }
    }

    override fun onConnectivityChange(active: Boolean) {
        super.onConnectivityChange(active)
        setConnectedStatus(active)
        if(active){
            if(strategy == Strategy.P2P_POINT_TO_POINT){
                viewModel.instance?.stopDiscoveringOrAdvertising()
            }
        }
    }

    override fun onBandwidthQuality(endPointInfo: JSONObject) {
        super.onBandwidthQuality(endPointInfo)
        bandwidthInfo[endPointInfo.getString("endPointId")] = endPointInfo.getInt("bandwidthInfo")
        adapterConnectedDevices.updateBandwidthItem(endPointInfo.getString("endPointId"), endPointInfo.getInt("bandwidthInfo"))
    }

    override fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject) {
        super.onDeviceConnected(isActive, endPointInfo)
        if(isActive){
            connectedDevices[endPointInfo.getString("endPointId")] = endPointInfo.getString("endPointName")
            deviceBandwidthQuality[endPointInfo.getString("endPointId")] = endPointInfo.getInt("bandwidthQuality")
            targetEndDeviceId = endPointInfo.getString("endPointId")
        }else {
            connectedDevices.remove(endPointInfo.getString("endPointId"))
            bandwidthInfo.remove(endPointInfo.getString("endPointId"))
            deviceBandwidthQuality.remove(endPointInfo.getString("endPointId"))
            targetEndDeviceId = null
        }
        val auxList = mutableListOf<List<String>>()
        connectedDevices.entries.forEach {
            auxList.add(mutableListOf(it.key, it.value, deviceBandwidthQuality[it.key].toString()))
        }
        viewModel.connectedDevices.value = auxList
    }

    override fun onEndPointsDiscovered(isActive: Boolean, endPointInfo: JSONObject) {
        super.onEndPointsDiscovered(isActive, endPointInfo)
        if(isActive){
            var mEndPointId:String? = null
            discoveredDevices.entries.forEach { (endPointId, endPointName) ->
                if(endPointInfo.getString("endPointName") == endPointName){
                    mEndPointId = endPointId
                    return@forEach
                }
            }
            mEndPointId?.let{
                discoveredDevices.remove(it)
            }
            discoveredDevices[endPointInfo.getString("endPointId")] = endPointInfo.getString("endPointName")
        } else {
            discoveredDevices.remove(endPointInfo.getString("endPointId"))
        }
        val auxList = mutableListOf<List<String>>()
        discoveredDevices.entries.forEach {
            auxList.add(mutableListOf(it.value, it.key))
        }
        viewModel.discoveredDevices.value = auxList
    }

    override fun onExperimentProgress(isExperimentBar: Boolean, progression: Int) {
        super.onExperimentProgress(isExperimentBar, progression)
        if(isExperimentBar){
            experimentProgressBar.setProgress(progression, true)
        } else {
            taskProgressBar.setProgress(progression, true)
        }
    }

    override fun onInfoPacketReceived(messageTag: Byte, payload: List<String>) {
        super.onInfoPacketReceived(messageTag, payload)
        Log.i(TAG, "onInfoPacketReceived: $payload")

        val notificationParameters: JSONObject
        val payloadParameters = JSONObject(payload[1])
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
            "file" -> {
                experimentViewModel.insertFileExperiment(
                    FileExperiments(
                        0,
                        payloadParameters.getString("experimentName"),
                        payloadParameters.getLong("fileSize"),
                        payloadParameters.getInt("fileTries"),
                    )
                )
            }
        }
    }

    override fun onReceivedTaskResul(from: D2D.ParameterTag, value: JSONObject) {
        super.onReceivedTaskResul(from, value)
        when(from){
            D2D.ParameterTag.FILE->{
                if(value.getString("experimentState") == "running"){
                    val experimentValueParameters = value.getJSONObject("experimentValueParameters")
                    viewModel.insertFileExperiments(
                        DataFileExperiments(0,
                            experimentValueParameters.getLong("experimentId"),
                            experimentValueParameters.getString("experimentName"),
                            viewModel.deviceId!!,
                            experimentValueParameters.getString("targetId"),
                            experimentValueParameters.getInt("repetition"),
                            experimentValueParameters.getLong("bytesTransferred"),
                            experimentValueParameters.getLong("bytesToTransfer"),
                            experimentValueParameters.getLong("timing"),
                            experimentValueParameters.getBoolean("uploading"),
                            readableStrategy
                        )
                    )
                    when(bandwidthInfo[experimentValueParameters.getString("endPointId")]){
                        BandwidthInfo.Quality.UNKNOWN -> {
                            bandwidthStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_grey))
                        }
                        BandwidthInfo.Quality.HIGH -> {
                            bandwidthStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
                        }
                        BandwidthInfo.Quality.MEDIUM -> {
                            bandwidthStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
                        }
                        BandwidthInfo.Quality.LOW -> {
                            bandwidthStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                        }
                    }
                }
                if(value.getString("experimentState") == "finished"){
                    resetToStandbyStatus()
                }
            }
            D2D.ParameterTag.DISCOVERY ->{
                if(value.getString("experimentState") == "running"){
                    val experimentValueParameters = value.getJSONObject("experimentValueParameters")
                    viewModel.insertDataConnectionAttempts(
                        DataConnectionAttempts(0,
                            experimentId,
                            viewModel.deviceId!!,
                            experimentValueParameters.getString("targetId"),
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
            else -> {}
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
        startExperiment.isEnabled = false
        if(viewModel.instance?.isDiscovering() == false){
            stopExperiment.isEnabled = false
            initD2D.isEnabled = true
        }
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