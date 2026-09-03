package org.sesacteamproject.passmate.room.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.core.storage.TokenStorage
import org.sesacteamproject.passmate.room.data.dto.CreateRoomRequest
import org.sesacteamproject.passmate.room.data.dto.JoinRoomRequest
import org.sesacteamproject.passmate.room.data.mapper.toDomain
import org.sesacteamproject.passmate.room.data.remote.RoomRemoteDataSource
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class RoomRepositoryImpl(
    private val remoteDataSource: RoomRemoteDataSource,
    private val tokenStorage: TokenStorage
) : RoomRepository {

    private var myParticipation: MyParticipation? = null

    override suspend fun getRoomInfo(pin: String): AppResult<RoomInfo> {
        return apiCall { remoteDataSource.fetchRoomByPin(pin) }.map { it.toDomain() }
    }

    override suspend fun getRoomPin(roomId: Long): AppResult<String> {
        return apiCall { remoteDataSource.fetchRoomById(roomId) }.map { it.pin }
    }

    override suspend fun joinRoom(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        val request = JoinRoomRequest(nickname = nickname, avatarId = avatarId)

        return apiCall { remoteDataSource.join(room.roomId, request) }.map { response ->
            val participation = MyParticipation(
                participantId = response.participantId,
                roomId = room.roomId,
                pin = room.pin,
                nickname = nickname,
                avatarId = response.avatarId ?: avatarId,
                isGuest = response.participantToken != null
            )

            if (response.participantToken != null) {
                tokenStorage.guestToken = response.participantToken
            }
            myParticipation = participation
            participation
        }
    }

    override suspend fun getParticipants(roomId: Long): AppResult<List<Participant>> {
        return apiCall { remoteDataSource.fetchParticipants(roomId) }
            .map { response -> response.participants.map { it.toDomain() } }
    }

    override suspend fun leaveRoom(roomId: Long): AppResult<Unit> {
        return apiCall { remoteDataSource.leave(roomId) }
            .onSuccess {
                if (myParticipation?.roomId == roomId) {
                    myParticipation = null
                    tokenStorage.guestToken = null
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
