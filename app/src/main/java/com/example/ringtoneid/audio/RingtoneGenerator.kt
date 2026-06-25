package com.example.ringtoneid.audio

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.ringtoneid.domain.model.RingtoneProfile
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class RingtoneGenerator @Inject constructor() {

    private var audioTrack: AudioTrack? = null

    private val digitToMidi = mapOf(
        '0' to 60, '1' to 62, '2' to 64, '3' to 65, '4' to 67,
        '5' to 69, '6' to 71, '7' to 72, '8' to 74, '9' to 76
    )

    fun phoneNumberToNotes(phoneNumber: String, seed: Int = 0, noteCount: Int = 8): List<Int> {
        val digits = phoneNumber.filter { it.isDigit() }
        // Use all digits as base material, cycling if needed
        val baseNotes = digits.map { digitToMidi[it] ?: 60 }
        if (baseNotes.isEmpty()) return List(noteCount) { 60 }

        // Generate noteCount notes using seed to permute
        val random = java.util.Random((digits.hashCode().toLong() * 31) + seed.toLong())
        val notes = mutableListOf<Int>()
        for (i in 0 until noteCount) {
            val baseIdx = (i + seed) % baseNotes.size
            val baseNote = baseNotes[baseIdx]
            // Seed adds slight variation: shift octave or pick from nearby scale degrees
            val offset = if (seed == 0) 0 else (random.nextInt(5) - 2) * 2 // -4 to +4 semitones
            notes.add((baseNote + offset).coerceIn(48, 84)) // Keep in reasonable range (C3-C6)
        }
        return notes
    }

    private fun midiToFrequency(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)

    fun generatePcm(notes: List<Int>): ByteArray {
        val sampleRate = 44100
        val noteDurationMs = 200
        val fadeMs = 20
        val samplesPerNote = sampleRate * noteDurationMs / 1000
        val fadeSamples = sampleRate * fadeMs / 1000

        val output = ByteArrayOutputStream()
        val buffer = ByteBuffer.allocate(samplesPerNote * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (midi in notes) {
            buffer.clear()
            val freq = midiToFrequency(midi)
            for (i in 0 until samplesPerNote) {
                val sample = sin(2 * PI * freq * i / sampleRate)
                val envelope = when {
                    i < fadeSamples -> i.toDouble() / fadeSamples
                    i > samplesPerNote - fadeSamples -> (samplesPerNote - i).toDouble() / fadeSamples
                    else -> 1.0
                }
                val pcmValue = (sample * envelope * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer.putShort(pcmValue)
            }
            output.write(buffer.array(), 0, samplesPerNote * 2)
        }
        return output.toByteArray()
    }

    fun generateAndSave(context: Context, profile: RingtoneProfile): Uri {
        val pcm = generatePcm(profile.notes)
        val wav = pcmToWav(pcm, 44100, 1)
        val safeName = profile.contactName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val fileName = "ringtone_${profile.contactId}_${safeName}.wav"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.IS_RINGTONE, 1)
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
            put(MediaStore.Audio.Media.IS_ALARM, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Ringtones/")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val uri = context.contentResolver.insert(collection, values)
            ?: error("Failed to create MediaStore entry for $fileName")

        context.contentResolver.openOutputStream(uri)?.use { it.write(wav) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val update = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, update, null, null)
        }

        return uri
    }

    private fun pcmToWav(pcm: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.size
        val totalSize = 36 + dataSize

        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(totalSize)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1)  // PCM
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort(blockAlign.toShort())
        buf.putShort(bitsPerSample.toShort())
        buf.put("data".toByteArray())
        buf.putInt(dataSize)
        buf.put(pcm)
        return buf.array()
    }

    fun previewPlay(notes: List<Int>) {
        stopPreview()
        val pcm = generatePcm(notes)
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, pcm.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack = track
        track.write(pcm, 0, pcm.size)
        track.play()
    }

    fun stopPreview() {
        audioTrack?.let {
            if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
            it.release()
        }
        audioTrack = null
    }
}
