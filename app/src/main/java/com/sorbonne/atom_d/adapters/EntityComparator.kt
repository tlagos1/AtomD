package com.sorbonne.atom_d.adapters

import androidx.recyclerview.widget.DiffUtil
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperiments
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.entities.file_experiments.FileExperiments

enum class AdapterType {
    ChunkExperiments,
    FileExperiments,
    ConnectionAttempts,
    CustomQueries,
    DynamicList
}

class EntityComparator(private val adapterType: AdapterType) : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when(adapterType){
            AdapterType.ChunkExperiments ->
                (oldItem as ChunkExperiments).expName == (newItem as ChunkExperiments).expName
            AdapterType.FileExperiments ->
                (oldItem as FileExperiments).expName == (newItem as FileExperiments).expName
            AdapterType.ConnectionAttempts ->
                (oldItem as ConnectionAttempts).expName == (newItem as ConnectionAttempts).expName
            AdapterType.CustomQueries ->
                (oldItem as CustomQueriesDao.AllExperimentsName).experiment_name == (newItem as CustomQueriesDao.AllExperimentsName).experiment_name
            else -> {
                false
            }
        }
    }
}