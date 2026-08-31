package org.sesacteamproject.passmate.ui.mypage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {

    private lateinit var userRepository: FakeUserRepository

    private fun viewModel(
        deleteResult: AppResult<Unit> = AppResult.Success(Unit),
        coinsResult: AppResult<CoinBalance> = AppResult.Success(
            CoinBalance(balance = 1200, defaultMethod = PaymentMethod.KAKAO_PAY, recent = null)
        )
    ): DeleteAccountViewModel {
        val authRepository = FakeAuthRepository(true)
        userRepository = FakeUserRepository(deleteResult = deleteResult)
        val paymentRepository = FakePaymentRepository(coinsResult = coinsResult)

        return DeleteAccountViewModel(
            getMyCoinsUseCase = GetMyCoinsUseCase(paymentRepository),
            deleteAccountUseCase = DeleteAccountUseCase(userRepository, authRepository)
        )
    }

    @Test
    fun enterLoadsCoinBalanceForDeletionNotice() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(DeleteAccountAction.Enter)

        val state = viewModel.uiState.value

        assertEquals(1200, state.coins)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun deleteIsBlockedUntilConfirmChecked() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<DeleteAccountEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(DeleteAccountAction.Enter)
        viewModel.onAction(DeleteAccountAction.ClickDelete)

        assertEquals(0, userRepository.deleteCalls)
        assertEquals(emptyList(), events)
    }

    @Test
    fun toggleConfirmFlipsCheckedState() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(DeleteAccountAction.ToggleConfirm)

        assertTrue(viewModel.uiState.value.isConfirmed)

        viewModel.onAction(DeleteAccountAction.ToggleConfirm)

        assertEquals(false, viewModel.uiState.value.isConfirmed)
    }

    @Test
    fun deleteAfterConfirmEmitsDeleted() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<DeleteAccountEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(DeleteAccountAction.Enter)
        viewModel.onAction(DeleteAccountAction.ToggleConfirm)
        viewModel.onAction(DeleteAccountAction.ClickDelete)

        assertEquals(1, userRepository.deleteCalls)
        assertEquals(listOf<DeleteAccountEvent>(DeleteAccountEvent.Deleted), events)
        assertEquals(false, viewModel.uiState.value.isProcessing)
    }

    @Test
    fun deleteConflictShowsServerMessage() = runTest {
        val viewModel = viewModel(
            deleteResult = AppResult.Failure(AppError.Conflict(serverMessage = "정산 대기 금액이 있어요"))
        )
        val events = mutableListOf<DeleteAccountEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(DeleteAccountAction.Enter)
        viewModel.onAction(DeleteAccountAction.ToggleConfirm)
        viewModel.onAction(DeleteAccountAction.ClickDelete)

        assertEquals(listOf<DeleteAccountEvent>(DeleteAccountEvent.ShowNotice("정산 대기 금액이 있어요")), events)
        assertEquals(false, viewModel.uiState.value.isProcessing)
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
