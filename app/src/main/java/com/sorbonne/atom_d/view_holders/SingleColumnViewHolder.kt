package com.sorbonne.atom_d.view_holders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.R

enum class SingleColumnType {
    TextView, RadioButton
}

class SingleColumnViewHolder(itemView: View, type: SingleColumnType) : RecyclerView.ViewHolder(itemView) {

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

    fun bind(text: String?, isChecked: Boolean) {
        radioData.isChecked = isChecked
        radioData.text = text
    }

    fun bind(data: String?) {
        textViewData.text = data
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