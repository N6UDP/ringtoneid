package com.example.ringtoneid.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ringtoneid.audio.Articulations
import com.example.ringtoneid.audio.AudioOutputFormat
import com.example.ringtoneid.audio.Harmonies
import com.example.ringtoneid.audio.MelodicContours
import com.example.ringtoneid.audio.MidiInstruments
import com.example.ringtoneid.audio.MotifRepeat
import com.example.ringtoneid.audio.MusicalKeys
import com.example.ringtoneid.audio.MusicalStyles
import com.example.ringtoneid.audio.Octaves
import com.example.ringtoneid.audio.Tempo
import com.example.ringtoneid.audio.TempoContours
import com.example.ringtoneid.domain.model.GenerationSettings
import kotlin.math.roundToInt

/**
 * The full set of generation controls (format, instrument, length, style, key, tempo,
 * melody/register/repeat/articulation/harmony) bound to a [GenerationSettings] bundle.
 * Reused for editing each preset in the pool. Renders as a plain [Column] so it can be
 * embedded inside a LazyColumn item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationControls(
    settings: GenerationSettings,
    onFormat: (String) -> Unit,
    onInstrument: (Int) -> Unit,
    onLength: (Int) -> Unit,
    onStyle: (String) -> Unit,
    onRoot: (Int) -> Unit,
    onTempoRange: (Int, Int) -> Unit,
    onTempoContour: (String) -> Unit,
    onContour: (String) -> Unit,
    onOctave: (Int) -> Unit,
    onRepeat: (Int) -> Unit,
    onArticulation: (String) -> Unit,
    onHarmony: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel("Audio format")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AudioOutputFormat.entries.forEach { fmt ->
                FilterChip(
                    selected = settings.format.equals(fmt.name, ignoreCase = true),
                    onClick = { onFormat(fmt.name.lowercase()) },
                    label = { Text(fmt.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Instrument")
        var instrumentExpanded by remember { mutableStateOf(false) }
        val currentInstrument = MidiInstruments.findByProgram(settings.instrument)
        ExposedDropdownMenuBox(
            expanded = instrumentExpanded,
            onExpandedChange = { instrumentExpanded = it }
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
                            onInstrument(instrument.program)
                            instrumentExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Length (notes)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = settings.length,
                range = 4..16,
                onValue = onLength,
                modifier = Modifier.width(96.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = settings.length.toFloat(),
            onValueChange = { onLength(it.roundToInt()) },
            valueRange = 4f..16f,
            steps = 11,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        SectionLabel("Musical style")
        var styleExpanded by remember { mutableStateOf(false) }
        val currentStyle = MusicalStyles.fromId(settings.style)
        ExposedDropdownMenuBox(
            expanded = styleExpanded,
            onExpandedChange = { styleExpanded = it }
        ) {
            OutlinedTextField(
                value = "${currentStyle.displayName} — ${currentStyle.description}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = styleExpanded,
                onDismissRequest = { styleExpanded = false }
            ) {
                MusicalStyles.ALL.forEach { style ->
                    DropdownMenuItem(
                        text = { OptionText(style.displayName, style.description) },
                        onClick = {
                            onStyle(style.id)
                            styleExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Key")
        var keyExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = keyExpanded,
            onExpandedChange = { keyExpanded = it }
        ) {
            OutlinedTextField(
                value = MusicalKeys.nameForRoot(settings.root),
                onValueChange = {},
                readOnly = true,
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
                            onRoot(key.rootNote)
                            keyExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Tempo: ${settings.tempoMin}–${settings.tempoMax} BPM")
        RangeSlider(
            value = settings.tempoMin.toFloat()..settings.tempoMax.toFloat(),
            onValueChange = { onTempoRange(it.start.roundToInt(), it.endInclusive.roundToInt()) },
            valueRange = Tempo.MIN_BPM.toFloat()..Tempo.MAX_BPM.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberField(
                value = settings.tempoMin,
                range = Tempo.MIN_BPM..settings.tempoMax,
                onValue = { onTempoRange(it, settings.tempoMax) },
                label = "Min BPM",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = settings.tempoMax,
                range = settings.tempoMin..Tempo.MAX_BPM,
                onValue = { onTempoRange(settings.tempoMin, it) },
                label = "Max BPM",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Tempo motion")
        var tempoContourExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = tempoContourExpanded,
            onExpandedChange = { tempoContourExpanded = it }
        ) {
            OutlinedTextField(
                value = TempoContours.fromId(settings.tempoContour).displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tempoContourExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = tempoContourExpanded,
                onDismissRequest = { tempoContourExpanded = false }
            ) {
                TempoContours.ALL.forEach { option ->
                    DropdownMenuItem(
                        text = { OptionText(option.displayName, option.description) },
                        onClick = {
                            onTempoContour(option.id)
                            tempoContourExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Melody contour")
        var contourExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = contourExpanded,
            onExpandedChange = { contourExpanded = it }
        ) {
            OutlinedTextField(
                value = MelodicContours.fromId(settings.contour).displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contourExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = contourExpanded,
                onDismissRequest = { contourExpanded = false }
            ) {
                MelodicContours.ALL.forEach { option ->
                    DropdownMenuItem(
                        text = { OptionText(option.displayName, option.description) },
                        onClick = {
                            onContour(option.id)
                            contourExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Register")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Octaves.ALL.forEach { option ->
                FilterChip(
                    selected = settings.octave == option.shift,
                    onClick = { onOctave(option.shift) },
                    label = { Text(option.displayName) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Repeat phrase: ${settings.repeat}×")
        Slider(
            value = settings.repeat.toFloat(),
            onValueChange = { onRepeat(it.roundToInt()) },
            valueRange = MotifRepeat.MIN.toFloat()..MotifRepeat.MAX.toFloat(),
            steps = MotifRepeat.MAX - MotifRepeat.MIN - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        SectionLabel("Articulation")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Articulations.ALL.forEach { option ->
                FilterChip(
                    selected = settings.articulation == option.id,
                    onClick = { onArticulation(option.id) },
                    label = { Text(option.displayName) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Harmony")
        var harmonyExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = harmonyExpanded,
            onExpandedChange = { harmonyExpanded = it }
        ) {
            OutlinedTextField(
                value = Harmonies.fromId(settings.harmony).displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = harmonyExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = harmonyExpanded,
                onDismissRequest = { harmonyExpanded = false }
            ) {
                Harmonies.ALL.forEach { option ->
                    DropdownMenuItem(
                        text = { OptionText(option.displayName, option.description) },
                        onClick = {
                            onHarmony(option.id)
                            harmonyExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
}

/** Compact numeric entry kept in sync with an external Int value, coerced to [range]. */
@Composable
private fun NumberField(
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.filter { it.isDigit() }.take(4)
            text.toIntOrNull()?.let { onValue(it.coerceIn(range.first, range.last)) }
        },
        label = label?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun OptionText(title: String, description: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold)
        Text(
            description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
