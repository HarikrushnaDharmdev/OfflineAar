package com.hiren.grpcsync.grpc_manager

import com.hiren.grpcsync.ChatResponse

sealed class GrpcResult {
    data class Success(
        val ip: String,
        val response: ChatResponse
    ) : GrpcResult()

    data class Error(
        val ip: String,
        val throwable: Throwable
    ) : GrpcResult()
}