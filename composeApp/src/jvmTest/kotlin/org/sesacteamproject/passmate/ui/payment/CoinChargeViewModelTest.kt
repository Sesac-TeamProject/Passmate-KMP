package org.sesacteamproject.passmate.ui.payment

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.component.PortOneResult
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.ChargeConfirm
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.policy.CoinPolicy
import org.sesacteamproject.passmate.payment.domain.usecase.ConfirmChargeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.RequestChargeUseCase
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class CoinChargeViewModelTest {

    private val checkout = CoinCheckout(
        chargeId = "charge_1",
        storeId = "store-1",
        channelKey = "channel-1",
        paymentId = "payment_1",
        orderName = "패스메이트 코인 10,000 C 충전",
        amount = 10_000,
        currency = "KRW",
        payMethod = "EASY_PAY"
    )

    // 충전 흐름 테스트는 잠금이 풀린 상태가 전제다 — 운영 스위치(BetaConfig)에 기대지 않게 기본값을 false로 고정한다.
    // 베타 잠금 테스트만 true를 명시한다
    private fun viewModel(
        repository: FakePaymentRepository,
        isBetaPaymentLocked: Boolean = false
    ): CoinChargeViewModel {
        return CoinChargeViewModel(
            getMyCoinsUseCase = GetMyCoinsUseCase(repository),
            requestChargeUseCase = RequestChargeUseCase(repository),
            confirmChargeUseCase = ConfirmChargeUseCase(repository),
            coinPolicy = CoinPolicy(),
            isBetaPaymentLocked = isBetaPaymentLocked
        )
    }

    @Test
    fun enterLoadsBalanceAndSelectsDefaultMethod() = runTest {
        val repository = FakePaymentRepository(
            coinsResult = AppResult.Success(
                CoinBalance(balance = 1200, defaultMethod = PaymentMethod.NAVER_PAY, recent = null)
            )
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(CoinChargeAction.Enter)

        val state = viewModel.uiState.value

        assertEquals(1200, state.balance)
        assertEquals(10_000, state.selectedAmount)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun selectAmountUpdatesSelection() = runTest {
        val viewModel = viewModel(FakePaymentRepository())

        viewModel.onAction(CoinChargeAction.SelectAmount(30_000))

        assertEquals(30_000, viewModel.uiState.value.selectedAmount)
    }

    @Test
    fun clickChargeOpensPortOneWithSelectedAmount() = runTest {
        val repository = FakePaymentRepository(
            coinsResult = AppResult.Success(CoinBalance(1200, PaymentMethod.KAKAO_PAY, null)),
            chargeResult = AppResult.Success(checkout)
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(CoinChargeAction.Enter)
        viewModel.onAction(CoinChargeAction.SelectAmount(30_000))
        viewModel.onAction(CoinChargeAction.ClickCharge)

        assertEquals(30_000, repository.chargedAmount)
        assertNotNull(viewModel.uiState.value.checkout)
    }

    @Test
    fun portOneSuccessConfirmsChargeAndShowsCompleted() = runTest {
        val repository = FakePaymentRepository(
            coinsResult = AppResult.Success(CoinBalance(1200, PaymentMethod.KAKAO_PAY, null)),
            chargeResult = AppResult.Success(checkout),
            confirmResult = AppResult.Success(ChargeConfirm(balance = 11_200, entryPayment = null))
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(CoinChargeAction.Enter)
        viewModel.onAction(CoinChargeAction.ClickCharge)
        viewModel.onAction(CoinChargeAction.ReceivePortOneResult(PortOneResult.Success("payment_1")))

        val state = viewModel.uiState.value

        assertEquals("payment_1", repository.confirmedPaymentId)
        assertTrue(state.isCompleted)
        assertEquals(11_200, state.balance)
        assertEquals(10_000, state.chargedAmount)
        assertNull(state.checkout)
    }

    @Test
    fun loadFailureShowsErrorState() = runTest {
        val viewModel = viewModel(FakePaymentRepository(coinsResult = AppResult.Failure(AppError.NetworkError())))

        viewModel.onAction(CoinChargeAction.Enter)

        val state = viewModel.uiState.value

        assertTrue(state.hasLoadError)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun chargeRequestFailureShowsMessageAndStopsProcessing() = runTest {
        val repository = FakePaymentRepository(
            coinsResult = AppResult.Success(CoinBalance(1200, PaymentMethod.KAKAO_PAY, null)),
            chargeResult = AppResult.Failure(AppError.NetworkError())
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(CoinChargeAction.Enter)
        viewModel.onAction(CoinChargeAction.ClickCharge)

        val state = viewModel.uiState.value

        assertEquals("네트워크 연결을 확인해 주세요", state.errorMessage)
        assertEquals(false, state.isProcessing)
    }

    @Test
    fun portOneCancelStopsProcessingWithoutError() = runTest {
        val repository = FakePaymentRepository(
            coinsResult = AppResult.Success(CoinBalance(1200, PaymentMethod.KAKAO_PAY, null)),
            chargeResult = AppResult.Success(checkout)
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(CoinChargeAction.Enter)
        viewModel.onAction(CoinChargeAction.ClickCharge)
        viewModel.onAction(CoinChargeAction.ReceivePortOneResult(PortOneResult.Cancelled))

        val state = viewModel.uiState.value

        assertEquals(false, state.isProcessing)
        assertNull(state.errorMessage)
        assertNull(state.checkout)
    }

    // 베타 잠금 (Figma "13 · 베타 운영" M-12-4β) — 결제가 잠기면 충전 요청이 아예 나가지 않는다.
    // 버튼만 끄는 것으로는 부족하다: 화면 밖 경로로 액션이 들어와도 서버에 닿으면 안 된다
    @Test
    fun lockedChargeSendsNoRequest() = runTest {
        val repository = FakePaymentRepository(
            coinsResult = AppResult.Success(CoinBalance(1200, PaymentMethod.KAKAO_PAY, null)),
            chargeResult = AppResult.Success(checkout)
        )
        val viewModel = viewModel(repository, isBetaPaymentLocked = true)

        viewModel.onAction(CoinChargeAction.Enter)
        viewModel.onAction(CoinChargeAction.ClickCharge)

        assertNull(repository.chargedAmount)
        assertFalse(viewModel.uiState.value.isProcessing)
        assertNull(viewModel.uiState.value.checkout)
    }

    // 화면은 이 값 하나로 배너를 띄우고 버튼을 끈다
    @Test
    fun lockedStateIsExposedToScreen() = runTest {
        val locked = viewModel(FakePaymentRepository(), isBetaPaymentLocked = true)
        val unlocked = viewModel(FakePaymentRepository(), isBetaPaymentLocked = false)

        assertTrue(locked.uiState.value.isBetaPaymentLocked)
        assertFalse(unlocked.uiState.value.isBetaPaymentLocked)
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
