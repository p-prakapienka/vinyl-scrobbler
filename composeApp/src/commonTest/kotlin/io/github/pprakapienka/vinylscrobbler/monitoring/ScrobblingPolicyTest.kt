package io.github.pprakapienka.vinylscrobbler.monitoring

import kotlin.test.Test
import kotlin.test.assertEquals

class ScrobblingPolicyTest {
    private val policy = ScrobblingPolicy()

    @Test
    fun `rejects tracks that are thirty seconds long`() {
        assertEquals(
            ScrobbleEligibility.TrackTooShort,
            policy.evaluate(trackDurationSeconds = 30, listenedSeconds = 30),
        )
    }

    @Test
    fun `requires half of a short track`() {
        assertEquals(
            ScrobbleEligibility.MoreListeningRequired(remainingSeconds = 1),
            policy.evaluate(trackDurationSeconds = 180, listenedSeconds = 89),
        )
        assertEquals(
            ScrobbleEligibility.Eligible,
            policy.evaluate(trackDurationSeconds = 180, listenedSeconds = 90),
        )
    }

    @Test
    fun `rounds half of an odd duration up to a complete second`() {
        assertEquals(
            ScrobbleEligibility.MoreListeningRequired(remainingSeconds = 1),
            policy.evaluate(trackDurationSeconds = 181, listenedSeconds = 90),
        )
        assertEquals(
            ScrobbleEligibility.Eligible,
            policy.evaluate(trackDurationSeconds = 181, listenedSeconds = 91),
        )
    }

    @Test
    fun `caps required listening time at four minutes`() {
        assertEquals(
            ScrobbleEligibility.MoreListeningRequired(remainingSeconds = 1),
            policy.evaluate(trackDurationSeconds = 900, listenedSeconds = 239),
        )
        assertEquals(
            ScrobbleEligibility.Eligible,
            policy.evaluate(trackDurationSeconds = 900, listenedSeconds = 240),
        )
    }
}
