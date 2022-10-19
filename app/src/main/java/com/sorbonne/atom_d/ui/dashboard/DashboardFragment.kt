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
import com.sorbonne.atom_d.MainActivity
import com.sorbonne.atom_d.guard
import com.sorbonne.d2d.D2DListener
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.gms.nearby.connection.Strategy
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.double_column.AdapterCategoryType
import com.sorbonne.atom_d.adapters.double_column.FullExperimentsAdapter
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.tools.CustomRecyclerView

class DashboardFragment : Fragment(), D2DListener {

    companion object {
        fun newInstance() = DashboardFragment()
    }

    private lateinit var viewModel: DashboardViewModel
    private val strategy = Strategy.P2P_STAR

    private var discoveryState: Chip?= null
    private var connectionState: Chip?= null

    private lateinit var initD2D: Button
    private lateinit var startExperiment: Button
    private lateinit var stopExperiment: Button

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

        startExperiment.isEnabled = false
        stopExperiment.isEnabled = false

        val dashboardAdapter = FullExperimentsAdapter(AdapterCategoryType.RADIOBUTTON_TEXTVIEW)
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
    }


    override fun onDiscoveryChange(active: Boolean) {
        super.onDiscoveryChange(active)
        Log.e(tag, active.toString())
        if(active){
            discoveryState?.setChipBackgroundColorResource(R.color.light_coral)
        }else{
            discoveryState?.setChipBackgroundColorResource(R.color.light_grey)
        }
    }

    override fun onConnectivityChange(active: Boolean) {
        super.onConnectivityChange(active)
        if(active){
            if(!isExperimentRunning){
                startExperiment.isEnabled = true
            }
            connectionState?.setChipBackgroundColorResource(R.color.light_coral)
            viewModel.instance?.stopDiscoveringOrAdvertising()
        }else{
            if(!isExperimentRunning){
                stopExperiment()
            }
            connectionState?.setChipBackgroundColorResource(R.color.light_grey)
        }
    }

    private fun stopExperiment(){
        isExperimentRunning = false
        initD2D.isEnabled = true
        startExperiment.isEnabled = false
        stopExperiment.isEnabled = false
    }
}