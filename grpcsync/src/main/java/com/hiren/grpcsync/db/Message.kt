package com.hiren.grpcsync.db

import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse

data class Message(
    val messageId: Long = 0,  // Auto-generated primary key
    val senderId: String, // Sender device IP
    val receiverId: String, // Receiver device IP
    val content: String, // Actual Message content
    val timestamp: Long, // Time when the message was sent
    val status: Boolean // e.g., "sent", "delivered", "read"
) {
    fun toGrpcRequest(): ChatRequest {
        return ChatRequest.newBuilder()
            .setMessageId(messageId)
            .setSenderId(senderId)
            .setReceiverId(receiverId)
            .setContent(content)
            .setTimestamp(timestamp)
            .setStatus(status)
            .build()
    }
}

data class MessageResponse(
    val received: Boolean,
    val info: String,
) {
    fun toGrpcRequest(): ChatResponse {
        return ChatResponse.newBuilder()
            .setReceived(received)
            .setInfo(info)
            .build()
    }
}