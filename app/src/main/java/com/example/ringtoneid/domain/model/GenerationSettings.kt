package com.example.ringtoneid.domain.model

import android.content.SharedPreferences
import com.example.ringtoneid.audio.RingtoneDefaults
import org.json.JSONObject

/**
 * A complete, self-contained bundle of every setting that drives ringtone generation.
 * Used as the single unit that a [GenerationPreset] carries and that the global defaults
 * (legacy flat prefs) are read into, so every "generate" entry point shares one shape.
 */
data class GenerationSettings(
    val format: String = RingtoneDefaults.FORMAT,
    val instrument: Int = RingtoneDefaults.INSTRUMENT,
    val length: Int = RingtoneDefaults.LENGTH,
    val style: String = RingtoneDefaults.STYLE,
    val root: Int = RingtoneDefaults.ROOT,
    val tempoMin: Int = RingtoneDefaults.TEMPO,
    val tempoMax: Int = RingtoneDefaults.TEMPO,
    val tempoContour: String = RingtoneDefaults.TEMPO_CONTOUR,
    val contour: String = RingtoneDefaults.CONTOUR,
    val octave: Int = RingtoneDefaults.OCTAVE,
    val repeat: Int = RingtoneDefaults.REPEAT,
    val articulation: String = RingtoneDefaults.ARTICULATION,
    val harmony: String = RingtoneDefaults.HARMONY
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("format", format)
        put("instrument", instrument)
        put("length", length)
        put("style", style)
        put("root", root)
        put("tempoMin", tempoMin)
        put("tempoMax", tempoMax)
        put("tempoContour", tempoContour)
        put("contour", contour)
        put("octave", octave)
        put("repeat", repeat)
        put("articulation", articulation)
        put("harmony", harmony)
    }

    companion object {
        /** Hard-coded factory bundle (mirrors [RingtoneDefaults]). */
        val FACTORY = GenerationSettings()

        /** Extracts the generation settings carried by an already-built [RingtoneProfile]. */
        fun fromProfile(p: RingtoneProfile): GenerationSettings = GenerationSettings(
            format = p.format,
            instrument = p.midiProgram,
            length = p.noteCount,
            style = p.style,
            root = p.rootNote,
            tempoMin = p.tempoMin,
            tempoMax = p.tempoMax,
            tempoContour = p.tempoContour,
            contour = p.contour,
            octave = p.octaveShift,
            repeat = p.repeatCount,
            articulation = p.articulation,
            harmony = p.harmony
        )

        fun fromJson(o: JSONObject): GenerationSettings = GenerationSettings(
            format = o.optString("format", RingtoneDefaults.FORMAT),
            instrument = o.optInt("instrument", RingtoneDefaults.INSTRUMENT),
            length = o.optInt("length", RingtoneDefaults.LENGTH),
            style = o.optString("style", RingtoneDefaults.STYLE),
            root = o.optInt("root", RingtoneDefaults.ROOT),
            tempoMin = o.optInt("tempoMin", RingtoneDefaults.TEMPO),
            tempoMax = o.optInt("tempoMax", RingtoneDefaults.TEMPO),
            tempoContour = o.optString("tempoContour", RingtoneDefaults.TEMPO_CONTOUR),
            contour = o.optString("contour", RingtoneDefaults.CONTOUR),
            octave = o.optInt("octave", RingtoneDefaults.OCTAVE),
            repeat = o.optInt("repeat", RingtoneDefaults.REPEAT),
            articulation = o.optString("articulation", RingtoneDefaults.ARTICULATION),
            harmony = o.optString("harmony", RingtoneDefaults.HARMONY)
        )

        /** Reads the legacy flat `default_*` prefs into a bundle (used for migration + sample). */
        fun fromPrefs(prefs: SharedPreferences): GenerationSettings {
            val legacyTempo = prefs.getInt("default_tempo", RingtoneDefaults.TEMPO)
            return GenerationSettings(
                format = prefs.getString("default_format", RingtoneDefaults.FORMAT) ?: RingtoneDefaults.FORMAT,
                instrument = prefs.getInt("default_instrument", RingtoneDefaults.INSTRUMENT),
                length = prefs.getInt("default_length", RingtoneDefaults.LENGTH),
                style = prefs.getString("default_style", RingtoneDefaults.STYLE) ?: RingtoneDefaults.STYLE,
                root = prefs.getInt("default_root", RingtoneDefaults.ROOT),
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
    }
}
