package com.example.ringtoneid.data.preset

import android.content.Context
import android.content.SharedPreferences
import com.example.ringtoneid.domain.model.GenerationPreset
import com.example.ringtoneid.domain.model.GenerationSettings
import org.json.JSONArray
import java.util.Random

/**
 * Persists the user's pool of [GenerationPreset]s as a JSON array in SharedPreferences
 * (no DB migration). On first access it migrates the legacy flat `default_*` prefs into a
 * single seed "Default" preset, so existing users keep their configured settings.
 */
object PresetStore {
    private const val PREFS = "ringtone_id_prefs"
    private const val KEY = "generation_presets"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<GenerationPreset> = load(prefs(context))

    fun load(prefs: SharedPreferences): List<GenerationPreset> {
        val raw = prefs.getString(KEY, null)
        if (raw.isNullOrBlank()) {
            val seed = GenerationPreset(name = "Default", settings = GenerationSettings.fromPrefs(prefs))
            save(prefs, listOf(seed))
            return listOf(seed)
        }
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { GenerationPreset.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            val seed = GenerationPreset(name = "Default", settings = GenerationSettings.fromPrefs(prefs))
            save(prefs, listOf(seed))
            listOf(seed)
        }
    }

    fun save(context: Context, presets: List<GenerationPreset>) = save(prefs(context), presets)

    fun save(prefs: SharedPreferences, presets: List<GenerationPreset>) {
        val arr = JSONArray()
        presets.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    /**
     * Weighted-random pick among enabled presets. Returns null when none are enabled,
     * letting callers fall back to [GenerationSettings.FACTORY].
     */
    fun pickWeighted(presets: List<GenerationPreset>, rng: Random = Random()): GenerationPreset? {
        val enabled = presets.filter { it.enabled && it.weight > 0 }
        if (enabled.isEmpty()) return null
        if (enabled.size == 1) return enabled.first()
        val total = enabled.sumOf { it.weight }
        var r = rng.nextInt(total)
        for (p in enabled) {
            r -= p.weight
            if (r < 0) return p
        }
        return enabled.last()
    }
}
