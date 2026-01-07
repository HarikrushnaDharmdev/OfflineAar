package com.hiren.grpcsync.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hiren.grpcsync.grpc.GrpcSdkImpl
import com.hiren.grpcsync.public_classes.ServiceBus
import com.hiren.grpcsync.public_classes.ServiceEvent
import com.hiren.grpcsync.grpc.ChannelPool
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.utils.Constants
import com.hiren.grpcsync.utils.NotificationHelper.setNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class OfflineSyncService : LifecycleService() {

    private var grpcManager: GrpcSdkImpl? = null

    @Inject
    lateinit var udpBroadcastService: UdpBroadcastService

    private val started = AtomicBoolean(false)

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

        startForeground(2, setNotification("Discovering Devices.....", this))

        // 🔒 Prevent duplicate start
        if (started.getAndSet(true)) {
            return START_STICKY
        }

        if (intent == null) return START_STICKY

        val udpPort = intent.getIntExtra(Constants.EXTRA_UDP_PORT, 10080)
        val grpcPort = intent.getIntExtra(Constants.EXTRA_GRPC_PORT, 50051)
        val broadcastInterval =
            intent.getLongExtra(Constants.EXTRA_BROADCAST_INTERVAL, 3000L)
        val deviceTimeout =
            intent.getLongExtra(Constants.EXTRA_DEVICE_TIMEOUT, 10000L)
        val deleteOnTimeout =
            intent.getBooleanExtra(Constants.EXTRA_DELETE_ON_TIMEOUT, false)
        val printLog =
            intent.getBooleanExtra(Constants.EXTRA_PRINT_LOG, true)

        lifecycleScope.launch {
            ServiceBus.events.collect { event ->
                when (event) {
                    is ServiceEvent.StartDiscovery -> {
                        Log.d("SyncService", "ServiceEvent.StartDiscovery")
                        startServer(
                            udpPort = udpPort,
                            grpcPort = grpcPort,
                            broadcastIntervalMs = broadcastInterval,
                            deviceTimeoutMs = deviceTimeout,
                            deleteDeviceOnTimeout = deleteOnTimeout,
                            printLog = printLog,
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
                        Log.d("SyncService", "$grpcManager.")
                        grpcManager?.sendMessage(
                            ip = event.ip,
                            port = event.port,
                            message = event.payload
                        )
                    }

                    is ServiceEvent.SendWithCallback -> {
                        Log.d("SyncService", "ServiceEvent.Send")
                        Log.d("SyncService", "$grpcManager.")
                        grpcManager?.sendMessageWithCallback(
                            ip = event.ip,
                            port = event.port,
                            message = event.payload,
                            callback = event.callback
                        )
                    }

                    is ServiceEvent.Broadcast -> {
                        Log.d("SyncService", "ServiceEvent.Broadcast ${event.payload.content}")
                        grpcManager?.broadcast(message = event.payload, devices = event.devices)
                    }

                    is ServiceEvent.ChangeGrpcPort -> {
                        Log.d("SyncService", "ServiceEvent.ChangeGrpcPort ${event.port}")
                        udpBroadcastService.changeGrpcPort(event.port)
                    }

                    is ServiceEvent.ChangeUdpPort -> {
                        Log.d("SyncService", "ServiceEvent.ChangeUdpPort ${event.port}")
                        udpBroadcastService.changeUdpPort(event.port)
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
        super.onDestroy()
    }

    private fun startServer(
        udpPort: Int = 10080,
        grpcPort: Int = 50051,
        broadcastIntervalMs: Long = 3000L,
        deviceTimeoutMs: Long = 10000L,
        deleteDeviceOnTimeout: Boolean = false,
        printLog: Boolean = true,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {

        udpBroadcastService.start(
            udpPort = udpPort,
            grpcPort = grpcPort,
            broadcastIntervalMs = broadcastIntervalMs,
            deviceTimeoutMs = deviceTimeoutMs,
            deleteDeviceOnTimeout = deleteDeviceOnTimeout,
            printLog = printLog
        )
        grpcManager = GrpcSdkImpl(serviceScope)
        grpcManager?.startServer(grpcPort, provider, events)

        /*lifecycleScope.launch {
            grpcManager?.events?.collect { event ->
                when (event) {
                    is GrpcEvent.ServerStarted ->
                        Log.d("GRPC", "Server started on ${event.port}")

                    is GrpcEvent.MessageReceived ->
                        Log.d("GRPC", "From ${event.from}: ${event.content}")

                    is GrpcEvent.MessageSent ->
                        Log.d("GRPC", "Sent to ${event.to}")

                    is GrpcEvent.Error ->
                        Log.e("GRPC", "Error ${event.target}", event.throwable)

                    else -> {}
                }
            }
        }*/
    }

    fun stopServer() {
        udpBroadcastService.stop()
        grpcManager?.stopServer()
        ChannelPool().shutdownAll()
    }
}