package com.hiren.grpcsync.grpc_manager


sealed class GrpcResult1 {
    data class Success<T>(
        val ip: String,
        val response: T
    ) : GrpcResult1()

    data class Error(
        val ip: String,
        val throwable: Throwable
    ) : GrpcResult1()
}