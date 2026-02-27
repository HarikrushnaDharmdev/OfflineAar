package com.hiren.grpcsync.repo

import com.hiren.grpcsync.db.MessageDao
import com.hiren.grpcsync.db.MessageWithDevice
import com.hiren.grpcsync.db.MessagesEntity

class MessageRepositoryImpl(private val messageDao: MessageDao) : MessageRepository {

    override suspend fun upsertMessage(message: MessagesEntity) {
        messageDao.upsertMessage(message)
    }

    override suspend fun getMessageList(): List<MessagesEntity> =
        messageDao.getMessageList()

    override suspend fun deleteByMessageId(messageId: Long) {
        messageDao.deleteByMessageId(messageId)
    }

    override suspend fun deleteOlderThan(olderThan: Long) {
        messageDao.deleteOlderThan(olderThan)
    }

    override suspend fun getBroadcastMessagesByMessageId(messageId: Long): List<MessagesEntity> =
        messageDao.getBroadcastMessagesByMessageId(messageId)

    override suspend fun getMessagesWithDevice(): List<MessageWithDevice> =
        messageDao.getMessagesWithDevice()
}