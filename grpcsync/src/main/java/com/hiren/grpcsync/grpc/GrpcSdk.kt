package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatResponse
import com.hiren.grpcsync.db.MessageEntity

interface GrpcSdk {

    /* Server lifecycle */
    fun startServer(startPort: Int = 50051)

    fun stopServer()

    /* 1. Normal send */
    fun sendMessage(
        ip: String,
        message: MessageEntity
    )

    /* 2. Send with callback */
    fun sendMessageWithCallback(
        ip: String,
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
    ): ChatStreamSession

    /* Cleanup */
    fun shutdown()
}