package com.hiren.offlineaar

import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.db.MessageResponse
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.public_classes.OfflineCommImpl
import com.hiren.grpcsync.repo.DeviceRepository
import com.hiren.grpcsync.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val offlineSdk: OfflineCommImpl,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    val currentIP = Utils.getDeviceIpAddress()

    // SharedFlow to receive gRPC events from the SDK (server started, messages, errors, etc.)
    private val grpcEvents = MutableSharedFlow<GrpcEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    init {
        viewModelScope.launch {
            grpcEvents.collect { event ->
                when (event) {
                    is GrpcEvent.ServerStarted -> {
                        Log.d("GRPC ->>>>>>", "Server started on ${event.port}")
                        addMessage("Server started on ${event.port}")
                    }

                    is GrpcEvent.ServerStopped -> {
                        Log.d("GRPC ->>>>>>", "Server Stopped")
                        addMessage("Server Stopped")
                    }

                    is GrpcEvent.MessageReceived -> {
                        Log.d("GRPC ->>>>>>", "From ${event.from}: ${event.message}")
                        addMessage("From ${event.from}: ${event.message}")
                        addToMessageList(event.message)
                    }

                    is GrpcEvent.MessageSent -> {
                        Log.d("GRPC ->>>>>>", "Sent to ${event.to}")
                        addMessage("Sent to ${event.to}")
                    }

                    is GrpcEvent.Error -> {
                        Log.e("GRPC ->>>>>>", "Error ${event.target}", event.throwable)
                        addMessage("Error ${event.target}, ${event.throwable}")
                    }
                }
            }
        }
    }

    private val responseProvider = object : ChatResponseProvider {
        override suspend fun onMessageReceived(request: Message): MessageResponse {
            // For now just acknowledge that we received the message.
            Log.e("onMessageReceived: ", "Message received from $request")
            addMessage("Message Received and awaiting for response : $request")
            /*addToMessageList(
                Message(
                    messageId = request.messageId,
                    senderId = request.senderId,
                    receiverId = request.receiverId,
                    content = request.content,
                    timestamp = request.timestamp,
                    status = true,
                    type = request.type
                )
            )*/
            return MessageResponse(
                received = true,
                info = "Ack from Main Activity",
                type = "SINGLE"
            )
        }
    }

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    fun addMessage(message: String) {
        _messages.update { it + message }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    private val _messageList = MutableStateFlow<List<Message>>(emptyList())
    val messageList: StateFlow<List<Message>> = _messageList.asStateFlow()

    fun addToMessageList(message: Message) {
        _messageList.update { it + message }
    }

    //<editor-fold desc="Devices Management">
    val devices: StateFlow<List<DeviceEntity>> =
        deviceRepository.observeDevices()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // Selected Device state
    private val _selectedDevice = MutableStateFlow<DeviceEntity?>(null)
    val selectedDevice: StateFlow<DeviceEntity?> = _selectedDevice.asStateFlow()
    fun selectDevice(device: DeviceEntity) {
        _selectedDevice.value = device
    }
    //</editor-fold>

    fun changeGrpcPort(port: Int) {
        offlineSdk.changeGrpcPort(port)
    }

    fun changeUdpPort(port: Int) {
        offlineSdk.changeUdpPort(port)
    }

    fun start(deviceId: String) {
        offlineSdk.startServiceOnCustomModeWithStartDiscovery(
            udpPort = 35353,
            grpcPort = 35354,
            broadcastIntervalMs = 2000L,
            deviceTimeoutMs = 5000L,
            deleteDeviceOnTimeout = false,
            printLog = true,
            udpPrintLog = false,
            events = grpcEvents,
            provider = responseProvider,
            deviceId = deviceId
        )
    }

    fun stop() {
        offlineSdk.stopService()
    }

    fun deleteAllDevices() {
        clearMessages()
        viewModelScope.launch(Dispatchers.IO) {
            deviceRepository.deleteAllDevices()
        }
    }

    fun broadcastMessage(broadCastMessage: String) {
        offlineSdk.sendMessageBroadcast(
            targets = null,
            message = Message(
                messageId = Calendar.getInstance().timeInMillis,
                senderId = Utils.getDeviceIpAddress() ?: "",
                receiverId = "Broadcast",
                content = broadCastMessage,
                timestamp = Calendar.getInstance().timeInMillis,
                status = false,
                type = "BROADCAST"
            ),
            maxConcurrency = 10
        )
    }

    fun sendMessageToSelectedDevice(messageContent: String) {
        val device = selectedDevice.value ?: return
        val message = Message(
            messageId = Calendar.getInstance().timeInMillis,
            senderId = Utils.getDeviceIpAddress() ?: "",
            receiverId = device.id,
            content = messageContent,
            timestamp = Calendar.getInstance().timeInMillis,
            status = false,
            type = "SINGLE"
        )
        offlineSdk.sendMessage(
            ip = device.id,
            port = device.port,
            payload = message
        )
        addToMessageList(message)
    }

    fun sendMessageWithCallbackToSelectedDevice(messageContent: String) {
        val device = selectedDevice.value ?: return
        val message = Message(
            messageId = Calendar.getInstance().timeInMillis,
            senderId = Utils.getDeviceIpAddress() ?: "",
            receiverId = device.id,
            content = messageContent,
            timestamp = Calendar.getInstance().timeInMillis,
            status = false,
            type = "SINGLE"
        )
        addToMessageList(message)

        offlineSdk.sendMessageWithCallback(
            ip = device.id,
            port = device.port,
            payload = message
        ) { result ->
            when (result) {
                is GrpcResult.Success -> {
                    Log.d("Message Callback", "Success: ${result.data}")
                    addMessage("Response from ${device.name}: ${result.data}")
                }

                is GrpcResult.Error -> {
                    Log.e("Message Callback", "Error: ${result.throwable}")
                    addMessage("Error sending message to ${device.name}: ${result.throwable.message}")
                }

                is GrpcResult.Timeout -> {
                    addMessage("Sent Single message to ${device.id}: $messageContent -> No response from ${result.ip}")
                    Log.e(
                        "sendSingleMessage: ",
                        "GrpcResult.Timeout -> No response from ${result.ip}"
                    )
                }
            }
        }
    }
}