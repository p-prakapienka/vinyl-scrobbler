package io.github.pprakapienka.vinylscrobbler.catalog

import androidx.compose.runtime.Composable
import io.github.pprakapienka.vinylscrobbler.model.Track
import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint

data class CatalogTrack(
    val id: String,
    val artist: String,
    val title: String,
    val album: String?,
    val durationSeconds: Int?,
    val musicBrainzId: String?,
    val fingerprints: List<AudioFingerprint>,
) {
    fun toTrack(): Track = Track(
        artist = artist,
        title = title,
        album = album,
        durationSeconds = durationSeconds,
        musicBrainzId = musicBrainzId,
    )
}

interface Catalog {
    suspend fun tracks(): List<CatalogTrack>

    suspend fun add(track: Track, fingerprint: AudioFingerprint): CatalogTrack
}

interface CatalogStore {
    suspend fun readText(): String?

    suspend fun writeText(text: String)
}

@Composable
expect fun rememberCatalog(): Catalog
