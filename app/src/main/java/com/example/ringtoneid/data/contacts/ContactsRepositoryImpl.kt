package com.example.ringtoneid.data.contacts

import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import com.example.ringtoneid.domain.model.Contact
import com.example.ringtoneid.domain.repository.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactsRepository {

    override fun getContacts(): Flow<List<Contact>> = flow {
        emit(queryContacts())
    }

    override suspend fun getContact(id: Long): Contact? =
        queryContacts().firstOrNull { it.id == id }

    override suspend fun setRingtoneForContact(contactId: Long, ringtoneUri: String) {
        val values = ContentValues().apply {
            put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri)
        }
        context.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString())
        )
    }

    override suspend fun clearRingtoneForContact(contactId: Long) {
        val values = ContentValues().apply {
            putNull(ContactsContract.Contacts.CUSTOM_RINGTONE)
        }
        context.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString())
        )
    }

    private fun queryContacts(): List<Contact> {
        val contacts = mutableMapOf<Long, Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        ) ?: return emptyList()

        cursor.use {
            val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (it.moveToNext()) {
                val id = it.getLong(idIdx)
                if (!contacts.containsKey(id)) {
                    contacts[id] = Contact(
                        id = id,
                        name = it.getString(nameIdx) ?: "Unknown",
                        phoneNumber = it.getString(numberIdx) ?: "",
                        photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null
                    )
                }
            }
        }
        return contacts.values.toList()
    }
}
