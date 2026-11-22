package io.github.pprakapienka.vinylscrobbler.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChromaprintCatalogMatcherTest {
    @Test
    fun `matches a slice of the same fingerprint`() {
        val reference = IntArray(40) { index -> 1 shl (index % 16) }
        val slice = reference.copyOfRange(8, 28)
        assertEquals(1.0, ChromaprintCatalogMatcher.similarity(slice, reference))
        assertEquals(1.0, ChromaprintCatalogMatcher.similarity(reference, slice))
    }

    @Test
    fun `tolerates two bit errors and rejects three`() {
        val reference = IntArray(32) { index -> 1 shl (index % 16) }
        val close = IntArray(reference.size) { index -> reference[index] xor 0b11 }
        val far = IntArray(reference.size) { index -> reference[index] xor 0b111 }
        assertTrue(ChromaprintCatalogMatcher.similarity(close, reference) >= ChromaprintCatalogMatcher.MINIMUM_SCORE)
        assertTrue(ChromaprintCatalogMatcher.similarity(far, reference) < ChromaprintCatalogMatcher.MINIMUM_SCORE)
    }

    @Test
    fun `misses an unrelated fingerprint`() {
        val reference = IntArray(32) { index -> 1 shl (index % 16) }
        val other = IntArray(32) { index -> -1 - index * 997 }
        assertTrue(ChromaprintCatalogMatcher.similarity(other, reference) < ChromaprintCatalogMatcher.MINIMUM_SCORE)
    }

    @Test
    fun `ignores a fingerprint shorter than the overlap`() {
        val reference = IntArray(32) { index -> index }
        assertEquals(0.0, ChromaprintCatalogMatcher.similarity(intArrayOf(1, 2, 3), reference))
    }
}
