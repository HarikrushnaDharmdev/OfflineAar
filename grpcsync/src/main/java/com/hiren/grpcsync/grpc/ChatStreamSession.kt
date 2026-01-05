package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatRequest
import kotlinx.coroutines.flow.Flow

data class ChatStreamSession(
    val send: (ChatRequest) -> Unit,
    val responses: Flow<GrpcResult>,
    val close: () -> Unit
)