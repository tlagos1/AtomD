package com.sorbonne.atom_d.viewHolders;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sorbonne.atom_d.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class RelaySelectionColumnViewHolder extends RecyclerView.ViewHolder {

    private final TextView
            deviceId,
            connectionDelay,
            batteryLife,
            throughput,
            rank;

    public RelaySelectionColumnViewHolder(@NonNull View itemView) {
        super(itemView);
        deviceId = itemView.findViewById(R.id.relay_selection_recycleview_id);
        connectionDelay = itemView.findViewById(R.id.relay_selection_recycleview_cn_dl);
        batteryLife = itemView.findViewById(R.id.relay_selection_recycleview_bttr);
        throughput = itemView.findViewById(R.id.relay_selection_recycleview_t_put);
        rank = itemView.findViewById(R.id.relay_selection_recycleview_rank);
    }

    @SuppressLint("DefaultLocale")
    public void bind(JSONObject deviceInfo) throws JSONException {
        this.deviceId.setText(deviceInfo.getString("deviceId"));
        this.connectionDelay.setText(String.format("%.2f",deviceInfo.getDouble("connectionDelay")));
        this.batteryLife.setText(String.valueOf(deviceInfo.getInt("batteryLife")));
        this.throughput.setText(String.format("%.2f",deviceInfo.getDouble("throughput")));
        this.rank.setText(String.format("%.2f",deviceInfo.getDouble("rank")));
    }

    public static RelaySelectionColumnViewHolder create(ViewGroup parent){
        View view;
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.relayselection_recyclerview_column, parent,false);
        return  new RelaySelectionColumnViewHolder(view);
    }
}
