package com.hiren.grpcsync.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DeviceEntity::class, MessagesEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    abstract fun messageDao(): MessageDao
}