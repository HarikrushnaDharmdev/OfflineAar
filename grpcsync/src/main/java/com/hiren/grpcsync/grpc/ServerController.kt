package com.hiren.grpcsync.grpc

import io.grpc.BindableService
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Lightweight controller responsible for starting and stopping the embedded gRPC server.
 *
 * The server is created on demand the first time [start] is called and will keep
 * running until [stop] is invoked. All server startup work is done on the provided
 * [CoroutineScope] to avoid blocking the caller thread.
 */

internal class ServerController(
    private val scope: CoroutineScope
) {

    /**
     * Backing reference to the currently running gRPC [Server], if any.
     *
     * When `null`, the server has not been started or has already been stopped.
     */
    private var server: Server? = null

    /**
     * Mutex-like guard to ensure start/stop/restart operations
     * do not overlap and corrupt server state.
     */
    private val lock = Any()

    /**
     * Starts the gRPC server on the given [port] with the provided [service].
     *
     * - If a server instance is already running, this call is a no-op.
     * - The server is started asynchronously on [scope] to keep the API non-blocking.
     */
    fun start(port: Int, service: BindableService, events: MutableSharedFlow<GrpcEvent>?) {
        scope.launch {
            synchronized(lock) {
                // Avoid starting a new server if one is already running.
                if (server != null) return@launch

                try {
                    server = Grpc
                        .newServerBuilderForPort(
                            port,
                            InsecureServerCredentials.create()
                        )
                        .addService(service)
                        .build()
                        .start()

                    events?.tryEmit(GrpcEvent.ServerStarted(port = port))
                } catch (e: Exception) {
                    events?.tryEmit(
                        GrpcEvent.Error(
                            target = "ServerController.start",
                            throwable = e
                        )
                    )
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Stops the currently running gRPC server (if any) and clears the reference.
     *
     * Safe to call multiple times; subsequent calls after the first will be no-ops.
     */
    fun stop(events: MutableSharedFlow<GrpcEvent>?) {
        synchronized(lock) {
            try {
                server?.shutdown()
                server = null
                events?.tryEmit(GrpcEvent.ServerStopped(reason = "Manual stop invoked"))
            } catch (e: Exception) {
                events?.tryEmit(GrpcEvent.Error(target = "ServerController.stop", throwable = e))
                e.printStackTrace()
            }
        }
    }

    /**
     * Restarts the gRPC server on a new port.
     *
     * This method:
     * - Gracefully shuts down the existing server (if running)
     * - Starts a new server on the provided [newPort]
     * - Emits lifecycle events for observability
     *
     * This operation is asynchronous and safe to call multiple times.
     *
     * @param newPort New port on which the server should listen
     * @param service gRPC service implementation
     * @param events Optional shared flow for emitting server events
     */
    fun restart(
        newPort: Int,
        service: BindableService,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        scope.launch {
            synchronized(lock) {
                try {
                    // Stop existing server (if any)
                    server?.shutdown()
                    server = null

                    events?.tryEmit(
                        GrpcEvent.ServerStopped(
                            reason = "Restart requested"
                        )
                    )

                    // Start server on new port
                    server = Grpc
                        .newServerBuilderForPort(
                            newPort,
                            InsecureServerCredentials.create()
                        )
                        .addService(service)
                        .build()
                        .start()

                    events?.tryEmit(
                        GrpcEvent.ServerStarted(port = newPort)
                    )
                } catch (e: Exception) {
                    events?.tryEmit(
                        GrpcEvent.Error(
                            target = "ServerController.restart",
                            throwable = e
                        )
                    )
                }
            }
        }
    }

    fun isRunning(): Boolean {
        if (server != null) {
            return server?.isTerminated ?: false
        }
        return false
    }
}