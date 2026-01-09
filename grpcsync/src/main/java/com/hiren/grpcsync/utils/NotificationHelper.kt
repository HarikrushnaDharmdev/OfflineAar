package com.hiren.grpcsync.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.app.NotificationCompat
import com.hiren.grpcsync.R

internal object NotificationHelper {
    const val CHANNEL_ID = "DEVICE_DISCOVERY_CHANNEL"

    fun setNotification(message: String, context: Context): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Discovering Devices via UDP",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Discovering Devices via UDP")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentText(message)
            .build()
    }

    fun updateNotification(message: String, context: Context) {
        try {
            val notification = setNotification(message, context)
            val service = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            service.notify(2, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}