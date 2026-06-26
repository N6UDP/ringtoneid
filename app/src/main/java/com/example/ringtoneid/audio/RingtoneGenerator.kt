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

    /**
     * Maps a phone number onto a melody that always stays within the chosen
     * musical scale (style) and key (rootNote). Each digit selects a scale degree;
     * the seed reorders digits and nudges degrees *within* the scale, so shuffle
     * variations remain in-key instead of introducing dissonant chromatic offsets.
     */
    fun phoneNumberToNotes(
        phoneNumber: String,
        seed: Int = 0,
        noteCount: Int = 8,
        styleId: String = MusicalStyles.DEFAULT.id,
        rootNote: Int = MusicalKeys.DEFAULT_ROOT,
        contourId: String = MelodicContours.DEFAULT.id,
        octaveShift: Int = Octaves.DEFAULT_SHIFT,
        repeat: Int = MotifRepeat.DEFAULT
    ): List<Int> {
        val scale = MusicalStyles.fromId(styleId).intervals
        val digits = phoneNumber.filter { it.isDigit() }.map { it - '0' }
        val root = rootNote + octaveShift
        if (digits.isEmpty()) return List(noteCount * repeat.coerceAtLeast(1)) { root }

        val random = java.util.Random((digits.hashCode().toLong() * 31) + seed.toLong())
        val motif = mutableListOf<Int>()
        for (i in 0 until noteCount) {
            val digit = digits[(i + seed) % digits.size]
            var degree = digit
            // Seed variation: shift by whole scale steps only — never leaves the key.
            if (seed != 0) degree += random.nextInt(3) - 1
            val scaleIndex = ((degree % scale.size) + scale.size) % scale.size
            val octave = Math.floorDiv(degree, scale.size)
            val note = (root + octave * 12 + scale[scaleIndex]).coerceIn(36, 96)
            motif.add(note)
        }

        val shaped = applyContour(motif, contourId, random)

        val result = mutableListOf<Int>()
        repeat(repeat.coerceIn(MotifRepeat.MIN, MotifRepeat.MAX)) { result.addAll(shaped) }
        return result
    }

    private fun applyContour(notes: List<Int>, contourId: String, random: java.util.Random): List<Int> {
        return when (contourId) {
            MelodicContours.ASCENDING.id -> notes.sorted()
            MelodicContours.DESCENDING.id -> notes.sortedDescending()
            MelodicContours.ARCH.id -> {
                val asc = notes.sorted()
                val half = (asc.size + 1) / 2
                asc.take(half) + asc.drop(half).reversed()
            }
            MelodicContours.RANDOM.id -> notes.shuffled(random)
            else -> notes
        }
    }

    /** Resolves a per-note tempo (BPM) list based on the tempo range and motion. */
    private fun resolveTempos(count: Int, tempoMin: Int, tempoMax: Int, contourId: String, seed: Int): List<Int> {
        val lo = tempoMin.coerceIn(Tempo.MIN_BPM, Tempo.MAX_BPM)
        val hi = tempoMax.coerceIn(Tempo.MIN_BPM, Tempo.MAX_BPM)
        val min = minOf(lo, hi)
        val max = maxOf(lo, hi)
        if (count <= 0) return emptyList()
        if (min == max) return List(count) { min }
        return when (contourId) {
            TempoContours.ACCELERATE.id -> List(count) { i ->
                (min + (max - min) * i / maxOf(1, count - 1))
            }
            TempoContours.DECELERATE.id -> List(count) { i ->
                (max - (max - min) * i / maxOf(1, count - 1))
            }
            TempoContours.RANDOM.id -> {
                val rnd = java.util.Random(seed.toLong() * 101 + 7)
                List(count) { min + rnd.nextInt(max - min + 1) }
            }
            else -> List(count) { (min + max) / 2 }
        }
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

    /**
     * Per-note amplitude envelope. A short attack and decay-to-sustain plus a release
     * ramp give each note a more natural, less "buzzy" shape than a flat gate. All
     * lengths are expressed in samples relative to the audible portion of the note.
     */
    private fun adsrEnvelope(i: Int, soundSamples: Int, sampleRate: Int): Double {
        if (i >= soundSamples) return 0.0
        val attack = (sampleRate * 0.008).toInt().coerceAtLeast(1)
        val decay = (sampleRate * 0.05).toInt().coerceAtLeast(1)
        val release = (sampleRate * 0.04).toInt().coerceIn(1, soundSamples)
        val sustain = 0.75
        val releaseStart = soundSamples - release
        return when {
            i < attack -> i.toDouble() / attack
            i < attack + decay -> 1.0 - (1.0 - sustain) * ((i - attack).toDouble() / decay)
            i >= releaseStart -> sustain * ((soundSamples - i).toDouble() / release).coerceIn(0.0, 1.0)
            else -> sustain
        }
    }

    /**
     * Subtle feedback-comb reverb tail for a touch of room/space, mixed lightly so it
     * enriches without muddying short ringtones. Operates in-place on a float buffer.
     */
    private fun applyReverb(buf: DoubleArray, sampleRate: Int, wet: Double = 0.18) {
        if (wet <= 0.0) return
        val delay = (sampleRate * 0.06).toInt().coerceAtLeast(1)
        val feedback = 0.32
        val wetBuf = DoubleArray(buf.size)
        for (i in buf.indices) {
            val echoed = if (i >= delay) wetBuf[i - delay] * feedback else 0.0
            wetBuf[i] = buf[i] + echoed
        }
        for (i in buf.indices) {
            buf[i] = buf[i] * (1.0 - wet) + wetBuf[i] * wet
        }
    }

    fun generatePcm(
        notes: List<Int>,
        midiProgram: Int = 0,
        tempos: List<Int> = List(notes.size) { Tempo.DEFAULT_BPM },
        articulationGate: Double = 0.9,
        harmonyInterval: Int? = null
    ): ByteArray {
        val sampleRate = 44100

        // Resolve per-note slot sizes up front so we can render into one continuous buffer
        // (enables a reverb tail and whole-tune peak normalization).
        val slotSizes = notes.indices.map { index ->
            val bpm = (tempos.getOrNull(index) ?: Tempo.DEFAULT_BPM).coerceIn(Tempo.MIN_BPM, Tempo.MAX_BPM)
            val noteDurationMs = 30000 / bpm
            sampleRate * noteDurationMs / 1000
        }
        val totalSamples = slotSizes.sum()
        if (totalSamples == 0) return ByteArray(0)

        val buf = DoubleArray(totalSamples)
        var cursor = 0
        notes.forEachIndexed { index, midi ->
            val samplesPerNote = slotSizes[index]
            val soundSamples = (samplesPerNote * articulationGate).toInt().coerceIn(1, samplesPerNote)
            val freq = midiToFrequency(midi)
            val harmonyFreq = harmonyInterval?.let { midiToFrequency((midi + it).coerceIn(24, 108)) }
            for (i in 0 until samplesPerNote) {
                var s = if (i < soundSamples) {
                    val phase = 2 * PI * freq * i / sampleRate
                    var v = waveform(phase, midiProgram)
                    if (harmonyFreq != null) {
                        val hp = 2 * PI * harmonyFreq * i / sampleRate
                        v = (v + 0.6 * waveform(hp, midiProgram)) * 0.7
                    }
                    v
                } else 0.0
                s *= adsrEnvelope(i, soundSamples, sampleRate)
                buf[cursor + i] = s
            }
            cursor += samplesPerNote
        }

        applyReverb(buf, sampleRate)

        // Peak-normalize so output is consistently loud across instruments/harmony
        // stacking without clipping.
        var peak = 0.0
        for (v in buf) { val a = abs(v); if (a > peak) peak = a }
        val gain = if (peak > 0.0) (0.95 / peak).coerceAtMost(4.0) else 1.0

        val output = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (v in buf) {
            val pcmValue = (v * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            output.putShort(pcmValue)
        }
        return output.array()
    }

    /** Builds the per-note tempo list for a profile, honouring range + tempo motion. */
    private fun temposFor(profile: RingtoneProfile): List<Int> =
        resolveTempos(profile.notes.size, profile.tempoMin, profile.tempoMax, profile.tempoContour, profile.seed)

    fun generateAndSave(context: Context, profile: RingtoneProfile, purpose: RingtonePurpose = RingtonePurpose.RINGTONE): Uri {
        val format = formatOf(profile)
        val audioData = generateBytes(context, profile)

        val safeName = profile.contactName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val fileName = "ringtone_${profile.contactId}_${safeName}.${format.extension}"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Audio.Media.IS_RINGTONE, if (purpose == RingtonePurpose.RINGTONE) 1 else 0)
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, if (purpose == RingtonePurpose.NOTIFICATION) 1 else 0)
            put(MediaStore.Audio.Media.IS_ALARM, if (purpose == RingtonePurpose.ALARM) 1 else 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, purpose.relativePath)
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

    /** The output format selected by the profile, defaulting to WAV for unknown values. */
    fun formatOf(profile: RingtoneProfile): AudioOutputFormat =
        try { AudioOutputFormat.valueOf(profile.format.uppercase()) } catch (_: Exception) { AudioOutputFormat.WAV }

    /** Renders the profile to encoded audio bytes (WAV/M4A/MIDI) without persisting them. */
    fun generateBytes(context: Context, profile: RingtoneProfile): ByteArray {
        val format = formatOf(profile)
        val tempos = temposFor(profile)
        val gate = Articulations.gateForId(profile.articulation)
        val harmony = Harmonies.intervalForId(profile.harmony)
        return when (format) {
            AudioOutputFormat.WAV -> { val pcm = generatePcm(profile.notes, profile.midiProgram, tempos, gate, harmony); pcmToWav(pcm, 44100, 1) }
            AudioOutputFormat.M4A -> { val pcm = generatePcm(profile.notes, profile.midiProgram, tempos, gate, harmony); pcmToM4a(context, pcm, 44100, 1) }
            AudioOutputFormat.MIDI -> notesToMidi(profile.notes, profile.midiProgram, tempos, gate, harmony)
        }
    }

    /**
     * Writes the profile's audio to app cache and returns a shareable FileProvider
     * content Uri (used for the system share sheet). The file lives under cache/shared.
     */
    fun exportToCacheUri(context: Context, profile: RingtoneProfile): Uri {
        val format = formatOf(profile)
        val bytes = generateBytes(context, profile)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val safeName = profile.contactName.ifBlank { "ringtone" }.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val file = File(dir, "${safeName}_${profile.seed}.${format.extension}")
        file.writeBytes(bytes)
        return androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
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

    private fun notesToMidi(
        notes: List<Int>,
        midiProgram: Int = 0,
        tempos: List<Int> = List(notes.size) { Tempo.DEFAULT_BPM },
        articulationGate: Double = 0.9,
        harmonyInterval: Int? = null
    ): ByteArray {
        // Eighth notes: division is 96 ticks/quarter, so 48 ticks = one eighth note.
        val ticksPerNote = 48
        val onTicks = (ticksPerNote * articulationGate).toInt().coerceIn(1, ticksPerNote)
        val offTicks = ticksPerNote - onTicks
        val channel = 0
        val velocity = 100

        val output = ByteArrayOutputStream()
        val track = ByteArrayOutputStream()

        // Program change
        track.write(byteArrayOf(0x00))
        track.write(byteArrayOf((0xC0 or channel).toByte(), midiProgram.toByte()))

        // Note events (tempo meta emitted per note so range/motion is honoured).
        // pendingDelta carries the silent gap from the previous note onto the next event.
        var pendingDelta = 0
        notes.forEachIndexed { index, note ->
            val bpm = (tempos.getOrNull(index) ?: Tempo.DEFAULT_BPM).coerceIn(Tempo.MIN_BPM, Tempo.MAX_BPM)
            val tempo = 60000000 / bpm
            val harmonyNote = harmonyInterval?.let { (note + it).coerceIn(24, 108) }

            // Tempo meta for this note (absorbs any pending gap delta)
            track.write(writeVarLen(pendingDelta))
            track.write(byteArrayOf(0xFF.toByte(), 0x51, 0x03))
            track.write(byteArrayOf(
                ((tempo shr 16) and 0xFF).toByte(),
                ((tempo shr 8) and 0xFF).toByte(),
                (tempo and 0xFF).toByte()
            ))

            // Note on (melody + optional harmony)
            track.write(byteArrayOf(0x00))
            track.write(byteArrayOf((0x90 or channel).toByte(), note.toByte(), velocity.toByte()))
            if (harmonyNote != null) {
                track.write(byteArrayOf(0x00))
                track.write(byteArrayOf((0x90 or channel).toByte(), harmonyNote.toByte(), (velocity * 3 / 4).toByte()))
            }

            // Note off after the sounding portion
            track.write(writeVarLen(onTicks))
            track.write(byteArrayOf((0x80 or channel).toByte(), note.toByte(), 0x00))
            if (harmonyNote != null) {
                track.write(byteArrayOf(0x00))
                track.write(byteArrayOf((0x80 or channel).toByte(), harmonyNote.toByte(), 0x00))
            }

            pendingDelta = offTicks
        }

        // End of track (absorbs final gap)
        track.write(writeVarLen(pendingDelta))
        track.write(byteArrayOf(0xFF.toByte(), 0x2F, 0x00))

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

    /** Plays a full profile preview (chooses MIDI vs PCM and applies all params). */
    fun preview(context: Context, profile: RingtoneProfile) {
        val tempos = temposFor(profile)
        val gate = Articulations.gateForId(profile.articulation)
        val harmony = Harmonies.intervalForId(profile.harmony)
        if (profile.format.equals("midi", ignoreCase = true)) {
            previewMidi(context, profile.notes, profile.midiProgram, tempos, gate, harmony)
        } else {
            previewPlay(profile.notes, profile.midiProgram, tempos, gate, harmony)
        }
    }

    fun previewPlay(
        notes: List<Int>,
        midiProgram: Int = 0,
        tempos: List<Int> = List(notes.size) { Tempo.DEFAULT_BPM },
        articulationGate: Double = 0.9,
        harmonyInterval: Int? = null
    ) {
        stopPreview()
        val pcm = generatePcm(notes, midiProgram, tempos, articulationGate, harmonyInterval)
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

    fun previewMidi(
        context: Context,
        notes: List<Int>,
        midiProgram: Int = 0,
        tempos: List<Int> = List(notes.size) { Tempo.DEFAULT_BPM },
        articulationGate: Double = 0.9,
        harmonyInterval: Int? = null
    ) {
        stopPreview()
        try {
            val midiData = notesToMidi(notes, midiProgram, tempos, articulationGate, harmonyInterval)
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
            previewPlay(notes, midiProgram, tempos, articulationGate, harmonyInterval)
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
