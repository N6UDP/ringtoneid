package com.example.ringtoneid.domain.usecase

import android.content.Context
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SetContactRingtoneUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val ringtoneRepository: RingtoneRepository,
    private val generator: RingtoneGenerator
) {
    suspend operator fun invoke(profile: RingtoneProfile): Result<Unit> = runCatching {
        val uri = generator.generateAndSave(context, profile)
        val savedProfile = profile.copy(audioFilePath = uri.toString())
        val id = ringtoneRepository.saveRingtone(savedProfile)
        contactsRepository.setRingtoneForContact(profile.contactId, uri.toString())
    }
}
