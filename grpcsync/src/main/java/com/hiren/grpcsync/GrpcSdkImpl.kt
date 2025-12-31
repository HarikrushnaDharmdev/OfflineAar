package com.hiren.grpcsync

import android.content.Context
import android.content.Intent
import android.os.Build
import com.hiren.grpcsync.db.AppDatabase
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.MessageEntity
import com.hiren.grpcsync.grpc_manager.ChatStreamSession
import com.hiren.grpcsync.grpc_manager.GrpcManager
import com.hiren.grpcsync.grpc_manager.GrpcResult
import com.hiren.grpcsync.repo.DeviceRepositoryImpl
import com.hiren.grpcsync.service.OfflineSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class GrpcSdkImpl : GrpcSdk {

    private var grpcManager: GrpcManager? = null
    private lateinit var repository: DeviceRepositoryImpl

    /* ---------------- LIFECYCLE ---------------- */

    override fun start(context: Context) {
        repository = DeviceRepositoryImpl(AppDatabase.getDatabase(context).deviceDao())

        val intent = Intent(context, OfflineSyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        grpcManager = GrpcManager()
        grpcManager?.startServer()
    }

    override fun stop(context: Context) {
        context.stopService(
            Intent(context, OfflineSyncService::class.java)
        )
        grpcManager?.stopServer()
        grpcManager = null
    }

    /* ---------------- FIRE & FORGET ---------------- */

    override fun sendMessage(
        ip: String,
        payload: MessageEntity
    ) {
        checkGrpcManagerStatus()
        grpcManager?.sendMessage(payload, ip)
    }

    /* ---------------- CALLBACK FLOW ---------------- */

    override fun sendMessageWithCallback(
        ip: String,
        payload: MessageEntity
    ): Flow<GrpcResult> {
        checkGrpcManagerStatus()
        //return grpcManager!!.sendMessageWithCallback(payload, ip)
        return flow {  }
    }

    /* ---------------- BROADCAST ---------------- */

    override fun broadcast(
        devices: List<String>,
        payload: MessageEntity
    ) {
        checkGrpcManagerStatus()
        grpcManager!!.broadcast(message = payload, devices = devices)
    }

    override fun openChatStream(ip: String): ChatStreamSession {
        checkGrpcManagerStatus()
        return grpcManager!!.openChatStream(ip = ip)
    }

    override fun getDevices(): Flow<List<DeviceEntity>> {
        //checkGrpcManagerStatus()
        /*if (!this::repository.isInitialized) {
            repository = DeviceRepositoryImpl(AppDatabase.getDatabase(context).deviceDao())
        }*/
        return repository.observeDevices()
    }

    fun checkGrpcManagerStatus() {
        if (grpcManager == null)
            throw (IllegalStateException("GrpcSdk not started. Call start(context) before sending messages."))
    }
}
