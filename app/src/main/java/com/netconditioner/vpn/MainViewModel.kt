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
    private val configStore = ConfigStore(application)

    val configs: StateFlow<List<UltraConfig>> = configStore.configsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = listOf(ConfigStore.defaultConfig()),
    )

    val floatingScale: StateFlow<Float> = configStore.floatingScaleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 1.0f,
    )

    val graphOverlayEnabled: StateFlow<Boolean> = configStore.graphOverlayFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    private val _editingId = MutableStateFlow<String?>(null)

    var configName by mutableStateOf(ConfigStore.defaultConfig().name)
        private set

    var latency by mutableStateOf(ConfigStore.defaultConfig().latencyMs)
        private set

    var jitter by mutableStateOf(ConfigStore.defaultConfig().jitterMs)
        private set

    var packetLoss by mutableStateOf(ConfigStore.defaultConfig().lossPercent)
        private set

    var uploadKbps by mutableStateOf(ConfigStore.defaultConfig().uploadKbps)
        private set

    var downloadKbps by mutableStateOf(ConfigStore.defaultConfig().downloadKbps)
        private set

    var importPreview by mutableStateOf<ConfigPayload?>(null)
        private set

    var importError by mutableStateOf<String?>(null)
        private set

    val previewText: String
        get() = "Latency ${latency}ms ±${jitter}ms, Loss ${packetLoss}%, " +
            "Up ${uploadKbps}kbps / Down ${downloadKbps}kbps"

    fun updateConfigName(value: String) {
        configName = value
    }

    fun updateLatency(value: Int) {
        latency = value
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

    fun updateFloatingScale(value: Float) {
        viewModelScope.launch {
            configStore.saveConfigs(configs.value, value, graphOverlayEnabled.value)
        }
    }

    fun saveConfig() {
        val id = _editingId.value ?: UUID.randomUUID().toString()
        val config = UltraConfig(
            id = id,
            name = configName.ifBlank { "Config ${configs.value.size + 1}" },
            latencyMs = latency,
            jitterMs = jitter,
            lossPercent = packetLoss,
            uploadKbps = uploadKbps,
            downloadKbps = downloadKbps,
            createdAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            configStore.addOrUpdateConfig(configs.value, config, floatingScale.value, graphOverlayEnabled.value)
        }
        _editingId.value = null
    }

    fun startEdit(config: UltraConfig) {
        _editingId.value = config.id
        configName = config.name
        latency = config.latencyMs
        jitter = config.jitterMs
        packetLoss = config.lossPercent
        uploadKbps = config.uploadKbps
        downloadKbps = config.downloadKbps
        VpnController.updateConfig(config)
    }

    fun deleteConfig(configId: String) {
        viewModelScope.launch {
            configStore.deleteConfig(configs.value, configId, floatingScale.value, graphOverlayEnabled.value)
        }
    }

    fun applyConfig(config: UltraConfig) {
        configName = config.name
        latency = config.latencyMs
        jitter = config.jitterMs
        packetLoss = config.lossPercent
        uploadKbps = config.uploadKbps
        downloadKbps = config.downloadKbps
        _editingId.value = config.id
        VpnController.updateConfig(config)
    }

    fun applyCurrentConfig() {
        val config = UltraConfig(
            id = _editingId.value ?: UUID.randomUUID().toString(),
            name = configName.ifBlank { "Live Config" },
            latencyMs = latency,
            jitterMs = jitter,
            lossPercent = packetLoss,
            uploadKbps = uploadKbps,
            downloadKbps = downloadKbps,
            createdAt = System.currentTimeMillis(),
        )
        VpnController.updateConfig(config)
    }

    fun pushConfigToVpn(config: UltraConfig) {
        getApplication<Application>().startService(
            ConditionerVpnService.createApplyConfigIntent(getApplication(), config),
        )
    }

    fun setGraphOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configStore.saveConfigs(configs.value, floatingScale.value, enabled)
        }
    }

    fun updateImportPreview(payload: ConfigPayload?, error: String? = null) {
        importPreview = payload
        importError = error
    }

    fun clearImportPreview() {
        importPreview = null
        importError = null
    }

    fun importPayload(payload: ConfigPayload) {
        val config = UltraConfig(
            id = UUID.randomUUID().toString(),
            name = payload.name,
            latencyMs = payload.latency,
            jitterMs = payload.jitter,
            lossPercent = payload.loss,
            uploadKbps = payload.uploadKbps,
            downloadKbps = payload.downloadKbps,
            createdAt = payload.createdAt,
        )
        viewModelScope.launch {
            configStore.addOrUpdateConfig(configs.value, config, floatingScale.value, graphOverlayEnabled.value)
        }
        clearImportPreview()
    }
}
