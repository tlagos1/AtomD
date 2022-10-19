package com.sorbonne.atom_d.ui.experiment

import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
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
import com.sorbonne.atom_d.adapters.single_column.FileExperimentsAdapter
import com.sorbonne.atom_d.entities.DatabaseRepository
import com.sorbonne.atom_d.entities.file_experiments.FileExperiments
import com.sorbonne.atom_d.tools.CustomRecyclerView
import com.sorbonne.atom_d.tools.MyInputFilter
import java.io.IOException
import kotlin.math.pow

class FileParametersFragment : Fragment()  {

    private val TAG = FileParametersFragment::class.simpleName

    private val viewModel: ExperimentViewModel by viewModels {
        ExperimentViewModelFactory(DatabaseRepository(requireActivity().application))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_file_parameters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val experimentName = view.findViewById<TextView>(R.id.File_Experiment_Name)
        val fileSize = view.findViewById<TextView>(R.id.File_Experiment_Size)
        val fileSizePowerOf = view.findViewById<TextView>(R.id.File_Experiment_Size_power_of)
        val fileTries = view.findViewById<TextView>(R.id.File_Experiment_tries)
        val customName = view.findViewById<CheckBox>(R.id.File_Experiment_Custom_Name)
        val submitButton = view.findViewById<Button>(R.id.File_Experiment_Submit)

        val fileExperimentsAdapter = FileExperimentsAdapter()
        CustomRecyclerView(
            requireContext(),
            view.findViewById(R.id.File_Experiment_RecyclerView),
            fileExperimentsAdapter,
            CustomRecyclerView.CustomLayoutManager.LINEAR_LAYOUT
        ).getRecyclerView()
        viewModel.getAllFileExperiments().observe(requireActivity(), fileExperimentsAdapter::submitList)

        fileSize.filters = arrayOf(MyInputFilter(1, 99))
        fileSizePowerOf.filters = arrayOf(MyInputFilter(1, 9))

        customName.setOnClickListener { v: View? ->
            experimentName.isEnabled = customName.isChecked
        }

        submitButton.setOnClickListener {
            val mExperimentName: String
            val mFileSize: Long
            var mFileTries: Int
            var mFile: FileExperiments? = null
            if (fileSize.text.isNotEmpty() && fileSizePowerOf.text.isNotEmpty() && fileTries.text.isNotEmpty()) {
                mExperimentName = experimentName.text.toString()

                mFileSize = fileSize.text.toString().toInt() * 10.0.pow(
                    fileSizePowerOf.text.toString().toInt().toDouble()
                ).toLong()

                mFileTries = fileTries.text.toString().toInt()
                if (mFileTries <= 0) {
                    mFileTries = 1
                }

                if (customName.isChecked) {
                    if (experimentName.textSize > 0) {
                        mFile = FileExperiments(0, mExperimentName, mFileSize, mFileTries)
                    }
                } else {
                    val sizeFormat =
                        Formatter.formatFileSize(context, mFileSize)
                    mFile = FileExperiments(
                        0,
                        "File_experiment - $sizeFormat - N $mFileTries",
                        mFileSize,
                        mFileTries
                    )
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "complete the required fields",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (mFile != null) {
                try {
                    experimentName.text = ""
                    fileSize.text = ""
                    fileSizePowerOf.text = ""
                    fileTries.text = ""
                    viewModel.insertFileExperiment(mFile)
                } catch (e: IOException) {
                    Log.e(TAG, e.message!!)
                }
            }
        }

    }
}