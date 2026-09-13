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
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
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
            isGuestAllowed = true,
            host = null
        )
    }

    private fun freeRoom(): RoomInfo {
        return paidRoom().copy(title = "무료 방", isPaid = false, entryFee = null)
    }

    private fun joinFailingWith(error: AppError): FakeRoomRepository {
        val roomRepository = FakeRoomRepository(roomInfo = freeRoom())

        roomRepository.joinResult = AppResult.Failure(error)
        return roomRepository
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

    // 서버는 409를 네 가지 이유로 준다 — code로 갈라야 문구가 맞다 (규칙 §10)
    // 회원이 이미 들어와 있는 방이면 막지 않고 그대로 들여보낸다 (규칙 §2-1-2 재접속 복구)
    @Test
    fun alreadyJoinedMemberEntersRoomInsteadOfBeingBlocked() = runTest {
        val roomRepository = joinFailingWith(AppError.Conflict(serverCode = "ALREADY_JOINED"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(JoinEvent.JoinCompleted("123456"), events.last())
        assertEquals(false, viewModel.uiState.value.isJoining)
    }

    @Test
    fun roomNotJoinableIsNotReportedAsNicknameClash() = runTest {
        val roomRepository = joinFailingWith(AppError.Conflict(serverCode = "ROOM_NOT_JOINABLE"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(JoinEvent.ShowNotice("이미 시작했거나 끝난 방이라 입장할 수 없어요"), events.last())
    }

    @Test
    fun roomFullIsNotReportedAsNicknameClash() = runTest {
        val roomRepository = joinFailingWith(AppError.Conflict(serverCode = "ROOM_FULL"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(JoinEvent.ShowNotice("정원이 가득 찼어요"), events.last())
    }

    @Test
    fun nicknameDuplicatedKeepsNicknameGuidance() = runTest {
        val roomRepository = joinFailingWith(AppError.Conflict(serverCode = "NICKNAME_DUPLICATED"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(
            JoinEvent.ShowNotice("이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"),
            events.last()
        )
    }

    // 403도 code로 갈라야 한다 — 방장이 자기 방에 들어오려는 것과 일반 권한 거부는 원인이 다르다 (규칙 §10)
    @Test
    fun hostCannotJoinExplainsOwnRoomInsteadOfGenericFailure() = runTest {
        val roomRepository = joinFailingWith(AppError.PermissionDenied(serverCode = "HOST_CANNOT_JOIN"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(
            JoinEvent.ShowNotice("내가 만든 방에는 참가자로 입장할 수 없어요"),
            events.last()
        )
    }

    @Test
    fun otherForbiddenKeepsPermissionGuidance() = runTest {
        val roomRepository = joinFailingWith(AppError.PermissionDenied(serverCode = "ACCESS_DENIED"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(JoinEvent.ShowNotice("이 방에 입장할 권한이 없어요"), events.last())
    }
}
