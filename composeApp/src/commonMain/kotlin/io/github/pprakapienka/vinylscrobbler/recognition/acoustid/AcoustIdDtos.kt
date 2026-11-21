package io.github.pprakapienka.vinylscrobbler.recognition.acoustid

import kotlinx.serialization.Serializable

@Serializable
internal data class AcoustIdResponse(
    val status: String,
    val results: List<AcoustIdResult> = emptyList(),
    val error: AcoustIdError? = null,
)

@Serializable
internal data class AcoustIdResult(
    val id: String,
    val score: Double,
    val recordings: List<AcoustIdRecording> = emptyList(),
)

@Serializable
internal data class AcoustIdRecording(
    val id: String,
    val title: String? = null,
    val duration: Int? = null,
    val artists: List<AcoustIdArtist> = emptyList(),
    val releasegroups: List<AcoustIdReleaseGroup> = emptyList(),
)

@Serializable
internal data class AcoustIdArtist(
    val id: String,
    val name: String,
)

@Serializable
internal data class AcoustIdReleaseGroup(
    val id: String,
    val title: String,
    val type: String? = null,
)

@Serializable
internal data class AcoustIdError(
    val code: Int? = null,
    val message: String? = null,
)
