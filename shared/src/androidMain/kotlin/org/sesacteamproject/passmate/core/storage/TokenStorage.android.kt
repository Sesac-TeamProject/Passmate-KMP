package org.sesacteamproject.passmate.core.storage

import android.content.Context
import android.content.SharedPreferences

actual class TokenStorage(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    actual var guestToken: String? = null

    actual fun saveMemberTokens(accessToken: String, refreshToken: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    actual fun accessToken(): String? {
        return preferences.getString(KEY_ACCESS_TOKEN, null)
    }

    actual fun refreshToken(): String? {
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }

    actual fun clearMemberTokens() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "passmate_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
