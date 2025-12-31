package com.hiren.grpcsync.grpc_manager

sealed class GrpcEvent {
    data class ServerStarted(val port: Int) : GrpcEvent()
    data class ServerStopped(val reason: String) : GrpcEvent()

    data class MessageReceived(
        val from: String,
        val content: String
    ) : GrpcEvent()

    data class MessageSent(
        val to: String,
        val success: Boolean
    ) : GrpcEvent()

    data class Error(
        val target: String?,
        val throwable: Throwable
    ) : GrpcEvent()
}
