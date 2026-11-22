package io.github.pprakapienka.vinylscrobbler.recognition.catalog

import io.github.pprakapienka.vinylscrobbler.catalog.Catalog
import io.github.pprakapienka.vinylscrobbler.catalog.ChromaprintCatalogMatcher
import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import io.github.pprakapienka.vinylscrobbler.recognition.MusicRecognitionService
import io.github.pprakapienka.vinylscrobbler.recognition.RecognitionCandidate
import io.github.pprakapienka.vinylscrobbler.recognition.chromaprint.ChromaprintEngine

class CatalogMusicRecognitionService(
    private val catalog: Catalog,
    private val minimumScore: Double = ChromaprintCatalogMatcher.MINIMUM_SCORE,
) : MusicRecognitionService {
    override suspend fun recognize(fingerprint: AudioFingerprint): List<RecognitionCandidate> {
        val query = decodeOrNull(fingerprint.encoded) ?: return emptyList()
        return catalog.tracks().mapNotNull { entry ->
            val score = entry.fingerprints.maxOfOrNull { stored ->
                val reference = decodeOrNull(stored.encoded) ?: return@maxOfOrNull 0.0
                ChromaprintCatalogMatcher.similarity(query, reference)
            } ?: 0.0
            if (score < minimumScore) {
                null
            } else {
                RecognitionCandidate(
                    track = entry.toTrack(),
                    score = score,
                    recordingId = entry.musicBrainzId,
                )
            }
        }.sortedByDescending { it.score }
    }

    private fun decodeOrNull(encoded: String): IntArray? = try {
        ChromaprintEngine.decode(encoded)
    } catch (error: IllegalArgumentException) {
        null
    }
}
