package com.example.ringtoneid.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringtoneid.audio.AudioOutputFormat
import com.example.ringtoneid.audio.Articulations
import com.example.ringtoneid.audio.Harmonies
import com.example.ringtoneid.audio.MelodicContours
import com.example.ringtoneid.audio.MidiInstruments
import com.example.ringtoneid.audio.MotifRepeat
import com.example.ringtoneid.audio.MusicalKeys
import com.example.ringtoneid.audio.MusicalStyles
import com.example.ringtoneid.audio.Octaves
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.audio.Tempo
import com.example.ringtoneid.audio.TempoContours
import com.example.ringtoneid.data.history.VariationStore
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.GenerationSettings
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.model.Variation
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.SetContactRingtoneUseCase
import com.example.ringtoneid.domain.usecase.from
import com.example.ringtoneid.domain.usecase.fromPresetPool
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Ready(
        val contact: Contact,
        val profile: RingtoneProfile,
        val history: List<Variation> = emptyList(),
        val isPlaying: Boolean = false,
        val isSaving: Boolean = false,
        val savedSuccess: Boolean = false,
        val error: String? = null
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

@HiltViewModel
class RingtoneDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val ringtoneRepository: RingtoneRepository,
    private val generateRingtoneUseCase: GenerateRingtoneUseCase,
    private val setContactRingtoneUseCase: SetContactRingtoneUseCase,
    private val ringtoneGenerator: RingtoneGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /** Initial generation for a contact with no saved ringtone uses the preset pool. */
    private fun generateFromDefaults(contact: Contact): RingtoneProfile =
        generateRingtoneUseCase.fromPresetPool(context, contact)

    private fun regen(
        contact: Contact,
        p: RingtoneProfile,
        seed: Int = p.seed,
        noteCount: Int = p.noteCount,
        format: String = p.format,
        midiProgram: Int = p.midiProgram,
        style: String = p.style,
        rootNote: Int = p.rootNote,
        tempoMin: Int = p.tempoMin,
        tempoMax: Int = p.tempoMax,
        tempoContour: String = p.tempoContour,
        contour: String = p.contour,
        octave: Int = p.octaveShift,
        repeat: Int = p.repeatCount,
        articulation: String = p.articulation,
        harmony: String = p.harmony
    ): RingtoneProfile = generateRingtoneUseCase(
        contact, seed, noteCount, format, midiProgram, style, rootNote,
        tempoMin, tempoMax, tempoContour, contour, octave, repeat, articulation, harmony
    )

    private fun apply(newProfile: RingtoneProfile) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        _uiState.value = state.copy(profile = newProfile, isPlaying = false, savedSuccess = false, error = null)
    }

    fun loadContact(contactId: Long) {
        viewModelScope.launch {
            val contact = contactsRepository.getContact(contactId)
            if (contact == null) {
                _uiState.value = DetailUiState.Error("Contact not found")
                return@launch
            }
            val existingProfile = ringtoneRepository.getRingtoneForContact(contactId)
            val profile = existingProfile ?: generateFromDefaults(contact)
            _uiState.value = DetailUiState.Ready(
                contact = contact,
                profile = profile,
                history = VariationStore.historyFor(context, contactId)
            )
        }
    }

    /** Snapshots the current profile into this contact's history (deduped + capped). */
    private fun recordCurrent(state: DetailUiState.Ready, favorite: Boolean = false) {
        val variation = Variation(
            contactId = state.contact.id,
            contactName = state.contact.name,
            phoneNumber = state.contact.phoneNumber,
            seed = state.profile.seed,
            settings = GenerationSettings.fromProfile(state.profile),
            favorite = favorite
        )
        VariationStore.record(context, variation)
        _uiState.value = state.copy(history = VariationStore.historyFor(context, state.contact.id))
    }

    /** Restore a saved variation as the current profile. */
    fun restoreVariation(variation: Variation) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(generateRingtoneUseCase.from(state.contact, variation.settings, variation.seed))
    }

    fun toggleFavorite(variation: Variation) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        VariationStore.setFavorite(context, state.contact.id, variation.id, !variation.favorite)
        _uiState.value = state.copy(history = VariationStore.historyFor(context, state.contact.id))
    }

    fun deleteVariation(variation: Variation) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        VariationStore.delete(context, state.contact.id, variation.id)
        _uiState.value = state.copy(history = VariationStore.historyFor(context, state.contact.id))
    }

    /** Save the current variation explicitly as a favorite. */
    fun favoriteCurrent() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        recordCurrent(state, favorite = true)
    }

    fun shuffleSeed() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(regen(state.contact, state.profile, seed = state.profile.seed + 1))
    }

    fun prevSeed() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        if (state.profile.seed <= 0) return
        apply(regen(state.contact, state.profile, seed = state.profile.seed - 1))
    }

    fun updateNoteCount(count: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(regen(state.contact, state.profile, noteCount = count))
    }

    fun updateStyle(styleId: String) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(regen(state.contact, state.profile, style = styleId))
    }

    fun updateRootNote(rootNote: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(regen(state.contact, state.profile, rootNote = rootNote))
    }

    fun updateContour(contourId: String) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(regen(state.contact, state.profile, contour = contourId))
    }

    fun updateOctave(shift: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(regen(state.contact, state.profile, octave = shift))
    }

    fun updateRepeat(count: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(regen(state.contact, state.profile, repeat = count))
    }

    fun updateTempoRange(min: Int, max: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        val updated = state.profile.withTempoRange(min, max).withTempo((min + max) / 2)
        _uiState.value = state.copy(profile = updated, isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateTempoContour(contourId: String) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        _uiState.value = state.copy(profile = state.profile.withTempoContour(contourId), isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateArticulation(id: String) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        _uiState.value = state.copy(profile = state.profile.withArticulation(id), isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateHarmony(id: String) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        _uiState.value = state.copy(profile = state.profile.withHarmony(id), isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateFormat(format: String) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        _uiState.value = state.copy(profile = state.profile.withFormat(format), isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateInstrument(midiProgram: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        _uiState.value = state.copy(profile = state.profile.withMidiProgram(midiProgram), isPlaying = false, savedSuccess = false, error = null)
    }

    fun resetToDefaults() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        apply(generateRingtoneUseCase.from(state.contact, GenerationSettings.FACTORY, seed = 0))
    }

    fun surprise() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        val rnd = java.util.Random()
        val tA = Tempo.MIN_BPM + rnd.nextInt(Tempo.MAX_BPM - Tempo.MIN_BPM + 1)
        val tB = Tempo.MIN_BPM + rnd.nextInt(Tempo.MAX_BPM - Tempo.MIN_BPM + 1)
        apply(
            regen(
                state.contact, state.profile,
                seed = rnd.nextInt(8),
                style = MusicalStyles.ALL[rnd.nextInt(MusicalStyles.ALL.size)].id,
                rootNote = MusicalKeys.ALL[rnd.nextInt(MusicalKeys.ALL.size)].rootNote,
                midiProgram = MidiInstruments.instruments[rnd.nextInt(MidiInstruments.instruments.size)].program,
                tempoMin = minOf(tA, tB),
                tempoMax = maxOf(tA, tB),
                tempoContour = TempoContours.ALL[rnd.nextInt(TempoContours.ALL.size)].id,
                contour = MelodicContours.ALL[rnd.nextInt(MelodicContours.ALL.size)].id,
                octave = Octaves.ALL[rnd.nextInt(Octaves.ALL.size)].shift,
                repeat = MotifRepeat.MIN + rnd.nextInt(MotifRepeat.MAX - MotifRepeat.MIN + 1),
                articulation = Articulations.ALL[rnd.nextInt(Articulations.ALL.size)].id,
                harmony = Harmonies.ALL[rnd.nextInt(Harmonies.ALL.size)].id
            )
        )
    }

    fun togglePreview() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        if (state.isPlaying) {
            ringtoneGenerator.stopPreview()
            _uiState.value = state.copy(isPlaying = false)
        } else {
            ringtoneGenerator.preview(context, state.profile)
            _uiState.value = state.copy(isPlaying = true)
            // Snapshot the tune the user is auditioning so they can return to it later.
            recordCurrent(_uiState.value as DetailUiState.Ready)
        }
    }

    fun setAsRingtone() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            ringtoneGenerator.stopPreview()
            val result = setContactRingtoneUseCase(state.profile)
            _uiState.value = if (result.isSuccess) {
                recordCurrent(state)
                val refreshed = _uiState.value as? DetailUiState.Ready ?: state
                refreshed.copy(isSaving = false, isPlaying = false, savedSuccess = true)
            } else {
                state.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to set ringtone")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ringtoneGenerator.stopPreview()
    }
}
