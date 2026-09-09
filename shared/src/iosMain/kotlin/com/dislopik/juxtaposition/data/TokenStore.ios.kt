package com.dislopik.juxtaposition.data

import platform.Foundation.NSUserDefaults

actual class TokenStore {
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun load(): String? = defaults.stringForKey(KEY_TOKEN)

    actual fun save(token: String?) {
        if (token == null) {
            defaults.removeObjectForKey(KEY_TOKEN)
        } else {
            defaults.setObject(token, KEY_TOKEN)
        }
    }

    private companion object {
        const val KEY_TOKEN = "juxtaposition_access_token"
    }
}
