package com.example.ringtoneid.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.RingtoneRepository
import com.example.ringtoneid.worker.AutoGenerateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val savedRingtones: List<RingtoneProfile> = emptyList(),
    val defaultFormat: String = "wav",
    val defaultInstrument: Int = 0,
    val defaultLength: Int = 8,
    val generateOnLaunch: Boolean = false,
    val backgroundSync: Boolean = false,
    val syncInterval: String = "daily",
    val isSampling: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
    private val ringtoneGenerator: RingtoneGenerator
) : ViewModel() {

    private val prefs = context.getSharedPreferences("ringtone_id_prefs", Context.MODE_PRIVATE)

    private val _defaultFormat = MutableStateFlow(prefs.getString("default_format", "wav") ?: "wav")
    private val _defaultInstrument = MutableStateFlow(prefs.getInt("default_instrument", 0))
    private val _defaultLength = MutableStateFlow(prefs.getInt("default_length", 8))
    private val _generateOnLaunch = MutableStateFlow(prefs.getBoolean("generate_on_launch", false))
    private val _backgroundSync = MutableStateFlow(prefs.getBoolean("background_sync", false))
    private val _syncInterval = MutableStateFlow(prefs.getString("sync_interval", "daily") ?: "daily")
    private val _isSampling = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        ringtoneRepository.getSavedRingtones(),
        _defaultFormat,
        combine(_defaultInstrument, _defaultLength, _isSampling) { i, l, s -> Triple(i, l, s) },
        combine(_generateOnLaunch, _backgroundSync, _syncInterval) { g, b, s -> Triple(g, b, s) }
    ) { ringtones, format, (instrument, length, sampling), (genLaunch, bgSync, interval) ->
        SettingsUiState(
            savedRingtones = ringtones,
            defaultFormat = format,
            defaultInstrument = instrument,
            defaultLength = length,
            generateOnLaunch = genLaunch,
            backgroundSync = bgSync,
            syncInterval = interval,
            isSampling = sampling,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDefaultFormat(format: String) {
        prefs.edit().putString("default_format", format).apply()
        _defaultFormat.value = format
    }

    fun setDefaultInstrument(program: Int) {
        prefs.edit().putInt("default_instrument", program).apply()
        _defaultInstrument.value = program
    }

    fun setDefaultLength(length: Int) {
        prefs.edit().putInt("default_length", length).apply()
        _defaultLength.value = length
    }

    fun setGenerateOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean("generate_on_launch", enabled).apply()
        _generateOnLaunch.value = enabled
    }

    fun setBackgroundSync(enabled: Boolean) {
        prefs.edit().putBoolean("background_sync", enabled).apply()
        _backgroundSync.value = enabled
        if (enabled) {
            scheduleBackgroundSync()
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(AutoGenerateWorker.WORK_NAME)
        }
    }

    fun setSyncInterval(interval: String) {
        prefs.edit().putString("sync_interval", interval).apply()
        _syncInterval.value = interval
        if (_backgroundSync.value) {
            scheduleBackgroundSync()
        }
    }

    private fun scheduleBackgroundSync() {
        val interval = _syncInterval.value
        val (amount, unit) = when (interval) {
            "weekly" -> 7L to TimeUnit.DAYS
            else -> 1L to TimeUnit.DAYS
        }
        val request = PeriodicWorkRequestBuilder<AutoGenerateWorker>(amount, unit)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AutoGenerateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun deleteRingtone(profileId: Long) {
        viewModelScope.launch { ringtoneRepository.deleteRingtone(profileId) }
    }

    fun playSample() {
        ringtoneGenerator.stopPreview()
        val seed = (System.currentTimeMillis() % 10000).toInt()
        val fakePhone = "5551234567"
        val notes = ringtoneGenerator.phoneNumberToNotes(fakePhone, seed, _defaultLength.value)
        val format = _defaultFormat.value
        val instrument = _defaultInstrument.value
        _isSampling.value = true
        if (format.equals("midi", ignoreCase = true)) {
            ringtoneGenerator.previewMidi(context, notes, instrument)
        } else {
            ringtoneGenerator.previewPlay(notes, instrument)
        }
    }

    fun stopSample() {
        ringtoneGenerator.stopPreview()
        _isSampling.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ringtoneGenerator.stopPreview()
    }
}
