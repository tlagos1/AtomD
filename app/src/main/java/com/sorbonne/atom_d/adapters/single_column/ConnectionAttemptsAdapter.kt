package com.sorbonne.atom_d.adapters.single_column

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sorbonne.atom_d.entities.connections_attempts.ConnectionAttempts
import com.sorbonne.atom_d.view_holders.SingleColumnType
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder

class ConnectionAttemptsAdapter : ListAdapter<ConnectionAttempts, SingleColumnViewHolder>(Comparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleColumnViewHolder {
        return try {
            SingleColumnViewHolder.create(parent, SingleColumnType.TextView)
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }
    }

    override fun onBindViewHolder(holder: SingleColumnViewHolder, position: Int) {
        val current: ConnectionAttempts = getItem(position)
        holder.bind(current.expName)
    }

    class Comparator : DiffUtil.ItemCallback<ConnectionAttempts>() {
        override fun areItemsTheSame(oldItem: ConnectionAttempts, newItem: ConnectionAttempts): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: ConnectionAttempts, newItem: ConnectionAttempts): Boolean {
            return oldItem.expName == newItem.expName
        }
    }
}