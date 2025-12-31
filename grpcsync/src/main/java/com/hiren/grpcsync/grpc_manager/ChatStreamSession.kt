package com.hiren.grpcsync.grpc_manager

import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse
import kotlinx.coroutines.flow.Flow

class ChatStreamSession(
    val send: suspend (ChatRequest) -> Unit,
    val responses: Flow<ChatResponse>,
    val close: () -> Unit
)