package com.hiren.grpcsync.udp

import android.os.Build
import android.util.Log
import com.hiren.grpcsync.db.DeviceDao
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.service.UDPDiscoveryService
import com.hiren.grpcsync.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

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
class UdpHelper @Inject constructor(
    private val deviceDao: DeviceDao
) {

    /* ================= CONFIG ================= */

    /** UDP broadcast port (5-digit range recommended) */
    private var udpPort = Constants.DEFAULT_UDP_PORT

    /** gRPC server port shared with other devices */
    private var grpcPort = Constants.DEFAULT_GRPC_PORT

    /** Interval between UDP broadcast packets (in milliseconds) */
    private var broadcastIntervalMs = Constants.DEFAULT_BROADCAST_INTERVAL

    /** Timeout after which a device is considered offline (in milliseconds) */
    private var deviceTimeoutMs = Constants.DEFAULT_DEVICE_TIMEOUT

    /**
     * If true:
     *   - Devices not seen within timeout are deleted
     * If false:
     *   - Devices are marked as offline
     */
    private var deleteDeviceOnTimeout = false

    /* ================= COROUTINE ================= */

    /**
     * Single lifecycle-aware scope.
     * Cancelling this scope stops everything safely.
     */
    private var serviceScope: CoroutineScope? = null

    @Volatile
    private var running: Boolean = false

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
     */
    fun start(
        udpPort: Int = 10080,
        grpcPort: Int = 50051,
        broadcastIntervalMs: Long = 3000L,
        deviceTimeoutMs: Long = 10000L,
        deleteDeviceOnTimeout: Boolean = false
    ) {
        if (running) {
            log("UDP service already running")
            return
        }

        this.udpPort = udpPort
        this.grpcPort = grpcPort
        this.broadcastIntervalMs = broadcastIntervalMs
        this.deviceTimeoutMs = deviceTimeoutMs
        this.deleteDeviceOnTimeout = deleteDeviceOnTimeout

        running = true
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        log("UDP discovery started on port $udpPort")

        broadcastJob = serviceScope?.launch { broadcastLoop() }
        listenJob = serviceScope?.launch { listenLoop() }
        cleanupJob = serviceScope?.launch { cleanupLoop() }
    }

    /**
     * Stops all running jobs and marks all devices as offline.
     * Safely closes UDP sockets.
     */
    fun stop(needOfflineDevices: Boolean = true) {
        if (!running) return

        running = false
        log("UDP service stopping")

        serviceScope?.launch {
            if (needOfflineDevices) {
                deviceDao.offlineAllDevice()
            }
        }
        /* if (needOfflineDevices)
             runBlocking {
                 withContext(Dispatchers.IO) {
                     deviceDao.offlineAllDevice()
                 }
             }*/

        broadcastJob?.cancel()
        broadcastJob = null

        listenJob?.cancel()
        listenJob = null

        cleanupJob?.cancel()
        cleanupJob = null

        listeningSocket?.close()
        listeningSocket = null

        serviceScope?.cancel()
        serviceScope = null
    }

    /**
     * Updates the gRPC port that is broadcasted to other devices.
     */
    fun changeGrpcPort(port: Int) {
        grpcPort = port
        log("gRPC port updated to $port")
    }

    /**
     * Changes UDP broadcast/listening port at runtime.
     * Restarts broadcast and listening jobs with new port.
     */
    fun changeUdpPort(newPort: Int) {
        if (udpPort == newPort) {
            log("UDP can't change Grpc Port -> already to $newPort")
            return
        }

        log("UDP change Grpc Port : $udpPort → $newPort")

        udpPort = newPort
        stop(false)
        start(
            udpPort = udpPort,
            grpcPort = grpcPort,
            broadcastIntervalMs = broadcastIntervalMs,
            deviceTimeoutMs = deviceTimeoutMs,
            deleteDeviceOnTimeout = deleteDeviceOnTimeout
        )
    }

    /* ================= INTERNAL ================= */

    /*private fun isRunning(): Boolean {
        return if (serviceScope == null) false
        else serviceScope!!.isActive &&
                (broadcastJob?.isActive == true || listenJob?.isActive == true)
    }*/

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

                    log("Broadcast sent: $payload")
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
                if (!isActive) break
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

        // neglect Strict checking as Some devices may send incomplete info
        //if (parts.size != 2) return

        val device = DeviceEntity(
            id = packet.address.hostAddress ?: return,
            name = parts.getOrNull(0) ?: "-",
            port = parts.getOrNull(1)?.toInt() ?: 0,
            status = true,
            lastSeen = System.currentTimeMillis(),
            note = ""
        )
        log("Broadcast update from device: $device")

        deviceDao.upsertDevice(device)
    }

    /**
     * Periodically removes or marks stale devices
     */
    private suspend fun cleanupLoop() {
        log("Cleanup job started")

        while (serviceScope?.isActive == true) {
            val now = System.currentTimeMillis()

            if (deleteDeviceOnTimeout) {
                deviceDao.removeStaleDevices(now, deviceTimeoutMs)
            } else {
                deviceDao.updateStaleDevices(now, deviceTimeoutMs)
            }

            delay(UDPDiscoveryService.cleanUpTimeoutDelay)
        }
    }

    private fun log(msg: String) {
        if (UDPDiscoveryService.printLog) {
            //if (false) {
            Log.d("GRPC SYNC ->> UdpHelper", msg)
        }
    }
}