package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.db.Message
import kotlinx.coroutines.flow.MutableSharedFlow

interface GrpcSdk {

    /* Server lifecycle */
    fun startServer(
        startPort: Int,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    )

    fun stopServer()

    /* 1. Normal send */
    fun sendMessage(
        ip: String,
        port: Int,
        message: Message
    )

    /* 2. Send with callback */
    fun sendMessageWithCallback(
        ip: String,
        port: Int,
        message: Message,
        callback: (GrpcResult) -> Unit
    )

    /* 3. Broadcast */
    fun broadcast(
        devices: List<String>,
        message: Message
    )

    /* 4. Stream */
    fun openStream(
        ip: String,
        port: Int,
    ): ChatStreamSession

    /* Cleanup */
    fun shutdown()
}