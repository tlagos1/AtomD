package com.sorbonne.atom_d.adapters

import androidx.recyclerview.widget.DiffUtil
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperiments
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.entities.file_experiments.FileExperiments

enum class EntityType {
    ChunkExperiments,
    FileExperiments,
    ConnectionAttempts,
    CustomQueries
}

class EntityComparator(private val entityType: EntityType) : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when(entityType){
            EntityType.ChunkExperiments ->
                (oldItem as ChunkExperiments).expName == (newItem as ChunkExperiments).expName
            EntityType.FileExperiments ->
                (oldItem as FileExperiments).expName == (newItem as FileExperiments).expName
            EntityType.ConnectionAttempts ->
                (oldItem as ConnectionAttempts).expName == (newItem as ConnectionAttempts).expName
            EntityType.CustomQueries ->
                (oldItem as CustomQueriesDao.AllExperimentsName).experiment_name == (newItem as CustomQueriesDao.AllExperimentsName).experiment_name
        }
    }
}