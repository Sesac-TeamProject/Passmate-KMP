package org.sesacteamproject.passmate.ui.mypage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.model.UserProfile
import org.sesacteamproject.passmate.user.domain.usecase.GetMyProfileUseCase
import org.sesacteamproject.passmate.user.domain.usecase.UpdateMyProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    private val profile = UserProfile(
        nickname = "준영",
        email = "junyoung@example.com",
        joinedAt = "2026-08-01T00:00:00Z",
        avatarId = 3,
        level = HostLevel.GROWING,
        coins = 1200L,
        joinedRoomCount = 32,
        hostedRoomCount = 12
    )

    private lateinit var userRepository: FakeUserRepository

    private fun viewModel(): EditProfileViewModel {
        return EditProfileViewModel(
            getMyProfileUseCase = GetMyProfileUseCase(userRepository),
            updateMyProfileUseCase = UpdateMyProfileUseCase(userRepository)
        )
    }

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
        userRepository = FakeUserRepository(profileResult = AppResult.Success(profile))
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }

    @Test
    fun enterLoadsProfileFields() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.Enter)

        val state = viewModel.uiState.value

        assertEquals("준영", state.nickname)
        assertEquals("junyoung@example.com", state.email)
        assertEquals(3, state.avatarId)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.hasLoadError)
    }

    @Test
    fun loadFailureMarksLoadError() = runTest {
        userRepository.profileResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.Enter)

        val state = viewModel.uiState.value

        assertEquals(true, state.hasLoadError)
        assertEquals(false, state.isLoading)
    }

    // M-12-7에서 캐릭터를 바꾸고 돌아오면 이 화면이 다시 조회해야 한다 — 컨테이너는 복귀할 때마다 Enter를 보낸다
    @Test
    fun reEnterReloadsProfile() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.Enter)
        assertEquals(3, viewModel.uiState.value.avatarId)

        userRepository.profileResult = AppResult.Success(profile.copy(avatarId = 7))
        viewModel.onAction(EditProfileAction.Enter)

        assertEquals(7, viewModel.uiState.value.avatarId)
    }
}
