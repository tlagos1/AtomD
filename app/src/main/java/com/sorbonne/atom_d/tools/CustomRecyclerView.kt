package com.sorbonne.atom_d.tools

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.LayoutManager

class CustomRecyclerView(context: Context, private val recyclerView: RecyclerView, private val adapter: Adapter<*>, layout: CustomLayoutManager) {
    enum class CustomLayoutManager{
        LINEAR_LAYOUT
    }

    private lateinit var layoutManager: LayoutManager

    init {
        if(layout == CustomLayoutManager.LINEAR_LAYOUT){
            layoutManager = LinearLayoutManager(context)
        }
    }

    fun getRecyclerView(): RecyclerView {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        return recyclerView
    }

}