package com.netconditioner.vpn

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = ConfigDataStore(application)

    val configs: StateFlow<List<NetworkConfig>> = dataStore.configsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = listOf(NetworkConfig.default()),
    )

    val floatingButtonSize: StateFlow<Float> = dataStore.floatingButtonSizeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 56f,
    )

    private val _editingId = MutableStateFlow<String?>(null)

    var configName by mutableStateOf(NetworkConfig.default().name)
        private set

    var baseLatency by mutableStateOf(NetworkConfig.default().baseLatencyMs)
        private set

    var jitter by mutableStateOf(NetworkConfig.default().jitterMs)
        private set

    var packetLoss by mutableStateOf(NetworkConfig.default().packetLossPercent)
        private set

    var uploadKbps by mutableStateOf(NetworkConfig.default().uploadKbps)
        private set

    var downloadKbps by mutableStateOf(NetworkConfig.default().downloadKbps)
        private set

    val previewText: String
        get() = "Latency ${baseLatency}ms ±${jitter}ms, Loss ${packetLoss}%, " +
            "Up ${uploadKbps}kbps / Down ${downloadKbps}kbps"

    fun updateConfigName(value: String) {
        configName = value
    }

    fun updateBaseLatency(value: Int) {
        baseLatency = value
    }

    fun updateJitter(value: Int) {
        jitter = value
    }

    fun updatePacketLoss(value: Int) {
        packetLoss = value
    }

    fun updateUpload(value: Int) {
        uploadKbps = value
    }

    fun updateDownload(value: Int) {
        downloadKbps = value
    }

    fun updateFloatingButtonSize(value: Float) {
        viewModelScope.launch {
            dataStore.saveFloatingButtonSize(configs.value, value)
        }
    }

    fun saveConfig() {
        val id = _editingId.value ?: UUID.randomUUID().toString()
        val config = NetworkConfig(
            id = id,
            name = configName.ifBlank { "Config ${configs.value.size + 1}" },
            baseLatencyMs = baseLatency,
            jitterMs = jitter,
            packetLossPercent = packetLoss,
            uploadKbps = uploadKbps,
            downloadKbps = downloadKbps,
        )
        viewModelScope.launch {
            dataStore.addOrUpdateConfig(configs.value, config, floatingButtonSize.value)
        }
        _editingId.value = null
    }

    fun startEdit(config: NetworkConfig) {
        _editingId.value = config.id
        configName = config.name
        baseLatency = config.baseLatencyMs
        jitter = config.jitterMs
        packetLoss = config.packetLossPercent
        uploadKbps = config.uploadKbps
        downloadKbps = config.downloadKbps
        VpnController.updateConfig(config)
    }

    fun deleteConfig(configId: String) {
        viewModelScope.launch {
            dataStore.deleteConfig(configs.value, configId, floatingButtonSize.value)
        }
    }

    fun applyConfig(config: NetworkConfig) {
        configName = config.name
        baseLatency = config.baseLatencyMs
        jitter = config.jitterMs
        packetLoss = config.packetLossPercent
        uploadKbps = config.uploadKbps
        downloadKbps = config.downloadKbps
        _editingId.value = config.id
        VpnController.updateConfig(config)
    }

    fun applyCurrentConfig() {
        val config = NetworkConfig(
            id = _editingId.value ?: UUID.randomUUID().toString(),
            name = configName.ifBlank { "Live Config" },
            baseLatencyMs = baseLatency,
            jitterMs = jitter,
            packetLossPercent = packetLoss,
            uploadKbps = uploadKbps,
            downloadKbps = downloadKbps,
        )
        VpnController.updateConfig(config)
    }

    fun pushConfigToVpn(config: NetworkConfig) {
        getApplication<Application>().startService(
            ConditionerVpnService.createApplyConfigIntent(getApplication(), config),
        )
    }
}
