package com.netconditioner.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetConditionerTheme {
                val context = LocalContext.current
                var pendingStart by remember { mutableStateOf(false) }
                val vpnLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && pendingStart) {
                        context.startService(
                            ConditionerVpnService.createStartIntent(
                                context,
                                VpnController.currentConfig.value,
                            ),
                        )
                    }
                    pendingStart = false
                }

                MainScreenCompose(
                    onRequestVpnStart = {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            pendingStart = true
                            vpnLauncher.launch(intent)
                        } else {
                            context.startService(
                                ConditionerVpnService.createStartIntent(
                                    context,
                                    VpnController.currentConfig.value,
                                ),
                            )
                        }
                    },
                    onRequestVpnStop = {
                        context.startService(ConditionerVpnService.createStopIntent(context))
                    },
                    onRequestOverlayPermission = {
                        val overlayIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}"),
                        )
                        context.startActivity(overlayIntent)
                    },
                )

                LaunchedEffect(Unit) {
                    VpnController.bindAppContext(applicationContext)
                }
            }
        }
    }
}
