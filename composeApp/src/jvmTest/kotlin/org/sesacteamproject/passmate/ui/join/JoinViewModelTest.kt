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
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.RejoinRoomUseCase
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

    // 재입장이 돌려주는 원래 참가자 행 — 참가자 id가 채워져야 대기실 강조·강퇴 안내가 산다
    private fun myParticipation(): MyParticipation {
        return MyParticipation(
            participantId = 77L,
            roomId = 1L,
            pin = "123456",
            nickname = "처음이름",
            avatarId = 3,
            isGuest = false
        )
    }

    private fun viewModel(roomRepository: FakeRoomRepository, isSignedIn: Boolean): JoinViewModel {
        return JoinViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(roomRepository),
            joinRoomUseCase = JoinRoomUseCase(roomRepository),
            rejoinRoomUseCase = RejoinRoomUseCase(roomRepository),
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

        roomRepository.rejoinResult = AppResult.Success(myParticipation())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        // 새 입장이 아니라 재입장으로 들어가야 참가자 id가 채워진다
        assertEquals(1, roomRepository.rejoinCallCount)
        assertEquals(JoinEvent.JoinCompleted("123456"), events.last())
        assertEquals(false, viewModel.uiState.value.isJoining)
    }

    // 강퇴당한 사람은 재입장이 403이다 — 다시 눌러도 안 되는 상황이라 원인을 말해야 한다
    @Test
    fun kickedParticipantIsToldRejoinIsImpossible() = runTest {
        val roomRepository = joinFailingWith(AppError.Conflict(serverCode = "ALREADY_JOINED"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        roomRepository.rejoinResult = AppResult.Failure(AppError.PermissionDenied(serverCode = "ACCESS_DENIED"))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(JoinEvent.ShowNotice("내보내진 방에는 다시 들어올 수 없어요"), events.last())
    }

    // 진행 중인 방은 새 입장이 막히지만(409 ROOM_NOT_JOINABLE) 전에 들어갔던 사람은 돌아올 수 있다
    @Test
    fun runningRoomLetsPreviousParticipantBackInThroughRejoin() = runTest {
        val roomRepository = joinFailingWith(AppError.Conflict(serverCode = "ROOM_NOT_JOINABLE"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        roomRepository.rejoinResult = AppResult.Success(myParticipation())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(1, roomRepository.rejoinCallCount)
        assertEquals(JoinEvent.JoinCompleted("123456"), events.last())
    }

    @Test
    fun roomNotJoinableIsNotReportedAsNicknameClash() = runTest {
        val roomRepository = joinFailingWith(AppError.Conflict(serverCode = "ROOM_NOT_JOINABLE"))
        val viewModel = viewModel(roomRepository, isSignedIn = true)
        val events = mutableListOf<JoinEvent>()

        // 들어간 적이 없으면 재입장도 404다 — 원래 문구로 돌아간다
        roomRepository.rejoinResult = AppResult.Failure(AppError.NotFound(serverCode = "PARTICIPANT_NOT_FOUND"))
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

    // 재입장으로 들어오면 입력한 이름이 아니라 처음 이름으로 들어간다 — 말없이 바뀌면 혼란스럽다
    @Test
    fun rejoinedEntryTellsUserTheyResumedOriginalSeat() = runTest {
        val roomRepository = FakeRoomRepository(roomInfo = freeRoom())
        val viewModel = viewModel(roomRepository, isSignedIn = false)
        val events = mutableListOf<JoinEvent>()

        roomRepository.joinResult = AppResult.Success(myParticipation().copy(isRejoined = true))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("감귤에이드"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(
            listOf<JoinEvent>(
                JoinEvent.ShowNotice("이미 입장해 있는 방이에요. 처음 입장한 이름으로 이어서 들어갈게요"),
                JoinEvent.JoinCompleted("123456")
            ),
            events
        )
    }

    // 평범한 첫 입장에는 안내를 띄우지 않는다
    @Test
    fun freshEntryShowsNoResumeNotice() = runTest {
        val roomRepository = FakeRoomRepository(roomInfo = freeRoom())
        val viewModel = viewModel(roomRepository, isSignedIn = false)
        val events = mutableListOf<JoinEvent>()

        roomRepository.joinResult = AppResult.Success(myParticipation())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("감귤에이드"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(listOf<JoinEvent>(JoinEvent.JoinCompleted("123456")), events)
    }
}
