package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatServiceGrpcKt
import com.hiren.grpcsync.db.DeviceIpPort
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.repo.MessageRepository
import com.hiren.grpcsync.utils.BroadCastDevice
import com.hiren.grpcsync.utils.MessageType
import com.hiren.grpcsync.utils.Utils.eventLog
import com.hiren.grpcsync.utils.toGrpcSafeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/**
 * Core gRPC SDK implementation.
 *
 * Responsibilities:
 * - Manage embedded gRPC server lifecycle
 * - Send single messages
 * - Broadcast messages with concurrency control
 * - Manage bidirectional streams
 * - Handle retry persistence for failed messages
 *
 * This class is designed to be:
 * - Coroutine-safe
 * - Failure-isolated (uses supervisorScope)
 * - Timeout-protected (5s per RPC)
 *
 *Created On: 24-02-2026 |
 *Author: Hiren Patel
 */
internal class GrpcSdkImpl(
    private val serviceScope: CoroutineScope
) : GrpcSdk {

    /**
     * Controls embedded gRPC server lifecycle.
     * Lazy to avoid unnecessary allocation until first use.
     */
    private val serverController by lazy {
        ServerController(serviceScope)
    }

    // For Bidirectional streaming --------------

    /**
     * Reusable channel pool.
     * Prevents channel recreation overhead.
     */
    private val channelPool by lazy {
        ChannelPool()
    }

    /**
     * Handles bidirectional streaming sessions.
     */
    private val streamManager by lazy {
        StreamManager(serviceScope, channelPool)
    }

    // ----------------------------------------------------

    private var responseProvider: ChatResponseProvider? = null
    private var events: MutableSharedFlow<GrpcEvent>? = null

    /**
     * Starts embedded gRPC server.
     *
     * @param startPort Port to bind server
     * @param provider Optional response provider
     * @param events Optional event flow emitter
     */
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

    /**
     * Stops embedded server.
     */
    override fun stopServer() {
        eventLog("GrpcSdkImpl", "stopServer called")
        serverController.stop(events)
    }

    /**
     * Sends a single message (fire & persist-on-failure).
     *
     * If:
     * - RPC throws exception
     * - OR response.received == false
     *
     * → message is stored for retry.
     */
    override fun sendMessage(
        ip: String,
        port: Int,
        message: Message,
        retryIfFail: Boolean,
        messageRepository: MessageRepository,
    ) {
        eventLog(
            "GrpcSdkImpl",
            "sendMessage to $ip:$port with message: $message"
        )
        serviceScope.launch {
            val result = executeGrpcCall {
                val channel = channelPool.get(ip, port)
                val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)
                stub.getChat(message.toGrpcRequest())
            }

            if (!retryIfFail) return@launch

            result.fold(
                onSuccess = { response ->
                    if (!response.received) {
                        handleFailure(
                            message = message,
                            reason = response.info.ifBlank { "Message not acknowledged" },
                            messageType = MessageType.SINGLE,
                            messageRepository = messageRepository
                        )
                    }
                },
                onFailure = { throwable ->
                    handleFailure(
                        message = message,
                        reason = throwable.message ?: "Unknown Exception",
                        messageType = MessageType.SINGLE,
                        messageRepository = messageRepository
                    )
                }
            )
        }
    }

    /**
     * Sends message and returns result via callback.
     *
     * Unlike sendMessage(), this reports result immediately
     * but still persists message if delivery fails.
     */
    override fun sendMessageWithCallback(
        ip: String,
        port: Int,
        message: Message,
        callback: (GrpcResult) -> Unit,
        retryIfFail: Boolean,
        messageRepository: MessageRepository,
    ) {
        eventLog(
            "GrpcSdkImpl",
            "sendMessageWithCallback to $ip:$port with message: $message"
        )
        serviceScope.launch {

            executeGrpcCall {
                val channel = channelPool.get(ip, port)
                val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)
                stub.getChat(message.toGrpcRequest())
            }
                .onSuccess { response ->
                    callback(GrpcResult.Success(ip = ip, data = response.toString()))

                    if (!response.received && retryIfFail) {
                        handleFailure(
                            message = message,
                            reason = response.info.ifBlank { "Message not acknowledged" },
                            messageType = MessageType.SINGLE,
                            messageRepository = messageRepository
                        )
                    }
                }
                .onFailure { throwable ->
                    callback(GrpcResult.Error(ip = ip, throwable = throwable.toGrpcSafeException()))
                    if (retryIfFail)
                        handleFailure(
                            message = message,
                            reason = throwable.message ?: "Unknown Exception",
                            messageType = MessageType.SINGLE,
                            messageRepository = messageRepository
                        )
                }
        }
    }

    /**
     * Broadcast message to multiple devices.
     *
     * Features:
     * - Optional target filtering
     * - Concurrency limit via Semaphore
     * - Failure isolation via supervisorScope
     * - Timeout protection per RPC
     *
     * Does NOT stop entire broadcast if one device fails.
     */
    override fun sendBroadcast(
        deviceRepository: DeviceRepository,
        targets: List<DeviceIpPort>?,
        deviceId: String,
        message: Message,
        maxConcurrency: Int,
        deviceFilter: BroadCastDevice,
        messageRepository: MessageRepository,
    ) {
        eventLog(
            "GrpcSdkImpl",
            "broadcastFireAndForget to targets: $targets with message: $message and maxConcurrency: $maxConcurrency"
        )

        val semaphore = Semaphore(maxConcurrency)

        serviceScope.launch {

            // Resolve target device list
            val devices = targets ?: deviceRepository.getDevicesForBroadcast(deviceId, deviceFilter)

            supervisorScope {

                // Launch async jobs for parallel execution
                val jobs = devices.map { (ip, port) ->
                    async {
                        semaphore.withPermit {

                            executeGrpcCall {
                                val channel = channelPool.get(ip, port)
                                val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)
                                stub.getChat(message.toGrpcRequest())
                            }
                                .onSuccess { response ->
                                    if (!response.received) {
                                        handleFailure(
                                            message = message,
                                            reason = response.info.ifBlank { "Message not acknowledged" },
                                            messageType = MessageType.BROADCAST,
                                            messageRepository = messageRepository
                                        )
                                    }
                                }
                                .onFailure { throwable ->
                                    eventLog(
                                        "GrpcBroadcast",
                                        "Failed to send to $ip:$port"
                                    )
                                    handleFailure(
                                        message = message,
                                        reason = throwable.message ?: "Unknown Exception",
                                        messageType = MessageType.BROADCAST,
                                        messageRepository = messageRepository
                                    )
                                }
                        }
                    }
                }

                // Wait for all broadcast jobs to complete
                jobs.awaitAll()
            }
        }
    }

    /**
     * Opens bidirectional streaming session.
     */
    override fun openStream(ip: String, port: Int): ChatStreamSession {
        return streamManager.open(ip, port)
    }

    /**
     * Gracefully shutdown SDK:
     * - Stop server
     * - Shutdown channels
     * - Cancel coroutine scope
     */
    override fun shutdown() {
        eventLog(
            "GrpcSdkImpl",
            "shutdown called"
        )
        stopServer()
        channelPool.shutdownAll()
        serviceScope.cancel()
    }

    /**
     * Restart server safely:
     * - Shutdown all channels
     * - Restart server instance
     */
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
            )
        )
    }

    /**
     * Returns true if embedded server is running.
     */
    override fun serverIsRunning(): Boolean {
        return serverController.isRunning()
    }

    /**
     * Persists failed message for retry mechanism.
     *
     * This method is suspended to allow database IO.
     */
    private suspend fun handleFailure(
        messageRepository: MessageRepository,
        message: Message,
        reason: String,
        messageType: MessageType
    ) {
        messageRepository.upsertMessage(
            message.toMessageEntity(reason = reason, messageType = messageType)
        )
    }

    /**
     * Executes gRPC call with:
     * - 5 second timeout
     * - runCatching wrapper
     *
     * Ensures:
     * - No hanging RPC calls
     * - No exception leaks
     */
    private suspend fun <T> executeGrpcCall(
        block: suspend () -> T
    ): Result<T> {
        return runCatching {
            withTimeout(5_000) {
                block()
            }
        }
    }
}