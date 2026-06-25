package com.example.ringtoneid.domain.model

data class RingtoneProfile(
    val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val notes: List<Int>,     // MIDI note numbers
    val seed: Int = 0,
    val noteCount: Int = 8,
    val audioFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
