package com.kokoro.tts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kokoro.tts.data.downloader.DownloadState
import com.kokoro.tts.data.model.ModelType
import com.kokoro.tts.ui.theme.*

@Composable
fun ModelManagerSheet(
    installedModels: Set<ModelType>,
    activeModel: ModelType?,
    isModelLoaded: Boolean,
    downloadState: DownloadState,
    downloadingModelType: ModelType?,
    onDismiss: () -> Unit,
    onLoadModel: (ModelType) -> Unit,
    onUnloadModel: () -> Unit,
    onDownloadModel: (ModelType) -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: (ModelType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Slate800,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kokoro Models",
                            style = MaterialTheme.typography.titleLarge,
                            color = Slate100
                        )
                        Text(
                            text = "Download on demand. Max 1 model loaded in RAM.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate200.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate200)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Single-RAM alert note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Indigo600.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Indigo500,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "To conserve RAM, switching models immediately releases and unloads the previous model.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate200
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(ModelType.entries) { type ->
                        val isInstalled = installedModels.contains(type)
                        val isActive = isInstalled && activeModel == type && isModelLoaded
                        val isDownloadingThis = downloadingModelType == type

                        ModelCard(
                            modelType = type,
                            isInstalled = isInstalled,
                            isActive = isActive,
                            isDownloading = isDownloadingThis,
                            downloadState = if (isDownloadingThis) downloadState else DownloadState.Idle,
                            onLoad = { onLoadModel(type) },
                            onUnload = onUnloadModel,
                            onDownload = { onDownloadModel(type) },
                            onCancelDownload = onCancelDownload,
                            onDelete = { onDeleteModel(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                ) {
                    Text("Done", color = Slate100)
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    modelType: ModelType,
    isInstalled: Boolean,
    isActive: Boolean,
    isDownloading: Boolean,
    downloadState: DownloadState,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Slate700 else Slate800.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelType.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = modelType.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate200.copy(alpha = 0.7f)
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            when {
                                isActive -> Emerald500.copy(alpha = 0.2f)
                                isInstalled -> Sky500.copy(alpha = 0.2f)
                                else -> Slate700
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isActive -> "Loaded in RAM"
                            isInstalled -> "Downloaded"
                            else -> "Not Downloaded"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isActive -> Emerald500
                            isInstalled -> Sky500
                            else -> Slate200.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs row: Download size & RAM footprint
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Download: ~${modelType.estimatedDownloadSizeMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate200.copy(alpha = 0.6f)
                )
                Text(
                    text = "RAM: ~${modelType.estimatedRamUsageMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate200.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download Progress if downloading
            if (isDownloading) {
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Indigo500,
                                trackColor = Slate700
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${downloadState.progressPercent}% (${downloadState.downloadedMb} / ${downloadState.totalMb})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate200
                                )
                                Text(
                                    text = downloadState.speedFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Sky500
                                )
                            }
                            Button(
                                onClick = onCancelDownload,
                                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Download")
                            }
                        }
                    }
                    is DownloadState.Extracting -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = Amber500,
                                trackColor = Slate700
                            )
                            Text(
                                text = "Extracting model files...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber500
                            )
                        }
                    }
                    is DownloadState.Error -> {
                        Text(
                            text = "Error: ${downloadState.message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Rose500
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                        ) {
                            Text("Retry")
                        }
                    }
                    else -> {}
                }
            } else {
                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInstalled) {
                        if (isActive) {
                            OutlinedButton(
                                onClick = onUnload,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber500)
                            ) {
                                Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unload RAM")
                            }
                        } else {
                            Button(
                                onClick = onLoad,
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Load into RAM")
                            }
                        }

                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Rose500)
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Model")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Model?", color = Slate100) },
            text = {
                Text(
                    "Are you sure you want to delete ${modelType.displayName}? You will need to redownload it to use it again.",
                    color = Slate200
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Rose500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Slate200)
                }
            },
            containerColor = Slate800
        )
    }
}
