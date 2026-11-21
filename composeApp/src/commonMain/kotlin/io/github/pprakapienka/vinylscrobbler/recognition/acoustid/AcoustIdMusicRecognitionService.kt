package io.github.pprakapienka.vinylscrobbler.recognition.acoustid

import io.github.pprakapienka.vinylscrobbler.model.Track
import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import io.github.pprakapienka.vinylscrobbler.recognition.MusicRecognitionService
import io.github.pprakapienka.vinylscrobbler.recognition.RecognitionCandidate

class AcoustIdMusicRecognitionService(
    private val client: AcoustIdClient,
) : MusicRecognitionService {
    override suspend fun recognize(fingerprint: AudioFingerprint): List<RecognitionCandidate> =
        client.lookup(fingerprint)
            .results
            .flatMap { result ->
                result.recordings.mapNotNull { recording ->
                    val artist = recording.artists.firstOrNull()?.name ?: return@mapNotNull null
                    val title = recording.title ?: return@mapNotNull null
                    val duration = recording.duration ?: return@mapNotNull null

                    RecognitionCandidate(
                        track = Track(
                            artist = artist,
                            title = title,
                            durationSeconds = duration,
                            album = recording.releasegroups.firstOrNull()?.title,
                            musicBrainzId = recording.id,
                        ),
                        score = result.score.coerceIn(0.0, 1.0),
                    )
                }
            }
            .distinctBy { candidate -> candidate.track.musicBrainzId }
            .sortedByDescending { candidate -> candidate.score }
}
