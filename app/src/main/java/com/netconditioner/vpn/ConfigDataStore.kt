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

private val Context.configDataStore by preferencesDataStore("netconditioner_configs")

@Serializable
@SerialName("NetworkConfig")
data class NetworkConfig(
    val id: String,
    val name: String,
    val baseLatencyMs: Int,
    val jitterMs: Int,
    val packetLossPercent: Int,
    val uploadKbps: Int,
    val downloadKbps: Int,
) {
    companion object {
        fun default() = NetworkConfig(
            id = "default",
            name = "Default",
            baseLatencyMs = 80,
            jitterMs = 20,
            packetLossPercent = 1,
            uploadKbps = 1500,
            downloadKbps = 5000,
        )
    }
}

@Serializable
private data class ConfigCollection(
    val configs: List<NetworkConfig>,
    val floatingButtonSizeDp: Float = 56f,
)

class ConfigDataStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val configsKey = preferencesKey<String>("configs_json")

    val configsFlow: Flow<List<NetworkConfig>> = context.configDataStore.data.map { prefs ->
        val stored = prefs[configsKey]
        if (stored.isNullOrBlank()) {
            listOf(NetworkConfig.default())
        } else {
            runCatching { json.decodeFromString(ConfigCollection.serializer(), stored).configs }
                .getOrElse { listOf(NetworkConfig.default()) }
        }
    }

    val floatingButtonSizeFlow: Flow<Float> = context.configDataStore.data.map { prefs ->
        val stored = prefs[configsKey]
        if (stored.isNullOrBlank()) {
            56f
        } else {
            runCatching { json.decodeFromString(ConfigCollection.serializer(), stored).floatingButtonSizeDp }
                .getOrDefault(56f)
        }
    }

    suspend fun saveConfigs(configs: List<NetworkConfig>, floatingButtonSizeDp: Float) {
        val payload = ConfigCollection(configs = configs, floatingButtonSizeDp = floatingButtonSizeDp)
        context.configDataStore.edit { prefs ->
            prefs[configsKey] = json.encodeToString(payload)
        }
    }

    suspend fun saveFloatingButtonSize(configs: List<NetworkConfig>, sizeDp: Float) {
        saveConfigs(configs, sizeDp)
    }

    suspend fun addOrUpdateConfig(configs: List<NetworkConfig>, config: NetworkConfig, sizeDp: Float) {
        val updated = configs.toMutableList()
        val index = updated.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            updated[index] = config
        } else {
            updated.add(0, config)
        }
        saveConfigs(updated, sizeDp)
    }

    suspend fun deleteConfig(configs: List<NetworkConfig>, configId: String, sizeDp: Float) {
        val updated = configs.filterNot { it.id == configId }
        saveConfigs(updated, sizeDp)
    }
}
