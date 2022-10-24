package com.sorbonne.atom_d.ui.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.EntityType
import com.sorbonne.atom_d.adapters.double_column.EntityAdapterDoubleColumn
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder

class ExperimentFragment : Fragment() {

    companion object {
        fun newInstance() = ExperimentFragment()
    }

    private val viewModel: ExperimentViewModel by viewModels {
        ExperimentViewModelFactory(DatabaseRepository(requireActivity().application))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return  inflater.inflate(R.layout.fragment_experiment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toNewChunk = view.findViewById<Button>(R.id.experiment_new_chunk)
        val toNewFile = view.findViewById<Button>(R.id.experiment_new_file)
        val toConnectionAttempt = view.findViewById<Button>(R.id.experiment_new_discover_repetitions)
        val toDeleteExp = view.findViewById<Button>(R.id.experiment_delete_experiment)

        val experimentsAdapter = EntityAdapterDoubleColumn(DoubleColumnViewHolder.DoubleColumnType.TextViewTextView, EntityType.CustomQueries)

        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.Experiment_RecyclerView),
            experimentsAdapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()

        viewModel.getAllExperimentsName().observe(requireActivity(), experimentsAdapter::submitList)

        toNewChunk.setOnClickListener {
            val action: NavDirections =
                ExperimentFragmentDirections.actionExperimentFragmentToChunksMessageFragment()
            findNavController(view).navigate(action)
        }

        toNewFile.setOnClickListener {
            val action: NavDirections =
                ExperimentFragmentDirections.actionExperimentFragmentToFileParametersFragment()
            findNavController(view).navigate(action)
        }

        toConnectionAttempt.setOnClickListener {
            val action: NavDirections =
                ExperimentFragmentDirections.actionExperimentFragmentToConnectionAttemptsFragment()
            findNavController(view).navigate(action)
        }

        toDeleteExp.setOnClickListener {
            val action: NavDirections =
                ExperimentFragmentDirections.actionExperimentFragmentToDeleteExperimentFragment()
            findNavController(view).navigate(action)
        }
    }
}