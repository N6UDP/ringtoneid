package com.example.ringtoneid.domain.usecase

import android.content.Context
import com.example.ringtoneid.audio.RingtoneDefaults
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.RingtoneProfile

/**
 * Generates a ringtone profile for [contact] using the user's global default settings
 * stored in SharedPreferences. Single source of truth so every "generate" entry point
 * (Generate All, auto-on-launch, background worker, contact detail) stays consistent.
 */
fun GenerateRingtoneUseCase.fromDefaults(
    context: Context,
    contact: Contact,
    seed: Int = 0
): RingtoneProfile {
    val prefs = context.getSharedPreferences("ringtone_id_prefs", Context.MODE_PRIVATE)
    val legacyTempo = prefs.getInt("default_tempo", RingtoneDefaults.TEMPO)
    return this(
        contact = contact,
        seed = seed,
        noteCount = prefs.getInt("default_length", RingtoneDefaults.LENGTH),
        format = prefs.getString("default_format", RingtoneDefaults.FORMAT) ?: RingtoneDefaults.FORMAT,
        midiProgram = prefs.getInt("default_instrument", RingtoneDefaults.INSTRUMENT),
        style = prefs.getString("default_style", RingtoneDefaults.STYLE) ?: RingtoneDefaults.STYLE,
        rootNote = prefs.getInt("default_root", RingtoneDefaults.ROOT),
        tempoMin = prefs.getInt("default_tempo_min", legacyTempo),
        tempoMax = prefs.getInt("default_tempo_max", legacyTempo),
        tempoContour = prefs.getString("default_tempo_contour", RingtoneDefaults.TEMPO_CONTOUR) ?: RingtoneDefaults.TEMPO_CONTOUR,
        contour = prefs.getString("default_contour", RingtoneDefaults.CONTOUR) ?: RingtoneDefaults.CONTOUR,
        octave = prefs.getInt("default_octave", RingtoneDefaults.OCTAVE),
        repeat = prefs.getInt("default_repeat", RingtoneDefaults.REPEAT),
        articulation = prefs.getString("default_articulation", RingtoneDefaults.ARTICULATION) ?: RingtoneDefaults.ARTICULATION,
        harmony = prefs.getString("default_harmony", RingtoneDefaults.HARMONY) ?: RingtoneDefaults.HARMONY
    )
}
