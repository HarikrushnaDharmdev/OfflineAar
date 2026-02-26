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
import com.hiren.grpcsync.service.SyncServiceController
import com.hiren.grpcsync.service.UDPDiscoveryService
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
     * Starts services in fully customizable mode.
     *
     * Allows complete control over:
     * - UDP port
     * - gRPC port
     * - Broadcast interval
     * - Device timeout duration
     * - Concurrency level
     * - Response timeout
     * - Cleanup delay
     */
    override fun startServiceOnCustomMode(
        udpPort: Int,
        grpcPort: Int,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        deleteDeviceOnTimeout: Boolean,
        printLog: Boolean,
        udpPrintLog: Boolean,
        responseTimeout: Long,
        maxBroadcastDevicesConcurrency: Int,
        cleanUpTimeoutDelay: Long,
        deviceId: String
    ) {
        require(udpPort in 0..65535) {
            "udpPort must be between 0 and 65535"
        }

        require(grpcPort in 0..65535) {
            "grpcPort must be between 0 and 65535"
        }

        // Apply runtime configuration
        Constants.RESPONSE_TIMEOUT_MS = responseTimeout
        Constants.MAX_BROADCAST_DEVICES_CONCURRENCY = maxBroadcastDevicesConcurrency
        Constants.PRINT_LOG = printLog

        startSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            udpPrintLog = udpPrintLog,
            broadcastIntervalMs = broadcastIntervalMs,
            deviceTimeoutMs = deviceTimeoutMs,
            cleanUpTimeoutDelay = cleanUpTimeoutDelay,
            deleteDeviceOnTimeout = deleteDeviceOnTimeout,
            deviceId = deviceId
        )
    }

    override fun startServiceOnCustomModeWithStartDiscovery(
        udpPort: Int,
        grpcPort: Int,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        deleteDeviceOnTimeout: Boolean,
        printLog: Boolean,
        udpPrintLog: Boolean,
        responseTimeout: Long,
        maxBroadcastDevicesConcurrency: Int,
        cleanUpTimeoutDelay: Long,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?,
        deviceId: String
    ) {
        require(udpPort in 0..65535) {
            "udpPort must be between 0 and 65535"
        }

        require(grpcPort in 0..65535) {
            "grpcPort must be between 0 and 65535"
        }

        // Apply runtime configuration
        Constants.RESPONSE_TIMEOUT_MS = responseTimeout
        Constants.MAX_BROADCAST_DEVICES_CONCURRENCY = maxBroadcastDevicesConcurrency
        Constants.PRINT_LOG = printLog

        controller.setOnStarted { _ ->
            startDiscovery(grpcPort = grpcPort, provider = provider, events = events)
        }

        startSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            udpPrintLog = udpPrintLog,
            broadcastIntervalMs = broadcastIntervalMs,
            deviceTimeoutMs = deviceTimeoutMs,
            cleanUpTimeoutDelay = cleanUpTimeoutDelay,
            deleteDeviceOnTimeout = deleteDeviceOnTimeout,
            deviceId = deviceId
        )
    }

    /**
     * Starts services in high-performance (Booster) mode.
     *
     * - Faster broadcast interval
     * - Shorter timeout
     * - Higher device responsiveness
     */
    override fun startServiceOnBoosterMode(
        udpPort: Int,
        grpcPort: Int,
        printLog: Boolean,
        udpPrintLog: Boolean,
        deviceId: String
    ) {
        require(udpPort in 0..65535) {
            "udpPort must be between 0 and 65535"
        }

        require(grpcPort in 0..65535) {
            "grpcPort must be between 0 and 65535"
        }

        Constants.PRINT_LOG = printLog

        startSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            udpPrintLog = udpPrintLog,
            broadcastIntervalMs = 2000L,
            deviceTimeoutMs = 5000L,
            cleanUpTimeoutDelay = 2000L,
            deleteDeviceOnTimeout = false,
            deviceId = deviceId
        )
    }

    /**
     * Starts services in energy-saving mode.
     *
     * - Slower broadcast interval
     * - Longer timeout window
     * - Deletes devices on timeout
     */
    override fun startServiceOnEnergySaving(
        udpPort: Int,
        grpcPort: Int,
        printLog: Boolean,
        udpPrintLog: Boolean,
        deviceId: String
    ) {
        require(udpPort in 0..65535) {
            "udpPort must be between 0 and 65535"
        }

        require(grpcPort in 0..65535) {
            "grpcPort must be between 0 and 65535"
        }

        Constants.PRINT_LOG = printLog

        startSyncService(
            udpPort = udpPort,
            grpcPort = grpcPort,
            udpPrintLog = udpPrintLog,
            broadcastIntervalMs = 10000L,
            deviceTimeoutMs = 11000L,
            deleteDeviceOnTimeout = true,
            cleanUpTimeoutDelay = 10000L,
            deviceId = deviceId
        )
    }

    /**
     * Stops UDP discovery service.
     * Sends a STOP command via intent and notifies ServiceBus.
     */
    override fun stopService() {
        startService(
            Intent(context, UDPDiscoveryService::class.java)
                .apply { putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_STOP) }
        )
        ServiceBus.post(ServiceEvent.Stop)
    }

    override fun stopDiscovery() {
        startService(
            Intent(context, UDPDiscoveryService::class.java)
                .apply { putExtra(Constants.EXTRA_UDP_TYPE, Constants.UDP_STOP_DISCOVERY) }
        )
        ServiceBus.post(ServiceEvent.StopDiscovery)
    }

    /**
     * Sends a message to a specific device without callback.
     */
    override fun sendMessage(ip: String, port: Int, payload: Message) {
        ServiceBus.post(ServiceEvent.Send(ip = ip, port = port, payload = payload))
    }

    /**
     * Sends a message with response callback.
     * Callback receives GrpcResult (success/failure).
     */
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

    /**
     * Broadcasts a message to multiple devices.
     *
     * @param targets List of (IP, Port) pairs.
     * @param message Message payload.
     * @param maxConcurrency Limits parallel sending.
     */
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
     * Starts both:
     * - OfflineSyncService (gRPC server)
     * - UDPDiscoveryService (device discovery)
     *
     * Prevents duplicate starts using AtomicBoolean flags.
     */
    fun startSyncService(
        udpPort: Int,
        grpcPort: Int,
        udpPrintLog: Boolean,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        cleanUpTimeoutDelay: Long,
        deleteDeviceOnTimeout: Boolean,
        deviceId: String
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
                    putExtra(Constants.EXTRA_PRINT_LOG, udpPrintLog)
                    putExtra(Constants.EXTRA_CLEANUP_TIME_DELAY, cleanUpTimeoutDelay)
                    putExtra(Constants.EXTRA_DEVICE_ID, deviceId)
                }
        )
    }

    /**
     * Starts discovery flow.
     *
     * @param grpcPort gRPC server port
     * @param provider Optional response provider
     * @param events Optional shared flow for event streaming
     */
    override fun startDiscovery(
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
}