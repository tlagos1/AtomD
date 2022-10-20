package com.sorbonne.atom_d.view_holders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.R


class DoubleColumnViewHolder(itemView: View, type: DoubleColumnType): RecyclerView.ViewHolder(itemView)  {

    enum class DoubleColumnType{
        TextViewTextView,
        RadioButtonTextView
    }

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

    fun bind(data: Any, data2: Any, type: DoubleColumnType, isChecked: Boolean = false){
        when(type){
            DoubleColumnType.TextViewTextView -> {
                textViewData.text = data as String
                secondTextViewData.text = data2 as String
            }
            DoubleColumnType.RadioButtonTextView -> {
                radioData.isChecked = isChecked
                radioData.text = data as String
                textViewData.text = data2 as String
            }
        }
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