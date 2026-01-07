package com.hiren.grpcsync.public_classes

import android.content.Context
import android.content.Intent
import android.os.Build
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.service.OfflineSyncService
import com.hiren.grpcsync.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineCommImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : OfflineComm {

    /*init {
        DevicePublic.init(context)
    }*/

    /*companion object {
        @Volatile
        private var instance: OfflineCommImpl? = null

        fun getInstance(context: Context): OfflineCommImpl =
            instance ?: synchronized(this) {
                instance ?: OfflineCommImpl().also {
                    instance = it
                    DevicePublic.init(context)
                }
            }
    }*/

    override fun startService(
        context: Context,
        udpPort: Int,
        grpcPort: Int,
        broadcastIntervalMs: Long,
        deviceTimeoutMs: Long,
        deleteDeviceOnTimeout: Boolean,
        printLog: Boolean
    ) {
        val intent = Intent(context, OfflineSyncService::class.java)
            .apply {
                putExtra(Constants.EXTRA_UDP_PORT, udpPort)
                putExtra(Constants.EXTRA_GRPC_PORT, grpcPort)
                putExtra(Constants.EXTRA_BROADCAST_INTERVAL, broadcastIntervalMs)
                putExtra(Constants.EXTRA_DEVICE_TIMEOUT, deviceTimeoutMs)
                putExtra(Constants.EXTRA_DELETE_ON_TIMEOUT, deleteDeviceOnTimeout)
                putExtra(Constants.EXTRA_PRINT_LOG, printLog)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopService(context: Context) {
        context.stopService(Intent(context, OfflineSyncService::class.java))
    }

    override fun startDiscovery(
        provider: ChatResponseProvider?,
        events: MutableSharedFlow<GrpcEvent>?
    ) {
        ServiceBus.post(
            ServiceEvent.StartDiscovery(
                provider = provider,
                events = events
            )
        )
    }

    override fun stopDiscovery(
    ) {
        ServiceBus.post(ServiceEvent.StopDiscovery)
    }

    override fun sendMessage(ip: String, port: Int, payload: Message) {
        ServiceBus.post(ServiceEvent.Send(ip = ip, port = port, payload = payload))
    }

    override fun sendMessageWithCallback(
        ip: String,
        port: Int,
        payload: Message,
        callback: (GrpcResult) -> Unit
    ) {
        ServiceBus.post(
            ServiceEvent.SendWithCallback(
                ip = ip,
                payload = payload,
                port = port,
                callback = callback
            )
        )
    }

    override fun broadcast(devices: List<String>, payload: Message) {
        ServiceBus.post(
            ServiceEvent.Broadcast(devices, payload)
        )
    }

    override fun getDevices(): Flow<List<DeviceEntity>> {
        return DevicePublic.deviceRepository.observeDevices()
    }

    override fun changeUdpPort(port: Int) {
        ServiceBus.post(ServiceEvent.ChangeUdpPort(port))
    }

    override fun changeGrpcPort(port: Int) {
        ServiceBus.post(ServiceEvent.ChangeGrpcPort(port))
    }
}