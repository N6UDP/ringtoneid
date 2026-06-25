package com.example.ringtoneid.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.SetContactRingtoneUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AutoGenerateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val contactsRepository: ContactsRepository,
    private val ringtoneRepository: RingtoneRepository,
    private val generateRingtoneUseCase: GenerateRingtoneUseCase,
    private val setContactRingtoneUseCase: SetContactRingtoneUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("ringtone_id_prefs", Context.MODE_PRIVATE)
            val defaultFormat = prefs.getString("default_format", "wav") ?: "wav"
            val defaultInstrument = prefs.getInt("default_instrument", 0)
            val defaultLength = prefs.getInt("default_length", 8)

            val contacts = contactsRepository.getContacts().first()
            val savedIds = ringtoneRepository.getSavedRingtones().first().map { it.contactId }.toSet()

            val newContacts = contacts.filter { it.id !in savedIds && it.phoneNumber.isNotBlank() }

            for (contact in newContacts) {
                try {
                    val profile = generateRingtoneUseCase(
                        contact,
                        format = defaultFormat,
                        midiProgram = defaultInstrument,
                        noteCount = defaultLength
                    )
                    setContactRingtoneUseCase(profile)
                } catch (_: Exception) {}
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "auto_generate_ringtones"
    }
}
