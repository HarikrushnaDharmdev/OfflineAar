package com.hiren.grpcsync.grpc

sealed class GrpcResult {

    data class Success(
        val ip: String,
        val data: String
    ) : GrpcResult()


    data class Error(
        val ip: String,
        val throwable: Throwable
    ) : GrpcResult()


    data class Timeout(
        val ip: String
    ) : GrpcResult()
}