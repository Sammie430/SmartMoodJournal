package com.example.smartmoodjournal.model

data class Message(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
