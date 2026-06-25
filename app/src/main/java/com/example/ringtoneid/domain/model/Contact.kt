package com.example.ringtoneid.domain.model

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val hasCustomRingtone: Boolean = false
)
