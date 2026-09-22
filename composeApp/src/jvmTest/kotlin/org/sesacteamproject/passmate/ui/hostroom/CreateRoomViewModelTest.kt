package org.sesacteamproject.passmate.ui.hostroom

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary
import org.sesacteamproject.passmate.question.domain.usecase.GetMyQuestionSetsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.CreateRoomUseCase
import org.sesacteamproject.passmate.testing.FakeQuestionRepository
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// 베타 잠금 (Figma "13 · 베타 운영" M-13aβ) — 결제가 잠긴 동안은 유료 방을 열 수 없다.
// 결제만 막고 개설을 열어 두면 아무도 못 들어가는 방이 생긴다
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRoomViewModelTest {

    private fun viewModel(
        roomRepository: FakeRoomRepository,
        isBetaPaymentLocked: Boolean
    ): CreateRoomViewModel {
        val questionRepository = FakeQuestionRepository(
            sets = listOf(QuestionSetSummary(setId = 7L, title = "Spring 기초 세트", isConfirmed = true, questionCount = 8))
        )

        return CreateRoomViewModel(
            getMyQuestionSetsUseCase = GetMyQuestionSetsUseCase(questionRepository),
            createRoomUseCase = CreateRoomUseCase(roomRepository),
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
    fun lockedSelectPaidStaysFree() = runTest {
        val viewModel = viewModel(FakeRoomRepository(), isBetaPaymentLocked = true)

        viewModel.onAction(CreateRoomAction.Enter)
        viewModel.onAction(CreateRoomAction.SelectPaid(true))

        assertFalse(viewModel.uiState.value.isPaid)
        assertTrue(viewModel.uiState.value.isBetaPaymentLocked)
    }

    // 무료 방 만들기는 잠금과 무관하게 그대로 된다 — 시안의 만들기 버튼은 활성이다
    @Test
    fun lockedSubmitCreatesFreeRoom() = runTest {
        val roomRepository = FakeRoomRepository()
        val viewModel = viewModel(roomRepository, isBetaPaymentLocked = true)

        viewModel.onAction(CreateRoomAction.Enter)
        viewModel.onAction(CreateRoomAction.ChangeTitle("8월 4주차 Spring 스터디"))
        viewModel.onAction(CreateRoomAction.SelectPaid(true))
        viewModel.onAction(CreateRoomAction.Submit)

        assertEquals(false, roomRepository.createdIsPaid)
    }

    // 정식 출시 때 스위치를 내리면 유료 탭이 원래대로 골라진다
    @Test
    fun unlockedSelectPaidTurnsPaid() = runTest {
        val viewModel = viewModel(FakeRoomRepository(), isBetaPaymentLocked = false)

        viewModel.onAction(CreateRoomAction.Enter)
        viewModel.onAction(CreateRoomAction.SelectPaid(true))

        assertTrue(viewModel.uiState.value.isPaid)
        assertFalse(viewModel.uiState.value.isBetaPaymentLocked)
    }
}
