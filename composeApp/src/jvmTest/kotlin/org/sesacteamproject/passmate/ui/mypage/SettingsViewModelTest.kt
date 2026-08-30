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
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private fun viewModel(
        isSignedIn: Boolean,
        deleteResult: AppResult<Unit> = AppResult.Success(Unit)
    ): SettingsViewModel {
        val authRepository = FakeAuthRepository(isSignedIn)
        val userRepository = FakeUserRepository(deleteResult = deleteResult)

        return SettingsViewModel(
            deleteAccountUseCase = DeleteAccountUseCase(userRepository, authRepository),
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
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)

        assertEquals(listOf<SettingsEvent>(SettingsEvent.RequireSignIn), events)
    }

    @Test
    fun deleteAccountSuccessEmitsAccountDeleted() = runTest {
        val viewModel = viewModel(isSignedIn = true)
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)
        viewModel.onAction(SettingsAction.ConfirmDeleteAccount)

        assertEquals(listOf<SettingsEvent>(SettingsEvent.AccountDeleted), events)
        assertEquals(false, viewModel.uiState.value.isProcessing)
    }

    @Test
    fun deleteAccountConflictShowsServerMessage() = runTest {
        val viewModel = viewModel(
            isSignedIn = true,
            deleteResult = AppResult.Failure(AppError.Conflict(serverMessage = "정산 대기 금액이 있어요"))
        )
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)
        viewModel.onAction(SettingsAction.ConfirmDeleteAccount)

        assertEquals(listOf<SettingsEvent>(SettingsEvent.ShowNotice("정산 대기 금액이 있어요")), events)
    }
}
