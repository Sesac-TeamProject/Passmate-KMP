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
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// 탈퇴 동작 테스트는 DeleteAccountViewModelTest로 옮겼다 (M-12-12 전용 화면 분리)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private fun viewModel(isSignedIn: Boolean): SettingsViewModel {
        return SettingsViewModel(isSignedInUseCase = IsSignedInUseCase(FakeAuthRepository(isSignedIn)))
    }

    @Test
    fun guestEnterRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)

        assertEquals(listOf<SettingsEvent>(SettingsEvent.RequireSignIn), events)
    }

    @Test
    fun memberEnterEmitsNothing() = runTest {
        val viewModel = viewModel(isSignedIn = true)
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)

        assertEquals(emptyList(), events)
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
