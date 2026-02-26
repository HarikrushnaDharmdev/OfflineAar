package com.hiren.offlineaar

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.db.Message
import com.hiren.grpcsync.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NewChatScreen(
    paddingValues: PaddingValues,
    viewModel: NewChatViewModel = hiltViewModel()
) {

    val androidId = Settings.Secure.getString(
        LocalContext.current.contentResolver,
        Settings.Secure.ANDROID_ID
    )
    viewModel.start(androidId)

    Row(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(0.5f)
                .padding(10.dp)
                .fillMaxSize(),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(15.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            LeftView(viewModel)
        }
        Card(
            modifier = Modifier
                .weight(0.5f)
                .padding(10.dp)
                .fillMaxSize(),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(15.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            RightView(viewModel)
        }
    }
}

@Composable
fun RightView(viewModel: NewChatViewModel) {

    val componentHeight = 55.dp
    var sendMessageText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
        val tabs = listOf("Chat", "Log")
        val pagerState = rememberPagerState(pageCount = { tabs.size })
        val scope = rememberCoroutineScope()

        //<editor-fold desc="Tab And Pager">
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->

            when (page) {
                0 -> ChatView(viewModel)
                1 -> LogView(viewModel)
            }
        }
        //</editor-fold>

        //<editor-fold desc="Device Name Display">
        if (selectedDevice == null) {
            Text(
                "Select a device to start chatting",
                style = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF158CF5), shape = RoundedCornerShape(10.dp))
                    .padding(15.dp),
            )
        } else {
            Text(
                "Currently Chat with ${selectedDevice!!.name} (${selectedDevice!!.id})",
                style = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF158CF5), shape = RoundedCornerShape(10.dp))
                    .padding(15.dp)
            )
        }
        //</editor-fold>

        //<editor-fold desc="Send Message">
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC2E1FF))
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = sendMessageText,
                    onValueChange = { value ->
                        sendMessageText = value
                    },
                    label = { Text("Message") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(componentHeight + 5.dp)
                )

                AssistChip(
                    modifier = Modifier
                        .height(componentHeight),
                    onClick = {
                        viewModel.sendMessageToSelectedDevice(sendMessageText)
                        scope.launch {
                            delay(2000)
                            sendMessageText = ""
                        }
                    },
                    label = {
                        Text(
                            "Send",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )

                AssistChip(
                    modifier = Modifier
                        .height(componentHeight),
                    onClick = {
                        viewModel.sendMessageWithCallbackToSelectedDevice(sendMessageText)
                        scope.launch {
                            delay(2000)
                            sendMessageText = ""
                        }
                    },
                    label = {
                        Text(
                            "Send With Callback",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )
            }
        }
        //</editor-fold>
    }
}

@Composable
fun LogView(viewModel: NewChatViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    //<editor-fold desc="Messages List">
    LazyColumn(modifier = Modifier.padding(15.dp)) {
        items(
            items = messages,
            key = { it }
        ) { device ->
            Text(
                text = device,
                style = TextStyle(
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
    //</editor-fold>
}

@Composable
fun ChatView(viewModel: NewChatViewModel) {
    val messages by viewModel.messageList.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.messageId }) { message ->
            MessageItem(
                message = message,
                isCurrentUser = message.senderId == viewModel.currentIP
            )
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean
) {

    when (message.type) {

        "BROADCAST" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                Surface(
                    shape = RoundedCornerShape(25.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        "SINGLE" -> {
            if (isCurrentUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 0.dp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 16.dp
                        ),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeftView(viewModel: NewChatViewModel) {

    var portText by remember { mutableStateOf("") }
    var broadCastMessage by remember { mutableStateOf("") }

    val componentHeight = 55.dp

    val androidId = Settings.Secure.getString(
        LocalContext.current.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    val scope = rememberCoroutineScope()

    val devices by viewModel.devices.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFC2E1FF))
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            //<editor-fold desc="Control Buttons">
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    modifier = Modifier
                        .weight(1f)
                        .height(componentHeight),
                    onClick = { viewModel.start(androidId) },
                    label = {
                        Text(
                            "Start",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                                .fillMaxWidth()
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )

                AssistChip(
                    modifier = Modifier
                        .weight(1f)
                        .height(componentHeight),
                    onClick = { viewModel.stop() },
                    label = {
                        Text(
                            "Stop",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                                .fillMaxWidth()
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )

                AssistChip(
                    modifier = Modifier.height(componentHeight),
                    onClick = { viewModel.deleteAllDevices() },
                    label = {
                        Text(
                            "Delete All Devices",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )
            }
            //</editor-fold>

            //<editor-fold desc="Port Configuration">
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                OutlinedTextField(
                    value = portText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() }) {
                            portText = value
                        }
                    },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(componentHeight + 5.dp)
                )

                AssistChip(
                    modifier = Modifier.height(componentHeight - 10.dp),
                    onClick = {
                        portText.toIntOrNull()?.let { port ->
                            viewModel.changeUdpPort(port)
                            portText = ""
                        }
                    },
                    label = {
                        Text(
                            "Set UDP",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )

                AssistChip(
                    modifier = Modifier.height(componentHeight - 10.dp),
                    onClick = {
                        portText.toIntOrNull()?.let { port ->
                            viewModel.changeGrpcPort(port)
                            portText = ""
                        }
                    },
                    label = {
                        Text(
                            "Set gRPC",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )
            }
            //</editor-fold>
        }

        //<editor-fold desc="Device List">
        LazyColumn(modifier = Modifier.padding(15.dp)) {
            items(
                items = devices,
                key = { it.id }
            ) { device ->
                DeviceRow(device) {
                    viewModel.selectDevice(it)
                }
            }
        }
        //</editor-fold>

        //<editor-fold desc="Broadcast Message">
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC2E1FF))
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = broadCastMessage,
                    onValueChange = { value ->
                        broadCastMessage = value
                    },
                    label = { Text("Broadcast Message") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(componentHeight + 5.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                AssistChip(
                    modifier = Modifier.height(componentHeight),
                    onClick = {
                        viewModel.broadcastMessage(broadCastMessage)
                        scope.launch {
                            delay(2000)
                            broadCastMessage = ""
                        }
                    },
                    label = {
                        Text(
                            "Send",
                            color = Color(0xFFD8E8FA),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    },
                    border = BorderStroke(1.dp, Color(0xFF07498A)),
                    colors = AssistChipDefaults.assistChipColors(Color(0xFF07498A))
                )
            }
        }
        //</editor-fold>
    }
}

@Composable
fun DeviceRow(device: DeviceEntity, onDevicSelected: (DeviceEntity) -> Unit = {}) {
    val thisIp = Utils.getDeviceIpAddress()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFC2E1FF) // Light cyan
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                onDevicSelected.invoke(device)
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
                            color = Color(0xFF07498A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        device.deviceId,
                        style = TextStyle(
                            color = Color.Black.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
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