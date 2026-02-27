package com.hiren.grpcsync.repo

import com.hiren.grpcsync.db.DeviceDao
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.DeviceIpPort
import com.hiren.grpcsync.utils.BroadCastDevice
import kotlinx.coroutines.flow.Flow

class DeviceRepositoryImpl(private val deviceDao: DeviceDao) : DeviceRepository {
    override fun observeDevices(): Flow<List<DeviceEntity>> =
        deviceDao.observeDevices()

    override suspend fun deleteAllDevices() {
        deviceDao.deleteAllDevices()
    }

    override suspend fun getAllDevices(): List<DeviceEntity> {
        return deviceDao.getAllDevice()
    }

    override suspend fun getDevicesForBroadcast(
        currentDeviceId: String,
        deviceFilter: BroadCastDevice
    ): List<DeviceIpPort> {

        val (onlineOnly, includeSelf) = when (deviceFilter) {
            is BroadCastDevice.ALL -> false to deviceFilter.includeSelf
            is BroadCastDevice.ONLINE -> true to deviceFilter.includeSelf
        }

        return deviceDao.getDevicesForBroadcast(
            currentDeviceId = currentDeviceId,
            onlineOnly = onlineOnly,
            includeSelf = includeSelf
        )
    }
}