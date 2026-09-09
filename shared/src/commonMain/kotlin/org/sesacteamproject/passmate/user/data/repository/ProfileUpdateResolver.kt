package org.sesacteamproject.passmate.user.data.repository

import org.sesacteamproject.passmate.user.data.dto.UpdateProfileRequest

// 서버 PUT /users/me는 부분 수정이 아니라 전체 교체다 — 본문에서 뺀 필드는 보존되지 않고 null로 덮인다.
// (2026-09-09 배포 서버 실측: defaultAvatarId를 저장한 뒤 닉네임만 담아 PUT 하면 캐릭터가 지워졌다.
//  M-12-7에서 캐릭터를 바꾸고 M-12-1에서 닉네임을 저장하면 캐릭터가 사라지던 원인이다.)
// 그래서 이번에 바꾸지 않는 값도 현재 프로필에서 채워 함께 보낸다.
//
// 닉네임을 끝내 못 구하면 null을 반환한다 — 호출부가 검증 실패로 접는다.
internal fun resolveProfileUpdate(
    requestedNickname: String?,
    requestedAvatarKey: String?,
    currentNickname: String?,
    currentAvatarKey: String?
): UpdateProfileRequest? {
    val nickname = requestedNickname?.trim()?.ifEmpty { null }
        ?: currentNickname?.trim()?.ifEmpty { null }
    val avatarKey = requestedAvatarKey ?: currentAvatarKey

    return if (nickname == null) {
        null
    } else {
        UpdateProfileRequest(nickname = nickname, defaultAvatarId = avatarKey)
    }
}
