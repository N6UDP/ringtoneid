package com.example.ringtoneid.ui.favorites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringtoneid.audio.RingtoneGenerator
import com.example.ringtoneid.data.history.VariationStore
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.model.Variation
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.SetContactRingtoneUseCase
import com.example.ringtoneid.domain.usecase.from
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Variation> = emptyList(),
    val playingId: String? = null,
    val message: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateRingtoneUseCase: GenerateRingtoneUseCase,
    private val setContactRingtoneUseCase: SetContactRingtoneUseCase,
    private val ringtoneGenerator: RingtoneGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(
            favorites = VariationStore.allFavorites(context),
            isLoading = false
        )
    }

    private fun contactOf(v: Variation) =
        Contact(id = v.contactId, name = v.contactName, phoneNumber = v.phoneNumber)

    fun togglePlay(v: Variation) {
        if (_uiState.value.playingId == v.id) {
            ringtoneGenerator.stopPreview()
            _uiState.value = _uiState.value.copy(playingId = null)
        } else {
            ringtoneGenerator.stopPreview()
            val profile = generateRingtoneUseCase.from(contactOf(v), v.settings, v.seed)
            ringtoneGenerator.preview(context, profile)
            _uiState.value = _uiState.value.copy(playingId = v.id)
        }
    }

    fun applyToContact(v: Variation) {
        viewModelScope.launch {
            ringtoneGenerator.stopPreview()
            val profile = generateRingtoneUseCase.from(contactOf(v), v.settings, v.seed)
            val result = setContactRingtoneUseCase(profile)
            _uiState.value = _uiState.value.copy(
                playingId = null,
                message = if (result.isSuccess) "Set ringtone for ${v.contactName}"
                else "Failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
            )
        }
    }

    fun unfavorite(v: Variation) {
        VariationStore.setFavorite(context, v.contactId, v.id, false)
        load()
    }

    fun messageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun stop() {
        ringtoneGenerator.stopPreview()
        _uiState.value = _uiState.value.copy(playingId = null)
    }

    override fun onCleared() {
        super.onCleared()
        ringtoneGenerator.stopPreview()
    }
}
