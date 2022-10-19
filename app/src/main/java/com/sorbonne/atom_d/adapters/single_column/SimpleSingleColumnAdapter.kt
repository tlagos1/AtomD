package com.sorbonne.atom_d.adapters.single_column

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.view_holders.SingleColumnType
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder

class SimpleSingleColumnAdapter(private val endPoints: List<String>): RecyclerView.Adapter<SingleColumnViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleColumnViewHolder {
        return try {
            SingleColumnViewHolder.create(parent, SingleColumnType.TextView)
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }
    }

    override fun onBindViewHolder(holder: SingleColumnViewHolder, position: Int) {
        holder.bind(endPoints[position])
    }

    override fun getItemCount(): Int {
        return endPoints.size
    }
}