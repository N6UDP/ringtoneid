package com.example.ringtoneid.ui.contacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.usecase.GenerateRingtoneUseCase
import com.example.ringtoneid.domain.usecase.fromPresetPool
import com.example.ringtoneid.domain.usecase.GetContactsUseCase
import com.example.ringtoneid.domain.usecase.SetContactRingtoneUseCase
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ContactListUiState {
    object Loading : ContactListUiState
    data class Success(val contacts: List<Contact>) : ContactListUiState
    data class Error(val message: String) : ContactListUiState
}

sealed interface BulkActionState {
    object Idle : BulkActionState
    data class InProgress(val progress: Float, val message: String) : BulkActionState
}

@HiltViewModel
class ContactListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getContactsUseCase: GetContactsUseCase,
    private val generateRingtoneUseCase: GenerateRingtoneUseCase,
    private val setContactRingtoneUseCase: SetContactRingtoneUseCase,
    private val ringtoneRepository: RingtoneRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactListUiState>(ContactListUiState.Loading)
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    private val _bulkActionState = MutableStateFlow<BulkActionState>(BulkActionState.Idle)
    val bulkActionState: StateFlow<BulkActionState> = _bulkActionState.asStateFlow()

    init {
        loadContacts()
        checkAutoGenerate()
    }

    private fun checkAutoGenerate() {
        val prefs = context.getSharedPreferences("ringtone_id_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("generate_on_launch", false)) return

        viewModelScope.launch {
            try {
                val contacts = getContactsUseCase().first()
                val contactsWithout = contacts.filter { !it.hasCustomRingtone && it.phoneNumber.isNotBlank() }
                if (contactsWithout.isEmpty()) return@launch

                val total = contactsWithout.size
                contactsWithout.forEachIndexed { index, contact ->
                    _bulkActionState.value = BulkActionState.InProgress(
                        progress = index.toFloat() / total,
                        message = "Auto-generating: ${contact.name} (${index + 1}/$total)"
                    )
                    try {
                        val profile = generateRingtoneUseCase.fromPresetPool(context, contact)
                        setContactRingtoneUseCase(profile)
                    } catch (_: Exception) {}
                }
                _bulkActionState.value = BulkActionState.Idle
                loadContacts()
            } catch (_: Exception) {}
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = ContactListUiState.Loading
            try {
                val contacts = getContactsUseCase().first()
                _uiState.value = ContactListUiState.Success(contacts)
            } catch (e: Exception) {
                _uiState.value = ContactListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun generateAllRingtones() {
        val state = _uiState.value as? ContactListUiState.Success ?: return
        val contactsWithout = state.contacts.filter { !it.hasCustomRingtone }
        if (contactsWithout.isEmpty()) return

        viewModelScope.launch {
            val total = contactsWithout.size
            contactsWithout.forEachIndexed { index, contact ->
                _bulkActionState.value = BulkActionState.InProgress(
                    progress = index.toFloat() / total,
                    message = "Setting ringtone for ${contact.name} (${index + 1}/$total)"
                )
                try {
                    val profile = generateRingtoneUseCase.fromPresetPool(context, contact)
                    setContactRingtoneUseCase(profile)
                } catch (_: Exception) {
                    // Skip failures on individual contacts
                }
            }
            _bulkActionState.value = BulkActionState.Idle
            loadContacts()
        }
    }

    fun removeAllRingtones() {
        viewModelScope.launch {
            _bulkActionState.value = BulkActionState.InProgress(
                progress = -1f,
                message = "Removing all ringtones..."
            )
            try {
                val ringtones = ringtoneRepository.getSavedRingtones().first()
                val total = ringtones.size
                ringtones.forEachIndexed { index, profile ->
                    _bulkActionState.value = BulkActionState.InProgress(
                        progress = (index.toFloat() / total).coerceAtLeast(0f),
                        message = "Removing ${profile.contactName} (${index + 1}/$total)"
                    )
                    // Delete the audio file from MediaStore
                    profile.audioFilePath?.let { ringtoneRepository.deleteAudioFile(it) }
                    // Clear the contact's custom ringtone assignment
                    contactsRepository.clearRingtoneForContact(profile.contactId)
                    // Delete the Room record
                    ringtoneRepository.deleteRingtone(profile.id)
                }
            } catch (_: Exception) {}
            _bulkActionState.value = BulkActionState.Idle
            loadContacts()
        }
    }
}
