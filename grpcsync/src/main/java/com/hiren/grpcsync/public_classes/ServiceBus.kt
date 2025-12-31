package com.hiren.grpcsync.public_classes

import com.hiren.grpcsync.db.MessageEntity
import com.hiren.grpcsync.grpc_manager.GrpcResult
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
    object StartDiscovery : ServiceEvent()

    object StopDiscovery : ServiceEvent()

    object Stop : ServiceEvent()

    data class Send(val ip: String, val payload: MessageEntity) : ServiceEvent()

    data class SendWithCallback(val ip: String, val payload: MessageEntity, val callback: (GrpcResult) -> Unit) : ServiceEvent()

    data class Broadcast(val devices: List<String>, val payload: MessageEntity) : ServiceEvent()

    data class StartStream(val ip: String) : ServiceEvent()

    data class ChangeUdpPort(val port: Int) : ServiceEvent()

    data class ChangeGrpcPort(val port: Int) : ServiceEvent()
}