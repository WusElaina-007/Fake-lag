package com.netconditioner.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConditionerVpnService : VpnService() {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunInterface: ParcelFileDescriptor? = null
    private var processorJob: Job? = null

    private val configFlow = MutableStateFlow(NetworkConfig.default())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val config = intent.getStringExtra(EXTRA_CONFIG_JSON)?.let {
                    runCatching { Json.decodeFromString(NetworkConfig.serializer(), it) }.getOrNull()
                } ?: NetworkConfig.default()
                startVpn(config)
            }
            ACTION_STOP -> stopVpn()
            ACTION_APPLY_CONFIG -> {
                val config = intent.getStringExtra(EXTRA_CONFIG_JSON)?.let {
                    runCatching { Json.decodeFromString(NetworkConfig.serializer(), it) }.getOrNull()
                }
                config?.let { updateConfig(it) }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn(config: NetworkConfig) {
        updateConfig(config)
        if (tunInterface != null) return

        tunInterface = Builder()
            .setSession("NetConditionerVPN")
            .setMtu(1500)
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .establish()

        if (tunInterface == null) {
            Log.e(TAG, "Failed to establish VPN interface")
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        VpnController.updateRunning(true)
        startService(FloatingButtonService.createStartIntent(this))

        processorJob = scope.launch {
            val processor = PacketProcessor(configFlow)
            processor.start(tunInterface!!)
        }
    }

    private fun updateConfig(config: NetworkConfig) {
        configFlow.value = config
        VpnController.updateConfig(config)
    }

    private fun stopVpn() {
        processorJob?.cancel()
        processorJob = null
        tunInterface?.close()
        tunInterface = null
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopService(FloatingButtonService.createStopIntent(this))
        VpnController.updateRunning(false)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val channelId = "netconditioner_vpn"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NetConditioner VPN",
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NetConditionerVPN running")
            .setContentText("Conditioning traffic with live profile")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ConditionerVpnService"
        private const val NOTIFICATION_ID = 4041
        private const val ACTION_START = "com.netconditioner.vpn.action.START"
        private const val ACTION_STOP = "com.netconditioner.vpn.action.STOP"
        private const val ACTION_APPLY_CONFIG = "com.netconditioner.vpn.action.APPLY_CONFIG"
        private const val EXTRA_CONFIG_JSON = "extra_config_json"

        fun createStartIntent(context: Context, config: NetworkConfig): Intent {
            return Intent(context, ConditionerVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CONFIG_JSON, Json.encodeToString(NetworkConfig.serializer(), config))
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, ConditionerVpnService::class.java).apply {
                action = ACTION_STOP
            }
        }

        fun pushConfig(config: NetworkConfig) {
            VpnController.updateConfig(config)
        }

        fun createApplyConfigIntent(context: Context, config: NetworkConfig): Intent {
            return Intent(context, ConditionerVpnService::class.java).apply {
                action = ACTION_APPLY_CONFIG
                putExtra(EXTRA_CONFIG_JSON, Json.encodeToString(NetworkConfig.serializer(), config))
            }
        }
    }
}
