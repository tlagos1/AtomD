package com.sorbonne.atom_d.adapters.double_column

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.widget.CheckBox
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

    private var TAG = EntityAdapterDoubleColumn::class.simpleName

    private var lastCheckedPosition = -1
    private val checkedBoxes = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoubleColumnViewHolder {
        return DoubleColumnViewHolder.create(parent, doubleColumnType)
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
                    else -> {
                        TODO()
                    }
                }
            }
            EntityType.DynamicList -> {
                when(doubleColumnType) {

                    DoubleColumnViewHolder.DoubleColumnType.CheckBoxTextView -> {
                        current as List<*>
                        holder.bind(current[0], current[1], doubleColumnType, checkedBoxes.contains(current[0].toString()))
                        val item: CheckBox = holder.itemView.findViewById(R.id.Data_checkBox)
                        item.setOnClickListener{ mCheckBox ->
                            mCheckBox as CheckBox
                            if(mCheckBox.isChecked){
                                if(!checkedBoxes.contains(current[0].toString())){
                                    checkedBoxes.add(current[0].toString())
                                }
                            } else {
                                checkedBoxes.remove(current[0].toString())
                            }
                        }
                    }
                    else -> {
                        TODO()
                    }
                }
            }
            else -> {
                TODO()
            }
        }
    }

    override fun onCurrentListChanged(previousList: MutableList<Any>, currentList: MutableList<Any>) {
        super.onCurrentListChanged(previousList, currentList)
        when(entityType){
            EntityType.DynamicList ->{
                val currentListIds = mutableListOf<String>()
                currentList.forEach {
                    it as List<*>
                    currentListIds.add(it[0].toString())
                }
                for (index in 0 until checkedBoxes.size){
                    if(!currentListIds.contains(checkedBoxes[index])){
                        checkedBoxes.removeAt(index)
                        break
                    }
                }
            }
            else ->{

            }
        }

    }

    fun getLastCheckedPosition(): Int {
        return lastCheckedPosition
    }
    fun getCheckedBoxes(): List<String> {
        return checkedBoxes
    }
}