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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
            item {
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
                        supportingContent = { Text(profile.phoneNumber, style = MaterialTheme.typography.bodySmall) },
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
