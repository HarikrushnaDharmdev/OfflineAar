package com.hiren.grpcsync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(device: MessagesEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp ASC LIMIT 10 ")
    suspend fun getMessageList(): List<MessagesEntity>

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: Long)

    @Query("DELETE FROM messages WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("SELECT * FROM messages WHERE type = 'BROADCAST' AND messageId = :messageId")
    suspend fun getBroadcastMessagesByMessageId(messageId: Long): List<MessagesEntity>

    @Query(
        """
    SELECT 
        m.id,
        m.messageId,
        m.content,
        m.timestamp,
        m.type,
        m.retryCount,
        m.lastSyncTime,
        m.reason,
        d.id AS ip,
        d.port AS port
    FROM messages m
    LEFT JOIN DeviceEntity d
    ON d.deviceId = m.receiverDeviceId
    ORDER BY m.timestamp ASC LIMIT 10
    """
    )
    suspend fun getMessagesWithDevice(): List<MessageWithDevice>
}