package io.github.pprakapienka.vinylscrobbler.recognition.chromaprint

import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChromaprintFingerprinterTest {
    private val fingerprinter = ChromaprintFingerprinter()

    @Test
    fun `compresses a raw fingerprint the way Chromaprint does`() {
        assertEquals("NwAAAkEA", ChromaprintEngine.encode(intArrayOf(1, 0), algorithm = 55))
    }

    @Test
    fun `matches Chromaprint on silence`() {
        val samples = ShortArray(130 * 1024)
        val audio = PcmAudio(samples, sampleRate = 44_100, channels = 1)
        val raw = fingerprinter.rawNow(audio)
        assertEquals(3, raw.size)
        assertTrue(raw.all { it == 627964279 })
        assertEquals("AQAAA0mUaEkSRZEGAA", fingerprinter.fingerprintNow(audio).encoded)
    }

    @Test
    fun `matches Chromaprint on a two-tone recording`() {
        val sampleRate = 11_025
        val samples = ShortArray(sampleRate * 12) { index ->
            val t = index.toDouble() / sampleRate
            val mixed = 16_000.0 * sin(2.0 * PI * 440.0 * t) + 4_000.0 * sin(2.0 * PI * 554.37 * t)
            mixed.toInt().toShort()
        }
        val fingerprint = fingerprinter.fingerprintNow(PcmAudio(samples, sampleRate, 1))
        assertEquals(EXPECTED_TONE, fingerprint.encoded)
    }

    @Test
    fun `decodes a Chromaprint fingerprint back to the raw items`() {
        assertTrue(ChromaprintEngine.decode("NwAAAkEA").contentEquals(intArrayOf(1, 0)))
        val silence = ChromaprintEngine.decode("AQAAA0mUaEkSRZEGAA")
        assertEquals(3, silence.size)
        assertTrue(silence.all { it == 627964279 })
        val raw = intArrayOf(0, 1, -1, 627964279, 1 shl 31, 0x00ff00ff, 42)
        assertTrue(ChromaprintEngine.decode(ChromaprintEngine.encode(raw)).contentEquals(raw))
        assertTrue(ChromaprintEngine.decode(EXPECTED_TONE).isNotEmpty())
    }

    private companion object {
        const val EXPECTED_TONE = "AQAAS0mUaEmSTQQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    }
}
