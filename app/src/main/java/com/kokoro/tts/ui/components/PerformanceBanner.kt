package com.kokoro.tts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kokoro.tts.ui.PerformanceMetrics
import com.kokoro.tts.ui.theme.*

@Composable
fun PerformanceBanner(
    metrics: PerformanceMetrics,
    activeModelName: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate800, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ON-DEVICE TELEMETRY",
                style = MaterialTheme.typography.labelSmall,
                color = Slate200.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            if (activeModelName != null) {
                Box(
                    modifier = Modifier
                        .background(Indigo600.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = activeModelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Indigo200
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MetricItem(
                icon = Icons.Default.Timer,
                label = "TTFB Latency",
                value = if (metrics.latencyMs > 0) "${metrics.latencyMs} ms" else "--"
            )
            MetricItem(
                icon = Icons.Default.Speed,
                label = "Real-Time Factor",
                value = if (metrics.rtf > 0) "%.2fx".format(metrics.rtf) else "--"
            )
            MetricItem(
                icon = Icons.Default.Memory,
                label = "App RAM",
                value = if (metrics.appRamUsedMb > 0) "${metrics.appRamUsedMb} MB" else "--"
            )
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Sky500,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate200.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
            color = Slate100
        )
    }
}
