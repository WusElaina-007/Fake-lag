package com.netconditioner.vpn

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.configDataStore by preferencesDataStore("netconditioner_ultra_configs")

@Serializable
@SerialName("UltraConfig")
data class UltraConfig(
    val id: String,
    val name: String,
    val latencyMs: Int,
    val jitterMs: Int,
    val lossPercent: Int,
    val uploadKbps: Int,
    val downloadKbps: Int,
    val createdAt: Long,
)

@Serializable
data class ConfigPayload(
    val schemaVersion: Int = 1,
    val name: String,
    val latency: Int,
    val jitter: Int,
    val loss: Int,
    val uploadKbps: Int,
    val downloadKbps: Int,
    val createdAt: Long,
)

@Serializable
private data class ConfigCollection(
    val configs: List<UltraConfig>,
    val floatingScale: Float = 1.0f,
    val graphOverlayEnabled: Boolean = true,
)

class ConfigStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val configsKey = preferencesKey<String>("configs_json")

    val configsFlow: Flow<List<UltraConfig>> = context.configDataStore.data.map { prefs ->
        val stored = prefs[configsKey]
        if (stored.isNullOrBlank()) {
            listOf(defaultConfig())
        } else {
            runCatching { json.decodeFromString(ConfigCollection.serializer(), stored).configs }
                .getOrElse { listOf(defaultConfig()) }
        }
    }

    val floatingScaleFlow: Flow<Float> = context.configDataStore.data.map { prefs ->
        val stored = prefs[configsKey]
        if (stored.isNullOrBlank()) {
            1.0f
        } else {
            runCatching { json.decodeFromString(ConfigCollection.serializer(), stored).floatingScale }
                .getOrDefault(1.0f)
        }
    }

    val graphOverlayFlow: Flow<Boolean> = context.configDataStore.data.map { prefs ->
        val stored = prefs[configsKey]
        if (stored.isNullOrBlank()) {
            true
        } else {
            runCatching { json.decodeFromString(ConfigCollection.serializer(), stored).graphOverlayEnabled }
                .getOrDefault(true)
        }
    }

    suspend fun saveConfigs(
        configs: List<UltraConfig>,
        floatingScale: Float,
        graphOverlayEnabled: Boolean,
    ) {
        val payload = ConfigCollection(
            configs = configs,
            floatingScale = floatingScale,
            graphOverlayEnabled = graphOverlayEnabled,
        )
        context.configDataStore.edit { prefs ->
            prefs[configsKey] = json.encodeToString(payload)
        }
    }

    suspend fun addOrUpdateConfig(
        configs: List<UltraConfig>,
        config: UltraConfig,
        floatingScale: Float,
        graphOverlayEnabled: Boolean,
    ) {
        val updated = configs.toMutableList()
        val index = updated.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            updated[index] = config
        } else {
            updated.add(0, config)
        }
        saveConfigs(updated, floatingScale, graphOverlayEnabled)
    }

    suspend fun deleteConfig(
        configs: List<UltraConfig>,
        configId: String,
        floatingScale: Float,
        graphOverlayEnabled: Boolean,
    ) {
        val updated = configs.filterNot { it.id == configId }
        saveConfigs(updated, floatingScale, graphOverlayEnabled)
    }

    fun encodeExport(config: UltraConfig): String {
        val payload = ConfigPayload(
            schemaVersion = 1,
            name = config.name,
            latency = config.latencyMs,
            jitter = config.jitterMs,
            loss = config.lossPercent,
            uploadKbps = config.uploadKbps,
            downloadKbps = config.downloadKbps,
            createdAt = config.createdAt,
        )
        return json.encodeToString(payload)
    }

    fun decodeImport(raw: String): Result<ConfigPayload> {
        return runCatching {
            val payload = json.decodeFromString(ConfigPayload.serializer(), raw)
            require(payload.schemaVersion == 1) { "Unsupported schema version" }
            payload
        }
    }

    companion object {
        fun defaultConfig() = UltraConfig(
            id = "default",
            name = "Ultra Balanced",
            latencyMs = 80,
            jitterMs = 20,
            lossPercent = 1,
            uploadKbps = 1500,
            downloadKbps = 5000,
            createdAt = System.currentTimeMillis(),
        )
    }
}
