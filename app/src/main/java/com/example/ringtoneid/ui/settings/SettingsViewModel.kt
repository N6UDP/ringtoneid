package com.example.ringtoneid.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.data.preset.BuiltInPreset
import com.example.ringtoneid.data.preset.BuiltInPresets
import com.example.ringtoneid.data.preset.PresetStore
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.GenerationPreset
import com.example.ringtoneid.domain.model.GenerationSettings
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.RingtoneRepository
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.from
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
    val presets: List<GenerationPreset> = emptyList(),
    /** Id of the preset whose generation controls are expanded for editing, or null. */
    val editingPresetId: String? = null,
    val generateOnLaunch: Boolean = false,
    val backgroundSync: Boolean = false,
    val syncInterval: String = "daily",
    val isSampling: Boolean = false,
    val isLoading: Boolean = true
) {
    val editingPreset: GenerationPreset?
        get() = presets.find { it.id == editingPresetId }
}

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

    private fun loadSettings(): SettingsUiState = SettingsUiState(
        presets = PresetStore.load(prefs),
        generateOnLaunch = prefs.getBoolean("generate_on_launch", false),
        backgroundSync = prefs.getBoolean("background_sync", false),
        syncInterval = prefs.getString("sync_interval", "daily") ?: "daily",
        isSampling = sampling,
        isLoading = false
    )

    // --- Preset pool management ------------------------------------------------

    /** Expand/collapse a preset's editor. Passing the same id again collapses it. */
    fun selectPreset(id: String?) {
        _settings.value = _settings.value.copy(
            editingPresetId = if (_settings.value.editingPresetId == id) null else id
        )
    }

    fun addPreset() {
        val list = _settings.value.presets
        val preset = GenerationPreset(
            name = "Preset ${list.size + 1}",
            settings = GenerationSettings.FACTORY
        )
        persist(list + preset)
        _settings.value = _settings.value.copy(editingPresetId = preset.id)
    }

    fun duplicatePreset(id: String) {
        val source = _settings.value.presets.find { it.id == id } ?: return
        val copy = GenerationPreset(
            name = "${source.name} copy",
            enabled = source.enabled,
            weight = source.weight,
            settings = source.settings
        )
        persist(_settings.value.presets + copy)
        _settings.value = _settings.value.copy(editingPresetId = copy.id)
    }

    /** Add a curated genre starter into the pool and open it for tweaking. */
    fun addBuiltIn(builtIn: BuiltInPreset) {
        val preset = GenerationPreset(name = builtIn.name, settings = builtIn.settings)
        persist(_settings.value.presets + preset)
        _settings.value = _settings.value.copy(editingPresetId = preset.id)
    }

    fun deletePreset(id: String) {
        val newList = _settings.value.presets.filterNot { it.id == id }
        val newEditing = if (_settings.value.editingPresetId == id) null else _settings.value.editingPresetId
        PresetStore.save(prefs, newList)
        _settings.value = _settings.value.copy(presets = newList, editingPresetId = newEditing)
    }

    fun renamePreset(id: String, name: String) = mutatePreset(id) { it.copy(name = name) }

    fun setPresetEnabled(id: String, enabled: Boolean) =
        mutatePreset(id) { it.copy(enabled = enabled) }

    fun setPresetWeight(id: String, weight: Int) =
        mutatePreset(id) { it.copy(weight = weight.coerceIn(1, 10)) }

    /** Reset the currently-edited preset's generation settings back to factory values. */
    fun resetEditingToFactory() = mutateEditing { GenerationSettings.FACTORY }

    // --- Generation-setting editors (operate on the edited preset) -------------

    fun setFormat(format: String) = mutateEditing { it.copy(format = format) }
    fun setInstrument(program: Int) = mutateEditing { it.copy(instrument = program) }
    fun setLength(length: Int) = mutateEditing { it.copy(length = length) }
    fun setStyle(styleId: String) = mutateEditing { it.copy(style = styleId) }
    fun setRoot(rootNote: Int) = mutateEditing { it.copy(root = rootNote) }
    fun setTempoRange(min: Int, max: Int) = mutateEditing { it.copy(tempoMin = min, tempoMax = max) }
    fun setTempoContour(id: String) = mutateEditing { it.copy(tempoContour = id) }
    fun setContour(id: String) = mutateEditing { it.copy(contour = id) }
    fun setOctave(shift: Int) = mutateEditing { it.copy(octave = shift) }
    fun setRepeat(count: Int) = mutateEditing { it.copy(repeat = count) }
    fun setArticulation(id: String) = mutateEditing { it.copy(articulation = id) }
    fun setHarmony(id: String) = mutateEditing { it.copy(harmony = id) }

    private fun mutateEditing(transform: (GenerationSettings) -> GenerationSettings) {
        val id = _settings.value.editingPresetId ?: return
        mutatePreset(id) { it.copy(settings = transform(it.settings)) }
    }

    private fun mutatePreset(id: String, transform: (GenerationPreset) -> GenerationPreset) {
        persist(_settings.value.presets.map { if (it.id == id) transform(it) else it })
    }

    private fun persist(list: List<GenerationPreset>) {
        PresetStore.save(prefs, list)
        _settings.value = _settings.value.copy(presets = list)
    }

    // --- Bulk-generation behaviour toggles -------------------------------------

    fun setGenerateOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean("generate_on_launch", enabled).apply()
        _settings.value = _settings.value.copy(generateOnLaunch = enabled)
    }

    fun setBackgroundSync(enabled: Boolean) {
        prefs.edit().putBoolean("background_sync", enabled).apply()
        _settings.value = _settings.value.copy(backgroundSync = enabled)
        if (enabled) {
            scheduleBackgroundSync()
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(AutoGenerateWorker.WORK_NAME)
        }
    }

    fun setSyncInterval(interval: String) {
        prefs.edit().putString("sync_interval", interval).apply()
        _settings.value = _settings.value.copy(syncInterval = interval)
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

    /** Plays a sample using the edited preset's settings (or the first preset / factory). */
    fun playSample() {
        ringtoneGenerator.stopPreview()
        val seed = (System.currentTimeMillis() % 10000).toInt()
        val settings = sampleSettings()
        val sampleContact = Contact(id = -1L, name = "Sample", phoneNumber = "5551234567")
        val profile = generateRingtoneUseCase.from(sampleContact, settings, seed)
        sampling = true
        _settings.value = _settings.value.copy(isSampling = true)
        ringtoneGenerator.preview(context, profile) {
            sampling = false
            _settings.value = _settings.value.copy(isSampling = false)
        }
    }

    private fun sampleSettings(): GenerationSettings {
        val st = _settings.value
        val preset = st.editingPreset ?: st.presets.firstOrNull()
        return preset?.settings ?: GenerationSettings.FACTORY
    }

    fun stopSample() {
        ringtoneGenerator.stopPreview()
        sampling = false
        _settings.value = _settings.value.copy(isSampling = false)
    }

    override fun onCleared() {
        super.onCleared()
        ringtoneGenerator.stopPreview()
    }
}
