package com.sorbonne.atom_d.ui.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.AdapterType
import com.sorbonne.atom_d.adapters.double_column.AdapterDoubleColumn
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.tools.MyAlertDialog
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder

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

        val deleteExperimentAdapter = AdapterDoubleColumn(DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView, AdapterType.CustomQueries)

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

                var experimentName: String
                var experimentType: String
                var experimentSize: Long

                deleteExperimentAdapter.currentList[deleteExperimentAdapter.getLastCheckedPosition()].let {
                    it as CustomQueriesDao.AllExperimentsName
                    experimentName = it.experiment_name
                    experimentType = it.type
                    experimentSize = it.size
                }

                MyAlertDialog.showDialog(
                    parentFragmentManager,
                    TAG!!,
                    MyAlertDialog.MessageType.ALERT_ACCEPT_CANCEL,
                    R.drawable.ic_alert_dialog_info_24,
                    "Delete Experiment",
                    "You are about to delete $experimentName. Do you want to continue?",
                    option1 = fun (_){
                        when(experimentType) {
                            "CHUNK" ->
                                viewModel.deleteChunkExperiment(experimentName)

                            "FILE" ->
                                viewModel.deleteFileExperiment(experimentName)

                            "DISCOVERY" ->
                                viewModel.deleteConnectionAttempts(experimentName)
                        }
                    }
                )
            }
        }
    }
}