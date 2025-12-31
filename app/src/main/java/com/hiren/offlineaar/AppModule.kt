package com.hiren.offlineaar

import android.content.Context
import com.hiren.grpcsync.public_classes.OfflineComm
import com.hiren.grpcsync.public_classes.OfflineCommImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /* @Provides
     @Singleton
     fun provideGrpcSdk(): GrpcSdk = GrpcSdkImpl()*/

    @Provides
    @Singleton
    fun provideOfflineComm(@ApplicationContext appContext: Context): OfflineComm =
        OfflineCommImpl(appContext)
}