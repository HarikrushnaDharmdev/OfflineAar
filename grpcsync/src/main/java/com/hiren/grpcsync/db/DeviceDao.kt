package com.hiren.grpcsync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * -----------------------------------------------------------------------------
 * File Name     : DeviceDao.kt
 * Description   : Data Access Object (DAO) for managing DeviceEntity records
 *                 in the local Room database.
 *                 DAO interface for performing CRUD operations on DeviceEntity.
 *
 * Created On    : 24-02-2026
 * Author        : Hiren Patel
 * -----------------------------------------------------------------------------
 */
@Dao
interface DeviceDao {

    /**
     * Inserts a new device into the database.
     * If a device with the same primary key already exists,
     * it will be replaced (updated) with the new data.
     *
     * @param device DeviceEntity object to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: DeviceEntity)

    /**
     * Observes all devices stored in the database.
     * Emits updates automatically whenever the table data changes.
     *
     * @return Flow emitting a list of DeviceEntity ordered by id (latest first).
     */
    @Query("SELECT * FROM DeviceEntity ORDER BY id DESC")
    fun observeDevices(): Flow<List<DeviceEntity>>

    /**
     * Removes devices that have not been seen within the specified timeout duration.
     *
     * @param timestamp Current system timestamp.
     * @param timeout Time threshold in milliseconds.
     */
    @Query("DELETE FROM DeviceEntity WHERE :timestamp - lastSeen > :timeout")
    suspend fun removeStaleDevices(timestamp: Long, timeout: Long)

    /**
     * Updates stale devices by marking them as offline (status = 0).
     * Only devices that are currently online (status = 1) and
     * whose lastSeen timestamp exceeds the timeout will be updated.
     *
     * @param timestamp Current system timestamp.
     * @param timeout Time threshold in milliseconds.
     */
    @Query("UPDATE DEVICEENTITY SET status = 0 WHERE status = 1 AND lastSeen < :timestamp - :timeout")
    suspend fun updateStaleDevices(timestamp: Long, timeout: Long)

    /**
     * Marks all devices as offline (status = 0).
     * Useful when resetting connection state.
     */
    @Query("UPDATE DEVICEENTITY SET status = 0")
    fun offlineAllDevice()

    /**
     * Deletes all device records from the table.
     * Use with caution.
     */
    @Query("DELETE FROM DEVICEENTITY")
    fun deleteAllDevices()

    /**
     * Retrieves all devices synchronously.
     * This does NOT observe changes (non-reactive).
     *
     * @return List of all DeviceEntity ordered by id (latest first).
     */
    @Query("SELECT * FROM DeviceEntity ORDER BY id DESC")
    fun getAllDevice(): List<DeviceEntity>
}