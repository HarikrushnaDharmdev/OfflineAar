package com.hiren.grpcsync.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DeviceEntity(
    @PrimaryKey val id: String, // ip
    val name: String, // DeviceName
    val port: Int, // GRPC Port
    val status: Boolean, // Device online status
    val lastSeen: Long, // Timestamp of the last time the device was seen
    val note: String
)