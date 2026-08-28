package org.sesacteamproject.passmate.user.domain.model

// 가입 유도 → 로그인 → claim 파이프라인의 대기 상태 (규칙 §8 pendingAction).
// 게스트가 "기록 저장"을 누르면 participantId를 여기 담아두고, 로그인 완료 후 연동한다
class PendingGuestClaim {

    var participantId: Long? = null

    fun request(participantId: Long) {
        this.participantId = participantId
    }

    fun consume(): Long? {
        val id = participantId

        participantId = null
        return id
    }
}
