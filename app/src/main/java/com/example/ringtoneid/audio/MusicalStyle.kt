package com.example.ringtoneid.audio

/**
 * A musical style defines the scale (set of semitone intervals within an octave)
 * that phone-number digits are mapped onto. Keeping every generated note inside a
 * single scale guarantees the melody always sounds musical, and lets the "mood"
 * be chosen independently of the contact's digits.
 */
data class MusicalStyle(
    val id: String,
    val displayName: String,
    val description: String,
    val intervals: List<Int>
)

object MusicalStyles {
    val MAJOR = MusicalStyle(
        "major", "Classic", "Bright and clear — the original LG feel",
        listOf(0, 2, 4, 5, 7, 9, 11)
    )
    val MAJOR_PENTATONIC = MusicalStyle(
        "major_pentatonic", "Happy", "Cheerful and safe — no wrong notes",
        listOf(0, 2, 4, 7, 9)
    )
    val MINOR = MusicalStyle(
        "minor", "Melancholy", "Sad and serious",
        listOf(0, 2, 3, 5, 7, 8, 10)
    )
    val MINOR_PENTATONIC = MusicalStyle(
        "minor_pentatonic", "Bluesy", "Soulful and laid-back",
        listOf(0, 3, 5, 7, 10)
    )
    val HARMONIC_MINOR = MusicalStyle(
        "harmonic_minor", "Spooky", "Eerie and exotic",
        listOf(0, 2, 3, 5, 7, 8, 11)
    )
    val DORIAN = MusicalStyle(
        "dorian", "Chill", "Jazzy and cool",
        listOf(0, 2, 3, 5, 7, 9, 10)
    )
    val PHRYGIAN = MusicalStyle(
        "phrygian", "Dark", "Spanish and ominous",
        listOf(0, 1, 3, 5, 7, 8, 10)
    )
    val LYDIAN = MusicalStyle(
        "lydian", "Dreamy", "Floating and full of wonder",
        listOf(0, 2, 4, 6, 7, 9, 11)
    )
    val BLUES = MusicalStyle(
        "blues", "Blues", "Classic six-note blues scale",
        listOf(0, 3, 5, 6, 7, 10)
    )

    val ALL = listOf(
        MAJOR, MAJOR_PENTATONIC, MINOR, MINOR_PENTATONIC,
        HARMONIC_MINOR, DORIAN, PHRYGIAN, LYDIAN, BLUES
    )

    /** Default preserves the original "Classic" major character. */
    val DEFAULT = MAJOR

    fun fromId(id: String?): MusicalStyle = ALL.find { it.id == id } ?: DEFAULT
}

/** Musical keys (root notes), one octave of choices starting at C4 (MIDI 60). */
data class MusicalKey(val rootNote: Int, val displayName: String)

object MusicalKeys {
    val ALL = listOf(
        MusicalKey(60, "C"),
        MusicalKey(61, "C#"),
        MusicalKey(62, "D"),
        MusicalKey(63, "D#"),
        MusicalKey(64, "E"),
        MusicalKey(65, "F"),
        MusicalKey(66, "F#"),
        MusicalKey(67, "G"),
        MusicalKey(68, "G#"),
        MusicalKey(69, "A"),
        MusicalKey(70, "A#"),
        MusicalKey(71, "B")
    )

    const val DEFAULT_ROOT = 60

    fun nameForRoot(root: Int): String = ALL.find { it.rootNote == root }?.displayName ?: "C"
}

/** Tempo range, expressed in BPM where each note is an eighth note. */
object Tempo {
    const val MIN_BPM = 80
    const val MAX_BPM = 240
    /** 150 BPM eighth-notes ≈ 200 ms/note, matching the original fixed timing. */
    const val DEFAULT_BPM = 150
}

/** A simple curated, id-addressable option (mood/behaviour selectors). */
data class NamedOption(val id: String, val displayName: String, val description: String)

