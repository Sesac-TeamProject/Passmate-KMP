package org.sesacteamproject.passmate.navigation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {

    private fun viewModel(isSignedIn: Boolean): AppShellViewModel {
        return AppShellViewModel(IsSignedInUseCase(FakeAuthRepository(isSignedIn)))
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
    fun memberSelectingAnyTabNavigates() = runTest {
        val viewModel = viewModel(isSignedIn = true)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.MY_INFO))

        assertEquals(listOf<AppShellEvent>(AppShellEvent.NavigateToTab(AppTab.MY_INFO)), events)
        assertEquals(true, viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun guestSelectingSignInRequiredTabRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.JOINED_ROOMS))

        assertEquals(listOf<AppShellEvent>(AppShellEvent.RequireSignIn), events)
        assertEquals(false, viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun guestSelectingHomeNavigates() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.HOME))

        assertEquals(listOf<AppShellEvent>(AppShellEvent.NavigateToTab(AppTab.HOME)), events)
    }

    @Test
    fun signInStateIsReadOnEverySelection() = runTest {
        val authRepository = FakeAuthRepository(isSignedIn = false)
        val viewModel = AppShellViewModel(IsSignedInUseCase(authRepository))
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.MY_INFO))
        authRepository.isSignedIn = true
        viewModel.onAction(AppShellAction.SelectTab(AppTab.MY_INFO))

        assertEquals(
            listOf(AppShellEvent.RequireSignIn, AppShellEvent.NavigateToTab(AppTab.MY_INFO)),
            events
        )
    }

    @Test
    fun tabRouteLookup() {
        assertEquals(AppTab.JOINED_ROOMS, AppTab.fromRoute("joinedRooms"))
        assertEquals(null, AppTab.fromRoute("waiting/{pin}"))
        assertEquals(null, AppTab.fromRoute(null))
    }
}
