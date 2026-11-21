package io.github.pprakapienka.vinylscrobbler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.pprakapienka.vinylscrobbler.capture.WithMicrophoneAccess
import io.github.pprakapienka.vinylscrobbler.capture.rememberMicrophone
import io.github.pprakapienka.vinylscrobbler.platform.platformHttpClient
import io.github.pprakapienka.vinylscrobbler.recognition.PcmAudio
import io.github.pprakapienka.vinylscrobbler.recognition.acoustid.AcoustIdClient
import io.github.pprakapienka.vinylscrobbler.recognition.acoustid.AcoustIdException
import io.github.pprakapienka.vinylscrobbler.recognition.acoustid.AcoustIdMusicRecognitionService
import io.github.pprakapienka.vinylscrobbler.recognition.chromaprint.ChromaprintFingerprinter
import io.github.pprakapienka.vinylscrobbler.recognition.spike.CaptureSlice
import io.github.pprakapienka.vinylscrobbler.recognition.spike.PartialCaptureStudy
import io.github.pprakapienka.vinylscrobbler.recognition.spike.SliceObservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

private val durations = listOf(10, 20, 30)

@Composable
fun App() {
    val microphone = rememberMicrophone()
    val fingerprinter = remember { ChromaprintFingerprinter() }
    val study = remember { PartialCaptureStudy(fingerprinter) }
    val http = remember { platformHttpClient() }
    val scope = rememberCoroutineScope()
    var duration by remember { mutableStateOf(20) }
    var apiKey by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Record a stretch of the record, then read the fingerprint.") }
    var detail by remember { mutableStateOf("") }
    var slices by remember { mutableStateOf(emptyList<SliceObservation>()) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            WithMicrophoneAccess { granted, request ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Vinyl Scrobbler", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Listens through the microphone, fingerprints the audio with Chromaprint, and can ask AcoustID what it was.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        durations.forEach { seconds ->
                            if (seconds == duration) {
                                Button(onClick = { duration = seconds }, enabled = !busy) {
                                    Text("${seconds}s")
                                }
                            } else {
                                OutlinedButton(onClick = { duration = seconds }, enabled = !busy) {
                                    Text("${seconds}s")
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (!granted) {
                                request()
                                return@Button
                            }
                            scope.launch {
                                busy = true
                                status = "Listening for $duration seconds…"
                                detail = ""
                                slices = emptyList()
                                try {
                                    val audio = microphone.capture(duration)
                                    val report = withContext(Dispatchers.Default) {
                                        val started = TimeSource.Monotonic.markNow()
                                        val inspected = fingerprinter.inspectNow(audio)
                                        val elapsed = started.elapsedNow().inWholeMilliseconds
                                        val pieces = study.observe(audio, slicePlan(audio))
                                        Triple(inspected, elapsed, pieces)
                                    }
                                    val (inspected, elapsed, pieces) = report
                                    slices = pieces
                                    detail = inspected.fingerprint.encoded
                                    status = "${inspected.rawItemCount} subfingerprints in ${elapsed} ms, " +
                                        "${inspected.fingerprint.sourceDurationSeconds}s of audio."
                                    val key = apiKey.trim()
                                    if (key.isNotEmpty()) {
                                        val candidates = AcoustIdMusicRecognitionService(AcoustIdClient(http, key))
                                            .recognize(inspected.fingerprint)
                                        val top = candidates.firstOrNull()
                                        status = if (top == null) {
                                            "$status AcoustID returned nothing."
                                        } else {
                                            "$status ${top.track.artist} — ${top.track.title} (${top.score})"
                                        }
                                    }
                                } catch (error: AcoustIdException) {
                                    status = "AcoustID rejected the lookup: ${error.message}"
                                } catch (error: Throwable) {
                                    status = error.message ?: "Capture failed"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                    ) {
                        Text(if (granted) "Listen" else "Allow microphone")
                    }
                    Text(status, style = MaterialTheme.typography.titleMedium)
                    if (detail.isNotEmpty()) {
                        Text(
                            text = detail,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    slices.forEach { slice ->
                        Text("${slice.name}: ${slice.rawItems} items, ${slice.elapsedMillis} ms")
                    }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("AcoustID application key") },
                        singleLine = true,
                        supportingText = { Text("Kept in memory for this session. Not stored.") },
                    )
                }
            }
        }
    }
}

private fun slicePlan(audio: PcmAudio): List<CaptureSlice> {
    val frames = audio.samples.size / audio.channels
    return listOf(5, 10, 20).flatMap { seconds ->
        val length = seconds * audio.sampleRate
        if (length > frames) return@flatMap emptyList()
        val middle = ((frames - length) / 2).coerceAtLeast(0)
        listOf(
            CaptureSlice("start ${seconds}s", 0, length),
            CaptureSlice("middle ${seconds}s", middle, length),
        )
    }
}
