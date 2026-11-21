package io.github.pprakapienka.vinylscrobbler.platform

import io.ktor.client.HttpClient

expect fun platformHttpClient(): HttpClient
