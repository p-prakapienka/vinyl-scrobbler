package io.github.pprakapienka.vinylscrobbler.recognition.acoustid

import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AcoustIdClientTest {
    @Test
    fun `maps an AcoustID response`() = runTest {
        val engine = MockEngine {
            respond(
                content = RESPONSE_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val client = AcoustIdClient(httpClient = httpClient, apiKey = "test-key")
        val service = AcoustIdMusicRecognitionService(client)

        val candidates = service.recognize(
            AudioFingerprint(
                encoded = "fingerprint",
                sourceDurationSeconds = 639,
            ),
        )

        assertEquals(1, candidates.size)
        assertEquals("M83", candidates.single().track.artist)
        assertEquals("Lower Your Eyelids to Die With the Sun", candidates.single().track.title)
        assertEquals("Before the Dawn Heals Us", candidates.single().track.album)
    }

    private companion object {
        val RESPONSE_JSON = """
            {
              "status": "ok",
              "results": [{
                "score": 1.0,
                "id": "9ff43b6a-4f16-427c-93c2-92307ca505e0",
                "recordings": [{
                  "duration": 639,
                  "title": "Lower Your Eyelids to Die With the Sun",
                  "id": "cd2e7c47-16f5-46c6-a37c-a1eb7bf599ff",
                  "artists": [{
                    "id": "6d7b7cd4-254b-4c25-83f6-dd20f98ceacd",
                    "name": "M83"
                  }],
                  "releasegroups": [{
                    "type": "Album",
                    "id": "ddaa2d4d-314e-3e7c-b1d0-f6d207f5aa2f",
                    "title": "Before the Dawn Heals Us"
                  }]
                }]
              }]
            }
        """.trimIndent()
    }
}
