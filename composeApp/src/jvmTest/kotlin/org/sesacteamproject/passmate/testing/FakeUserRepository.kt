package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.NotificationSettings
import org.sesacteamproject.passmate.user.domain.model.ReportReason
import org.sesacteamproject.passmate.user.domain.model.UserProfile
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class FakeUserRepository(
    var profileResult: AppResult<UserProfile> = AppResult.Failure(AppError.Unknown()),
    var myPageResults: List<AppResult<MyPage>> = emptyList(),
    var deleteResult: AppResult<Unit> = AppResult.Success(Unit)
) : UserRepository {

    var myPageCalls: MutableList<String?> = mutableListOf()

    var deleteCalls: Int = 0

    override suspend fun getMyPage(cursor: String?): AppResult<MyPage> {
        val index = myPageCalls.size

        myPageCalls.add(cursor)
        return myPageResults.getOrElse(index) { AppResult.Failure(AppError.Unknown()) }
    }

    override suspend fun claimGuestRecord(participantId: Long): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getMyGrade(): AppResult<MyGrade> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getMyBadges(): AppResult<List<Badge>> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getHostProfile(userId: Long): AppResult<HostProfile> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun blockUser(userId: Long): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun reportUser(userId: Long, reason: ReportReason, detail: String?): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getMyProfile(): AppResult<UserProfile> {
        return profileResult
    }

    override suspend fun updateMyProfile(nickname: String?, avatarId: Int?): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        deleteCalls += 1
        return deleteResult
    }

    override suspend fun getNotificationSettings(): AppResult<NotificationSettings> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}
