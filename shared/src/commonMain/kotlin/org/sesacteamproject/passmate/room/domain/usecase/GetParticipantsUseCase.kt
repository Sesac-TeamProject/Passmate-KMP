package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class GetParticipantsUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<List<Participant>> {
        return roomRepository.getParticipants(roomId)
    }
}
