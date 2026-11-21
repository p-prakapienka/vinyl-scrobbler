package io.github.pprakapienka.vinylscrobbler.recognition

interface AudioFingerprinter {
    suspend fun fingerprint(audio: PcmAudio): AudioFingerprint
}
