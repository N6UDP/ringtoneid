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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.combinedClickable
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

private enum class ContactFilter(val label: String) {
    ALL("All"), NEEDS("Needs tone"), HAS("Has tone")
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ContactListScreen(
    onContactClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
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
    var filter by remember { mutableStateOf(ContactFilter.ALL) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Bulk action state
    val bulkState by viewModel.bulkActionState.collectAsState()
    val playingContactId by viewModel.playingContactId.collectAsState()

    val successContacts = (uiState as? ContactListUiState.Success)?.contacts.orEmpty()

    fun exitSelection() {
        selectionMode = false
        selectedIds.clear()
    }

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
                title = { Text(if (selectionMode) "${selectedIds.size} selected" else "Ringtone ID") },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { exitSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        TextButton(onClick = {
                            val all = successContacts.map { it.id }
                            if (selectedIds.size == all.size) selectedIds.clear()
                            else { selectedIds.clear(); selectedIds.addAll(all) }
                        }) {
                            Text(if (selectedIds.size == successContacts.size && successContacts.isNotEmpty()) "None" else "All")
                        }
                    } else {
                        TextButton(onClick = {
                            viewModel.stopPlayback()
                            selectionMode = true
                        }) { Text("Select") }
                        IconButton(onClick = onFavoritesClick) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectionMode) {
                BottomAppBar {
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val chosen = successContacts.filter { selectedIds.contains(it.id) }
                            viewModel.generateForContacts(chosen)
                            exitSelection()
                        },
                        enabled = selectedIds.isNotEmpty() && bulkState !is BulkActionState.InProgress
                    ) {
                        Text("Generate for ${selectedIds.size}")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { selectedIds.clear() }) { Text("Clear") }
                    Spacer(Modifier.width(12.dp))
                }
            }
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

                        // Summary + bulk actions card (hidden during multi-select)
                        if (!selectionMode) {
                            BulkActionsCard(
                                withRingtone = withRingtone,
                                total = total,
                                bulkState = bulkState,
                                onGenerateAll = { viewModel.generateAllRingtones() },
                                onRemoveAll = { showRemoveAllDialog = true }
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search name or number") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ContactFilter.entries.forEach { f ->
                                FilterChip(
                                    selected = filter == f,
                                    onClick = { filter = f },
                                    label = { Text(f.label) }
                                )
                            }
                        }

                        val filtered = contacts.filter { c ->
                            val q = searchQuery.trim()
                            val qDigits = q.filter { it.isDigit() }
                            val matchesSearch = q.isBlank() ||
                                c.name.contains(q, ignoreCase = true) ||
                                (qDigits.isNotEmpty() && c.phoneNumber.filter { it.isDigit() }.contains(qDigits))
                            val matchesFilter = when (filter) {
                                ContactFilter.ALL -> true
                                ContactFilter.NEEDS -> !c.hasCustomRingtone
                                ContactFilter.HAS -> c.hasCustomRingtone
                            }
                            matchesSearch && matchesFilter
                        }
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No contacts found")
                            }
                        } else {
                            LazyColumn {
                                items(filtered, key = { it.id }) { contact ->
                                    ContactItem(
                                        contact = contact,
                                        selectionMode = selectionMode,
                                        selected = selectedIds.contains(contact.id),
                                        isPlaying = playingContactId == contact.id,
                                        onClick = {
                                            if (selectionMode) {
                                                if (selectedIds.contains(contact.id)) selectedIds.remove(contact.id)
                                                else selectedIds.add(contact.id)
                                            } else {
                                                onContactClick(contact.id)
                                            }
                                        },
                                        onLongPress = {
                                            if (!selectionMode) {
                                                viewModel.stopPlayback()
                                                selectionMode = true
                                                selectedIds.add(contact.id)
                                            }
                                        },
                                        onTogglePlay = { viewModel.togglePlay(contact) }
                                    )
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ContactItem(
    contact: Contact,
    selectionMode: Boolean,
    selected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onTogglePlay: () -> Unit
) {
    val avatarColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )
    val color = avatarColors[(contact.name.firstOrNull()?.code ?: 0) % avatarColors.size]

    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress),
        headlineContent = {
            Text(contact.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            } else {
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
            }
        },
        trailingContent = when {
            selectionMode -> null
            contact.hasCustomRingtone -> {
                {
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play ringtone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            else -> null
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
}
