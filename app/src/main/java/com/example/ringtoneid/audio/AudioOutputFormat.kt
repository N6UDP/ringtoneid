package com.example.ringtoneid.audio

enum class AudioOutputFormat(val extension: String, val mimeType: String, val label: String) {
    WAV("wav", "audio/wav", "WAV"),
    M4A("m4a", "audio/mp4", "M4A (AAC)"),
    MIDI("mid", "audio/midi", "MIDI")
}
