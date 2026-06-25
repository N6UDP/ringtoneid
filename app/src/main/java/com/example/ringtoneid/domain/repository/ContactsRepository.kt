package com.example.ringtoneid.domain.repository

import com.example.ringtoneid.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun getContacts(): Flow<List<Contact>>
    suspend fun getContact(id: Long): Contact?
    suspend fun setRingtoneForContact(contactId: Long, ringtoneUri: String)
    suspend fun clearRingtoneForContact(contactId: Long)
}
