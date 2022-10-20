package com.sorbonne.atom_d.ui.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.EntityType
import com.sorbonne.atom_d.adapters.single_column.EntityAdapterSingleColumn
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder

class ConnectionAttemptsFragment : Fragment()  {

    private val viewModel: ExperimentViewModel by viewModels {
        ExperimentViewModelFactory(DatabaseRepository(requireActivity().application))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_connection_attempts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val experimentName = view.findViewById<TextView>(R.id.Discovery_Repetitions_Experiment_Name)
        val repetitions = view.findViewById<TextView>(R.id.Discovery_Repetitions_Experiment_tries)
        val customName = view.findViewById<CheckBox>(R.id.Discovery_Repetitions_Experiment_Custom_Name)
        val submitButton = view.findViewById<Button>(R.id.Discovery_Repetitions_submit_experiment)

        val connectionAttemptsAdapter = EntityAdapterSingleColumn(SingleColumnViewHolder.SingleColumnType.TextView, EntityType.ConnectionAttempts)
        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.Discovery_Repetitions_Experiment_RecyclerView),
            connectionAttemptsAdapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()
        viewModel.getAllConnectionAttempts().observe(requireActivity(), connectionAttemptsAdapter::submitList)

        submitButton.setOnClickListener {

            val mExperimentName: String
            var mRepetitions: Int
            var mDiscoveryRepetitions: ConnectionAttempts? = null

            if (repetitions.text.isNotEmpty()) {
                mExperimentName = experimentName.text.toString()
                mRepetitions = repetitions.text.toString().toInt()

                if (mRepetitions <= 0) {
                    mRepetitions = 1
                }

                if (customName.isChecked) {
                    if (experimentName.textSize > 0) {
                        mDiscoveryRepetitions =
                            ConnectionAttempts(0, mExperimentName, mRepetitions)
                    }
                } else {
                    mDiscoveryRepetitions = ConnectionAttempts(
                        0,
                        "Discovery_Repetitions - N $mRepetitions",
                        mRepetitions
                    )
                }
            } else {
                Toast.makeText(requireContext(), "complete the required fields", Toast.LENGTH_LONG)
                    .show()
            }
            if (mDiscoveryRepetitions != null) {
                experimentName.text = ""
                repetitions.text = ""
                viewModel.insertConnectionAttemptExperiment(mDiscoveryRepetitions)
                customName.isChecked = false
            }
        }

    }
}