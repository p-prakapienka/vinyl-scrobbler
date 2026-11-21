package vinylscrobbler.lastfm

object ScrobblingPolicy {
    /** Last.fm: track > 30s, played >= half duration or 4 minutes (whichever first). */
    fun isEligible(durationMs: Long, playedMs: Long): Boolean {
        if (durationMs <= 30_000) return false
        val half = durationMs / 2
        val threshold = minOf(half, 4 * 60 * 1000)
        return playedMs >= threshold
    }
}
