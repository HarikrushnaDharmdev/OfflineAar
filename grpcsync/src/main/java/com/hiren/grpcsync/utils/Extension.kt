package com.hiren.grpcsync.utils

import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.TimeoutCancellationException

internal fun Throwable.toGrpcSafeException(): Exception {
    return when (this) {
        is StatusRuntimeException -> {
            Exception(
                status.description ?: status.code.name
            )
        }

        is StatusException -> {
            Exception(
                status.description ?: status.code.name
            )
        }

        is TimeoutCancellationException -> {
            Exception("Request Timeout")
        }

        else -> Exception(message ?: "Unknown Exception")
    }
}