package com.sorbonne.atom_d.adapters.double_column

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.EntityComparator
import com.sorbonne.atom_d.adapters.EntityType
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder

class EntityAdapterDoubleColumn(
        private val doubleColumnType: DoubleColumnViewHolder.DoubleColumnType,
        private val entityType: EntityType
    ): ListAdapter<Any, DoubleColumnViewHolder>(EntityComparator(entityType)) {

    private var lastCheckedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoubleColumnViewHolder {
        return when(doubleColumnType){
            DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView ->
                DoubleColumnViewHolder.create(parent, DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView)
            DoubleColumnViewHolder.DoubleColumnType.TextViewTextView ->
                DoubleColumnViewHolder.create(parent, DoubleColumnViewHolder.DoubleColumnType.TextViewTextView)
        }
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: DoubleColumnViewHolder, position: Int) {
        val current = getItem(position)
        when(entityType){
            EntityType.CustomQueries ->{
                current as CustomQueriesDao.AllExperimentsName
                when(doubleColumnType){
                    DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView -> {
                        holder.bind(current.experiment_name, current.type, doubleColumnType,position == lastCheckedPosition)
                        val item: RadioButton = holder.itemView.findViewById(R.id.Data_Radio)
                        item.setOnClickListener{
                            notifyItemChanged(lastCheckedPosition)
                            notifyItemChanged(position)
                            lastCheckedPosition = position
                        }
                    }
                    DoubleColumnViewHolder.DoubleColumnType.TextViewTextView -> {
                        holder.bind(current.experiment_name, current.type, doubleColumnType)
                    }
                }
            }
            else -> {}
        }
    }

    fun getLastCheckedPosition(): Int {
        return lastCheckedPosition
    }
}