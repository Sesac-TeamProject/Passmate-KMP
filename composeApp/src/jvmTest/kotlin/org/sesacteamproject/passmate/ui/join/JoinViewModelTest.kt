package org.sesacteamproject.passmate.ui.join

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class JoinViewModelTest {

    private fun paidRoom(): RoomInfo {
        return RoomInfo(
            roomId = 1L,
            pin = "123456",
            title = "유료 방",
            topic = null,
            status = RoomStatus.WAITING,
            questionCount = 10,
            estimatedMinutes = 15,
            scheduledAt = null,
            participantCount = 3,
            maxParticipants = 30,
            isPaid = true,
            entryFee = 100,
            host = null
        )
    }

    private fun viewModel(roomRepository: FakeRoomRepository, isSignedIn: Boolean): JoinViewModel {
        return JoinViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(roomRepository),
            joinRoomUseCase = JoinRoomUseCase(roomRepository),
            isSignedInUseCase = IsSignedInUseCase(FakeAuthRepository(isSignedIn)),
            joinInputPolicy = JoinInputPolicy()
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

    // 규칙 §12 가드 시나리오 — 게스트의 유료 방 입장은 결제 화면을 목적지로 로그인 유도한다 (스펙 §3)
    @Test
    fun guestJoiningPaidRoomRequestsSignInWithPaymentTarget() = runTest {
        val roomRepository = FakeRoomRepository(roomInfo = paidRoom())
        val viewModel = viewModel(roomRepository, isSignedIn = false)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(JoinEvent.SignInRequiredForPaidRoom("123456"), events.last())
        assertEquals(0, roomRepository.joinCallCount)
    }

    // 로그인 링크는 목적지가 없다 — 로그인 후 홈으로 (스펙 §3)
    @Test
    fun clickingSignInLinkRequestsPlainSignIn() = runTest {
        val roomRepository = FakeRoomRepository()
        val viewModel = viewModel(roomRepository, isSignedIn = false)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ClickSignIn)

        assertEquals(listOf<JoinEvent>(JoinEvent.SignInRequested), events)
    }
}
