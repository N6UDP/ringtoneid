package com.example.ringtoneid.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.ringtoneid.R
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.SetContactRingtoneUseCase
import com.example.ringtoneid.domain.usecase.fromPresetPool
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
        // Promote to a foreground service so a long bulk run isn't killed mid-way.
        runCatching { setForeground(buildForegroundInfo()) }

        return try {
            val contacts = contactsRepository.getContacts().first()
            val savedIds = ringtoneRepository.getSavedRingtones().first().map { it.contactId }.toSet()

            val newContacts = contacts.filter { it.id !in savedIds && it.phoneNumber.isNotBlank() }

            for (contact in newContacts) {
                try {
                    val profile = generateRingtoneUseCase.fromPresetPool(applicationContext, contact)
                    setContactRingtoneUseCase(profile)
                } catch (_: Exception) {}
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun buildForegroundInfo(): ForegroundInfo {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Ringtone generation",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "Shown while ringtones are generated in the background" }
                )
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Ringtone ID")
            .setContentText("Generating ringtones for new contacts…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val WORK_NAME = "auto_generate_ringtones"
        private const val CHANNEL_ID = "ringtone_generation"
        private const val NOTIFICATION_ID = 4201
    }
}
