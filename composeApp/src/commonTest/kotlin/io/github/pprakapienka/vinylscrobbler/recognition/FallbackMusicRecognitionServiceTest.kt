package io.github.pprakapienka.vinylscrobbler.recognition

import io.github.pprakapienka.vinylscrobbler.catalog.FileCatalog
import io.github.pprakapienka.vinylscrobbler.catalog.MemoryCatalogStore
import io.github.pprakapienka.vinylscrobbler.model.Track
import io.github.pprakapienka.vinylscrobbler.recognition.acoustid.AcoustIdException
import io.github.pprakapienka.vinylscrobbler.recognition.catalog.CatalogMusicRecognitionService
import io.github.pprakapienka.vinylscrobbler.recognition.chromaprint.ChromaprintEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FallbackMusicRecognitionServiceTest {
    private val query = AudioFingerprint(encoded = ChromaprintEngine.encode(REFERENCE), sourceDurationSeconds = 20)
    private val owned = Track(artist = "M83", title = "Midnight City", album = "Hurry Up, We're Dreaming")
    private val weak = RecognitionCandidate(
        track = Track(artist = "Someone Else", title = "Other Song", durationSeconds = 180),
        score = 0.2,
        recordingId = "weak",
    )
    private val strong = weak.copy(
        score = FallbackMusicRecognitionService.USEFUL_SCORE,
        track = Track(artist = "M83", title = "Midnight City", durationSeconds = 243),
        recordingId = "strong",
    )

    @Test
    fun `keeps a strong AcoustID hit`() = runTest {
        val fallback = countingCatalog()
        val service = FallbackMusicRecognitionService(scripted(listOf(strong)), fallback)

        val result = service.recognize(query)

        assertEquals("strong", result.single().recordingId)
        assertEquals(0, fallback.calls)
    }

    @Test
    fun `falls through a low-confidence AcoustID result`() = runTest {
        val fallback = countingCatalog(saved = true)
        val service = FallbackMusicRecognitionService(scripted(listOf(weak)), fallback)

        val result = service.recognize(query)

        assertEquals("Midnight City", result.single().track.title)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun `falls through when AcoustID returns nothing`() = runTest {
        val fallback = countingCatalog(saved = true)
        val service = FallbackMusicRecognitionService(scripted(emptyList()), fallback)

        assertEquals("Midnight City", service.recognize(query).single().track.title)
    }

    @Test
    fun `falls through when AcoustID cannot be reached`() = runTest {
        val fallback = countingCatalog(saved = true)
        val service = FallbackMusicRecognitionService(
            scripted(failure = AcoustIdException("offline")),
            fallback,
        )

        assertEquals("Midnight City", service.recognize(query).single().track.title)
    }

    @Test
    fun `keeps a weak AcoustID candidate when the catalog misses`() = runTest {
        val fallback = countingCatalog(saved = false)
        val service = FallbackMusicRecognitionService(scripted(listOf(weak)), fallback)

        assertEquals("Other Song", service.recognize(query).single().track.title)
        assertTrue(fallback.calls == 1)
    }

    @Test
    fun `reports an unreachable AcoustID when the catalog also misses`() = runTest {
        val service = FallbackMusicRecognitionService(
            scripted(failure = AcoustIdException("offline")),
            countingCatalog(saved = false),
        )

        assertFailsWith<AcoustIdException> {
            service.recognize(query)
        }
    }

    private fun scripted(
        results: List<RecognitionCandidate> = emptyList(),
        failure: Exception? = null,
    ): MusicRecognitionService = object : MusicRecognitionService {
        override suspend fun recognize(fingerprint: AudioFingerprint): List<RecognitionCandidate> {
            failure?.let { throw it }
            return results
        }
    }

    private suspend fun countingCatalog(saved: Boolean = false): CountingRecognitionService {
        val catalog = FileCatalog(MemoryCatalogStore()) { "id" }
        if (saved) {
            catalog.add(owned, query)
        }
        return CountingRecognitionService(CatalogMusicRecognitionService(catalog))
    }

    private class CountingRecognitionService(
        private val delegate: MusicRecognitionService,
    ) : MusicRecognitionService {
        var calls: Int = 0

        override suspend fun recognize(fingerprint: AudioFingerprint): List<RecognitionCandidate> {
            calls++
            return delegate.recognize(fingerprint)
        }
    }

    private companion object {
        val REFERENCE = IntArray(40) { index -> 1 shl (index % 16) }
    }
}
