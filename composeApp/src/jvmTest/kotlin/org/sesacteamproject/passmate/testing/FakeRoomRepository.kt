package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class FakeRoomRepository(
    var roomInfo: RoomInfo? = null
) : RoomRepository {

    var joinResult: AppResult<MyParticipation> = AppResult.Failure(AppError.Unknown())

    var joinCallCount: Int = 0

    override suspend fun getRoomInfo(pin: String): AppResult<RoomInfo> {
        val room = roomInfo

        return if (room != null) {
            AppResult.Success(room)
        } else {
            AppResult.Failure(AppError.NotFound())
        }
    }

    override suspend fun joinRoom(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        joinCallCount += 1
        return joinResult
    }

    override suspend fun getParticipants(roomId: Long): AppResult<List<Participant>> {
        return AppResult.Success(emptyList())
    }

    override suspend fun leaveRoom(roomId: Long): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override fun myParticipation(): MyParticipation? {
        return null
    }

    override suspend fun getHostedRooms(cursor: String?): AppResult<PagedResult<HostedRoom>> {
        return AppResult.Success(PagedResult(items = emptyList(), nextCursor = null, hasNext = false))
    }

    override suspend fun createRoom(
        title: String,
        questionSetId: Long?,
        isPaid: Boolean,
        entryFee: Int?
    ): AppResult<CreatedRoom> {
        return AppResult.Failure(AppError.Unknown())
    }
}
