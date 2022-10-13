package com.sorbonne.atom_d.adapters.double_column


import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.view_holders.DoubleColumnType
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder

enum class AdapterCategoryType {
    RADIOBUTTON_TEXTVIEW, TEXTVIEW_TEXTVIEW
}

class FullExperimentsAdapter(private val type: AdapterCategoryType): ListAdapter<CustomQueriesDao.AllExperimentsName, DoubleColumnViewHolder>(Comparator()){

    private var lastCheckedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoubleColumnViewHolder {
        return when(type){
            AdapterCategoryType.RADIOBUTTON_TEXTVIEW ->
                DoubleColumnViewHolder.create(parent, DoubleColumnType.RadioButtonTextView)
            AdapterCategoryType.TEXTVIEW_TEXTVIEW ->
                DoubleColumnViewHolder.create(parent, DoubleColumnType.TextViewTextView)
        }
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: DoubleColumnViewHolder, position: Int) {
        val current = getItem(position)
        when(type){
            AdapterCategoryType.RADIOBUTTON_TEXTVIEW -> {
                holder.bind(position == lastCheckedPosition, current.experiment_name, current.type)
                val item: RadioButton = holder.itemView.findViewById(R.id.Data_Radio)
                item.setOnClickListener{
                    notifyItemChanged(lastCheckedPosition)
                    notifyItemChanged(position)
                    lastCheckedPosition = position
                }
            }
            AdapterCategoryType.TEXTVIEW_TEXTVIEW ->
                holder.bind(current.experiment_name, current.type)
        }
    }

    fun getLastCheckedPosition(): Int {
        return lastCheckedPosition
    }

    class Comparator : DiffUtil.ItemCallback<CustomQueriesDao.AllExperimentsName>() {
        override fun areItemsTheSame(oldItem: CustomQueriesDao.AllExperimentsName, newItem: CustomQueriesDao.AllExperimentsName): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: CustomQueriesDao.AllExperimentsName, newItem: CustomQueriesDao.AllExperimentsName): Boolean {
            return oldItem.experiment_name == newItem.experiment_name
        }
    }
}