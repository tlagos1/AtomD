package com.sorbonne.atom_d.view_holders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.R



class SingleColumnViewHolder(itemView: View, type: SingleColumnType) : RecyclerView.ViewHolder(itemView) {

    enum class SingleColumnType {
        TextView, RadioButton
    }

    private lateinit var textViewData: TextView
    private lateinit var radioData: RadioButton

    init{
        when(type){
            SingleColumnType.TextView ->
                textViewData = itemView.findViewById(R.id.Data_TextView)
            SingleColumnType.RadioButton ->
                radioData = itemView.findViewById(R.id.Data_Radio)
        }
    }

    fun bind(data: Any, type: SingleColumnType, isChecked: Boolean = false){
        when(type){
            SingleColumnType.TextView ->{
                data as String
                textViewData.text = data
            }
            SingleColumnType.RadioButton -> {
                data as String
                radioData.isChecked = isChecked
                radioData.text = data
            }
        }
    }


    companion object {
        fun create(parent: ViewGroup, type: SingleColumnType): SingleColumnViewHolder {
            val view: View = when (type) {
                SingleColumnType.TextView ->
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.textview_single_column, parent, false)
                SingleColumnType.RadioButton ->
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.radiobutton_single_column, parent, false)
            }
            return SingleColumnViewHolder(view, type)
        }
    }
}