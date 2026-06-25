package com.example.ringtoneid.domain.usecase

import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.RingtoneProfile
import javax.inject.Inject

class GenerateRingtoneUseCase @Inject constructor(
    private val generator: RingtoneGenerator
) {
    operator fun invoke(contact: Contact, seed: Int = 0, noteCount: Int = 8): RingtoneProfile {
        val notes = generator.phoneNumberToNotes(contact.phoneNumber, seed, noteCount)
        return RingtoneProfile(
            contactId = contact.id,
            contactName = contact.name,
            phoneNumber = contact.phoneNumber,
            notes = notes,
            seed = seed,
            noteCount = noteCount
        )
    }
}
