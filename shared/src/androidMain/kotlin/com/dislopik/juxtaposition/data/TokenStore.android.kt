package com.dislopik.juxtaposition.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/**
 * Holds the application context so the shared module can reach Android storage.
 * [initJuxtAndroid] must be called once from the launcher activity.
 */
@SuppressLint("StaticFieldLeak")
internal object AndroidAppContext {
    private var context: Context? = null

    fun init(value: Context) {
        context = value.applicationContext
    }

    fun require(): Context = requireNotNull(context) {
        "initJuxtAndroid(context) must be called before using the shared module"
    }
}

fun initJuxtAndroid(context: Context) = AndroidAppContext.init(context)

actual class TokenStore {
    private val prefs: SharedPreferences
        get() = AndroidAppContext.require().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun load(): String? = prefs.getString(KEY_TOKEN, null)

    actual fun save(token: String?) {
        prefs.edit().apply {
            if (token == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
        }.apply()
    }

    private companion object {
        const val PREFS = "juxtaposition"
        const val KEY_TOKEN = "access_token"
    }
}
