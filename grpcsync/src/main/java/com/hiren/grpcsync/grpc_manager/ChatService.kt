package com.hiren.grpcsync.grpc_manager

import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse
import com.hiren.grpcsync.ChatServiceGrpc
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.flow.MutableSharedFlow

class ChatService(
    private val events: MutableSharedFlow<GrpcEvent>
) : ChatServiceGrpc.ChatServiceImplBase() {

    override fun getChat(
        request: ChatRequest,
        responseObserver: StreamObserver<ChatResponse>
    ) {

        events.tryEmit(
            GrpcEvent.MessageReceived(
                from = request.senderId,
                content = request.content
            )
        )

        responseObserver.onNext(
            ChatResponse.newBuilder()
                .setReceived(true)
                .build()
        )
        responseObserver.onCompleted()
    }

    override fun streamChat(
        responseObserver: StreamObserver<ChatResponse>
    ): StreamObserver<ChatRequest> {

        return object : StreamObserver<ChatRequest> {

            override fun onNext(request: ChatRequest) {
                events.tryEmit(
                    GrpcEvent.MessageReceived(request.senderId, request.content)
                )

                // ACK back
                responseObserver.onNext(
                    ChatResponse.newBuilder()
                        .setReceived(true)
                        .build()
                )
            }

            override fun onError(t: Throwable) {
                // stream broken
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
            }
        }
    }
}
