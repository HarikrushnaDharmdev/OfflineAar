package com.hiren.offlineaar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.hiren.grpcsync.utils.NotificationHelper.CHANNEL_ID
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppClass : Application() {

    /*@Inject
    lateinit var grpcSdk: GrpcSdk*/

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Retailz Cloud Sync.....",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        //grpcSdk.start(this)

        //GrpcSdkProvider.init()
        //GrpcSdkProvider.getInstance().start(this)

    }
}