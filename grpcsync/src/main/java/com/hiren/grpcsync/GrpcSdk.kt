package com.hiren.grpcsync

import android.content.Context
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.MessageEntity
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.grpc_manager.ChatStreamSession
import kotlinx.coroutines.flow.Flow

internal interface GrpcSdk {
    fun start(context: Context)

    fun stop(context: Context)

    fun sendMessage(ip: String, payload: MessageEntity)

    fun sendMessageWithCallback(ip: String, payload: MessageEntity): Flow<GrpcResult>

    fun broadcast(devices: List<String>, payload: MessageEntity)

    fun openChatStream(
        ip: String
    ): ChatStreamSession

    fun getDevices(): Flow<List<DeviceEntity>>
}