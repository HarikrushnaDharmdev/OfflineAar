package com.hiren.grpcsync.grpc

import android.util.Log
import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse
import com.hiren.grpcsync.ChatServiceGrpc
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
 */
class ChatService(
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
                content = request.content
            )
        )

        Log.e("RECEIVED_MESSAGE_FROM_SERVER", "getChat: ${request.content}")

        if (responseProvider != null) {
            scope.launch {
                val response = responseProvider.onMessageReceived(request)
                responseObserver.onNext(response.toGrpcRequest())
                responseObserver.onCompleted()
            }
            // If scop not work use runBlocking here
            /* val response = runBlocking {
                 responseProvider.onMessageReceived(request)
             }
             responseObserver.onNext(response)
             responseObserver.onCompleted()*/
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
                events?.tryEmit(
                    GrpcEvent.MessageReceived(
                        from = request.senderId,
                        content = request.content
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
                // stream broken
            }


            /**
             * Called when the client completes the stream.
             *
             * The server responds by completing its side of the stream.
             */
            override fun onCompleted() {
                responseObserver.onCompleted()
            }
        }
    }
}