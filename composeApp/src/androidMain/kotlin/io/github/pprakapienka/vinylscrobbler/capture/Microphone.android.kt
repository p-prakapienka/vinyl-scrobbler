package io.github.pprakapienka.vinylscrobbler.capture

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMicrophone(): Microphone = remember { AndroidMicrophone() }

@Composable
actual fun WithMicrophoneAccess(
    content: @Composable (granted: Boolean, request: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
    }
    content(granted) { launcher.launch(Manifest.permission.RECORD_AUDIO) }
}

class AndroidMicrophone : Microphone {
    override suspend fun capture(durationSeconds: Int): PcmAudio = withContext(Dispatchers.IO) {
        require(durationSeconds > 0)
        val rate = supportedRate()
        val minBuffer = AudioRecord.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer.coerceAtLeast(rate),
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            error("Microphone is not available")
        }
        val samples = ShortArray(rate * durationSeconds)
        recorder.startRecording()
        try {
            var read = 0
            while (read < samples.size) {
                val count = recorder.read(samples, read, samples.size - read)
                if (count <= 0) error("Microphone read failed ($count)")
                read += count
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
        PcmAudio(samples, rate, 1)
    }

    private fun supportedRate(): Int {
        val rates = intArrayOf(11_025, 16_000, 22_050, 44_100, 48_000)
        for (rate in rates) {
            val min = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (min > 0) return rate
        }
        error("No supported microphone sample rate")
    }
}
