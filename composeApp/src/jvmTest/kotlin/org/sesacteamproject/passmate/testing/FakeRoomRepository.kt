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

    var rejoinResult: AppResult<MyParticipation> = AppResult.Failure(AppError.Unknown())

    var rejoinCallCount: Int = 0

    var pinByRoomId: Map<Long, String> = emptyMap()

    var participantsResult: AppResult<List<Participant>> = AppResult.Success(emptyList())

    var participantsCallCount: Int = 0

    // 화면이 "나"를 알아보는 기준 — 퇴장 이벤트가 내 것인지 가를 때 쓴다
    var currentParticipation: MyParticipation? = null

    override suspend fun getRoomPin(roomId: Long): AppResult<String> {
        val pin = pinByRoomId[roomId]

        return if (pin != null) {
            AppResult.Success(pin)
        } else {
            AppResult.Failure(AppError.NotFound())
        }
    }

    override suspend fun getRoomHostUserId(roomId: Long): AppResult<Long?> {
        return AppResult.Success(null)
    }

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

    override fun hasGuestSession(roomId: Long): Boolean {
        return false
    }

    override suspend fun rejoinRoom(room: RoomInfo): AppResult<MyParticipation> {
        rejoinCallCount += 1
        return rejoinResult
    }

    override suspend fun getParticipants(roomId: Long): AppResult<List<Participant>> {
        participantsCallCount += 1
        return participantsResult
    }

    override suspend fun leaveRoom(roomId: Long): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override fun myParticipation(): MyParticipation? {
        return currentParticipation
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
