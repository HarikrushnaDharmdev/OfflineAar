package com.hiren.grpcsync.repo

import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.DeviceIpPort
import com.hiren.grpcsync.utils.BroadCastDevice
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {

    fun observeDevices(): Flow<List<DeviceEntity>>

    suspend fun deleteAllDevices()

    suspend fun getAllDevices(): List<DeviceEntity>

    suspend fun getDevicesForBroadcast(
        currentDeviceId: String,
        deviceFilter: BroadCastDevice
    ): List<DeviceIpPort>

}