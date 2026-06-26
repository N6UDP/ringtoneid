package com.example.ringtoneid.data.history

import android.content.Context
import android.content.SharedPreferences
import com.example.ringtoneid.domain.model.Variation
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists per-contact variation history (and favorites) as a single JSON object in
 * SharedPreferences keyed by contact id: `{ "<contactId>": [ {variation}, ... ] }`.
 * No DB migration. History is capped per contact, evicting the oldest non-favorite entries.
 */
object VariationStore {
    private const val PREFS = "ringtone_id_prefs"
    private const val KEY = "variation_history"
    private const val MAX_PER_CONTACT = 30

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadAll(context: Context): MutableMap<Long, MutableList<Variation>> {
        val raw = prefs(context).getString(KEY, null) ?: return mutableMapOf()
        return try {
            val root = JSONObject(raw)
            val map = mutableMapOf<Long, MutableList<Variation>>()
            root.keys().forEach { key ->
                val arr = root.getJSONArray(key)
                val list = (0 until arr.length()).map { Variation.fromJson(arr.getJSONObject(it)) }
                map[key.toLong()] = list.toMutableList()
            }
            map
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun saveAll(context: Context, map: Map<Long, List<Variation>>) {
        val root = JSONObject()
        map.forEach { (contactId, list) ->
            if (list.isNotEmpty()) {
                val arr = JSONArray()
                list.forEach { arr.put(it.toJson()) }
                root.put(contactId.toString(), arr)
            }
        }
        prefs(context).edit().putString(KEY, root.toString()).apply()
    }

    /** History for one contact, newest first. */
    fun historyFor(context: Context, contactId: Long): List<Variation> =
        loadAll(context)[contactId]?.sortedByDescending { it.createdAt } ?: emptyList()

    /** All favorited variations across every contact, newest first. */
    fun allFavorites(context: Context): List<Variation> =
        loadAll(context).values.flatten().filter { it.favorite }.sortedByDescending { it.createdAt }

    /**
     * Records [variation] into its contact's history. De-dupes against the most recent entry
     * with identical seed+settings, and caps the list (favorites are never evicted).
     */
    fun record(context: Context, variation: Variation) {
        val map = loadAll(context)
        val list = map.getOrPut(variation.contactId) { mutableListOf() }
        val duplicate = list.any {
            it.seed == variation.seed && it.settings == variation.settings
        }
        if (duplicate) return
        list.add(variation)
        // Evict oldest non-favorites beyond the cap.
        val nonFavCount = list.count { !it.favorite }
        if (nonFavCount > MAX_PER_CONTACT) {
            var toRemove = nonFavCount - MAX_PER_CONTACT
            val sortedOldestFirst = list.sortedBy { it.createdAt }
            for (v in sortedOldestFirst) {
                if (toRemove == 0) break
                if (!v.favorite) {
                    list.remove(v)
                    toRemove--
                }
            }
        }
        saveAll(context, map)
    }

    fun setFavorite(context: Context, contactId: Long, variationId: String, favorite: Boolean) {
        val map = loadAll(context)
        val list = map[contactId] ?: return
        val idx = list.indexOfFirst { it.id == variationId }
        if (idx < 0) return
        list[idx] = list[idx].copy(favorite = favorite)
        saveAll(context, map)
    }

    fun delete(context: Context, contactId: Long, variationId: String) {
        val map = loadAll(context)
        val list = map[contactId] ?: return
        list.removeAll { it.id == variationId }
        saveAll(context, map)
    }
}
