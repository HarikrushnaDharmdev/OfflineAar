package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.db.MessageEntity
import kotlinx.coroutines.flow.MutableSharedFlow

interface GrpcSdk {

    /* Server lifecycle */
    fun startServer(startPort: Int)

    fun stopServer()

    /* 1. Normal send */
    fun sendMessage(
        ip: String,
        port: Int,
        message: MessageEntity
    )

    /* 2. Send with callback */
    fun sendMessageWithCallback(
        ip: String,
        port: Int,
        message: MessageEntity,
        callback: (GrpcResult) -> Unit
    )

    /* 3. Broadcast */
    fun broadcast(
        devices: List<String>,
        message: MessageEntity
    )

    /* 4. Stream */
    fun openStream(
        ip: String,
        port: Int,
    ): ChatStreamSession

    /* Cleanup */
    fun shutdown()

    fun registerResponseProvider(provider: ChatResponseProvider)

    fun registerGrpcEvents(events: MutableSharedFlow<GrpcEvent>)
}