package com.hiren.grpcsync.grpc

import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.db.MessageResponse

/**
 * Allows the host application to decide how to respond
 * to incoming chat messages.
 *
 * Implemented by the SDK consumer.
 *
 *  Created On    : 24-02-2026 |
 *  Author        : Hiren Patel
 */
interface ChatResponseProvider {

    /**
     * Called when a chat message is received.
     *
     * @param request Incoming chat request
     * @return ChatResponse to be sent back to the client
     */
    suspend fun onMessageReceived(request: Message): MessageResponse
}