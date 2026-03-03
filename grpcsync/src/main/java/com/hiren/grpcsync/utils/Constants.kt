package com.hiren.grpcsync.utils

internal object Constants {

    const val NOTIFICATION_ID = 1001

    // --------------Intent Extras--------------
    const val EXTRA_UDP_TYPE = "extra_udp_type"
    const val EXTRA_UDP_PORT = "extra_udp_port"
    const val EXTRA_GRPC_PORT = "extra_grpc_port"
    const val EXTRA_BROADCAST_INTERVAL = "extra_broadcast_interval"
    const val EXTRA_DEVICE_TIMEOUT = "extra_device_timeout"
    const val EXTRA_DELETE_ON_TIMEOUT = "extra_delete_on_timeout"
    const val EXTRA_PRINT_LOG = "extra_print_log"
    const val EXTRA_CLEANUP_TIME_DELAY = "extra_cleanup_time_delay"
    const val EXTRA_DEVICE_ID = "extra_device_id"

    // --------------Database--------------
    const val DATABASE_NAME = "GRPC_OFFLINE_SYNC_DB"

    // --------------UDP TYPES--------------
    const val UDP_START = "UDP_START"
    const val UDP_STOP = "UDP_STOP"
    const val UDP_STOP_DISCOVERY = "UDP_STOP_DISCOVERY"
    const val UDP_CHANGE_UDP_PORT = "UDP_CHANGE_UDP_PORT"
    const val UDP_CHANGE_GRPC_PORT = "UDP_CHANGE_GRPC_PORT"

    // --------------Defaults--------------
    const val DEFAULT_UDP_PORT = 10080
    const val DEFAULT_GRPC_PORT = 50051
    const val DEFAULT_BROADCAST_INTERVAL = 3000L
    const val DEFAULT_DEVICE_TIMEOUT = 10000L

    // --------------Editable Constants--------------
    var PRINT_LOG = false
    var RESPONSE_TIMEOUT_MS = 3_000L // example: 3 seconds
    var MAX_BROADCAST_DEVICES_CONCURRENCY = 15 // example: limit to 15 concurrent device processing

}