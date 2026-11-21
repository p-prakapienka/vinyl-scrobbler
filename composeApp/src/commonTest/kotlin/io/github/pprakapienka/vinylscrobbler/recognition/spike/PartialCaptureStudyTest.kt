package io.github.pprakapienka.vinylscrobbler.recognition.spike

import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio
import io.github.pprakapienka.vinylscrobbler.recognition.chromaprint.ChromaprintFingerprinter
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartialCaptureStudyTest {
    private val fingerprinter = ChromaprintFingerprinter()
    private val study = PartialCaptureStudy(fingerprinter)

    @Test
    fun `start slices are prefixes and middle slices sit inside the full fingerprint`() {
        val records = List(5) { index -> record(index) }
        val lengths = listOf(5, 10, 20)
        var falsePositiveWindows = 0
        val timings = mutableListOf<Long>()
        val startItems = mutableListOf<Int>()

        records.forEachIndexed { index, audio ->
            val full = fingerprinter.rawNow(audio)
            assertEquals(221, full.size, "record $index")
            val started = kotlin.time.TimeSource.Monotonic.markNow()
            fingerprinter.rawNow(audio)
            timings += started.elapsedNow().inWholeMilliseconds

            for (seconds in lengths) {
                val length = seconds * audio.sampleRate
                val start = study.rawSlice(audio, 0, length)
                assertTrue(start.isNotEmpty(), "start ${seconds}s of record $index was empty")
                if (index == 0) startItems += start.size
                assertTrue(
                    start.indices.all { start[it] == full[it] },
                    "start ${seconds}s of record $index was not a prefix",
                )

                val offset = PartialCaptureStudy.alignedSampleOffset(audio.sampleRate, 8.0)
                val middle = study.rawSlice(audio, offset, length)
                val hops = offset / ChromaprintFingerprinter.HOP_SAMPLES
                val at = PartialCaptureStudy.contiguousIndex(middle, full)
                assertEquals(
                    hops,
                    at,
                    "middle ${seconds}s of record $index aligned at $at, expected $hops",
                )
            }
        }

        for (a in records.indices) {
            for (b in records.indices) {
                if (a == b) continue
                val slice = study.rawSlice(records[a], 0, 10 * records[a].sampleRate)
                val other = fingerprinter.rawNow(records[b])
                if (PartialCaptureStudy.contiguousIndex(slice, other) >= 0) falsePositiveWindows++
            }
        }

        assertEquals(0, falsePositiveWindows)
        assertEquals(listOf(19, 59, 140), startItems)
        assertTrue(timings.all { it < 2_000 }, "fingerprint of 30s took ${timings.maxOrNull()} ms")
    }

    private fun record(index: Int): PcmAudio {
        val sampleRate = 11_025
        val seconds = 30
        val notes = MELODIES[index]
        val samples = ShortArray(sampleRate * seconds) { frame ->
            val second = frame / sampleRate
            val frequency = notes[second % notes.size]
            val mixed = 12_000.0 * sin(2.0 * PI * frequency * frame / sampleRate) +
                3_000.0 * sin(2.0 * PI * frequency * 2 * frame / sampleRate + index)
            mixed.toInt().toShort()
        }
        return PcmAudio(samples, sampleRate, 1)
    }

    private companion object {
        val MELODIES = listOf(
            doubleArrayOf(196.0, 247.0, 294.0, 330.0, 392.0, 440.0),
            doubleArrayOf(220.0, 277.0, 330.0, 370.0, 415.0, 494.0),
            doubleArrayOf(174.6, 233.1, 311.1, 349.2, 466.2, 523.3),
            doubleArrayOf(146.8, 196.0, 261.6, 329.6, 392.0, 493.9),
            doubleArrayOf(130.8, 164.8, 196.0, 246.9, 329.6, 392.0),
        )
    }
}
