package org.sesacteamproject.passmate.ui.mypage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignOutUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransactionType
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.NextPayout
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccountSummary
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.model.UserProfile
import org.sesacteamproject.passmate.user.domain.usecase.GetMyProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class MyInfoViewModelTest {

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

    private val coins = CoinBalance(
        balance = 1200,
        defaultMethod = PaymentMethod.KAKAO_PAY,
        recent = CoinTransaction(
            id = 1L,
            type = CoinTransactionType.DEDUCT,
            amount = -10000,
            balanceAfter = 1200,
            method = null,
            roomTitle = "Spring 실전 모의고사",
            paymentNo = null,
            createdAt = "2026-08-22T10:00:00Z"
        )
    )

    private val earnings = Earnings(
        monthlyTotal = 384000L,
        hostSharePercent = 80,
        nextPayout = NextPayout(dateLabel = "9/5", amount = 64000L),
        paidRoomCount = 12,
        studentCount = 48,
        items = emptyList(),
        nextCursor = null,
        hasNext = false,
        account = SettlementAccountSummary(bankName = "국민", maskedNumber = "***-***-4821", payoutNote = null)
    )

    private lateinit var authRepository: FakeAuthRepository

    private lateinit var userRepository: FakeUserRepository

    private lateinit var paymentRepository: FakePaymentRepository

    private fun viewModel(isSignedIn: Boolean = true): MyInfoViewModel {
        authRepository = FakeAuthRepository(isSignedIn)
        return MyInfoViewModel(
            getMyProfileUseCase = GetMyProfileUseCase(userRepository),
            getMyCoinsUseCase = GetMyCoinsUseCase(paymentRepository),
            getEarningsUseCase = GetEarningsUseCase(paymentRepository),
            signOutUseCase = SignOutUseCase(authRepository),
            isSignedInUseCase = IsSignedInUseCase(authRepository)
        )
    }

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
        userRepository = FakeUserRepository(profileResult = AppResult.Success(profile))
        paymentRepository = FakePaymentRepository(
            coinsResult = AppResult.Success(coins),
            earningsResult = AppResult.Success(earnings)
        )
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }

    @Test
    fun guestEnterRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<MyInfoEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(MyInfoAction.Enter)

        assertEquals(listOf<MyInfoEvent>(MyInfoEvent.RequireSignIn), events)
        assertEquals(0, paymentRepository.coinsCalls)
    }

    @Test
    fun memberEnterLoadsProfileCoinsAndEarnings() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.loadFailed)
        assertEquals(profile, state.profile)
        assertEquals(PaymentMethod.KAKAO_PAY, state.defaultMethod)
        assertEquals(coins.recent, state.recentTransaction)
        assertEquals(earnings.account, state.settlementAccount)
        assertEquals(earnings.nextPayout, state.nextPayout)
        assertEquals(false, state.isCoinInfoFailed)
        assertEquals(false, state.isEarningsFailed)
    }

    @Test
    fun profileFailureMarksWholeScreenFailed() = runTest {
        userRepository.profileResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        assertEquals(true, viewModel.uiState.value.loadFailed)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun coinFailureOnlyMarksCoinCard() = runTest {
        paymentRepository.coinsResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        val state = viewModel.uiState.value
        assertEquals(false, state.loadFailed)
        assertEquals(profile, state.profile)
        assertEquals(true, state.isCoinInfoFailed)
        assertEquals(false, state.isEarningsFailed)
        assertEquals(true, state.hasPartialFailure)
    }

    @Test
    fun cardRetryReloadsOnlyThatCard() = runTest {
        paymentRepository.coinsResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)
        paymentRepository.coinsResult = AppResult.Success(coins)
        viewModel.onAction(MyInfoAction.RetryCoinInfo)

        val state = viewModel.uiState.value
        assertEquals(2, paymentRepository.coinsCalls)
        assertEquals(1, paymentRepository.earningsCalls)
        assertEquals(false, state.isCoinInfoFailed)
        assertEquals(false, state.hasPartialFailure)
    }

    // 진행 중 재시도가 끝나기 전에 또 눌러도 요청이 늘지 않는다 (규칙 §9)
    @Test
    fun retryIsIgnoredWhileTheSameCardIsStillLoading() = runTest {
        val gate = CompletableDeferred<Unit>()
        paymentRepository.coinsResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)
        paymentRepository.coinsGate = gate
        viewModel.onAction(MyInfoAction.RetryCoinInfo)
        val callsWhileLoading = paymentRepository.coinsCalls

        viewModel.onAction(MyInfoAction.RetryCoinInfo)

        assertEquals(true, viewModel.uiState.value.isCoinInfoLoading)
        assertEquals(callsWhileLoading, paymentRepository.coinsCalls)

        gate.complete(Unit)

        assertEquals(false, viewModel.uiState.value.isCoinInfoLoading)
    }

    @Test
    fun earningsFailureOnlyMarksSettlementCard() = runTest {
        paymentRepository.earningsResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        val state = viewModel.uiState.value
        assertEquals(false, state.loadFailed)
        assertEquals(true, state.isEarningsFailed)
        assertEquals(null, state.settlementAccount)
    }

    @Test
    fun confirmSignOutEmitsSignedOut() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<MyInfoEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(MyInfoAction.Enter)
        viewModel.onAction(MyInfoAction.ConfirmSignOut)

        assertEquals(listOf<MyInfoEvent>(MyInfoEvent.SignedOut), events)
        assertEquals(1, authRepository.signOutCount)
        assertEquals(false, viewModel.uiState.value.isProcessing)
    }

    @Test
    fun clickActionsEmitOpenEvents() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<MyInfoEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(MyInfoAction.Enter)
        viewModel.onAction(MyInfoAction.ClickProfile)
        viewModel.onAction(MyInfoAction.ClickEditProfile)
        viewModel.onAction(MyInfoAction.ClickCharge)
        viewModel.onAction(MyInfoAction.ClickSettlementAccount)
        viewModel.onAction(MyInfoAction.ClickDeleteAccount)

        assertEquals(
            listOf(
                MyInfoEvent.OpenReputation,
                MyInfoEvent.OpenEditProfile(nickname = "준영", avatarId = 3),
                MyInfoEvent.OpenCharge,
                MyInfoEvent.OpenSettlementAccount,
                MyInfoEvent.OpenDeleteAccount
            ),
            events
        )
    }

    // 약관 전용 화면이 없어 안내 문구만 내보낸다 (card/기타 4행)
    @Test
    fun clickTermsShowsPreparingNotice() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<MyInfoEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(MyInfoAction.Enter)
        viewModel.onAction(MyInfoAction.ClickTerms)

        assertEquals(1, events.size)
        assertEquals(true, events.first() is MyInfoEvent.ShowNotice)
    }

    @Test
    fun updatedActionsReloadOnlyTheirSection() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)
        viewModel.onAction(MyInfoAction.PaymentMethodUpdated)
        viewModel.onAction(MyInfoAction.AccountUpdated)

        assertEquals(2, paymentRepository.coinsCalls)
        assertEquals(2, paymentRepository.earningsCalls)
    }
}
