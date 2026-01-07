package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.ChatResponse

/**
 * Allows the host application to decide how to respond
 * to incoming chat messages.
 *
 * Implemented by the SDK consumer.
 */
interface ChatResponseProvider {

    /**
     * Called when a chat message is received.
     *
     * @param request Incoming chat request
     * @return ChatResponse to be sent back to the client
     */
    suspend fun onMessageReceived(request: ChatRequest): ChatResponse
}