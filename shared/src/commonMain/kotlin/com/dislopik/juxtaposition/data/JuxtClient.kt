package com.dislopik.juxtaposition.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

/**
 * The one HTTP client the app uses.
 *
 * Redirects are handled by hand rather than followed: Juxtaposition answers a successful
 * post with a 302 to the new post, and an expired session with a 302 to /login, so the
 * Location header is the signal in both cases.
 */
fun createJuxtHttpClient(): HttpClient = HttpClient {
    followRedirects = false
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }
}

/**
 * Simple process-wide wiring. The app has a single API client and a single token store,
 * so a service locator keeps the UI free of dependency plumbing.
 */
object Juxt {
    val httpClient: HttpClient by lazy { createJuxtHttpClient() }
    val api: JuxtApi by lazy { JuxtApi(httpClient, TokenStore()) }
}
