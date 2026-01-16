package com.hiren.offlineaar

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.hiren.grpcsync.ChatRequest
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.db.MessageResponse
import com.hiren.grpcsync.grpc.ChatResponseProvider
import com.hiren.grpcsync.grpc.GrpcEvent
import com.hiren.grpcsync.grpc.GrpcResult
import com.hiren.grpcsync.public_classes.OfflineCommImpl
import com.hiren.grpcsync.utils.Utils
import com.hiren.offlineaar.ui.theme.OfflineAarTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var offlineComm: OfflineCommImpl

    // SharedFlow to receive gRPC events from the SDK (server started, messages, errors, etc.)
    private val grpcEvents = MutableSharedFlow<GrpcEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startSyncService()
        enableEdgeToEdge()
        setContent {
            OfflineAarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainPage(innerPadding)
                }
            }
        }
    }

    @Preview
    @Composable
    fun PreviewMain() {
        MainPage(PaddingValues(2.dp))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainPage(innerPadding: PaddingValues) {

        val selectedUser by viewModel.selectedDevice.collectAsState()

        var udpPortText by remember { mutableStateOf("") }
        var grpcPortText by remember { mutableStateOf("") }
        var broadCastMessage by remember { mutableStateOf("") }

        val messages by viewModel.messages.collectAsState()

        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(10.dp)
                    .fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Button(
                            onClick = {
                                startDiscoveryService()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Start", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                stopSyncService()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Text("Stop", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                viewModel.clearMessages()
                                viewModel.deleteAllDevices()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF000000))
                        ) {
                            Text("Delete All Devices", color = Color.White)
                        }

                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // UDP Port Input
                        OutlinedTextField(
                            value = udpPortText,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() }) {
                                    udpPortText = value
                                }
                            },
                            label = { Text("UDP Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.width(120.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                udpPortText.toIntOrNull()?.let { port ->
                                    offlineComm.changeUdpPort(port)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("Set UDP", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // gRPC Port Input
                        OutlinedTextField(
                            value = grpcPortText,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() }) {
                                    grpcPortText = value
                                }
                            },
                            label = { Text("gRPC Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.width(120.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                grpcPortText.toIntOrNull()?.let { port ->
                                    offlineComm.changeGrpcPort(port)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                        ) {
                            Text("Set gRPC", color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Broadcast message field
                        OutlinedTextField(
                            value = broadCastMessage,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() }) {
                                    broadCastMessage = value
                                }
                            },
                            label = { Text("Broadcast Message") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text
                            ),
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                offlineComm.sendMessageBroadcast(
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("Send", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    ListingView(viewModel)
                }
            }
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(10.dp)
                    .fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                selectedUser?.let { device ->
                    ChatView(deviceEntity = device, messages)
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select a device to start chat",
                                fontSize = 16.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startSyncService() {
        offlineComm.startServiceOnCustomMode(
            udpPort = 35353,
            grpcPort = 35354,
            broadcastIntervalMs = 2000L,
            deviceTimeoutMs = 5000L,
            deleteDeviceOnTimeout = false,
            printLog = true
        )
    }

    private fun startDiscoveryService() {
        offlineComm.startDiscovery(
            grpcPort = 35354,
            events = grpcEvents,
            provider = responseProvider
        )

        lifecycleScope.launch {
            grpcEvents.collect { event ->
                when (event) {
                    is GrpcEvent.ServerStarted -> {
                        Log.d("GRPC ->>>>>>", "Server started on ${event.port}")
                        viewModel.addMessage("Server started on ${event.port}")
                    }

                    is GrpcEvent.ServerStopped -> {
                        Log.d("GRPC ->>>>>>", "Server Stopped")
                        viewModel.addMessage("Server Stopped")
                    }

                    is GrpcEvent.MessageReceived -> {
                        Log.d("GRPC ->>>>>>", "From ${event.from}: ${event.message}")
                        viewModel.addMessage("From ${event.from}: ${event.message}")
                    }

                    is GrpcEvent.MessageSent -> {
                        Log.d("GRPC ->>>>>>", "Sent to ${event.to}")
                        viewModel.addMessage("Sent to ${event.to}")
                    }

                    is GrpcEvent.Error -> {
                        Log.e("GRPC ->>>>>>", "Error ${event.target}", event.throwable)
                        viewModel.addMessage("Error ${event.target}, ${event.throwable}")
                    }
                }
            }
        }
    }

    private val responseProvider = object : ChatResponseProvider {
        override suspend fun onMessageReceived(request: ChatRequest): MessageResponse {
            // For now just acknowledge that we received the message.
            Log.e("onMessageReceived: ", "Message received from $request")
            viewModel.addMessage("Message Received and awaiting for response : $request")
            return MessageResponse(
                received = true,
                info = "Ack from Main Activity",
                type = "SINGLE"
            )
        }
    }

    private fun stopSyncService() {
        offlineComm.stopService()
    }

    @Composable
    fun ChatView(deviceEntity: DeviceEntity, messages: List<String>) {
        val keyboardController = LocalSoftwareKeyboardController.current

        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    val message = uri.toString()
                    sendSingleMessage(message, deviceEntity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        var inputText by remember { mutableStateOf("") }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    if (deviceEntity.name == "Broadcast Message") {
                        Text(
                            text = deviceEntity.name,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = deviceEntity.id,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = (if (deviceEntity.status) "Online" else "Offline"),
                            fontSize = 12.sp,
                            color = if (deviceEntity.status) Color(0xFF4CAF50) else Color(0xFFC21852)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        MessageItem(message)
                    }
                }

                // Input Area
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF0F0F0),
                                unfocusedContainerColor = Color(0xFFF0F0F0),
                                disabledContainerColor = Color(0xFFF0F0F0),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.size(48.dp)
                        ) {

                        }

                        FloatingActionButton(
                            onClick = {
                                keyboardController?.hide()
                                if (inputText.isNotBlank()) {
                                    sendSingleMessage(inputText, deviceEntity)
                                    inputText = ""
                                }
                            },
                            containerColor = Color(0xFF056C9B),
                            modifier = Modifier.size(48.dp)
                        ) {
                        }
                    }
                }
            }
        }
    }

    private fun sendSingleMessage(
        inputText: String,
        deviceEntity: DeviceEntity
    ) {
        val time = Calendar.getInstance().timeInMillis
        val message = Message(
            messageId = time,
            senderId = Utils.getDeviceIpAddress() ?: "",
            receiverId = deviceEntity.id,
            content = inputText,
            timestamp = Calendar.getInstance().timeInMillis,
            status = false,
            type = "SINGLE"
        )
        viewModel.addMessage("Sent Single message to ${deviceEntity.id}: $inputText")
        offlineComm.sendMessageWithCallback(
            ip = deviceEntity.id,
            port = deviceEntity.port,
            payload = message
        ) { result ->
            when (result) {
                is GrpcResult.Success -> {
                    viewModel.addMessage("Sent Single message to ${deviceEntity.id}: $inputText -> RESPONSE SUCCESS : $result")
                    Log.e("sendSingleMessage: ", "RESPONSE SUCCESS : $result")
                }

                is GrpcResult.Error -> {
                    viewModel.addMessage("Sent Single message to ${deviceEntity.id}: $inputText -> RESPONSE ERROR : ${result.throwable.localizedMessage}")
                    Log.e(
                        "sendSingleMessage: ",
                        "RESPONSE ERROR : ${result.throwable.localizedMessage}"
                    )
                }

                is GrpcResult.Timeout -> {
                    viewModel.addMessage("Sent Single message to ${deviceEntity.id}: $inputText -> No response from ${result.ip}")
                    Log.e(
                        "sendSingleMessage: ",
                        "GrpcResult.Timeout -> No response from ${result.ip}"
                    )
                }
            }
        }
    }


    @Composable
    fun ListingView(viewModel: DeviceViewModel) {
        val devices by viewModel.devices.collectAsStateWithLifecycle()

        LazyColumn(modifier = Modifier.padding(10.dp)) {
            items(
                items = devices,
                key = { it.id } // VERY IMPORTANT
            ) { device ->
                DeviceRow(device)
            }
        }
    }

    @Composable
    fun DeviceRow(device: DeviceEntity) {
        val thisIp = Utils.getDeviceIpAddress()
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE0F7FA) // Light cyan
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable {
                    openChatOfDevice(device)
                }
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (device.name == "Broadcast Message") {
                    Text(
                        device.name,
                        style = TextStyle(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                color = if (device.status) Color.Green else Color.Red,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Column {
                        Text(
                            "${device.id} ${if (device.id == thisIp) "( This Device )" else ""}",
                            style = TextStyle(
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            device.name,
                            style = TextStyle(
                                color = Color.Black.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            "Grpc Port : ${device.port}",
                            style = TextStyle(
                                color = Color.Black.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MessageItem(message: String) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }

    fun openChatOfDevice(device: DeviceEntity) {
        viewModel.selectDevice(device)
    }
}

