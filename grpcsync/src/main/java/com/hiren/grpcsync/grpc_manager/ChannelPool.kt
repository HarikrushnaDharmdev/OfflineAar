package com.hiren.grpcsync.grpc_manager

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.ConcurrentHashMap

object ChannelPool {

    private val channels = ConcurrentHashMap<String, ManagedChannel>()

    fun get(ip: String, port: Int): ManagedChannel {
        return channels.getOrPut("$ip:$port") {
            ManagedChannelBuilder
                .forAddress(ip, port)
                .usePlaintext()
                .build()
        }
    }

    fun shutdownAll() {
        channels.values.forEach { it.shutdown() }
        channels.clear()
    }
}
