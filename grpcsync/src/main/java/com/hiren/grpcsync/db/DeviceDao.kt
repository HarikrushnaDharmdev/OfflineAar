package com.hiren.grpcsync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: DeviceEntity)

    @Query("SELECT * FROM DeviceEntity ORDER BY id DESC")
    fun observeDevices(): Flow<List<DeviceEntity>>

    @Query("DELETE FROM DeviceEntity WHERE :timestamp - lastSeen > :timeout")
    suspend fun removeStaleDevices(timestamp: Long, timeout: Long)

    //@Query("UPDATE DEVICEENTITY SET status = 0 WHERE :timestamp - lastSeen > :timeout")
    @Query("UPDATE DEVICEENTITY SET status = 0 WHERE status = 1 AND lastSeen < :timestamp - :timeout")
    suspend fun updateStaleDevices(timestamp: Long, timeout: Long)

    @Query("UPDATE DEVICEENTITY SET  status = 0")
    fun offlineAllDevice()

    @Query("DELETE FROM DEVICEENTITY")
    fun deleteAllDevices()

    @Query("SELECT * FROM DeviceEntity ORDER BY id DESC")
    fun getAllDevice(): List<DeviceEntity>

}