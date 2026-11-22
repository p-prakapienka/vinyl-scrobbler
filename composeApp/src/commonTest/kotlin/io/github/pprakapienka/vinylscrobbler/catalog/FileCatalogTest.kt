package io.github.pprakapienka.vinylscrobbler.catalog

import io.github.pprakapienka.vinylscrobbler.model.Track
import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileCatalogTest {
    @Test
    fun `adds a track and reloads it without the recording`() = runTest {
        val store = MemoryCatalogStore()
        var next = 0
        val catalog = FileCatalog(store) { "id-${next++}" }
        val fingerprint = AudioFingerprint(encoded = "AQAAA0mUaEkSRZEGAA", sourceDurationSeconds = 20)

        val saved = catalog.add(
            Track(artist = " M83 ", title = " Midnight City ", album = " Hurry Up, We're Dreaming ", durationSeconds = 243),
            fingerprint,
        )

        assertEquals("id-0", saved.id)
        assertEquals("M83", saved.artist)
        assertEquals(1, saved.fingerprints.size)
        val text = store.text!!
        assertTrue(text.contains("AQAAA0mUaEkSRZEGAA"))
        assertTrue(text.contains("M83"))
        assertFalse(text.contains("samples"))
        val reloaded = FileCatalog(store) { "unused" }
        assertEquals(saved, reloaded.tracks().single())
    }

    @Test
    fun `appends another fingerprint to the same track and drops the oldest past eight`() = runTest {
        val catalog = FileCatalog(MemoryCatalogStore()) { "id" }
        val track = Track(artist = "M83", title = "Midnight City", album = "Hurry Up, We're Dreaming")
        repeat(9) { index ->
            catalog.add(track, AudioFingerprint(encoded = "fp-$index", sourceDurationSeconds = 10))
        }

        val saved = catalog.tracks().single()
        assertEquals(8, saved.fingerprints.size)
        assertEquals("fp-1", saved.fingerprints.first().encoded)
        assertEquals("fp-8", saved.fingerprints.last().encoded)
    }

    @Test
    fun `does not store the same fingerprint twice`() = runTest {
        val catalog = FileCatalog(MemoryCatalogStore()) { "id" }
        val track = Track(artist = "M83", title = "Midnight City")
        val fingerprint = AudioFingerprint(encoded = "same", sourceDurationSeconds = 10)
        catalog.add(track, fingerprint)
        catalog.add(track, fingerprint)
        assertEquals(1, catalog.tracks().single().fingerprints.size)
    }
}
