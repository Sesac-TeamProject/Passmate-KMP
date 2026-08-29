package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class GetHostedRoomsUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(cursor: String?): AppResult<PagedResult<HostedRoom>> {
        return roomRepository.getHostedRooms(cursor)
    }
}
