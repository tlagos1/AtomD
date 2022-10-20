package com.sorbonne.atom_d.adapters.single_column

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.view_holders.SingleColumnType
import com.sorbonne.atom_d.view_holders.SingleColumnViewHolder
import org.json.JSONObject

class SimpleSingleColumnAdapter(private val type: SingleColumnType, private val endPointsList: List<Any>) : RecyclerView.Adapter<SingleColumnViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleColumnViewHolder {
        return try {
            SingleColumnViewHolder.create(parent, type)
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message)
        }
    }

    override fun onBindViewHolder(holder: SingleColumnViewHolder, position: Int) {
        holder.bind(endPointsList[position], type)
    }

    override fun getItemCount(): Int {
        return endPointsList.size
    }
}