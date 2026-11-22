package io.github.pprakapienka.vinylscrobbler.catalog

import io.github.pprakapienka.vinylscrobbler.model.Track
import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import kotlin.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class FileCatalog(
    private val store: CatalogStore,
    private val ids: () -> String = { Uuid.random().toString() },
) : Catalog {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun tracks(): List<CatalogTrack> = mutex.withLock {
        load().tracks.map { it.toPublic() }
    }

    override suspend fun add(track: Track, fingerprint: AudioFingerprint): CatalogTrack = mutex.withLock {
        val artist = track.artist.trim()
        val title = track.title.trim()
        val album = track.album?.trim()?.ifEmpty { null }
        require(artist.isNotEmpty()) { "Artist must not be blank" }
        require(title.isNotEmpty()) { "Title must not be blank" }
        require(fingerprint.encoded.isNotBlank()) { "Fingerprint must not be blank" }
        track.durationSeconds?.let { duration ->
            require(duration > 0) { "Duration must be positive" }
        }
        val document = load()
        val existing = document.tracks.indexOfFirst { stored ->
            stored.artist == artist && stored.title == title && stored.album == album
        }
        val storedFingerprint = StoredFingerprint(fingerprint.encoded, fingerprint.sourceDurationSeconds)
        val saved = if (existing < 0) {
            StoredTrack(
                id = ids(),
                artist = artist,
                title = title,
                album = album,
                durationSeconds = track.durationSeconds,
                musicBrainzId = track.musicBrainzId,
                fingerprints = listOf(storedFingerprint),
            )
        } else {
            val current = document.tracks[existing]
            val fingerprints = if (current.fingerprints.any { it.encoded == storedFingerprint.encoded }) {
                current.fingerprints
            } else {
                (current.fingerprints + storedFingerprint).takeLast(MAX_FINGERPRINTS_PER_TRACK)
            }
            current.copy(
                durationSeconds = track.durationSeconds ?: current.durationSeconds,
                musicBrainzId = current.musicBrainzId ?: track.musicBrainzId,
                fingerprints = fingerprints,
            )
        }
        val tracks = if (existing < 0) {
            document.tracks + saved
        } else {
            document.tracks.toMutableList().apply { this[existing] = saved }
        }
        store.writeText(json.encodeToString(CatalogDocument(tracks)))
        saved.toPublic()
    }

    private suspend fun load(): CatalogDocument {
        val text = store.readText()?.takeIf { it.isNotBlank() } ?: return CatalogDocument()
        return json.decodeFromString(text)
    }

    private companion object {
        const val MAX_FINGERPRINTS_PER_TRACK = 8
    }
}

@Serializable
private data class CatalogDocument(
    val tracks: List<StoredTrack> = emptyList(),
)

@Serializable
private data class StoredTrack(
    val id: String,
    val artist: String,
    val title: String,
    val album: String? = null,
    val durationSeconds: Int? = null,
    val musicBrainzId: String? = null,
    val fingerprints: List<StoredFingerprint>,
) {
    fun toPublic(): CatalogTrack = CatalogTrack(
        id = id,
        artist = artist,
        title = title,
        album = album,
        durationSeconds = durationSeconds,
        musicBrainzId = musicBrainzId,
        fingerprints = fingerprints.map { AudioFingerprint(it.encoded, it.durationSeconds) },
    )
}

@Serializable
private data class StoredFingerprint(
    val encoded: String,
    val durationSeconds: Int,
)
