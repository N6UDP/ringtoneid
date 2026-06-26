package com.example.ringtoneid.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ringtoneid.domain.model.GenerationPreset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Profile presets header
            item {
                Text(
                    "Profile Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Text(
                    "Generating for all contacts picks a random preset from this pool — higher " +
                        "weight means chosen more often. Contacts that already have a custom " +
                        "ringtone are never overwritten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            items(uiState.presets, key = { it.id }) { preset ->
                PresetItem(
                    preset = preset,
                    expanded = uiState.editingPresetId == preset.id,
                    isSampling = uiState.isSampling,
                    canDelete = uiState.presets.size > 1,
                    viewModel = viewModel
                )
                HorizontalDivider()
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.addPreset() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add preset")
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // Automation section
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Automation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                ListItem(
                    headlineContent = { Text("Generate on launch") },
                    supportingContent = { Text("Auto-assign ringtones to new contacts when the app opens") },
                    trailingContent = {
                        Switch(
                            checked = uiState.generateOnLaunch,
                            onCheckedChange = { viewModel.setGenerateOnLaunch(it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Background sync") },
                    supportingContent = { Text("Periodically check for new contacts and generate ringtones") },
                    trailingContent = {
                        Switch(
                            checked = uiState.backgroundSync,
                            onCheckedChange = { viewModel.setBackgroundSync(it) }
                        )
                    }
                )

                if (uiState.backgroundSync) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.syncInterval == "daily",
                            onClick = { viewModel.setSyncInterval("daily") },
                            label = { Text("Daily") }
                        )
                        FilterChip(
                            selected = uiState.syncInterval == "weekly",
                            onClick = { viewModel.setSyncInterval("weekly") },
                            label = { Text("Weekly") }
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            "⚠️ Some manufacturers (Samsung, Xiaomi, Huawei, etc.) aggressively kill background apps. " +
                                "You may need to exempt Ringtone ID from battery optimization in your device settings for this to work reliably.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // Saved ringtones section
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Saved Ringtones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.savedRingtones.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No ringtones saved yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.savedRingtones, key = { it.id }) { profile ->
                    ListItem(
                        headlineContent = { Text(profile.contactName) },
                        supportingContent = {
                            Text(
                                "${profile.phoneNumber} · ${profile.format.uppercase()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteRingtone(profile.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ringtone ID", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Version 1.0", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Generates unique personalized ringtones for your contacts based on their phone numbers — inspired by the classic LG Ringtone ID feature.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PresetItem(
    preset: GenerationPreset,
    expanded: Boolean,
    isSampling: Boolean,
    canDelete: Boolean,
    viewModel: SettingsViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectPreset(preset.id) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = preset.enabled,
                onCheckedChange = { viewModel.setPresetEnabled(preset.id, it) }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Weight ${preset.weight}" + if (!preset.enabled) " · disabled" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = preset.name,
                    onValueChange = { viewModel.renamePreset(preset.id, it) },
                    label = { Text("Preset name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "Weight: ${preset.weight}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = preset.weight.toFloat(),
                    onValueChange = { viewModel.setPresetWeight(preset.id, it.roundToInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                GenerationControls(
                    settings = preset.settings,
                    onFormat = viewModel::setFormat,
                    onInstrument = viewModel::setInstrument,
                    onLength = viewModel::setLength,
                    onStyle = viewModel::setStyle,
                    onRoot = viewModel::setRoot,
                    onTempoRange = viewModel::setTempoRange,
                    onTempoContour = viewModel::setTempoContour,
                    onContour = viewModel::setContour,
                    onOctave = viewModel::setOctave,
                    onRepeat = viewModel::setRepeat,
                    onArticulation = viewModel::setArticulation,
                    onHarmony = viewModel::setHarmony
                )

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isSampling) viewModel.stopSample() else viewModel.playSample()
                        }
                    ) {
                        Text(if (isSampling) "⏹ Stop" else "▶ Sample")
                    }
                    OutlinedButton(onClick = { viewModel.duplicatePreset(preset.id) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Duplicate")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.resetEditingToFactory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Reset to factory")
                    }
                    if (canDelete) {
                        TextButton(
                            onClick = { viewModel.deletePreset(preset.id) },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
