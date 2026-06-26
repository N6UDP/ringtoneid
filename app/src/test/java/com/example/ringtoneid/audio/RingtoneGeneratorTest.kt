package com.example.ringtoneid.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingtoneGeneratorTest {

    private val generator = RingtoneGenerator()

    @Test
    fun `phoneNumberToNotes returns requested note count for asis contour`() {
        val notes = generator.phoneNumberToNotes(
            phoneNumber = "5551234567",
            seed = 0,
            noteCount = 8,
            repeat = 1
        )
        assertEquals(8, notes.size)
    }

    @Test
    fun `phoneNumberToNotes honours repeat by tiling the motif`() {
        val once = generator.phoneNumberToNotes("5551234567", seed = 0, noteCount = 6, repeat = 1)
        val thrice = generator.phoneNumberToNotes("5551234567", seed = 0, noteCount = 6, repeat = 3)
        assertEquals(6, once.size)
        assertEquals(18, thrice.size)
        // The repeated output is the single motif tiled back-to-back.
        assertEquals(once + once + once, thrice)
    }

    @Test
    fun `every generated note stays within the chosen scale`() {
        for (style in MusicalStyles.ALL) {
            val root = MusicalKeys.DEFAULT_ROOT
            val notes = generator.phoneNumberToNotes(
                phoneNumber = "8005558765",
                seed = 0,
                noteCount = 12,
                styleId = style.id,
                rootNote = root,
                repeat = 1
            )
            val scalePitchClasses = style.intervals.map { ((it % 12) + 12) % 12 }.toSet()
            notes.forEach { note ->
                val pc = (((note - root) % 12) + 12) % 12
                assertTrue(
                    "Note $note (pc=$pc) not in ${style.id} scale $scalePitchClasses",
                    pc in scalePitchClasses
                )
            }
        }
    }

    @Test
    fun `same inputs produce identical melodies and different seeds usually differ`() {
        val a = generator.phoneNumberToNotes("5551234567", seed = 3, noteCount = 10)
        val b = generator.phoneNumberToNotes("5551234567", seed = 3, noteCount = 10)
        assertEquals(a, b)
        val c = generator.phoneNumberToNotes("5551234567", seed = 4, noteCount = 10)
        assertNotEquals(a, c)
    }

    @Test
    fun `blank phone number falls back to a steady root tone`() {
        val notes = generator.phoneNumberToNotes("", seed = 0, noteCount = 5, repeat = 2)
        assertEquals(10, notes.size)
        assertTrue(notes.all { it == notes.first() })
    }

    @Test
    fun `ascending contour yields a non-decreasing melody`() {
        val notes = generator.phoneNumberToNotes(
            phoneNumber = "8005558765",
            seed = 0,
            noteCount = 12,
            contourId = MelodicContours.ASCENDING.id,
            repeat = 1
        )
        assertEquals(notes.sorted(), notes)
    }

    @Test
    fun `generatePcm produces 16-bit samples matching the slot length`() {
        val notes = listOf(60, 62, 64, 65)
        val bpm = 150
        val pcm = generator.generatePcm(notes, midiProgram = 0, tempos = List(notes.size) { bpm })
        val samplesPerNote = 44100 * (30000 / bpm) / 1000
        val expectedBytes = samplesPerNote * notes.size * 2
        assertEquals(expectedBytes, pcm.size)
    }

    @Test
    fun `generatePcm returns empty output for no notes`() {
        assertEquals(0, generator.generatePcm(emptyList()).size)
    }
}
