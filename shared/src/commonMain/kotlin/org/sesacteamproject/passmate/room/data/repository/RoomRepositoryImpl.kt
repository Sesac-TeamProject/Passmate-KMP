package org.sesacteamproject.passmate.room.data.repository

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.core.storage.TokenStorage
import org.sesacteamproject.passmate.room.data.dto.CreateRoomRequest
import org.sesacteamproject.passmate.room.data.dto.JoinRoomRequest
import org.sesacteamproject.passmate.room.data.dto.JoinRoomResponse
import org.sesacteamproject.passmate.room.data.mapper.toDomain
import org.sesacteamproject.passmate.room.data.mapper.toMyParticipation
import org.sesacteamproject.passmate.room.data.remote.RoomRemoteDataSource
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.StudentAvatarKeys
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

// 게스트 토큰은 방 하나에 묶인다 — 다른 방 토큰으로 재입장하면 남의 방에서 내 기록을 찾는 꼴이다
internal fun hasGuestSessionFor(roomId: Long, guestToken: String?, guestRoomId: Long?): Boolean {
    return guestToken != null && guestRoomId == roomId
}

// 재입장이 실패했을 때 게스트 세션을 버리는 경우 — 서버가 그 기록을 더는 받아 주지 않을 때뿐이다:
// 토큰 만료·위조(401), 방·참가 기록 없음(404), 끝난 방(410) (규칙 §8).
// 강퇴(403)는 남겨야 새 입장을 막을 수 있고, 네트워크·서버 오류는 기록이 살아 있어 다시 시도하면 된다
internal fun shouldDiscardGuestSession(error: AppError): Boolean {
    return error is AppError.Unauthorized || error is AppError.NotFound || error is AppError.Gone
}

class RoomRepositoryImpl(
    private val remoteDataSource: RoomRemoteDataSource,
    private val tokenStorage: TokenStorage
) : RoomRepository {

    private var myParticipation: MyParticipation? = null

    // 입장·재입장이 똑같이 남겨야 하는 것 — 세션 스코프 참가 정보와 게스트 토큰 (규칙 §8)
    private fun rememberParticipation(
        response: JoinRoomResponse,
        participation: MyParticipation
    ): MyParticipation {
        if (participation.isGuest && response.accessToken != null) {
            tokenStorage.saveGuestSession(response.accessToken, participation.roomId)
        }
        myParticipation = participation

        return participation
    }

    override suspend fun getRoomInfo(pin: String): AppResult<RoomInfo> {
        return apiCall { remoteDataSource.fetchRoomByPin(pin) }.map { it.toDomain(pin) }
    }

    override suspend fun getRoomPin(roomId: Long): AppResult<String> {
        return apiCall { remoteDataSource.fetchRoomById(roomId) }.map { it.pin }
    }

    override suspend fun getRoomHostUserId(roomId: Long): AppResult<Long?> {
        return apiCall { remoteDataSource.fetchRoomById(roomId) }.map { it.hostUserId }
    }

    override suspend fun joinRoom(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        val request = JoinRoomRequest(nickname = nickname, avatarId = StudentAvatarKeys.toKey(avatarId))

        return apiCall { remoteDataSource.join(room.roomId, request) }.map { response ->
            rememberParticipation(
                response = response,
                participation = response.toMyParticipation(room.roomId, room.pin, nickname, avatarId)
            )
        }
    }

    // 이미 들어갔던 방은 새로 입장하지 않고 원래 참가자 행으로 돌아간다 —
    // 참가자 id가 채워져야 대기실에서 내 칸을 강조하고 강퇴 안내를 받을 수 있다
    override fun hasGuestSession(roomId: Long): Boolean {
        return hasGuestSessionFor(roomId, tokenStorage.guestToken, tokenStorage.guestRoomId)
    }

    override suspend fun rejoinRoom(room: RoomInfo): AppResult<MyParticipation> {
        val result = apiCall { remoteDataSource.rejoin(room.roomId) }.map { response ->
            rememberParticipation(
                response = response,
                participation = response.toMyParticipation(room.roomId, room.pin).copy(isRejoined = true)
            )
        }

        // 서버가 기록을 거부했을 때만 버린다 — 다음 입장이 깨끗이 새로 가도록
        return result.onFailure { error ->
            if (shouldDiscardGuestSession(error) && tokenStorage.guestRoomId == room.roomId) {
                tokenStorage.clearGuestSession()
            }
        }
    }

    override suspend fun getParticipants(roomId: Long): AppResult<List<Participant>> {
        return apiCall { remoteDataSource.fetchParticipants(roomId) }
            .map { participants -> participants.map { it.toDomain() } }
    }

    override suspend fun leaveRoom(roomId: Long): AppResult<Unit> {
        return apiCall { remoteDataSource.leave(roomId) }
            .onSuccess {
                if (myParticipation?.roomId == roomId) {
                    myParticipation = null
                    tokenStorage.clearGuestSession()
                }
            }
    }

    override fun myParticipation(): MyParticipation? {
        return myParticipation
    }

    override suspend fun getHostedRooms(cursor: String?): AppResult<PagedResult<HostedRoom>> {
        return apiCall { remoteDataSource.fetchHostedRooms(cursor) }.map { it.toDomain() }
    }

    override suspend fun createRoom(
        title: String,
        questionSetId: Long?,
        isPaid: Boolean,
        entryFee: Int?
    ): AppResult<CreatedRoom> {
        val request = CreateRoomRequest(
            title = title,
            questionSetId = questionSetId,
            isPaid = isPaid,
            entryFee = entryFee
        )

        return apiCall { remoteDataSource.createRoom(request) }.map { it.toDomain() }
    }
}
