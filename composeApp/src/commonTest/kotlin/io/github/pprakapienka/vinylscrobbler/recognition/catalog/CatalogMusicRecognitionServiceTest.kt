package io.github.pprakapienka.vinylscrobbler.recognition.catalog

import io.github.pprakapienka.vinylscrobbler.catalog.FileCatalog
import io.github.pprakapienka.vinylscrobbler.catalog.MemoryCatalogStore
import io.github.pprakapienka.vinylscrobbler.model.Track
import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import io.github.pprakapienka.vinylscrobbler.recognition.chromaprint.ChromaprintEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogMusicRecognitionServiceTest {
    @Test
    fun `hits a saved fingerprint and misses a different one`() = runTest {
        val catalog = FileCatalog(MemoryCatalogStore()) { "id" }
        val reference = IntArray(40) { index -> 1 shl (index % 16) }
        val other = IntArray(40) { index -> -1 - index * 997 }
        catalog.add(
            Track(artist = "M83", title = "Midnight City", album = "Hurry Up, We're Dreaming", durationSeconds = 243),
            AudioFingerprint(ChromaprintEngine.encode(reference), sourceDurationSeconds = 20),
        )
        val service = CatalogMusicRecognitionService(catalog)

        val hit = service.recognize(AudioFingerprint(ChromaprintEngine.encode(reference.copyOfRange(8, 28)), 10))
        assertEquals("Midnight City", hit.single().track.title)
        assertEquals("M83", hit.single().track.artist)
        assertEquals(243, hit.single().track.durationSeconds)
        assertTrue(service.recognize(AudioFingerprint(ChromaprintEngine.encode(other), 20)).isEmpty())
    }

    @Test
    fun `matches whichever saved fingerprint lines up`() = runTest {
        val catalog = FileCatalog(MemoryCatalogStore()) { "id" }
        val first = IntArray(24) { index -> index * 17 + 3 }
        val second = IntArray(24) { index -> index * 31 + 9 }
        val track = Track(artist = "M83", title = "Midnight City")
        catalog.add(track, AudioFingerprint(ChromaprintEngine.encode(first), 10))
        catalog.add(track, AudioFingerprint(ChromaprintEngine.encode(second), 10))
        val service = CatalogMusicRecognitionService(catalog)

        val hit = service.recognize(AudioFingerprint(ChromaprintEngine.encode(second), 10))
        assertEquals(1, catalog.tracks().size)
        assertEquals("Midnight City", hit.single().track.title)
    }
}
