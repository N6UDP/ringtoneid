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

    fun withProperty(key: String, value: String) = copy(properties = properties + (key to value))
    fun withFormat(fmt: String) = withProperty("format", fmt)
    fun withMidiProgram(program: Int) = withProperty("midiProgram", program.toString())
}
