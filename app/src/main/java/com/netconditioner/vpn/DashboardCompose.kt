package com.netconditioner.vpn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@Composable
fun DashboardCompose(
    onRequestVpnStart: () -> Unit,
    onRequestVpnStop: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestExport: () -> Unit,
    onRequestImport: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val configs by viewModel.configs.collectAsState(initial = emptyList())
    val isRunning by VpnController.isRunning.collectAsState()
    val floatingScale by viewModel.floatingScale.collectAsState(initial = 1.0f)
    val graphEnabled by viewModel.graphOverlayEnabled.collectAsState(initial = true)
    val importPreview = viewModel.importPreview
    val importError = viewModel.importError
    val context = LocalContext.current

    LaunchedEffect(graphEnabled, isRunning) {
        if (isRunning && graphEnabled) {
            context.startService(OverlayGraphService.createStartIntent(context))
        } else {
            context.startService(OverlayGraphService.createStopIntent(context))
        }
    }

    val neonGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        ),
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Text(
                text = "NetConditionerVPN Ultra",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
            Text(
                text = "Cyber-grade network conditioning",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .background(GlassSurfaceBrush)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Live Conditioning",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.configName,
                        onValueChange = viewModel::updateConfigName,
                        label = { Text("Config name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        CircularSlider(
                            label = "Latency",
                            value = viewModel.latency.toFloat(),
                            range = 0f..800f,
                            unit = "ms",
                            onValueChange = { viewModel.updateLatency(it.roundToInt()) },
                        )
                        CircularSlider(
                            label = "Jitter",
                            value = viewModel.jitter.toFloat(),
                            range = 0f..300f,
                            unit = "ms",
                            onValueChange = { viewModel.updateJitter(it.roundToInt()) },
                        )
                        CircularSlider(
                            label = "Loss",
                            value = viewModel.packetLoss.toFloat(),
                            range = 0f..30f,
                            unit = "%",
                            onValueChange = { viewModel.updatePacketLoss(it.roundToInt()) },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearControl(
                        label = "Upload throttle",
                        value = viewModel.uploadKbps.toFloat(),
                        range = 128f..20000f,
                        unit = "kbps",
                        onValueChange = { viewModel.updateUpload(it.roundToInt()) },
                    )
                    LinearControl(
                        label = "Download throttle",
                        value = viewModel.downloadKbps.toFloat(),
                        range = 128f..50000f,
                        unit = "kbps",
                        onValueChange = { viewModel.updateDownload(it.roundToInt()) },
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.previewText,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.saveConfig() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }
                        Button(
                            onClick = onRequestExport,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2533)),
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export")
                        }
                        Button(
                            onClick = onRequestImport,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2533)),
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import")
                        }
                        Button(
                            onClick = {
                                viewModel.applyCurrentConfig()
                                onRequestVpnStart()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start")
                        }
                        TextButton(onClick = onRequestVpnStop) {
                            Icon(Icons.Outlined.StopCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Floating button scale (${String.format("%.2f", floatingScale)}x)")
                    Slider(
                        value = floatingScale,
                        onValueChange = { viewModel.updateFloatingScale(it) },
                        valueRange = 0.6f..1.6f,
                    )
                    Text("Overlay graph ${if (graphEnabled) "enabled" else "disabled"}")
                    Slider(
                        value = if (graphEnabled) 1f else 0f,
                        onValueChange = { viewModel.setGraphOverlayEnabled(it > 0.5f) },
                        valueRange = 0f..1f,
                    )
                    TextButton(onClick = onRequestOverlayPermission) {
                        Text("Grant overlay permission")
                    }
                }
            }

            AnimatedVisibility(visible = importPreview != null || importError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Import Preview", style = MaterialTheme.typography.titleSmall)
                        if (importError != null) {
                            Text(importError, color = Color(0xFFFF6F91))
                            TextButton(onClick = viewModel::clearImportPreview) { Text("Dismiss") }
                        } else if (importPreview != null) {
                            Text("Name: ${importPreview.name}")
                            Text("Latency ${importPreview.latency}ms • Jitter ${importPreview.jitter}ms • Loss ${importPreview.loss}%")
                            Text("Up ${importPreview.uploadKbps}kbps / Down ${importPreview.downloadKbps}kbps")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.importPayload(importPreview) }) { Text("Import") }
                                TextButton(onClick = viewModel::clearImportPreview) { Text("Cancel") }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Saved Configs",
                style = MaterialTheme.typography.titleMedium,
            )

            AnimatedVisibility(
                visible = configs.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(configs, key = { it.id }) { config ->
                        ConfigRow(
                            config = config,
                            isActive = config.id == VpnController.currentConfig.value.id,
                            onEdit = { viewModel.startEdit(config) },
                            onDelete = { viewModel.deleteConfig(config.id) },
                            onApply = {
                                viewModel.applyConfig(config)
                                if (isRunning) {
                                    viewModel.pushConfigToVpn(config)
                                }
                            },
                        )
                    }
                }
            }
            if (configs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No configs saved yet.")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(neonGradient)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFF45F5D0) else Color(0xFFFF6F91)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "VPN is running" else "VPN stopped",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun LinearControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Text("${value.roundToInt()} $unit", color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(it)
            },
            valueRange = range,
        )
    }
}

@Composable
private fun CircularSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val animatedValue by animateFloatAsState(value, label = "circle")
    Card(
        modifier = Modifier.size(110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, fontSize = 12.sp)
            Text("${animatedValue.roundToInt()} $unit", color = MaterialTheme.colorScheme.primary)
            Slider(
                value = value,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(it)
                },
                valueRange = range,
                modifier = Modifier.height(32.dp),
            )
        }
    }
}

@Composable
private fun ConfigRow(
    config: UltraConfig,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (isActive) {
                    Text(
                        text = "ACTIVE",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${config.latencyMs}ms ±${config.jitterMs}ms • Loss ${config.lossPercent}% • " +
                    "Up ${config.uploadKbps}kbps / Down ${config.downloadKbps}kbps",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply) {
                    Text("Apply")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
