package org.sesacteamproject.passmate.ui.waiting

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
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.FakeSessionEventStream
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// M-07 연결 끊김·재접속 — 스트림 Connected/Disconnected를 uiState.isDisconnected로 옮기고,
// Reconnect 액션은 백오프를 기다리지 않고 즉시 재구독한다
@OptIn(ExperimentalCoroutinesApi::class)
class WaitingReconnectViewModelTest {

    private fun waitingRoom(): RoomInfo {
        return RoomInfo(
            roomId = 1L,
            pin = "123456",
            title = "대기 중인 방",
            topic = null,
            status = RoomStatus.WAITING,
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

    private fun viewModel(stream: FakeSessionEventStream): WaitingViewModel {
        val roomRepository = FakeRoomRepository(roomInfo = waitingRoom())

        return WaitingViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(roomRepository),
            getParticipantsUseCase = GetParticipantsUseCase(roomRepository),
            leaveRoomUseCase = LeaveRoomUseCase(roomRepository),
            getMyParticipationUseCase = GetMyParticipationUseCase(roomRepository),
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

        viewModel.onAction(WaitingAction.Enter("123456"))
        stream.emit(SessionEventStream.StreamEvent.Disconnected)

        assertTrue(viewModel.uiState.value.isDisconnected)
    }

    @Test
    fun reconnectedStreamClearsDisconnectedState() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)

        viewModel.onAction(WaitingAction.Enter("123456"))
        stream.emit(SessionEventStream.StreamEvent.Disconnected)
        assertTrue(viewModel.uiState.value.isDisconnected)
        stream.emit(SessionEventStream.StreamEvent.Connected)

        assertFalse(viewModel.uiState.value.isDisconnected)
    }

    @Test
    fun reconnectActionResubscribesImmediately() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)

        viewModel.onAction(WaitingAction.Enter("123456"))
        stream.emit(SessionEventStream.StreamEvent.Disconnected)
        viewModel.onAction(WaitingAction.Reconnect)

        assertEquals(2, stream.subscribeCount)
    }

    // 방 정보를 아직 못 받았으면 구독할 roomId가 없다 — 아무것도 하지 않는다
    @Test
    fun reconnectBeforeRoomLoadedDoesNothing() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)

        viewModel.onAction(WaitingAction.Reconnect)

        assertEquals(0, stream.subscribeCount)
    }
}
