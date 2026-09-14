package org.sesacteamproject.passmate.core.model

// 서버 오류 응답 {code}의 값 — 백엔드 ErrorCode 이름과 1:1 (규칙 §10).
// 화면 문구 분기는 문자열을 직접 쓰지 않고 이 상수로 비교한다. Swift에서는 ServerErrorCode.shared.X로 읽는다
object ServerErrorCode {
    // 403
    const val GUEST_NOT_ALLOWED = "GUEST_NOT_ALLOWED"

    const val HOST_CANNOT_JOIN = "HOST_CANNOT_JOIN"

    const val HOST_LEVEL_REQUIRED = "HOST_LEVEL_REQUIRED"

    // 404
    const val PARTICIPANT_NOT_FOUND = "PARTICIPANT_NOT_FOUND"

    const val QUESTION_NOT_FOUND = "QUESTION_NOT_FOUND"

    const val QUESTION_SET_NOT_FOUND = "QUESTION_SET_NOT_FOUND"

    // 409
    const val ALREADY_JOINED = "ALREADY_JOINED"

    const val NICKNAME_DUPLICATED = "NICKNAME_DUPLICATED"

    const val ROOM_NOT_JOINABLE = "ROOM_NOT_JOINABLE"

    const val ROOM_FULL = "ROOM_FULL"

    const val QUESTION_NOT_RUNNING = "QUESTION_NOT_RUNNING"

    const val SCREEN_LOCKED = "SCREEN_LOCKED"
}
