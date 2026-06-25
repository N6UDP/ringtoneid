package com.example.ringtoneid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringtoneid.domain.model.RingtoneProfile
import com.example.ringtoneid.domain.repository.RingtoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val savedRingtones: List<RingtoneProfile> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ringtoneRepository: RingtoneRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = ringtoneRepository.getSavedRingtones()
        .map { SettingsUiState(savedRingtones = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun deleteRingtone(profileId: Long) {
        viewModelScope.launch { ringtoneRepository.deleteRingtone(profileId) }
    }
}
