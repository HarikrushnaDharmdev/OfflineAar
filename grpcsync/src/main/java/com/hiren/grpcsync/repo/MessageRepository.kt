package com.hiren.grpcsync.repo

import com.hiren.grpcsync.db.MessageDao
import com.hiren.grpcsync.db.MessageEntity
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val dao: MessageDao) {
    fun getMessages(channelIds: List<String>): Flow<List<MessageEntity>> = dao.getMessages(channelIds)

    suspend fun addMessage(message: MessageEntity) = dao.addMessage(message)
    suspend fun updateMessageStatus(channelId: String, status: Boolean) {
        dao.updateMessageStatus(channelId, status)
    }
}
