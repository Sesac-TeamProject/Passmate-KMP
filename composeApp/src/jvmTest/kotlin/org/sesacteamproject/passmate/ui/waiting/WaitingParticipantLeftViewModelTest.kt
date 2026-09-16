package org.sesacteamproject.passmate.ui.waiting

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.event.ServerEvent
import org.sesacteamproject.passmate.core.network.event.ServerEventFrame
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.FakeSessionEventStream
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// 강퇴 — 서버의 PARTICIPANT_LEFT에는 아직 reason이 없다(백엔드 요청 대기).
// "나가기"는 구독부터 끊고 퇴장을 부르므로, 구독 중에 받은 내 퇴장은 남이 나를 내보낸 것이다
@OptIn(ExperimentalCoroutinesApi::class)
class WaitingParticipantLeftViewModelTest {

    private val me = Participant(participantId = 11L, nickname = "포카리", avatarId = 1, isGuest = true, isConnected = true)

    private val other = Participant(participantId = 12L, nickname = "파워에이드", avatarId = 2, isGuest = true, isConnected = true)

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
            participantCount = 2,
            maxParticipants = 30,
            isPaid = false,
            entryFee = null,
            isGuestAllowed = true,
            host = null
        )
    }

    private fun viewModel(stream: FakeSessionEventStream): WaitingViewModel {
        val roomRepository = FakeRoomRepository(roomInfo = waitingRoom())

        roomRepository.currentParticipation = MyParticipation(
            participantId = 11L,
            roomId = 1L,
            pin = "123456",
            nickname = "포카리",
            avatarId = 1,
            isGuest = true
        )
        roomRepository.participantsResult = AppResult.Success(listOf(me, other))
        return WaitingViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(roomRepository),
            getParticipantsUseCase = GetParticipantsUseCase(roomRepository),
            leaveRoomUseCase = LeaveRoomUseCase(roomRepository),
            getMyParticipationUseCase = GetMyParticipationUseCase(roomRepository),
            sessionEventStream = stream
        )
    }

    // 배포 서버가 실제로 보내는 모양 — reason이 없다
    private fun participantLeftFrame(participantId: Long): SessionEventStream.StreamEvent.Received {
        val event = ServerEvent.ParticipantLeft(participantId = participantId, reason = null)

        return SessionEventStream.StreamEvent.Received(ServerEventFrame(ts = "2026-09-13T10:00:00", event = event))
    }

    // KICKED일 때만 닫으면 reason이 없는 서버에서는 강퇴당해도 대기실에 남는다 (실기기 A-6)
    @Test
    fun myParticipantLeftWithoutReasonClosesRoom() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)
        val events = mutableListOf<WaitingEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(WaitingAction.Enter("123456"))
        stream.emit(participantLeftFrame(participantId = 11L))

        assertIs<WaitingEvent.RoomClosed>(events.single())
    }

    // 남이 나가면 명단에서만 지운다 — 내 퇴장으로 오판해 방을 닫으면 안 된다
    @Test
    fun otherParticipantLeftOnlyRemovesFromList() = runTest {
        val stream = FakeSessionEventStream()
        val viewModel = viewModel(stream)
        val events = mutableListOf<WaitingEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(WaitingAction.Enter("123456"))
        stream.emit(participantLeftFrame(participantId = 12L))

        assertTrue(events.isEmpty())
        assertEquals(listOf(me), viewModel.uiState.value.participants)
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
