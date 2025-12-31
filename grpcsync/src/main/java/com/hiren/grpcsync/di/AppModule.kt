package com.hiren.grpcsync.di

import android.content.Context
import androidx.room.Room
import com.hiren.grpcsync.db.AppDatabase
import com.hiren.grpcsync.db.DeviceDao
import com.hiren.grpcsync.db.MessageDao
import com.hiren.grpcsync.public_classes.OfflineComm
import com.hiren.grpcsync.public_classes.OfflineCommImpl
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.repo.DeviceRepositoryImpl
import com.hiren.grpcsync.repo.MessageRepository
import com.hiren.grpcsync.service.UdpBroadcastService
import com.hiren.grpcsync.service.UdpBroadcastServiceV1
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideUdpBroadcastService(deviceDao: DeviceDao): UdpBroadcastService {
        return UdpBroadcastService(deviceDao)
    }

    @Singleton
    @Provides
    fun provideUdpBroadcastServiceV1(deviceDao: DeviceDao): UdpBroadcastServiceV1 {
        return UdpBroadcastServiceV1(deviceDao)
    }

    @Singleton
    @Provides
    fun provideDeviceRepository(
        deviceDao: DeviceDao
    ): DeviceRepository {
        return DeviceRepositoryImpl(deviceDao)
    }

    @Singleton
    @Provides
    fun provideMessageRepository(
        messageDao: MessageDao
    ): MessageRepository {
        return MessageRepository(messageDao)
    }

    @Singleton
    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao {
        return database.deviceDao()
    }

    @Singleton
    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    // Provide Room database
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "DEVICE_DATABASE"
        ).fallbackToDestructiveMigration().build()
    }
}