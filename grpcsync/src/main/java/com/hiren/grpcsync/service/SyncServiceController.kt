package com.hiren.grpcsync.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncServiceController @Inject constructor() {

    private var callback: ((Boolean) -> Unit)? = null

    fun setOnStarted(callback: (Boolean) -> Unit) {
        this.callback = callback
    }

    fun notifyStarted(isDiscoveryStarted: Boolean) {
        callback?.invoke(isDiscoveryStarted)
        callback = null
    }
}