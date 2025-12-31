package com.hiren.grpcsync.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DeviceEntity(
    @PrimaryKey val id: String, // ip
    val name: String,
    val port: Int,
    val status: Boolean,
    val lastSeen: Long
)