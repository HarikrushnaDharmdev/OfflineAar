package com.hiren.grpcsync.service

import android.os.Build
import android.util.Log
import com.hiren.grpcsync.db.DeviceDao
import com.hiren.grpcsync.db.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.getOrNull
import kotlin.text.split
import kotlin.text.toByteArray
import kotlin.text.toInt

/**
 * UdpBroadcastService
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
@Singleton
class UdpBroadcastService @Inject constructor(
    private val deviceDao: DeviceDao
) {

    /* ================= CONFIG ================= */

    /** UDP broadcast port (5-digit range recommended) */
    private var udpPort = 10080

    /** gRPC server port shared with other devices */
    private var grpcPort = 50051

    /** Interval between UDP broadcast packets (in milliseconds) */
    private var broadcastIntervalMs = 3000L

    /** Timeout after which a device is considered offline (in milliseconds) */
    private var deviceTimeoutMs = 10000L

    /**
     * If true:
     *   - Devices not seen within timeout are deleted
     * If false:
     *   - Devices are marked as offline
     */
    private var deleteDeviceOnTimeout = false

    /** Enable or disable log printing */
    private var printLog = true

    /* ================= COROUTINE ================= */

    /**
     * Single lifecycle-aware scope.
     * Cancelling this scope stops everything safely.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var cleanupJob: Job? = null

    /* ================= SOCKET ================= */

    @Volatile
    private var listeningSocket: DatagramSocket? = null

    /* ================= PUBLIC API ================= */

    /**
     * Starts UDP broadcasting, listening, and cleanup jobs.
     *
     * @param udpPort UDP port used for broadcasting and listening
     * @param grpcPort gRPC server port to broadcast
     * @param broadcastIntervalMs Interval between broadcasts
     * @param deviceTimeoutMs Offline timeout threshold
     * @param deleteDeviceOnTimeout Whether to delete or mark devices offline
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
        if (isRunning()) {
            log("UDP service already running")
            return
        }

        this.udpPort = udpPort
        this.grpcPort = grpcPort
        this.broadcastIntervalMs = broadcastIntervalMs
        this.deviceTimeoutMs = deviceTimeoutMs
        this.deleteDeviceOnTimeout = deleteDeviceOnTimeout
        this.printLog = printLog

        log("UDP service starting")

        broadcastJob = serviceScope.launch { broadcastLoop() }
        listenJob = serviceScope.launch { listenLoop() }
        cleanupJob = serviceScope.launch { cleanupLoop() }
    }

    /**
     * Stops all running jobs and marks all devices as offline.
     * Safely closes UDP sockets.
     */
    fun stop() {
        log("UDP service stopping")

        runBlocking {
            withContext(Dispatchers.IO) {
                deviceDao.offlineAllDevice()
            }
        }
        serviceScope.cancel()

        listeningSocket?.close()
        listeningSocket = null

    }

    /**
     * Updates the gRPC port that is broadcasted to other devices.
     */
    fun changeGrpcPort(port: Int) {
        grpcPort = port
        log("UDP change Grpc Port to $port")
    }

    /**
     * Changes UDP broadcast/listening port at runtime.
     * Restarts broadcast and listening jobs with new port.
     */
    suspend fun changeUdpPort(newPort: Int) {
        if (udpPort == newPort) {
            log("UDP can't change Grpc Port -> already to $newPort")
            return
        }

        log("UDP change Grpc Port : $udpPort → $newPort")

        listenJob?.cancelAndJoin()
        broadcastJob?.cancelAndJoin()

        listeningSocket?.close()
        listeningSocket = null

        udpPort = newPort

        broadcastJob = serviceScope.launch { broadcastLoop() }
        listenJob = serviceScope.launch { listenLoop() }
    }

    /* ================= INTERNAL ================= */

    private fun isRunning(): Boolean {
        return serviceScope.isActive &&
                (broadcastJob?.isActive == true || listenJob?.isActive == true)
    }

    /**
     * Periodically broadcasts this device info via UDP
     */
    private suspend fun broadcastLoop() = coroutineScope {
        val broadcastAddress = InetAddress.getByName("255.255.255.255")

        DatagramSocket().use { socket ->
            socket.broadcast = true

            log("Broadcast started on port $udpPort")

            while (isActive) {
                try {
                    val payload = "${Build.MODEL},$grpcPort"
                    val data = payload.toByteArray()

                    val packet = DatagramPacket(
                        data,
                        data.size,
                        broadcastAddress,
                        udpPort
                    )

                    socket.send(packet)
                    log("Broadcast socket sent: $payload")
                } catch (e: Exception) {
                    log("Broadcast error: ${e.message}")
                }

                delay(broadcastIntervalMs)
            }
        }
    }

    /**
     * Listens for incoming UDP broadcasts
     */
    private suspend fun listenLoop() = coroutineScope {
        val socket = DatagramSocket(udpPort, InetAddress.getByName("0.0.0.0"))
        listeningSocket = socket

        socket.broadcast = true
        //socket.soTimeout = 2000

        log("Listening on UDP port $udpPort")

        while (isActive) {
            try {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                socket.receive(packet)
                log("Broadcast socket received")
                handleIncomingPacket(packet)
            } catch (e: SocketTimeoutException) {
                // allow coroutine cancellation
                log("SocketTimeoutException error: ${e.message}")
            } catch (e: SocketException) {
                if (!isActive) return@coroutineScope
                log("Socket error: ${e.message}")
            } catch (e: Exception) {
                log("Listen error: ${e.message}")
            }
        }

        socket.close()
        listeningSocket = null
    }

    /**
     * Processes received UDP packet
     */
    private suspend fun handleIncomingPacket(packet: DatagramPacket) {
        val message = String(packet.data, 0, packet.length)
        val parts = message.split(",")

        // neglect Strict checking.. as Some devices may send incomplete info
        //if (parts.size != 2) return

        val device = DeviceEntity(
            id = packet.address.hostAddress ?: return,
            name = parts.getOrNull(0) ?: "-",
            port = parts.getOrNull(1)?.toInt() ?: 0,
            status = true,
            lastSeen = System.currentTimeMillis()
        )
        log("Broadcast update from device: $device")

        deviceDao.upsertDevice(device)
    }

    /**
     * Periodically removes or marks stale devices
     */
    private suspend fun cleanupLoop() {
        log("Cleanup job started")

        while (serviceScope.isActive) {
            val now = System.currentTimeMillis()

            if (deleteDeviceOnTimeout) {
                deviceDao.removeStaleDevices(now, deviceTimeoutMs)
            } else {
                deviceDao.updateStaleDevices(now, deviceTimeoutMs)
            }

            delay(2500)
        }
    }

    private fun log(msg: String) {
        if (printLog) {
            Log.d("UdpBroadcastService", msg)
        }
    }
}
