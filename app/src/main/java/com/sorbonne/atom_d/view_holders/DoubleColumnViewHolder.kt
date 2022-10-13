package com.sorbonne.atom_d.view_holders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.R

enum class DoubleColumnType{
    TextViewTextView,
    RadioButtonTextView
}

class DoubleColumnViewHolder(itemView: View, type: DoubleColumnType): RecyclerView.ViewHolder(itemView)  {

    private var textViewData: TextView
    private lateinit var secondTextViewData: TextView
    private lateinit var radioData: RadioButton

    init {
        when(type){
            DoubleColumnType.TextViewTextView ->{
                textViewData = itemView.findViewById(R.id.Data_TextView)
                secondTextViewData = itemView.findViewById(R.id.Second_Data_TextView)
            }
            DoubleColumnType.RadioButtonTextView ->{
                radioData = itemView.findViewById(R.id.Data_Radio)
                textViewData = itemView.findViewById(R.id.Data_TextView)
            }
        }
    }

    fun bind(isChecked: Boolean, radioText: String?, textViewText: String?) {
        radioData.isChecked = isChecked
        radioData.text = radioText
        textViewData.text = textViewText
    }

    fun bind(data: String?, data2: String?) {
        textViewData.text = data
        secondTextViewData.text = data2
    }

    companion object {
        fun create(parent: ViewGroup, type: DoubleColumnType): DoubleColumnViewHolder {
            val view = when (type) {
                DoubleColumnType.TextViewTextView ->
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.textview_textview_column, parent, false)
                DoubleColumnType.RadioButtonTextView ->
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.radiobutton_textview_column, parent, false)
            }
            return DoubleColumnViewHolder(view, type)
        }
    }
}