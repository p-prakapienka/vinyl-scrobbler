package io.github.pprakapienka.vinylscrobbler.recognition

import kotlinx.coroutines.CancellationException

class FallbackMusicRecognitionService(
    private val primary: MusicRecognitionService,
    private val fallback: MusicRecognitionService,
    private val usefulScore: Double = USEFUL_SCORE,
) : MusicRecognitionService {
    override suspend fun recognize(fingerprint: AudioFingerprint): List<RecognitionCandidate> {
        val primaryResults = try {
            primary.recognize(fingerprint)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val local = fallback.recognize(fingerprint)
            if (local.isNotEmpty()) return local
            throw error
        }
        if (primaryResults.any { it.score >= usefulScore }) return primaryResults
        val local = fallback.recognize(fingerprint)
        return local.ifEmpty { primaryResults }
    }

    companion object {
        const val USEFUL_SCORE = 0.5
    }
}
