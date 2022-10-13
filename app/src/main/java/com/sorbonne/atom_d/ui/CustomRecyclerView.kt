package com.sorbonne.atom_d.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("ViewConstructor")
class CustomRecyclerView(context: Context, recyclerView: RecyclerView, adapter: Adapter<*>, layout: CustomLayoutManager):
    RecyclerView(context){

    enum class CustomLayoutManager {
        LINEAR_LAYOUT
    }

    private var mRecyclerView: RecyclerView
    private var mAdapter: Adapter<*>
    private var mLayout: LayoutManager ?= null

    init {
        mRecyclerView = recyclerView
        mAdapter = adapter
        when (layout) {
            CustomLayoutManager.LINEAR_LAYOUT -> mLayout = LinearLayoutManager(context)
        }
    }

    fun getRecyclerView(): RecyclerView {
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = mLayout
        return mRecyclerView
    }
}