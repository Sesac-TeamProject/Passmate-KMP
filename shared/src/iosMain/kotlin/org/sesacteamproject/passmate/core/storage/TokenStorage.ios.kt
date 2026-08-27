package org.sesacteamproject.passmate.core.storage

import platform.Foundation.NSUserDefaults

// MVP 단순화로 NSUserDefaults 사용 — Keychain 이관은 보안 후속 과제
actual class TokenStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    actual var guestToken: String? = null

    actual fun saveMemberTokens(accessToken: String, refreshToken: String) {
        defaults.setObject(accessToken, KEY_ACCESS_TOKEN)
        defaults.setObject(refreshToken, KEY_REFRESH_TOKEN)
    }

    actual fun accessToken(): String? {
        return defaults.stringForKey(KEY_ACCESS_TOKEN)
    }

    actual fun refreshToken(): String? {
        return defaults.stringForKey(KEY_REFRESH_TOKEN)
    }

    actual fun clearMemberTokens() {
        defaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "passmate_access_token"
        private const val KEY_REFRESH_TOKEN = "passmate_refresh_token"
    }
}
