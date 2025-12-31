package com.hiren.grpcsync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMessage(message: MessageEntity)

    @Query("SELECT * FROM MessageEntity WHERE channelId IN (:channelIds) ORDER BY timestamp ASC")
    fun getMessages(channelIds: List<String>): Flow<List<MessageEntity>>

    @Query("UPDATE MessageEntity SET status = :status WHERE channelId = :channelId")
    suspend fun updateMessageStatus(channelId: String, status: Boolean)

}