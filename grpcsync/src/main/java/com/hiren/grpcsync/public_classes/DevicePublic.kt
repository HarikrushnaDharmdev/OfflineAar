package com.hiren.grpcsync.public_classes

import android.content.Context
import com.hiren.grpcsync.db.AppDatabase
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.repo.DeviceRepositoryImpl

/**
 * This class is used to initialize the DeviceRepository for public access
 * */
internal object DevicePublic {

    lateinit var deviceRepository: DeviceRepository
        private set

    fun init(context: Context) {
        val db = AppDatabase.getDatabase(context)
        deviceRepository = DeviceRepositoryImpl(db.deviceDao())
    }
}