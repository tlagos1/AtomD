package com.sorbonne.atom_d.ui.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.double_column.AdapterCategoryType
import com.sorbonne.atom_d.adapters.double_column.FullExperimentsAdapter
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.ui.CustomRecyclerView

class DeleteExperimentFragment : Fragment()  {

    private val viewModel: ExperimentViewModel by viewModels {
        ExperimentViewModelFactory(DatabaseRepository(requireActivity().application))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_delete_experiment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deleteExperimentAdapter = FullExperimentsAdapter(AdapterCategoryType.RADIOBUTTON_TEXTVIEW)
        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.Delete_Experiment_RecyclerView),
            deleteExperimentAdapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()

        viewModel.getAllExperimentsName().observe(requireActivity(), deleteExperimentAdapter::submitList)
    }
}