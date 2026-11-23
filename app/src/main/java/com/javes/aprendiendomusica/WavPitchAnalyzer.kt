package com.javes.aprendiendomusica

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

object WavPitchAnalyzer {
    data class Result(val notes: List<String>, val sampleRate: Int, val durationSeconds: Int)

    fun analyze(bytes: ByteArray): Result {
        require(bytes.size >= 44) { "El archivo WAV está incompleto." }
        fun ascii(pos: Int, count: Int) = String(bytes, pos, count, Charsets.US_ASCII)
        fun u16(pos: Int) = (bytes[pos].toInt() and 255) or ((bytes[pos + 1].toInt() and 255) shl 8)
        fun i32(pos: Int) = (bytes[pos].toInt() and 255) or ((bytes[pos + 1].toInt() and 255) shl 8) or
            ((bytes[pos + 2].toInt() and 255) shl 16) or ((bytes[pos + 3].toInt() and 255) shl 24)
        require(ascii(0, 4) == "RIFF" && ascii(8, 4) == "WAVE") { "Formato no válido. Selecciona un WAV PCM." }
        var offset = 12
        var format = 0
        var channels = 0
        var rate = 0
        var bits = 0
        var dataStart = -1
        var dataSize = 0
        while (offset + 8 <= bytes.size - 1) {
            val id = ascii(offset, 4)
            val size = i32(offset + 4).coerceAtLeast(0)
            val start = offset + 8
            if (id == "fmt " && size >= 16 && start + 16 <= bytes.size) {
                format = u16(start); channels = u16(start + 2); rate = i32(start + 4); bits = u16(start + 14)
            } else if (id == "data") {
                dataStart = start; dataSize = minOf(size, bytes.size - start); break
            }
            offset = start + size + (size % 2)
        }
        require(format == 1 && bits == 16 && channels in 1..2 && dataStart >= 0 && rate > 0) {
            "La detección requiere WAV PCM de 16 bits, mono o estéreo."
        }
        val frameBytes = channels * 2
        val sampleCount = dataSize / frameBytes
        val factor = (rate / 11025).coerceAtLeast(1)
        val effectiveRate = rate / factor
        val frame = 2048
        val hop = 4096
        val minLag = (effectiveRate / 1000).coerceAtLeast(2)
        val maxLag = (effectiveRate / 55).coerceAtMost(frame / 2)
        val detected = mutableListOf<String>()
        var processed = 0
        var pos = 0
        while (pos + (frame - 1) * factor < sampleCount && processed < 500) {
            val samples = DoubleArray(frame)
            var mean = 0.0
            for (i in 0 until frame) {
                val sampleIndex = pos + i * factor
                var sum = 0
                for (ch in 0 until channels) {
                    val p = dataStart + sampleIndex * frameBytes + ch * 2
                    val raw = (bytes[p].toInt() and 255) or (bytes[p + 1].toInt() shl 8)
                    sum += raw.toShort().toInt()
                }
                samples[i] = sum.toDouble() / channels
                mean += samples[i]
            }
            mean /= frame
            var energy = 0.0
            for (i in samples.indices) {
                samples[i] = (samples[i] - mean) * (0.5 - 0.5 * kotlin.math.cos(2.0 * Math.PI * i / (frame - 1)))
                energy += samples[i] * samples[i]
            }
            if (energy > 1.0e8) {
                var bestLag = 0
                var bestCorrelation = Double.NEGATIVE_INFINITY
                for (lag in minLag..maxLag) {
                    var corr = 0.0
                    for (i in 0 until frame - lag) corr += samples[i] * samples[i + lag]
                    if (corr > bestCorrelation) { bestCorrelation = corr; bestLag = lag }
                }
                if (bestLag > 0) {
                    val frequency = effectiveRate.toDouble() / bestLag
                    if (frequency in 55.0..1000.0) {
                        val midi = (69 + 12 * (ln(frequency / 440.0) / ln(2.0))).roundToInt()
                        val names = listOf("Do","Do♯","Re","Re♯","Mi","Fa","Fa♯","Sol","Sol♯","La","La♯","Si")
                        val name = "${names[Math.floorMod(midi, 12)]} ${midi / 12 - 1}"
                        if (detected.lastOrNull() != name) detected.add(name)
                    }
                }
            }
            pos += hop
            processed++
        }
        return Result(detected.take(80), rate, sampleCount / rate)
    }
}
