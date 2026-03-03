package com.hiren.grpcsync.public_classes

import android.content.Context
import android.content.Intent
import android.os.Build
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.DeviceIpPort
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.grpc.ServiceBus
import com.hiren.grpcsync.grpc.ServiceEvent
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.service.OfflineSyncService
import com.hiren.grpcsync.service.SyncServiceController
import com.hiren.grpcsync.service.UDPDiscoveryService
import com.hiren.grpcsync.utils.BroadCastDevice
import com.hiren.grpcsync.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * -----------------------------------------------------------------------------
 * File Name     : OfflineCommImpl.kt
 * Description   : Implementation of OfflineComm interface.
 *                 Responsible for managing:
 *                 - UDP discovery service
 *                 - gRPC sync service
 *                 - Message sending (direct & broadcast)
 *                 - Device observation
 *                 - Dynamic configuration changes
 *
 * Architecture  : Acts as a communication facade between UI/domain layer
 *                 and background services (UDPDiscoveryService & OfflineSyncService).
 *
 * Created On    : 24-02-2026
 * Author        : Hiren Patel
 * -----------------------------------------------------------------------------
 */
@Singleton
class OfflineCommImpl @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val controller: SyncServiceController,
    @ApplicationContext private val context: Context
) : OfflineComm {

    /**
     * Helper method to start service safely.
     *
     * Uses startForegroundService() for Android O+,
     * otherwise falls back to startService().
     */
    fun startService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun startMessageService(
        grpcPort: Int,
        printLog: Boolean,
        responseTimeout: Long,
        maxBroadcastDevicesConcurrency: Int,
        deviceId: String
    ) {
        require(grpcPort in 0..65535) {
            "grpcPort must be between 0 and 65535"
        }

        // Apply runtime configuration
        Constants.PRINT_LOG = printLog
        Constants.RESPONSE_TIMEOUT_MS = responseTimeout
        Constants.MAX_BROADCAST_DEVICES_CONCURRENCY = maxBroadcastDevicesConcurrency

        startMessageSyncService(
            grpcPort = grpcPort,
            deviceId = deviceId
        )
    }

    override fun startMessageServiceWithDiscovery(
        grpcPort: Int,
        printLog: Boolean,
        responseTimeout: Long,
        maxBroadcastDevicesConcurrency: Int,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?,
        deviceId: String
    ) {
        require(grpcPort in 0..65535) {
            "grpcPort must be between 0 and 65535"
        }

        // Apply runtime configuration
        Constants.PRINT_LOG = printLog
        Constants.RESPONSE_TIMEOUT_MS = responseTimeout
        Constants.MAX_BROADCAST_DEVICES_CONCURRENCY = maxBroadcastDevicesConcurrency

        controller.setOnStarted { _ ->
            startMessageDiscovery(grpcPort = grpcPort, provider = provider, events = events)
        }

        startMessageSyncService(
            grpcPort = grpcPort,
            deviceId = deviceId
        )
    }

    override fun startMessageDiscovery(
        grpcPort: Int,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        ServiceBus.post(
            ServiceEvent.StartDiscovery(
                grpcPort = grpcPort,
                provider = provider,
                events = events
            )
        )
    }

    /**
     * Dynamically changes gRPC port at runtime.
     * Also notifies internal ServiceBus.
     */
    override fun changeGrpcPort(port: Int) {
        startService(
            Intent(context, UDPDiscoveryService::class.java).apply {
                putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_CHANGE_GRPC_PORT)
                putExtra(Constants.EXTRA_GRPC_PORT, port)
            }
        )
        ServiceBus.post(ServiceEvent.ChangeGrpcPort(port))
    }

    /**
     * Sends a message to a specific device without callback.
     */
    override fun sendMessage(ip: String, port: Int, payload: Message, retryIfFail: Boolean) {
        ServiceBus.post(
            ServiceEvent.Send(
                ip = ip,
                port = port,
                payload = payload,
                retryIfFail = retryIfFail
            )
        )
    }

    /**
     * Sends a message with response callback.
     * Callback receives GrpcResult (success/failure).
     */
    override fun sendMessageWithCallback(
        ip: String,
        port: Int,
        payload: Message,
        retryIfFail: Boolean,
        callback: (GrpcResult) -> Unit
    ) {
        ServiceBus.post(
            ServiceEvent.SendWithCallback(
                ip = ip,
                payload = payload,
                port = port,
                retryIfFail = retryIfFail,
                callback = callback
            )
        )
    }

    /**
     * Broadcasts a message to multiple devices.
     *
     * @param targets List of (IP, Port) pairs.
     * @param message Message payload.
     * @param maxConcurrency Limits parallel sending.
     */
    override fun sendMessageBroadcast(
        targets: List<DeviceIpPort>?,
        message: Message,
        maxConcurrency: Int,
        deviceFilter: BroadCastDevice
    ) {
        ServiceBus.post(
            ServiceEvent.SendBroadcast(
                targets = targets,
                message = message,
                maxConcurrency = maxConcurrency,
                deviceFilter = deviceFilter
            )
        )
    }

    override fun stopMessageDiscovery() {
        ServiceBus.post(ServiceEvent.StopDiscovery)
    }

    override fun stopMessageServer() {
        ServiceBus.post(ServiceEvent.Stop)
    }

    private fun startMessageSyncService(grpcPort: Int, deviceId: String) {
        startService(
            Intent(context, OfflineSyncService::class.java)
                .apply {
                    putExtra(Constants.EXTRA_GRPC_PORT, grpcPort)
                    putExtra(Constants.EXTRA_DEVICE_ID, deviceId)
                }
        )
    }


    override fun startDeviceDiscovery(
        udpPort: Int,
        grpcPort: Int,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        deleteDeviceOnTimeout: Boolean,
        printLog: Boolean,
        responseTimeout: Long,
        cleanUpTimeoutDelay: Long,
        deviceId: String
    ) {
        require(udpPort in 0..65535) {
            "udpPort must be between 0 and 65535"
        }
        startDeviceSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            printLog = printLog,
            broadcastIntervalMs = broadcastIntervalMs,
            deviceTimeoutMs = deviceTimeoutMs,
            cleanUpTimeoutDelay = cleanUpTimeoutDelay,
            deleteDeviceOnTimeout = deleteDeviceOnTimeout,
            deviceId = deviceId
        )
    }

    override fun startDeviceDiscoveryOnBoosterMode(
        udpPort: Int,
        grpcPort: Int,
        udpPrintLog: Boolean,
        deviceId: String
    ) {
        require(udpPort in 0..65535) {
            "udpPort must be between 0 and 65535"
        }

        startDeviceSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            printLog = udpPrintLog,
            broadcastIntervalMs = 2000L,
            deviceTimeoutMs = 5000L,
            cleanUpTimeoutDelay = 2000L,
            deleteDeviceOnTimeout = false,
            deviceId = deviceId
        )
    }

    override fun startDeviceDiscoveryOnEnergySaving(
        udpPort: Int,
        grpcPort: Int,
        udpPrintLog: Boolean,
        deviceId: String
    ) {
        require(udpPort in 0..65535) {
            "udpPort must be between 0 and 65535"
        }

        startDeviceSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            printLog = udpPrintLog,
            broadcastIntervalMs = 10000L,
            deviceTimeoutMs = 11000L,
            deleteDeviceOnTimeout = true,
            cleanUpTimeoutDelay = 10000L,
            deviceId = deviceId
        )
    }

    override fun stopDeviceDiscovery() {
        startService(
            Intent(context, UDPDiscoveryService::class.java)
                .apply { putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_STOP_DISCOVERY) }
        )
    }

    override fun stopDeviceServer() {
        startService(
            Intent(context, UDPDiscoveryService::class.java)
                .apply { putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_STOP) }
        )
    }

    fun startDeviceSyncService(
        udpPort: Int,
        grpcPort: Int,
        printLog: Boolean,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        cleanUpTimeoutDelay: Long,
        deleteDeviceOnTimeout: Boolean,
        deviceId: String
    ) {
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
                    putExtra(Constants.EXTRA_PRINT_LOG, printLog)
                    putExtra(Constants.EXTRA_CLEANUP_TIME_DELAY, cleanUpTimeoutDelay)
                    putExtra(Constants.EXTRA_DEVICE_ID, deviceId)
                }
        )
    }

    /**
     * Observes devices from local database as Flow.
     * UI layer can collect this to get real-time updates.
     */
    override fun getDevices(): Flow<List<DeviceEntity>> {
        return deviceRepository.observeDevices()
    }

    /**
     * Dynamically changes UDP port at runtime.
     */
    override fun changeUdpPort(port: Int) {
        startService(
            Intent(context, UDPDiscoveryService::class.java).apply {
                putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_CHANGE_UDP_PORT)
                putExtra(Constants.EXTRA_UDP_PORT, port)
            }
        )
    }

}