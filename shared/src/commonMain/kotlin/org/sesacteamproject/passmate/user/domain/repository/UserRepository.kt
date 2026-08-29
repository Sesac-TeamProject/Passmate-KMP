package org.sesacteamproject.passmate.user.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.NotificationSettings
import org.sesacteamproject.passmate.user.domain.model.ReportReason
import org.sesacteamproject.passmate.user.domain.model.UserProfile

interface UserRepository {

    // cursor가 null이면 첫 페이지(요약·진행중 포함), 있으면 목록 다음 페이지
    suspend fun getMyPage(cursor: String?): AppResult<MyPage>

    // 게스트 기록 계정 연동 — 가입 후 7일 내, 경과 시 410 RECORD_PURGED (FR-036)
    suspend fun claimGuestRecord(participantId: Long): AppResult<Unit>

    // 내 명성 — 등급·집계·다음 승급 진행도 (M-09, FR-045~048)
    suspend fun getMyGrade(): AppResult<MyGrade>

    // 내 뱃지 컬렉션 — 획득/미획득·진행도 (M-09, FR-048)
    suspend fun getMyBadges(): AppResult<List<Badge>>

    // 호스트 공개 프로필 — 차단한 호스트는 404 (M-10, FR-044·048)
    suspend fun getHostProfile(userId: Long): AppResult<HostProfile>

    // 호스트 차단 — 공개 목록에서 숨김·프로필 접근 제한 (M-10)
    suspend fun blockUser(userId: Long): AppResult<Unit>

    // 신고 접수 — 게스트 익명 신고 가능 (M-10)
    suspend fun reportUser(userId: Long, reason: ReportReason, detail: String?): AppResult<Unit>

    // ── 내 정보 관리 (M-12 설정 허브) ──

    suspend fun getMyProfile(): AppResult<UserProfile>

    // 닉네임·기본 캐릭터 수정 (M-12-1·M-12-7)
    suspend fun updateMyProfile(nickname: String?, avatarId: Int?): AppResult<Unit>

    // 회원 탈퇴 — 정산 미지급분·진행 중 방 있으면 409 (M-12-12)
    suspend fun deleteAccount(): AppResult<Unit>

    suspend fun getNotificationSettings(): AppResult<NotificationSettings>

    suspend fun updateNotificationSettings(settings: NotificationSettings): AppResult<Unit>
}
