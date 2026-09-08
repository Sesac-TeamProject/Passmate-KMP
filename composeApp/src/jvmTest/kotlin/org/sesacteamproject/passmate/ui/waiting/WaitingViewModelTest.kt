package org.sesacteamproject.passmate.ui.waiting

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.StompClient
import org.sesacteamproject.passmate.core.storage.TokenStorage
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// 종료된 세션에서 "입장 완료" 대기실이 남지 않아야 한다 (규칙 §2-1-2 — FINISHED는 Result로).
// 여기 테스트는 STOMP 구독 경로(observeRoomEvents)를 타지 않는 상태만 다룬다 —
// WAITING으로 끝나는 분기는 실제 웹소켓 연결을 시도하므로 단위 테스트 대상이 아니다
@OptIn(ExperimentalCoroutinesApi::class)
class WaitingViewModelTest {

    private fun room(status: RoomStatus): RoomInfo {
        return RoomInfo(
            roomId = 7L,
            pin = "123456",
            title = "안드로이드 면접",
            topic = null,
            status = status,
            questionCount = 5,
            estimatedMinutes = 10,
            scheduledAt = null,
            participantCount = 2,
            maxParticipants = 30,
            isPaid = false,
            entryFee = null,
            isGuestAllowed = true,
            host = null
        )
    }

    private fun viewModel(repository: FakeRoomRepository): WaitingViewModel {
        return WaitingViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(repository),
            getParticipantsUseCase = GetParticipantsUseCase(repository),
            leaveRoomUseCase = LeaveRoomUseCase(repository),
            getMyParticipationUseCase = GetMyParticipationUseCase(repository),
            // FINISHED·RUNNING 분기는 이 스트림을 구독하지 않는다 — 연결은 일어나지 않는다
            sessionEventStream = SessionEventStream(StompClient(TokenStorage(), "ws://127.0.0.1:1/ws"))
        )
    }

    // 이미 끝난 방으로 들어오면 대기실에 머무르지 않고 결과로 보낸다
    @Test
    fun entersFinishedRoomAndGoesToResult() = runTest {
        val repository = FakeRoomRepository(roomInfo = room(RoomStatus.FINISHED))
        val viewModel = viewModel(repository)
        val events = mutableListOf<WaitingEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(WaitingAction.Enter("123456"))

        assertEquals(listOf<WaitingEvent>(WaitingEvent.SessionFinished(roomId = 7L)), events)
        assertTrue(viewModel.uiState.value.isSessionFinished)
    }

    // 재현 경로: 대기실 → (RUNNING이라 풀이로) → 세션 종료 → 뒤로가기로 대기실 복귀.
    // 이때 "입장 완료" 화면이 유지되면 안 되고 결과로 가야 한다
    @Test
    fun returningAfterSessionEndedLeavesWaitingRoom() = runTest {
        val repository = FakeRoomRepository(roomInfo = room(RoomStatus.RUNNING))
        val viewModel = viewModel(repository)
        val events = mutableListOf<WaitingEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(WaitingAction.Enter("123456"))
        repository.roomInfo = room(RoomStatus.FINISHED)
        viewModel.onAction(WaitingAction.Enter("123456"))

        assertEquals(
            listOf<WaitingEvent>(
                WaitingEvent.SessionStarted(pin = "123456"),
                WaitingEvent.SessionFinished(roomId = 7L)
            ),
            events
        )
        assertTrue(viewModel.uiState.value.isSessionFinished)
    }

    // 참가자 조회가 STOMP 연결 성공에 묶여 있으면 연결이 늦거나 실패할 때 "학생 0명"이 남는다.
    // 방 정보 로드 직후 REST로 곧바로 불러와야 한다 (규칙 §2-1-2 — 초기 목록은 REST)
    @Test
    fun loadsParticipantsWithoutWaitingForWebSocket() = runTest {
        val repository = FakeRoomRepository(roomInfo = room(RoomStatus.WAITING))
        val members = listOf(
            Participant(participantId = 1L, nickname = "민지", avatarId = 1, isGuest = false, isConnected = true),
            Participant(participantId = 2L, nickname = "준영", avatarId = 2, isGuest = true, isConnected = true)
        )

        repository.participantsResult = AppResult.Success(members)
        val viewModel = viewModel(repository)

        viewModel.onAction(WaitingAction.Enter("123456"))

        val state = viewModel.uiState.value

        assertEquals(1, repository.participantsCallCount)
        assertEquals(members, state.participants)
        assertEquals(2, state.totalCount)
        assertFalse(state.isParticipantsLoading)
    }

    // 조회 실패를 삼키면 "학생 0명이 함께해요"로 둔갑해 방이 빈 것처럼 보인다.
    // RUNNING으로 진입하면 STOMP를 구독하지 않아 조회 경로만 따로 검증할 수 있다
    @Test
    fun surfacesParticipantsFailureInsteadOfShowingZero() = runTest {
        val repository = FakeRoomRepository(roomInfo = room(RoomStatus.RUNNING))

        repository.participantsResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel(repository)

        viewModel.onAction(WaitingAction.Enter("123456"))
        viewModel.onAction(WaitingAction.RetryParticipants)

        val failed = viewModel.uiState.value

        assertTrue(failed.hasParticipantsError)
        assertFalse(failed.isParticipantsLoading)
        assertEquals(0, failed.totalCount)

        // 다시 시도해서 성공하면 오류 표시가 걷힌다
        repository.participantsResult = AppResult.Success(
            listOf(Participant(participantId = 1L, nickname = "민지", avatarId = 1, isGuest = false, isConnected = true))
        )
        viewModel.onAction(WaitingAction.RetryParticipants)

        val recovered = viewModel.uiState.value

        assertFalse(recovered.hasParticipantsError)
        assertEquals(1, recovered.totalCount)
    }

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }
}
