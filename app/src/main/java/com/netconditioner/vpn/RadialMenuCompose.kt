package com.netconditioner.vpn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RadialMenuCompose(
    visible: Boolean,
    configs: List<UltraConfig>,
    activeConfigId: String,
    onConfigSelected: (UltraConfig) -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(stiffness = 180f),
        label = "radialScale",
    )

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale),
            contentAlignment = Alignment.Center,
        ) {
            val radius = 90f
            configs.take(6).forEachIndexed { index, config ->
                val angle = (Math.PI * 2 * index / maxOf(1, configs.take(6).size)).toFloat()
                val x = cos(angle) * radius
                val y = sin(angle) * radius
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        .size(56.dp)
                        .background(
                            if (config.id == activeConfigId) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            },
                            CircleShape,
                        )
                        .clickable { onConfigSelected(config) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = config.name.take(2).uppercase(),
                        color = if (config.id == activeConfigId) Color.Black else Color.White,
                    )
                }
            }
        }
    }
}
