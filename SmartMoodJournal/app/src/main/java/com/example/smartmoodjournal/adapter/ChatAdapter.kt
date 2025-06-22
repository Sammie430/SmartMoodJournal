package com.example.smartmoodjournal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userTextView: TextView = view.findViewById(R.id.userTextView)
        val aiTextView: TextView = view.findViewById(R.id.aiTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUser) {
            holder.userTextView.visibility = View.VISIBLE
            holder.aiTextView.visibility = View.GONE
            holder.userTextView.text = message.message
        } else {
            holder.userTextView.visibility = View.GONE
            holder.aiTextView.visibility = View.VISIBLE
            holder.aiTextView.text = message.message
        }
    }

    override fun getItemCount(): Int = messages.size
}
