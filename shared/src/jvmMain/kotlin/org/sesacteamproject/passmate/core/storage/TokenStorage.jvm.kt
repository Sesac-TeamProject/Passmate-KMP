package org.sesacteamproject.passmate.core.storage

import java.util.prefs.Preferences

actual class TokenStorage {

    private val preferences: Preferences =
        Preferences.userRoot().node(PREFERENCES_NODE)

    actual var guestToken: String? = null

    actual fun saveMemberTokens(accessToken: String, refreshToken: String) {
        preferences.put(KEY_ACCESS_TOKEN, accessToken)
        preferences.put(KEY_REFRESH_TOKEN, refreshToken)
    }

    actual fun accessToken(): String? {
        return preferences.get(KEY_ACCESS_TOKEN, null)
    }

    actual fun refreshToken(): String? {
        return preferences.get(KEY_REFRESH_TOKEN, null)
    }

    actual fun clearMemberTokens() {
        preferences.remove(KEY_ACCESS_TOKEN)
        preferences.remove(KEY_REFRESH_TOKEN)
    }

    companion object {
        private const val PREFERENCES_NODE = "org/sesacteamproject/passmate"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
