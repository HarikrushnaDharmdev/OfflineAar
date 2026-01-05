package com.hiren.grpcsync.grpc

import io.grpc.BindableService
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ServerController(
    private val scope: CoroutineScope
) {

    private var server: Server? = null
    private val maxRetry = 5


    fun start(startPort: Int, service: BindableService) {
        scope.launch {
            var port = startPort
            repeat(maxRetry) {
                try {
                    server = Grpc
                        .newServerBuilderForPort(
                            port,
                            InsecureServerCredentials.create()
                        )
                        .addService(service)
                        .build()
                        .start()
                    return@launch
                } catch (e: Exception) {
                    port++
                }
            }
        }
    }


    fun stop() {
        server?.shutdown()
        server = null
    }
}