package com.netconditioner.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetConditionerTheme {
                val context = LocalContext.current
                val viewModel: MainViewModel = viewModel()
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

                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json"),
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    val exporter = ConfigImportExport(context)
                    val payload = ConfigStore(context).encodeExport(VpnController.currentConfig.value)
                    lifecycleScope.launch {
                        exporter.exportConfig(uri, payload)
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    val importer = ConfigImportExport(context)
                    lifecycleScope.launch {
                        val result = importer.importConfig(uri)
                        result.onSuccess { raw ->
                            val store = ConfigStore(context)
                            val parsed = store.decodeImport(raw)
                            if (parsed.isSuccess) {
                                viewModel.updateImportPreview(parsed.getOrNull(), null)
                            } else {
                                viewModel.updateImportPreview(null, "Invalid or unsupported config file")
                            }
                        }.onFailure {
                            viewModel.updateImportPreview(null, it.message ?: "Failed to read file")
                        }
                    }
                }

                DashboardCompose(
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
                    onRequestExport = {
                        exportLauncher.launch("netconditioner-${System.currentTimeMillis()}.netcfg")
                    },
                    onRequestImport = {
                        importLauncher.launch(arrayOf("application/json", "text/*"))
                    },
                    viewModel = viewModel,
                )

                LaunchedEffect(Unit) {
                    VpnController.bindAppContext(applicationContext)
                }
            }
        }
    }
}
