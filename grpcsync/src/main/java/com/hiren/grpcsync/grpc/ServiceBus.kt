package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.db.DeviceIpPort
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.utils.BroadCastDevice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal object ServiceBus {

    private val _events = MutableSharedFlow<ServiceEvent>(
        replay = 0,
        extraBufferCapacity = 2
    )

    val events: SharedFlow<ServiceEvent> = _events

    fun post(event: ServiceEvent) {
        _events.tryEmit(event)
    }
}

internal sealed class ServiceEvent {

    data class StartDiscovery(
        val grpcPort: Int,
        val provider: ChatResponseProvider?,
        val events: MutableSharedFlow<GrpcEvent>?
    ) : ServiceEvent()

    object StopDiscovery : ServiceEvent()

    object Stop : ServiceEvent()

    data class Send(val ip: String, val port: Int, val payload: Message, val retryIfFail: Boolean) :
        ServiceEvent()

    data class SendWithCallback(
        val ip: String,
        val port: Int,
        val payload: Message,
        val callback: (GrpcResult) -> Unit,
        val retryIfFail: Boolean
    ) : ServiceEvent()

    data class SendBroadcast(
        val targets: List<DeviceIpPort>?,
        val message: Message,
        val maxConcurrency: Int,
        val deviceFilter: BroadCastDevice
    ) : ServiceEvent()

    data class StartStream(val ip: String, val port: Int) : ServiceEvent()

    data class ChangeGrpcPort(val port: Int) : ServiceEvent()
}