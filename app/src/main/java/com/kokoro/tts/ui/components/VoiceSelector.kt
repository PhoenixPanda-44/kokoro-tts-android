package com.kokoro.tts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kokoro.tts.data.model.VoiceAccent
import com.kokoro.tts.data.model.VoiceGender
import com.kokoro.tts.data.model.VoiceInfo
import com.kokoro.tts.ui.theme.*

@Composable
fun VoiceSelector(
    selectedVoice: VoiceInfo,
    availableVoices: List<VoiceInfo>,
    onVoiceSelected: (VoiceInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate800, RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = Indigo500,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedVoice.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                        color = Slate100
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (selectedVoice.gender == VoiceGender.FEMALE) Rose500.copy(alpha = 0.2f) else Sky500.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (selectedVoice.gender == VoiceGender.FEMALE) "Female" else "Male",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedVoice.gender == VoiceGender.FEMALE) Rose500 else Sky500
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Slate700, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = selectedVoice.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate200
                        )
                    }
                }
                Text(
                    text = selectedVoice.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate200.copy(alpha = 0.6f)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Select Voice",
            tint = Slate200
        )
    }

    if (showDialog) {
        VoiceSelectionDialog(
            selectedVoice = selectedVoice,
            availableVoices = availableVoices,
            onDismiss = { showDialog = false },
            onSelect = {
                onVoiceSelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun VoiceSelectionDialog(
    selectedVoice: VoiceInfo,
    availableVoices: List<VoiceInfo>,
    onDismiss: () -> Unit,
    onSelect: (VoiceInfo) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Female", "Male", "American", "British", "International")

    val filteredVoices = remember(selectedFilter, availableVoices) {
        when (selectedFilter) {
            "Female" -> availableVoices.filter { it.gender == VoiceGender.FEMALE }
            "Male" -> availableVoices.filter { it.gender == VoiceGender.MALE }
            "American" -> availableVoices.filter { it.accent == VoiceAccent.AMERICAN }
            "British" -> availableVoices.filter { it.accent == VoiceAccent.BRITISH }
            "International" -> availableVoices.filter { it.accent == VoiceAccent.INTERNATIONAL }
            else -> availableVoices
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate800,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Speaker Voice",
                    style = MaterialTheme.typography.titleLarge,
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo500,
                                selectedLabelColor = Slate100,
                                containerColor = Slate700,
                                labelColor = Slate200
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredVoices) { voice ->
                        val isSelected = voice.speakerId == selectedVoice.speakerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Indigo600.copy(alpha = 0.25f) else Slate700.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelect(voice) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = voice.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                                        color = if (isSelected) Indigo200 else Slate100
                                    )
                                    Text(
                                        text = voice.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate200.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = voice.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate200.copy(alpha = 0.5f)
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(voice) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Indigo500,
                                    unselectedColor = Slate600
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = Indigo200)
                }
            }
        }
    }
}
