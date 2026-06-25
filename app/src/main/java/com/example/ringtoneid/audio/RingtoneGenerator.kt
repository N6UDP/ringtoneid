package com.example.ringtoneid.audio

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.ringtoneid.domain.model.RingtoneProfile
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Singleton
class RingtoneGenerator @Inject constructor() {

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null

    private val digitToMidi = mapOf(
        '0' to 60, '1' to 62, '2' to 64, '3' to 65, '4' to 67,
        '5' to 69, '6' to 71, '7' to 72, '8' to 74, '9' to 76
    )

    fun phoneNumberToNotes(phoneNumber: String, seed: Int = 0, noteCount: Int = 8): List<Int> {
        val digits = phoneNumber.filter { it.isDigit() }
        val baseNotes = digits.map { digitToMidi[it] ?: 60 }
        if (baseNotes.isEmpty()) return List(noteCount) { 60 }

        val random = java.util.Random((digits.hashCode().toLong() * 31) + seed.toLong())
        val notes = mutableListOf<Int>()
        for (i in 0 until noteCount) {
            val baseIdx = (i + seed) % baseNotes.size
            val baseNote = baseNotes[baseIdx]
            val offset = if (seed == 0) 0 else (random.nextInt(5) - 2) * 2
            notes.add((baseNote + offset).coerceIn(48, 84))
        }
        return notes
    }

    private fun midiToFrequency(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)

    private fun waveform(phase: Double, midiProgram: Int): Double {
        val p = phase % (2 * PI)
        return when (midiProgram) {
            in 16..23 -> // Organ: square wave
                if (p < PI) 0.6 else -0.6
            in 24..39 -> // Guitar/Bass: plucked (sine + 2nd harmonic)
                0.6 * sin(p) + 0.3 * sin(2 * p) + 0.1 * sin(3 * p)
            in 40..55 -> { // Strings: sawtooth
                val t = p / (2 * PI)
                (2.0 * t - 1.0) * 0.7
            }
            in 56..63 -> // Brass: bright (sine + harmonics)
                0.5 * sin(p) + 0.25 * sin(2 * p) + 0.15 * sin(3 * p) + 0.1 * sin(4 * p)
            in 64..79 -> { // Reed/Pipe: triangle wave
                val t = p / (2 * PI)
                (2.0 * abs(2.0 * t - 1.0) - 1.0) * 0.7
            }
            in 80..95 -> // Synth lead: pulse wave (narrow square)
                if (p < PI * 0.7) 0.6 else -0.6
            else -> // Piano/default: sine wave
                sin(p)
        }
    }

