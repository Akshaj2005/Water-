package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioFeedback {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    /**
     * Synthesizes a bubbly, fluid popping sound.
     * Uses a fast rising frequency sweep combined with a short resonance pluck decay.
     */
    fun playBubblePop() {
        scope.launch {
            try {
                val durationMs = 110
                val numSamples = (SAMPLE_RATE * durationMs / 1000)
                val buffer = FloatArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = i.toFloat() / numSamples
                    
                    // Quick logarithmic/linear exponential pitch rise sweeping from 350Hz up to 1700Hz
                    val freq = 350f + (1350f * progress)
                    val angle = 2f * Math.PI * freq * t
                    
                    // Tight exponential amplitude envelope mimicking water bubble surface tension pop
                    val envelope = kotlin.math.exp(-4.5f * progress)
                    
                    buffer[i] = (sin(angle) * envelope * 0.45f).toFloat()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Synthesizes a crystalline ice cracking snap sound.
     * Emulates microscopic high frequency acoustic stress pulses and fractured resonance.
     */
    fun playIceCrack() {
        scope.launch {
            try {
                val durationMs = 240
                val numSamples = (SAMPLE_RATE * durationMs / 1000)
                val buffer = FloatArray(numSamples)

                // We construct 3 distinct sharp cracking pulses spaced closely to simulate fracturing tension
                val pulseOffsets = intArrayOf(0, (numSamples * 0.25f).toInt(), (numSamples * 0.58f).toInt())
                val pulseLengths = intArrayOf((numSamples * 0.16f).toInt(), (numSamples * 0.22f).toInt(), (numSamples * 0.15f).toInt())

                for (p in pulseOffsets.indices) {
                    val start = pulseOffsets[p]
                    val duration = pulseLengths[p]
                    val scaling = when (p) {
                        0 -> 0.08f  // Initial sharp snap
                        1 -> 0.11f  // Resonant crackle
                        else -> 0.04f // Tiny crystalline fracture tail
                    }

                    for (i in 0 until duration) {
                        val idx = start + i
                        if (idx >= numSamples) break

                        val progress = i.toFloat() / duration
                        
                        // Very high frequency snapping cycle
                        val freq = 3200f + (1800f * (sin(i.toFloat() * 0.18f) * 0.5f + 0.5f))
                        val angle = 2f * Math.PI * freq * (i.toFloat() / SAMPLE_RATE)

                        val noise = (Math.random() * 2.0 - 1.0).toFloat()
                        val sine = sin(angle).toFloat()

                        // Crack sound is high-pass colored noise mixed with high frequency sine waves
                        val click = (0.25f * sine + 0.75f * noise)
                        val env = kotlin.math.exp(-6.5f * progress)

                        buffer[idx] += click * env * scaling
                    }
                }

                // Smooth brickwall limiting
                for (i in buffer.indices) {
                    buffer[i] = buffer[i].coerceIn(-0.9f, 0.9f)
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playBuffer(buffer: FloatArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        
        val trackSize = maxOf(minBufferSize, buffer.size * 4)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(trackSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_NON_BLOCKING)
        audioTrack.play()

        // Asynchronously release track resources when the sound is finished playing
        scope.launch {
            val playDelay = (buffer.size * 1000L) / SAMPLE_RATE + 100L
            kotlinx.coroutines.delay(playDelay)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Handle dynamic release bounds
            }
        }
    }
}
