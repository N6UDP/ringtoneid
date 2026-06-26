package com.example.ringtoneid.domain.usecase

import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.RingtoneProfile
import javax.inject.Inject

class GenerateRingtoneUseCase @Inject constructor(
    private val generator: RingtoneGenerator
) {
    operator fun invoke(
        contact: Contact,
        seed: Int = 0,
        noteCount: Int = 8,
        format: String = "wav",
        midiProgram: Int = 0,
        style: String = "major",
        rootNote: Int = 60,
        tempoMin: Int = 150,
        tempoMax: Int = 150,
        tempoContour: String = "steady",
        contour: String = "asis",
        octave: Int = 0,
        repeat: Int = 1,
        articulation: String = "normal",
        harmony: String = "none"
    ): RingtoneProfile {
        val notes = generator.phoneNumberToNotes(
            contact.phoneNumber, seed, noteCount, style, rootNote, contour, octave, repeat
        )
        val midTempo = (tempoMin + tempoMax) / 2
        return RingtoneProfile(
            contactId = contact.id,
            contactName = contact.name,
            phoneNumber = contact.phoneNumber,
            notes = notes,
            seed = seed,
            noteCount = noteCount,
            properties = mutableMapOf<String, String>().apply {
                put("format", format)
                put("style", style)
                put("tempo", midTempo.toString())
                put("tempoMin", tempoMin.toString())
                put("tempoMax", tempoMax.toString())
                put("tempoContour", tempoContour)
                put("contour", contour)
                put("octave", octave.toString())
                put("repeat", repeat.toString())
                put("articulation", articulation)
                put("harmony", harmony)
                if (midiProgram != 0) put("midiProgram", midiProgram.toString())
                if (rootNote != 60) put("rootNote", rootNote.toString())
            }
        )
    }
}
