package com.hiren.grpcsync.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("deviceId")])
data class DeviceEntity(
    @PrimaryKey
    val deviceId: String, // Unique device ID (could be a UUID or any unique identifier)
    val id: String, // ip
    val name: String, // DeviceName
    val port: Int, // GRPC Port
    val status: Boolean, // Device online status
    val lastSeen: Long, // Timestamp of the last time the device was seen
    val note: String
)

data class DeviceIpPort(
    val id: String,
    val port: Int
)