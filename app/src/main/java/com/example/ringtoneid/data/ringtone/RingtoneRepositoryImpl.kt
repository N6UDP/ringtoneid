package com.example.ringtoneid.data.ringtone

import android.content.Context
import android.net.Uri
import com.example.ringtoneid.data.db.RingtoneDao
import com.example.ringtoneid.data.db.RingtoneEntity
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.RingtoneRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: RingtoneDao
) : RingtoneRepository {

    override fun getSavedRingtones(): Flow<List<RingtoneProfile>> =
        dao.getAllRingtones().map { list -> list.map { it.toDomain() } }

    override suspend fun getRingtoneForContact(contactId: Long): RingtoneProfile? =
        dao.getRingtoneByContactId(contactId)?.toDomain()

    override suspend fun saveRingtone(profile: RingtoneProfile): Long =
        dao.insertRingtone(profile.toEntity())

    override suspend fun deleteRingtone(profileId: Long) =
        dao.deleteRingtone(profileId)

    override suspend fun deleteAudioFile(audioFilePath: String) {
        try {
            val uri = Uri.parse(audioFilePath)
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
            // File may already be deleted or URI invalid
        }
    }

    private fun RingtoneEntity.toDomain() = RingtoneProfile(
        id = id,
        contactId = contactId,
        contactName = contactName,
        phoneNumber = phoneNumber,
        notes = notes.removePrefix("[").removeSuffix("]")
            .split(",").mapNotNull { it.trim().toIntOrNull() },
        seed = seed,
        noteCount = noteCount,
        audioFilePath = audioFilePath,
        createdAt = createdAt
    )

    private fun RingtoneProfile.toEntity() = RingtoneEntity(
        id = id,
        contactId = contactId,
        contactName = contactName,
        phoneNumber = phoneNumber,
        notes = "[${notes.joinToString(",")}]",
        seed = seed,
        noteCount = noteCount,
        audioFilePath = audioFilePath,
        createdAt = createdAt
    )
}
