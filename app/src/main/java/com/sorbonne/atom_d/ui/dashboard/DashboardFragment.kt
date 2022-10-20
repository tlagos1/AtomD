package com.sorbonne.atom_d.ui.dashboard

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.Navigation
import com.sorbonne.atom_d.MainActivity
import com.sorbonne.atom_d.guard
import com.sorbonne.d2d.D2DListener
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.gms.nearby.connection.Strategy
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.EntityType
import com.sorbonne.atom_d.adapters.double_column.EntityAdapterDoubleColumn
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder

class DashboardFragment : Fragment(), D2DListener {

    private val TAG = DashboardFragment::class.simpleName

    private lateinit var viewModel: DashboardViewModel
    private val strategy = Strategy.P2P_STAR

    private var discoveryState: Chip?= null
    private var connectionState: Chip?= null

    private lateinit var initD2D: Button
    private lateinit var startExperiment: Button
    private lateinit var stopExperiment: Button

    private var viewInstanceState: Bundle ?= null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceId: TextView = view.findViewById(R.id.dashboard_device_id)
        initD2D = view.findViewById(R.id.dashboard_start_d2d)
        val selectedRole: MaterialButtonToggleGroup = view.findViewById(R.id.dashboard_role)
        startExperiment = view.findViewById(R.id.dashboard_start_experiment)
        stopExperiment = view.findViewById(R.id.dashboard_stop_experiment)
        val endDevicesDiscovered: Button = view.findViewById(R.id.dashboard_playerList)

        startExperiment.isEnabled = false
        stopExperiment.isEnabled = false

        val dashboardAdapter = EntityAdapterDoubleColumn(DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView, EntityType.CustomQueries)
        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.dashboard_recyclerView),
            dashboardAdapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()
        viewModel.getAllExperimentsName().observe(requireActivity(), dashboardAdapter::submitList)


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

        startExperiment.setOnClickListener {
            isExperimentRunning = true
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

    private fun stopExperiment(){
        isExperimentRunning = false
        initD2D.isEnabled = true
        startExperiment.isEnabled = false
        stopExperiment.isEnabled = false
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
}