package org.sesacteamproject.passmate.ui.payment

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.policy.CoinPolicy
import org.sesacteamproject.passmate.payment.domain.usecase.ConfirmChargeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.PayEntryFeeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.RequestChargeUseCase
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// 베타 잠금 (Figma "13 · 베타 운영" M-11β) — 결제가 잠기면 참가비 차감도, 부족분 충전도 나가지 않는다
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private fun paidRoom(): RoomInfo {
        return RoomInfo(
            roomId = 1L,
            pin = "482913",
            title = "8월 4주차 Spring 스터디",
            topic = null,
            status = RoomStatus.WAITING,
            questionCount = 8,
            estimatedMinutes = 15,
            scheduledAt = null,
            participantCount = 12,
            maxParticipants = 30,
            isPaid = true,
            entryFee = 10_000,
            isGuestAllowed = false,
            host = null
        )
    }

    private fun paymentRepository(balance: Int): FakePaymentRepository {
        return FakePaymentRepository(
            coinsResult = AppResult.Success(CoinBalance(balance = balance, defaultMethod = null, recent = null))
        )
    }

    private fun viewModel(
        paymentRepository: FakePaymentRepository,
        isBetaPaymentLocked: Boolean
    ): PaymentViewModel {
        val roomRepository = FakeRoomRepository(roomInfo = paidRoom())

        return PaymentViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(roomRepository),
            getMyCoinsUseCase = GetMyCoinsUseCase(paymentRepository),
            requestChargeUseCase = RequestChargeUseCase(paymentRepository),
            confirmChargeUseCase = ConfirmChargeUseCase(paymentRepository),
            payEntryFeeUseCase = PayEntryFeeUseCase(paymentRepository),
            joinRoomUseCase = JoinRoomUseCase(roomRepository),
            coinPolicy = CoinPolicy(),
            joinInputPolicy = JoinInputPolicy(),
            isBetaPaymentLocked = isBetaPaymentLocked
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
    fun lockedPaySendsNoEntryPayment() = runTest {
        val repository = paymentRepository(balance = 20_000)
        val viewModel = viewModel(repository, isBetaPaymentLocked = true)

        viewModel.onAction(PaymentAction.Start("482913"))
        viewModel.onAction(PaymentAction.ChangeNickname("패시"))
        viewModel.onAction(PaymentAction.ClickPay)

        assertEquals(0, repository.payEntryCalls)
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    // 코인이 모자란 경로 — 부족 시트도 뜨지 않고, 시트의 충전 CTA가 들어와도 요청이 나가지 않는다
    @Test
    fun lockedShortagePathSendsNoChargeRequest() = runTest {
        val repository = paymentRepository(balance = 0)
        val viewModel = viewModel(repository, isBetaPaymentLocked = true)

        viewModel.onAction(PaymentAction.Start("482913"))
        viewModel.onAction(PaymentAction.ChangeNickname("패시"))
        viewModel.onAction(PaymentAction.ClickPay)
        viewModel.onAction(PaymentAction.ConfirmCharge)

        assertFalse(viewModel.uiState.value.isCoinShortageSheetVisible)
        assertNull(repository.chargedAmount)
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun lockedStateIsExposedToScreen() = runTest {
        val viewModel = viewModel(paymentRepository(balance = 20_000), isBetaPaymentLocked = true)

        assertTrue(viewModel.uiState.value.isBetaPaymentLocked)
    }

    // 정식 출시 때 스위치를 내리면 참가비 결제가 원래대로 나간다
    @Test
    fun unlockedPayRequestsEntryPayment() = runTest {
        val repository = paymentRepository(balance = 20_000)
        val viewModel = viewModel(repository, isBetaPaymentLocked = false)

        viewModel.onAction(PaymentAction.Start("482913"))
        viewModel.onAction(PaymentAction.ChangeNickname("패시"))
        viewModel.onAction(PaymentAction.ClickPay)

        assertEquals(1, repository.payEntryCalls)
        assertFalse(viewModel.uiState.value.isBetaPaymentLocked)
    }
}
