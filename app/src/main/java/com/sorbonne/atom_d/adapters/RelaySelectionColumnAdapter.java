package com.sorbonne.atom_d.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sorbonne.atom_d.viewHolders.RelaySelectionColumnViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class RelaySelectionColumnAdapter extends RecyclerView.Adapter<RelaySelectionColumnViewHolder> {

    private final List<JSONObject> mPlayersList;

    public RelaySelectionColumnAdapter(List<JSONObject> playersList){
        mPlayersList = playersList;
    }

    @NonNull
    @Override
    public RelaySelectionColumnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return RelaySelectionColumnViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RelaySelectionColumnViewHolder holder, int position) {
        try {
            holder.bind(mPlayersList.get(position));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return mPlayersList.size();
    }

}
