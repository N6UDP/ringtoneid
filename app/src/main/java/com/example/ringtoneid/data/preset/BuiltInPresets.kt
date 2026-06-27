package com.example.ringtoneid.data.preset

import com.example.ringtoneid.audio.Articulations
import com.example.ringtoneid.audio.Harmonies
import com.example.ringtoneid.audio.MelodicContours
import com.example.ringtoneid.audio.MusicalStyles
import com.example.ringtoneid.audio.TempoContours
import com.example.ringtoneid.domain.model.GenerationSettings

/**
 * A curated, ready-made genre preset the user can drop into their pool and then tweak.
 * These are opinionated "best guess" starting points — instrument, scale, key, tempo and
 * articulation chosen to evoke each genre with the current synth engine.
 */
data class BuiltInPreset(
    val name: String,
    val description: String,
    val settings: GenerationSettings
)

object BuiltInPresets {

    private const val MIDI = "midi"

    val ALL: List<BuiltInPreset> = listOf(
        BuiltInPreset(
            name = "Classic Cell Ringer",
            description = "Bright square-wave beeps — the nostalgic feature-phone sound",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 80,                 // Square Lead
                length = 8,
                style = MusicalStyles.MAJOR_PENTATONIC.id,
                root = 72,                        // C5, up in the beepy register
                tempoMin = 150, tempoMax = 150,
                tempoContour = TempoContours.STEADY.id,
                contour = MelodicContours.AS_IS.id,
                octave = 0,
                repeat = 2,
                articulation = Articulations.STACCATO.id,
                harmony = Harmonies.NONE.id
            )
        ),
        BuiltInPreset(
            name = "Chiptune",
            description = "8-bit pulse lead, fast and bouncy with a looping motif",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 80,                 // Square Lead
                length = 8,
                style = MusicalStyles.MAJOR_PENTATONIC.id,
                root = 60,
                tempoMin = 170, tempoMax = 190,
                tempoContour = TempoContours.STEADY.id,
                contour = MelodicContours.ARCH.id,
                octave = 1,
                repeat = 2,
                articulation = Articulations.STACCATO.id,
                harmony = Harmonies.OCTAVE.id
            )
        ),
        BuiltInPreset(
            name = "Synthwave",
            description = "Retro-80s saw lead in a moody minor, tempo gently breathing",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 81,                 // Sawtooth Lead
                length = 12,
                style = MusicalStyles.MINOR.id,
                root = 60,
                tempoMin = 108, tempoMax = 120,
                tempoContour = TempoContours.WAVE.id,
                contour = MelodicContours.AS_IS.id,
                octave = 0,
                repeat = 1,
                articulation = Articulations.LEGATO.id,
                harmony = Harmonies.FIFTH.id
            )
        ),
        BuiltInPreset(
            name = "Jazz",
            description = "Smooth jazz guitar over a dorian groove with a swing feel",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 26,                 // Jazz Guitar
                length = 10,
                style = MusicalStyles.DORIAN.id,
                root = 60,
                tempoMin = 110, tempoMax = 140,
                tempoContour = TempoContours.SWING.id,
                contour = MelodicContours.AS_IS.id,
                octave = 0,
                repeat = 1,
                articulation = Articulations.LEGATO.id,
                harmony = Harmonies.THIRD.id
            )
        ),
        BuiltInPreset(
            name = "Rock",
            description = "Distortion guitar with power-chord fifths in a bluesy minor",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 30,                 // Distortion Guitar
                length = 8,
                style = MusicalStyles.MINOR_PENTATONIC.id,
                root = 60,
                tempoMin = 150, tempoMax = 170,
                tempoContour = TempoContours.STEADY.id,
                contour = MelodicContours.AS_IS.id,
                octave = 0,
                repeat = 1,
                articulation = Articulations.NORMAL.id,
                harmony = Harmonies.FIFTH.id
            )
        ),
        BuiltInPreset(
            name = "Classical",
            description = "String ensemble, an elegant arch melody with thirds",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 48,                 // String Ensemble
                length = 12,
                style = MusicalStyles.MAJOR.id,
                root = 60,
                tempoMin = 90, tempoMax = 110,
                tempoContour = TempoContours.WAVE.id,
                contour = MelodicContours.ARCH.id,
                octave = 0,
                repeat = 1,
                articulation = Articulations.LEGATO.id,
                harmony = Harmonies.THIRD.id
            )
        ),
        BuiltInPreset(
            name = "Ambient",
            description = "Dreamy new-age pad floating on a lydian scale",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 88,                 // New Age Pad
                length = 12,
                style = MusicalStyles.LYDIAN.id,
                root = 60,
                tempoMin = 80, tempoMax = 96,
                tempoContour = TempoContours.WAVE.id,
                contour = MelodicContours.AS_IS.id,
                octave = 0,
                repeat = 1,
                articulation = Articulations.LEGATO.id,
                harmony = Harmonies.OCTAVE.id
            )
        ),
        BuiltInPreset(
            name = "Blues",
            description = "Steel guitar walking the blues scale with a swung shuffle",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 25,                 // Steel Guitar
                length = 8,
                style = MusicalStyles.BLUES.id,
                root = 60,
                tempoMin = 90, tempoMax = 112,
                tempoContour = TempoContours.SWING.id,
                contour = MelodicContours.AS_IS.id,
                octave = 0,
                repeat = 1,
                articulation = Articulations.LEGATO.id,
                harmony = Harmonies.NONE.id
            )
        ),
        BuiltInPreset(
            name = "Music Box",
            description = "Gentle lullaby twinkle, high and sweet with octaves",
            settings = GenerationSettings(
                format = MIDI,
                instrument = 10,                 // Music Box
                length = 12,
                style = MusicalStyles.MAJOR_PENTATONIC.id,
                root = 72,
                tempoMin = 112, tempoMax = 124,
                tempoContour = TempoContours.STEADY.id,
                contour = MelodicContours.AS_IS.id,
                octave = 0,
                repeat = 1,
                articulation = Articulations.LEGATO.id,
                harmony = Harmonies.OCTAVE.id
            )
        )
    )
}
