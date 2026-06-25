package com.example.ringtoneid.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringtoneid.audio.AudioOutputFormat
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.SetContactRingtoneUseCase
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

    fun loadContact(contactId: Long) {
        viewModelScope.launch {
            val contact = contactsRepository.getContact(contactId)
            if (contact == null) {
                _uiState.value = DetailUiState.Error("Contact not found")
                return@launch
            }
            val existingProfile = ringtoneRepository.getRingtoneForContact(contactId)
            val profile = existingProfile ?: run {
                val prefs = context.getSharedPreferences("ringtone_id_prefs", Context.MODE_PRIVATE)
                val defaultFormat = prefs.getString("default_format", "wav") ?: "wav"
                val defaultInstrument = prefs.getInt("default_instrument", 0)
                val defaultLength = prefs.getInt("default_length", 8)
                generateRingtoneUseCase(contact, format = defaultFormat, midiProgram = defaultInstrument, noteCount = defaultLength)
            }
            _uiState.value = DetailUiState.Ready(contact = contact, profile = profile)
        }
    }

    fun shuffleSeed() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        val p = state.profile
        val newProfile = generateRingtoneUseCase(state.contact, p.seed + 1, p.noteCount, p.format, p.midiProgram)
        _uiState.value = state.copy(profile = newProfile, isPlaying = false, savedSuccess = false, error = null)
    }

    fun prevSeed() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        if (state.profile.seed <= 0) return
        ringtoneGenerator.stopPreview()
        val p = state.profile
        val newProfile = generateRingtoneUseCase(state.contact, p.seed - 1, p.noteCount, p.format, p.midiProgram)
        _uiState.value = state.copy(profile = newProfile, isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateNoteCount(count: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        val p = state.profile
        val newProfile = generateRingtoneUseCase(state.contact, p.seed, count, p.format, p.midiProgram)
        _uiState.value = state.copy(profile = newProfile, isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateFormat(format: String) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        val newProfile = state.profile.withFormat(format)
        _uiState.value = state.copy(profile = newProfile, isPlaying = false, savedSuccess = false, error = null)
    }

    fun updateInstrument(midiProgram: Int) {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        ringtoneGenerator.stopPreview()
        val newProfile = state.profile.withMidiProgram(midiProgram)
        _uiState.value = state.copy(profile = newProfile, isPlaying = false, savedSuccess = false, error = null)
    }

    fun togglePreview() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        if (state.isPlaying) {
            ringtoneGenerator.stopPreview()
            _uiState.value = state.copy(isPlaying = false)
        } else {
            val p = state.profile
            val isMidi = p.format.equals("midi", ignoreCase = true)
            if (isMidi) {
                ringtoneGenerator.previewMidi(context, p.notes, p.midiProgram)
            } else {
                ringtoneGenerator.previewPlay(p.notes, p.midiProgram)
            }
            _uiState.value = state.copy(isPlaying = true)
        }
    }

    fun setAsRingtone() {
        val state = _uiState.value as? DetailUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            ringtoneGenerator.stopPreview()
            val result = setContactRingtoneUseCase(state.profile)
            _uiState.value = if (result.isSuccess) {
                state.copy(isSaving = false, isPlaying = false, savedSuccess = true)
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
