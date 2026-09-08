package org.sesacteamproject.passmate.ui.payment

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.Bank
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.usecase.GetSettlementAccountUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.SaveSettlementAccountUseCase
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// M-12-3 정산 계좌 — 은행은 드롭다운에서 고르고, 저장 요청에 은행 코드가 실린다
@OptIn(ExperimentalCoroutinesApi::class)
class SettlementAccountViewModelTest {

    private fun viewModel(repository: FakePaymentRepository): SettlementAccountViewModel {
        return SettlementAccountViewModel(
            getSettlementAccountUseCase = GetSettlementAccountUseCase(repository),
            saveSettlementAccountUseCase = SaveSettlementAccountUseCase(repository)
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
    fun cannotSubmitUntilBankIsSelected() = runTest {
        val viewModel = viewModel(FakePaymentRepository())

        viewModel.onAction(SettlementAccountAction.Enter)
        viewModel.onAction(SettlementAccountAction.ChangeAccountNumber("110-123-456789"))
        viewModel.onAction(SettlementAccountAction.ChangeHolderName("홍희표"))

        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onAction(SettlementAccountAction.SelectBank(Bank.SHINHAN))

        assertTrue(viewModel.uiState.value.canSubmit)
        assertEquals("088", viewModel.uiState.value.bankCode)
        assertEquals("신한은행", viewModel.uiState.value.bankName)
    }

    @Test
    fun submitSendsSelectedBankCode() = runTest {
        val repository = FakePaymentRepository()
        val viewModel = viewModel(repository)

        viewModel.onAction(SettlementAccountAction.Enter)
        viewModel.onAction(SettlementAccountAction.SelectBank(Bank.KOOKMIN))
        viewModel.onAction(SettlementAccountAction.ChangeAccountNumber("123456-01-234567"))
        viewModel.onAction(SettlementAccountAction.ChangeHolderName("이한결"))
        viewModel.onAction(SettlementAccountAction.Submit)

        val saved = repository.savedSettlementAccount

        assertEquals("004", saved?.bankCode)
        assertEquals("국민은행", saved?.bankName)
        assertEquals("123456-01-234567", saved?.maskedAccountNumber)
        assertEquals("이한결", saved?.holderName)
    }

    // 등록된 계좌를 다시 열면 드롭다운이 서버가 준 은행으로 복원된다
    @Test
    fun enterRestoresRegisteredBank() = runTest {
        val repository = FakePaymentRepository(
            settlementAccountResult = AppResult.Success(
                SettlementAccount(
                    bankCode = "090",
                    bankName = "카카오뱅크",
                    maskedAccountNumber = "3333-**-1234",
                    holderName = "홍희표"
                )
            )
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(SettlementAccountAction.Enter)

        val state = viewModel.uiState.value

        assertEquals("090", state.bankCode)
        assertEquals("카카오뱅크", state.bankName)
        assertEquals("3333-**-1234", state.maskedAccountNumber)
        assertEquals("", state.accountNumber)
    }
}
