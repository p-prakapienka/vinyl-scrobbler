package io.github.pprakapienka.vinylscrobbler.recognition

data class AudioFingerprint(
    val encoded: String,
    val sourceDurationSeconds: Int,
)
