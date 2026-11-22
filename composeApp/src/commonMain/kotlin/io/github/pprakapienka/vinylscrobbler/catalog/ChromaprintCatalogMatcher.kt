package io.github.pprakapienka.vinylscrobbler.catalog

import kotlin.math.min

object ChromaprintCatalogMatcher {
    const val MAX_BIT_ERRORS = 2
    const val MIN_OVERLAP = 8
    const val MINIMUM_SCORE = 0.9

    fun similarity(query: IntArray, reference: IntArray): Double {
        if (query.size < MIN_OVERLAP || reference.size < MIN_OVERLAP) return 0.0
        var best = 0.0
        for (offset in (1 - query.size) until reference.size) {
            val queryStart = if (offset < 0) -offset else 0
            val referenceStart = if (offset > 0) offset else 0
            val overlap = min(query.size - queryStart, reference.size - referenceStart)
            if (overlap < MIN_OVERLAP) continue
            var matches = 0
            for (index in 0 until overlap) {
                val difference = query[queryStart + index] xor reference[referenceStart + index]
                if (difference.countOneBits() <= MAX_BIT_ERRORS) matches++
            }
            val score = matches.toDouble() / overlap
            if (score > best) best = score
        }
        return best
    }
}
