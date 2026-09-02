package org.sesacteamproject.passmate.ui.payment

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
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class EarningsViewModelTest {

    private val emptyEarnings = Earnings(
        monthlyTotal = 0L,
        hostSharePercent = 80,
        nextPayout = null,
        paidRoomCount = 0,
        studentCount = 0,
        items = emptyList(),
        nextCursor = null,
        hasNext = false,
        account = null
    )

    private fun viewModel(
        isSignedIn: Boolean,
        earningsResult: AppResult<Earnings>
    ): EarningsViewModel {
        val authRepository = FakeAuthRepository(isSignedIn)
        val paymentRepository = FakePaymentRepository(earningsResult = earningsResult)

        return EarningsViewModel(
            getEarningsUseCase = GetEarningsUseCase(paymentRepository),
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

    // 빈 상태 CTA — 「유료 방 만들기」는 방 개설 진입점인 「내가 만든 방」 탭으로 보낸다
    @Test
    fun clickCreatePaidRoomOpensHostedRooms() = runTest {
        val viewModel = viewModel(isSignedIn = true, earningsResult = AppResult.Success(emptyEarnings))
        val events = mutableListOf<EarningsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(EarningsAction.ClickCreatePaidRoom)

        assertEquals(listOf<EarningsEvent>(EarningsEvent.OpenHostedRooms), events)
    }

    // 목록 불러오기 실패 — 실패 화면 분기는 loadFailed로만 판단한다 (E-List 공통 패턴)
    @Test
    fun loadFailureMarksLoadFailed() = runTest {
        val viewModel = viewModel(
            isSignedIn = true,
            earningsResult = AppResult.Failure(AppError.NetworkError())
        )

        viewModel.onAction(EarningsAction.Enter)

        val state = viewModel.uiState.value

        assertEquals(false, state.isLoading)
        assertEquals(true, state.loadFailed)
        assertEquals(null, state.earnings)
    }
}
