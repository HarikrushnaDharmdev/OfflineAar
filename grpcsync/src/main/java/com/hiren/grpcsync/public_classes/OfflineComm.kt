package com.hiren.grpcsync.public_classes

import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.DeviceIpPort
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.utils.BroadCastDevice
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
        udpPrintLog: Boolean = Constants.PRINT_LOG,
        responseTimeout: Long = Constants.RESPONSE_TIMEOUT_MS,
        maxBroadcastDevicesConcurrency: Int = Constants.MAX_BROADCAST_DEVICES_CONCURRENCY,
        cleanUpTimeoutDelay: Long = 2500L,
        deviceId: String
    )

    fun startServiceOnCustomModeWithStartDiscovery(
        udpPort: Int = Constants.DEFAULT_UDP_PORT,
        grpcPort: Int = Constants.DEFAULT_GRPC_PORT,
        broadcastIntervalMs: Long = Constants.DEFAULT_BROADCAST_INTERVAL,
        deviceTimeoutMs: Long = Constants.DEFAULT_DEVICE_TIMEOUT,
        deleteDeviceOnTimeout: Boolean = false,
        printLog: Boolean = Constants.PRINT_LOG,
        udpPrintLog: Boolean = Constants.PRINT_LOG,
        responseTimeout: Long = Constants.RESPONSE_TIMEOUT_MS,
        maxBroadcastDevicesConcurrency: Int = Constants.MAX_BROADCAST_DEVICES_CONCURRENCY,
        cleanUpTimeoutDelay: Long = 2500L,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?,
        deviceId: String
    )

    fun startServiceOnBoosterMode(
        udpPort: Int = Constants.DEFAULT_UDP_PORT,
        grpcPort: Int = Constants.DEFAULT_GRPC_PORT,
        printLog: Boolean = Constants.PRINT_LOG,
        udpPrintLog: Boolean = Constants.PRINT_LOG,
        deviceId: String
    )

    fun startServiceOnEnergySaving(
        udpPort: Int = Constants.DEFAULT_UDP_PORT,
        grpcPort: Int = Constants.DEFAULT_GRPC_PORT,
        printLog: Boolean = Constants.PRINT_LOG,
        udpPrintLog: Boolean = Constants.PRINT_LOG,
        deviceId: String
    )

    fun startDiscovery(
        grpcPort: Int,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    )

    fun stopService()

    fun stopDiscovery()

    fun sendMessage(ip: String, port: Int, payload: Message, retryIfFail: Boolean = true)

    fun sendMessageWithCallback(
        ip: String,
        port: Int,
        payload: Message,
        retryIfFail: Boolean = true,
        callback: (GrpcResult) -> Unit
    )

    fun sendMessageBroadcast(
        targets: List<DeviceIpPort>? = null,
        message: Message,
        maxConcurrency: Int = Constants.MAX_BROADCAST_DEVICES_CONCURRENCY,
        deviceFilter: BroadCastDevice = BroadCastDevice.ALL(includeSelf = false)
    )

    fun getDevices(): Flow<List<DeviceEntity>>

    fun changeUdpPort(port: Int)

    fun changeGrpcPort(port: Int)

}