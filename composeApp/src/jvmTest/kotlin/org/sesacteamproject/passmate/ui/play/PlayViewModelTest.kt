package org.sesacteamproject.passmate.ui.play

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.network.SessionEventStream
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

    private fun viewModel(stream: FakeSessionEventStream): PlayViewModel {
        val roomRepository = FakeRoomRepository(roomInfo = runningRoom())
        val sessionRepository = FakeSessionRepository()

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
}
