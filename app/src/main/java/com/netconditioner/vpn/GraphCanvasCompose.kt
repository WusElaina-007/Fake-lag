package com.netconditioner.vpn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun GraphCanvasCompose(
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val history = remember { mutableStateListOf<MetricsSnapshot>() }
    val snapshot by MetricsBus.shared.collectAsState()

    LaunchedEffect(snapshot) {
        history.add(snapshot)
        if (history.size > 120) {
            history.removeAt(0)
        }
    }

    Box(
        modifier = modifier
            .size((260 * scale).dp, (160 * scale).dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x99101010))
            .padding(12.dp),
    ) {
        Column {
            Text(
                text = "NetConditionerVPN Ultra",
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "In ${snapshot.packetsIn}pps  Out ${snapshot.packetsOut}pps  Drop ${snapshot.packetsDropped}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Text(
                text = "Latency ${snapshot.avgLatencyMs}ms  Jitter ${snapshot.avgJitterMs}ms",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Text(
                text = "Up ${snapshot.uploadKbps}kbps  Down ${snapshot.downloadKbps}kbps",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Canvas(modifier = Modifier.size((220 * scale).dp, (80 * scale).dp)) {
                val maxPps = (history.maxOfOrNull { it.packetsIn + it.packetsOut } ?: 1).toFloat()
                val path = Path()
                history.forEachIndexed { index, item ->
                    val x = size.width * index / maxOf(1, history.size - 1)
                    val y = size.height - (size.height * (item.packetsIn + item.packetsOut) / maxPps)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(path, color = MaterialTheme.colorScheme.primary, alpha = 0.8f)
            }
        }
    }
}
