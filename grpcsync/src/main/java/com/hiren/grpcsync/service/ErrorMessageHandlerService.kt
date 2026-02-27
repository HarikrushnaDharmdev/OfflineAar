package com.hiren.grpcsync.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.hiren.grpcsync.db.MessageWithDevice
import com.hiren.grpcsync.repo.MessageRepository
import com.hiren.grpcsync.utils.NotificationHelper.setNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ErrorMessageHandlerService : LifecycleService() {

    @Inject
    lateinit var messageRepository: MessageRepository

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(2, setNotification("Retailz Cloud Sync.....", this))

        handleErrorMessages()
        return START_STICKY
    }

    private fun handleErrorMessages() {
        serviceScope.launch(Dispatchers.IO) {
            val messageList = messageRepository.getMessagesWithDevice()
            syncMessages(messageList[0])
        }
    }

    private fun syncMessages(messageWithDevice: MessageWithDevice) {

    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
