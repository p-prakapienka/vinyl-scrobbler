package io.github.pprakapienka.vinylscrobbler.recognition.spike

import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio
import io.github.pprakapienka.vinylscrobbler.recognition.chromaprint.ChromaprintFingerprinter
import kotlin.time.TimeSource

data class CaptureSlice(
    val name: String,
    val offsetSamples: Int,
    val lengthSamples: Int,
)

data class SliceObservation(
    val name: String,
    val rawItems: Int,
    val elapsedMillis: Long,
)

class PartialCaptureStudy(
    private val fingerprinter: ChromaprintFingerprinter = ChromaprintFingerprinter(),
) {
    fun observe(audio: PcmAudio, slices: List<CaptureSlice>): List<SliceObservation> =
        slices.map { slice ->
            val piece = sliceOf(audio, slice.offsetSamples, slice.lengthSamples)
            val started = TimeSource.Monotonic.markNow()
            val raw = fingerprinter.rawNow(piece)
            SliceObservation(
                name = slice.name,
                rawItems = raw.size,
                elapsedMillis = started.elapsedNow().inWholeMilliseconds,
            )
        }

    fun rawSlice(audio: PcmAudio, offsetSamples: Int, lengthSamples: Int): IntArray =
        fingerprinter.rawNow(sliceOf(audio, offsetSamples, lengthSamples))

    companion object {
        fun sliceOf(audio: PcmAudio, offsetSamples: Int, lengthSamples: Int): PcmAudio {
            val frameSize = audio.channels
            val start = (offsetSamples * frameSize).coerceIn(0, audio.samples.size)
            val end = (start + lengthSamples * frameSize).coerceAtMost(audio.samples.size)
            return PcmAudio(audio.samples.copyOfRange(start, end), audio.sampleRate, audio.channels)
        }

        fun alignedSampleOffset(sampleRate: Int, seconds: Double): Int {
            val hop = ChromaprintFingerprinter.HOP_SAMPLES * (sampleRate / ChromaprintFingerprinter.WORKING_RATE).coerceAtLeast(1)
            val samples = (seconds * sampleRate).toInt()
            return samples - (samples % hop)
        }

        fun contiguousIndex(needle: IntArray, haystack: IntArray): Int {
            if (needle.isEmpty() || needle.size > haystack.size) return -1
            for (start in 0..haystack.size - needle.size) {
                var same = true
                for (index in needle.indices) {
                    if (needle[index] != haystack[start + index]) {
                        same = false
                        break
                    }
                }
                if (same) return start
            }
            return -1
        }
    }
}
