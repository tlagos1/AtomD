package com.sorbonne.atom_d.adapters.single_column

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.sorbonne.atom_d.adapters.EntityComparator
import com.sorbonne.atom_d.adapters.AdapterType
import com.sorbonne.atom_d.entities.chunk_experiments.ChunkExperiments
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.entities.file_experiments.FileExperiments
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder


class EntityAdapterSingleColumn(
        private val singleColumnType: SingleColumnViewHolder.SingleColumnType,
        private val adapterType: AdapterType
    ): ListAdapter<Any, SingleColumnViewHolder> (EntityComparator(adapterType)) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleColumnViewHolder {
        return try {
            SingleColumnViewHolder.create(parent, singleColumnType)
        } catch (e: Exception){
            throw IllegalArgumentException(e.message)
        }
    }

    override fun onBindViewHolder(holder: SingleColumnViewHolder, position: Int) {
        val current = getItem(position)
        when(adapterType){
            AdapterType.ChunkExperiments ->
                holder.bind((current as ChunkExperiments).expName, singleColumnType)
            AdapterType.FileExperiments ->
                holder.bind((current as FileExperiments).expName, singleColumnType)
            AdapterType.ConnectionAttempts ->
                holder.bind((current as ConnectionAttempts).expName, singleColumnType)
            else -> {}
        }
    }
}