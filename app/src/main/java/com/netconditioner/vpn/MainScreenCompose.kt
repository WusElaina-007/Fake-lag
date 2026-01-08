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
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@Composable
fun MainScreenCompose(
    onRequestVpnStart: () -> Unit,
    onRequestVpnStop: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val configs by viewModel.configs.collectAsState(initial = emptyList())
    val isRunning by VpnController.isRunning.collectAsState()
    val floatingSize by viewModel.floatingButtonSize.collectAsState(initial = 56f)

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
                text = "NetConditionerVPN",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
            Text(
                text = "Neon network conditioning for your device",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .background(GlassSurfaceBrush)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Live Config",
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
                    ConfigSlider(
                        label = "Base Latency",
                        value = viewModel.baseLatency.toFloat(),
                        range = 0f..800f,
                        unit = "ms",
                        onValueChange = { viewModel.updateBaseLatency(it.roundToInt()) },
                    )
                    ConfigSlider(
                        label = "Jitter",
                        value = viewModel.jitter.toFloat(),
                        range = 0f..300f,
                        unit = "ms",
                        onValueChange = { viewModel.updateJitter(it.roundToInt()) },
                    )
                    ConfigSlider(
                        label = "Packet Loss",
                        value = viewModel.packetLoss.toFloat(),
                        range = 0f..30f,
                        unit = "%",
                        onValueChange = { viewModel.updatePacketLoss(it.roundToInt()) },
                    )
                    ConfigSlider(
                        label = "Upload speed",
                        value = viewModel.uploadKbps.toFloat(),
                        range = 128f..20000f,
                        unit = "kbps",
                        onValueChange = { viewModel.updateUpload(it.roundToInt()) },
                    )
                    ConfigSlider(
                        label = "Download speed",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.saveConfig() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Config")
                        }
                        Button(
                            onClick = {
                                viewModel.applyCurrentConfig()
                                onRequestVpnStart()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                        ) {
                            Icon(Icons.Filled.Power, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start VPN")
                        }
                        TextButton(onClick = onRequestVpnStop) {
                            Icon(Icons.Outlined.StopCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop VPN")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Floating button size (${floatingSize.roundToInt()}dp)")
                    }
                    Slider(
                        value = floatingSize,
                        onValueChange = { viewModel.updateFloatingButtonSize(it) },
                        valueRange = 40f..96f,
                    )
                    TextButton(onClick = onRequestOverlayPermission) {
                        Text("Grant overlay permission")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Saved Configs",
                style = MaterialTheme.typography.titleMedium,
            )

            val listAlpha by animateFloatAsState(if (configs.isNotEmpty()) 1f else 0.6f, label = "listAlpha")
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
private fun ConfigSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Text("${value.roundToInt()} $unit", color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ConfigRow(
    config: NetworkConfig,
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
                text = "${config.baseLatencyMs}ms ±${config.jitterMs}ms • Loss ${config.packetLossPercent}% • " +
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
