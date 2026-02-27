package com.hiren.grpcsync.repo

import com.hiren.grpcsync.db.MessageWithDevice
import com.hiren.grpcsync.db.MessagesEntity

interface MessageRepository {

    suspend fun getMessageList(): List<MessagesEntity>

    suspend fun upsertMessage(message: MessagesEntity)

    suspend fun deleteByMessageId(messageId: Long)

    suspend fun deleteOlderThan(olderThan: Long)

    suspend fun getBroadcastMessagesByMessageId(messageId: Long): List<MessagesEntity>

    suspend fun getMessagesWithDevice(): List<MessageWithDevice>
}
