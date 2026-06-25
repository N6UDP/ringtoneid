package com.example.ringtoneid.audio

data class MidiInstrument(val program: Int, val name: String, val category: String)

object MidiInstruments {
    val instruments = listOf(
        // Piano
        MidiInstrument(0, "Acoustic Grand Piano", "Piano"),
        MidiInstrument(1, "Bright Piano", "Piano"),
        MidiInstrument(4, "Electric Piano", "Piano"),
        MidiInstrument(6, "Harpsichord", "Piano"),
        MidiInstrument(8, "Celesta", "Piano"),
        // Chromatic Percussion
        MidiInstrument(9, "Glockenspiel", "Chromatic"),
        MidiInstrument(10, "Music Box", "Chromatic"),
        MidiInstrument(11, "Vibraphone", "Chromatic"),
        MidiInstrument(13, "Xylophone", "Chromatic"),
        MidiInstrument(14, "Tubular Bells", "Chromatic"),
        // Organ
        MidiInstrument(16, "Drawbar Organ", "Organ"),
        MidiInstrument(19, "Church Organ", "Organ"),
        // Guitar
        MidiInstrument(24, "Nylon Guitar", "Guitar"),
        MidiInstrument(25, "Steel Guitar", "Guitar"),
        MidiInstrument(26, "Jazz Guitar", "Guitar"),
        MidiInstrument(30, "Distortion Guitar", "Guitar"),
        // Bass
        MidiInstrument(32, "Acoustic Bass", "Bass"),
        MidiInstrument(33, "Electric Bass (finger)", "Bass"),
        MidiInstrument(36, "Slap Bass", "Bass"),
        // Strings
        MidiInstrument(40, "Violin", "Strings"),
        MidiInstrument(42, "Cello", "Strings"),
        MidiInstrument(48, "String Ensemble", "Strings"),
        // Brass
        MidiInstrument(56, "Trumpet", "Brass"),
        MidiInstrument(57, "Trombone", "Brass"),
        MidiInstrument(61, "Brass Section", "Brass"),
        // Reed / Woodwind
        MidiInstrument(64, "Soprano Sax", "Reed"),
        MidiInstrument(65, "Alto Sax", "Reed"),
        MidiInstrument(73, "Flute", "Pipe"),
        MidiInstrument(71, "Clarinet", "Reed"),
        // Synth
        MidiInstrument(80, "Square Lead", "Synth"),
        MidiInstrument(81, "Sawtooth Lead", "Synth"),
        MidiInstrument(88, "New Age Pad", "Synth"),
        MidiInstrument(94, "Halo Pad", "Synth"),
    )

    fun findByProgram(program: Int): MidiInstrument =
        instruments.firstOrNull { it.program == program } ?: instruments[0]
}
