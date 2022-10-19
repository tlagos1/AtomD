package com.sorbonne.atom_d.ui.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.double_column.AdapterCategoryType
import com.sorbonne.atom_d.adapters.double_column.FullExperimentsAdapter
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.tools.MyAlertDialog

class DeleteExperimentFragment : Fragment()  {

    private val TAG = DeleteExperimentFragment::class.simpleName

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

        val submitButton = view.findViewById<Button>(R.id.Delete_Experiment_Submit)

        submitButton.setOnClickListener {
            if(deleteExperimentAdapter.getLastCheckedPosition() > -1 &&
                deleteExperimentAdapter.getLastCheckedPosition() < deleteExperimentAdapter.currentList.size){
                val experimentName =
                    deleteExperimentAdapter.currentList[deleteExperimentAdapter.getLastCheckedPosition()].experiment_name
                val experimentType =
                    deleteExperimentAdapter.currentList[deleteExperimentAdapter.getLastCheckedPosition()].type
                val experimentSize: Long =
                    deleteExperimentAdapter.currentList[deleteExperimentAdapter.getLastCheckedPosition()].size

                MyAlertDialog.showDialog(
                    parentFragmentManager,
                    TAG,
                    "Delete Experiment",
                    "You are about to delete $experimentName. Do you want to continue?",
                    R.drawable.ic_alert_dialog_info_24,
                    false,
                    MyAlertDialog.MESSAGE_TYPE.ALERT_OPTION,
                    {
                        when(experimentType) {
                            "CHUNK" ->
                                viewModel.deleteChunkExperiment(experimentName)

                            "FILE" ->
                                viewModel.deleteFileExperiment(experimentName)

                            "DISCOVERY" ->
                                viewModel.deleteConnectionAttempts(experimentName)
                        }
                    },
                    null
                )
            }
        }
    }
}