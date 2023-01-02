package com.sorbonne.atom_d.adapters.double_column

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import androidx.recyclerview.widget.ListAdapter
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.adapters.EntityComparator
import com.sorbonne.atom_d.adapters.AdapterType
import com.sorbonne.atom_d.entities.custom_queries.CustomQueriesDao
import com.sorbonne.atom_d.view_holders.DoubleColumnViewHolder

class AdapterDoubleColumn(
        private val doubleColumnType: DoubleColumnViewHolder.DoubleColumnType,
        private val adapterType: AdapterType
    ): ListAdapter<Any, DoubleColumnViewHolder>(EntityComparator(adapterType)) {

    private var TAG = AdapterDoubleColumn::class.simpleName

    private var lastCheckedPosition = mutableMapOf<String, Int>()
    private val checkedBoxes = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoubleColumnViewHolder {
        return DoubleColumnViewHolder.create(parent, doubleColumnType)
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: DoubleColumnViewHolder, position: Int) {
        val current = getItem(position)
        when(adapterType){
            AdapterType.CustomQueries ->{
                current as CustomQueriesDao.AllExperimentsName
                when(doubleColumnType){
                    DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView -> {
                        holder.bind(current.experiment_name, current.type, doubleColumnType,position == lastCheckedPosition[adapterType.name])
                        val item: RadioButton = holder.itemView.findViewById(R.id.Data_Radio)
                        item.setOnClickListener{
                            lastCheckedPosition[adapterType.name]?.let { mLastCheckedPosition ->
                                notifyItemChanged(mLastCheckedPosition)
                            }
                            notifyItemChanged(position)
                            lastCheckedPosition[adapterType.name] = position

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
            AdapterType.DynamicList -> {
                when(doubleColumnType) {
                    DoubleColumnViewHolder.DoubleColumnType.CheckBoxTextView -> {
                        current as List<*>
                        holder.bind(current,  doubleColumnType, checkedBoxes.contains(current[0].toString()))
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
                    DoubleColumnViewHolder.DoubleColumnType.RadioButtonTextView -> {
                        current as List<*>
                        holder.bind(current, doubleColumnType, position == lastCheckedPosition[adapterType.name])
                        val item: RadioButton = holder.itemView.findViewById(R.id.Data_Radio)
                        item.setOnClickListener{
                            lastCheckedPosition[adapterType.name]?.let { mLastCheckedPosition ->
                                notifyItemChanged(mLastCheckedPosition)
                            }
                            notifyItemChanged(position)
                            lastCheckedPosition[adapterType.name] = position

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
        when(adapterType){
            AdapterType.DynamicList ->{
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

    fun updateBandwidthItem(endPointId: String, data: Int){
        for(index in 0 until itemCount){
            val item = getItem(index) as MutableList<String>
            if(item[0] == endPointId){
                item[2] = data.toString()
                notifyItemChanged(index, item)
            }
        }
    }

    fun getLastCheckedPosition(): Int {
        lastCheckedPosition[adapterType.name]?.let {
            return it
        }
        return -1
    }
    fun getCheckedBoxes(): List<String> {
        return checkedBoxes
    }
}