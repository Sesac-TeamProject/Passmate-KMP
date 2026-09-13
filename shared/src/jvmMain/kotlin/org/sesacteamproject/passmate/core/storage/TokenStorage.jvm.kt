package org.sesacteamproject.passmate.core.storage

import java.util.prefs.Preferences

actual class TokenStorage {

    private val preferences: Preferences =
        Preferences.userRoot().node(PREFERENCES_NODE)

    actual val guestToken: String?
        get() = preferences.get(KEY_GUEST_TOKEN, null)

    actual val guestRoomId: Long?
        get() = preferences.getLong(KEY_GUEST_ROOM_ID, NO_ROOM).takeIf { it != NO_ROOM }

    actual fun saveGuestSession(token: String, roomId: Long) {
        preferences.put(KEY_GUEST_TOKEN, token)
        preferences.putLong(KEY_GUEST_ROOM_ID, roomId)
    }

    actual fun clearGuestSession() {
        preferences.remove(KEY_GUEST_TOKEN)
        preferences.remove(KEY_GUEST_ROOM_ID)
    }

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

        private const val KEY_GUEST_TOKEN = "guest_token"

        private const val KEY_GUEST_ROOM_ID = "guest_room_id"

        // Preferences에는 "없음"이 없다 — 방 id가 될 수 없는 값을 빈 값으로 쓴다
        private const val NO_ROOM = -1L
    }
}
