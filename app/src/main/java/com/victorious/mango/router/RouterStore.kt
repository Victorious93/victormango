package com.victorious.mango.router

import android.content.Context

/** On-device router login used by the VPN toggle after the user confirms. */
class RouterStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Credentials(val host: String, val username: String, val password: String)

    fun load(): Credentials? {
        val host = prefs.getString(KEY_HOST, null)?.trim().orEmpty()
        val user = prefs.getString(KEY_USER, "root")?.trim().orEmpty().ifBlank { "root" }
        val pass = prefs.getString(KEY_PASS, null) ?: return null
        if (host.isBlank() || pass.isBlank()) return null
        return Credentials(host, user, pass)
    }

    fun save(host: String, username: String, password: String) {
        prefs.edit()
            .putString(KEY_HOST, host.trim())
            .putString(KEY_USER, username.trim().ifBlank { "root" })
            .putString(KEY_PASS, password)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS = "router"
        const val KEY_HOST = "host"
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
        const val DEFAULT_HOST = "192.168.8.1"
    }
}
