package org.sesacteamproject.passmate.user.domain.model

import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.room.domain.model.HostLevel

// 호스트 공개 프로필 (GET /users/{userId}/profile) — M-10 선생님 프로필 시트 (FR-044·048)
data class HostProfile(
    val userId: Long,
    val nickname: String,
    val intro: String?,
    val level: HostLevel?,
    val avgStars: Double?,
    val ratingCount: Int,
    val roomCount: Int,
    val totalStudents: Int,
    val badges: List<BadgeType>,
    val rooms: List<PublicRoom>
)

// 신고 유형 (POST /reports) — M-10 "프로필 신고"
enum class ReportReason(val wireValue: String, val label: String) {
    NICKNAME("NICKNAME", "부적절한 닉네임"),
    QUESTION_ERROR("QUESTION_ERROR", "문제 오류"),
    PAID_ROOM("PAID_ROOM", "유료 방 문제"),
    OPERATION("OPERATION", "부적절한 운영"),
    SPAM("SPAM", "도배·광고"),
    DIFFICULTY("DIFFICULTY", "난이도 불일치")
}
