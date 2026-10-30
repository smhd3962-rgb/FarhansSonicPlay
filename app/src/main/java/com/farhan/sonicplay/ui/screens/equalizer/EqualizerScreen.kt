package com.farhan.sonicplay.ui.screens.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farhan.sonicplay.playback.AudioEffectsController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    effectsController: AudioEffectsController,
    onBack: () -> Unit
) {
    val effectsState by effectsController.effectsState.collectAsState()
    val presets = remember { effectsController.getPresetNames() }
    val bands = remember { effectsController.getEqualizerBands() }
    val bandRange = remember { effectsController.getBandLevelRange() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer & Sound Effects") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Switch(
                        checked = effectsState.isEnabled,
                        onCheckedChange = { effectsController.setEnabled(it) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Equalizer Presets Dropdown
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preset",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val currentPresetName = if (presets.isNotEmpty() && effectsState.presetIndex in presets.indices) {
                        presets[effectsState.presetIndex]
                    } else "Custom"

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = currentPresetName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            presets.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        effectsController.setPreset(index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Graphic Equalizer Frequency Bands
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Graphic Equalizer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val displayBands = if (bands.isNotEmpty()) bands else listOf(60, 230, 910, 3600, 14000)
                    val minDb = bandRange.first / 100
                    val maxDb = bandRange.second / 100

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        displayBands.forEachIndexed { bandIndex, freqHz ->
                            val currentMilliBels = effectsState.bandLevels[bandIndex] ?: 0
                            val currentDb = currentMilliBels / 100

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Text(
                                    text = "${if (currentDb > 0) "+$currentDb" else "$currentDb"}dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Slider(
                                    value = currentMilliBels.toFloat(),
                                    onValueChange = { newValue ->
                                        effectsController.setBandLevel(bandIndex, newValue.toInt())
                                    },
                                    valueRange = bandRange.first.toFloat()..bandRange.second.toFloat(),
                                    enabled = effectsState.isEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(36.dp)
                                )

                                Text(
                                    text = if (freqHz >= 1000) "${freqHz / 1000}kHz" else "${freqHz}Hz",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Bass Boost Slider
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${effectsState.bassBoostStrength / 10}%",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = effectsState.bassBoostStrength.toFloat(),
                        onValueChange = { effectsController.setBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = effectsState.isEnabled
                    )
                }
            }

            // 3D Virtualizer / Surround Sound Slider
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "3D Surround Virtualizer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${effectsState.virtualizerStrength / 10}%",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = effectsState.virtualizerStrength.toFloat(),
                        onValueChange = { effectsController.setVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = effectsState.isEnabled
                    )
                }
            }
        }
    }
}