/** How tempo moves across a single ringtone when a tempo range is set. */
object TempoContours {
    val STEADY = NamedOption("steady", "Steady", "One constant tempo (midpoint of range)")
    val ACCELERATE = NamedOption("accelerate", "Speed up", "Glides from low to high BPM")
    val DECELERATE = NamedOption("decelerate", "Slow down", "Glides from high to low BPM")
    val RANDOM = NamedOption("random", "Random", "Each note a random tempo in range")
    val ALL = listOf(STEADY, ACCELERATE, DECELERATE, RANDOM)
    val DEFAULT = STEADY
    fun fromId(id: String?): NamedOption = ALL.find { it.id == id } ?: DEFAULT
}

/** Shape of the melody line. */
object MelodicContours {
    val AS_IS = NamedOption("asis", "Natural", "Follows the phone number order")
    val ASCENDING = NamedOption("ascending", "Ascending", "Rises from low to high")
    val DESCENDING = NamedOption("descending", "Descending", "Falls from high to low")
    val ARCH = NamedOption("arch", "Arch", "Rises then falls")
    val RANDOM = NamedOption("random", "Random", "Shuffled note order")
    val ALL = listOf(AS_IS, ASCENDING, DESCENDING, ARCH, RANDOM)
    val DEFAULT = AS_IS
    fun fromId(id: String?): NamedOption = ALL.find { it.id == id } ?: DEFAULT
}

/** Note length / gap. Gate is the fraction of each slot that actually sounds. */
object Articulations {
    val STACCATO = NamedOption("staccato", "Staccato", "Short and detached")
    val NORMAL = NamedOption("normal", "Normal", "Balanced note length")
    val LEGATO = NamedOption("legato", "Legato", "Smooth and connected")
    val ALL = listOf(STACCATO, NORMAL, LEGATO)
    val DEFAULT = NORMAL
    fun fromId(id: String?): NamedOption = ALL.find { it.id == id } ?: DEFAULT
    fun gateForId(id: String?): Double = when (id) {
        STACCATO.id -> 0.5
        LEGATO.id -> 1.0
        else -> 0.9
    }
}

/** Optional second voice played under the melody. */
object Harmonies {
    val NONE = NamedOption("none", "None", "Single melody line")
    val OCTAVE = NamedOption("octave", "Octave", "Adds a note one octave below")
    val THIRD = NamedOption("third", "Third", "Adds a note a third below")
    val FIFTH = NamedOption("fifth", "Fifth", "Adds a note a fifth below")
    val ALL = listOf(NONE, OCTAVE, THIRD, FIFTH)
    val DEFAULT = NONE
    fun fromId(id: String?): NamedOption = ALL.find { it.id == id } ?: DEFAULT
    /** Semitone offset for the harmony voice, or null for no harmony. */
    fun intervalForId(id: String?): Int? = when (id) {
        OCTAVE.id -> -12
        THIRD.id -> -4
        FIFTH.id -> -7
        else -> null
    }
}

/** Register / octave shift applied to the whole melody. */
data class OctaveOption(val shift: Int, val displayName: String)

object Octaves {
    val ALL = listOf(
        OctaveOption(-12, "Low"),
        OctaveOption(0, "Mid"),
        OctaveOption(12, "High")
    )
    const val DEFAULT_SHIFT = 0
    fun nameForShift(shift: Int): String = ALL.find { it.shift == shift }?.displayName ?: "Mid"
}

/** Motif repeat range (how many times the phrase loops). */
object MotifRepeat {
    const val MIN = 1
    const val MAX = 3
    const val DEFAULT = 1
}

/** Factory defaults — the single source of truth for "reset to defaults" and indicators. */
object RingtoneDefaults {
    const val FORMAT = "wav"
    const val INSTRUMENT = 0
    const val LENGTH = 8
    const val STYLE = "major"
    const val ROOT = 60
    const val TEMPO = 150
    const val TEMPO_CONTOUR = "steady"
    const val CONTOUR = "asis"
    const val OCTAVE = 0
    const val REPEAT = 1
    const val ARTICULATION = "normal"
    const val HARMONY = "none"
}
