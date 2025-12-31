package com.hiren.grpcsync.repo

import com.hiren.grpcsync.db.DeviceDao
import com.hiren.grpcsync.db.DeviceEntity
import kotlinx.coroutines.flow.Flow

class DeviceRepositoryImpl(private val deviceDao: DeviceDao) : DeviceRepository {
    override fun observeDevices(): Flow<List<DeviceEntity>> =
        deviceDao.observeDevices()

    override suspend fun deleteAllDevices() {
        deviceDao.deleteAllDevices()
    }
}