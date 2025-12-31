package com.hiren.offlineaar

import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.MessageEntity
import com.hiren.grpcsync.public_classes.OfflineCommImpl
import com.hiren.grpcsync.utils.Utils
import com.hiren.offlineaar.ui.theme.OfflineAarTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import java.util.Calendar
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var offlineComm: OfflineCommImpl

    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        var isStarted by remember {
            mutableStateOf(false)
        }

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
                    Row(modifier = Modifier.padding(10.dp)) {
                        Button(
                            onClick = {
                                isStarted = true
                                startSyncService()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("Start Sync", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                isStarted = false
                                stopSyncService()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("Stop Sync", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                offlineComm.startDiscovery()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("startDiscovery", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                offlineComm.stopDiscovery()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("stopDiscovery", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Row(modifier = Modifier.padding(10.dp)) {
                        Button(
                            onClick = {
                                offlineComm.sendMessage(
                                    ip = "192.168.2.16", payload = MessageEntity(
                                        messageId = 121321231L,
                                        channelId = "deviceEntity.id",
                                        senderId = Utils.getDeviceIpAddress() ?: "",
                                        receiverId = "0.0.0.0",
                                        content = "Hello from Offline AAR",
                                        timestamp = 12145122145L,
                                        status = false
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("msg", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = {
                                viewModel.deleteAllDevices()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("Delete All Devices", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                offlineComm.changeUdpPort(35363)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("Change UDP Port", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                offlineComm.changeGrpcPort(Random.nextInt(10000, 30000))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF056C9B))
                        ) {
                            Text("Change GRPC Port", color = Color.White)
                        }
                    }
                    if (isStarted)
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
                    ChatView(deviceEntity = device)
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
        offlineComm.startService(
            context = application,
            udpPort = 35353,
            grpcPort = 35354,
            broadcastIntervalMs = 2000L,
            deviceTimeoutMs = 5000L,
            deleteDeviceOnTimeout = false,
            printLog = true
        )
    }

    private fun stopSyncService() {
        offlineComm.stopService(application)
    }

    @Composable
    fun ChatView(deviceEntity: DeviceEntity) {
        val keyboardController = LocalSoftwareKeyboardController.current

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
        val listState = rememberLazyListState()

        /*val messages by viewModel.messages.collectAsState()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }*/

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
                    /*items(messages) { message ->
                        MessageItem(message)
                    }*/
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
        val message = MessageEntity(
            messageId = time,
            channelId = deviceEntity.id,
            senderId = Utils.getDeviceIpAddress() ?: "",
            receiverId = deviceEntity.id,
            content = inputText,
            timestamp = time,
            status = false
        )
    }

    @Composable
    fun ListingView(viewModel: DeviceViewModel) {
        val thisIp = Utils.getDeviceIpAddress()
        val devices by viewModel.devices.collectAsState()

        LazyColumn(modifier = Modifier.padding(10.dp)) {
            items(devices) { device ->
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
        }
    }

    fun openChatOfDevice(device: DeviceEntity) {
        viewModel.selectDevice(device)
    }
}

