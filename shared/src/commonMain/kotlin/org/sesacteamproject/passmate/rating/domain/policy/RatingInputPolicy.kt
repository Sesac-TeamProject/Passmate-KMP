package org.sesacteamproject.passmate.rating.domain.policy

// 세션 평가 입력의 클라이언트 제한 (규칙 §5). 최종 판정은 서버 응답을 따른다.
// 한 줄 후기 100자는 앱의 입력 제한이다 — 서버는 500자까지 받는다(RoomRating.COMMENT_MAX)
class RatingInputPolicy {

    companion object {
        const val COMMENT_MAX_LENGTH = 100
    }
}
