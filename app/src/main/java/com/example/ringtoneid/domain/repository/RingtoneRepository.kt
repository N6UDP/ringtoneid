package com.example.ringtoneid.domain.repository

import com.example.ringtoneid.domain.model.RingtoneProfile
import kotlinx.coroutines.flow.Flow

interface RingtoneRepository {
    fun getSavedRingtones(): Flow<List<RingtoneProfile>>
    suspend fun getRingtoneForContact(contactId: Long): RingtoneProfile?
    suspend fun saveRingtone(profile: RingtoneProfile): Long
    suspend fun deleteRingtone(profileId: Long)
    suspend fun deleteAudioFile(audioFilePath: String)
}
