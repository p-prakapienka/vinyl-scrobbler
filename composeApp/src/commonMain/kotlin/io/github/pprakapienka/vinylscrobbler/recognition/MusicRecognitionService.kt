package io.github.pprakapienka.vinylscrobbler.recognition

interface MusicRecognitionService {
    suspend fun recognize(fingerprint: AudioFingerprint): List<RecognitionCandidate>
}
