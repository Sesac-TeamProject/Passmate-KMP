package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

// 방 생성 (M-13 새 방 만들기 시트) — 유료 방은 서버가 Lv.3+ 검증(403 HOST_LEVEL_REQUIRED)
class CreateRoomUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(
        title: String,
        questionSetId: Long?,
        isPaid: Boolean,
        entryFee: Int?
    ): AppResult<CreatedRoom> {
        return roomRepository.createRoom(
            title = title.trim(),
            questionSetId = questionSetId,
            isPaid = isPaid,
            entryFee = if (isPaid) entryFee else null
        )
    }
}
