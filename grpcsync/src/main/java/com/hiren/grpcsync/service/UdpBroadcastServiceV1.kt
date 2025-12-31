package com.hiren.grpcsync.service

import android.os.Build
import android.util.Log
import com.hiren.grpcsync.db.DeviceDao
import com.hiren.grpcsync.db.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.collections.getOrNull
import kotlin.text.split
import kotlin.text.toByteArray
import kotlin.text.toInt

/**
 * UdpBroadcastServiceV1
 *
 * Responsible for:
 * 1. Broadcasting this device's presence over UDP at a fixed interval
 * 2. Listening for UDP broadcasts from other devices
 * 3. Maintaining device availability state in local database
 *
 * This service helps in device discovery within the same local network
 * and shares the gRPC port for further communication.
 *
 * Threading:
 * - Uses IO dispatcher for all network and database operations
 * - Runs broadcasting, listening, and cleanup as independent coroutines
 *
 * Lifecycle:
 * - start() initializes all jobs
 * - stop() gracefully cancels jobs and closes sockets
 */
class UdpBroadcastServiceV1 @Inject constructor(
    private val deviceDao: DeviceDao
) {
    /** UDP broadcast port (5-digit range recommended) */
    private var port = 10080

    /** gRPC server port shared with other devices */
    private var grpcPort = 50051

    /** Interval between UDP broadcast packets (in milliseconds) */
    private var broadcastInterval = 3000L

    /** Timeout after which a device is considered offline (in milliseconds) */
    private var timeout = 10000L

    /**
     * If true:
     *   - Devices not seen within timeout are deleted
     * If false:
     *   - Devices are marked as offline
     */
    private var deleteDeviceOnNotAvailable = false

    /** Enable or disable log printing */
    private var printLog = true

    /** Atomic flag to control service lifecycle safely across threads */
    private var isRunning = AtomicBoolean(false)

    /** Socket used for listening incoming UDP packets */
    private var listeningSocket: DatagramSocket? = null

    /** Coroutine jobs */
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var cleanupJob: Job? = null

    /**
     * Starts UDP broadcasting, listening, and cleanup jobs.
     *
     * @param udpPORT UDP port used for broadcasting and listening
     * @param grpcPORT gRPC server port to broadcast
     * @param broadcastIntervalMS Interval between broadcasts
     * @param timeoutMS Offline timeout threshold
     * @param deleteDeviceOnNotAvailable Whether to delete or mark devices offline
     * @param printLog Enable debug logs
     */
    fun start(
        udpPort: Int = 10080,
        grpcPort: Int = 50051,
        broadcastIntervalMs: Long = 3000L,
        deviceTimeoutMs: Long = 10000L,
        deleteDeviceOnTimeout: Boolean = false,
        printLog: Boolean = true
    ) {
        if (isRunning.getAndSet(true)) return

        this.port = udpPort
        this.grpcPort = grpcPort
        this.broadcastInterval = broadcastIntervalMs
        this.timeout = deviceTimeoutMs
        this.deleteDeviceOnNotAvailable = deleteDeviceOnTimeout
        this.printLog = printLog

        val scope = CoroutineScope(Dispatchers.IO)

        // Start UDP broadcast sender
        broadcastJob = scope.launch { startBroadcasting() }

        // Start UDP listener
        listenJob = scope.launch { startListening() }

        // Start stale device cleanup job
        cleanupJob = scope.launch { startCleanupJob() }
    }

    /**
     * Stops all running jobs and marks all devices as offline.
     * Safely closes UDP sockets.
     */
    fun stop() {
        isRunning.set(false)

        // Mark all devices as offline when service stops
        CoroutineScope(Dispatchers.IO).launch {
            deviceDao.offlineAllDevice()
        }

        broadcastJob?.cancel()
        listenJob?.cancel()
        cleanupJob?.cancel()

        listeningSocket?.close()
    }

    /**
     * Changes UDP broadcast/listening port at runtime.
     * Restarts broadcast and listening jobs with new port.
     */
    fun changePort(port: Int) {
        isRunning.set(false)

        this.port = port

        // Restart broadcasting/listening jobs
        broadcastJob?.cancel()
        listenJob?.cancel()

        isRunning.set(true)
        val scope = CoroutineScope(Dispatchers.IO)

        broadcastJob = scope.launch { startBroadcasting() }
        listenJob = scope.launch { startListening() }

        addLog("Switched to new UDP port: $port")
    }

    /**
     * Updates the gRPC port that is broadcasted to other devices.
     */
    fun changeGrpcPort(port: Int) {
        grpcPort = port
    }

    /**
     * Sends UDP broadcast packets periodically.
     *
     * Packet format:
     *   "<DEVICE_MODEL>,<GRPC_PORT>"
     */
    private suspend fun startBroadcasting() {
        try {
            addLog("StartBroadcasting")

            val socket = DatagramSocket()
            socket.broadcast = true

            val broadcastAddress = InetAddress.getByName("255.255.255.255")

            while (isRunning.get()) {
                try {
                    val message = "${Build.MODEL},$grpcPort"
                    val buffer = message.toByteArray()

                    val packet = DatagramPacket(
                        buffer,
                        buffer.size,
                        broadcastAddress,
                        port
                    )

                    socket.send(packet)
                    addLog("Broadcast packet sent")
                } catch (e: Exception) {
                    e.printStackTrace()
                    addLog("Broadcast error: ${e.message}")
                } finally {
                    delay(broadcastInterval)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            addLog("startBroadcasting failed: ${e.message}")
        }
    }

    /**
     * Listens for incoming UDP broadcast packets.
     * Updates or inserts devices into the database.
     */
    private suspend fun startListening() {
        listeningSocket = DatagramSocket(port, InetAddress.getByName("0.0.0.0"))
        listeningSocket?.broadcast = true

        val buffer = ByteArray(1024)
        addLog("startListening")

        while (isRunning.get()) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                listeningSocket?.receive(packet)

                val message = String(packet.data, 0, packet.length)
                val parts = message.split(",")

                if (parts.size == 2) {
                    val device =
                        DeviceEntity(
                            id = packet.address.hostAddress ?: "",
                            name = parts.getOrNull(0) ?: "-",
                            status = true,
                            port = parts.getOrNull(1)?.toInt() ?: 0,
                            lastSeen = System.currentTimeMillis()
                        )

                    addLog("Device discovered: ${device.id}")
                    deviceDao.upsertDevice(device)
                }
            } catch (e: IOException) {
                if (isRunning.get())
                    e.printStackTrace() // only log if not intentionally stopped
                addLog("found device IOException")
            }
        }

        listeningSocket?.close()
        addLog("Listening socket closed")
    }

    /**
     * Periodically checks for stale devices and:
     * - Deletes them OR
     * - Marks them offline
     */
    private suspend fun startCleanupJob() {
        try {
            addLog("startCleanupJob")

            while (isRunning.get()) {
                val now = System.currentTimeMillis()

                if (deleteDeviceOnNotAvailable) {
                    // Delete offline devices
                    deviceDao.removeStaleDevices(now, timeout)
                    addLog("Stale devices removed")
                } else {
                    // Update status of devices
                    deviceDao.updateStaleDevices(now, timeout)
                    addLog("Stale devices marked offline")
                }

                delay(2500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Logs messages conditionally based on configuration.
     */
    fun addLog(msg: String) {
        if (printLog)
            Log.e("UdpBroadcastService -> ", msg)
    }
}