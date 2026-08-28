package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class GetMyParticipationUseCase(
    private val roomRepository: RoomRepository
) {
    operator fun invoke(): MyParticipation? {
        return roomRepository.myParticipation()
    }
}
