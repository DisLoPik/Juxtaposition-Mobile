package com.dislopik.juxtaposition.data

/**
 * Persists the `access_token` cookie issued by juxt.pretendo.network so the user stays
 * signed in between launches.
 */
expect class TokenStore() {
    fun load(): String?
    fun save(token: String?)
}
