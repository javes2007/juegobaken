package com.javes.aprendiendomusica

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Instrument(val label: String, val emoji: String) {
    PIANO("Piano", "🎹"), BASS("Bajo", "🎸"), ACCORDION("Acordeón", "🪗"),
    DRUMS("Batería", "🥁"), CONGAS("Congas", "🪘")
}

object MusicEngine {
    private const val RATE = 44100
    fun play(frequency: Double, instrument: Instrument, durationMs: Int = 420) {
        Thread {
            var track: AudioTrack? = null
            try {
                val count = RATE * durationMs / 1000
                val data = ShortArray(count)
                val harmonics = when (instrument) {
                    Instrument.PIANO -> doubleArrayOf(1.0, .45, .22, .10, .04)
                    Instrument.BASS -> doubleArrayOf(1.0, .24, .08)
                    Instrument.ACCORDION -> doubleArrayOf(1.0, .8, .48, .2)
                    Instrument.DRUMS -> doubleArrayOf(1.0, .2)
                    Instrument.CONGAS -> doubleArrayOf(1.0, .25)
                }
                for (i in data.indices) {
                    val t = i.toDouble() / RATE
                    var wave = 0.0
                    harmonics.forEachIndexed { n, amp -> wave += amp * sin(2.0 * PI * frequency * (n + 1) * t) }
                    val attack = (i / (RATE * .012)).coerceIn(0.0, 1.0)
                    val release = ((count - i).toDouble() / (RATE * .08)).coerceIn(0.0, 1.0)
                    val decay = if (instrument == Instrument.DRUMS || instrument == Instrument.CONGAS) exp(-t * 15.0) else 1.0
                    data[i] = (wave / harmonics.sum() * minOf(attack, release) * decay * 24000).toInt().coerceIn(-32768,32767).toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(data.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(data, 0, data.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 50)
            } catch (_: Exception) {
            } finally {
                try { track?.stop() } catch (_: Exception) { }
                track?.release()
            }
        }.start()
    }
}
