package com.hiren.grpcsync.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hiren.grpcsync.grpc.GrpcSdkImpl
import com.hiren.grpcsync.grpc.ServiceBus
import com.hiren.grpcsync.grpc.ServiceEvent
import com.hiren.grpcsync.grpc.ChannelPool
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.utils.NotificationHelper.setNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class OfflineSyncService : LifecycleService() {

    @Inject
    lateinit var deviceRepository: DeviceRepository

    private val started by lazy { AtomicBoolean(false) }

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var grpcManager: GrpcSdkImpl? = GrpcSdkImpl(serviceScope)

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(2, setNotification("Discovering Devices.....", this))

        // 🔒 Prevent duplicate start
        if (started.getAndSet(true)) {
            return START_STICKY
        }

        if (intent == null) return START_STICKY

        lifecycleScope.launch {
            ServiceBus.events.collect { event ->
                when (event) {
                    is ServiceEvent.StartDiscovery -> {
                        Log.d("SyncService", "ServiceEvent.StartDiscovery")
                        startServer(
                            grpcPort = event.grpcPort,
                            provider = event.provider,
                            events = event.events
                        )
                    }

                    is ServiceEvent.StopDiscovery -> {
                        Log.d("SyncService", "ServiceEvent.StopDiscovery")
                        stopServer()
                    }

                    is ServiceEvent.Stop -> {
                        Log.d("SyncService", "ServiceEvent.Stop")
                        stopSelf()
                    }

                    is ServiceEvent.Send -> {
                        Log.d("SyncService", "ServiceEvent.Send")
                        grpcManager?.sendMessage(
                            ip = event.ip,
                            port = event.port,
                            message = event.payload
                        )
                    }

                    is ServiceEvent.SendWithCallback -> {
                        Log.d("SyncService", "ServiceEvent.Send")
                        grpcManager?.sendMessageWithCallback(
                            ip = event.ip,
                            port = event.port,
                            message = event.payload,
                            callback = event.callback
                        )
                    }

                    is ServiceEvent.BroadcastFireAndForget -> {
                        Log.d("SyncService", "ServiceEvent.BroadcastFireAndForget")
                        grpcManager?.broadcastFireAndForget(
                            deviceRepository = deviceRepository,
                            targets = event.targets,
                            message = event.message,
                            maxConcurrency = event.maxConcurrency
                        )
                    }

                    is ServiceEvent.ChangeGrpcPort -> {
                        Log.d("SyncService", "ServiceEvent.ChangeGrpcPort ${event.port}")
                        grpcManager?.restartServer(event.port)
                    }

                    is ServiceEvent.StartStream -> {
                        Log.d("SyncService", "ServiceEvent.Broadcast")
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        grpcManager = null
        serviceScope.cancel()
        started.set(false)
        super.onDestroy()
    }

    private fun startServer(
        grpcPort: Int = 50051,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        grpcManager?.startServer(grpcPort, provider, events)
    }

    fun stopServer() {
        grpcManager?.stopServer()
        ChannelPool().shutdownAll()
    }
}