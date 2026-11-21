package io.github.pprakapienka.vinylscrobbler.recognition.acoustid

import io.github.pprakapienka.vinylscrobbler.recognition.AudioFingerprint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

class AcoustIdClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    init {
        require(apiKey.isNotBlank()) { "AcoustID API key must not be blank" }
    }

    internal suspend fun lookup(fingerprint: AudioFingerprint): AcoustIdResponse {
        val response = httpClient.post(LOOKUP_URL) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client", apiKey)
                        append("duration", fingerprint.sourceDurationSeconds.toString())
                        append("fingerprint", fingerprint.encoded)
                        append("format", "json")
                        append("meta", "recordings releasegroups compress")
                    },
                ),
            )
        }.body<AcoustIdResponse>()

        if (response.status != "ok") {
            val detail = response.error?.message ?: "Unknown AcoustID error"
            throw AcoustIdException(detail)
        }

        return response
    }

    private companion object {
        const val LOOKUP_URL = "https://api.acoustid.org/v2/lookup"
    }
}
