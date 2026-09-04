package org.sesacteamproject.passmate.ui.auth

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.DevSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.IsDevSignInAvailableUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.model.PendingGuestClaim
import org.sesacteamproject.passmate.user.domain.usecase.CompleteGuestClaimUseCase

// 개발용 로그인은 로컬 개발 서버에서만 노출된다 — 운영 URL에서 새면 안 된다
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private lateinit var authRepository: FakeAuthRepository

    private fun viewModel(
        isDevSignInAvailable: Boolean = true,
        devSignInResult: AppResult<Unit> = AppResult.Success(Unit)
    ): SignInViewModel {
        authRepository = FakeAuthRepository(false)
        authRepository.isDevSignInAvailable = isDevSignInAvailable
        authRepository.devSignInResult = devSignInResult

        val userRepository = FakeUserRepository()
        val completeGuestClaimUseCase = CompleteGuestClaimUseCase(PendingGuestClaim(), userRepository)

        return SignInViewModel(
            buildGoogleSignInUrlUseCase = BuildGoogleSignInUrlUseCase(authRepository),
            completeSignInUseCase = CompleteSignInUseCase(authRepository),
            completeGuestClaimUseCase = completeGuestClaimUseCase,
            devSignInUseCase = DevSignInUseCase(authRepository),
            isDevSignInAvailableUseCase = IsDevSignInAvailableUseCase(authRepository)
        )
    }

    @Test
    fun hidesDevSignInWhenServerIsNotLocal() = runTest {
        val target = viewModel(isDevSignInAvailable = false)

        assertFalse(target.uiState.value.isDevSignInAvailable)
    }

    @Test
    fun showsDevSignInOnLocalServer() = runTest {
        val target = viewModel(isDevSignInAvailable = true)

        assertTrue(target.uiState.value.isDevSignInAvailable)
    }

    @Test
    fun devSignInStoresSessionAndCompletes() = runTest {
        val target = viewModel()
        val events = mutableListOf<SignInEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.event.collect { events.add(it) }
        }

        target.onAction(SignInAction.ClickDevSignIn)

        assertEquals(1, authRepository.devSignInCount)
        assertTrue(authRepository.isSignedIn())
        assertFalse(target.uiState.value.isSigningIn)
        assertTrue(events.contains(SignInEvent.SignInCompleted))
    }

    @Test
    fun devSignInFailureShowsNoticeAndKeepsGuest() = runTest {
        val target = viewModel(devSignInResult = AppResult.Failure(AppError.NetworkError()))
        val events = mutableListOf<SignInEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.event.collect { events.add(it) }
        }

        target.onAction(SignInAction.ClickDevSignIn)

        assertFalse(authRepository.isSignedIn())
        assertFalse(target.uiState.value.isSigningIn)
        assertTrue(events.any { it is SignInEvent.ShowNotice })
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
