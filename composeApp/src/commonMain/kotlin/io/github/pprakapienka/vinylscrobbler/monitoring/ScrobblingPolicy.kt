package io.github.pprakapienka.vinylscrobbler.monitoring

sealed interface ScrobbleEligibility {
    data object TrackTooShort : ScrobbleEligibility

    data class MoreListeningRequired(
        val remainingSeconds: Int,
    ) : ScrobbleEligibility

    data object Eligible : ScrobbleEligibility
}

class ScrobblingPolicy {
    fun evaluate(
        trackDurationSeconds: Int,
        listenedSeconds: Int,
    ): ScrobbleEligibility {
        require(trackDurationSeconds > 0) { "Track duration must be positive" }
        require(listenedSeconds >= 0) { "Listened time must not be negative" }

        if (trackDurationSeconds <= MINIMUM_TRACK_DURATION_SECONDS) {
            return ScrobbleEligibility.TrackTooShort
        }

        val requiredSeconds = minOf(
            (trackDurationSeconds + 1) / 2,
            MAXIMUM_REQUIRED_LISTENING_SECONDS,
        )

        if (listenedSeconds >= requiredSeconds) {
            return ScrobbleEligibility.Eligible
        }

        return ScrobbleEligibility.MoreListeningRequired(
            remainingSeconds = requiredSeconds - listenedSeconds,
        )
    }

    private companion object {
        const val MINIMUM_TRACK_DURATION_SECONDS = 30
        const val MAXIMUM_REQUIRED_LISTENING_SECONDS = 4 * 60
    }
}
