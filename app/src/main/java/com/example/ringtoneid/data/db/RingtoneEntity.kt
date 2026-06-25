package com.example.ringtoneid.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ringtones")
data class RingtoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val notes: String,   // JSON array of ints, e.g. "[60,62,64]"
    val seed: Int = 0,
    val noteCount: Int = 8,
    val audioFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
