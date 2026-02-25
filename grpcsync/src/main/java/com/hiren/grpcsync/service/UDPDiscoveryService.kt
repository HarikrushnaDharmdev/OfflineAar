package com.hiren.grpcsync.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.hiren.grpcsync.udp.UdpHelper
import com.hiren.grpcsync.utils.Constants
import com.hiren.grpcsync.utils.NotificationHelper.setNotification
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * UDPDiscoveryService
 *
 * Foreground service responsible for:
 *  - Broadcasting UDP discovery packets
 *  - Listening for devices on the local network
 *  - Maintaining device presence and timeouts
 *
 * This service is designed to:
 *  - Be START_STICKY (resilient to process kill)
 *  - Run safely on Android 12+ as a foreground service
 *  - Prevent duplicate starts using an atomic guard
 */
@AndroidEntryPoint
class UDPDiscoveryService : LifecycleService() {

    companion object {
        /**
         * Enables verbose logging across the UDP discovery module.
         * Controlled externally via intent extra.
         */
        var printLog = false
        var cleanUpTimeoutDelay = 2500L
    }

    /**
     * Helper responsible for actual UDP socket operations and discovery logic.
     * Injected via Hilt to keep service lightweight.
     */
    @Inject
    lateinit var udpHelper: UdpHelper

    /**
     * Atomic flag to ensure discovery is started only once.
     *
     * Prevents:
     *  - Multiple socket binds
     *  - Duplicate broadcast loops
     *  - EADDRINUSE crashes
     */
    private val started by lazy { AtomicBoolean(false) }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * Entry point for controlling the discovery service.
     *
     * Supported actions:
     *  - START discovery
     *  - STOP discovery
     *  - Change UDP port dynamically
     *  - Change gRPC port dynamically
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(2, setNotification("Retailz Cloud Sync.....", this))

        // Defensive: system restart may deliver null intent
        if (intent == null) return START_STICKY

        val udpType = intent.getStringExtra(Constants.EXTRA_UDP_TYPE)

        when (udpType) {
            Constants.UDP_START -> {
                // 🔒 Prevent duplicate start
                if (started.getAndSet(true))
                    return START_STICKY

                val udpPort =
                    intent.getIntExtra(
                        Constants.EXTRA_UDP_PORT,
                        Constants.DEFAULT_UDP_PORT
                    )
                val grpcPort =
                    intent.getIntExtra(
                        Constants.EXTRA_GRPC_PORT,
                        Constants.DEFAULT_GRPC_PORT
                    )
                val broadcastInterval =
                    intent.getLongExtra(
                        Constants.EXTRA_BROADCAST_INTERVAL,
                        Constants.DEFAULT_BROADCAST_INTERVAL
                    )
                val deviceTimeout =
                    intent.getLongExtra(
                        Constants.EXTRA_DEVICE_TIMEOUT,
                        Constants.DEFAULT_DEVICE_TIMEOUT
                    )
                val deleteOnTimeout =
                    intent.getBooleanExtra(
                        Constants.EXTRA_DELETE_ON_TIMEOUT,
                        false
                    )
                printLog =
                    intent.getBooleanExtra(
                        Constants.EXTRA_PRINT_LOG,
                        false
                    )

                cleanUpTimeoutDelay =
                    intent.getLongExtra(
                        Constants.EXTRA_CLEANUP_TIME_DELAY,
                        2500L
                    )

                try {
                    udpHelper.start(
                        udpPort = udpPort,
                        grpcPort = grpcPort,
                        broadcastIntervalMs = broadcastInterval,
                        deviceTimeoutMs = deviceTimeout,
                        deleteDeviceOnTimeout = deleteOnTimeout
                    )
                } catch (e: Exception) {
                    started.set(false) // rollback on failure
                    e.printStackTrace()
                }
            }

            Constants.UDP_STOP -> {
                started.set(false)
                stopSelf()
            }

            Constants.UDP_STOP_DISCOVERY -> {
                started.set(false)
                stopDiscovery()
            }

            Constants.UDP_CHANGE_UDP_PORT -> {
                val newUdpPort =
                    intent.getIntExtra(
                        Constants.EXTRA_UDP_PORT,
                        Constants.DEFAULT_UDP_PORT
                    )
                udpHelper.changeUdpPort(newUdpPort)
            }

            Constants.UDP_CHANGE_GRPC_PORT -> {
                val newGrpcPort =
                    intent.getIntExtra(
                        Constants.EXTRA_GRPC_PORT,
                        Constants.DEFAULT_GRPC_PORT
                    )
                udpHelper.changeGrpcPort(newGrpcPort)
            }
        }

        return START_STICKY
    }

    /**
     * Called when service is destroyed by:
     *  - User action
     *  - System reclaiming resources
     *  - Explicit stopSelf()
     */
    override fun onDestroy() {
        stopDiscovery()
        stopForegroundCompat()
        super.onDestroy()
    }

    /**
     * Clean shutdown of discovery components.
     *
     * Ensures:
     *  - UDP sockets are closed
     *  - Coroutines are canceled
     *  - No background leaks remain
     */
    fun stopDiscovery() {
        udpHelper.stop()
    }

    private fun stopForegroundCompat() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

}