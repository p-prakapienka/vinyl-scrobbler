package io.github.pprakapienka.vinylscrobbler.recognition

data class PcmAudio(
    val samples: ShortArray,
    val sampleRate: Int,
    val channels: Int,
) {
    init {
        require(sampleRate > 0) { "Sample rate must be positive" }
        require(channels in 1..2) { "Only mono and stereo are supported" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PcmAudio) return false
        return samples.contentEquals(other.samples) && sampleRate == other.sampleRate && channels == other.channels
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        return result
    }
}
