package com.sorbonne.atom_d.ui.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.EntityType
import com.sorbonne.atom_d.adapters.single_column.EntityAdapterSingleColumn
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperiments
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder
import kotlin.math.pow

class ChunksMessageFragment : Fragment()  {

    private val viewModel: ExperimentViewModel by viewModels {
        ExperimentViewModelFactory(DatabaseRepository(requireActivity().application))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chunks_message_parameters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val experimentName = view.findViewById<TextView>(R.id.Message_Experiment_Name)
        val attempts = view.findViewById<TextView>(R.id.Message_Experiment_Attempts)
        val size = view.findViewById<Spinner>(R.id.Message_Experiment_Size)
        val customName = view.findViewById<CheckBox>(R.id.Message_Experiment_Custom_Name)
        val submitButton = view.findViewById<Button>(R.id.Message_Experiment_Submit)

        val chunkExperimentsAdapter = EntityAdapterSingleColumn(SingleColumnViewHolder.SingleColumnType.TextView, EntityType.ChunkExperiments)
        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.Message_Experiment_RecyclerView),
            chunkExperimentsAdapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()
        viewModel.getAllChunks().observe(requireActivity(), chunkExperimentsAdapter::submitList)

        val experimentSizeSet = ArrayList(listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
        val arrayAdapterForSize =
            ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_item, experimentSizeSet)
        arrayAdapterForSize.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        size.adapter = arrayAdapterForSize

        customName.setOnClickListener {
            experimentName.isEnabled = customName.isChecked
        }

        submitButton.setOnClickListener {
            var chunkExperiments: ChunkExperiments? = null
            if (attempts.textSize > 0) {
                if (customName.isChecked) {
                    if (experimentName.textSize > 0) {
                        chunkExperiments = ChunkExperiments(
                            0,
                            experimentName.text.toString(),
                            2.0.pow(size.selectedItem.toString().toInt().toDouble()).toInt(),
                            attempts.text.toString().toInt(),
                            null
                        )
                    }
                } else {
                    chunkExperiments = ChunkExperiments(
                        0,
                        "Chunk - 2^${size.selectedItem} bytes N - ${attempts.text} ",
                        2.0.pow(size.selectedItem.toString().toInt().toDouble()).toInt(),
                        attempts.text.toString().toInt(),
                        null
                    )
                }
            }
            if (chunkExperiments != null) {
                experimentName.text = ""
                size.setSelection(0)
                attempts.text = ""
                viewModel.insertChunkExperiment(chunkExperiments)
            }
        }
    }
}