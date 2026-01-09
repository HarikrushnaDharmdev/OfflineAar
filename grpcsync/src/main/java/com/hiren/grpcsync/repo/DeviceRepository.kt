package com.hiren.grpcsync.repo

import com.hiren.grpcsync.db.DeviceEntity
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeDevices(): Flow<List<DeviceEntity>>
    suspend fun deleteAllDevices()
    suspend fun getAllDevices(): List<DeviceEntity>
}