/**
 * -----------------------------------------------------------------------------
 * File Name     : Message.kt
 * Description   : Data models used for gRPC chat communication between devices.
 *                 Contains request and response mapping helpers.
 *
 * Created On    : 24-02-2026
 * Author        : Hiren Patel
 * -----------------------------------------------------------------------------
 */
package com.hiren.grpcsync.db

import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse
import com.hiren.grpcsync.utils.MessageType
import java.util.Calendar

/**
 * Represents a chat/message entity exchanged between devices.
 *
 * @property messageId Unique identifier of the message (can be auto-generated if stored locally).
 * @property senderId Identifier of the sender device (e.g., device IP).
 * @property receiverId Identifier of the receiver device (e.g., device IP).
 * @property content Actual message payload/content.
 * @property timestamp Time at which the message was created/sent (epoch millis).
 * @property status Delivery status of the message (true/false based on your logic).
 * @property type Type of operation (e.g., "EDIT", "ADD", "DELETE").
 */
data class Message(
    val messageId: Long = 0,  // Auto-generated primary key
    val senderId: String, // Sender device IP
    val receiverId: String, // Receiver device IP
    val receiverDeviceId: String, // Receiver device IP
    val content: String, // Actual Message content
    val timestamp: Long, // Time when the message was sent
    val status: Boolean, // e.g., "sent", "delivered", "read"
    val type: String // Message type such as "EDIT", "ADD", "DELETE"
) {

    /**
     * Converts this Message model into a gRPC ChatRequest object.
     *
     * @return ChatRequest built from current Message instance.
     */
    fun toGrpcRequest(): ChatRequest {
        return ChatRequest.newBuilder()
            .setMessageId(messageId)
            .setSenderId(senderId)
            .setReceiverId(receiverId)
            .setReceiverDeviceId(receiverDeviceId)
            .setContent(content)
            .setTimestamp(timestamp)
            .setStatus(status)
            .setType(type)
            .build()
    }

    fun toMessageEntity(
        lastSyncTime: Long = Calendar.getInstance().timeInMillis,
        reason: String = "-",
        retryCount: Int = 0,
        messageType: MessageType
    ): MessagesEntity {
        return MessagesEntity(
            id = 0,
            receiverDeviceId = receiverDeviceId,
            messageId = messageId,
            content = content,
            timestamp = timestamp,
            type = type,
            retryCount = retryCount,
            lastSyncTime = lastSyncTime,
            reason = reason,
            messageType = messageType
        )
    }
}


/**
 * Represents the response received after sending a message via gRPC.
 *
 * @property received Indicates whether the message was successfully received.
 * @property info Additional information or acknowledgment message.
 * @property type Type of operation (e.g., "EDIT", "ADD", "DELETE").
 */
data class MessageResponse(
    val received: Boolean,
    val info: String,
    val type: String // Message type such as "EDIT", "ADD", "DELETE"
) {

    /**
     * Converts this MessageResponse model into a gRPC ChatResponse object.
     *
     * @return ChatResponse built from current MessageResponse instance.
     */
    fun toGrpcRequest(): ChatResponse {
        return ChatResponse.newBuilder()
            .setReceived(received)
            .setInfo(info)
            .setType(type)
            .build()
    }
}