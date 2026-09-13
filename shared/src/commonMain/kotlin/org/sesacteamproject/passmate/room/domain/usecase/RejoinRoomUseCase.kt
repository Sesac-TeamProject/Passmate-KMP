package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

// PIN 없이 이미 들어갔던 방으로 돌아간다 — 새 입장이 막히는 상황(기입장·진행 중)의 정상 경로다
class RejoinRoomUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(room: RoomInfo): AppResult<MyParticipation> {
        return roomRepository.rejoinRoom(room)
    }
}
