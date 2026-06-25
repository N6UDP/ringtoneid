package com.example.ringtoneid.domain.usecase

import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val ringtoneRepository: RingtoneRepository
) {
    operator fun invoke(): Flow<List<Contact>> =
        combine(
            contactsRepository.getContacts(),
            ringtoneRepository.getSavedRingtones()
        ) { contacts, profiles ->
            val profiledIds = profiles.map { it.contactId }.toSet()
            contacts.map { it.copy(hasCustomRingtone = it.id in profiledIds) }
        }
}
