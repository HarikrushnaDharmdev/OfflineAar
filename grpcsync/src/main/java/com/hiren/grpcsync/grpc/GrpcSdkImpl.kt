package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatServiceGrpcKt
import com.hiren.grpcsync.db.MessageEntity
import com.hiren.grpcsync.grpc_manager.ChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class GrpcSdkImpl(
    private val serviceScope: CoroutineScope
) : GrpcSdk {

    private val channelPool = ChannelPool()
    private val serverController = ServerController(serviceScope)
    private val streamManager = StreamManager(serviceScope, channelPool)
    private val broadcastLimiter = Semaphore(10)

    override fun startServer(startPort: Int) {
        serverController.start(startPort, ChatService())
    }

    override fun stopServer() {
        serverController.stop()
    }

    override fun sendMessage(ip: String, message: MessageEntity) {
        serviceScope.launch {
            try {
                val channel = channelPool.get(ip)
                val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)
                stub.getChat(message.toGrpcRequest())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun sendMessageWithCallback(
        ip: String,
        message: MessageEntity,
        callback: (GrpcResult) -> Unit
    ) {
        serviceScope.launch {
            try {
                val channel = channelPool.get(ip)
                val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)
                val response = stub.getChat(message.toGrpcRequest())
                callback(GrpcResult.Success(ip, response.received.toString()))
            } catch (e: Exception) {
                e.printStackTrace()
                callback(GrpcResult.Error(ip, e))
            }
        }
    }

    override fun broadcast(devices: List<String>, message: MessageEntity) {
        devices.forEach { ip ->
            serviceScope.launch {
                broadcastLimiter.withPermit {
                    sendMessage(ip, message)
                }
            }
        }
    }

    override fun openStream(
        ip: String,
    ): ChatStreamSession {
        return streamManager.open(ip)
    }

    override fun shutdown() {
        stopServer()
        channelPool.shutdownAll()
        serviceScope.cancel()
    }
}