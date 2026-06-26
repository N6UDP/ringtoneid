package com.example.ringtoneid.domain.model

import org.json.JSONObject
import java.util.UUID

/**
 * A saved snapshot of one ringtone variation the user has played with, so they can return
 * to a tune they liked. Captures the full [GenerationSettings] plus the seed, which together
 * fully reproduce the melody via [GenerateRingtoneUseCase.from].
 */
data class Variation(
    val id: String = UUID.randomUUID().toString(),
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val seed: Int,
    val settings: GenerationSettings,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("contactId", contactId)
        put("contactName", contactName)
        put("phoneNumber", phoneNumber)
        put("seed", seed)
        put("settings", settings.toJson())
        put("favorite", favorite)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject): Variation = Variation(
            id = o.optString("id", UUID.randomUUID().toString()),
            contactId = o.optLong("contactId", -1L),
            contactName = o.optString("contactName", ""),
            phoneNumber = o.optString("phoneNumber", ""),
            seed = o.optInt("seed", 0),
            settings = o.optJSONObject("settings")?.let { GenerationSettings.fromJson(it) }
                ?: GenerationSettings(),
            favorite = o.optBoolean("favorite", false),
            createdAt = o.optLong("createdAt", 0L)
        )
    }
}
