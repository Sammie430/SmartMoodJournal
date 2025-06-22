package com.example.smartmoodjournal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmoodjournal.R
import com.example.smartmoodjournal.model.Message
import com.google.firebase.auth.FirebaseAuth

class MessagesAdapter(private val messageList: List<Message>) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = if (viewType == VIEW_TYPE_SENT) {
            LayoutInflater.from(parent.context).inflate(R.layout.chat_item_sent, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.chat_item_received, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.messageText.text = message.message
    }

    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}
