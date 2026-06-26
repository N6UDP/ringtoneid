package com.example.ringtoneid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GenerationModelJsonTest {

    private val sample = GenerationSettings(
        format = "midi",
        instrument = 24,
        length = 12,
        style = "minor",
        root = 57,
        tempoMin = 90,
        tempoMax = 180,
        tempoContour = "accelerate",
        contour = "ascending",
        octave = 12,
        repeat = 3,
        articulation = "staccato",
        harmony = "fifth"
    )

    @Test
    fun `GenerationSettings survives a JSON round-trip`() {
        val restored = GenerationSettings.fromJson(sample.toJson())
        assertEquals(sample, restored)
    }

    @Test
    fun `GenerationSettings fromJson falls back to defaults for missing keys`() {
        val restored = GenerationSettings.fromJson(org.json.JSONObject())
        assertEquals(GenerationSettings.FACTORY, restored)
    }

    @Test
    fun `GenerationPreset survives a JSON round-trip`() {
        val preset = GenerationPreset(
            id = "preset-1",
            name = "Spooky",
            enabled = false,
            weight = 5,
            settings = sample
        )
        val restored = GenerationPreset.fromJson(preset.toJson())
        assertEquals(preset, restored)
    }

    @Test
    fun `GenerationPreset weight is floored at one`() {
        val json = GenerationPreset(name = "Zero", weight = 0).toJson()
        assertEquals(1, GenerationPreset.fromJson(json).weight)
    }

    @Test
    fun `Variation survives a JSON round-trip including settings`() {
        val variation = Variation(
            id = "var-1",
            contactId = 42L,
            contactName = "Ada Lovelace",
            phoneNumber = "5551234567",
            seed = 7,
            settings = sample,
            favorite = true,
            createdAt = 1_700_000_000_000L
        )
        val restored = Variation.fromJson(variation.toJson())
        assertEquals(variation, restored)
    }
}
