package com.hiren.grpcsync.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.ConcurrentHashMap

class ChannelPool {

    private val channels = ConcurrentHashMap<String, ManagedChannel>()

    fun get(ip: String, port: Int = 50051): ManagedChannel {
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