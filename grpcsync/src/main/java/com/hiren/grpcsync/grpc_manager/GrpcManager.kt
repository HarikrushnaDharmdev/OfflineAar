package com.hiren.grpcsync.grpc_manager

import android.util.Log
import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse
import com.hiren.grpcsync.ChatServiceGrpc
import com.hiren.grpcsync.ChatServiceGrpcKt
import com.hiren.grpcsync.db.MessageEntity
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class GrpcManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<GrpcEvent>(
        replay = 0,
        extraBufferCapacity = 128
    )
    val events = _events.asSharedFlow()

    private var server: Server? = null
    private val port = 50051

    /* ---------------- SERVER ---------------- */

    private var serverErrorRetryCount = 0
    private var maxServerErrorRetryCount = 3

    fun startServer() {
        if (server != null) return

        try {
            server = Grpc
                .newServerBuilderForPort(
                    port,
                    InsecureServerCredentials.create()
                )
                .addService(ChatService(_events))
                .build()
                .start()

            _events.tryEmit(GrpcEvent.ServerStarted(port))

        } catch (e: Exception) {
            _events.tryEmit(GrpcEvent.Error("SERVER", e))
            autoRestartServer()
        }
    }

    fun stopServer() {
        serverErrorRetryCount = 0
        server?.shutdown()
        server = null
        _events.tryEmit(GrpcEvent.ServerStopped("Manual stop"))
    }

    private fun autoRestartServer() {
        scope.launch {
            delay(3_000)
            if (serverErrorRetryCount >= maxServerErrorRetryCount) {
                serverErrorRetryCount = 0
                return@launch
            } else {
                serverErrorRetryCount++
            }
            startServer()
        }
    }

    /* ---------------- CLIENT ---------------- */

    suspend fun sendMessageWithCallback(
        message: MessageEntity,
        ip: String,
        port: Int = 50051,
        callback: (GrpcResult) -> Unit
    ){
        val channel = ManagedChannelBuilder
            .forAddress(ip, port)
            .usePlaintext()
            .build()

        try {
            val stub =
                ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)

            val response = stub.getChat(
                ChatRequest.newBuilder()
                    .setContent(message.content)
                    .build()
            )

            callback.invoke(GrpcResult.Success(ip, response))

        } catch (e: Exception) {
            callback.invoke(GrpcResult.Error(ip, e))
        } finally {
            channel.shutdown()
        }
    }

    fun sendMessage(
        message: MessageEntity,
        ip: String,
        port: Int = 50051
    ) {
        scope.launch {
            try {
                log("Sending gRPC message to $ip")
                val channel = ChannelPool.get(ip, port)

                val stub = ChatServiceGrpcKt
                    .ChatServiceCoroutineStub(channel)
                    .withDeadlineAfter(2, TimeUnit.MINUTES)

                val request = stub.getChat(
                    ChatRequest.newBuilder()
                        .setMessageId(message.messageId)
                        .setChannelId(message.channelId)
                        .setSenderId(message.senderId)
                        .setReceiverId(message.receiverId)
                        .setContent(message.content)
                        .setTimestamp(message.timestamp)
                        .setStatus(message.status)
                        .build()
                )
                val response = request.received
                log("gRPC message sent to $ip, response: $response")
                _events.tryEmit(GrpcEvent.MessageSent(ip, response))
            } catch (e: Exception) {
                log("gRPC message sent to $ip, response: ${e.message}")
                _events.tryEmit(GrpcEvent.Error(ip, e))
                //autoReconnect(ip, message)
            }
        }
    }

    /*private fun autoReconnect(ip: String, message: MessageEntity) {
        scope.launch {
            delay(2_000)
            sendMessage(message, ip)
        }
    }*/

    fun broadcast(
        message: MessageEntity,
        devices: List<String>
    ) {
        devices.chunked(10).forEach { batch ->
            batch.forEach { ip ->
                sendMessage(message, ip)
            }
        }
    }

    fun openChatStream(
        ip: String
    ): ChatStreamSession {

        val channel = ManagedChannelBuilder
            .forAddress(ip, port)
            .usePlaintext()
            .build()

        val stub = ChatServiceGrpc.newStub(channel)

        val responseFlow = MutableSharedFlow<ChatResponse>(
            extraBufferCapacity = 64
        )

        val requestObserver =
            stub.streamChat(object : StreamObserver<ChatResponse> {

                override fun onNext(value: ChatResponse) {
                    responseFlow.tryEmit(value)
                }

                override fun onError(t: Throwable) {
                    responseFlow.tryEmit(
                        ChatResponse.newBuilder()
                            .setReceived(false)
                            .build()
                    )
                    channel.shutdown()
                }

                override fun onCompleted() {}
            })

        return ChatStreamSession(
            send = { req -> requestObserver.onNext(req) },
            responses = responseFlow.asSharedFlow(),
            close = {
                requestObserver.onCompleted()
                channel.shutdown()
            }
        )
    }

    private fun log(msg: String) {
        if (true) {
            Log.d("UdpBroadcastService", msg)
        }
    }
}
