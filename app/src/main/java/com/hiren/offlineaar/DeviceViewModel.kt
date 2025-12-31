package com.hiren.offlineaar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiren.grpcsync.db.DeviceEntity
import com.hiren.grpcsync.public_classes.OfflineCommImpl
import com.hiren.grpcsync.repo.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    offlineSdk: OfflineCommImpl,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val defaultDevice = DeviceEntity(
        id = "0.0.0.0",
        name = "Broadcast Message",
        port = 0,
        status = false,
        lastSeen = 12451254L,
    )

    val devices by lazy {
        deviceRepository.observeDevices()
            .map { deviceList ->
                val others = deviceList.filterNot { it.id == defaultDevice.id }
                listOf(defaultDevice) + others
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
    }

    // Selected Device state
    private val _selectedDevice = MutableStateFlow<DeviceEntity?>(null)
    val selectedDevice: StateFlow<DeviceEntity?> = _selectedDevice.asStateFlow()

    // Select Device
    fun selectDevice(device: DeviceEntity) {
        _selectedDevice.value = device
    }

    // Clear selection
    fun clearSelection() {
        _selectedDevice.value = null
    }

    fun deleteAllDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            deviceRepository.deleteAllDevices()
        }
    }

}
