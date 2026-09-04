package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignOutUseCase
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyProfileUseCase

// 약관 전용 화면·계약이 아직 없다 — 라우트가 생기기 전까지는 안내 문구만 노출한다
private const val TERMS_NOTICE = "약관 · 개인정보 처리방침은 준비 중이에요"

// 마이 탭 루트 (M-12) — 프로필·코인·정산 3섹션을 독립 로드한다. 금액·등급 계산은 전부 서버 값 렌더 (규칙 §1)
class MyInfoViewModel(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getMyCoinsUseCase: GetMyCoinsUseCase,
    private val getEarningsUseCase: GetEarningsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<MyInfoUiState, MyInfoAction, MyInfoEvent>(MyInfoUiState()) {

    private fun onEnter() {
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            emit(MyInfoEvent.RequireSignIn)
        } else {
            // 재진입(상세 페이지에서 pop)마다 다시 부른다 — 시트가 아니라 push라
            // 닉네임·결제 수단·정산 계좌 저장 결과가 이 화면으로 돌아온다 (hasEntered 가드 없음)
            loadAll()
        }
    }

    private fun loadAll() {
        loadProfile()
        loadCoinInfo()
        loadEarnings()
    }

    private fun loadProfile() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getMyProfileUseCase.invoke()
                .onSuccess { profile ->
                    _uiState.update { it.copy(isLoading = false, loadFailed = false, profile = profile) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    // 진행 중 재시도가 있으면 새로 던지지 않는다 — onEnter와 같은 in-flight 가드 (규칙 §9)
    private fun loadCoinInfo() {
        if (_uiState.value.isCoinInfoLoading) {
            return
        }
        _uiState.update { it.copy(isCoinInfoLoading = true) }
        viewModelScope.launch {
            getMyCoinsUseCase.invoke()
                .onSuccess { coins ->
                    _uiState.update {
                        it.copy(
                            defaultMethod = coins.defaultMethod,
                            recentTransaction = coins.recent,
                            isCoinInfoFailed = false,
                            isCoinInfoLoading = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isCoinInfoFailed = true, isCoinInfoLoading = false) }
                }
        }
    }

    private fun loadEarnings() {
        if (_uiState.value.isEarningsLoading) {
            return
        }
        _uiState.update { it.copy(isEarningsLoading = true) }
        viewModelScope.launch {
            getEarningsUseCase.invoke(null)
                .onSuccess { earnings ->
                    _uiState.update {
                        it.copy(
                            settlementAccount = earnings.account,
                            nextPayout = earnings.nextPayout,
                            isEarningsFailed = false,
                            isEarningsLoading = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isEarningsFailed = true, isEarningsLoading = false) }
                }
        }
    }

    private fun onConfirmSignOut() {
        if (_uiState.value.isProcessing) {
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            // 로컬 세션 정리는 shared가 항상 수행 — 실패 케이스 없음 (M-12-11)
            signOutUseCase.invoke()
            _uiState.update { it.copy(isProcessing = false) }
            _event.emit(MyInfoEvent.SignedOut)
        }
    }

    private fun emit(event: MyInfoEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    override fun onAction(action: MyInfoAction) {
        when (action) {
            is MyInfoAction.Enter -> onEnter()
            is MyInfoAction.Retry -> loadAll()
            is MyInfoAction.RetryCoinInfo -> loadCoinInfo()
            is MyInfoAction.RetryEarnings -> loadEarnings()
            is MyInfoAction.ClickProfile -> emit(MyInfoEvent.OpenReputation)
            is MyInfoAction.ClickEditProfile -> emit(MyInfoEvent.OpenEditProfile)
            is MyInfoAction.ClickCharge -> emit(MyInfoEvent.OpenCharge)
            is MyInfoAction.ClickPaymentMethod -> emit(MyInfoEvent.OpenPaymentMethod)
            is MyInfoAction.ClickCoinHistory -> emit(MyInfoEvent.OpenCoinHistory)
            is MyInfoAction.ClickSettlementAccount -> emit(MyInfoEvent.OpenSettlementAccount)
            is MyInfoAction.ClickEarnings -> emit(MyInfoEvent.OpenEarnings)
            is MyInfoAction.ClickNotifications -> emit(MyInfoEvent.OpenNotifications)
            is MyInfoAction.ClickDeleteAccount -> emit(MyInfoEvent.OpenDeleteAccount)
            is MyInfoAction.ClickTerms -> emit(MyInfoEvent.ShowNotice(TERMS_NOTICE))
            is MyInfoAction.ConfirmSignOut -> onConfirmSignOut()
            is MyInfoAction.Notice -> emit(MyInfoEvent.ShowNotice(action.message))
        }
    }
}
