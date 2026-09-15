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
    private val joinResult: AppResult<MyParticipation>,
    private val rejoinResult: AppResult<MyParticipation> = AppResult.Failure(AppError.NotFound()),
    private var hasGuestSession: Boolean = false,
    // 재입장이 실패했을 때 저장소가 게스트 세션을 버리는가 — 실제 저장소는 401·404·410에서만 버린다
    private val rejoinDiscardsSession: Boolean = true
) : RoomRepository {

    var lastNickname: String? = null

    // 호출 순서를 그대로 적는다 — "join 전에 rejoin을 불렀는가"가 이 기능의 핵심이다
    val calls = mutableListOf<String>()

    override suspend fun getRoomInfo(pin: String): AppResult<RoomInfo> {
        return AppResult.Failure(AppError.NotFound())
    }

    override suspend fun getRoomPin(roomId: Long): AppResult<String> {
        return AppResult.Failure(AppError.NotFound())
    }

    override suspend fun getRoomHostUserId(roomId: Long): AppResult<Long?> {
        return AppResult.Failure(AppError.NotFound())
    }

    override suspend fun joinRoom(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        lastNickname = nickname
        calls.add("join")
        return joinResult
    }

    override suspend fun rejoinRoom(room: RoomInfo): AppResult<MyParticipation> {
        calls.add("rejoin")
        if (rejoinResult is AppResult.Failure && rejoinDiscardsSession) {
            hasGuestSession = false
        }
        return rejoinResult
    }

    override fun hasGuestSession(roomId: Long): Boolean {
        return hasGuestSession
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
        isGuestAllowed = true,
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

    // 재입장 폴백은 닉네임 중복에만 탄다 — 정원 초과 같은 다른 409는 그대로 돌려준다
    @Test
    fun otherJoinConflictPropagatesWithoutRejoin() = runTest {
        val repository = FakeRoomRepository(
            AppResult.Failure(AppError.Conflict(serverCode = "ROOM_FULL"))
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "준영", null)
        val failure = assertIs<AppResult.Failure>(result)

        assertEquals("ROOM_FULL", failure.error.serverCode)
        assertEquals(listOf("join"), repository.calls)
    }

    // 게스트는 서버가 토큰 없이는 알아보지 못한다 — 이어갈 기록이 있으면 새 입장 대신 재입장을 먼저 부른다.
    // 그냥 join 하면 새 참가자 행이 생겨 대기실에 같은 사람이 둘로 보인다
    @Test
    fun guestWithSavedSessionRejoinsBeforeJoining() = runTest {
        val resumed = MyParticipation(11L, 1L, "482913", "파워에이드", 3, true, isRejoined = true)
        val repository = FakeRoomRepository(
            joinResult = AppResult.Failure(AppError.Unknown()),
            rejoinResult = AppResult.Success(resumed),
            hasGuestSession = true
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "감귤에이드", 3)

        assertEquals(listOf("rejoin"), repository.calls)
        assertEquals(resumed, (result as AppResult.Success).value)
    }

    // 토큰이 만료됐거나(401) 그 방 기록이 사라졌으면(404) 평소대로 새로 입장한다
    @Test
    fun expiredGuestSessionFallsBackToNormalJoin() = runTest {
        val joined = MyParticipation(12L, 1L, "482913", "감귤에이드", 3, true)
        val repository = FakeRoomRepository(
            joinResult = AppResult.Success(joined),
            rejoinResult = AppResult.Failure(AppError.Unauthorized()),
            hasGuestSession = true
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "감귤에이드", 3)

        assertEquals(listOf("rejoin", "join"), repository.calls)
        assertEquals(joined, (result as AppResult.Success).value)
    }

    // 강퇴는 새로 들어오는 것도 막아야 한다 — join으로 우회하면 내보낸 의미가 없다.
    // 저장소는 강퇴(403)에 게스트 세션을 버리지 않는다
    @Test
    fun kickedGuestIsNotLetBackInThroughNormalJoin() = runTest {
        val repository = FakeRoomRepository(
            joinResult = AppResult.Success(MyParticipation(12L, 1L, "482913", "감귤에이드", 3, true)),
            rejoinResult = AppResult.Failure(AppError.PermissionDenied(serverCode = "ACCESS_DENIED")),
            hasGuestSession = true,
            rejoinDiscardsSession = false
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "감귤에이드", 3)
        val failure = assertIs<AppResult.Failure>(result)

        assertEquals(listOf("rejoin"), repository.calls)
        assertEquals("ACCESS_DENIED", failure.error.serverCode)
    }

    // 재입장이 네트워크 오류로 실패해도 기록은 살아 있다 — 곧바로 새로 입장하면 연결이 돌아온 순간
    // 같은 사람이 새 참가자로 또 들어간다. 실패를 그대로 돌려 다시 시도하게 한다
    @Test
    fun networkFailureOnRejoinDoesNotFallBackToNewJoin() = runTest {
        val repository = FakeRoomRepository(
            joinResult = AppResult.Success(MyParticipation(12L, 1L, "482913", "감귤에이드", 3, true)),
            rejoinResult = AppResult.Failure(AppError.NetworkError()),
            hasGuestSession = true,
            rejoinDiscardsSession = false
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "감귤에이드", 3)
        val failure = assertIs<AppResult.Failure>(result)

        assertEquals(listOf("rejoin"), repository.calls)
        assertIs<AppError.NetworkError>(failure.error)
    }

    // 백엔드가 닉네임 중복을 기입장보다 먼저 검사하고, 그 검사는 내 옛 행도 센다.
    // 그래서 같은 이름으로 돌아오면 재입장에 닿지도 못하고 "닉네임 중복"으로 막힌다 —
    // 한 번 재입장을 시도해 내 자리가 맞는지 서버에 물어본다
    @Test
    fun nicknameTakenByMyOwnRowFallsBackToRejoin() = runTest {
        val resumed = MyParticipation(11L, 1L, "482913", "파워에이드", 3, false, isRejoined = true)
        val repository = FakeRoomRepository(
            joinResult = AppResult.Failure(AppError.Conflict(serverCode = "NICKNAME_DUPLICATED")),
            rejoinResult = AppResult.Success(resumed)
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "파워에이드", 3)

        assertEquals(listOf("join", "rejoin"), repository.calls)
        assertEquals(resumed, (result as AppResult.Success).value)
    }

    // 진짜로 남이 쓰는 이름이면 내 참가 기록이 없어 재입장이 404다 — 원래 닉네임 안내로 돌아간다.
    // 재입장은 닉네임을 인자로 받지 않으므로 남의 자리로 들어갈 수 없다
    @Test
    fun nicknameTakenBySomeoneElseKeepsOriginalError() = runTest {
        val repository = FakeRoomRepository(
            joinResult = AppResult.Failure(AppError.Conflict(serverCode = "NICKNAME_DUPLICATED")),
            rejoinResult = AppResult.Failure(AppError.NotFound(serverCode = "PARTICIPANT_NOT_FOUND"))
        )
        val useCase = JoinRoomUseCase(repository)

        val result = useCase.invoke(roomInfo(), "남의이름", 3)
        val failure = assertIs<AppResult.Failure>(result)

        assertEquals(listOf("join", "rejoin"), repository.calls)
        assertEquals("NICKNAME_DUPLICATED", failure.error.serverCode)
    }

    // 게스트 기록으로 이미 재입장을 해봤으면 또 부르지 않는다 — 방금 실패한 호출을 반복할 뿐이다
    @Test
    fun alreadyAttemptedRejoinIsNotRetriedOnNicknameConflict() = runTest {
        val repository = FakeRoomRepository(
            joinResult = AppResult.Failure(AppError.Conflict(serverCode = "NICKNAME_DUPLICATED")),
            rejoinResult = AppResult.Failure(AppError.Unauthorized()),
            hasGuestSession = true
        )
        val useCase = JoinRoomUseCase(repository)

        useCase.invoke(roomInfo(), "파워에이드", 3)

        assertEquals(listOf("rejoin", "join"), repository.calls)
    }
}
