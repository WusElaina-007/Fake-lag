package com.netconditioner.vpn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun FloatingMenuCompose(
    visible: Boolean,
    configs: List<NetworkConfig>,
    activeConfigId: String,
    onConfigSelected: (NetworkConfig) -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "menuScale",
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
            modifier = Modifier
                .scale(scale)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            ),
                        ),
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Quick Config", style = MaterialTheme.typography.titleSmall)
                configs.forEach { config ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfigSelected(config) }
                            .background(
                                if (config.id == activeConfigId) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(12.dp),
                            )
                            .padding(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(config.name, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${config.baseLatencyMs}ms ±${config.jitterMs}ms • Loss ${config.packetLossPercent}%",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "Up ${config.uploadKbps}kbps / Down ${config.downloadKbps}kbps",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                        if (config.id == activeConfigId) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACTIVE", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}
