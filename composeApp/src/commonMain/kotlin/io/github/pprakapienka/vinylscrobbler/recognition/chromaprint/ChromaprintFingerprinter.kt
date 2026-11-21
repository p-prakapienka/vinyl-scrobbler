package io.github.pprakapienka.vinylscrobbler.recognition.chromaprint

import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprinter
import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class InspectedFingerprint(
    val fingerprint: AudioFingerprint,
    val rawItemCount: Int,
)

class ChromaprintFingerprinter : AudioFingerprinter {
    override suspend fun fingerprint(audio: PcmAudio): AudioFingerprint =
        withContext(Dispatchers.Default) { fingerprintNow(audio) }

    fun fingerprintNow(audio: PcmAudio): AudioFingerprint = inspectNow(audio).fingerprint

    fun inspectNow(audio: PcmAudio): InspectedFingerprint {
        val raw = rawNow(audio)
        return InspectedFingerprint(
            fingerprint = AudioFingerprint(
                encoded = ChromaprintEngine.encode(raw),
                sourceDurationSeconds = durationSeconds(audio),
            ),
            rawItemCount = raw.size,
        )
    }

    fun rawNow(audio: PcmAudio): IntArray = ChromaprintEngine.rawFingerprint(workingSamples(audio))

    private fun workingSamples(audio: PcmAudio): ShortArray {
        val mono = downmix(audio)
        return resampleToWorkingRate(mono, audio.sampleRate)
    }

    private fun durationSeconds(audio: PcmAudio): Int {
        val frames = audio.samples.size / audio.channels
        if (frames == 0) return 0
        return (frames.toDouble() / audio.sampleRate).roundToInt().coerceAtLeast(1)
    }

    companion object {
        const val WORKING_RATE = ChromaprintEngine.SAMPLE_RATE
        const val HOP_SAMPLES = ChromaprintEngine.HOP
    }
}

internal fun downmix(audio: PcmAudio): ShortArray {
    if (audio.channels == 1) return audio.samples.copyOf()
    val frames = audio.samples.size / audio.channels
    return ShortArray(frames) { frame ->
        var sum = 0
        for (channel in 0 until audio.channels) {
            sum += audio.samples[frame * audio.channels + channel].toInt()
        }
        (sum / audio.channels).toShort()
    }
}

internal fun resampleToWorkingRate(mono: ShortArray, sampleRate: Int): ShortArray {
    require(sampleRate > 0)
    if (sampleRate == ChromaprintEngine.SAMPLE_RATE) return mono
    if (sampleRate % ChromaprintEngine.SAMPLE_RATE == 0) {
        val factor = sampleRate / ChromaprintEngine.SAMPLE_RATE
        return ShortArray(mono.size / factor) { index ->
            var sum = 0
            val base = index * factor
            for (offset in 0 until factor) sum += mono[base + offset].toInt()
            (sum / factor).toShort()
        }
    }
    val outLength = (mono.size.toLong() * ChromaprintEngine.SAMPLE_RATE / sampleRate).toInt()
    if (outLength <= 0 || mono.isEmpty()) return ShortArray(0)
    val step = sampleRate.toDouble() / ChromaprintEngine.SAMPLE_RATE
    return ShortArray(outLength) { index ->
        val source = index * step
        val left = source.toInt().coerceAtMost(mono.lastIndex)
        val right = (left + 1).coerceAtMost(mono.lastIndex)
        val fraction = source - left
        val mixed = mono[left] * (1.0 - fraction) + mono[right] * fraction
        mixed.toInt().toShort()
    }
}
