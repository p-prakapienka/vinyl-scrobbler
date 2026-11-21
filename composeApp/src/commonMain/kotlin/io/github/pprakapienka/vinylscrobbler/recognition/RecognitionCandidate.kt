package io.github.pprakapienka.vinylscrobbler.recognition

import io.github.pprakapienka.vinylscrobbler.model.Track

data class RecognitionCandidate(
    val track: Track,
    val score: Double,
    val recordingId: String?,
)
