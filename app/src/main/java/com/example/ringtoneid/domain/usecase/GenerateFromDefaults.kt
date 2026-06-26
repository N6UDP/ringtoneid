package com.example.ringtoneid.domain.usecase

import android.content.Context
import com.example.ringtoneid.data.preset.PresetStore
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.GenerationSettings
import com.example.ringtoneid.domain.model.RingtoneProfile

/**
 * Generates a [RingtoneProfile] for [contact] from an explicit [GenerationSettings] bundle.
 * The single low-level entry point that the preset-pool and defaults helpers delegate to.
 */
fun GenerateRingtoneUseCase.from(
    contact: Contact,
    settings: GenerationSettings,
    seed: Int = 0
): RingtoneProfile = this(
    contact = contact,
    seed = seed,
    noteCount = settings.length,
    format = settings.format,
    midiProgram = settings.instrument,
    style = settings.style,
    rootNote = settings.root,
    tempoMin = settings.tempoMin,
    tempoMax = settings.tempoMax,
    tempoContour = settings.tempoContour,
    contour = settings.contour,
    octave = settings.octave,
    repeat = settings.repeat,
    articulation = settings.articulation,
    harmony = settings.harmony
)

/**
 * Generates from the legacy flat `default_*` prefs. Retained for the Settings sample button
 * and any single-default call site.
 */
fun GenerateRingtoneUseCase.fromDefaults(
    context: Context,
    contact: Contact,
    seed: Int = 0
): RingtoneProfile {
    val prefs = context.getSharedPreferences("ringtone_id_prefs", Context.MODE_PRIVATE)
    return from(contact, GenerationSettings.fromPrefs(prefs), seed)
}

/**
 * Bulk-generation entry point: picks a weighted-random enabled preset from the user's pool
 * (falling back to factory settings when the pool is empty/all disabled) and generates with it.
 * Called by Generate All, auto-on-launch, and the background worker so every contact in a bulk
 * run can get a different preset.
 */
fun GenerateRingtoneUseCase.fromPresetPool(
    context: Context,
    contact: Contact,
    seed: Int = 0
): RingtoneProfile {
    val presets = PresetStore.load(context)
    val settings = PresetStore.pickWeighted(presets)?.settings ?: GenerationSettings.FACTORY
    return from(contact, settings, seed)
}
