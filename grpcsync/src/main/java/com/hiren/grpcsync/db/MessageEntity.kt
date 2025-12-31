package com.hiren.grpcsync.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,  // Auto-generated primary key
    val channelId: String,  // e.g., conversation or chat ID mixture of sender and receiver IDs
    val senderId: String, // Sender device IP
    val receiverId: String, // Receiver device IP
    val content: String, // Actual Message content
    val timestamp: Long, // Time when the message was sent
    val status: Boolean // e.g., "sent", "delivered", "read"
)