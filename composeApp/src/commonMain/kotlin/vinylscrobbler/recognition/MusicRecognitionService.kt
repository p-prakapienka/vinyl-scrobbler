package vinylscrobbler.recognition

data class RecognitionObservation(
    val artist: String?,
    val title: String?,
    val confidence: Double,
    val timestampMs: Long
)

interface MusicRecognitionService {
    suspend fun recognize(pcm16Mono: ShortArray, sampleRate: Int): RecognitionObservation?
}
