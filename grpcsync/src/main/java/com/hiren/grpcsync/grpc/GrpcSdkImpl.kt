package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatServiceGrpcKt
import com.hiren.grpcsync.db.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private var responseProvider: ChatResponseProvider? = null
    private var events: MutableSharedFlow<GrpcEvent>? = null

    override fun startServer(startPort: Int) {
        serverController.start(
            startPort,
            service = ChatService(events = events, responseProvider = responseProvider, scope = serviceScope)
        )
    }

    override fun stopServer() {
        serverController.stop()
    }

    override fun registerResponseProvider(provider: ChatResponseProvider) {
        this.responseProvider = provider
    }

    override fun registerGrpcEvents(events: MutableSharedFlow<GrpcEvent>) {
        this.events = events
    }

    override fun sendMessage(ip: String, port: Int, message: MessageEntity) {
        serviceScope.launch {
            try {
                val channel = channelPool.get(ip, port)
                val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)
                stub.getChat(message.toGrpcRequest())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun sendMessageWithCallback(
        ip: String,
        port: Int,
        message: MessageEntity,
        callback: (GrpcResult) -> Unit
    ) {
        serviceScope.launch {
            try {
                val channel = channelPool.get(ip, port)
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
                    sendMessage(ip, 50051, message)
                }
            }
        }
    }

    override fun openStream(
        ip: String,
        port: Int,
    ): ChatStreamSession {
        return streamManager.open(ip, port)
    }

    override fun shutdown() {
        stopServer()
        channelPool.shutdownAll()
        serviceScope.cancel()
    }
}