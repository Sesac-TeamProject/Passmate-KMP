package org.sesacteamproject.passmate.user.data.repository

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.user.data.dto.ClaimGuestRecordRequest
import org.sesacteamproject.passmate.user.data.dto.NotificationSettingsDto
import org.sesacteamproject.passmate.user.data.dto.ReportRequest
import org.sesacteamproject.passmate.room.domain.model.StudentAvatarKeys
import org.sesacteamproject.passmate.user.data.dto.UserProfileResponse
import org.sesacteamproject.passmate.user.data.mapper.toDomain
import org.sesacteamproject.passmate.user.data.remote.UserRemoteDataSource
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.NotificationSettings
import org.sesacteamproject.passmate.user.domain.model.ReportReason
import org.sesacteamproject.passmate.user.domain.model.UserProfile
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {

    override suspend fun getMyPage(cursor: String?): AppResult<MyPage> {
        return apiCall { remoteDataSource.fetchMyPage(cursor) }.map { it.toDomain() }
    }

    override suspend fun claimGuestRecord(participantId: Long): AppResult<Unit> {
        return apiCall { remoteDataSource.claimGuestRecord(ClaimGuestRecordRequest(participantId)) }
    }

    override suspend fun getMyGrade(): AppResult<MyGrade> {
        return apiCall { remoteDataSource.fetchGrade() }.map { it.toDomain() }
    }

    override suspend fun getMyBadges(): AppResult<List<Badge>> {
        return apiCall { remoteDataSource.fetchBadges() }.map { it.toDomain() }
    }

    override suspend fun getHostProfile(userId: Long): AppResult<HostProfile> {
        return apiCall { remoteDataSource.fetchHostProfile(userId) }.map { it.toDomain() }
    }

    override suspend fun blockUser(userId: Long): AppResult<Unit> {
        return apiCall { remoteDataSource.blockUser(userId) }
    }

    override suspend fun reportUser(userId: Long, reason: ReportReason, detail: String?): AppResult<Unit> {
        val request = ReportRequest(
            targetType = "USER",
            targetId = userId,
            reason = reason.wireValue,
            detail = detail?.trim()?.ifEmpty { null }
        )

        return apiCall { remoteDataSource.submitReport(request) }
    }

    override suspend fun getMyProfile(): AppResult<UserProfile> {
        return apiCall { remoteDataSource.fetchMyProfile() }.map { it.toDomain() }
    }

    // 이번에 바꾸지 않는 값을 채울 현재 프로필 — 실패하면 null로 두고 호출부가 검증 실패로 접는다
    private suspend fun currentProfileOrNull(): UserProfileResponse? {
        val result = apiCall { remoteDataSource.fetchMyProfile() }

        return (result as? AppResult.Success)?.value
    }

    // 서버 PUT은 전체 교체다 — 생략한 필드가 null로 덮이므로 안 바꾸는 값도 현재 값을 실어 보낸다
    // (§resolveProfileUpdate). 둘 다 주어졌으면 현재 프로필을 조회하지 않는다.
    override suspend fun updateMyProfile(nickname: String?, avatarId: Int?): AppResult<Unit> {
        val requestedNickname = nickname?.trim()?.ifEmpty { null }
        val requestedAvatarKey = StudentAvatarKeys.toKey(avatarId)
        val needsCurrent = requestedNickname == null || requestedAvatarKey == null
        val current = if (needsCurrent) currentProfileOrNull() else null
        val request = resolveProfileUpdate(
            requestedNickname = requestedNickname,
            requestedAvatarKey = requestedAvatarKey,
            currentNickname = current?.nickname,
            currentAvatarKey = current?.defaultAvatarId
        )

        return if (request == null) {
            AppResult.Failure(AppError.ValidationFailed("닉네임을 확인하지 못했어요"))
        } else {
            apiCall { remoteDataSource.updateMyProfile(request) }
        }
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        return apiCall { remoteDataSource.deleteAccount() }
    }

    override suspend fun getNotificationSettings(): AppResult<NotificationSettings> {
        return apiCall { remoteDataSource.fetchNotificationSettings() }.map { it.toDomain() }
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings): AppResult<Unit> {
        val request = NotificationSettingsDto(
            sessionStart = settings.sessionStart,
            ratingRequest = settings.ratingRequest,
            settlementDone = settings.settlementDone
        )

        return apiCall { remoteDataSource.updateNotificationSettings(request) }
    }
}
