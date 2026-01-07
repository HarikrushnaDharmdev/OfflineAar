package com.hiren.grpcsync.public_classes

import android.content.Context
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface OfflineComm {

    fun startService(
        context: Context,
        udpPort: Int = 10080,
        grpcPort: Int = 50051,
        broadcastIntervalMs: Long = 3000L,
        deviceTimeoutMs: Long = 10000L,
        deleteDeviceOnTimeout: Boolean = false,
        printLog: Boolean = true
    )

    fun stopService(context: Context)

    fun startDiscovery(
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    )

    fun stopDiscovery()

    fun sendMessage(ip: String, port: Int, payload: Message)

    fun sendMessageWithCallback(
        ip: String,
        port: Int,
        payload: Message,
        callback: (GrpcResult) -> Unit
    )

    fun broadcast(devices: List<String>, payload: Message)

    fun getDevices(): Flow<List<DeviceEntity>>
    fun changeUdpPort(port: Int)

    fun changeGrpcPort(port: Int)

}