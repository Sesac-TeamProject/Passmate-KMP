package org.sesacteamproject.passmate.user.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.user.data.dto.ClaimGuestRecordRequest
import org.sesacteamproject.passmate.user.data.mapper.toDomain
import org.sesacteamproject.passmate.user.data.remote.UserRemoteDataSource
import org.sesacteamproject.passmate.user.domain.model.MyPage
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
}
