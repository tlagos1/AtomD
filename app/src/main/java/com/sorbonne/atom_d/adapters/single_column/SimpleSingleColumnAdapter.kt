package com.sorbonne.atom_d.adapters.single_column

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder

class SimpleSingleColumnAdapter(
        private val type: SingleColumnViewHolder.SingleColumnType,
        private val endPoints: List<Any>
    ): RecyclerView.Adapter<SingleColumnViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleColumnViewHolder {
        return try {
            SingleColumnViewHolder.create(parent, SingleColumnViewHolder.SingleColumnType.TextView)
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }
    }

    override fun onBindViewHolder(holder: SingleColumnViewHolder, position: Int) {
        holder.bind(endPoints[position], type)
    }

    override fun getItemCount(): Int {
        return endPoints.size
    }
}