package com.example.ringtoneid.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ringtoneid.audio.AudioOutputFormat
import com.example.ringtoneid.audio.MidiInstruments
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
            // Default format section
            item {
                Text(
                    "Default Audio Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Text(
                    "New ringtones will use this format unless overridden per-contact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioOutputFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = uiState.defaultFormat.equals(fmt.name, ignoreCase = true),
                            onClick = { viewModel.setDefaultFormat(fmt.name.lowercase()) },
                            label = { Text(fmt.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // Default instrument section
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Default Instrument",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                val currentInstrument = MidiInstruments.findByProgram(uiState.defaultInstrument)
                var instrumentExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = instrumentExpanded,
                    onExpandedChange = { instrumentExpanded = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = currentInstrument.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instrumentExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = instrumentExpanded,
                        onDismissRequest = { instrumentExpanded = false }
                    ) {
                        var lastCategory = ""
                        MidiInstruments.instruments.forEach { instrument ->
                            if (instrument.category != lastCategory) {
                                lastCategory = instrument.category
                                Text(
                                    instrument.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(instrument.name) },
                                onClick = {
                                    viewModel.setDefaultInstrument(instrument.program)
                                    instrumentExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // Default length section
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Default Length",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Text(
                    "${uiState.defaultLength} notes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Slider(
                    value = uiState.defaultLength.toFloat(),
                    onValueChange = { viewModel.setDefaultLength(it.roundToInt()) },
                    valueRange = 4f..16f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("16", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))

                // Sample button
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (uiState.isSampling) viewModel.stopSample() else viewModel.playSample()
                        }
                    ) {
                        Text(if (uiState.isSampling) "⏹ Stop" else "▶ Sample")
                    }
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
                    supportingContent = { Text("Periodically check for new contacts and generate ringtones (daily)") },
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
