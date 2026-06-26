package com.example.ringtoneid.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ringtoneid.audio.AudioOutputFormat
import com.example.ringtoneid.audio.MidiInstruments
import com.example.ringtoneid.audio.MusicalKeys
import com.example.ringtoneid.audio.MusicalStyles
import androidx.compose.material3.RangeSlider
import com.example.ringtoneid.audio.Articulations
import com.example.ringtoneid.audio.Harmonies
import com.example.ringtoneid.audio.MelodicContours
import com.example.ringtoneid.audio.MotifRepeat
import com.example.ringtoneid.audio.Octaves
import com.example.ringtoneid.audio.Tempo
import com.example.ringtoneid.audio.TempoContours
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneDetailScreen(
    contactId: Long,
    onBack: () -> Unit,
    viewModel: RingtoneDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var hasWritePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingSetRingtone by remember { mutableStateOf(false) }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasWritePermission = granted
        if (granted && pendingSetRingtone) {
            pendingSetRingtone = false
            viewModel.setAsRingtone()
        }
    }

    LaunchedEffect(contactId) { viewModel.loadContact(contactId) }

    LaunchedEffect(uiState) {
        val state = uiState as? DetailUiState.Ready ?: return@LaunchedEffect
        when {
            state.savedSuccess -> snackbarHostState.showSnackbar("Ringtone set successfully!")
            state.error != null -> snackbarHostState.showSnackbar("Error: ${state.error}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((uiState as? DetailUiState.Ready)?.contact?.name ?: "Ringtone") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is DetailUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is DetailUiState.Error -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

            is DetailUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(state.contact.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(state.contact.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(24.dp))
                    Text("Your Ringtone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state.profile.seed > 0) {
                        Text(
                            "Variation #${state.profile.seed}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // Waveform visualization
                    WaveformVisualizer(
                        isPlaying = state.isPlaying,
                        barCount = state.profile.notes.size.coerceAtLeast(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Note names
                    Text(
                        text = state.profile.notes.joinToString(" ") { midiToNoteName(it) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Controls: Play + Variation nav
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedIconButton(
                            onClick = { viewModel.prevSeed() },
                            enabled = state.profile.seed > 0
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous variation")
                        }
                        FloatingActionButton(
                            onClick = { viewModel.togglePreview() },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        OutlinedIconButton(onClick = { viewModel.shuffleSeed() }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next variation")
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { viewModel.surprise() }) {
                            Text("🎲 Surprise")
                        }
                        TextButton(onClick = { viewModel.resetToDefaults() }) {
                            Text("Reset to defaults")
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Length slider
                    Text(
                        "Length: ${state.profile.noteCount} notes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.profile.noteCount.toFloat(),
                        onValueChange = { viewModel.updateNoteCount(it.roundToInt()) },
                        valueRange = 4f..16f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("16", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Format selector
                    Text(
                        "Format: ${state.profile.format.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioOutputFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = state.profile.format.equals(fmt.name, ignoreCase = true),
                                onClick = { viewModel.updateFormat(fmt.name.lowercase()) },
                                label = { Text(fmt.label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Instrument selector
                    val currentInstrument = MidiInstruments.findByProgram(state.profile.midiProgram)
                    var instrumentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = instrumentExpanded,
                        onExpandedChange = { instrumentExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentInstrument.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Instrument") },
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
                                        viewModel.updateInstrument(instrument.program)
                                        instrumentExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Musical style selector
                    val currentStyle = MusicalStyles.fromId(state.profile.style)
                    var styleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = styleExpanded,
                        onExpandedChange = { styleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${currentStyle.displayName} — ${currentStyle.description}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Style") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = styleExpanded,
                            onDismissRequest = { styleExpanded = false }
                        ) {
                            MusicalStyles.ALL.forEach { style ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(style.displayName, fontWeight = FontWeight.Bold)
                                            Text(
                                                style.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateStyle(style.id)
                                        styleExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Key selector
                    val currentKey = MusicalKeys.nameForRoot(state.profile.rootNote)
                    var keyExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = keyExpanded,
                        onExpandedChange = { keyExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentKey,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Key") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = keyExpanded,
                            onDismissRequest = { keyExpanded = false }
                        ) {
                            MusicalKeys.ALL.forEach { key ->
                                DropdownMenuItem(
                                    text = { Text(key.displayName) },
                                    onClick = {
                                        viewModel.updateRootNote(key.rootNote)
                                        keyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Tempo range + motion
                    Text(
                        "Tempo: ${state.profile.tempoMin}–${state.profile.tempoMax} BPM",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    RangeSlider(
                        value = state.profile.tempoMin.toFloat()..state.profile.tempoMax.toFloat(),
                        onValueChange = { viewModel.updateTempoRange(it.start.roundToInt(), it.endInclusive.roundToInt()) },
                        valueRange = Tempo.MIN_BPM.toFloat()..Tempo.MAX_BPM.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${Tempo.MIN_BPM} (slow)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${Tempo.MAX_BPM} (fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(16.dp))

                    var tempoContourExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = tempoContourExpanded,
                        onExpandedChange = { tempoContourExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = TempoContours.fromId(state.profile.tempoContour).displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tempo motion") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tempoContourExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = tempoContourExpanded,
                            onDismissRequest = { tempoContourExpanded = false }
                        ) {
                            TempoContours.ALL.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.displayName, fontWeight = FontWeight.Bold)
                                            Text(
                                                option.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateTempoContour(option.id)
                                        tempoContourExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Melody contour
                    var contourExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = contourExpanded,
                        onExpandedChange = { contourExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = MelodicContours.fromId(state.profile.contour).displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Melody") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contourExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = contourExpanded,
                            onDismissRequest = { contourExpanded = false }
                        ) {
                            MelodicContours.ALL.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.displayName, fontWeight = FontWeight.Bold)
                                            Text(
                                                option.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateContour(option.id)
                                        contourExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Octave/Register
                    Text(
                        "Register",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Octaves.ALL.forEach { option ->
                            FilterChip(
                                selected = state.profile.octaveShift == option.shift,
                                onClick = { viewModel.updateOctave(option.shift) },
                                label = { Text(option.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Repeat
                    Text(
                        "Repeat phrase: ${state.profile.repeatCount}×",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.profile.repeatCount.toFloat(),
                        onValueChange = { viewModel.updateRepeat(it.roundToInt()) },
                        valueRange = MotifRepeat.MIN.toFloat()..MotifRepeat.MAX.toFloat(),
                        steps = MotifRepeat.MAX - MotifRepeat.MIN - 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    // Articulation
                    Text(
                        "Articulation",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Articulations.ALL.forEach { option ->
                            FilterChip(
                                selected = state.profile.articulation == option.id,
                                onClick = { viewModel.updateArticulation(option.id) },
                                label = { Text(option.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Harmony
                    var harmonyExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = harmonyExpanded,
                        onExpandedChange = { harmonyExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = Harmonies.fromId(state.profile.harmony).displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Harmony") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = harmonyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = harmonyExpanded,
                            onDismissRequest = { harmonyExpanded = false }
                        ) {
                            Harmonies.ALL.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.displayName, fontWeight = FontWeight.Bold)
                                            Text(
                                                option.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateHarmony(option.id)
                                        harmonyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Set as Ringtone button
                    Button(
                        onClick = {
                            if (hasWritePermission) {
                                viewModel.setAsRingtone()
                            } else {
                                pendingSetRingtone = true
                                writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                            }
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text(if (state.savedSuccess) "✓ Set as Ringtone" else "Set as Ringtone")
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(
    isPlaying: Boolean,
    barCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val heightFraction = if (isPlaying) {
                (0.3f + 0.7f * abs(sin(phase + i * 0.5f))).coerceIn(0.15f, 1f)
            } else {
                0.3f + 0.15f * sin(i * 1.2f).toFloat()
            }
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

private fun midiToNoteName(midi: Int): String {
    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octave = (midi / 12) - 1
    val note = noteNames[midi % 12]
    return "$note$octave"
}
