package org.sesacteamproject.passmate.room.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomHost
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

private class FakeRoomRepository(
    private val joinResult: AppResult<MyParticipation>
) : RoomRepository {

    var lastNickname: String? = null

    override suspend fun getRoomInfo(pin: String): AppResult<RoomInfo> {
        return AppResult.Failure(AppError.NotFound())
    }

    override suspend fun joinRoom(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        lastNickname = nickname
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
        return AppResult.Success(PagedResult(emptyList(), null, false))
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

private fun roomInfo(): RoomInfo {
    return RoomInfo(
        roomId = 1L,
        pin = "482913",
        title = "8월 4주차 Spring 스터디",
        topic = null,
        status = RoomStatus.WAITING,
        questionCount = 10,
        estimatedMinutes = 15,
        scheduledAt = null,
        participantCount = 5,
        maxParticipants = 20,
        isPaid = false,
        entryFee = null,
        host = RoomHost(7L, "김선생", 3, 4.7, 28)
    )
}

class JoinRoomUseCaseTest {

    @Test
    fun trimsNicknameBeforeJoin() = runTest {
        val participation = MyParticipation(11L, 1L, "482913", "준영", 3, true)
        val repository = FakeRoomRepository(AppResult.Success(participation))
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "  준영  ", 3)

        assertEquals("준영", repository.lastNickname)
        assertEquals(participation, (result as AppResult.Success).value)
    }

    @Test
    fun propagatesNicknameConflict() = runTest {
        val repository = FakeRoomRepository(
            AppResult.Failure(AppError.Conflict(serverCode = "NICKNAME_TAKEN"))
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "준영", null)
        val failure = assertIs<AppResult.Failure>(result)

        assertEquals("NICKNAME_TAKEN", failure.error.serverCode)
    }
}
