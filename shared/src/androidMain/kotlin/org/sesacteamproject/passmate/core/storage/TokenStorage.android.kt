package org.sesacteamproject.passmate.core.storage

import android.content.Context
import android.content.SharedPreferences

actual class TokenStorage(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    actual val guestToken: String?
        get() = preferences.getString(KEY_GUEST_TOKEN, null)

    actual val guestRoomId: Long?
        get() = preferences.getLong(KEY_GUEST_ROOM_ID, NO_ROOM).takeIf { it != NO_ROOM }

    actual fun saveGuestSession(token: String, roomId: Long) {
        preferences.edit()
            .putString(KEY_GUEST_TOKEN, token)
            .putLong(KEY_GUEST_ROOM_ID, roomId)
            .apply()
    }

    actual fun clearGuestSession() {
        preferences.edit()
            .remove(KEY_GUEST_TOKEN)
            .remove(KEY_GUEST_ROOM_ID)
            .apply()
    }

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

        private const val KEY_GUEST_TOKEN = "guest_token"

        private const val KEY_GUEST_ROOM_ID = "guest_room_id"

        // SharedPreferences에는 "없음"이 없다 — 방 id가 될 수 없는 값을 빈 값으로 쓴다
        private const val NO_ROOM = -1L
    }
}
