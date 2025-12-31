package com.hiren.grpcsync.public_classes

import android.content.Context
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.MessageEntity
import com.hiren.grpcsync.grpc_manager.GrpcResult
import kotlinx.coroutines.flow.Flow

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

    fun startDiscovery()

    fun stopDiscovery()

    fun sendMessage(ip: String, payload: MessageEntity)

    fun sendMessageWithCallback(ip: String, payload: MessageEntity, callbackFlow: Flow<GrpcResult>)

    fun broadcast(devices: List<String>, payload: MessageEntity)

    fun getDevices(): Flow<List<DeviceEntity>>
}