package com.hiren.grpcsync.public_classes

import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface OfflineComm {

    fun startServiceOnCustomMode(
        udpPort: Int = Constants.DEFAULT_UDP_PORT,
        grpcPort: Int = Constants.DEFAULT_GRPC_PORT,
        broadcastIntervalMs: Long = Constants.DEFAULT_BROADCAST_INTERVAL,
        deviceTimeoutMs: Long = Constants.DEFAULT_DEVICE_TIMEOUT,
        deleteDeviceOnTimeout: Boolean = false,
        printLog: Boolean = Constants.PRINT_LOG,
        responseTimeout: Long = Constants.RESPONSE_TIMEOUT_MS,
        maxBroadcastDevicesConcurrency: Int = Constants.MAX_BROADCAST_DEVICES_CONCURRENCY,
        cleanUpTimeoutDelay: Long = 2500L,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    )

    fun startServiceOnBoosterMode(
        udpPort: Int = Constants.DEFAULT_UDP_PORT,
        grpcPort: Int = Constants.DEFAULT_GRPC_PORT,
        printLog: Boolean = Constants.PRINT_LOG,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    )

    fun startServiceOnEnergySaving(
        udpPort: Int = Constants.DEFAULT_UDP_PORT,
        grpcPort: Int = Constants.DEFAULT_GRPC_PORT,
        printLog: Boolean = Constants.PRINT_LOG,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    )

    fun stopService()

    fun sendMessage(ip: String, port: Int, payload: Message)

    fun sendMessageWithCallback(
        ip: String,
        port: Int,
        payload: Message,
        callback: (GrpcResult) -> Unit
    )

    fun sendMessageBroadcast(
        targets: List<Pair<String, Int>>? = null,
        message: Message,
        maxConcurrency: Int = Constants.MAX_BROADCAST_DEVICES_CONCURRENCY
    )

    fun getDevices(): Flow<List<DeviceEntity>>
    fun changeUdpPort(port: Int)

    fun changeGrpcPort(port: Int)

}