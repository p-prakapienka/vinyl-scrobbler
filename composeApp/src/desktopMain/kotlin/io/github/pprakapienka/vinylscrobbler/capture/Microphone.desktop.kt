package io.github.pprakapienka.vinylscrobbler.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

@Composable
actual fun rememberMicrophone(): Microphone = remember { DesktopMicrophone() }

@Composable
actual fun WithMicrophoneAccess(
    content: @Composable (granted: Boolean, request: () -> Unit) -> Unit,
) {
    content(true) {}
}

class DesktopMicrophone : Microphone {
    override suspend fun capture(durationSeconds: Int): PcmAudio = withContext(Dispatchers.IO) {
        require(durationSeconds > 0)
        val format = supportedFormat()
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()
        try {
            val sampleRate = format.sampleRate.toInt()
            val bytes = ByteArray(sampleRate * durationSeconds * 2)
            var read = 0
            while (read < bytes.size) {
                val count = line.read(bytes, read, bytes.size - read)
                if (count < 0) error("Microphone read failed")
                read += count
            }
            PcmAudio(littleEndianShorts(bytes), sampleRate, 1)
        } finally {
            line.stop()
            line.close()
        }
    }

    private fun supportedFormat(): AudioFormat {
        val rates = floatArrayOf(11_025f, 22_050f, 44_100f, 48_000f)
        for (rate in rates) {
            val format = AudioFormat(rate, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            if (AudioSystem.isLineSupported(info)) return format
        }
        error("No supported microphone sample rate")
    }

    private fun littleEndianShorts(bytes: ByteArray): ShortArray = ShortArray(bytes.size / 2) { index ->
        val low = bytes[index * 2].toInt() and 0xff
        val high = bytes[index * 2 + 1].toInt()
        ((high shl 8) or low).toShort()
    }
}
