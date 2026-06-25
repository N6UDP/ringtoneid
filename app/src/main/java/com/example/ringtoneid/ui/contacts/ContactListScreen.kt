package com.example.ringtoneid.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ringtoneid.domain.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onContactClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermission = allGranted
        if (allGranted) {
            viewModel.loadContacts()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showRemoveAllDialog by remember { mutableStateOf(false) }

    // Bulk action state
    val bulkState by viewModel.bulkActionState.collectAsState()

    if (showRemoveAllDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveAllDialog = false },
            title = { Text("Remove All Ringtones") },
            text = { Text("This will remove all generated ringtones from your contacts. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveAllDialog = false
                    viewModel.removeAllRingtones()
                }) {
                    Text("Remove All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ringtone ID") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermission) {
                PermissionCard(onGrantPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS
                        )
                    )
                })
            } else {
                when (val state = uiState) {
                    is ContactListUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ContactListUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is ContactListUiState.Success -> {
                        val contacts = state.contacts
                        val withRingtone = contacts.count { it.hasCustomRingtone }
                        val total = contacts.size

                        // Summary + bulk actions card
                        BulkActionsCard(
                            withRingtone = withRingtone,
                            total = total,
                            bulkState = bulkState,
                            onGenerateAll = { viewModel.generateAllRingtones() },
                            onRemoveAll = { showRemoveAllDialog = true }
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search contacts") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true
                        )

                        val filtered = contacts.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No contacts found")
                            }
                        } else {
                            LazyColumn {
                                items(filtered, key = { it.id }) { contact ->
                                    ContactItem(contact = contact, onClick = { onContactClick(contact.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkActionsCard(
    withRingtone: Int,
    total: Int,
    bulkState: BulkActionState,
    onGenerateAll: () -> Unit,
    onRemoveAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "$withRingtone of $total contacts have a ringtone",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (total - withRingtone > 0) {
                Text(
                    "${total - withRingtone} contacts still need a tone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bulkState is BulkActionState.InProgress) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { bulkState.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    bulkState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onGenerateAll,
                        enabled = bulkState !is BulkActionState.InProgress && (total - withRingtone) > 0
                    ) {
                        Text("Generate All")
                    }
                    OutlinedButton(
                        onClick = onRemoveAll,
                        enabled = bulkState !is BulkActionState.InProgress && withRingtone > 0
                    ) {
                        Text("Remove All")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrantPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Contacts Permission Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Ringtone ID needs access to your contacts to generate and set personalized ringtones.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGrantPermission) { Text("Grant Permission") }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact, onClick: () -> Unit) {
    val avatarColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )
    val color = avatarColors[(contact.name.firstOrNull()?.code ?: 0) % avatarColors.size]

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(contact.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        trailingContent = if (contact.hasCustomRingtone) {
            {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = "Has custom ringtone",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null
    )
    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
}
