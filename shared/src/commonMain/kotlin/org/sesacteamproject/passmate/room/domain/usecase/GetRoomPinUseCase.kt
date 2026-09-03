package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

// 공개 방 목록(GET /rooms/public)에는 pin이 없다(계약 `PublicRoomResponse`).
// 방 카드를 눌렀을 때 roomId로 pin을 얻어 Join 라우트(`join?pin=`)로 넘긴다.
class GetRoomPinUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<String> {
        return roomRepository.getRoomPin(roomId)
    }
}
