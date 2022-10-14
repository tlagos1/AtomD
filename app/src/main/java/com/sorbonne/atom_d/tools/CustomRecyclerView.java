package com.sorbonne.atom_d.tools;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

@SuppressLint("ViewConstructor")
public class CustomRecyclerView extends RecyclerView{


    public enum CustomLayoutManager{
        LINEAR_LAYOUT
    }

    RecyclerView mRecyclerView;
    Adapter<?> mAdapter;
    LayoutManager mLayout;


    public CustomRecyclerView(@NonNull @NotNull Context context, RecyclerView recyclerView, Adapter<?> adapter, CustomLayoutManager layout) {
        super(context);
        mRecyclerView = recyclerView;
        mAdapter = adapter;
        switch (layout){
            case LINEAR_LAYOUT:
                mLayout = new LinearLayoutManager(context);
                break;
        }
    }

    public RecyclerView getRecyclerView()
    {
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayout);

        return mRecyclerView;
    }
}
