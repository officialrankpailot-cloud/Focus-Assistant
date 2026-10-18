package com.example.focus

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

enum class Soundscape(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val colorHex: Long
) {
    LOFI_BEATS(
        id = "lofi",
        title = "Lo-Fi Chill Beats",
        subtitle = "Warm Rhodes chords & soft pulse",
        iconEmoji = "🎧",
        colorHex = 0xFF8B5CF6
    ),
    RAIN_THUNDER(
        id = "rain",
        title = "Gentle Rainfall",
        subtitle = "Soothing rain & distant rumble",
        iconEmoji = "🌧️",
        colorHex = 0xFF06B6D4
    ),
    BROWN_NOISE(
        id = "brown_noise",
        title = "Deep Brown Noise",
        subtitle = "Deep acoustic blanket for ADHD",
        iconEmoji = "🌊",
        colorHex = 0xFF10B981
    ),
    WHITE_NOISE(
        id = "white_noise",
        title = "Pure White Noise",
        subtitle = "Crisp focus & distraction blocker",
        iconEmoji = "🌫️",
        colorHex = 0xFFF59E0B
    ),
    FOREST_STREAM(
        id = "stream",
        title = "Forest Stream",
        subtitle = "Rippling water & woodland breeze",
        iconEmoji = "🌲",
        colorHex = 0xFF34D399
    )
}

object FocusAmbientAudioPlayer {

    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(Dispatchers.Default)
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentSoundscape = MutableStateFlow(Soundscape.LOFI_BEATS)
    val currentSoundscape = _currentSoundscape.asStateFlow()

    private val _volume = MutableStateFlow(0.75f)
    val volume = _volume.asStateFlow()

    val autoPlayOnFocusStart = MutableStateFlow(true)

    fun togglePlay() {
        if (_isPlaying.value) {
            stop()
        } else {
            play(_currentSoundscape.value)
        }
    }

    fun setSoundscape(soundscape: Soundscape) {
        _currentSoundscape.value = soundscape
        if (_isPlaying.value) {
            play(soundscape)
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (_: Exception) {}
    }

    fun play(soundscape: Soundscape) {
        stop()
        _currentSoundscape.value = soundscape
        _isPlaying.value = true

        playbackJob = scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufferSize, SAMPLE_RATE / 2)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.setVolume(_volume.value)
                audioTrack?.play()

                val buffer = ShortArray(1024)
                val random = Random()

                // State variables for smooth procedural synthesis
                var brownNoiseVal = 0.0
                var phase = 0.0
                var lofiNoteIndex = 0
                var sampleCounter = 0
                var vinylCrackleCounter = 0

                // Lo-fi chord progression: Cmaj7 -> Am7 -> Dm7 -> G7 (warm frequency fundamentals)
                val chords = listOf(
                    doubleArrayOf(130.81, 164.81, 196.00, 246.94), // C3, E3, G3, B3
                    doubleArrayOf(110.00, 130.81, 164.81, 196.00), // A2, C3, E3, G3
                    doubleArrayOf(146.83, 174.61, 220.00, 261.63), // D3, F3, A3, C4
                    doubleArrayOf(98.00, 123.47, 146.83, 174.61)   // G2, B2, D3, F3
                )
                val chordLengthSamples = SAMPLE_RATE * 3 // 3 seconds per chord

                while (isActive && _isPlaying.value) {
                    for (i in buffer.indices) {
                        sampleCounter++
                        val sample: Double = when (soundscape) {
                            Soundscape.WHITE_NOISE -> {
                                (random.nextDouble() * 2.0 - 1.0) * 0.25
                            }

                            Soundscape.BROWN_NOISE -> {
                                val white = random.nextDouble() * 2.0 - 1.0
                                brownNoiseVal = (brownNoiseVal + (0.02 * white)) / 1.02
                                brownNoiseVal * 1.5
                            }

                            Soundscape.RAIN_THUNDER -> {
                                val white = random.nextDouble() * 2.0 - 1.0
                                brownNoiseVal = (brownNoiseVal + (0.04 * white)) / 1.04
                                // Soft raindrops modulation
                                val rainMod = (1.0 + 0.3 * sin(sampleCounter * 0.005))
                                val drop = if (random.nextInt(60) == 0) (random.nextDouble() * 0.15) else 0.0
                                (brownNoiseVal * 1.2 * rainMod) + drop
                            }

                            Soundscape.FOREST_STREAM -> {
                                val white = random.nextDouble() * 2.0 - 1.0
                                brownNoiseVal = (brownNoiseVal + (0.06 * white)) / 1.06
                                val waterWave = sin(sampleCounter * 0.008) * 0.15
                                (brownNoiseVal * 0.9) + waterWave
                            }

                            Soundscape.LOFI_BEATS -> {
                                // Lo-fi chord generation with soft tape saturation
                                val currentChord = chords[lofiNoteIndex % chords.size]
                                if (sampleCounter % chordLengthSamples == 0) {
                                    lofiNoteIndex++
                                }

                                var chordSignal = 0.0
                                for (freq in currentChord) {
                                    val wave = sin(2.0 * Math.PI * freq * (sampleCounter.toDouble() / SAMPLE_RATE))
                                    chordSignal += wave
                                }
                                chordSignal /= currentChord.size

                                // Subtle vinyl crackle
                                vinylCrackleCounter++
                                val crackle = if (random.nextInt(450) == 0) (random.nextDouble() * 0.08 - 0.04) else 0.0

                                // Gentle low-beat pulse every 0.8 seconds (75 BPM kick/snare warm thump)
                                val beatCycle = sampleCounter % (SAMPLE_RATE * 60 / 75)
                                val kickPulse = if (beatCycle < 1500) {
                                    sin(2.0 * Math.PI * 65.0 * (beatCycle.toDouble() / SAMPLE_RATE)) * (1.0 - (beatCycle / 1500.0)) * 0.35
                                } else 0.0

                                (chordSignal * 0.22) + (kickPulse * 0.3) + crackle
                            }
                        }

                        // Clamp to 16-bit PCM range
                        val clamped = sample.coerceIn(-1.0, 1.0)
                        buffer[i] = (clamped * 32767.0).toInt().toShort()
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e("AmbientAudio", "Playback error: ${e.message}")
            } finally {
                cleanupAudioTrack()
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        cleanupAudioTrack()
    }

    private fun cleanupAudioTrack() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
