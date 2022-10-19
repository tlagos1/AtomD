package com.sorbonne.atom_d.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.sorbonne.atom_d.MainActivity
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.single_column.SimpleSingleColumnAdapter
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.guard
import com.sorbonne.d2d.D2DListener

class EndpointsDiscoveredFragment: Fragment(),  D2DListener{

    private val TAG = EndpointsDiscoveredFragment::class.simpleName

    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, DashboardViewModel.Factory(context, DatabaseRepository(requireActivity().application)))[DashboardViewModel::class.java]
        viewModel.instance = (context as? MainActivity).guard { return }.d2d
        viewModel.deviceId = (context as? MainActivity).guard { return }.androidId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_endpoints_discovered, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val endpointsDiscoveredAdapter = SimpleSingleColumnAdapter
    }
}