package com.kokoro.tts.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kokoro.tts.ui.theme.*

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isPaused: Boolean,
    isSynthesizing: Boolean,
    hasGeneratedAudio: Boolean,
    statusText: String?,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status text / pulse
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            if (isSynthesizing || isPlaying) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(scale)
                        .background(if (isSynthesizing) Amber500 else Emerald500, CircleShape)
                )
            }
            Text(
                text = statusText ?: (if (isPlaying) "Playing audio..." else "Ready"),
                style = MaterialTheme.typography.labelSmall,
                color = Slate200.copy(alpha = 0.8f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop button
            FilledTonalIconButton(
                onClick = onStop,
                enabled = isPlaying || isPaused || isSynthesizing,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Slate700,
                    contentColor = Slate100,
                    disabledContainerColor = Slate800.copy(alpha = 0.5f),
                    disabledContentColor = Slate600
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Primary Play / Pause Button
            Button(
                onClick = onPlayPause,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo500,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(68.dp)
            ) {
                if (isSynthesizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else if (isPlaying && !isPaused) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Share / Export WAV button
            FilledTonalIconButton(
                onClick = onShare,
                enabled = hasGeneratedAudio && !isSynthesizing,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Slate700,
                    contentColor = Sky500,
                    disabledContainerColor = Slate800.copy(alpha = 0.5f),
                    disabledContentColor = Slate600
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share WAV",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
