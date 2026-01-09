package com.hiren.grpcsync.public_classes

import android.content.Context
import android.content.Intent
import android.os.Build
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.grpc.ServiceBus
import com.hiren.grpcsync.grpc.ServiceEvent
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.service.OfflineSyncService
import com.hiren.grpcsync.service.UDPDiscoveryService
import com.hiren.grpcsync.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineCommImpl @Inject constructor(
    private val deviceRepository: DeviceRepository,
    @ApplicationContext private val context: Context
) : OfflineComm {

    override fun startServiceOnCustomMode(
        udpPort: Int,
        grpcPort: Int,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        deleteDeviceOnTimeout: Boolean,
        printLog: Boolean,
        responseTimeout: Long,
        maxBroadcastDevicesConcurrency: Int,
        cleanUpTimeoutDelay: Long,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        Constants.RESPONSE_TIMEOUT_MS = responseTimeout
        Constants.MAX_BROADCAST_DEVICES_CONCURRENCY = maxBroadcastDevicesConcurrency
        Constants.PRINT_LOG = printLog

        startSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            broadcastIntervalMs = broadcastIntervalMs,
            deviceTimeoutMs = deviceTimeoutMs,
            cleanUpTimeoutDelay = cleanUpTimeoutDelay,
            deleteDeviceOnTimeout = deleteDeviceOnTimeout,
            provider = provider,
            events = events
        )
    }

    override fun startServiceOnBoosterMode(
        udpPort: Int,
        grpcPort: Int,
        printLog: Boolean,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        Constants.PRINT_LOG = printLog

        startSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            broadcastIntervalMs = 2000L,
            deviceTimeoutMs = 5000L,
            cleanUpTimeoutDelay = 2000L,
            deleteDeviceOnTimeout = false,
            provider = provider,
            events = events
        )
    }

    override fun startServiceOnEnergySaving(
        udpPort: Int,
        grpcPort: Int,
        printLog: Boolean,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        Constants.PRINT_LOG = printLog

        startSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            broadcastIntervalMs = 10000L,
            deviceTimeoutMs = 11000L,
            deleteDeviceOnTimeout = true,
            cleanUpTimeoutDelay = 10000L,
            provider = provider,
            events = events
        )
    }

    override fun stopService() {
        startService(
            Intent(
                context,
                UDPDiscoveryService::class.java
            ).apply { putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_STOP) }
        )
        ServiceBus.post(ServiceEvent.StopDiscovery)
    }

    override fun sendMessage(ip: String, port: Int, payload: Message) {
        ServiceBus.post(ServiceEvent.Send(ip = ip, port = port, payload = payload))
    }

    override fun sendMessageWithCallback(
        ip: String,
        port: Int,
        payload: Message,
        callback: (GrpcResult) -> Unit
    ) {
        ServiceBus.post(
            ServiceEvent.SendWithCallback(
                ip = ip,
                payload = payload,
                port = port,
                callback = callback
            )
        )
    }

    override fun sendMessageBroadcast(
        targets: List<Pair<String, Int>>?,
        message: Message,
        maxConcurrency: Int
    ) {
        ServiceBus.post(
            ServiceEvent.BroadcastFireAndForget(
                targets = targets,
                message = message,
                maxConcurrency = maxConcurrency
            )
        )
    }

    override fun getDevices(): Flow<List<DeviceEntity>> {
        return deviceRepository.observeDevices()
    }

    override fun changeUdpPort(port: Int) {
        startService(
            Intent(
                context,
                UDPDiscoveryService::class.java
            ).apply {
                putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_CHANGE_UDP_PORT)
                putExtra(Constants.EXTRA_UDP_PORT, port)
            }
        )
    }

    override fun changeGrpcPort(port: Int) {
        startService(
            Intent(
                context,
                UDPDiscoveryService::class.java
            ).apply {
                putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_CHANGE_GRPC_PORT)
                putExtra(Constants.EXTRA_GRPC_PORT, port)
            }
        )
        ServiceBus.post(ServiceEvent.ChangeGrpcPort(port))
    }

    fun startSyncService(
        udpPort: Int,
        grpcPort: Int,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        cleanUpTimeoutDelay: Long,
        deleteDeviceOnTimeout: Boolean,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        // Start the OfflineSyncService with the provided configurations
        startService(
            Intent(context, OfflineSyncService::class.java)
                .apply { putExtra(Constants.EXTRA_GRPC_PORT, grpcPort) }
        )

        // Start UDP Discovery Service with the provided configurations
        startService(
            Intent(context, UDPDiscoveryService::class.java)
                .apply {
                    putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_START)
                    putExtra(Constants.EXTRA_UDP_PORT, udpPort)
                    putExtra(Constants.EXTRA_GRPC_PORT, grpcPort)
                    putExtra(Constants.EXTRA_BROADCAST_INTERVAL, broadcastIntervalMs)
                    putExtra(Constants.EXTRA_DEVICE_TIMEOUT, deviceTimeoutMs)
                    putExtra(Constants.EXTRA_DELETE_ON_TIMEOUT, deleteDeviceOnTimeout)
                    putExtra(Constants.EXTRA_PRINT_LOG, Constants.PRINT_LOG)
                    putExtra(Constants.EXTRA_CLEANUP_TIME_DELAY, cleanUpTimeoutDelay)
                }
        )

        ServiceBus.post(
            ServiceEvent.StartDiscovery(
                grpcPort = grpcPort,
                provider = provider,
                events = events
            )
        )
    }

    fun startService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}