package io.github.pprakapienka.vinylscrobbler.capture

import androidx.compose.runtime.Composable
import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio

interface Microphone {
    suspend fun capture(durationSeconds: Int): PcmAudio
}

@Composable
expect fun rememberMicrophone(): Microphone

@Composable
expect fun WithMicrophoneAccess(
    content: @Composable (granted: Boolean, request: () -> Unit) -> Unit,
)
