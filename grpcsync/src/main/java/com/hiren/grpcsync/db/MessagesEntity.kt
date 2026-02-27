package com.hiren.grpcsync.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hiren.grpcsync.utils.MessageType

@Entity(
    tableName = "messages", indices = [
        Index("messageId"),
        Index("timestamp"),
        Index("type"),
        Index("receiverDeviceId")
    ]
)
class MessagesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val receiverDeviceId: String,
    val messageId: Long = 0,
    val content: String,
    val timestamp: Long,
    val type: String,
    val messageType: MessageType,
    val retryCount: Int = 0,
    val lastSyncTime: Long = 0,
    val reason: String = ""
)

data class MessageWithDevice(
    val id: Int,
    val messageId: Long,
    val content: String,
    val timestamp: Long,
    val type: String,
    val retryCount: Int,
    val lastSyncTime: Long,
    val reason: String,
    val ip: String?,     // from DeviceEntity.id
    val port: Int?       // from DeviceEntity.port
)