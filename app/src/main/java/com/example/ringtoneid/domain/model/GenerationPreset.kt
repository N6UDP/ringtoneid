package com.example.ringtoneid.domain.model

import org.json.JSONObject
import java.util.UUID

/**
 * A named, weighted bundle of generation settings. The user maintains a pool of these;
 * bulk generation (Generate All, auto-on-launch, background sync) picks one at random
 * (weighted by [weight]) per contact so the contact list gets variety.
 */
data class GenerationPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val weight: Int = 1,
    val settings: GenerationSettings = GenerationSettings()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("enabled", enabled)
        put("weight", weight)
        put("settings", settings.toJson())
    }

    companion object {
        fun fromJson(o: JSONObject): GenerationPreset = GenerationPreset(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", "Preset"),
            enabled = o.optBoolean("enabled", true),
            weight = o.optInt("weight", 1).coerceAtLeast(1),
            settings = o.optJSONObject("settings")?.let { GenerationSettings.fromJson(it) }
                ?: GenerationSettings()
        )
    }
}
