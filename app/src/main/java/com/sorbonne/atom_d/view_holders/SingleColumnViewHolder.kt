package com.sorbonne.atom_d.view_holders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sorbonne.atom_d.R
import org.json.JSONObject
import org.w3c.dom.Text




class SingleColumnViewHolder(itemView: View, type: SingleColumnType) : RecyclerView.ViewHolder(itemView) {

    enum class SingleColumnType {
        TextView, RadioButton, RelaySelection
    }

    private lateinit var textViewData: TextView
    private lateinit var radioData: RadioButton

    private lateinit var deviceId: TextView
    private lateinit var connectionDelay: TextView
    private lateinit var batteryLife: TextView
    private lateinit var throughput: TextView
    private lateinit var rank: TextView

    init{
        when(type){
            SingleColumnType.TextView ->
                textViewData = itemView.findViewById(R.id.Data_TextView)
            SingleColumnType.RadioButton ->
                radioData = itemView.findViewById(R.id.Data_Radio)
            SingleColumnType.RelaySelection -> {
                deviceId = itemView.findViewById(R.id.relay_selection_recycleview_id)
                connectionDelay = itemView.findViewById(R.id.relay_selection_recycleview_cn_dl)
                batteryLife = itemView.findViewById(R.id.relay_selection_recycleview_bttr)
                throughput = itemView.findViewById(R.id.relay_selection_recycleview_t_put)
                rank = itemView.findViewById(R.id.relay_selection_recycleview_rank)
            }
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
            SingleColumnType.RelaySelection->{
                data as JSONObject
                deviceId.text = data.getString("deviceId")
                connectionDelay.text = String.format("%.2f", data.getDouble("connectionDelay"))
                batteryLife.text = data.getInt("batteryLife").toString()
                throughput.text = String.format("%.2f", data.getDouble("throughput"))
                rank.text = String.format("%.2f", data.getDouble("rank"))
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
                SingleColumnType.RelaySelection ->
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.relayselection_recyclerview_column, parent, false)
            }
            return SingleColumnViewHolder(view, type)
        }
    }
}