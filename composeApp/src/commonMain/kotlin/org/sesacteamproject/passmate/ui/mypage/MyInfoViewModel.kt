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

// 마이 탭 루트 (M-12) — 프로필·코인·정산 3섹션을 독립 로드한다. 금액·등급 계산은 전부 서버 값 렌더 (규칙 §1)
class MyInfoViewModel(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getMyCoinsUseCase: GetMyCoinsUseCase,
    private val getEarningsUseCase: GetEarningsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<MyInfoUiState, MyInfoAction, MyInfoEvent>(MyInfoUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            emit(MyInfoEvent.RequireSignIn)
        } else {
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

    private fun loadCoinInfo() {
        viewModelScope.launch {
            getMyCoinsUseCase.invoke()
                .onSuccess { coins ->
                    _uiState.update {
                        it.copy(
                            defaultMethod = coins.defaultMethod,
                            recentTransaction = coins.recent,
                            isCoinInfoFailed = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isCoinInfoFailed = true) }
                }
        }
    }

    private fun loadEarnings() {
        viewModelScope.launch {
            getEarningsUseCase.invoke(null)
                .onSuccess { earnings ->
                    _uiState.update {
                        it.copy(
                            settlementAccount = earnings.account,
                            nextPayout = earnings.nextPayout,
                            isEarningsFailed = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isEarningsFailed = true) }
                }
        }
    }

    private fun onClickEditProfile() {
        val profile = _uiState.value.profile

        if (profile != null) {
            emit(MyInfoEvent.OpenEditProfile(profile.nickname, profile.avatarId))
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

    private fun onProfileUpdated() {
        loadProfile()
        emit(MyInfoEvent.ShowNotice("내 정보를 저장했어요"))
    }

    private fun onPaymentMethodUpdated() {
        loadCoinInfo()
        emit(MyInfoEvent.ShowNotice("기본 결제 수단을 저장했어요"))
    }

    private fun onAccountUpdated() {
        loadEarnings()
        emit(MyInfoEvent.ShowNotice("정산 계좌를 저장했어요"))
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
            is MyInfoAction.ClickProfile -> emit(MyInfoEvent.OpenReputation)
            is MyInfoAction.ClickEditProfile -> onClickEditProfile()
            is MyInfoAction.ClickCharge -> emit(MyInfoEvent.OpenCharge)
            is MyInfoAction.ClickPaymentMethod -> emit(MyInfoEvent.OpenPaymentMethod)
            is MyInfoAction.ClickCoinHistory -> emit(MyInfoEvent.OpenCoinHistory)
            is MyInfoAction.ClickSettlementAccount -> emit(MyInfoEvent.OpenSettlementAccount)
            is MyInfoAction.ClickEarnings -> emit(MyInfoEvent.OpenEarnings)
            is MyInfoAction.ClickNotifications -> emit(MyInfoEvent.OpenNotifications)
            is MyInfoAction.ClickSettings -> emit(MyInfoEvent.OpenSettings)
            is MyInfoAction.ConfirmSignOut -> onConfirmSignOut()
            is MyInfoAction.ProfileUpdated -> onProfileUpdated()
            is MyInfoAction.PaymentMethodUpdated -> onPaymentMethodUpdated()
            is MyInfoAction.AccountUpdated -> onAccountUpdated()
            is MyInfoAction.Notice -> emit(MyInfoEvent.ShowNotice(action.message))
        }
    }
}
