package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse
import com.hiren.grpcsync.ChatServiceGrpc
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.utils.Constants.RESPONSE_TIMEOUT_MS
import com.hiren.grpcsync.utils.Utils.eventLog
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * gRPC server-side implementation of [ChatServiceGrpc.ChatServiceImplBase].
 *
 * This service handles both:
 * 1. Unary chat messages (`getChat`)
 * 2. Bidirectional streaming chat messages (`streamChat`)
 *
 * Incoming messages are converted into [GrpcEvent]s and emitted through
 * a [MutableSharedFlow], allowing the rest of the application (UI, repository,
 * foreground service, etc.) to react in a decoupled and asynchronous manner.
 *
 * This class is intended to run inside a long-lived component such as a
 * foreground service.
 *
 *  Created On    : 24-02-2026 |
 *  Author        : Hiren Patel
 */
internal class ChatService(
    /**
     * SharedFlow used to publish incoming gRPC events to the application layer.
     *
     * The flow should be configured with an appropriate buffer size to avoid
     * message drops under high throughput.
     */
    private val events: MutableSharedFlow<GrpcEvent>? = null,
    private val responseProvider: ChatResponseProvider? = null,
    private val scope: CoroutineScope
) : ChatServiceGrpc.ChatServiceImplBase() {

    /**
     * Handles a unary chat request.
     *
     * The client sends a single [ChatRequest] and expects a single
     * [ChatResponse] as acknowledgment.
     *
     * Flow:
     * - Emit a [GrpcEvent.MessageReceived] event
     * - Log the received message (for debugging)
     * - Respond with an ACK
     * - Complete the RPC
     *
     * @param request Incoming chat message
     * @param responseObserver Observer used to send the response back to the client
     */
    override fun getChat(
        request: ChatRequest,
        responseObserver: StreamObserver<ChatResponse>
    ) {

        events?.tryEmit(
            GrpcEvent.MessageReceived(
                from = request.senderId,
                message = getMessageFromGrpcRequest(request)
            )
        )

        eventLog(
            "ChatService",
            "Received message from ${request.messageId}: ${request.content}"
        )

        if (responseProvider != null) {
            eventLog(
                "ChatService",
                "waiting response for message from ${request.messageId}"
            )

            scope.launch {
                try {
                    val response = withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
                        responseProvider.onMessageReceived(request)
                    }

                    if (response != null) {
                        // Response arrived within timeout
                        eventLog(
                            "ChatService",
                            "sending response to ${request.messageId}"
                        )

                        responseObserver.onNext(response.toGrpcRequest())
                    } else {
                        // Timeout occurred
                        eventLog(
                            "ChatService",
                            "response timeout for ${request.messageId}"
                        )

                        responseObserver.onNext(
                            ChatResponse.newBuilder()
                                .setReceived(false)
                                .setInfo("Response timeout for ${request.messageId}")
                                .build()
                        )
                    }
                } catch (e: Exception) {
                    // Defensive: provider threw unexpectedly
                    responseObserver.onNext(
                        ChatResponse.newBuilder()
                            .setReceived(false)
                            .setInfo(e.message ?: "Internal error ${request.messageId}")
                            .build()
                    )
                    eventLog(
                        "ChatService",
                        "e.message ?: Internal error for ${request.messageId}"
                    )
                } finally {
                    // gRPC contract: MUST complete
                    responseObserver.onCompleted()
                }
            }
        } else {
            responseObserver.onNext(
                ChatResponse.newBuilder()
                    .setReceived(false)
                    .build()
            )
            responseObserver.onCompleted()
        }
    }

    /**
     * Handles bidirectional streaming chat messages.
     *
     * This method establishes a persistent stream where:
     * - The client can continuously send [ChatRequest] messages
     * - The server responds with a [ChatResponse] ACK for each message
     *
     * Use cases:
     * - Real-time chat
     * - Device-to-device streaming communication
     * - Continuous message synchronization
     *
     * @param responseObserver Observer used to push responses back to the client
     * @return [StreamObserver] that receives incoming [ChatRequest] messages
     */
    override fun streamChat(
        responseObserver: StreamObserver<ChatResponse>
    ): StreamObserver<ChatRequest> {

        return object : StreamObserver<ChatRequest> {

            /**
             * Called for each incoming message in the stream.
             *
             * Emits the message as a [GrpcEvent] and sends an ACK
             * back to the client.
             */
            override fun onNext(request: ChatRequest) {
                eventLog(
                    "ChatService",
                    "streamChat onNext from ${request.messageId}: ${request.content}"
                )

                events?.tryEmit(
                    GrpcEvent.MessageReceived(
                        from = request.senderId,
                        message = getMessageFromGrpcRequest(request)
                    )
                )

                // ACK back
                responseObserver.onNext(
                    ChatResponse.newBuilder()
                        .setReceived(true)
                        .build()
                )
            }

            /**
             * Called when the stream encounters an error.
             *
             * Typical reasons:
             * - Network interruption
             * - Client crash
             * - Channel shutdown
             *
             * Consider logging and emitting a connection-related event here.
             */
            override fun onError(t: Throwable) {
                t.printStackTrace()
                eventLog(
                    "ChatService",
                    "streamChat onError"
                )
                // stream broken
            }


            /**
             * Called when the client completes the stream.
             *
             * The server responds by completing its side of the stream.
             */
            override fun onCompleted() {
                eventLog(
                    "ChatService",
                    "streamChat onCompleted"
                )
                responseObserver.onCompleted()
            }
        }
    }

    fun getMessageFromGrpcRequest(request: ChatRequest): Message {
        return Message(
            messageId = request.messageId,
            senderId = request.senderId,
            receiverId = request.receiverId,
            content = request.content,
            timestamp = request.timestamp,
            status = request.status,
            type = request.type
        )
    }
}