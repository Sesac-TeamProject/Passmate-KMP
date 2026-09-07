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
import org.sesacteamproject.passmate.auth.domain.usecase.DevSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.IsDevSignInAvailableUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignInWithGoogleUseCase
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
        devSignInResult: AppResult<Unit> = AppResult.Success(Unit),
        googleSignInResult: AppResult<Unit> = AppResult.Success(Unit)
    ): SignInViewModel {
        authRepository = FakeAuthRepository(false)
        authRepository.isDevSignInAvailable = isDevSignInAvailable
        authRepository.devSignInResult = devSignInResult
        authRepository.googleSignInResult = googleSignInResult

        val userRepository = FakeUserRepository()
        val completeGuestClaimUseCase = CompleteGuestClaimUseCase(PendingGuestClaim(), userRepository)

        return SignInViewModel(
            signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository),
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

    // 구글 로그인은 네이티브 SDK 시트를 여는 것부터 시작한다 — URL을 여는 게 아니다
    @Test
    fun clickGoogleSignInAsksPlatformForIdToken() = runTest {
        val target = viewModel()
        val events = mutableListOf<SignInEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.event.collect { events.add(it) }
        }

        target.onAction(SignInAction.ClickGoogleSignIn)

        assertTrue(target.uiState.value.isSigningIn)
        assertTrue(events.contains(SignInEvent.RequestGoogleSignIn))
    }

    @Test
    fun googleIdTokenStoresSessionAndCompletes() = runTest {
        val target = viewModel()
        val events = mutableListOf<SignInEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.event.collect { events.add(it) }
        }

        target.onAction(SignInAction.ReceiveGoogleIdToken("google-id-token"))

        assertEquals("google-id-token", authRepository.googleIdToken)
        assertTrue(authRepository.isSignedIn())
        assertFalse(target.uiState.value.isSigningIn)
        assertTrue(events.contains(SignInEvent.SignInCompleted))
    }

    @Test
    fun googleSignInFailureShowsNoticeAndKeepsGuest() = runTest {
        val target = viewModel(googleSignInResult = AppResult.Failure(AppError.NetworkError()))
        val events = mutableListOf<SignInEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.event.collect { events.add(it) }
        }

        target.onAction(SignInAction.ReceiveGoogleIdToken("google-id-token"))

        assertFalse(authRepository.isSignedIn())
        assertFalse(target.uiState.value.isSigningIn)
        assertTrue(events.any { it is SignInEvent.ShowNotice })
    }

    // 시트를 닫은 것뿐이면 실패가 아니다 — 진행 표시만 걷고 안내는 띄우지 않는다
    @Test
    fun cancelGoogleSignInClearsProgressWithoutNotice() = runTest {
        val target = viewModel()
        val events = mutableListOf<SignInEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.event.collect { events.add(it) }
        }

        target.onAction(SignInAction.ClickGoogleSignIn)
        target.onAction(SignInAction.CancelGoogleSignIn)

        assertFalse(target.uiState.value.isSigningIn)
        assertFalse(events.any { it is SignInEvent.ShowNotice })
    }

    // 안내 문구는 화면이 아니라 ViewModel이 정한다 (규칙 §7)
    @Test
    fun failGoogleSignInShowsNotice() = runTest {
        val target = viewModel()
        val events = mutableListOf<SignInEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            target.event.collect { events.add(it) }
        }

        target.onAction(SignInAction.ClickGoogleSignIn)
        target.onAction(SignInAction.FailGoogleSignIn)

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
