package vinylscrobbler

import vinylscrobbler.lastfm.ScrobblingPolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrobblingPolicyTest {
    @Test fun shortTrackRejected() {
        assertFalse(ScrobblingPolicy.isEligible(20_000, 15_000))
    }
    @Test fun halfDurationQualifies() {
        assertTrue(ScrobblingPolicy.isEligible(180_000, 90_000))
    }
    @Test fun fourMinuteCap() {
        assertTrue(ScrobblingPolicy.isEligible(600_000, 240_000))
        assertFalse(ScrobblingPolicy.isEligible(600_000, 200_000))
    }
}
