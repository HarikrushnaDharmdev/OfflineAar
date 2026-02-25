package com.hiren.grpcsync.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.ConcurrentHashMap

/**
 * ChannelPool is responsible for managing and reusing gRPC [ManagedChannel] instances.
 *
 * Each channel is uniquely identified by a combination of `ip` and `port`.
 * Internally, channels are cached to avoid the overhead of repeatedly creating
 * and tearing down gRPC connections.
 *
 * This class is thread-safe and can be accessed concurrently from multiple
 * coroutines or threads.
 *
 * Typical usage:
 * - Reuse channels for multiple gRPC calls to the same device
 * - Maintain long-lived connections for streaming RPCs
 * - Centralized shutdown during SDK or service teardown
 *
 *  Created On    : 24-02-2026 |
 *  Author        : Hiren Patel
 */
class ChannelPool {

    /**
     * Cache of active gRPC channels.
     *
     * Key format: "ip:port"
     * Value: [ManagedChannel] associated with the target device
     *
     * [ConcurrentHashMap] ensures safe access from multiple threads.
     */
    private val channels = ConcurrentHashMap<String, ManagedChannel>()

    /**
     * Returns an existing [ManagedChannel] for the given `ip` and `port`,
     * or creates a new one if it does not already exist.
     *
     * Channels are created using plaintext communication.
     * (TLS can be enabled later if required.)
     *
     * @param ip Target device IP address
     * @param port Target gRPC port
     * @return Reused or newly created [ManagedChannel]
     */
    fun get(ip: String, port: Int): ManagedChannel {
        return channels.getOrPut("$ip:$port") {
            ManagedChannelBuilder
                .forAddress(ip, port)
                .usePlaintext()
                .build()
        }
    }

    /**
     * Shuts down all active gRPC channels and clears the internal cache.
     *
     * This should be called when:
     * - The SDK is stopped
     * - The foreground service is destroyed
     * - The application is shutting down
     *
     * After calling this method, all channels will be recreated on demand.
     */
    fun shutdownAll() {
        channels.values.forEach { it.shutdown() }
        channels.clear()
    }
}