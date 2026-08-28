package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class GetRoomInfoUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(pin: String): AppResult<RoomInfo> {
        return roomRepository.getRoomInfo(pin)
    }
}
