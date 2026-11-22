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
        var lastError: String? = null
        for (rate in RATES) {
            val minBuffer = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuffer <= 0) continue
            val recorder = openRecorder(rate, minBuffer)
            if (recorder == null) {
                lastError = "Microphone rejected $rate Hz"
                continue
            }
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                lastError = "Microphone rejected $rate Hz"
                continue
            }
            return@withContext record(recorder, rate, durationSeconds)
        }
        error(lastError ?: "No supported microphone sample rate")
    }

    private fun openRecorder(rate: Int, minBuffer: Int): AudioRecord? = try {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes(minBuffer, rate),
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun record(recorder: AudioRecord, rate: Int, durationSeconds: Int): PcmAudio {
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
        return PcmAudio(samples, rate, 1)
    }

    private companion object {
        val RATES = intArrayOf(11_025, 16_000, 22_050, 44_100, 48_000)
        const val FRAME_BYTES = 2

        fun bufferBytes(minBuffer: Int, rate: Int): Int {
            val wanted = maxOf(minBuffer, rate * FRAME_BYTES)
            val remainder = wanted % FRAME_BYTES
            return if (remainder == 0) wanted else wanted + FRAME_BYTES - remainder
        }
    }
}
