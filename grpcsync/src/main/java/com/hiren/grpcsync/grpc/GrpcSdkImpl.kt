package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatServiceGrpcKt
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.utils.Utils.eventLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class GrpcSdkImpl(
    private val serviceScope: CoroutineScope,
) : GrpcSdk {

    private val serverController by lazy {
        ServerController(serviceScope)
    }

    // For Bidirectional streaming --------------

    private val channelPool by lazy {
        ChannelPool()
    }

    private val streamManager by lazy {
        StreamManager(serviceScope, channelPool)
    }

    // -----------------------------------------

    private var responseProvider: ChatResponseProvider? = null
    private var events: MutableSharedFlow<GrpcEvent>? = null

    override fun startServer(
        startPort: Int,
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        this.responseProvider = provider
        this.events = events

        eventLog(
            "GrpcSdkImpl",
            "startServer with port: $startPort, provider: $provider, events: $events"
        )

        serverController.start(
            port = startPort,
            service = ChatService(
                events = events,
                responseProvider = responseProvider,
                scope = serviceScope
            ),
            events = events
        )
    }

    override fun stopServer() {
        eventLog(
            "GrpcSdkImpl",
            "stopServer called"
        )
        serverController.stop(events)
    }

    override fun sendMessage(ip: String, port: Int, message: Message) {
        eventLog(
            "GrpcSdkImpl",
            "sendMessage to $ip:$port with message: $message"
        )
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
        message: Message,
        callback: (GrpcResult) -> Unit
    ) {
        eventLog(
            "GrpcSdkImpl",
            "sendMessageWithCallback to $ip:$port with message: $message"
        )
        serviceScope.launch {
            try {
                val channel = channelPool.get(ip, port)
                val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)
                val response = stub.getChat(message.toGrpcRequest())
                callback(GrpcResult.Success(ip, response.toString()))
            } catch (e: Exception) {
                e.printStackTrace()
                callback(GrpcResult.Error(ip, e))
            }
        }
    }

    override fun broadcastFireAndForget(
        deviceRepository: DeviceRepository,
        targets: List<Pair<String, Int>>?,
        message: Message,
        maxConcurrency: Int
    ) {
        eventLog(
            "GrpcSdkImpl",
            "broadcastFireAndForget to targets: $targets with message: $message and maxConcurrency: $maxConcurrency"
        )

        val semaphore = Semaphore(maxConcurrency)

        serviceScope.launch {
            val devices = if (targets.isNullOrEmpty()) {
                deviceRepository.getAllDevices().map { it.id to it.port }
            } else {
                targets
            }
            devices.forEach { (ip, port) ->
                launch {
                    semaphore.withPermit {
                        try {
                            val channel = channelPool.get(ip, port)
                            val stub =
                                ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)

                            stub.getChat(message.toGrpcRequest())
                        } catch (_: Exception) {
                        }
                    }
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
        eventLog(
            "GrpcSdkImpl",
            "shutdown called"
        )
        stopServer()
        channelPool.shutdownAll()
        serviceScope.cancel()
    }

    override fun restartServer(port: Int) {
        eventLog(
            "GrpcSdkImpl",
            "restartServer called with port: $port"
        )
        channelPool.shutdownAll()
        serverController.restart(
            port, events = events,
            service = ChatService(
                events = events,
                responseProvider = responseProvider,
                scope = serviceScope
            ),
        )
    }
}