package com.example.ringtoneid.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ringtoneid.audio.RingtoneDefaults
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.RingtoneRepository
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.fromDefaults
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
    val defaultStyle: String = "major",
    val defaultRoot: Int = 60,
    val defaultTempoMin: Int = 150,
    val defaultTempoMax: Int = 150,
    val defaultTempoContour: String = "steady",
    val defaultContour: String = "asis",
    val defaultOctave: Int = 0,
    val defaultRepeat: Int = 1,
    val defaultArticulation: String = "normal",
    val defaultHarmony: String = "none",
    val generateOnLaunch: Boolean = false,
    val backgroundSync: Boolean = false,
    val syncInterval: String = "daily",
    val isSampling: Boolean = false,
    val isLoading: Boolean = true,
    val isAtDefaults: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
    private val generateRingtoneUseCase: GenerateRingtoneUseCase,
    private val ringtoneGenerator: RingtoneGenerator
) : ViewModel() {

    private val prefs = context.getSharedPreferences("ringtone_id_prefs", Context.MODE_PRIVATE)
    private var sampling = false

    private val _settings = MutableStateFlow(loadSettings())

    val uiState: StateFlow<SettingsUiState> = combine(
        ringtoneRepository.getSavedRingtones(),
        _settings
    ) { ringtones, settings ->
        settings.copy(savedRingtones = ringtones, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private fun loadSettings(): SettingsUiState {
        val legacyTempo = prefs.getInt("default_tempo", RingtoneDefaults.TEMPO)
        return SettingsUiState(
            defaultFormat = prefs.getString("default_format", RingtoneDefaults.FORMAT) ?: RingtoneDefaults.FORMAT,
            defaultInstrument = prefs.getInt("default_instrument", RingtoneDefaults.INSTRUMENT),
            defaultLength = prefs.getInt("default_length", RingtoneDefaults.LENGTH),
            defaultStyle = prefs.getString("default_style", RingtoneDefaults.STYLE) ?: RingtoneDefaults.STYLE,
            defaultRoot = prefs.getInt("default_root", RingtoneDefaults.ROOT),
            defaultTempoMin = prefs.getInt("default_tempo_min", legacyTempo),
            defaultTempoMax = prefs.getInt("default_tempo_max", legacyTempo),
            defaultTempoContour = prefs.getString("default_tempo_contour", RingtoneDefaults.TEMPO_CONTOUR) ?: RingtoneDefaults.TEMPO_CONTOUR,
            defaultContour = prefs.getString("default_contour", RingtoneDefaults.CONTOUR) ?: RingtoneDefaults.CONTOUR,
            defaultOctave = prefs.getInt("default_octave", RingtoneDefaults.OCTAVE),
            defaultRepeat = prefs.getInt("default_repeat", RingtoneDefaults.REPEAT),
            defaultArticulation = prefs.getString("default_articulation", RingtoneDefaults.ARTICULATION) ?: RingtoneDefaults.ARTICULATION,
            defaultHarmony = prefs.getString("default_harmony", RingtoneDefaults.HARMONY) ?: RingtoneDefaults.HARMONY,
            generateOnLaunch = prefs.getBoolean("generate_on_launch", false),
            backgroundSync = prefs.getBoolean("background_sync", false),
            syncInterval = prefs.getString("sync_interval", "daily") ?: "daily",
            isSampling = sampling,
            isLoading = false
        ).withDefaultsFlag()
    }

    private fun SettingsUiState.withDefaultsFlag(): SettingsUiState = copy(
        isAtDefaults = defaultFormat == RingtoneDefaults.FORMAT &&
            defaultInstrument == RingtoneDefaults.INSTRUMENT &&
            defaultLength == RingtoneDefaults.LENGTH &&
            defaultStyle == RingtoneDefaults.STYLE &&
            defaultRoot == RingtoneDefaults.ROOT &&
            defaultTempoMin == RingtoneDefaults.TEMPO &&
            defaultTempoMax == RingtoneDefaults.TEMPO &&
            defaultTempoContour == RingtoneDefaults.TEMPO_CONTOUR &&
            defaultContour == RingtoneDefaults.CONTOUR &&
            defaultOctave == RingtoneDefaults.OCTAVE &&
            defaultRepeat == RingtoneDefaults.REPEAT &&
            defaultArticulation == RingtoneDefaults.ARTICULATION &&
            defaultHarmony == RingtoneDefaults.HARMONY
    )

    private fun update(transform: (SettingsUiState) -> SettingsUiState) {
        _settings.value = transform(_settings.value).withDefaultsFlag()
    }

    fun setDefaultFormat(format: String) {
        prefs.edit().putString("default_format", format).apply()
        update { it.copy(defaultFormat = format) }
    }

    fun setDefaultInstrument(program: Int) {
        prefs.edit().putInt("default_instrument", program).apply()
        update { it.copy(defaultInstrument = program) }
    }

    fun setDefaultLength(length: Int) {
        prefs.edit().putInt("default_length", length).apply()
        update { it.copy(defaultLength = length) }
    }

    fun setDefaultStyle(styleId: String) {
        prefs.edit().putString("default_style", styleId).apply()
        update { it.copy(defaultStyle = styleId) }
    }

    fun setDefaultRoot(rootNote: Int) {
        prefs.edit().putInt("default_root", rootNote).apply()
        update { it.copy(defaultRoot = rootNote) }
    }

    fun setDefaultTempoRange(min: Int, max: Int) {
        prefs.edit().putInt("default_tempo_min", min).putInt("default_tempo_max", max)
            .putInt("default_tempo", (min + max) / 2).apply()
        update { it.copy(defaultTempoMin = min, defaultTempoMax = max) }
    }

    fun setDefaultTempoContour(id: String) {
        prefs.edit().putString("default_tempo_contour", id).apply()
        update { it.copy(defaultTempoContour = id) }
    }

    fun setDefaultContour(id: String) {
        prefs.edit().putString("default_contour", id).apply()
        update { it.copy(defaultContour = id) }
    }

    fun setDefaultOctave(shift: Int) {
        prefs.edit().putInt("default_octave", shift).apply()
        update { it.copy(defaultOctave = shift) }
    }

    fun setDefaultRepeat(count: Int) {
        prefs.edit().putInt("default_repeat", count).apply()
        update { it.copy(defaultRepeat = count) }
    }

    fun setDefaultArticulation(id: String) {
        prefs.edit().putString("default_articulation", id).apply()
        update { it.copy(defaultArticulation = id) }
    }

    fun setDefaultHarmony(id: String) {
        prefs.edit().putString("default_harmony", id).apply()
        update { it.copy(defaultHarmony = id) }
    }

    fun resetToDefaults() {
        prefs.edit()
            .putString("default_format", RingtoneDefaults.FORMAT)
            .putInt("default_instrument", RingtoneDefaults.INSTRUMENT)
            .putInt("default_length", RingtoneDefaults.LENGTH)
            .putString("default_style", RingtoneDefaults.STYLE)
            .putInt("default_root", RingtoneDefaults.ROOT)
            .putInt("default_tempo_min", RingtoneDefaults.TEMPO)
            .putInt("default_tempo_max", RingtoneDefaults.TEMPO)
            .putInt("default_tempo", RingtoneDefaults.TEMPO)
            .putString("default_tempo_contour", RingtoneDefaults.TEMPO_CONTOUR)
            .putString("default_contour", RingtoneDefaults.CONTOUR)
            .putInt("default_octave", RingtoneDefaults.OCTAVE)
            .putInt("default_repeat", RingtoneDefaults.REPEAT)
            .putString("default_articulation", RingtoneDefaults.ARTICULATION)
            .putString("default_harmony", RingtoneDefaults.HARMONY)
            .apply()
        update {
            it.copy(
                defaultFormat = RingtoneDefaults.FORMAT,
                defaultInstrument = RingtoneDefaults.INSTRUMENT,
                defaultLength = RingtoneDefaults.LENGTH,
                defaultStyle = RingtoneDefaults.STYLE,
                defaultRoot = RingtoneDefaults.ROOT,
                defaultTempoMin = RingtoneDefaults.TEMPO,
                defaultTempoMax = RingtoneDefaults.TEMPO,
                defaultTempoContour = RingtoneDefaults.TEMPO_CONTOUR,
                defaultContour = RingtoneDefaults.CONTOUR,
                defaultOctave = RingtoneDefaults.OCTAVE,
                defaultRepeat = RingtoneDefaults.REPEAT,
                defaultArticulation = RingtoneDefaults.ARTICULATION,
                defaultHarmony = RingtoneDefaults.HARMONY
            )
        }
    }

    fun setGenerateOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean("generate_on_launch", enabled).apply()
        update { it.copy(generateOnLaunch = enabled) }
    }

    fun setBackgroundSync(enabled: Boolean) {
        prefs.edit().putBoolean("background_sync", enabled).apply()
        update { it.copy(backgroundSync = enabled) }
        if (enabled) {
            scheduleBackgroundSync()
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(AutoGenerateWorker.WORK_NAME)
        }
    }

    fun setSyncInterval(interval: String) {
        prefs.edit().putString("sync_interval", interval).apply()
        update { it.copy(syncInterval = interval) }
        if (_settings.value.backgroundSync) {
            scheduleBackgroundSync()
        }
    }

    private fun scheduleBackgroundSync() {
        val (amount, unit) = when (_settings.value.syncInterval) {
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
        val sampleContact = Contact(id = -1L, name = "Sample", phoneNumber = "5551234567")
        val profile = generateRingtoneUseCase.fromDefaults(context, sampleContact, seed)
        sampling = true
        update { it.copy(isSampling = true) }
        ringtoneGenerator.preview(context, profile)
    }

    fun stopSample() {
        ringtoneGenerator.stopPreview()
        sampling = false
        update { it.copy(isSampling = false) }
    }

    override fun onCleared() {
        super.onCleared()
        ringtoneGenerator.stopPreview()
    }
}
