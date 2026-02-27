package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.db.DeviceIpPort
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.repo.MessageRepository
import com.hiren.grpcsync.utils.BroadCastDevice
import kotlinx.coroutines.flow.MutableSharedFlow

internal interface GrpcSdk {

    fun startServer(
        startPort: Int,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    )

    fun stopServer()

    fun sendMessage(
        ip: String,
        port: Int,
        message: Message,
        retryIfFail: Boolean,
        messageRepository: MessageRepository,
    )

    fun sendMessageWithCallback(
        ip: String,
        port: Int,
        message: Message,
        callback: (GrpcResult) -> Unit,
        retryIfFail: Boolean,
        messageRepository: MessageRepository,
    )

    fun sendBroadcast(
        deviceRepository: DeviceRepository,
        targets: List<DeviceIpPort>?,
        deviceId: String,
        message: Message,
        maxConcurrency: Int,
        deviceFilter: BroadCastDevice,
        messageRepository: MessageRepository,
    )

    fun openStream(
        ip: String,
        port: Int,
    ): ChatStreamSession

    fun shutdown()

    fun restartServer(port: Int)

    fun serverIsRunning(): Boolean
}