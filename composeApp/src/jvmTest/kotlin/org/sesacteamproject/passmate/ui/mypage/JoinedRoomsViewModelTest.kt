package org.sesacteamproject.passmate.ui.mypage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.model.JoinedRoom
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.MyPageSummary
import org.sesacteamproject.passmate.user.domain.usecase.GetMyPageUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class JoinedRoomsViewModelTest {

    private val summary = MyPageSummary(
        participationCount = 3,
        accuracyPercent = 71,
        avgRank = 3.3,
        trendText = "지난주보다 정답률이 8%p 올랐어요",
        weakTopics = listOf("JPA 영속성", "트랜잭션")
    )

    private fun room(id: Long, title: String): JoinedRoom {
        return JoinedRoom(
            roomId = id,
            title = title,
            dateLabel = "8/22 (금)",
            questionCount = 8,
            myScore = 990.0,
            myRank = 3,
            hasReport = true
        )
    }

    private fun viewModel(isSignedIn: Boolean, pages: List<AppResult<MyPage>>): JoinedRoomsViewModel {
        val authRepository = FakeAuthRepository(isSignedIn)
        val userRepository = FakeUserRepository(myPageResults = pages)

        return JoinedRoomsViewModel(
            getMyPageUseCase = GetMyPageUseCase(userRepository),
            isSignedInUseCase = IsSignedInUseCase(authRepository)
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
    fun guestEnterRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false, pages = emptyList())
        val events = mutableListOf<JoinedRoomsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinedRoomsAction.Enter)

        assertEquals(listOf<JoinedRoomsEvent>(JoinedRoomsEvent.RequireSignIn), events)
        assertEquals(true, viewModel.uiState.value.isLoading)
    }

    @Test
    fun memberEnterLoadsFirstPageAndLoadMoreAppends() = runTest {
        val firstPage = MyPage(summary = summary, ongoing = null, rooms = listOf(room(1, "Spring 스터디")), nextCursor = "c1")
        val secondPage = MyPage(summary = summary, ongoing = null, rooms = listOf(room(2, "CS 모의면접")), nextCursor = null)
        val viewModel = viewModel(
            isSignedIn = true,
            pages = listOf(AppResult.Success(firstPage), AppResult.Success(secondPage))
        )

        viewModel.onAction(JoinedRoomsAction.Enter)

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(listOf(1L), viewModel.uiState.value.rooms.map { it.roomId })
        assertEquals("c1", viewModel.uiState.value.nextCursor)

        viewModel.onAction(JoinedRoomsAction.LoadMore)

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.rooms.map { it.roomId })
        assertEquals(null, viewModel.uiState.value.nextCursor)
    }

    @Test
    fun reportAndRejoinEmitNavigationEvents() = runTest {
        val viewModel = viewModel(isSignedIn = true, pages = emptyList())
        val events = mutableListOf<JoinedRoomsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinedRoomsAction.ClickRoomReport(7L))
        viewModel.onAction(JoinedRoomsAction.ClickRejoin("482913"))

        assertEquals(
            listOf(JoinedRoomsEvent.OpenReport(7L), JoinedRoomsEvent.Rejoin("482913")),
            events
        )
    }
}
