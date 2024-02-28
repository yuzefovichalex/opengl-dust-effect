package com.alexyuzefovich.dusteffect.sample.adapter

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alexyuzefovich.dusteffect.sample.components.AndyView
import com.alexyuzefovich.dusteffect.sample.model.Andy

class AndyAdapter(
    private val onItemClick: (Int, View) -> Unit
) : ListAdapter<Andy, AndyAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val andyView = AndyView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        val viewHolder = ViewHolder(andyView)

        andyView.setOnClickListener {
            onItemClick(viewHolder.adapterPosition, it)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }


    class ViewHolder(
        private val andyView: AndyView
    ) : RecyclerView.ViewHolder(andyView) {

        fun bind(andy: Andy) {
            andyView.bind(andy)
        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<Andy>() {
        override fun areItemsTheSame(oldItem: Andy, newItem: Andy): Boolean =
            oldItem.imageResId == newItem.imageResId

        override fun areContentsTheSame(oldItem: Andy, newItem: Andy): Boolean =
            oldItem == newItem
    }

}