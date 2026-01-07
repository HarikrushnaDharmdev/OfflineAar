package com.hiren.grpcsync.public_classes

import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal object ServiceBus {

    private val _events = MutableSharedFlow<ServiceEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val events: SharedFlow<ServiceEvent> = _events

    fun post(event: ServiceEvent) {
        _events.tryEmit(event)
    }
}

sealed class ServiceEvent {

    data class StartDiscovery(
        val provider: ChatResponseProvider?,
        val events: MutableSharedFlow<GrpcEvent>?
    ) : ServiceEvent()

    object StopDiscovery : ServiceEvent()

    object Stop : ServiceEvent()

    data class Send(val ip: String, val port: Int, val payload: Message) : ServiceEvent()

    data class SendWithCallback(
        val ip: String,
        val port: Int,
        val payload: Message,
        val callback: (GrpcResult) -> Unit
    ) : ServiceEvent()

    data class Broadcast(val devices: List<String>, val payload: Message) : ServiceEvent()

    data class StartStream(val ip: String, val port: Int) : ServiceEvent()

    data class ChangeUdpPort(val port: Int) : ServiceEvent()

    data class ChangeGrpcPort(val port: Int) : ServiceEvent()
}