package com.netconditioner.vpn

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VpnController {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _currentConfig = MutableStateFlow(ConfigStore.defaultConfig())
    val currentConfig: StateFlow<UltraConfig> = _currentConfig

    private var appContext: Context? = null

    fun bindAppContext(context: Context) {
        appContext = context.applicationContext
    }

    fun updateRunning(running: Boolean) {
        _isRunning.value = running
    }

    fun updateConfig(config: UltraConfig) {
        _currentConfig.value = config
    }
}