    fun generatePcm(notes: List<Int>, midiProgram: Int = 0): ByteArray {
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
                val phase = 2 * PI * freq * i / sampleRate
                val sample = waveform(phase, midiProgram)
                val envelope = when {
                    i < fadeSamples -> i.toDouble() / fadeSamples
                    i > samplesPerNote - fadeSamples -> (samplesPerNote - i).toDouble() / fadeSamples
                    else -> 1.0
                }
                val pcmValue = (sample * envelope * Short.MAX_VALUE * 0.8).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer.putShort(pcmValue)
            }
            output.write(buffer.array(), 0, samplesPerNote * 2)
        }
        return output.toByteArray()
    }

    fun generateAndSave(context: Context, profile: RingtoneProfile): Uri {
        val format = try { AudioOutputFormat.valueOf(profile.format.uppercase()) } catch (_: Exception) { AudioOutputFormat.WAV }
        val audioData = when (format) {
            AudioOutputFormat.WAV -> { val pcm = generatePcm(profile.notes, profile.midiProgram); pcmToWav(pcm, 44100, 1) }
            AudioOutputFormat.M4A -> { val pcm = generatePcm(profile.notes, profile.midiProgram); pcmToM4a(context, pcm, 44100, 1) }
            AudioOutputFormat.MIDI -> notesToMidi(profile.notes, profile.midiProgram)
        }

        val safeName = profile.contactName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val fileName = "ringtone_${profile.contactId}_${safeName}.${format.extension}"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
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

        context.contentResolver.openOutputStream(uri)?.use { it.write(audioData) }

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
        buf.putShort(1)
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

    private fun pcmToM4a(context: Context, pcm: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val tempFile = File(context.cacheDir, "temp_ringtone.m4a")
        try {
            val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, pcm.size)
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            codec.start()

            var inputOffset = 0
            var inputDone = false
            var outputDone = false
            val bufferInfo = MediaCodec.BufferInfo()

            while (!outputDone) {
                if (!inputDone) {
                    val inputBufIdx = codec.dequeueInputBuffer(10000)
                    if (inputBufIdx >= 0) {
                        val inputBuf = codec.getInputBuffer(inputBufIdx)!!
                        val remaining = pcm.size - inputOffset
                        if (remaining <= 0) {
                            codec.queueInputBuffer(inputBufIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val size = minOf(remaining, inputBuf.capacity())
                            inputBuf.clear()
                            inputBuf.put(pcm, inputOffset, size)
                            val pts = (inputOffset.toLong() * 1_000_000L) / (sampleRate * channels * 2)
                            codec.queueInputBuffer(inputBufIdx, 0, size, pts, 0)
                            inputOffset += size
                        }
                    }
                }

                val outputBufIdx = codec.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputBufIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputBufIdx >= 0 -> {
                        val outputBuf = codec.getOutputBuffer(outputBufIdx)!!
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted) {
                            outputBuf.position(bufferInfo.offset)
                            outputBuf.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, outputBuf, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufIdx, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            codec.stop()
            codec.release()
            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }

            return tempFile.readBytes()
        } finally {
            tempFile.delete()
        }
    }

    private fun notesToMidi(notes: List<Int>, midiProgram: Int = 0): ByteArray {
        val ticksPerNote = 96
        val tempo = 500000
        val channel = 0
        val velocity = 100

        val output = ByteArrayOutputStream()
        val track = ByteArrayOutputStream()

        // Tempo meta event
        track.write(byteArrayOf(0x00))
        track.write(byteArrayOf(0xFF.toByte(), 0x51, 0x03))
        track.write(byteArrayOf(
            ((tempo shr 16) and 0xFF).toByte(),
            ((tempo shr 8) and 0xFF).toByte(),
            (tempo and 0xFF).toByte()
        ))

        // Program change
        track.write(byteArrayOf(0x00))
        track.write(byteArrayOf((0xC0 or channel).toByte(), midiProgram.toByte()))

        // Note events
        for (note in notes) {
            track.write(byteArrayOf(0x00))
            track.write(byteArrayOf((0x90 or channel).toByte(), note.toByte(), velocity.toByte()))
            track.write(writeVarLen(ticksPerNote))
            track.write(byteArrayOf((0x80 or channel).toByte(), note.toByte(), 0x00))
        }

        // End of track
        track.write(byteArrayOf(0x00, 0xFF.toByte(), 0x2F, 0x00))

        val trackBytes = track.toByteArray()

        // MIDI header
        output.write("MThd".toByteArray())
        output.write(intTo4Bytes(6))
        output.write(intTo2Bytes(0))
        output.write(intTo2Bytes(1))
        output.write(intTo2Bytes(96))

        // Track header
        output.write("MTrk".toByteArray())
        output.write(intTo4Bytes(trackBytes.size))
        output.write(trackBytes)

        return output.toByteArray()
    }

    private fun writeVarLen(value: Int): ByteArray {
        if (value < 0x80) return byteArrayOf(value.toByte())
        val bytes = mutableListOf<Byte>()
        var v = value
        bytes.add((v and 0x7F).toByte())
        v = v shr 7
        while (v > 0) {
            bytes.add(0, ((v and 0x7F) or 0x80).toByte())
            v = v shr 7
        }
        return bytes.toByteArray()
    }

    private fun intTo4Bytes(value: Int) = byteArrayOf(
        ((value shr 24) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte()
    )

    private fun intTo2Bytes(value: Int) = byteArrayOf(
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte()
    )

    fun previewPlay(notes: List<Int>, midiProgram: Int = 0) {
        stopPreview()
        val pcm = generatePcm(notes, midiProgram)
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

    fun previewMidi(context: Context, notes: List<Int>, midiProgram: Int = 0) {
        stopPreview()
        try {
            val midiData = notesToMidi(notes, midiProgram)
            val tempFile = File(context.cacheDir, "preview_ringtone.mid")
            tempFile.writeBytes(midiData)
            val player = MediaPlayer()
            player.setDataSource(tempFile.absolutePath)
            player.setOnCompletionListener {
                try { it.release() } catch (_: Exception) {}
                tempFile.delete()
            }
            player.setOnErrorListener { mp, _, _ ->
                try { mp.release() } catch (_: Exception) {}
                tempFile.delete()
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (_: Exception) {
            // Fallback to PCM preview if MIDI playback fails
            previewPlay(notes, midiProgram)
        }
    }

    fun stopPreview() {
        audioTrack?.let {
            try {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
            } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {}
        }
        audioTrack = null
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {}
        }
        mediaPlayer = null
    }
}
