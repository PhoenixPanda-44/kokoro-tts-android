package com.kokoro.tts.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kokoro.tts.ui.components.*
import com.kokoro.tts.ui.theme.*

private val textPresets = listOf(
    "Hello! Kokoro TTS is running on-device with ONNX Runtime.",
    "The quick brown fox jumps over the lazy dog.",
    "Artificial intelligence transforms the way we communicate with technology.",
    "How are you doing today? I hope you are having a wonderful time!"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TtsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Kokoro TTS",
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        // Active Model Chip
                        Box(
                            modifier = Modifier
                                .background(
                                    if (uiState.isModelLoaded) Emerald500.copy(alpha = 0.2f) else Amber500.copy(alpha = 0.2f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.openModelManager() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = uiState.activeModel?.displayName?.split(" ")?.take(2)?.joinToString(" ") ?: "No Model",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isModelLoaded) Emerald500 else Amber500
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openModelManager() }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Manage Models",
                            tint = Indigo200
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        },
        containerColor = Slate900,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Performance & Memory Banner
            PerformanceBanner(
                metrics = uiState.performanceMetrics,
                activeModelName = uiState.activeModel?.displayName
            )

            // Text Input Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INPUT TEXT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate200.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Paste button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) viewModel.setInputText(text)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Slate200, modifier = Modifier.size(16.dp))
                            }
                            // Clear button
                            IconButton(
                                onClick = { viewModel.setInputText("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate200, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.setInputText(it) },
                        placeholder = { Text("Enter text to synthesize speech...", color = Slate600) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100,
                            cursorColor = Indigo500
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(textPresets) { preset ->
                            SuggestionChip(
                                onClick = { viewModel.setInputText(preset) },
                                label = {
                                    Text(
                                        text = preset.take(24) + "...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate200
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Slate700.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }

            // Voice Selector
            VoiceSelector(
                selectedVoice = uiState.selectedVoice,
                availableVoices = uiState.availableVoices,
                onVoiceSelected = { viewModel.setSelectedVoice(it) }
            )

            // Playback Speed Slider
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SPEED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate200.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "%.1fx".format(uiState.playbackSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Indigo200
                        )
                    }
                    Slider(
                        value = uiState.playbackSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14, // 0.1 increments
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo500,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate700
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Playback and Actions Bar
            PlaybackControls(
                isPlaying = uiState.isPlaying,
                isPaused = uiState.isPaused,
                isSynthesizing = uiState.isSynthesizing,
                hasGeneratedAudio = uiState.hasGeneratedAudio,
                statusText = uiState.statusMessage,
                onPlayPause = { viewModel.playOrSynthesize() },
                onStop = { viewModel.stopPlayback() },
                onShare = {
                    val shareIntent = viewModel.getShareWavIntent(context)
                    if (shareIntent != null) {
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Kokoro Speech WAV"))
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Model Manager Sheet / First-launch Setup
        if (uiState.isModelManagerOpen) {
            ModelManagerSheet(
                installedModels = uiState.installedModels,
                activeModel = uiState.activeModel,
                isModelLoaded = uiState.isModelLoaded,
                downloadState = uiState.downloadState,
                downloadingModelType = uiState.downloadingModelType,
                onDismiss = { viewModel.closeModelManager() },
                onLoadModel = { viewModel.loadModel(it) },
                onUnloadModel = { viewModel.unloadModel() },
                onDownloadModel = { viewModel.downloadModel(it) },
                onCancelDownload = { viewModel.cancelDownload() },
                onDeleteModel = { viewModel.deleteModel(it) }
            )
        }
    }
}
