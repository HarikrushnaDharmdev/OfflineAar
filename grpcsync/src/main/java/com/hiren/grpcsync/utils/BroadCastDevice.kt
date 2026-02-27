package com.hiren.grpcsync.utils

sealed class BroadCastDevice {
    data class ALL(val includeSelf: Boolean) : BroadCastDevice()
    data class ONLINE(val includeSelf: Boolean) : BroadCastDevice()
}

enum class MessageType {
    SINGLE, BROADCAST, STREAM
}