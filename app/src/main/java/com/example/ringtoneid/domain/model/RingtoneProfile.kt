package com.example.ringtoneid.domain.model

data class RingtoneProfile(
    val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val notes: List<Int>,     // MIDI note numbers
    val seed: Int = 0,
    val noteCount: Int = 8,
    val audioFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val properties: Map<String, String> = emptyMap()
) {
    val format: String get() = properties["format"] ?: "wav"
    val midiProgram: Int get() = properties["midiProgram"]?.toIntOrNull() ?: 0
    val style: String get() = properties["style"] ?: "major"
    val tempo: Int get() = properties["tempo"]?.toIntOrNull() ?: 150
    val rootNote: Int get() = properties["rootNote"]?.toIntOrNull() ?: 60
    val tempoMin: Int get() = properties["tempoMin"]?.toIntOrNull() ?: tempo
    val tempoMax: Int get() = properties["tempoMax"]?.toIntOrNull() ?: tempo
    val tempoContour: String get() = properties["tempoContour"] ?: "steady"
    val contour: String get() = properties["contour"] ?: "asis"
    val octaveShift: Int get() = properties["octave"]?.toIntOrNull() ?: 0
    val repeatCount: Int get() = properties["repeat"]?.toIntOrNull() ?: 1
    val articulation: String get() = properties["articulation"] ?: "normal"
    val harmony: String get() = properties["harmony"] ?: "none"

    fun withProperty(key: String, value: String) = copy(properties = properties + (key to value))
    fun withFormat(fmt: String) = withProperty("format", fmt)
    fun withMidiProgram(program: Int) = withProperty("midiProgram", program.toString())
    fun withStyle(styleId: String) = withProperty("style", styleId)
    fun withTempo(bpm: Int) = withProperty("tempo", bpm.toString())
    fun withRootNote(root: Int) = withProperty("rootNote", root.toString())
    fun withTempoRange(min: Int, max: Int) =
        copy(properties = properties + mapOf("tempoMin" to min.toString(), "tempoMax" to max.toString()))
    fun withTempoContour(id: String) = withProperty("tempoContour", id)
    fun withContour(id: String) = withProperty("contour", id)
    fun withOctave(shift: Int) = withProperty("octave", shift.toString())
    fun withRepeat(count: Int) = withProperty("repeat", count.toString())
    fun withArticulation(id: String) = withProperty("articulation", id)
    fun withHarmony(id: String) = withProperty("harmony", id)
}
