package org.sesacteamproject.passmate.ui.payment

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransactionType
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.usecase.GetCoinTransactionsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class CoinHistoryViewModelTest {

    private val charge = CoinTransaction(
        id = 1,
        type = CoinTransactionType.CHARGE,
        amount = 10_000,
        balanceAfter = 11_200,
        method = PaymentMethod.KAKAO_PAY,
        roomTitle = null,
        paymentNo = "PAY-1",
        createdAt = "2026-08-20T10:00:00Z"
    )

    private val deduct = CoinTransaction(
        id = 2,
        type = CoinTransactionType.DEDUCT,
        amount = -10_000,
        balanceAfter = 1_200,
        method = null,
        roomTitle = "Spring 실전 모의고사",
        paymentNo = null,
        createdAt = "2026-08-22T10:00:00Z"
    )

    private fun viewModel(repository: FakePaymentRepository): CoinHistoryViewModel {
        return CoinHistoryViewModel(
            getMyCoinsUseCase = GetMyCoinsUseCase(repository),
            getCoinTransactionsUseCase = GetCoinTransactionsUseCase(repository)
        )
    }

    private fun repository(
        coins: AppResult<CoinBalance> = AppResult.Success(CoinBalance(1_200, PaymentMethod.KAKAO_PAY, null)),
        transactions: AppResult<PagedResult<CoinTransaction>> = AppResult.Success(
            PagedResult(items = listOf(deduct, charge), nextCursor = null, hasNext = false)
        )
    ): FakePaymentRepository {
        return FakePaymentRepository(coinsResult = coins, transactionsResult = transactions)
    }

    @Test
    fun enterLoadsBalanceAndTransactions() = runTest {
        val viewModel = viewModel(repository())

        viewModel.onAction(CoinHistoryAction.Enter)

        val state = viewModel.uiState.value

        assertEquals(1_200, state.balance)
        assertEquals(2, state.items.size)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.hasError)
    }

    @Test
    fun balanceFailureKeepsListVisible() = runTest {
        val viewModel = viewModel(repository(coins = AppResult.Failure(AppError.NetworkError())))

        viewModel.onAction(CoinHistoryAction.Enter)

        val state = viewModel.uiState.value

        assertNull(state.balance)
        assertEquals(false, state.hasError)
        assertEquals(2, state.visibleItems.size)
    }

    @Test
    fun transactionsFailureShowsListFailure() = runTest {
        val viewModel = viewModel(repository(transactions = AppResult.Failure(AppError.NetworkError())))

        viewModel.onAction(CoinHistoryAction.Enter)

        val state = viewModel.uiState.value

        assertTrue(state.hasError)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.isEmpty)
    }

    @Test
    fun emptyListShowsEmptyState() = runTest {
        val viewModel = viewModel(
            repository(transactions = AppResult.Success(PagedResult(emptyList(), null, false)))
        )

        viewModel.onAction(CoinHistoryAction.Enter)

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun selectFilterNarrowsVisibleItemsBySign() = runTest {
        val viewModel = viewModel(repository())

        viewModel.onAction(CoinHistoryAction.Enter)
        viewModel.onAction(CoinHistoryAction.SelectFilter(CoinHistoryFilter.CHARGE))

        assertEquals(listOf(charge), viewModel.uiState.value.visibleItems)

        viewModel.onAction(CoinHistoryAction.SelectFilter(CoinHistoryFilter.SPEND))

        assertEquals(listOf(deduct), viewModel.uiState.value.visibleItems)

        viewModel.onAction(CoinHistoryAction.SelectFilter(CoinHistoryFilter.ALL))

        assertEquals(2, viewModel.uiState.value.visibleItems.size)
    }

    @Test
    fun clickChargeEmitsOpenCoinCharge() = runTest {
        val viewModel = viewModel(repository())
        val events = mutableListOf<CoinHistoryEvent>()

        backgroundScope.launch { viewModel.event.collect { events.add(it) } }
        runCurrent()
        viewModel.onAction(CoinHistoryAction.ClickCharge)
        runCurrent()

        assertEquals(1, events.size)
        assertTrue(events.first() is CoinHistoryEvent.OpenCoinCharge)
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
