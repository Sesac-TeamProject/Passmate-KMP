package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class JoinRoomUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        return roomRepository.joinRoom(room, nickname.trim(), avatarId)
    }
}
