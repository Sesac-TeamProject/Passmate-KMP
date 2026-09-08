package org.sesacteamproject.passmate.ui.play

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.event.ServerEvent
import org.sesacteamproject.passmate.core.network.event.ServerEventFrame
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase
import org.sesacteamproject.passmate.session.domain.policy.SnapshotPolicy
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetVoiceHintsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SubmitAnswerUseCase
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.FakeSessionEventStream
import org.sesacteamproject.passmate.testing.FakeSessionRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.model.PendingGuestClaim
import org.sesacteamproject.passmate.user.domain.usecase.RequestGuestClaimUseCase

// M-07 연결 끊김·재접속 — 스트림 Connected/Disconnected를 uiState.isDisconnected로 옮기고,
// Reconnect 액션은 백오프를 기다리지 않고 즉시 재구독한다
@OptIn(ExperimentalCoroutinesApi::class)
class PlayViewModelTest {

    private fun runningRoom(): RoomInfo {
        return RoomInfo(
            roomId = 1L,
            pin = "123456",
            title = "진행 중인 방",
            topic = null,
            status = RoomStatus.RUNNING,
            questionCount = 10,
            estimatedMinutes = 15,
            scheduledAt = null,
            participantCount = 1,
            maxParticipants = 30,
            isPaid = false,
            entryFee = null,
            isGuestAllowed = true,
            host = null
        )
    }

    private fun viewModel(
        stream: FakeSessionEventStream,
        sessionRepository: FakeSessionRepository = FakeSessionRepository()
    ): PlayViewModel {
        val roomRepository = FakeRoomRepository(roomInfo = runningRoom())

        return PlayViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(roomRepository),
            getSessionSnapshotUseCase = GetSessionSnapshotUseCase(sessionRepository),
            submitAnswerUseCase = SubmitAnswerUseCase(sessionRepository),
            getVoiceHintsUseCase = GetVoiceHintsUseCase(sessionRepository),
            leaveRoomUseCase = LeaveRoomUseCase(roomRepository),
            getMyParticipationUseCase = GetMyParticipationUseCase(roomRepository),
            requestGuestClaimUseCase = RequestGuestClaimUseCase(PendingGuestClaim()),
            snapshotPolicy = SnapshotPolicy(),
            sessionEventStream = stream
        )
    }

    private fun questionStartedFrame(): SessionEventStream.StreamEvent.Received {
        val event = ServerEvent.QuestionStarted(
            questionId = 10L,
            questionNo = 1,
            type = "MULTIPLE_CHOICE",
            body = "다음 중 옳은 것은?",
            choices = listOf("1번", "2번", "3번", "4번"),
            points = 100,
            timeLimitSec = 30,
            endsAt = "2026-09-09T00:00:30"
        )

        return SessionEventStream.StreamEvent.Received(ServerEventFrame(ts = "2026-09-09T00:00:00", event = event))
    }

    // 문항이 살아 있으면 남은 시간 티커가 계속 돌아 runTest가 가상 시간을 무한히 진행한다.
    // 검증이 끝나면 마감 프레임으로 티커를 세운다 (이 이벤트는 event를 발행하지 않는다)
    private fun questionEndedFrame(): SessionEventStream.StreamEvent.Received {
        val event = ServerEvent.QuestionEnded(
            questionNo = 1,
            answerReveal = ServerEvent.QuestionEnded.AnswerReveal(answer = "1번"),
            correctCount = 0
        )

        return SessionEventStream.StreamEvent.Received(ServerEventFrame(ts = "2026-09-09T00:00:31", event = event))
    }

    // 문항이 뜬 상태에서 보기를 고르고 제출한다 — 제출 실패 문구를 보려면 여기까지 와야 한다
    private suspend fun TestScope.submitWith(error: AppError): Pair<PlayViewModel, List<PlayEvent>> {
        val stream = FakeSessionEventStream()
        val sessionRepository = FakeSessionRepository()
        val viewModel = viewModel(stream, sessionRepository)
        val events = mutableListOf<PlayEvent>()

        sessionRepository.submitError = error
        backgroundScope.launch(Dispatchers.Main) { viewModel.event.toList(events) }
        viewModel.onAction(PlayAction.Enter("123456"))
        stream.emit(questionStartedFrame())
        viewModel.onAction(PlayAction.SelectChoice(0))
        viewModel.onAction(PlayAction.ClickSubmit)
        stream.emit(questionEndedFrame())

        return viewModel to events
    }

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }

    @Test
    fun disconnectedStreamMarksStateAsDisconnected() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)

        viewModel.onAction(PlayAction.Enter("123456"))
        stream.emit(SessionEventStream.StreamEvent.Disconnected)

        assertTrue(viewModel.uiState.value.isDisconnected)
    }

    @Test
    fun reconnectedStreamClearsDisconnectedState() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)

        viewModel.onAction(PlayAction.Enter("123456"))
        stream.emit(SessionEventStream.StreamEvent.Disconnected)
        assertTrue(viewModel.uiState.value.isDisconnected)
        stream.emit(SessionEventStream.StreamEvent.Connected)

        assertFalse(viewModel.uiState.value.isDisconnected)
    }

    @Test
    fun reconnectActionResubscribesImmediately() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)

        viewModel.onAction(PlayAction.Enter("123456"))
        stream.emit(SessionEventStream.StreamEvent.Disconnected)
        viewModel.onAction(PlayAction.Reconnect)

        assertEquals(2, stream.subscribeCount)
    }

    // 방 정보를 아직 못 받았으면 구독할 roomId가 없다 — 아무것도 하지 않는다
    @Test
    fun reconnectBeforeRoomLoadedDoesNothing() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)

        viewModel.onAction(PlayAction.Reconnect)

        assertEquals(0, stream.subscribeCount)
    }

    // 서버는 방·문항·참가자를 전부 404로 준다 — code를 보지 않으면 참가 기록이 없는데도
    // "방을 찾을 수 없어요"가 떠서 원인을 못 찾는다 (규칙 §10)
    @Test
    fun participantNotFoundOnSubmitAsksToEnterAgain() = runTest {
        val error = AppError.NotFound("PARTICIPANT_NOT_FOUND", "이 방에 입장한 기록이 없습니다.")
        val (viewModel, events) = submitWith(error)

        assertEquals(PlayEvent.ShowNotice("이 방에 입장한 기록이 없어요. 다시 입장해 주세요"), events.last())
        assertFalse(viewModel.uiState.value.hasSubmitted)
    }

    // 화면 잠금도 409지만 중복 제출이 아니다 — 잠금이 풀리면 다시 낼 수 있어야 한다
    @Test
    fun screenLockedOnSubmitKeepsQuestionSubmittable() = runTest {
        val error = AppError.Conflict("SCREEN_LOCKED", "화면이 잠겨 있어 지금은 답안을 낼 수 없습니다.")
        val (viewModel, events) = submitWith(error)

        assertEquals(PlayEvent.ShowNotice("선생님이 화면을 잠갔어요"), events.last())
        assertFalse(viewModel.uiState.value.hasSubmitted)
    }

    // 마감(QUESTION_NOT_RUNNING)은 409로 오지만 다시 낼 수 없는 실패다
    @Test
    fun closedQuestionOnSubmitMarksAsSubmitted() = runTest {
        val error = AppError.Conflict("QUESTION_NOT_RUNNING", "지금 풀 수 있는 문항이 아닙니다.")
        val (viewModel, events) = submitWith(error)

        assertEquals(PlayEvent.ShowNotice("이미 마감된 문항이에요"), events.last())
        assertTrue(viewModel.uiState.value.hasSubmitted)
    }
}
