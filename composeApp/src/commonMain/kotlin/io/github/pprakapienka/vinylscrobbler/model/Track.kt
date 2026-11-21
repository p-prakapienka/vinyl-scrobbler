package io.github.pprakapienka.vinylscrobbler.model

data class Track(
    val artist: String,
    val title: String,
    val album: String? = null,
    val durationSeconds: Int? = null,
    val musicBrainzId: String? = null,
)
