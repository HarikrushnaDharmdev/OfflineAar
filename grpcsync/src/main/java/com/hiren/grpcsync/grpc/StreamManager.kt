package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatResponse
import com.hiren.grpcsync.ChatServiceGrpc
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

class StreamManager(
    private val scope: CoroutineScope,
    private val channelPool: ChannelPool
) {

    private val activeStreams = ConcurrentHashMap<String, Job>()

    fun open(
        ip: String,
        port: Int,
    ): ChatStreamSession {


        val channel = channelPool.get(ip, port)
        val stub = ChatServiceGrpc.newStub(channel)


        val responses = MutableSharedFlow<GrpcResult>(
            extraBufferCapacity = 64
        )


        val observer = stub.streamChat(
            object : StreamObserver<ChatResponse> {
                override fun onNext(value: ChatResponse) {
                    responses.tryEmit(
                        GrpcResult.Success(ip, value.received.toString())
                    )
                }


                override fun onError(t: Throwable) {
                    responses.tryEmit(GrpcResult.Error(ip, t))
                }


                override fun onCompleted() {}
            }
        )


        return ChatStreamSession(
            send = { observer.onNext(it) },
            responses = responses.asSharedFlow(),
            close = { observer.onCompleted() }
        )
    }
}