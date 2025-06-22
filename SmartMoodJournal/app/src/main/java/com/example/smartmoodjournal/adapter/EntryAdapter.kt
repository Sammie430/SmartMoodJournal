package com.example.smartmoodjournal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmoodjournal.R

class EntryAdapter(private val entries: List<String>) :
    RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val entryText: TextView = itemView.findViewById(R.id.entryText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.entryText.text = entries[position]
    }

    override fun getItemCount(): Int = entries.size
}
