package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class LeaveRoomUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<Unit> {
        return roomRepository.leaveRoom(roomId)
    }
}
